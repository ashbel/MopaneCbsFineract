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
package org.apache.fineract.mopane.rbz.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1Constants;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1ReportData;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1ReportData.InsiderLoanRow;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1ReportData.InstitutionProfileRow;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1ReportData.LongTermDebtRow;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1ReportData.OfficeChannelRow;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1ReportData.PortfolioManagementRow;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1ReportData.PurposeRow;
import org.apache.fineract.mopane.rbz.data.RbzFormMfi1ReportData.TopBorrowerRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class RbzFormMfi1ReadPlatformServiceImpl implements RbzFormMfi1ReadPlatformService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public RbzFormMfi1ReadPlatformServiceImpl(final RoutingDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public RbzFormMfi1ReportData gather(final Date startDate, final Date endDate, final Long officeId) {
        final RbzFormMfi1ReportData data = new RbzFormMfi1ReportData();
        data.setStartDate(startDate);
        data.setEndDate(endDate);
        data.setOfficeId(officeId);

        final OfficeContext office = resolveOffice(officeId);
        data.setInstitutionName(office.name);
        if (endDate != null) {
            final Calendar cal = Calendar.getInstance();
            cal.setTime(endDate);
            data.setFinancialYear(String.valueOf(cal.get(Calendar.YEAR)));
        }

        final boolean hasLoanDetails = tableExists(RbzFormMfi1Constants.DT_LOAN_DETAILS);
        loadOutstandingByClass(data, office.hierarchy, hasLoanDetails);
        loadIncomeByClass(data, office.hierarchy, startDate, endDate, hasLoanDetails);
        loadAssetQuality(data, office.hierarchy, endDate, hasLoanDetails);
        loadPurposeDistribution(data, office.hierarchy);
        loadTopBorrowers(data, office.hierarchy);
        loadMaturity(data, office.hierarchy, endDate);
        loadPortfolio(data, office.hierarchy, startDate, endDate, hasLoanDetails);
        loadProfile(data, office);
        if (hasLoanDetails) {
            loadInsiderLoans(data, office.hierarchy);
        }
        if (tableExists(RbzFormMfi1Constants.DT_LONG_TERM_DEBT)) {
            loadLongTermDebt(data, office.id);
            loadLiabilityMaturity(data, office.id, endDate);
        }
        if (tableExists(RbzFormMfi1Constants.DT_OFFICE_CHANNELS)) {
            loadOfficeChannels(data, office.hierarchy);
        }
        return data;
    }

    private OfficeContext resolveOffice(final Long officeId) {
        final OfficeContext ctx = new OfficeContext();
        if (officeId == null) {
            final List<Map<String, Object>> rows = this.jdbcTemplate
                    .queryForList("SELECT id, name, hierarchy FROM m_office WHERE parent_id IS NULL ORDER BY id LIMIT 1");
            if (!rows.isEmpty()) {
                ctx.id = ((Number) rows.get(0).get("id")).longValue();
                ctx.name = (String) rows.get(0).get("name");
                ctx.hierarchy = (String) rows.get(0).get("hierarchy");
            } else {
                ctx.id = 1L;
                ctx.name = "Institution";
                ctx.hierarchy = ".";
            }
        } else {
            final Map<String, Object> row = this.jdbcTemplate.queryForMap("SELECT id, name, hierarchy FROM m_office WHERE id = ?", officeId);
            ctx.id = ((Number) row.get("id")).longValue();
            ctx.name = (String) row.get("name");
            ctx.hierarchy = (String) row.get("hierarchy");
        }
        return ctx;
    }

    private void loadOutstandingByClass(final RbzFormMfi1ReportData data, final String hierarchy, final boolean hasLoanDetails) {
        final String sql = "SELECT " + loanClassExpr(hasLoanDetails) + " AS loan_class, "
                + "COALESCE(SUM(l.principal_outstanding_derived),0) AS outstanding "
                + "FROM m_loan l " + "JOIN m_client c ON c.id = l.client_id " + "JOIN m_office o ON o.id = c.office_id "
                + loanDetailsJoin(hasLoanDetails) + "WHERE l.loan_status_id = 300 AND o.hierarchy LIKE ? "
                + "GROUP BY loan_class";
        final List<Map<String, Object>> rows = this.jdbcTemplate.queryForList(sql, hierarchy + "%");
        for (final Map<String, Object> row : rows) {
            data.getLoanClassOutstanding().put(normalizeLoanClass(String.valueOf(row.get("loan_class"))), toBd(row.get("outstanding")));
        }
    }

    private void loadIncomeByClass(final RbzFormMfi1ReportData data, final String hierarchy, final Date start, final Date end,
            final boolean hasLoanDetails) {
        final String interestSql = "SELECT " + loanClassExpr(hasLoanDetails) + " AS loan_class, "
                + "COALESCE(SUM(CASE WHEN tr.transaction_type_enum = 2 THEN tr.interest_portion_derived ELSE 0 END),0) AS interest_amt, "
                + "COALESCE(SUM(CASE WHEN tr.transaction_type_enum = 2 THEN tr.fee_charges_portion_derived ELSE 0 END),0) AS fee_amt, "
                + "COALESCE(SUM(CASE WHEN tr.transaction_type_enum = 2 THEN tr.penalty_charges_portion_derived ELSE 0 END),0) AS penalty_amt "
                + "FROM m_loan_transaction tr " + "JOIN m_loan l ON l.id = tr.loan_id " + "JOIN m_client c ON c.id = l.client_id "
                + "JOIN m_office o ON o.id = c.office_id " + loanDetailsJoin(hasLoanDetails)
                + "WHERE tr.is_reversed = 0 AND tr.transaction_date BETWEEN ? AND ? AND o.hierarchy LIKE ? "
                + "GROUP BY loan_class";
        BigDecimal penalty = BigDecimal.ZERO;
        BigDecimal fees = BigDecimal.ZERO;
        final List<Map<String, Object>> rows = this.jdbcTemplate.queryForList(interestSql, start, end, hierarchy + "%");
        for (final Map<String, Object> row : rows) {
            final String loanClass = normalizeLoanClass(String.valueOf(row.get("loan_class")));
            data.getLoanClassInterestIncome().put(loanClass,
                    data.getLoanClassInterestIncome().getOrDefault(loanClass, BigDecimal.ZERO).add(toBd(row.get("interest_amt"))));
            fees = fees.add(toBd(row.get("fee_amt")));
            penalty = penalty.add(toBd(row.get("penalty_amt")));
        }
        data.getFeeIncome().put("admin", fees);
        data.getFeeIncome().put("insurance", BigDecimal.ZERO);
        data.getFeeIncome().put("other", BigDecimal.ZERO);
        data.setPenaltyIncome(penalty);
    }

    private void loadAssetQuality(final RbzFormMfi1ReportData data, final String hierarchy, final Date asOf,
            final boolean hasLoanDetails) {
        for (final String band : new String[] { "CURRENT", "SPECIAL_MENTION", "SUBSTANDARD", "DOUBTFUL", "LOSS" }) {
            data.getAssetQuality().put(band, new HashMap<String, BigDecimal>());
        }
        final String sql = "SELECT " + loanClassExpr(hasLoanDetails) + " AS loan_class, "
                + "COALESCE(l.principal_outstanding_derived,0) AS outstanding, "
                + "CASE WHEN la.overdue_since_date_derived IS NULL THEN 0 "
                + "ELSE DATEDIFF(?, la.overdue_since_date_derived) END AS days_arrears "
                + "FROM m_loan l " + "JOIN m_client c ON c.id = l.client_id " + "JOIN m_office o ON o.id = c.office_id "
                + "LEFT JOIN m_loan_arrears_aging la ON la.loan_id = l.id " + loanDetailsJoin(hasLoanDetails)
                + "WHERE l.loan_status_id = 300 AND o.hierarchy LIKE ?";
        final List<Map<String, Object>> rows = this.jdbcTemplate.queryForList(sql, asOf, hierarchy + "%");
        for (final Map<String, Object> row : rows) {
            final String loanClass = normalizeLoanClass(String.valueOf(row.get("loan_class")));
            final BigDecimal outstanding = toBd(row.get("outstanding"));
            final int days = row.get("days_arrears") == null ? 0 : ((Number) row.get("days_arrears")).intValue();
            final String band = bandForDays(days);
            final Map<String, BigDecimal> byClass = data.getAssetQuality().get(band);
            byClass.put(loanClass, byClass.getOrDefault(loanClass, BigDecimal.ZERO).add(outstanding));
        }
    }

    private void loadPurposeDistribution(final RbzFormMfi1ReportData data, final String hierarchy) {
        for (final String purpose : RbzFormMfi1Constants.RBZ_PURPOSES) {
            data.getPurposeDistribution().put(normalize(purpose), new PurposeRow());
            data.getPurposeGender().put(normalize(purpose), new PurposeRow());
        }
        final String sql = "SELECT COALESCE(cv.code_value, 'Other') AS purpose, "
                + "COUNT(DISTINCT l.client_id) AS clients, COUNT(l.id) AS loans, "
                + "COALESCE(SUM(l.principal_outstanding_derived),0) AS value, "
                + "COUNT(DISTINCT CASE WHEN LOWER(g.code_value) LIKE 'f%' THEN l.client_id END) AS female_clients "
                + "FROM m_loan l " + "JOIN m_client c ON c.id = l.client_id " + "JOIN m_office o ON o.id = c.office_id "
                + "LEFT JOIN m_code_value cv ON cv.id = l.loanpurpose_cv_id "
                + "LEFT JOIN m_code_value g ON g.id = c.gender_cv_id "
                + "WHERE l.loan_status_id = 300 AND o.hierarchy LIKE ? " + "GROUP BY purpose";
        final List<Map<String, Object>> rows = this.jdbcTemplate.queryForList(sql, hierarchy + "%");
        for (final Map<String, Object> row : rows) {
            final String key = mapPurpose(String.valueOf(row.get("purpose")));
            PurposeRow dist = data.getPurposeDistribution().get(key);
            if (dist == null) {
                dist = data.getPurposeDistribution().get(normalize("Other"));
            }
            dist.numberOfClients += ((Number) row.get("clients")).longValue();
            dist.numberOfLoans += ((Number) row.get("loans")).longValue();
            dist.value = dist.value.add(toBd(row.get("value")));

            PurposeRow gender = data.getPurposeGender().get(key);
            if (gender == null) {
                gender = data.getPurposeGender().get(normalize("Other"));
            }
            gender.femaleClients += row.get("female_clients") == null ? 0 : ((Number) row.get("female_clients")).longValue();
            gender.numberOfLoans += ((Number) row.get("loans")).longValue();
            gender.value = gender.value.add(toBd(row.get("value")));
        }
    }

    private void loadTopBorrowers(final RbzFormMfi1ReportData data, final String hierarchy) {
        final String sql = "SELECT c.display_name AS name, COALESCE(l.principal_disbursed_derived, l.principal_amount, 0) AS borrowed, "
                + "COALESCE(l.principal_outstanding_derived,0) AS outstanding, l.expected_maturedon_date AS maturity, "
                + "(SELECT cv.code_value FROM m_loan_collateral lc JOIN m_code_value cv ON cv.id = lc.type_cv_id "
                + " WHERE lc.loan_id = l.id LIMIT 1) AS security_type, "
                + "(SELECT COALESCE(SUM(lc.value),0) FROM m_loan_collateral lc WHERE lc.loan_id = l.id) AS security_value "
                + "FROM m_loan l JOIN m_client c ON c.id = l.client_id JOIN m_office o ON o.id = c.office_id "
                + "WHERE l.loan_status_id = 300 AND o.hierarchy LIKE ? "
                + "ORDER BY outstanding DESC LIMIT 20";
        this.jdbcTemplate.query(sql, new Object[] { hierarchy + "%" }, new RowMapper<TopBorrowerRow>() {
            @Override
            public TopBorrowerRow mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                final TopBorrowerRow row = new TopBorrowerRow();
                row.name = rs.getString("name");
                row.amountBorrowed = rs.getBigDecimal("borrowed");
                row.outstanding = rs.getBigDecimal("outstanding");
                row.maturityDate = rs.getDate("maturity");
                row.securityType = rs.getString("security_type");
                row.securityValue = rs.getBigDecimal("security_value");
                data.getTopBorrowers().add(row);
                return row;
            }
        });
    }

    private void loadMaturity(final RbzFormMfi1ReportData data, final String hierarchy, final Date asOf) {
        for (final String bucket : new String[] { "0-7", "8-14", "15-30", "31-60", "61-90", "91-120", "121-180", "181-360", "360+" }) {
            data.getAssetMaturityInflows().put(bucket, BigDecimal.ZERO);
            data.getLiabilityMaturityOutflows().put(bucket, BigDecimal.ZERO);
        }
        final String sql = "SELECT COALESCE(l.principal_outstanding_derived,0) AS outstanding, "
                + "DATEDIFF(l.expected_maturedon_date, ?) AS days_to_maturity "
                + "FROM m_loan l JOIN m_client c ON c.id = l.client_id JOIN m_office o ON o.id = c.office_id "
                + "WHERE l.loan_status_id = 300 AND l.expected_maturedon_date IS NOT NULL AND o.hierarchy LIKE ?";
        final List<Map<String, Object>> rows = this.jdbcTemplate.queryForList(sql, asOf, hierarchy + "%");
        for (final Map<String, Object> row : rows) {
            final int days = row.get("days_to_maturity") == null ? 9999 : ((Number) row.get("days_to_maturity")).intValue();
            final String bucket = maturityBucket(Math.max(days, 0));
            data.getAssetMaturityInflows().put(bucket, data.getAssetMaturityInflows().get(bucket).add(toBd(row.get("outstanding"))));
        }
    }

    private void loadLiabilityMaturity(final RbzFormMfi1ReportData data, final Long officeId, final Date asOf) {
        try {
            final String sql = "SELECT COALESCE(outstanding_balance,0) AS outstanding, maturity_date "
                    + "FROM `" + RbzFormMfi1Constants.DT_LONG_TERM_DEBT + "` WHERE " + officeFkColumn() + " = ?";
            final List<Map<String, Object>> rows = this.jdbcTemplate.queryForList(sql, officeId);
            for (final Map<String, Object> row : rows) {
                final Date maturity = (Date) row.get("maturity_date");
                if (maturity == null) {
                    continue;
                }
                final long diff = (maturity.getTime() - asOf.getTime()) / (1000L * 60 * 60 * 24);
                final String bucket = maturityBucket((int) Math.max(diff, 0));
                data.getLiabilityMaturityOutflows().put(bucket,
                        data.getLiabilityMaturityOutflows().get(bucket).add(toBd(row.get("outstanding"))));
            }
        } catch (final DataAccessException ignored) {
            // datatable column names may differ slightly
        }
    }

    private void loadPortfolio(final RbzFormMfi1ReportData data, final String hierarchy, final Date start, final Date end,
            final boolean hasLoanDetails) {
        final PortfolioManagementRow p = data.getPortfolio();
        final Map<String, Object> disbursed = this.jdbcTemplate.queryForMap(
                "SELECT COUNT(l.id) AS cnt, COALESCE(SUM(l.principal_disbursed_derived),0) AS amt FROM m_loan l "
                        + "JOIN m_client c ON c.id = l.client_id JOIN m_office o ON o.id = c.office_id "
                        + "WHERE l.disbursedon_date BETWEEN ? AND ? AND o.hierarchy LIKE ?",
                start, end, hierarchy + "%");
        p.loansDisbursedCount = ((Number) disbursed.get("cnt")).longValue();
        p.loansDisbursedValue = toBd(disbursed.get("amt"));

        final Long firstLoan = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT l.client_id) FROM m_loan l JOIN m_client c ON c.id = l.client_id "
                        + "JOIN m_office o ON o.id = c.office_id "
                        + "WHERE l.disbursedon_date BETWEEN ? AND ? AND COALESCE(l.loan_counter,1) = 1 AND o.hierarchy LIKE ?",
                Long.class, start, end, hierarchy + "%");
        p.firstLoanClients = firstLoan == null ? 0 : firstLoan;

        final Long repeatLoan = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT l.client_id) FROM m_loan l JOIN m_client c ON c.id = l.client_id "
                        + "JOIN m_office o ON o.id = c.office_id "
                        + "WHERE l.disbursedon_date BETWEEN ? AND ? AND COALESCE(l.loan_counter,1) > 1 AND o.hierarchy LIKE ?",
                Long.class, start, end, hierarchy + "%");
        p.repeatLoanClients = repeatLoan == null ? 0 : repeatLoan;

        final Long activeEnd = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT l.client_id) FROM m_loan l JOIN m_client c ON c.id = l.client_id "
                        + "JOIN m_office o ON o.id = c.office_id WHERE l.loan_status_id = 300 AND o.hierarchy LIKE ?",
                Long.class, hierarchy + "%");
        p.activeClientsEnd = activeEnd == null ? 0 : activeEnd;
        p.activeClientsStart = p.activeClientsEnd; // approximate without historical snapshot

        final Long outstandingLoans = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(l.id) FROM m_loan l JOIN m_client c ON c.id = l.client_id JOIN m_office o ON o.id = c.office_id "
                        + "WHERE l.loan_status_id = 300 AND o.hierarchy LIKE ?",
                Long.class, hierarchy + "%");
        p.outstandingLoans = outstandingLoans == null ? 0 : outstandingLoans;

        final BigDecimal avgTerm = this.jdbcTemplate.queryForObject(
                "SELECT COALESCE(AVG(l.term_frequency),0) FROM m_loan l JOIN m_client c ON c.id = l.client_id "
                        + "JOIN m_office o ON o.id = c.office_id WHERE l.loan_status_id = 300 AND o.hierarchy LIKE ?",
                BigDecimal.class, hierarchy + "%");
        p.averageLoanTermMonths = avgTerm == null ? BigDecimal.ZERO : avgTerm;

        final Long totalClients = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(c.id) FROM m_client c JOIN m_office o ON o.id = c.office_id WHERE o.hierarchy LIKE ?", Long.class,
                hierarchy + "%");
        p.totalClients = totalClients == null ? 0 : totalClients;

        final Long groups = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT l.group_id) FROM m_loan l JOIN m_client c ON c.id = l.client_id "
                        + "JOIN m_office o ON o.id = c.office_id WHERE l.loan_status_id = 300 AND l.group_id IS NOT NULL AND o.hierarchy LIKE ?",
                Long.class, hierarchy + "%");
        p.borrowingGroups = groups == null ? 0 : groups;

        final Long groupClients = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT l.client_id) FROM m_loan l JOIN m_client c ON c.id = l.client_id "
                        + "JOIN m_office o ON o.id = c.office_id WHERE l.loan_status_id = 300 AND l.group_id IS NOT NULL AND o.hierarchy LIKE ?",
                Long.class, hierarchy + "%");
        p.groupLoanClients = groupClients == null ? 0 : groupClients;

        final Long individual = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT l.client_id) FROM m_loan l JOIN m_client c ON c.id = l.client_id "
                        + "JOIN m_office o ON o.id = c.office_id WHERE l.loan_status_id = 300 AND l.group_id IS NULL AND o.hierarchy LIKE ?",
                Long.class, hierarchy + "%");
        p.individualLoanClients = individual == null ? 0 : individual;

        final BigDecimal writeOffs = this.jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(l.principal_writtenoff_derived),0) FROM m_loan l JOIN m_client c ON c.id = l.client_id "
                        + "JOIN m_office o ON o.id = c.office_id WHERE l.loan_status_id = 601 AND l.closedon_date BETWEEN ? AND ? "
                        + "AND o.hierarchy LIKE ?",
                BigDecimal.class, start, end, hierarchy + "%");
        p.writeOffs = writeOffs == null ? BigDecimal.ZERO : writeOffs;

        if (tableExists(RbzFormMfi1Constants.DT_OFFICE_CHANNELS)) {
            try {
                final List<Map<String, Object>> locRows = this.jdbcTemplate.queryForList(
                        "SELECT cv.code_value AS loc FROM `" + RbzFormMfi1Constants.DT_OFFICE_CHANNELS + "` ch "
                                + "JOIN m_office o ON o.id = ch." + officeFkColumn() + " "
                                + "LEFT JOIN m_code_value cv ON cv.id = ch.location_type_cd_RbzLocationType "
                                + "WHERE o.hierarchy LIKE ?",
                        hierarchy + "%");
                for (final Map<String, Object> r : locRows) {
                    final String loc = r.get("loc") == null ? "" : String.valueOf(r.get("loc"));
                    if ("Rural".equalsIgnoreCase(loc)) {
                        p.ruralBranches++;
                    } else {
                        p.urbanBranches++;
                    }
                }
            } catch (final DataAccessException ignored) {
                countOfficesAsBranches(p, hierarchy);
            }
        } else {
            countOfficesAsBranches(p, hierarchy);
        }

        if (hasLoanDetails) {
            try {
                final BigDecimal litigation = this.jdbcTemplate.queryForObject(
                        "SELECT COALESCE(SUM(d.litigation_amount),0) FROM `" + RbzFormMfi1Constants.DT_LOAN_DETAILS + "` d "
                                + "JOIN m_loan l ON l.id = d.loan_id JOIN m_client c ON c.id = l.client_id "
                                + "JOIN m_office o ON o.id = c.office_id "
                                + "WHERE d.is_under_litigation = 1 AND o.hierarchy LIKE ?",
                        BigDecimal.class, hierarchy + "%");
                p.litigationAmount = litigation == null ? BigDecimal.ZERO : litigation;
            } catch (final DataAccessException ignored) {
                // optional column
            }
        }
    }

    private void countOfficesAsBranches(final PortfolioManagementRow p, final String hierarchy) {
        final Long branches = this.jdbcTemplate.queryForObject("SELECT COUNT(id) FROM m_office WHERE hierarchy LIKE ?", Long.class,
                hierarchy + "%");
        p.urbanBranches = branches == null ? 0 : branches;
    }

    private void loadProfile(final RbzFormMfi1ReportData data, final OfficeContext office) {
        final InstitutionProfileRow profile = data.getProfile();
        profile.institutionName = office.name;
        final Long branches = this.jdbcTemplate.queryForObject("SELECT COUNT(id) FROM m_office WHERE hierarchy LIKE ?", Long.class,
                office.hierarchy + "%");
        profile.branchCount = branches == null ? 0 : branches;
        final Long officers = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(id) FROM m_staff WHERE is_active = 1 AND is_loan_officer = 1 AND office_id IN "
                        + "(SELECT id FROM m_office WHERE hierarchy LIKE ?)",
                Long.class, office.hierarchy + "%");
        profile.loanOfficerCount = officers == null ? 0 : officers;

        if (!tableExists(RbzFormMfi1Constants.DT_INSTITUTION_PROFILE)) {
            return;
        }
        try {
            final List<Map<String, Object>> rows = this.jdbcTemplate.queryForList(
                    "SELECT * FROM `" + RbzFormMfi1Constants.DT_INSTITUTION_PROFILE + "` WHERE " + officeFkColumn() + " = ? LIMIT 1",
                    office.id);
            if (rows.isEmpty()) {
                return;
            }
            final Map<String, Object> r = rows.get(0);
            profile.institutionName = str(r.get("institution_name"), profile.institutionName);
            profile.licenceNumber = str(r.get("licence_number"), null);
            profile.dateCommenced = (Date) r.get("date_commenced");
            profile.physicalAddress = str(r.get("physical_address"), null);
            profile.postalAddress = str(r.get("postal_address"), null);
            profile.contactTelephones = str(r.get("contact_telephones"), null);
            profile.contactPerson = str(r.get("contact_person"), null);
            profile.numEmployeesFemale = toLong(r.get("num_employees_female"));
            profile.numEmployeesMale = toLong(r.get("num_employees_male"));
            profile.numLoanOfficersFemale = toLong(r.get("num_loan_officers_female"));
            profile.numLoanOfficersMale = toLong(r.get("num_loan_officers_male"));
            profile.externalAuditors = str(r.get("external_auditors"), null);
            profile.bankers = str(r.get("bankers"), null);
            profile.lawyers = str(r.get("lawyers"), null);
            if (profile.institutionName != null) {
                data.setInstitutionName(profile.institutionName);
            }
        } catch (final DataAccessException ignored) {
            // datatable not fully created yet
        }
    }

    private void loadInsiderLoans(final RbzFormMfi1ReportData data, final String hierarchy) {
        try {
            final String sql = "SELECT COALESCE(d.related_party_name, c.display_name) AS borrower, "
                    + "COALESCE(rp.code_value, 'Related Party') AS relationship, "
                    + "COALESCE(l.principal_amount,0) AS total_limit, COALESCE(l.principal_outstanding_derived,0) AS outstanding, "
                    + "COALESCE(l.nominal_interest_rate_per_period,0) AS interest_rate, "
                    + "(SELECT cv.code_value FROM m_loan_collateral lc JOIN m_code_value cv ON cv.id = lc.type_cv_id "
                    + " WHERE lc.loan_id = l.id LIMIT 1) AS security_type, "
                    + "(SELECT COALESCE(SUM(lc.value),0) FROM m_loan_collateral lc WHERE lc.loan_id = l.id) AS security_value "
                    + "FROM `" + RbzFormMfi1Constants.DT_LOAN_DETAILS + "` d "
                    + "JOIN m_loan l ON l.id = d.loan_id JOIN m_client c ON c.id = l.client_id "
                    + "JOIN m_office o ON o.id = c.office_id "
                    + "LEFT JOIN m_code_value rp ON rp.id = d.related_party_type_cd_RbzRelatedPartyType "
                    + "WHERE d.is_insider_loan = 1 AND l.loan_status_id = 300 AND o.hierarchy LIKE ?";
            this.jdbcTemplate.query(sql, new Object[] { hierarchy + "%" }, new RowMapper<InsiderLoanRow>() {
                @Override
                public InsiderLoanRow mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                    final InsiderLoanRow row = new InsiderLoanRow();
                    row.borrowerName = rs.getString("borrower");
                    row.relationship = rs.getString("relationship");
                    row.totalLimit = rs.getBigDecimal("total_limit");
                    row.outstanding = rs.getBigDecimal("outstanding");
                    row.interestRate = rs.getBigDecimal("interest_rate");
                    row.securityType = rs.getString("security_type");
                    row.securityValue = rs.getBigDecimal("security_value");
                    data.getInsiderLoans().add(row);
                    return row;
                }
            });
        } catch (final DataAccessException ignored) {
            // optional
        }
    }

    private void loadLongTermDebt(final RbzFormMfi1ReportData data, final Long officeId) {
        try {
            final String sql = "SELECT source_of_finance, amount_borrowed, outstanding_balance, interest_rate, maturity_date "
                    + "FROM `" + RbzFormMfi1Constants.DT_LONG_TERM_DEBT + "` WHERE " + officeFkColumn() + " = ?";
            this.jdbcTemplate.query(sql, new Object[] { officeId }, new RowMapper<LongTermDebtRow>() {
                @Override
                public LongTermDebtRow mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                    final LongTermDebtRow row = new LongTermDebtRow();
                    row.sourceOfFinance = rs.getString("source_of_finance");
                    row.amountBorrowed = rs.getBigDecimal("amount_borrowed");
                    row.outstanding = rs.getBigDecimal("outstanding_balance");
                    row.interestRate = rs.getBigDecimal("interest_rate");
                    row.maturityDate = rs.getDate("maturity_date");
                    data.getLongTermDebt().add(row);
                    return row;
                }
            });
        } catch (final DataAccessException ignored) {
            // optional
        }
    }

    private void loadOfficeChannels(final RbzFormMfi1ReportData data, final String hierarchy) {
        try {
            final String sql = "SELECT ch.province, ch.district, cv.code_value AS location_type, "
                    + "COALESCE(ch.num_pos,0) AS num_pos, COALESCE(ch.num_atms,0) AS num_atms, "
                    + "COALESCE(ch.num_agencies,0) AS num_agencies, COALESCE(ch.num_mobile_branches,0) AS num_mobile_branches, "
                    + "COALESCE(ch.num_agents,0) AS num_agents, COALESCE(ch.num_banking_kiosks,0) AS num_banking_kiosks "
                    + "FROM `" + RbzFormMfi1Constants.DT_OFFICE_CHANNELS + "` ch "
                    + "JOIN m_office o ON o.id = ch." + officeFkColumn() + " "
                    + "LEFT JOIN m_code_value cv ON cv.id = ch.location_type_cd_RbzLocationType "
                    + "WHERE o.hierarchy LIKE ?";
            this.jdbcTemplate.query(sql, new Object[] { hierarchy + "%" }, new RowMapper<OfficeChannelRow>() {
                @Override
                public OfficeChannelRow mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                    final OfficeChannelRow row = new OfficeChannelRow();
                    row.province = rs.getString("province");
                    row.district = rs.getString("district");
                    row.locationType = rs.getString("location_type");
                    row.numPos = rs.getLong("num_pos");
                    row.numAtms = rs.getLong("num_atms");
                    row.numAgencies = rs.getLong("num_agencies");
                    row.numMobileBranches = rs.getLong("num_mobile_branches");
                    row.numAgents = rs.getLong("num_agents");
                    row.numBankingKiosks = rs.getLong("num_banking_kiosks");
                    row.isBranch = true;
                    data.getOfficeChannels().add(row);
                    return row;
                }
            });
        } catch (final DataAccessException ignored) {
            // optional
        }
    }

    private String loanClassExpr(final boolean hasLoanDetails) {
        if (hasLoanDetails) {
            return "COALESCE(lc_cv.code_value, 'Other')";
        }
        return "'Other'";
    }

    private String loanDetailsJoin(final boolean hasLoanDetails) {
        if (!hasLoanDetails) {
            return "";
        }
        return "LEFT JOIN `" + RbzFormMfi1Constants.DT_LOAN_DETAILS + "` d ON d.loan_id = l.id "
                + "LEFT JOIN m_code_value lc_cv ON lc_cv.id = d.loan_class_cd_RbzLoanClass ";
    }

    private boolean tableExists(final String tableName) {
        final Integer count = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?", Integer.class,
                tableName);
        return count != null && count > 0;
    }

    private String officeFkColumn() {
        return "office_id";
    }

    private static String bandForDays(final int days) {
        if (days <= 0) {
            return "CURRENT";
        }
        if (days <= 30) {
            return "SPECIAL_MENTION";
        }
        if (days <= 60) {
            return "SUBSTANDARD";
        }
        if (days <= 90) {
            return "DOUBTFUL";
        }
        return "LOSS";
    }

    private static String maturityBucket(final int days) {
        if (days <= 7) {
            return "0-7";
        }
        if (days <= 14) {
            return "8-14";
        }
        if (days <= 30) {
            return "15-30";
        }
        if (days <= 60) {
            return "31-60";
        }
        if (days <= 90) {
            return "61-90";
        }
        if (days <= 120) {
            return "91-120";
        }
        if (days <= 180) {
            return "121-180";
        }
        if (days <= 360) {
            return "181-360";
        }
        return "360+";
    }

    private static String mapPurpose(final String purpose) {
        final String n = normalize(purpose);
        for (final String p : RbzFormMfi1Constants.RBZ_PURPOSES) {
            if (normalize(p).equals(n)) {
                return normalize(p);
            }
        }
        if (n.contains("farm") || n.contains("agri")) {
            return normalize("Agriculture");
        }
        if (n.contains("consum")) {
            return normalize("Consumption");
        }
        if (n.contains("retail") || n.contains("trade")) {
            return normalize("Retail");
        }
        if (n.contains("educ")) {
            return normalize("Education");
        }
        if (n.contains("health") || n.contains("medic")) {
            return normalize("Health");
        }
        if (n.contains("service")) {
            return normalize("Services");
        }
        if (n.contains("min")) {
            return normalize("Mining");
        }
        if (n.contains("manufact")) {
            return normalize("Manufacturing");
        }
        if (n.contains("funeral")) {
            return normalize("Funeral Assistance");
        }
        if (n.contains("vendor")) {
            return normalize("Vendors");
        }
        if (n.contains("cross")) {
            return normalize("Cross Border Traders");
        }
        return normalize("Other");
    }

    private static String normalize(final String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ENGLISH).replaceAll("\\s+", " ");
    }

    private static String normalizeLoanClass(final String value) {
        final String n = normalize(value);
        if (n.startsWith("consumer")) {
            return "Consumer";
        }
        if (n.startsWith("commercial")) {
            return "Commercial";
        }
        return "Other";
    }

    private static BigDecimal toBd(final Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }

    private static Long toLong(final Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).longValue();
    }

    private static String str(final Object value, final String fallback) {
        if (value == null) {
            return fallback;
        }
        final String s = String.valueOf(value);
        return s.trim().isEmpty() ? fallback : s;
    }

    private static final class OfficeContext {
        private Long id;
        private String name;
        private String hierarchy;
    }
}
