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
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.mopane.ssb.data.SsbConstants;
import org.apache.fineract.mopane.ssb.data.SsbResBatchData;
import org.apache.fineract.mopane.ssb.data.SsbResRowData;
import org.apache.fineract.portfolio.account.data.PortfolioAccountData;
import org.apache.fineract.portfolio.account.service.AccountAssociationsReadPlatformService;
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
public class SsbResImportWritePlatformServiceImpl implements SsbResImportWritePlatformService {

    private static final String ROW_SELECT = "SELECT id, batch_id, row_number, rec_id, deduction_code, reference, id_number, ec_number, "
            + "type, bureau_status, start_date, end_date, amount, name, message, status, reason, suggested_loan_id, "
            + "suggested_account_no, loan_id, disbursement_transaction_id, note_id, note FROM m_ssb_res_import_row";

    private static final String BATCH_SELECT = "SELECT id, bureau, filename, uploaded_by, uploaded_on, dry_run, auto_disburse, "
            + "payment_type_id, disbursed_count, authorised_count, noted_count, failed_count, needs_review_count, total_count "
            + "FROM m_ssb_res_import_batch";

    private final JdbcTemplate jdbcTemplate;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final AccountAssociationsReadPlatformService accountAssociationsReadPlatformService;
    private final SsbResResultWorkbookWriter resultWorkbookWriter;
    private final SsbWorkbookHelper workbookHelper;
    private final SsbLoanMatcher loanMatcher;
    private final SsbMandateService mandateService;

    @Autowired
    public SsbResImportWritePlatformServiceImpl(final RoutingDataSource dataSource,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            final AccountAssociationsReadPlatformService accountAssociationsReadPlatformService,
            final SsbResResultWorkbookWriter resultWorkbookWriter, final SsbWorkbookHelper workbookHelper,
            final SsbLoanMatcher loanMatcher, final SsbMandateService mandateService) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.accountAssociationsReadPlatformService = accountAssociationsReadPlatformService;
        this.resultWorkbookWriter = resultWorkbookWriter;
        this.workbookHelper = workbookHelper;
        this.loanMatcher = loanMatcher;
        this.mandateService = mandateService;
    }

    @Override
    public Response processUpload(final InputStream inputStream, final String filename, final String bureau, final Long paymentTypeId,
            final boolean dryRun, final Boolean autoDisburse, final AppUser user) {

        final String normalizedBureau = SsbImportSupport.normalizeBureau(bureau);
        final List<SsbResRowData> parsed;
        try {
            final Workbook workbook = WorkbookFactory.create(inputStream);
            parsed = parseResWorkbook(workbook);
        } catch (final PlatformDataIntegrityException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new PlatformDataIntegrityException("error.msg.ssb.res.parse.failed",
                    "Failed to parse RES workbook: " + ex.getMessage(), "file", ex.getMessage());
        }

        final boolean resolvedAutoDisburse = autoDisburse != null ? autoDisburse.booleanValue() : isAutoDisburseEnabled();
        final Long resolvedPaymentTypeId = paymentTypeId != null ? paymentTypeId : defaultPaymentTypeId();
        final Long batchId = insertBatch(normalizedBureau, filename, user.getId(), dryRun, resolvedAutoDisburse, resolvedPaymentTypeId);

        int disbursed = 0;
        int authorised = 0;
        int noted = 0;
        int failed = 0;
        int needsReview = 0;
        for (final SsbResRowData row : parsed) {
            processRow(row, normalizedBureau, resolvedPaymentTypeId, dryRun, resolvedAutoDisburse);
            insertRow(batchId, row);
            if (SsbConstants.STATUS_DISBURSED.equals(row.getStatus())) {
                disbursed++;
            } else if (SsbConstants.STATUS_AUTHORISED.equals(row.getStatus())) {
                authorised++;
            } else if (SsbConstants.STATUS_NOTED.equals(row.getStatus())) {
                noted++;
            } else if (SsbConstants.STATUS_NEEDS_REVIEW.equals(row.getStatus())) {
                needsReview++;
            } else {
                failed++;
            }
        }
        updateBatchCounts(batchId, disbursed, authorised, noted, failed, needsReview, parsed.size());

        try {
            final Workbook result = this.resultWorkbookWriter.write(parsed);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            result.write(baos);
            final String outName = normalizedBureau + "_RES_RESULT_" + batchId + ".xlsx";
            final ResponseBuilder response = Response.ok(baos.toByteArray());
            response.header("Content-Disposition", "attachment; filename=\"" + outName + "\"");
            response.header("Content-Type", SsbConstants.XLSX_CONTENT_TYPE);
            response.header(SsbConstants.HEADER_RES_BATCH_ID, String.valueOf(batchId));
            return response.build();
        } catch (final Exception ex) {
            throw new PlatformDataIntegrityException("error.msg.ssb.res.result.failed",
                    "Failed to build RES result workbook: " + ex.getMessage(), "workbook", ex.getMessage());
        }
    }

    @Override
    public Collection<SsbResBatchData> retrieveBatches(final Integer offset, final Integer limit) {
        final int off = offset == null || offset < 0 ? 0 : offset;
        final int lim = limit == null || limit <= 0 ? 50 : Math.min(limit, 200);
        return this.jdbcTemplate.query(BATCH_SELECT + " ORDER BY id DESC LIMIT ? OFFSET ?", new BatchMapper(), lim, off);
    }

    @Override
    public SsbResBatchData retrieveBatch(final Long batchId) {
        try {
            final SsbResBatchData batch = this.jdbcTemplate.queryForObject(BATCH_SELECT + " WHERE id = ?", new BatchMapper(), batchId);
            batch.setRows(this.jdbcTemplate.query(ROW_SELECT + " WHERE batch_id = ? ORDER BY row_number", new ResRowMapper(), batchId));
            return batch;
        } catch (final EmptyResultDataAccessException ex) {
            throw new PlatformDataIntegrityException("error.msg.ssb.res.batch.not.found", "RES import batch not found: " + batchId,
                    "batchId", batchId);
        }
    }

    @Override
    @Transactional
    public SsbResRowData approveRow(final Long batchId, final Long rowId, final Long paymentTypeId, final AppUser user) {
        final SsbResRowData row = loadRow(batchId, rowId);
        if (!SsbConstants.STATUS_NEEDS_REVIEW.equals(row.getStatus())) {
            throw new PlatformDataIntegrityException("error.msg.ssb.res.row.not.reviewable", "Only NEEDS_REVIEW rows can be approved.",
                    "status", row.getStatus());
        }
        if (row.getSuggestedLoanId() == null) {
            throw new PlatformDataIntegrityException("error.msg.ssb.res.row.no.suggested.loan", "Row has no suggested loan for approval.",
                    "rowId", rowId);
        }
        final SsbResBatchData batch = retrieveBatch(batchId);
        final Long resolvedPaymentTypeId = paymentTypeId != null ? paymentTypeId
                : (batch.getPaymentTypeId() != null ? batch.getPaymentTypeId() : defaultPaymentTypeId());
        try {
            applyMatchedAction(row, row.getSuggestedLoanId(), batch.getBureau(), resolvedPaymentTypeId, false, batch.isAutoDisburse());
            persistRowUpdate(row);
            refreshBatchCounts(batchId);
            return row;
        } catch (final RuntimeException ex) {
            throw new PlatformDataIntegrityException("error.msg.ssb.res.approve.failed", "Failed to approve RES row: " + ex.getMessage(),
                    "rowId", ex.getMessage());
        }
    }

    @Override
    @Transactional
    public SsbResRowData rejectRow(final Long batchId, final Long rowId, final AppUser user) {
        final SsbResRowData row = loadRow(batchId, rowId);
        if (!SsbConstants.STATUS_NEEDS_REVIEW.equals(row.getStatus())) {
            throw new PlatformDataIntegrityException("error.msg.ssb.res.row.not.reviewable", "Only NEEDS_REVIEW rows can be rejected.",
                    "status", row.getStatus());
        }
        row.setStatus(SsbConstants.STATUS_REJECTED);
        row.setReason("REJECTED_BY_USER");
        persistRowUpdate(row);
        refreshBatchCounts(batchId);
        return row;
    }

    private void processRow(final SsbResRowData row, final String bureau, final Long paymentTypeId, final boolean dryRun,
            final boolean autoDisburse) {
        row.setType(SsbImportSupport.normalizeType(row.getType()));
        row.setBureauStatus(SsbImportSupport.normalizeBureauStatus(row.getBureauStatus()));

        if (StringUtils.hasText(row.getRecId()) && alreadyProcessed(row.getRecId())) {
            row.setStatus(SsbConstants.STATUS_FAILED);
            row.setReason(SsbConstants.REASON_ALREADY_PROCESSED);
            return;
        }

        final SsbLoanMatcher.MatchResult match = this.loanMatcher.match(row.getReference(), row.getIdNumber(), row.getEcNumber());
        if (match.needsReview) {
            row.setStatus(SsbConstants.STATUS_NEEDS_REVIEW);
            row.setReason(match.reason);
            row.setSuggestedLoanId(match.suggestedLoanId);
            row.setSuggestedAccountNo(match.suggestedAccountNo);
            return;
        }
        if (match.failed) {
            row.setStatus(SsbConstants.STATUS_FAILED);
            row.setReason(match.reason);
            return;
        }
        applyMatchedAction(row, match.loanId, bureau, paymentTypeId, dryRun, autoDisburse);
    }

    private void applyMatchedAction(final SsbResRowData row, final Long loanId, final String bureau, final Long paymentTypeId,
            final boolean dryRun, final boolean autoDisburse) {
        if (!SsbConstants.BUREAU_STATUS_SUCCESS.equals(row.getBureauStatus())
                && !SsbConstants.BUREAU_STATUS_FAILED.equals(row.getBureauStatus())) {
            row.setStatus(SsbConstants.STATUS_FAILED);
            row.setReason(SsbConstants.REASON_INVALID_STATUS);
            return;
        }
        if (SsbConstants.BUREAU_STATUS_SUCCESS.equals(row.getBureauStatus())) {
            handleSuccess(row, loanId, bureau, paymentTypeId, dryRun, autoDisburse);
        } else {
            handleFailed(row, loanId, bureau, dryRun);
        }
    }

    private void handleSuccess(final SsbResRowData row, final Long loanId, final String bureau, final Long paymentTypeId,
            final boolean dryRun, final boolean autoDisburse) {
        row.setLoanId(loanId);
        row.setNote(buildAuditNote(row, bureau, "AUTHORISED"));
        if (!SsbImportSupport.isNewType(row.getType())) {
            row.setStatus(SsbConstants.STATUS_AUTHORISED);
            row.setReason(SsbConstants.REASON_TYPE_NOT_NEW);
            return;
        }
        final LoanInfo info = loadLoanInfo(loanId);
        if (info == null) {
            row.setStatus(SsbConstants.STATUS_FAILED);
            row.setReason(SsbConstants.REASON_REFERENCE_NOT_FOUND);
            return;
        }
        if (info.status == SsbConstants.LOAN_STATUS_ACTIVE) {
            row.setStatus(SsbConstants.STATUS_AUTHORISED);
            row.setReason(SsbConstants.REASON_ALREADY_ACTIVE);
            return;
        }
        if (info.status != SsbConstants.LOAN_STATUS_APPROVED) {
            row.setStatus(SsbConstants.STATUS_FAILED);
            row.setReason(SsbConstants.REASON_LOAN_NOT_APPROVED);
            return;
        }
        if (!autoDisburse) {
            row.setStatus(SsbConstants.STATUS_AUTHORISED);
            row.setReason(SsbConstants.REASON_AUTO_DISBURSE_DISABLED);
            return;
        }
        if (info.principal == null || info.principal.compareTo(BigDecimal.ZERO) <= 0) {
            row.setStatus(SsbConstants.STATUS_FAILED);
            row.setReason(SsbConstants.REASON_INVALID_AMOUNT);
            return;
        }
        if (dryRun) {
            row.setStatus(SsbConstants.STATUS_DISBURSED);
            row.setReason("DRY_RUN");
            return;
        }
        try {
            disburseLoan(row, loanId, info.principal, paymentTypeId);
            row.setStatus(SsbConstants.STATUS_DISBURSED);
            row.setReason(null);
        } catch (final RuntimeException ex) {
            row.setStatus(SsbConstants.STATUS_FAILED);
            row.setReason(SsbImportSupport.trimReason(ex.getMessage(), SsbConstants.REASON_DISBURSE_FAILED));
        }
    }

    private void handleFailed(final SsbResRowData row, final Long loanId, final String bureau, final boolean dryRun) {
        row.setLoanId(loanId);
        final String noteText = buildRejectionNote(row, bureau);
        row.setNote(noteText);
        if (dryRun) {
            row.setStatus(SsbConstants.STATUS_NOTED);
            row.setReason("DRY_RUN");
            return;
        }
        try {
            row.setNoteId(createLoanNote(loanId, noteText));
            this.mandateService.applyResFailure(loanId, bureau, row.getType());
            row.setStatus(SsbConstants.STATUS_NOTED);
            row.setReason(SsbConstants.REASON_BUREAU_REJECTED);
        } catch (final RuntimeException ex) {
            row.setStatus(SsbConstants.STATUS_FAILED);
            row.setReason(SsbImportSupport.trimReason(ex.getMessage(), "NOTE_FAILED"));
        }
    }

    private void disburseLoan(final SsbResRowData row, final Long loanId, final BigDecimal principal, final Long paymentTypeId) {
        final SimpleDateFormat df = new SimpleDateFormat(SsbConstants.DATE_FORMAT_API, Locale.ENGLISH);
        final JsonObject json = new JsonObject();
        json.addProperty("locale", SsbConstants.LOCALE);
        json.addProperty("dateFormat", SsbConstants.DATE_FORMAT_API);
        json.addProperty("actualDisbursementDate", df.format(DateUtils.getDateOfTenant()));
        json.addProperty("transactionAmount", principal);
        json.addProperty("note", SsbImportSupport.trimTo(row.getNote(), 1000));
        if (paymentTypeId != null) {
            json.addProperty("paymentTypeId", paymentTypeId);
        }

        final PortfolioAccountData linked = this.accountAssociationsReadPlatformService.retriveLoanLinkedAssociation(loanId);
        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(json.toString());
        final CommandWrapper commandRequest;
        if (linked != null && linked.accountId() != null) {
            commandRequest = builder.disburseLoanToSavingsApplication(loanId).build();
        } else {
            commandRequest = builder.disburseLoanApplication(loanId).build();
        }
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        if (result.getTransactionId() != null) {
            try {
                row.setDisbursementTransactionId(Long.valueOf(result.getTransactionId()));
            } catch (final NumberFormatException ignored) {
                // leave null
            }
        } else if (result.resourceId() != null) {
            row.setDisbursementTransactionId(result.resourceId());
        }
    }

    private Long createLoanNote(final Long loanId, final String noteText) {
        final JsonObject json = new JsonObject();
        json.addProperty("note", SsbImportSupport.trimTo(noteText, 1000));
        final CommandWrapper resourceDetails = new CommandWrapperBuilder().withLoanId(loanId).withEntityName("LOANNOTE").build();
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createNote(resourceDetails, "loans", loanId)
                .withJson(json.toString()).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return result.resourceId();
    }

    private List<SsbResRowData> parseResWorkbook(final Workbook workbook) {
        final Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
        if (sheet == null) {
            throw new PlatformDataIntegrityException("error.msg.ssb.res.sheet.missing", "RES workbook has no sheets.", "file");
        }
        final Row header = sheet.getRow(0);
        if (header == null) {
            throw new PlatformDataIntegrityException("error.msg.ssb.res.header.missing", "RES workbook header row is missing.", "file");
        }
        final Map<String, Integer> cols = this.workbookHelper.mapHeaders(header);
        final boolean pensionLayout = SsbWorkbookHelper.hasAny(cols, "processed", "reason rejection", "ref no");
        if (pensionLayout) {
            if (!SsbWorkbookHelper.hasAny(cols, "processed", "status")) {
                throw new PlatformDataIntegrityException("error.msg.ssb.res.column.missing",
                        "Missing required RES column: processed", "file", "processed");
            }
        } else {
            SsbWorkbookHelper.requireColumn(cols, "status", "res");
        }

        final List<SsbResRowData> rows = new ArrayList<>();
        final int last = sheet.getLastRowNum();
        for (int i = 1; i <= last; i++) {
            final Row excelRow = sheet.getRow(i);
            if (excelRow == null || this.workbookHelper.isEmptyRow(excelRow)) {
                continue;
            }
            final SsbResRowData row = new SsbResRowData();
            row.setRowNumber(i + 1);
            if (pensionLayout) {
                row.setReference(this.workbookHelper.cellString(excelRow, SsbWorkbookHelper.firstPresent(cols, "ref no", "reference")));
                row.setIdNumber(this.workbookHelper.cellString(excelRow, SsbWorkbookHelper.firstPresent(cols, "id number", "idnumber")));
                row.setType(this.workbookHelper.cellString(excelRow, SsbWorkbookHelper.firstPresent(cols, "trans type", "type")));
                row.setBureauStatus(this.workbookHelper.cellString(excelRow, SsbWorkbookHelper.firstPresent(cols, "processed", "status")));
                row.setStartDate(this.workbookHelper.cellDate(excelRow, SsbWorkbookHelper.firstPresent(cols, "start date")));
                row.setEndDate(this.workbookHelper.cellDate(excelRow, SsbWorkbookHelper.firstPresent(cols, "to date", "end date")));
                row.setAmount(this.workbookHelper.cellDecimal(excelRow, cols.get("amount")));
                row.setMessage(this.workbookHelper.cellString(excelRow, SsbWorkbookHelper.firstPresent(cols, "reason rejection", "message")));
                row.setName(joinName(this.workbookHelper.cellString(excelRow, cols.get("surname")),
                        this.workbookHelper.cellString(excelRow, SsbWorkbookHelper.firstPresent(cols, "first names", "first name"))));
            } else {
                row.setRecId(this.workbookHelper.cellString(excelRow, cols.get("rec id")));
                row.setDeductionCode(this.workbookHelper.cellString(excelRow, cols.get("deduction code")));
                row.setReference(this.workbookHelper.cellString(excelRow, cols.get("reference")));
                row.setIdNumber(this.workbookHelper.cellString(excelRow, SsbWorkbookHelper.firstPresent(cols, "id number", "idnumber")));
                row.setEcNumber(this.workbookHelper.cellString(excelRow, SsbWorkbookHelper.firstPresent(cols, "ec number", "ecnumber")));
                row.setType(this.workbookHelper.cellString(excelRow, cols.get("type")));
                row.setBureauStatus(this.workbookHelper.cellString(excelRow, cols.get("status")));
                row.setStartDate(this.workbookHelper.cellDate(excelRow, SsbWorkbookHelper.firstPresent(cols, "start date")));
                row.setEndDate(this.workbookHelper.cellDate(excelRow, SsbWorkbookHelper.firstPresent(cols, "end date")));
                row.setAmount(this.workbookHelper.cellDecimal(excelRow, cols.get("amount")));
                row.setName(this.workbookHelper.cellString(excelRow, cols.get("name")));
                row.setMessage(this.workbookHelper.cellString(excelRow, cols.get("message")));
            }
            rows.add(row);
        }
        return rows;
    }

    private boolean alreadyProcessed(final String recId) {
        final Integer rowCount = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM m_ssb_res_import_row r JOIN m_ssb_res_import_batch b ON b.id = r.batch_id "
                        + "WHERE r.rec_id = ? AND r.status IN (?, ?, ?) AND b.dry_run = 0",
                Integer.class, recId, SsbConstants.STATUS_DISBURSED, SsbConstants.STATUS_NOTED, SsbConstants.STATUS_AUTHORISED);
        return rowCount != null && rowCount > 0;
    }

    private boolean isAutoDisburseEnabled() {
        try {
            final Integer enabled = this.jdbcTemplate.queryForObject(
                    "SELECT enabled FROM c_configuration WHERE name = ?", Integer.class, SsbConstants.CONFIG_AUTO_DISBURSE);
            return enabled == null || enabled.intValue() != 0;
        } catch (final EmptyResultDataAccessException ex) {
            return true;
        }
    }

    private LoanInfo loadLoanInfo(final Long loanId) {
        try {
            return this.jdbcTemplate.queryForObject(
                    "SELECT loan_status_id, COALESCE(approved_principal, principal_amount) AS principal FROM m_loan WHERE id = ?",
                    (rs, rowNum) -> {
                        final LoanInfo info = new LoanInfo();
                        info.status = rs.getInt("loan_status_id");
                        info.principal = rs.getBigDecimal("principal");
                        return info;
                    }, loanId);
        } catch (final EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private Long defaultPaymentTypeId() {
        try {
            return this.jdbcTemplate.queryForObject("SELECT id FROM m_payment_type ORDER BY id ASC LIMIT 1", Long.class);
        } catch (final EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private Long insertBatch(final String bureau, final String filename, final Long userId, final boolean dryRun,
            final boolean autoDisburse, final Long paymentTypeId) {
        final KeyHolder keyHolder = new GeneratedKeyHolder();
        this.jdbcTemplate.update(connection -> {
            final java.sql.PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO m_ssb_res_import_batch (bureau, filename, uploaded_by, uploaded_on, dry_run, auto_disburse, "
                            + "payment_type_id, disbursed_count, authorised_count, noted_count, failed_count, needs_review_count, "
                            + "total_count) VALUES (?,?,?,?,?,?,?,0,0,0,0,0,0)",
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
            ps.setInt(6, autoDisburse ? 1 : 0);
            if (paymentTypeId == null) {
                ps.setNull(7, java.sql.Types.BIGINT);
            } else {
                ps.setLong(7, paymentTypeId);
            }
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private void insertRow(final Long batchId, final SsbResRowData row) {
        this.jdbcTemplate.update(
                "INSERT INTO m_ssb_res_import_row (batch_id, row_number, rec_id, deduction_code, reference, id_number, ec_number, "
                        + "type, bureau_status, start_date, end_date, amount, name, message, status, reason, suggested_loan_id, "
                        + "suggested_account_no, loan_id, disbursement_transaction_id, note_id, note) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                batchId, row.getRowNumber(), row.getRecId(), row.getDeductionCode(), row.getReference(), row.getIdNumber(),
                row.getEcNumber(), row.getType(), row.getBureauStatus(),
                row.getStartDate() == null ? null : new java.sql.Date(row.getStartDate().getTime()),
                row.getEndDate() == null ? null : new java.sql.Date(row.getEndDate().getTime()), row.getAmount(), row.getName(),
                SsbImportSupport.trimTo(row.getMessage(), 500), row.getStatus(), row.getReason(), row.getSuggestedLoanId(),
                row.getSuggestedAccountNo(), row.getLoanId(), row.getDisbursementTransactionId(), row.getNoteId(),
                SsbImportSupport.trimTo(row.getNote(), 1000));
    }

    private void updateBatchCounts(final Long batchId, final int disbursed, final int authorised, final int noted, final int failed,
            final int needsReview, final int total) {
        this.jdbcTemplate.update(
                "UPDATE m_ssb_res_import_batch SET disbursed_count = ?, authorised_count = ?, noted_count = ?, failed_count = ?, "
                        + "needs_review_count = ?, total_count = ? WHERE id = ?",
                disbursed, authorised, noted, failed, needsReview, total, batchId);
    }

    private void refreshBatchCounts(final Long batchId) {
        this.jdbcTemplate.update(
                "UPDATE m_ssb_res_import_batch b SET "
                        + "disbursed_count = (SELECT COUNT(*) FROM m_ssb_res_import_row r WHERE r.batch_id = b.id AND r.status = ?), "
                        + "authorised_count = (SELECT COUNT(*) FROM m_ssb_res_import_row r WHERE r.batch_id = b.id AND r.status = ?), "
                        + "noted_count = (SELECT COUNT(*) FROM m_ssb_res_import_row r WHERE r.batch_id = b.id AND r.status = ?), "
                        + "failed_count = (SELECT COUNT(*) FROM m_ssb_res_import_row r WHERE r.batch_id = b.id AND r.status IN (?, ?)), "
                        + "needs_review_count = (SELECT COUNT(*) FROM m_ssb_res_import_row r WHERE r.batch_id = b.id AND r.status = ?), "
                        + "total_count = (SELECT COUNT(*) FROM m_ssb_res_import_row r WHERE r.batch_id = b.id) WHERE b.id = ?",
                SsbConstants.STATUS_DISBURSED, SsbConstants.STATUS_AUTHORISED, SsbConstants.STATUS_NOTED, SsbConstants.STATUS_FAILED,
                SsbConstants.STATUS_REJECTED, SsbConstants.STATUS_NEEDS_REVIEW, batchId);
    }

    private SsbResRowData loadRow(final Long batchId, final Long rowId) {
        try {
            return this.jdbcTemplate.queryForObject(ROW_SELECT + " WHERE id = ? AND batch_id = ?", new ResRowMapper(), rowId, batchId);
        } catch (final EmptyResultDataAccessException ex) {
            throw new PlatformDataIntegrityException("error.msg.ssb.res.row.not.found", "RES import row not found.", "rowId", rowId);
        }
    }

    private void persistRowUpdate(final SsbResRowData row) {
        this.jdbcTemplate.update(
                "UPDATE m_ssb_res_import_row SET status = ?, reason = ?, suggested_loan_id = ?, suggested_account_no = ?, "
                        + "loan_id = ?, disbursement_transaction_id = ?, note_id = ?, note = ? WHERE id = ?",
                row.getStatus(), row.getReason(), row.getSuggestedLoanId(), row.getSuggestedAccountNo(), row.getLoanId(),
                row.getDisbursementTransactionId(), row.getNoteId(), SsbImportSupport.trimTo(row.getNote(), 1000), row.getId());
    }

    private static String buildRejectionNote(final SsbResRowData row, final String bureau) {
        final StringBuilder sb = new StringBuilder();
        sb.append(bureau).append(" REJECTED");
        if (StringUtils.hasText(row.getMessage())) {
            sb.append(": ").append(row.getMessage());
        } else {
            sb.append(" (no message)");
        }
        appendField(sb, "Rec id", row.getRecId());
        appendField(sb, "Reference", row.getReference());
        appendField(sb, "Type", row.getType());
        return SsbImportSupport.trimTo(sb.toString(), 1000);
    }

    private static String buildAuditNote(final SsbResRowData row, final String bureau, final String outcome) {
        final StringBuilder sb = new StringBuilder();
        sb.append(bureau).append(" ").append(outcome);
        appendField(sb, "Rec id", row.getRecId());
        appendField(sb, "Reference", row.getReference());
        appendField(sb, "Type", row.getType());
        appendField(sb, "Bureau status", row.getBureauStatus());
        return SsbImportSupport.trimTo(sb.toString(), 1000);
    }

    private static void appendField(final StringBuilder sb, final String label, final String value) {
        if (StringUtils.hasText(value)) {
            sb.append("; ").append(label).append("=").append(value);
        }
    }

    private static String joinName(final String surname, final String firstNames) {
        if (!StringUtils.hasText(surname) && !StringUtils.hasText(firstNames)) {
            return null;
        }
        if (!StringUtils.hasText(surname)) {
            return firstNames;
        }
        if (!StringUtils.hasText(firstNames)) {
            return surname;
        }
        return surname + " " + firstNames;
    }

    private static final class LoanInfo {
        int status;
        BigDecimal principal;
    }

    private static final class BatchMapper implements RowMapper<SsbResBatchData> {
        @Override
        public SsbResBatchData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final SsbResBatchData data = new SsbResBatchData();
            data.setId(rs.getLong("id"));
            data.setBureau(rs.getString("bureau"));
            data.setFilename(rs.getString("filename"));
            final long uploadedBy = rs.getLong("uploaded_by");
            data.setUploadedBy(rs.wasNull() ? null : uploadedBy);
            data.setUploadedOn(rs.getTimestamp("uploaded_on"));
            data.setDryRun(rs.getInt("dry_run") == 1);
            data.setAutoDisburse(rs.getInt("auto_disburse") == 1);
            final long paymentTypeId = rs.getLong("payment_type_id");
            data.setPaymentTypeId(rs.wasNull() ? null : paymentTypeId);
            data.setDisbursedCount(rs.getInt("disbursed_count"));
            data.setAuthorisedCount(rs.getInt("authorised_count"));
            data.setNotedCount(rs.getInt("noted_count"));
            data.setFailedCount(rs.getInt("failed_count"));
            data.setNeedsReviewCount(rs.getInt("needs_review_count"));
            data.setTotalCount(rs.getInt("total_count"));
            return data;
        }
    }

    private static final class ResRowMapper implements RowMapper<SsbResRowData> {
        @Override
        public SsbResRowData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final SsbResRowData row = new SsbResRowData();
            row.setId(rs.getLong("id"));
            row.setBatchId(rs.getLong("batch_id"));
            row.setRowNumber(rs.getInt("row_number"));
            row.setRecId(rs.getString("rec_id"));
            row.setDeductionCode(rs.getString("deduction_code"));
            row.setReference(rs.getString("reference"));
            row.setIdNumber(rs.getString("id_number"));
            row.setEcNumber(rs.getString("ec_number"));
            row.setType(rs.getString("type"));
            row.setBureauStatus(rs.getString("bureau_status"));
            row.setStartDate(rs.getDate("start_date"));
            row.setEndDate(rs.getDate("end_date"));
            row.setAmount(rs.getBigDecimal("amount"));
            row.setName(rs.getString("name"));
            row.setMessage(rs.getString("message"));
            row.setStatus(rs.getString("status"));
            row.setReason(rs.getString("reason"));
            final long suggestedLoanId = rs.getLong("suggested_loan_id");
            row.setSuggestedLoanId(rs.wasNull() ? null : suggestedLoanId);
            row.setSuggestedAccountNo(rs.getString("suggested_account_no"));
            final long loanId = rs.getLong("loan_id");
            row.setLoanId(rs.wasNull() ? null : loanId);
            final long txnId = rs.getLong("disbursement_transaction_id");
            row.setDisbursementTransactionId(rs.wasNull() ? null : txnId);
            final long noteId = rs.getLong("note_id");
            row.setNoteId(rs.wasNull() ? null : noteId);
            row.setNote(rs.getString("note"));
            return row;
        }
    }
}
