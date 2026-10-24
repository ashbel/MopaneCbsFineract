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
package org.apache.fineract.mopane.mobileofficer.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.infrastructure.security.exception.NoAuthorizationException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.mopane.mobileofficer.data.MobileOfficerDashboardApiConstants;
import org.apache.fineract.mopane.mobileofficer.data.MobileOfficerDashboardData;
import org.apache.fineract.mopane.mobileofficer.data.MobileOfficerDashboardData.TargetMetrics;
import org.apache.fineract.mopane.mobileofficer.exception.MobileOfficerDashboardDomainRuleException;
import org.apache.fineract.organisation.monetary.exception.CurrencyNotFoundException;
import org.apache.fineract.organisation.staff.exception.StaffNotFoundException;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MobileOfficerDashboardReadPlatformServiceImpl implements MobileOfficerDashboardReadPlatformService {

    private static final int LOAN_STATUS_ACTIVE = 300;
    private static final int CLIENT_STATUS_ACTIVE = 300;
    private static final int TXN_DISBURSEMENT = 1;
    private static final String PREFERRED_DEFAULT_CURRENCY = "USD";
    private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final PlatformSecurityContext context;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MobileOfficerDashboardReadPlatformServiceImpl(final PlatformSecurityContext context, final RoutingDataSource dataSource) {
        this.context = context;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public MobileOfficerDashboardData retrieveDashboard(final Long staffIdParam, final String currencyCodeParam,
            final String yearMonthParam) {
        final AppUser user = this.context.authenticatedUser();
        final StaffContext staff = resolveStaff(staffIdParam, user);

        final Date asOf = DateUtils.getDateOfTenant();
        final String yearMonth = resolveYearMonth(yearMonthParam, asOf);
        final Date monthStart = monthStartFromYearMonth(yearMonth);
        final Date monthEnd = endOfMonth(monthStart, asOf, yearMonth);

        final List<String> availableCurrencies = listAvailableCurrencies(staff.id);
        final String targetCurrencyHint = findTargetCurrencyForPeriod(staff.id, yearMonth);
        final String currencyCode = resolveCurrencyCode(currencyCodeParam, availableCurrencies, targetCurrencyHint);

        final MobileOfficerDashboardData data = new MobileOfficerDashboardData();
        data.setStaffId(staff.id);
        data.setStaffDisplayName(staff.displayName);
        data.setOfficeId(staff.officeId);
        data.setOfficeName(staff.officeName);
        data.setAsOfDate(asOf);
        data.setYearMonth(yearMonth);
        data.setCurrencyCode(currencyCode);
        data.setAvailableCurrencies(availableCurrencies);

        data.setActiveClients(countLong(
                "SELECT COUNT(*) FROM m_client c WHERE c.status_enum = ? AND c.staff_id = ?", CLIENT_STATUS_ACTIVE, staff.id));

        if (currencyCode != null) {
            loadLoanMetrics(data, staff.id, currencyCode, asOf, monthStart, monthEnd);
        }

        data.setNewClientsActualMonth(countLong(
                "SELECT COUNT(*) FROM m_client c WHERE c.staff_id = ? AND c.activation_date BETWEEN ? AND ?", staff.id, monthStart,
                monthEnd));

        loadTargets(data, staff.id, yearMonth, currencyCode, asOf, monthStart, monthEnd);
        return data;
    }

    private void loadLoanMetrics(final MobileOfficerDashboardData data, final Long staffId, final String currencyCode, final Date asOf,
            final Date monthStart, final Date monthEnd) {
        final Map<String, Object> active = queryForMapOrEmpty(
                "SELECT COUNT(l.id) AS active_loans, COALESCE(SUM(l.principal_outstanding_derived),0) AS gross_loan_book "
                        + "FROM m_loan l WHERE l.loan_status_id = ? AND l.loan_officer_id = ? AND l.currency_code = ?",
                LOAN_STATUS_ACTIVE, staffId, currencyCode);
        data.setActiveLoans(toLong(active.get("active_loans")));
        final BigDecimal grossLoanBook = toBd(active.get("gross_loan_book"));

        final Map<String, Object> arrears = queryForMapOrEmpty(
                "SELECT COUNT(DISTINCT l.id) AS loans_in_arrears, COALESCE(SUM(la.principal_overdue_derived),0) AS principal_overdue "
                        + "FROM m_loan l JOIN m_loan_arrears_aging la ON la.loan_id = l.id "
                        + "WHERE l.loan_status_id = ? AND l.loan_officer_id = ? AND l.currency_code = ? "
                        + "AND la.principal_overdue_derived > 0",
                LOAN_STATUS_ACTIVE, staffId, currencyCode);
        final BigDecimal principalOverdue = toBd(arrears.get("principal_overdue"));
        data.setLoansInArrears(toLong(arrears.get("loans_in_arrears")));
        data.setPrincipalOverdue(principalOverdue);
        data.setValueAtRisk(principalOverdue);
        data.setParPercent(percent(principalOverdue, grossLoanBook));

        data.setCollectionsDueToday(scheduleDueAmount(staffId, currencyCode, asOf, asOf));
        data.setCollectionsDueMonth(scheduleDueAmount(staffId, currencyCode, monthStart, monthEnd));
        final BigDecimal actualMonth = schedulePaidAmount(staffId, currencyCode, monthStart, monthEnd);
        data.setCollectionsActualMonth(actualMonth);
        data.setCollectionRateMtdPercent(percent(actualMonth, data.getCollectionsDueMonth()));

        data.setDisbursementsActualMonth(queryForObjectOrZero(
                "SELECT COALESCE(SUM(lt.amount),0) FROM m_loan_transaction lt JOIN m_loan l ON l.id = lt.loan_id "
                        + "WHERE lt.is_reversed = 0 AND lt.transaction_type_enum = ? AND lt.transaction_date BETWEEN ? AND ? "
                        + "AND l.loan_officer_id = ? AND l.currency_code = ?",
                TXN_DISBURSEMENT, monthStart, monthEnd, staffId, currencyCode));
    }

    private void loadTargets(final MobileOfficerDashboardData data, final Long staffId, final String yearMonth, final String currencyCode,
            final Date asOf, final Date monthStart, final Date monthEnd) {
        final TargetMetrics targets = new TargetMetrics();
        Map<String, Object> row = null;
        String matchedCurrency = currencyCode;

        if (currencyCode != null) {
            row = queryTargetRow(staffId, yearMonth, currencyCode);
        }
        if (row == null) {
            // Fall back to any target for this staff + period and realign metrics to that currency.
            try {
                row = this.jdbcTemplate.queryForMap(
                        "SELECT id, currency_code, collections_target_amount, disbursements_target_amount, new_clients_target "
                                + "FROM m_staff_monthly_target WHERE staff_id = ? AND target_year_month = ? ORDER BY currency_code LIMIT 1",
                        staffId, yearMonth);
                matchedCurrency = row.get("currency_code") == null ? null
                        : row.get("currency_code").toString().trim().toUpperCase(Locale.ENGLISH);
            } catch (final EmptyResultDataAccessException e) {
                data.setTargets(targets);
                return;
            }
        }

        if (matchedCurrency != null && (currencyCode == null || !matchedCurrency.equals(currencyCode))) {
            data.setCurrencyCode(matchedCurrency);
            if (!data.getAvailableCurrencies().contains(matchedCurrency)) {
                final List<String> currencies = new ArrayList<>(data.getAvailableCurrencies());
                currencies.add(matchedCurrency);
                data.setAvailableCurrencies(currencies);
            }
            loadLoanMetrics(data, staffId, matchedCurrency, asOf, monthStart, monthEnd);
        }

        targets.setTargetId(toLong(row.get("id")));
        targets.setCollectionsTarget(toBd(row.get("collections_target_amount")));
        targets.setDisbursementsTarget(toBd(row.get("disbursements_target_amount")));
        targets.setNewClientsTarget(toLong(row.get("new_clients_target")));
        targets.setCollectionsAchievementPercent(percent(data.getCollectionsActualMonth(), targets.getCollectionsTarget()));
        targets.setDisbursementsAchievementPercent(percent(data.getDisbursementsActualMonth(), targets.getDisbursementsTarget()));
        targets.setNewClientsAchievementPercent(
                percent(BigDecimal.valueOf(data.getNewClientsActualMonth()), BigDecimal.valueOf(targets.getNewClientsTarget())));
        data.setTargets(targets);
    }

    private Map<String, Object> queryTargetRow(final Long staffId, final String yearMonth, final String currencyCode) {
        try {
            return this.jdbcTemplate.queryForMap(
                    "SELECT id, currency_code, collections_target_amount, disbursements_target_amount, new_clients_target "
                            + "FROM m_staff_monthly_target WHERE staff_id = ? AND target_year_month = ? AND currency_code = ?",
                    staffId, yearMonth, currencyCode);
        } catch (final EmptyResultDataAccessException e) {
            return null;
        }
    }

    private String findTargetCurrencyForPeriod(final Long staffId, final String yearMonth) {
        try {
            final String code = this.jdbcTemplate.queryForObject(
                    "SELECT currency_code FROM m_staff_monthly_target WHERE staff_id = ? AND target_year_month = ? "
                            + "ORDER BY currency_code LIMIT 1",
                    String.class, staffId, yearMonth);
            if (code == null || code.trim().isEmpty()) { return null; }
            return code.trim().toUpperCase(Locale.ENGLISH);
        } catch (final EmptyResultDataAccessException e) {
            return null;
        }
    }

    private StaffContext resolveStaff(final Long staffIdParam, final AppUser user) {
        final Long ownStaffId = user.getStaffId();
        final Long requestedStaffId = staffIdParam != null ? staffIdParam : ownStaffId;
        if (requestedStaffId == null) {
            throw new MobileOfficerDashboardDomainRuleException("error.msg.mobile.officer.dashboard.no.staff",
                    "Authenticated user is not linked to a staff member.");
        }

        final boolean viewingOwn = ownStaffId != null && ownStaffId.equals(requestedStaffId);
        if (!viewingOwn) {
            user.validateHasReadPermission(MobileOfficerDashboardApiConstants.RESOURCE_NAME);
        }

        try {
            final Map<String, Object> row = this.jdbcTemplate.queryForMap(
                    "SELECT s.id as id, s.display_name as displayName, s.office_id as officeId, o.name as officeName, o.hierarchy as hierarchy "
                            + "FROM m_staff s JOIN m_office o ON o.id = s.office_id WHERE s.id = ?",
                    requestedStaffId);
            final String staffHierarchy = (String) row.get("hierarchy");
            final String userHierarchy = user.getOffice().getHierarchy();
            if (staffHierarchy == null || !staffHierarchy.startsWith(userHierarchy)) {
                throw new NoAuthorizationException("User does not have sufficient privileges for the selected staff member.");
            }
            final StaffContext ctx = new StaffContext();
            ctx.id = ((Number) row.get("id")).longValue();
            ctx.displayName = (String) row.get("displayName");
            ctx.officeId = ((Number) row.get("officeId")).longValue();
            ctx.officeName = (String) row.get("officeName");
            return ctx;
        } catch (final EmptyResultDataAccessException e) {
            throw new StaffNotFoundException(requestedStaffId);
        }
    }

    private List<String> listAvailableCurrencies(final Long staffId) {
        // Active loans only — closed-loan currencies must not steer dashboard/target selection.
        final List<String> codes = this.jdbcTemplate.queryForList(
                "SELECT DISTINCT l.currency_code FROM m_loan l WHERE l.loan_officer_id = ? AND l.loan_status_id = ? "
                        + "AND l.currency_code IS NOT NULL AND l.currency_code <> ''",
                String.class, staffId, LOAN_STATUS_ACTIVE);
        if (codes == null || codes.isEmpty()) { return new ArrayList<>(); }
        final List<String> sorted = new ArrayList<>();
        for (final String code : codes) {
            if (code == null || code.trim().isEmpty()) { continue; }
            final String normalized = code.trim().toUpperCase(Locale.ENGLISH);
            if (!sorted.contains(normalized)) {
                sorted.add(normalized);
            }
        }
        Collections.sort(sorted, new Comparator<String>() {

            @Override
            public int compare(final String left, final String right) {
                final boolean leftUsd = PREFERRED_DEFAULT_CURRENCY.equals(left);
                final boolean rightUsd = PREFERRED_DEFAULT_CURRENCY.equals(right);
                if (leftUsd && !rightUsd) { return -1; }
                if (!leftUsd && rightUsd) { return 1; }
                return left.compareTo(right);
            }
        });
        return sorted;
    }

    private String resolveCurrencyCode(final String currencyCodeParam, final List<String> availableCurrencies,
            final String targetCurrencyHint) {
        if (currencyCodeParam != null && !currencyCodeParam.trim().isEmpty()) {
            final String requested = currencyCodeParam.trim().toUpperCase(Locale.ENGLISH);
            if (availableCurrencies != null && !availableCurrencies.isEmpty() && !availableCurrencies.contains(requested)) {
                throw new CurrencyNotFoundException(requested);
            }
            return requested;
        }
        // Prefer the currency that already has a monthly target for this period.
        if (targetCurrencyHint != null) {
            if (availableCurrencies == null || availableCurrencies.isEmpty() || availableCurrencies.contains(targetCurrencyHint)) {
                return targetCurrencyHint;
            }
        }
        if (availableCurrencies == null || availableCurrencies.isEmpty()) { return null; }
        if (availableCurrencies.contains(PREFERRED_DEFAULT_CURRENCY)) { return PREFERRED_DEFAULT_CURRENCY; }
        return availableCurrencies.get(0);
    }

    private String resolveYearMonth(final String yearMonthParam, final Date asOf) {
        if (yearMonthParam != null && !yearMonthParam.trim().isEmpty()) {
            final String value = yearMonthParam.trim();
            if (!YEAR_MONTH_PATTERN.matcher(value).matches()) {
                throw new MobileOfficerDashboardDomainRuleException("error.msg.mobile.officer.dashboard.invalid.year.month",
                        "yearMonth must be in YYYY-MM format.", value);
            }
            return value;
        }
        final Calendar cal = Calendar.getInstance();
        cal.setTime(asOf);
        return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
    }

    private Date monthStartFromYearMonth(final String yearMonth) {
        final String[] parts = yearMonth.split("-");
        final Calendar cal = Calendar.getInstance();
        truncateTime(cal);
        cal.set(Calendar.YEAR, Integer.parseInt(parts[0]));
        cal.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        return cal.getTime();
    }

    private Date endOfMonth(final Date monthStart, final Date asOf, final String yearMonth) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(monthStart);
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        final Date lastDay = cal.getTime();
        final String currentMonth = resolveYearMonth(null, asOf);
        if (currentMonth.equals(yearMonth) && asOf.before(lastDay)) { return asOf; }
        return lastDay;
    }

    private BigDecimal scheduleDueAmount(final Long staffId, final String currencyCode, final Date fromInclusive, final Date toInclusive) {
        final String sql = "SELECT COALESCE(SUM("
                + "(IFNULL(ls.principal_amount,0) - IFNULL(ls.principal_writtenoff_derived,0))"
                + " + (IFNULL(ls.interest_amount,0) - IFNULL(ls.interest_writtenoff_derived,0) - IFNULL(ls.interest_waived_derived,0))"
                + " + (IFNULL(ls.fee_charges_amount,0) - IFNULL(ls.fee_charges_writtenoff_derived,0) - IFNULL(ls.fee_charges_waived_derived,0))"
                + " + (IFNULL(ls.penalty_charges_amount,0) - IFNULL(ls.penalty_charges_writtenoff_derived,0) - IFNULL(ls.penalty_charges_waived_derived,0))"
                + "),0) FROM m_loan_repayment_schedule ls JOIN m_loan l ON l.id = ls.loan_id "
                + "WHERE ls.duedate BETWEEN ? AND ? AND l.loan_officer_id = ? AND l.loan_status_id = ? AND l.currency_code = ?";
        return queryForObjectOrZero(sql, fromInclusive, toInclusive, staffId, LOAN_STATUS_ACTIVE, currencyCode);
    }

    private BigDecimal schedulePaidAmount(final Long staffId, final String currencyCode, final Date fromInclusive, final Date toInclusive) {
        final String sql = "SELECT COALESCE(SUM("
                + "IFNULL(ls.principal_completed_derived,0) + IFNULL(ls.interest_completed_derived,0)"
                + " + IFNULL(ls.fee_charges_completed_derived,0) + IFNULL(ls.penalty_charges_completed_derived,0)"
                + "),0) FROM m_loan_repayment_schedule ls JOIN m_loan l ON l.id = ls.loan_id "
                + "WHERE ls.duedate BETWEEN ? AND ? AND l.loan_officer_id = ? AND l.loan_status_id = ? AND l.currency_code = ?";
        return queryForObjectOrZero(sql, fromInclusive, toInclusive, staffId, LOAN_STATUS_ACTIVE, currencyCode);
    }

    private long countLong(final String sql, final Object... args) {
        final Long value = this.jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private BigDecimal queryForObjectOrZero(final String sql, final Object... args) {
        try {
            final BigDecimal value = this.jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
            return value == null ? BigDecimal.ZERO : value;
        } catch (final EmptyResultDataAccessException e) {
            return BigDecimal.ZERO;
        }
    }

    private Map<String, Object> queryForMapOrEmpty(final String sql, final Object... args) {
        try {
            return this.jdbcTemplate.queryForMap(sql, args);
        } catch (final EmptyResultDataAccessException e) {
            return new HashMap<>();
        }
    }

    private static void truncateTime(final Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private static BigDecimal toBd(final Object value) {
        if (value == null) { return BigDecimal.ZERO; }
        if (value instanceof BigDecimal) { return (BigDecimal) value; }
        if (value instanceof Number) { return BigDecimal.valueOf(((Number) value).doubleValue()); }
        try {
            return new BigDecimal(value.toString());
        } catch (final NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static long toLong(final Object value) {
        if (value == null) { return 0L; }
        if (value instanceof Number) { return ((Number) value).longValue(); }
        try {
            return Long.parseLong(value.toString());
        } catch (final NumberFormatException e) {
            return 0L;
        }
    }

    private static BigDecimal percent(final BigDecimal numerator, final BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) { return BigDecimal.ZERO; }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private static final class StaffContext {

        private Long id;
        private String displayName;
        private Long officeId;
        private String officeName;
    }
}
