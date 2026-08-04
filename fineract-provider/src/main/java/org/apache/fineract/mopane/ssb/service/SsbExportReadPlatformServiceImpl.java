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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.mopane.ssb.data.SsbConstants;
import org.apache.fineract.mopane.ssb.data.SsbExportResultData;
import org.apache.fineract.mopane.ssb.data.SsbExportRowData;
import org.apache.fineract.mopane.ssb.service.SsbMandateService.MandateSnapshot;
import org.apache.fineract.infrastructure.security.exception.NoAuthorizationException;
import org.apache.fineract.organisation.office.exception.OfficeNotFoundException;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SsbExportReadPlatformServiceImpl implements SsbExportReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final SsbMandateService mandateService;

    @Autowired
    public SsbExportReadPlatformServiceImpl(final RoutingDataSource dataSource, final SsbMandateService mandateService) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.mandateService = mandateService;
    }

    @Override
    public SsbExportResultData gather(final String bureau, final Long loanProductId, final Long officeId, final Integer loanStatusId,
            final boolean includeUnchanged, final Date asOfDate, final AppUser user) {

        final String normalizedBureau = normalizeBureau(bureau);
        if (loanProductId == null) {
            throw new PlatformDataIntegrityException("error.msg.ssb.export.product.required", "loanProductId is required.",
                    "loanProductId");
        }

        final OfficeContext office = resolveOffice(officeId, user);
        final boolean hasClientDetails = tableExists(SsbConstants.DT_CLIENT_DETAILS);
        final SsbExportResultData result = new SsbExportResultData();
        result.setBureau(normalizedBureau);

        final List<SsbExportRowData> candidates = new ArrayList<>();
        candidates.addAll(loadApprovedOrActive(normalizedBureau, loanProductId, office.hierarchyLike, loanStatusId, hasClientDetails));
        candidates.addAll(loadDeletes(normalizedBureau, loanProductId, office.hierarchyLike, hasClientDetails));

        final Set<Long> seen = new HashSet<>();
        for (final SsbExportRowData row : candidates) {
            if (row.getLoanId() == null || !seen.add(row.getLoanId())) {
                continue;
            }
            enrichInstallmentAndType(row, normalizedBureau, includeUnchanged, asOfDate);
            if (row.isSkipped()) {
                result.getSkipped().add(row);
            } else {
                result.getRows().add(row);
            }
        }
        return result;
    }

    private List<SsbExportRowData> loadApprovedOrActive(final String bureau, final Long loanProductId, final String hierarchyLike,
            final Integer loanStatusId, final boolean hasClientDetails) {
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT l.id AS loan_id, l.account_no, l.client_id, l.loan_status_id, ");
        sql.append("COALESCE(l.disbursedon_date, l.expected_disbursedon_date) AS start_date, ");
        sql.append("l.expected_maturedon_date AS end_date, ");
        sql.append("c.firstname, c.lastname, ");
        if (hasClientDetails) {
            sql.append("d.IdNumber AS id_number, d.EcNumber AS ec_number ");
        } else {
            sql.append("NULL AS id_number, NULL AS ec_number ");
        }
        sql.append("FROM m_loan l ");
        sql.append("JOIN m_client c ON c.id = l.client_id ");
        sql.append("JOIN m_office o ON o.id = c.office_id ");
        if (hasClientDetails) {
            sql.append("LEFT JOIN ").append(SsbConstants.DT_CLIENT_DETAILS).append(" d ON d.client_id = c.id ");
        }
        sql.append("WHERE l.product_id = ? AND o.hierarchy LIKE ? ");
        final List<Object> args = new ArrayList<>();
        args.add(loanProductId);
        args.add(hierarchyLike);
        if (loanStatusId != null) {
            sql.append("AND l.loan_status_id = ? ");
            args.add(loanStatusId);
        } else {
            sql.append("AND l.loan_status_id IN (?, ?) ");
            args.add(SsbConstants.LOAN_STATUS_APPROVED);
            args.add(SsbConstants.LOAN_STATUS_ACTIVE);
        }
        sql.append("ORDER BY l.id");

        return this.jdbcTemplate.query(sql.toString(), new LoanRowMapper(), args.toArray());
    }

    private List<SsbExportRowData> loadDeletes(final String bureau, final Long loanProductId, final String hierarchyLike,
            final boolean hasClientDetails) {
        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT l.id AS loan_id, l.account_no, l.client_id, l.loan_status_id, ");
        sql.append("COALESCE(l.disbursedon_date, l.expected_disbursedon_date) AS start_date, ");
        sql.append("l.expected_maturedon_date AS end_date, ");
        sql.append("c.firstname, c.lastname, ");
        if (hasClientDetails) {
            sql.append("d.IdNumber AS id_number, d.EcNumber AS ec_number ");
        } else {
            sql.append("NULL AS id_number, NULL AS ec_number ");
        }
        sql.append("FROM m_ssb_mandate m ");
        sql.append("JOIN m_loan l ON l.id = m.loan_id ");
        sql.append("JOIN m_client c ON c.id = l.client_id ");
        sql.append("JOIN m_office o ON o.id = c.office_id ");
        if (hasClientDetails) {
            sql.append("LEFT JOIN ").append(SsbConstants.DT_CLIENT_DETAILS).append(" d ON d.client_id = c.id ");
        }
        sql.append("WHERE m.bureau = ? AND m.active = 1 AND l.product_id = ? AND o.hierarchy LIKE ? ");
        sql.append("AND l.loan_status_id NOT IN (?, ?) ");
        sql.append("ORDER BY l.id");

        return this.jdbcTemplate.query(sql.toString(), new LoanRowMapper(), bureau, loanProductId, hierarchyLike,
                SsbConstants.LOAN_STATUS_APPROVED, SsbConstants.LOAN_STATUS_ACTIVE);
    }

    private void enrichInstallmentAndType(final SsbExportRowData row, final String bureau, final boolean includeUnchanged,
            final Date asOfDate) {
        if (!StringUtils.hasText(row.getIdNumber())) {
            markSkipped(row, "MISSING_ID_NUMBER");
            return;
        }
        if (SsbConstants.BUREAU_SSB.equals(bureau) && !StringUtils.hasText(row.getEcNumber())) {
            markSkipped(row, "MISSING_EC_NUMBER");
            return;
        }
        if (row.getStartDate() == null || row.getEndDate() == null) {
            markSkipped(row, "MISSING_DATES");
            return;
        }

        final Long amountCents = resolveInstallmentCents(row.getLoanId());
        if (amountCents == null || amountCents <= 0) {
            markSkipped(row, "MISSING_INSTALLMENT");
            return;
        }
        row.setAmountCents(amountCents);

        final MandateSnapshot mandate = this.mandateService.findActive(row.getLoanId(), bureau);
        final boolean isApprovedOrActive = row.getLoanStatusId() != null
                && (row.getLoanStatusId() == SsbConstants.LOAN_STATUS_APPROVED
                        || row.getLoanStatusId() == SsbConstants.LOAN_STATUS_ACTIVE);

        if (!isApprovedOrActive && mandate != null && mandate.active) {
            row.setType(SsbConstants.TYPE_DELETE);
            return;
        }

        if (mandate == null || !mandate.active) {
            row.setType(SsbConstants.TYPE_NEW);
            return;
        }

        final boolean amountChanged = mandate.lastAmountCents == null || !mandate.lastAmountCents.equals(amountCents);
        final boolean endChanged = mandate.lastEndDate == null || !sameDay(mandate.lastEndDate, row.getEndDate());
        if (amountChanged || endChanged) {
            row.setType(SsbConstants.TYPE_CHANGE);
            return;
        }

        if (includeUnchanged) {
            row.setType(SsbConstants.TYPE_CHANGE);
        } else {
            markSkipped(row, "UNCHANGED");
        }
    }

    private Long resolveInstallmentCents(final Long loanId) {
        try {
            final BigDecimal amount = this.jdbcTemplate.queryForObject(
                    "SELECT (COALESCE(ls.principal_amount,0) + COALESCE(ls.interest_amount,0) "
                            + "+ COALESCE(ls.fee_charges_amount,0) + COALESCE(ls.penalty_charges_amount,0)) AS installment "
                            + "FROM m_loan_repayment_schedule ls "
                            + "WHERE ls.loan_id = ? AND COALESCE(ls.completed_derived,0) = 0 "
                            + "ORDER BY ls.installment ASC LIMIT 1",
                    BigDecimal.class, loanId);
            if (amount == null) {
                return null;
            }
            return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
        } catch (final EmptyResultDataAccessException ex) {
            try {
                final BigDecimal amount = this.jdbcTemplate.queryForObject(
                        "SELECT (COALESCE(ls.principal_amount,0) + COALESCE(ls.interest_amount,0) "
                                + "+ COALESCE(ls.fee_charges_amount,0) + COALESCE(ls.penalty_charges_amount,0)) AS installment "
                                + "FROM m_loan_repayment_schedule ls WHERE ls.loan_id = ? ORDER BY ls.installment ASC LIMIT 1",
                        BigDecimal.class, loanId);
                if (amount == null) {
                    return null;
                }
                return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
            } catch (final EmptyResultDataAccessException ex2) {
                return null;
            }
        }
    }

    private static void markSkipped(final SsbExportRowData row, final String reason) {
        row.setSkipped(true);
        row.setSkipReason(reason);
    }

    private static boolean sameDay(final Date a, final Date b) {
        if (a == null || b == null) {
            return false;
        }
        return a.getTime() / 86400000L == b.getTime() / 86400000L;
    }

    private static String normalizeBureau(final String bureau) {
        if (!StringUtils.hasText(bureau)) {
            throw new PlatformDataIntegrityException("error.msg.ssb.export.bureau.required", "bureau is required (SSB or PENSION).",
                    "bureau");
        }
        final String value = bureau.trim().toUpperCase();
        if (SsbConstants.BUREAU_SSB.equals(value) || SsbConstants.BUREAU_PENSION.equals(value)) {
            return value;
        }
        throw new PlatformDataIntegrityException("error.msg.ssb.export.bureau.invalid",
                "bureau must be SSB or PENSION.", "bureau", bureau);
    }

    private OfficeContext resolveOffice(final Long officeIdParam, final AppUser user) {
        final String userHierarchy = user.getOffice().getHierarchy();
        final Long requestedId = officeIdParam != null ? officeIdParam : user.getOffice().getId();
        try {
            final Map<String, Object> row = this.jdbcTemplate.queryForMap(
                    "SELECT id, name, hierarchy FROM m_office WHERE id = ? AND hierarchy LIKE ?", requestedId, userHierarchy + "%");
            final OfficeContext ctx = new OfficeContext();
            ctx.id = ((Number) row.get("id")).longValue();
            ctx.hierarchy = (String) row.get("hierarchy");
            ctx.hierarchyLike = ctx.hierarchy + "%";
            return ctx;
        } catch (final EmptyResultDataAccessException e) {
            final Integer exists = this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM m_office WHERE id = ?", Integer.class,
                    requestedId);
            if (exists == null || exists == 0) {
                throw new OfficeNotFoundException(requestedId);
            }
            throw new NoAuthorizationException("User does not have sufficient privileges to act on the provided office.");
        }
    }

    private boolean tableExists(final String tableName) {
        final Integer count = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?", Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private static final class LoanRowMapper implements RowMapper<SsbExportRowData> {
        @Override
        public SsbExportRowData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final SsbExportRowData row = new SsbExportRowData();
            row.setLoanId(rs.getLong("loan_id"));
            row.setAccountNo(rs.getString("account_no"));
            row.setClientId(rs.getLong("client_id"));
            row.setLoanStatusId(rs.getInt("loan_status_id"));
            row.setStartDate(rs.getDate("start_date"));
            row.setEndDate(rs.getDate("end_date"));
            row.setFirstName(rs.getString("firstname"));
            row.setSurname(rs.getString("lastname"));
            row.setIdNumber(rs.getString("id_number"));
            row.setEcNumber(rs.getString("ec_number"));
            return row;
        }
    }

    private static final class OfficeContext {
        Long id;
        String hierarchy;
        String hierarchyLike;
    }
}
