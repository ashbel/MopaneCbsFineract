/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.mopane.ssb.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.mopane.ssb.data.SsbConstants;
import org.apache.fineract.mopane.ssb.data.SsbPayBatchData;
import org.apache.fineract.mopane.ssb.data.SsbPayRowData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.google.gson.JsonObject;

@Service
public class SsbPayImportWritePlatformServiceImpl implements SsbPayImportWritePlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final SsbPayResultWorkbookWriter resultWorkbookWriter;
    private final SsbWorkbookHelper workbookHelper;
    private final SsbLoanMatcher loanMatcher;

    @Autowired
    public SsbPayImportWritePlatformServiceImpl(final RoutingDataSource dataSource,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            final SsbPayResultWorkbookWriter resultWorkbookWriter, final SsbWorkbookHelper workbookHelper,
            final SsbLoanMatcher loanMatcher) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.resultWorkbookWriter = resultWorkbookWriter;
        this.workbookHelper = workbookHelper;
        this.loanMatcher = loanMatcher;
    }

    @Override
    public Response processUpload(final InputStream inputStream, final String filename, final String bureau, final Long paymentTypeId,
            final boolean dryRun, final AppUser user) {

        final String normalizedBureau = SsbImportSupport.normalizeBureau(bureau);
        final List<SsbPayRowData> parsed;
        try {
            final Workbook workbook = WorkbookFactory.create(inputStream);
            parsed = parsePayWorkbook(workbook);
        } catch (final PlatformDataIntegrityException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new PlatformDataIntegrityException("error.msg.ssb.pay.parse.failed",
                    "Failed to parse PAY workbook: " + ex.getMessage(), "file", ex.getMessage());
        }

        final Long resolvedPaymentTypeId = paymentTypeId != null ? paymentTypeId : defaultPaymentTypeId();
        final Long batchId = insertBatch(normalizedBureau, filename, user.getId(), dryRun, resolvedPaymentTypeId);

        int posted = 0;
        int failed = 0;
        int needsReview = 0;
        for (final SsbPayRowData row : parsed) {
            processRow(row, normalizedBureau, resolvedPaymentTypeId, dryRun);
            insertRow(batchId, row);
            if (SsbConstants.STATUS_POSTED.equals(row.getStatus())) {
                posted++;
            } else if (SsbConstants.STATUS_NEEDS_REVIEW.equals(row.getStatus())) {
                needsReview++;
            } else {
                failed++;
            }
        }
        updateBatchCounts(batchId, posted, failed, needsReview, parsed.size());

        try {
            final Workbook result = this.resultWorkbookWriter.write(parsed);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            result.write(baos);
            final String outName = normalizedBureau + "_PAY_RESULT_" + batchId + ".xlsx";
            final ResponseBuilder response = Response.ok(baos.toByteArray());
            response.header("Content-Disposition", "attachment; filename=\"" + outName + "\"");
            response.header("Content-Type", SsbConstants.XLSX_CONTENT_TYPE);
            response.header(SsbConstants.HEADER_BATCH_ID, String.valueOf(batchId));
            return response.build();
        } catch (final Exception ex) {
            throw new PlatformDataIntegrityException("error.msg.ssb.pay.result.failed",
                    "Failed to build PAY result workbook: " + ex.getMessage(), "workbook", ex.getMessage());
        }
    }

    @Override
    public Collection<SsbPayBatchData> retrieveBatches(final Integer offset, final Integer limit) {
        final int off = offset == null || offset < 0 ? 0 : offset;
        final int lim = limit == null || limit <= 0 ? 50 : Math.min(limit, 200);
        return this.jdbcTemplate.query(
                "SELECT id, bureau, filename, uploaded_by, uploaded_on, dry_run, payment_type_id, posted_count, failed_count, "
                        + "needs_review_count, total_count FROM m_ssb_pay_import_batch ORDER BY id DESC LIMIT ? OFFSET ?",
                new BatchMapper(), lim, off);
    }

    @Override
    public SsbPayBatchData retrieveBatch(final Long batchId) {
        try {
            final SsbPayBatchData batch = this.jdbcTemplate.queryForObject(
                    "SELECT id, bureau, filename, uploaded_by, uploaded_on, dry_run, payment_type_id, posted_count, failed_count, "
                            + "needs_review_count, total_count FROM m_ssb_pay_import_batch WHERE id = ?",
                    new BatchMapper(), batchId);
            batch.setRows(this.jdbcTemplate.query(
                    "SELECT id, batch_id, row_number, rec_id, deduction_code, reference, id_number, ec_number, trans_date, amount, name, "
                            + "status, reason, suggested_loan_id, suggested_account_no, posted_loan_id, posted_transaction_id, note "
                            + "FROM m_ssb_pay_import_row WHERE batch_id = ? ORDER BY row_number",
                    new PayRowMapper(), batchId));
            return batch;
        } catch (final EmptyResultDataAccessException ex) {
            throw new PlatformDataIntegrityException("error.msg.ssb.pay.batch.not.found", "PAY import batch not found: " + batchId,
                    "batchId", batchId);
        }
    }

    @Override
    @Transactional
    public SsbPayRowData approveRow(final Long batchId, final Long rowId, final Long paymentTypeId, final AppUser user) {
        final SsbPayRowData row = loadRow(batchId, rowId);
        if (!SsbConstants.STATUS_NEEDS_REVIEW.equals(row.getStatus())) {
            throw new PlatformDataIntegrityException("error.msg.ssb.pay.row.not.reviewable",
                    "Only NEEDS_REVIEW rows can be approved.", "status", row.getStatus());
        }
        if (row.getSuggestedLoanId() == null) {
            throw new PlatformDataIntegrityException("error.msg.ssb.pay.row.no.suggested.loan",
                    "Row has no suggested loan for approval.", "rowId", rowId);
        }
        final SsbPayBatchData batch = retrieveBatch(batchId);
        final Long resolvedPaymentTypeId = paymentTypeId != null ? paymentTypeId
                : (batch.getPaymentTypeId() != null ? batch.getPaymentTypeId() : defaultPaymentTypeId());
        try {
            postRepayment(row, row.getSuggestedLoanId(), batch.getBureau(), resolvedPaymentTypeId);
            row.setStatus(SsbConstants.STATUS_POSTED);
            row.setReason(null);
            persistRowUpdate(row);
            refreshBatchCounts(batchId);
            return row;
        } catch (final RuntimeException ex) {
            throw new PlatformDataIntegrityException("error.msg.ssb.pay.approve.failed",
                    "Failed to approve PAY row: " + ex.getMessage(), "rowId", ex.getMessage());
        }
    }

    @Override
    @Transactional
    public SsbPayRowData rejectRow(final Long batchId, final Long rowId, final AppUser user) {
        final SsbPayRowData row = loadRow(batchId, rowId);
        if (!SsbConstants.STATUS_NEEDS_REVIEW.equals(row.getStatus())) {
            throw new PlatformDataIntegrityException("error.msg.ssb.pay.row.not.reviewable",
                    "Only NEEDS_REVIEW rows can be rejected.", "status", row.getStatus());
        }
        row.setStatus(SsbConstants.STATUS_REJECTED);
        row.setReason("REJECTED_BY_USER");
        persistRowUpdate(row);
        refreshBatchCounts(batchId);
        return row;
    }

    private void processRow(final SsbPayRowData row, final String bureau, final Long paymentTypeId, final boolean dryRun) {
        row.setNote(buildNote(row, bureau));

        if (row.getAmount() == null || row.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            row.setStatus(SsbConstants.STATUS_FAILED);
            row.setReason(SsbConstants.REASON_INVALID_AMOUNT);
            return;
        }
        if (row.getTransDate() == null) {
            row.setStatus(SsbConstants.STATUS_FAILED);
            row.setReason(SsbConstants.REASON_INVALID_DATE);
            return;
        }

        if (StringUtils.hasText(row.getRecId()) && alreadyPosted(bureau, row.getRecId())) {
            row.setStatus(SsbConstants.STATUS_FAILED);
            row.setReason(SsbConstants.REASON_ALREADY_POSTED);
            return;
        }

        final SsbLoanMatcher.MatchResult match = this.loanMatcher.match(row.getReference(), row.getIdNumber(), row.getEcNumber());
        if (match.matchedByReference) {
            if (dryRun) {
                row.setStatus(SsbConstants.STATUS_POSTED);
                row.setPostedLoanId(match.loanId);
                row.setReason("DRY_RUN");
                return;
            }
            try {
                postRepayment(row, match.loanId, bureau, paymentTypeId);
                row.setStatus(SsbConstants.STATUS_POSTED);
            } catch (final RuntimeException ex) {
                row.setStatus(SsbConstants.STATUS_FAILED);
                row.setReason(SsbImportSupport.trimReason(ex.getMessage(), "REPAYMENT_FAILED"));
            }
            return;
        }
        if (match.needsReview) {
            row.setStatus(SsbConstants.STATUS_NEEDS_REVIEW);
            row.setReason(match.reason);
            row.setSuggestedLoanId(match.suggestedLoanId);
            row.setSuggestedAccountNo(match.suggestedAccountNo);
            return;
        }
        row.setStatus(SsbConstants.STATUS_FAILED);
        row.setReason(match.reason);
    }

    private void postRepayment(final SsbPayRowData row, final Long loanId, final String bureau, final Long paymentTypeId) {
        final Integer status = this.jdbcTemplate.queryForObject("SELECT loan_status_id FROM m_loan WHERE id = ?", Integer.class, loanId);
        if (status == null || status.intValue() != SsbConstants.LOAN_STATUS_ACTIVE) {
            throw new PlatformDataIntegrityException("error.msg.ssb.pay.loan.not.repayable",
                    "Loan is not active for repayment.", "loanId", loanId);
        }

        final SimpleDateFormat df = new SimpleDateFormat(SsbConstants.DATE_FORMAT_API, Locale.ENGLISH);
        final JsonObject json = new JsonObject();
        json.addProperty("locale", SsbConstants.LOCALE);
        json.addProperty("dateFormat", SsbConstants.DATE_FORMAT_API);
        json.addProperty("transactionDate", df.format(row.getTransDate()));
        json.addProperty("transactionAmount", row.getAmount());
        json.addProperty("note", SsbImportSupport.trimTo(row.getNote(), 1000));
        if (paymentTypeId != null) {
            json.addProperty("paymentTypeId", paymentTypeId);
        }
        if (StringUtils.hasText(row.getRecId())) {
            json.addProperty("externalId", externalId(bureau, row.getRecId()));
        }

        final CommandWrapper commandRequest = new CommandWrapperBuilder().loanRepaymentTransaction(loanId).withJson(json.toString())
                .build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        row.setPostedLoanId(loanId);
        if (result.getTransactionId() != null) {
            try {
                row.setPostedTransactionId(Long.valueOf(result.getTransactionId()));
            } catch (final NumberFormatException ignored) {
                // leave null
            }
        } else if (result.resourceId() != null) {
            row.setPostedTransactionId(result.resourceId());
        }
    }

    private List<SsbPayRowData> parsePayWorkbook(final Workbook workbook) {
        final Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
        if (sheet == null) {
            throw new PlatformDataIntegrityException("error.msg.ssb.pay.sheet.missing", "PAY workbook has no sheets.", "file");
        }
        final Row header = sheet.getRow(0);
        if (header == null) {
            throw new PlatformDataIntegrityException("error.msg.ssb.pay.header.missing", "PAY workbook header row is missing.", "file");
        }
        final Map<String, Integer> cols = this.workbookHelper.mapHeaders(header);
        SsbWorkbookHelper.requireColumn(cols, "rec id", "pay");
        SsbWorkbookHelper.requireColumn(cols, "reference", "pay");
        SsbWorkbookHelper.requireColumn(cols, "amount", "pay");

        final List<SsbPayRowData> rows = new ArrayList<>();
        final int last = sheet.getLastRowNum();
        for (int i = 1; i <= last; i++) {
            final Row excelRow = sheet.getRow(i);
            if (excelRow == null || this.workbookHelper.isEmptyRow(excelRow)) {
                continue;
            }
            final SsbPayRowData row = new SsbPayRowData();
            row.setRowNumber(i + 1);
            row.setRecId(this.workbookHelper.cellString(excelRow, cols.get("rec id")));
            row.setDeductionCode(this.workbookHelper.cellString(excelRow, cols.get("deduction code")));
            row.setReference(this.workbookHelper.cellString(excelRow, cols.get("reference")));
            row.setIdNumber(this.workbookHelper.cellString(excelRow, SsbWorkbookHelper.firstPresent(cols, "id number", "idnumber")));
            row.setEcNumber(this.workbookHelper.cellString(excelRow, SsbWorkbookHelper.firstPresent(cols, "ec number", "ecnumber")));
            row.setTransDate(this.workbookHelper.cellDate(excelRow, SsbWorkbookHelper.firstPresent(cols, "trans date", "transaction date")));
            row.setAmount(this.workbookHelper.cellDecimal(excelRow, cols.get("amount")));
            row.setName(this.workbookHelper.cellString(excelRow, cols.get("name")));
            rows.add(row);
        }
        return rows;
    }

    private boolean alreadyPosted(final String bureau, final String recId) {
        final String externalId = externalId(bureau, recId);
        final Integer txnCount = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM m_loan_transaction WHERE external_id = ? AND is_reversed = 0", Integer.class, externalId);
        if (txnCount != null && txnCount > 0) {
            return true;
        }
        final Integer rowCount = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM m_ssb_pay_import_row r JOIN m_ssb_pay_import_batch b ON b.id = r.batch_id "
                        + "WHERE r.rec_id = ? AND r.status = ? AND b.dry_run = 0",
                Integer.class, recId, SsbConstants.STATUS_POSTED);
        return rowCount != null && rowCount > 0;
    }

    private Long defaultPaymentTypeId() {
        try {
            return this.jdbcTemplate.queryForObject("SELECT id FROM m_payment_type ORDER BY id ASC LIMIT 1", Long.class);
        } catch (final EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private Long insertBatch(final String bureau, final String filename, final Long userId, final boolean dryRun,
            final Long paymentTypeId) {
        final KeyHolder keyHolder = new GeneratedKeyHolder();
        this.jdbcTemplate.update(connection -> {
            final java.sql.PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO m_ssb_pay_import_batch (bureau, filename, uploaded_by, uploaded_on, dry_run, payment_type_id, "
                            + "posted_count, failed_count, needs_review_count, total_count) VALUES (?,?,?,?,?,?,0,0,0,0)",
                    new String[] { "id" });
            ps.setString(1, bureau);
            ps.setString(2, filename);
            if (userId == null) {
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(3, userId);
            }
            ps.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.setInt(5, dryRun ? 1 : 0);
            if (paymentTypeId == null) {
                ps.setNull(6, java.sql.Types.BIGINT);
            } else {
                ps.setLong(6, paymentTypeId);
            }
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private void insertRow(final Long batchId, final SsbPayRowData row) {
        this.jdbcTemplate.update(
                "INSERT INTO m_ssb_pay_import_row (batch_id, row_number, rec_id, deduction_code, reference, id_number, ec_number, "
                        + "trans_date, amount, name, status, reason, suggested_loan_id, suggested_account_no, posted_loan_id, "
                        + "posted_transaction_id, note) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                batchId, row.getRowNumber(), row.getRecId(), row.getDeductionCode(), row.getReference(), row.getIdNumber(),
                row.getEcNumber(), row.getTransDate() == null ? null : new java.sql.Date(row.getTransDate().getTime()), row.getAmount(),
                row.getName(), row.getStatus(), row.getReason(), row.getSuggestedLoanId(), row.getSuggestedAccountNo(),
                row.getPostedLoanId(), row.getPostedTransactionId(), SsbImportSupport.trimTo(row.getNote(), 1000));
    }

    private void updateBatchCounts(final Long batchId, final int posted, final int failed, final int needsReview, final int total) {
        this.jdbcTemplate.update(
                "UPDATE m_ssb_pay_import_batch SET posted_count = ?, failed_count = ?, needs_review_count = ?, total_count = ? WHERE id = ?",
                posted, failed, needsReview, total, batchId);
    }

    private void refreshBatchCounts(final Long batchId) {
        this.jdbcTemplate.update(
                "UPDATE m_ssb_pay_import_batch b SET "
                        + "posted_count = (SELECT COUNT(*) FROM m_ssb_pay_import_row r WHERE r.batch_id = b.id AND r.status = ?), "
                        + "failed_count = (SELECT COUNT(*) FROM m_ssb_pay_import_row r WHERE r.batch_id = b.id AND r.status IN (?, ?)), "
                        + "needs_review_count = (SELECT COUNT(*) FROM m_ssb_pay_import_row r WHERE r.batch_id = b.id AND r.status = ?), "
                        + "total_count = (SELECT COUNT(*) FROM m_ssb_pay_import_row r WHERE r.batch_id = b.id) WHERE b.id = ?",
                SsbConstants.STATUS_POSTED, SsbConstants.STATUS_FAILED, SsbConstants.STATUS_REJECTED, SsbConstants.STATUS_NEEDS_REVIEW,
                batchId);
    }

    private SsbPayRowData loadRow(final Long batchId, final Long rowId) {
        try {
            return this.jdbcTemplate.queryForObject(
                    "SELECT id, batch_id, row_number, rec_id, deduction_code, reference, id_number, ec_number, trans_date, amount, name, "
                            + "status, reason, suggested_loan_id, suggested_account_no, posted_loan_id, posted_transaction_id, note "
                            + "FROM m_ssb_pay_import_row WHERE id = ? AND batch_id = ?",
                    new PayRowMapper(), rowId, batchId);
        } catch (final EmptyResultDataAccessException ex) {
            throw new PlatformDataIntegrityException("error.msg.ssb.pay.row.not.found", "PAY import row not found.", "rowId", rowId);
        }
    }

    private void persistRowUpdate(final SsbPayRowData row) {
        this.jdbcTemplate.update(
                "UPDATE m_ssb_pay_import_row SET status = ?, reason = ?, suggested_loan_id = ?, suggested_account_no = ?, "
                        + "posted_loan_id = ?, posted_transaction_id = ?, note = ? WHERE id = ?",
                row.getStatus(), row.getReason(), row.getSuggestedLoanId(), row.getSuggestedAccountNo(), row.getPostedLoanId(),
                row.getPostedTransactionId(), SsbImportSupport.trimTo(row.getNote(), 1000), row.getId());
    }

    private static String buildNote(final SsbPayRowData row, final String bureau) {
        final StringBuilder sb = new StringBuilder();
        sb.append("bureau=").append(bureau);
        appendField(sb, "Rec id", row.getRecId());
        appendField(sb, "Deduction code", row.getDeductionCode());
        appendField(sb, "Reference", row.getReference());
        appendField(sb, "Id number", row.getIdNumber());
        appendField(sb, "Ec number", row.getEcNumber());
        appendField(sb, "Name", row.getName());
        return SsbImportSupport.trimTo(sb.toString(), 1000);
    }

    private static void appendField(final StringBuilder sb, final String label, final String value) {
        if (StringUtils.hasText(value)) {
            sb.append("; ").append(label).append("=").append(value);
        }
    }

    private static String externalId(final String bureau, final String recId) {
        final String value = bureau + "-" + recId;
        return value.length() <= 100 ? value : value.substring(0, 100);
    }

    private static final class BatchMapper implements RowMapper<SsbPayBatchData> {
        @Override
        public SsbPayBatchData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final SsbPayBatchData data = new SsbPayBatchData();
            data.setId(rs.getLong("id"));
            data.setBureau(rs.getString("bureau"));
            data.setFilename(rs.getString("filename"));
            final long uploadedBy = rs.getLong("uploaded_by");
            data.setUploadedBy(rs.wasNull() ? null : uploadedBy);
            data.setUploadedOn(rs.getTimestamp("uploaded_on"));
            data.setDryRun(rs.getInt("dry_run") == 1);
            final long paymentTypeId = rs.getLong("payment_type_id");
            data.setPaymentTypeId(rs.wasNull() ? null : paymentTypeId);
            data.setPostedCount(rs.getInt("posted_count"));
            data.setFailedCount(rs.getInt("failed_count"));
            data.setNeedsReviewCount(rs.getInt("needs_review_count"));
            data.setTotalCount(rs.getInt("total_count"));
            return data;
        }
    }

    private static final class PayRowMapper implements RowMapper<SsbPayRowData> {
        @Override
        public SsbPayRowData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final SsbPayRowData row = new SsbPayRowData();
            row.setId(rs.getLong("id"));
            row.setBatchId(rs.getLong("batch_id"));
            row.setRowNumber(rs.getInt("row_number"));
            row.setRecId(rs.getString("rec_id"));
            row.setDeductionCode(rs.getString("deduction_code"));
            row.setReference(rs.getString("reference"));
            row.setIdNumber(rs.getString("id_number"));
            row.setEcNumber(rs.getString("ec_number"));
            row.setTransDate(rs.getDate("trans_date"));
            row.setAmount(rs.getBigDecimal("amount"));
            row.setName(rs.getString("name"));
            row.setStatus(rs.getString("status"));
            row.setReason(rs.getString("reason"));
            final long suggestedLoanId = rs.getLong("suggested_loan_id");
            row.setSuggestedLoanId(rs.wasNull() ? null : suggestedLoanId);
            row.setSuggestedAccountNo(rs.getString("suggested_account_no"));
            final long postedLoanId = rs.getLong("posted_loan_id");
            row.setPostedLoanId(rs.wasNull() ? null : postedLoanId);
            final long postedTxn = rs.getLong("posted_transaction_id");
            row.setPostedTransactionId(rs.wasNull() ? null : postedTxn);
            row.setNote(rs.getString("note"));
            return row;
        }
    }
}
