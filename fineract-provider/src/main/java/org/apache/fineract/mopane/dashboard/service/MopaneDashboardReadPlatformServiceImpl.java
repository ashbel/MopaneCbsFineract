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
package org.apache.fineract.mopane.dashboard.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.security.exception.NoAuthorizationException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.mopane.dashboard.data.MopaneDashboardApiConstants;
import org.apache.fineract.mopane.dashboard.data.MopaneDashboardLoanMetricsData;
import org.apache.fineract.mopane.dashboard.data.MopaneDashboardLoanMetricsData.AgingBucket;
import org.apache.fineract.mopane.dashboard.data.MopaneDashboardLoanMetricsData.DisbursementTrendPoint;
import org.apache.fineract.mopane.dashboard.data.MopaneDashboardLoanMetricsData.PipelineMetrics;
import org.apache.fineract.mopane.dashboard.data.MopaneDashboardLoanMetricsData.PortfolioMetrics;
import org.apache.fineract.mopane.dashboard.data.MopaneDashboardLoanMetricsData.RecentActivity;
import org.apache.fineract.mopane.dashboard.data.MopaneDashboardLoanMetricsData.TrendPoint;
import org.apache.fineract.mopane.dashboard.data.MopaneDashboardLoanMetricsData.Trends;
import org.apache.fineract.organisation.monetary.exception.CurrencyNotFoundException;
import org.apache.fineract.organisation.office.exception.OfficeNotFoundException;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class MopaneDashboardReadPlatformServiceImpl implements MopaneDashboardReadPlatformService {

    private static final int LOAN_STATUS_SUBMITTED = 100;
    private static final int LOAN_STATUS_APPROVED = 200;
    private static final int LOAN_STATUS_ACTIVE = 300;
    private static final int CLIENT_STATUS_ACTIVE = 300;
    private static final int GROUP_STATUS_ACTIVE = 300;
    private static final int GROUP_LEVEL = 2;
    private static final int TXN_DISBURSEMENT = 1;
    private static final int TXN_REPAYMENT = 2;
    private static final int TXN_WRITEOFF = 6;
    private static final String PREFERRED_DEFAULT_CURRENCY = "USD";

    private final PlatformSecurityContext context;
    private final JdbcTemplate jdbcTemplate;
    private final MopaneDashboardMetricsCache metricsCache;

    @Autowired
    public MopaneDashboardReadPlatformServiceImpl(final PlatformSecurityContext context, final RoutingDataSource dataSource,
            final MopaneDashboardMetricsCache metricsCache) {
        this.context = context;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.metricsCache = metricsCache;
    }

    @Override
    public MopaneDashboardLoanMetricsData retrieveLoanMetrics(final Long officeIdParam, final String currencyCodeParam,
            final String trendPeriodParam, final Integer activityLimitParam) {

        final AppUser user = this.context.authenticatedUser();
        final OfficeContext office = resolveOffice(officeIdParam, user);
        final List<String> availableCurrencies = listAvailableCurrencies(office.hierarchyLike);
        final String currencyCode = resolveCurrencyCode(currencyCodeParam, availableCurrencies);
        final String trendPeriod = normalizeTrendPeriod(trendPeriodParam);
        final int activityLimit = normalizeActivityLimit(activityLimitParam);
        final Date asOf = DateUtils.getDateOfTenant();
        final Date monthStart = startOfMonth(asOf);
        final String tenantId = ThreadLocalContextUtil.getTenant().getTenantIdentifier();
        final String asOfKey = DateUtils.getLocalDateOfTenant().toString();
        final String cacheKey = MopaneDashboardMetricsCache.buildKey(tenantId, office.id, currencyCode, trendPeriod, activityLimit, asOfKey);

        final MopaneDashboardLoanMetricsData cached = this.metricsCache.get(cacheKey);
        if (cached != null) { return cached; }

        final MopaneDashboardLoanMetricsData data = new MopaneDashboardLoanMetricsData();
        data.setOfficeId(office.id);
        data.setOfficeName(office.name);
        data.setAsOfDate(asOf);
        data.setCurrencyCode(currencyCode);
        data.setAvailableCurrencies(availableCurrencies);

        loadPortfolio(data.getPortfolio(), office.hierarchyLike, currencyCode, asOf, monthStart);
        loadPipeline(data.getPipeline(), office.hierarchyLike, currencyCode, asOf, monthStart);
        data.setAging(loadAging(office.hierarchyLike, currencyCode, asOf));
        data.setTrends(loadTrends(office.hierarchyLike, currencyCode, trendPeriod, asOf));
        data.setRecentActivity(loadRecentActivity(office.hierarchyLike, activityLimit));
        this.metricsCache.put(cacheKey, data);
        return data;
    }

    private OfficeContext resolveOffice(final Long officeIdParam, final AppUser user) {
        final String userHierarchy = user.getOffice().getHierarchy();
        final Long requestedId = officeIdParam != null ? officeIdParam : user.getOffice().getId();
        try {
            final Map<String, Object> row = this.jdbcTemplate.queryForMap(
                    "SELECT id, name, hierarchy FROM m_office WHERE id = ? AND hierarchy LIKE ?", requestedId, userHierarchy + "%");
            final OfficeContext ctx = new OfficeContext();
            ctx.id = ((Number) row.get("id")).longValue();
            ctx.name = (String) row.get("name");
            ctx.hierarchy = (String) row.get("hierarchy");
            ctx.hierarchyLike = ctx.hierarchy + "%";
            return ctx;
        } catch (final EmptyResultDataAccessException e) {
            // Distinguish missing office vs outside hierarchy
            final Integer exists = this.jdbcTemplate.queryForObject("SELECT COUNT(*) FROM m_office WHERE id = ?", Integer.class,
                    requestedId);
            if (exists == null || exists == 0) { throw new OfficeNotFoundException(requestedId); }
            throw new NoAuthorizationException("User does not have sufficient privileges to act on the provided office.");
        }
    }

    private List<String> listAvailableCurrencies(final String hierarchyLike) {
        final String sql = "SELECT DISTINCT l.currency_code FROM m_loan l " + loanOfficeJoins()
                + " WHERE o.hierarchy LIKE ? AND l.currency_code IS NOT NULL AND l.currency_code <> ''";
        final List<String> codes = this.jdbcTemplate.queryForList(sql, String.class, hierarchyLike);
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

    private String resolveCurrencyCode(final String currencyCodeParam, final List<String> availableCurrencies) {
        if (availableCurrencies == null || availableCurrencies.isEmpty()) { return null; }
        if (currencyCodeParam != null && !currencyCodeParam.trim().isEmpty()) {
            final String requested = currencyCodeParam.trim().toUpperCase(Locale.ENGLISH);
            if (!availableCurrencies.contains(requested)) { throw new CurrencyNotFoundException(requested); }
            return requested;
        }
        if (availableCurrencies.contains(PREFERRED_DEFAULT_CURRENCY)) { return PREFERRED_DEFAULT_CURRENCY; }
        return availableCurrencies.get(0);
    }

    private void loadPortfolio(final PortfolioMetrics portfolio, final String hierarchyLike, final String currencyCode, final Date asOf,
            final Date monthStart) {
        portfolio.setActiveClients(countLong(
                "SELECT COUNT(*) FROM m_client c JOIN m_office o ON o.id = c.office_id WHERE c.status_enum = ? AND o.hierarchy LIKE ?",
                CLIENT_STATUS_ACTIVE, hierarchyLike));
        portfolio.setActiveGroups(countLong(
                "SELECT COUNT(*) FROM m_group g JOIN m_office o ON o.id = g.office_id "
                        + "WHERE g.status_enum = ? AND g.level_id = ? AND o.hierarchy LIKE ?",
                GROUP_STATUS_ACTIVE, GROUP_LEVEL, hierarchyLike));

        if (currencyCode == null) { return; }

        final Map<String, Object> active = queryForMapOrEmpty(
                "SELECT COUNT(l.id) AS active_loans, "
                        + "COALESCE(SUM(l.principal_outstanding_derived),0) AS gross_loan_book, "
                        + "COALESCE(SUM(l.interest_outstanding_derived),0) AS interest_outstanding, "
                        + "COALESCE(SUM(CASE WHEN l.is_npa = 1 THEN 1 ELSE 0 END),0) AS npl_count, "
                        + "COALESCE(SUM(CASE WHEN l.is_npa = 1 THEN l.principal_outstanding_derived ELSE 0 END),0) AS npl_outstanding "
                        + "FROM m_loan l " + loanOfficeJoins()
                        + " WHERE l.loan_status_id = ? AND o.hierarchy LIKE ? AND l.currency_code = ?",
                LOAN_STATUS_ACTIVE, hierarchyLike, currencyCode);
        portfolio.setActiveLoans(toLong(active.get("active_loans")));
        final BigDecimal grossLoanBook = toBd(active.get("gross_loan_book"));
        portfolio.setGrossLoanBook(grossLoanBook);
        portfolio.setInterestOutstanding(toBd(active.get("interest_outstanding")));
        portfolio.setNplCount(toLong(active.get("npl_count")));
        portfolio.setNplOutstanding(toBd(active.get("npl_outstanding")));

        final Map<String, Object> arrears = queryForMapOrEmpty(
                "SELECT COUNT(DISTINCT l.id) AS loans_in_arrears, "
                        + "COALESCE(SUM(la.principal_overdue_derived),0) AS principal_overdue "
                        + "FROM m_loan l " + loanOfficeJoins()
                        + " JOIN m_loan_arrears_aging la ON la.loan_id = l.id "
                        + "WHERE l.loan_status_id = ? AND o.hierarchy LIKE ? AND l.currency_code = ? "
                        + "AND la.principal_overdue_derived > 0",
                LOAN_STATUS_ACTIVE, hierarchyLike, currencyCode);
        final BigDecimal principalOverdue = toBd(arrears.get("principal_overdue"));
        portfolio.setLoansInArrears(toLong(arrears.get("loans_in_arrears")));
        portfolio.setPrincipalOverdue(principalOverdue);
        portfolio.setValueAtRisk(principalOverdue);
        portfolio.setPortfolioAtRiskPercent(percent(principalOverdue, grossLoanBook));

        portfolio.setInterestCollectedMtd(queryForObjectOrZero(
                "SELECT COALESCE(SUM(lt.interest_portion_derived),0) FROM m_loan_transaction lt "
                        + "JOIN m_loan l ON l.id = lt.loan_id " + loanOfficeJoins()
                        + " WHERE lt.is_reversed = 0 AND lt.transaction_type_enum = ? "
                        + "AND lt.transaction_date BETWEEN ? AND ? AND o.hierarchy LIKE ? AND l.currency_code = ?",
                TXN_REPAYMENT, monthStart, asOf, hierarchyLike, currencyCode));

        portfolio.setWriteOffsMtd(queryForObjectOrZero(
                "SELECT COALESCE(SUM(lt.amount),0) FROM m_loan_transaction lt "
                        + "JOIN m_loan l ON l.id = lt.loan_id " + loanOfficeJoins()
                        + " WHERE lt.is_reversed = 0 AND lt.transaction_type_enum = ? "
                        + "AND lt.transaction_date BETWEEN ? AND ? AND o.hierarchy LIKE ? AND l.currency_code = ?",
                TXN_WRITEOFF, monthStart, asOf, hierarchyLike, currencyCode));

        final BigDecimal expectedMonth = scheduleDueAmount(hierarchyLike, currencyCode, monthStart, asOf);
        final BigDecimal actualMonth = schedulePaidAmount(hierarchyLike, currencyCode, monthStart, asOf);
        portfolio.setCollectionRateMtdPercent(percent(actualMonth, expectedMonth));
    }

    private void loadPipeline(final PipelineMetrics pipeline, final String hierarchyLike, final String currencyCode, final Date asOf,
            final Date monthStart) {
        if (currencyCode == null) { return; }

        final Map<String, Object> pendingApproval = queryForMapOrEmpty(
                "SELECT COUNT(l.id) AS cnt, COALESCE(SUM(l.principal_amount),0) AS amount FROM m_loan l " + loanOfficeJoins()
                        + " WHERE l.loan_status_id = ? AND o.hierarchy LIKE ? AND l.currency_code = ?",
                LOAN_STATUS_SUBMITTED, hierarchyLike, currencyCode);
        pipeline.setPendingApprovalCount(toLong(pendingApproval.get("cnt")));
        pipeline.setPendingApprovalAmount(toBd(pendingApproval.get("amount")));

        final Map<String, Object> pendingDisbursement = queryForMapOrEmpty(
                "SELECT COUNT(l.id) AS cnt, COALESCE(SUM(l.principal_amount),0) AS amount FROM m_loan l " + loanOfficeJoins()
                        + " WHERE l.loan_status_id = ? AND o.hierarchy LIKE ? AND l.currency_code = ?",
                LOAN_STATUS_APPROVED, hierarchyLike, currencyCode);
        pipeline.setPendingDisbursementCount(toLong(pendingDisbursement.get("cnt")));
        pipeline.setPendingDisbursementAmount(toBd(pendingDisbursement.get("amount")));

        pipeline.setDisbursementsTodayAmount(queryForObjectOrZero(
                "SELECT COALESCE(SUM(lt.amount),0) FROM m_loan_transaction lt "
                        + "JOIN m_loan l ON l.id = lt.loan_id " + loanOfficeJoins()
                        + " WHERE lt.is_reversed = 0 AND lt.transaction_type_enum = ? "
                        + "AND lt.transaction_date = ? AND o.hierarchy LIKE ? AND l.currency_code = ?",
                TXN_DISBURSEMENT, asOf, hierarchyLike, currencyCode));

        pipeline.setDisbursementsMonthAmount(queryForObjectOrZero(
                "SELECT COALESCE(SUM(lt.amount),0) FROM m_loan_transaction lt "
                        + "JOIN m_loan l ON l.id = lt.loan_id " + loanOfficeJoins()
                        + " WHERE lt.is_reversed = 0 AND lt.transaction_type_enum = ? "
                        + "AND lt.transaction_date BETWEEN ? AND ? AND o.hierarchy LIKE ? AND l.currency_code = ?",
                TXN_DISBURSEMENT, monthStart, asOf, hierarchyLike, currencyCode));

        pipeline.setCollectionsExpectedToday(scheduleDueAmount(hierarchyLike, currencyCode, asOf, asOf));
        pipeline.setCollectionsActualToday(schedulePaidAmount(hierarchyLike, currencyCode, asOf, asOf));
        pipeline.setCollectionsExpectedMonth(scheduleDueAmount(hierarchyLike, currencyCode, monthStart, asOf));
        pipeline.setCollectionsActualMonth(schedulePaidAmount(hierarchyLike, currencyCode, monthStart, asOf));
    }

    private BigDecimal scheduleDueAmount(final String hierarchyLike, final String currencyCode, final Date fromInclusive,
            final Date toInclusive) {
        final String sql = "SELECT COALESCE(SUM("
                + "(IFNULL(ls.principal_amount,0) - IFNULL(ls.principal_writtenoff_derived,0))"
                + " + (IFNULL(ls.interest_amount,0) - IFNULL(ls.interest_writtenoff_derived,0) - IFNULL(ls.interest_waived_derived,0))"
                + " + (IFNULL(ls.fee_charges_amount,0) - IFNULL(ls.fee_charges_writtenoff_derived,0) - IFNULL(ls.fee_charges_waived_derived,0))"
                + " + (IFNULL(ls.penalty_charges_amount,0) - IFNULL(ls.penalty_charges_writtenoff_derived,0) - IFNULL(ls.penalty_charges_waived_derived,0))"
                + "),0) FROM m_loan_repayment_schedule ls "
                + "JOIN m_loan l ON l.id = ls.loan_id " + loanOfficeJoins()
                + " WHERE ls.duedate BETWEEN ? AND ? AND o.hierarchy LIKE ? AND l.loan_status_id = ? AND l.currency_code = ?";
        return queryForObjectOrZero(sql, fromInclusive, toInclusive, hierarchyLike, LOAN_STATUS_ACTIVE, currencyCode);
    }

    private BigDecimal schedulePaidAmount(final String hierarchyLike, final String currencyCode, final Date fromInclusive,
            final Date toInclusive) {
        final String sql = "SELECT COALESCE(SUM("
                + "IFNULL(ls.principal_completed_derived,0) + IFNULL(ls.interest_completed_derived,0)"
                + " + IFNULL(ls.fee_charges_completed_derived,0) + IFNULL(ls.penalty_charges_completed_derived,0)"
                + "),0) FROM m_loan_repayment_schedule ls "
                + "JOIN m_loan l ON l.id = ls.loan_id " + loanOfficeJoins()
                + " WHERE ls.duedate BETWEEN ? AND ? AND o.hierarchy LIKE ? AND l.loan_status_id = ? AND l.currency_code = ?";
        return queryForObjectOrZero(sql, fromInclusive, toInclusive, hierarchyLike, LOAN_STATUS_ACTIVE, currencyCode);
    }

    private List<AgingBucket> loadAging(final String hierarchyLike, final String currencyCode, final Date asOf) {
        final Map<String, AgingBucket> buckets = new LinkedHashMap<>();
        for (final String name : new String[] { MopaneDashboardApiConstants.AGING_CURRENT, MopaneDashboardApiConstants.AGING_1_30,
                MopaneDashboardApiConstants.AGING_31_60, MopaneDashboardApiConstants.AGING_61_90,
                MopaneDashboardApiConstants.AGING_90_PLUS }) {
            buckets.put(name, new AgingBucket(name));
        }

        if (currencyCode == null) { return new ArrayList<>(buckets.values()); }

        // Principal overdue by band — same basis as portfolio.valueAtRisk (not principal outstanding).
        final String sql = "SELECT COALESCE(la.principal_overdue_derived,0) AS outstanding, "
                + "CASE WHEN la.overdue_since_date_derived IS NULL THEN 0 "
                + "ELSE DATEDIFF(?, la.overdue_since_date_derived) END AS days_arrears "
                + "FROM m_loan l " + loanOfficeJoins() + " LEFT JOIN m_loan_arrears_aging la ON la.loan_id = l.id "
                + "WHERE l.loan_status_id = ? AND o.hierarchy LIKE ? AND l.currency_code = ?";
        final List<Map<String, Object>> rows = this.jdbcTemplate.queryForList(sql, asOf, LOAN_STATUS_ACTIVE, hierarchyLike, currencyCode);
        for (final Map<String, Object> row : rows) {
            final String band = agingBucket(toInt(row.get("days_arrears")));
            final AgingBucket bucket = buckets.get(band);
            bucket.setLoanCount(bucket.getLoanCount() + 1);
            bucket.setOutstanding(bucket.getOutstanding().add(toBd(row.get("outstanding"))));
        }
        return new ArrayList<>(buckets.values());
    }

    private Trends loadTrends(final String hierarchyLike, final String currencyCode, final String period, final Date asOf) {
        final Trends trends = new Trends();
        trends.setPeriod(period);

        final int intervals = 12;
        final Map<String, Long> clientCounts = new LinkedHashMap<>();
        final Map<String, long[]> disbursementCounts = new LinkedHashMap<>();
        final List<String> buckets = buildTrendBuckets(period, asOf, intervals);
        for (final String bucket : buckets) {
            clientCounts.put(bucket, 0L);
            disbursementCounts.put(bucket, new long[] { 0L, 0L }); // count placeholder; amount stored separately
        }
        final Map<String, BigDecimal> disbursementAmounts = new HashMap<>();
        for (final String bucket : buckets) {
            disbursementAmounts.put(bucket, BigDecimal.ZERO);
        }

        if (MopaneDashboardApiConstants.TREND_PERIOD_DAY.equals(period)) {
            loadClientTrendsByDay(hierarchyLike, asOf, intervals, clientCounts);
            if (currencyCode != null) {
                loadDisbursementTrendsByDay(hierarchyLike, currencyCode, asOf, intervals, disbursementCounts, disbursementAmounts);
            }
        } else if (MopaneDashboardApiConstants.TREND_PERIOD_WEEK.equals(period)) {
            loadClientTrendsByWeek(hierarchyLike, asOf, intervals, clientCounts);
            if (currencyCode != null) {
                loadDisbursementTrendsByWeek(hierarchyLike, currencyCode, asOf, intervals, disbursementCounts, disbursementAmounts);
            }
        } else {
            loadClientTrendsByMonth(hierarchyLike, asOf, intervals, clientCounts);
            if (currencyCode != null) {
                loadDisbursementTrendsByMonth(hierarchyLike, currencyCode, asOf, intervals, disbursementCounts, disbursementAmounts);
            }
        }

        final List<TrendPoint> newClients = new ArrayList<>();
        final List<DisbursementTrendPoint> loansDisbursed = new ArrayList<>();
        for (final String bucket : buckets) {
            newClients.add(new TrendPoint(bucket, clientCounts.get(bucket)));
            final long[] counts = disbursementCounts.get(bucket);
            loansDisbursed.add(new DisbursementTrendPoint(bucket, counts[0], disbursementAmounts.get(bucket)));
        }
        trends.setNewClients(newClients);
        trends.setLoansDisbursed(loansDisbursed);
        return trends;
    }

    private void loadClientTrendsByDay(final String hierarchyLike, final Date asOf, final int intervals,
            final Map<String, Long> clientCounts) {
        final String sql = "SELECT DATE(c.activation_date) AS bucket, COUNT(c.id) AS cnt FROM m_client c "
                + "JOIN m_office o ON o.id = c.office_id "
                + "WHERE o.hierarchy LIKE ? AND c.activation_date BETWEEN DATE_SUB(?, INTERVAL ? DAY) AND ? "
                + "GROUP BY DATE(c.activation_date)";
        for (final Map<String, Object> row : this.jdbcTemplate.queryForList(sql, hierarchyLike, asOf, intervals - 1, asOf)) {
            final String key = formatDateBucket((Date) row.get("bucket"));
            if (clientCounts.containsKey(key)) {
                clientCounts.put(key, toLong(row.get("cnt")));
            }
        }
    }

    private void loadDisbursementTrendsByDay(final String hierarchyLike, final String currencyCode, final Date asOf, final int intervals,
            final Map<String, long[]> counts, final Map<String, BigDecimal> amounts) {
        final String sql = "SELECT DATE(lt.transaction_date) AS bucket, COUNT(lt.id) AS cnt, COALESCE(SUM(lt.amount),0) AS amount "
                + "FROM m_loan_transaction lt JOIN m_loan l ON l.id = lt.loan_id " + loanOfficeJoins()
                + " WHERE lt.is_reversed = 0 AND lt.transaction_type_enum = ? AND o.hierarchy LIKE ? AND l.currency_code = ? "
                + "AND lt.transaction_date BETWEEN DATE_SUB(?, INTERVAL ? DAY) AND ? "
                + "GROUP BY DATE(lt.transaction_date)";
        for (final Map<String, Object> row : this.jdbcTemplate.queryForList(sql, TXN_DISBURSEMENT, hierarchyLike, currencyCode, asOf,
                intervals - 1, asOf)) {
            final String key = formatDateBucket((Date) row.get("bucket"));
            if (counts.containsKey(key)) {
                counts.put(key, new long[] { toLong(row.get("cnt")) });
                amounts.put(key, toBd(row.get("amount")));
            }
        }
    }

    private void loadClientTrendsByWeek(final String hierarchyLike, final Date asOf, final int intervals,
            final Map<String, Long> clientCounts) {
        final String sql = "SELECT YEARWEEK(c.activation_date, 3) AS yw, COUNT(c.id) AS cnt FROM m_client c "
                + "JOIN m_office o ON o.id = c.office_id "
                + "WHERE o.hierarchy LIKE ? AND c.activation_date BETWEEN DATE_SUB(?, INTERVAL ? WEEK) AND ? "
                + "GROUP BY YEARWEEK(c.activation_date, 3)";
        for (final Map<String, Object> row : this.jdbcTemplate.queryForList(sql, hierarchyLike, asOf, intervals - 1, asOf)) {
            final String key = String.valueOf(row.get("yw"));
            if (clientCounts.containsKey(key)) {
                clientCounts.put(key, toLong(row.get("cnt")));
            }
        }
    }

    private void loadDisbursementTrendsByWeek(final String hierarchyLike, final String currencyCode, final Date asOf, final int intervals,
            final Map<String, long[]> counts, final Map<String, BigDecimal> amounts) {
        final String sql = "SELECT YEARWEEK(lt.transaction_date, 3) AS yw, COUNT(lt.id) AS cnt, COALESCE(SUM(lt.amount),0) AS amount "
                + "FROM m_loan_transaction lt JOIN m_loan l ON l.id = lt.loan_id " + loanOfficeJoins()
                + " WHERE lt.is_reversed = 0 AND lt.transaction_type_enum = ? AND o.hierarchy LIKE ? AND l.currency_code = ? "
                + "AND lt.transaction_date BETWEEN DATE_SUB(?, INTERVAL ? WEEK) AND ? "
                + "GROUP BY YEARWEEK(lt.transaction_date, 3)";
        for (final Map<String, Object> row : this.jdbcTemplate.queryForList(sql, TXN_DISBURSEMENT, hierarchyLike, currencyCode, asOf,
                intervals - 1, asOf)) {
            final String key = String.valueOf(row.get("yw"));
            if (counts.containsKey(key)) {
                counts.put(key, new long[] { toLong(row.get("cnt")) });
                amounts.put(key, toBd(row.get("amount")));
            }
        }
    }

    private void loadClientTrendsByMonth(final String hierarchyLike, final Date asOf, final int intervals,
            final Map<String, Long> clientCounts) {
        final String sql = "SELECT DATE_FORMAT(c.activation_date, '%Y-%m') AS bucket, COUNT(c.id) AS cnt FROM m_client c "
                + "JOIN m_office o ON o.id = c.office_id "
                + "WHERE o.hierarchy LIKE ? AND c.activation_date BETWEEN DATE_SUB(?, INTERVAL ? MONTH) AND ? "
                + "GROUP BY DATE_FORMAT(c.activation_date, '%Y-%m')";
        for (final Map<String, Object> row : this.jdbcTemplate.queryForList(sql, hierarchyLike, asOf, intervals - 1, asOf)) {
            final String key = String.valueOf(row.get("bucket"));
            if (clientCounts.containsKey(key)) {
                clientCounts.put(key, toLong(row.get("cnt")));
            }
        }
    }

    private void loadDisbursementTrendsByMonth(final String hierarchyLike, final String currencyCode, final Date asOf, final int intervals,
            final Map<String, long[]> counts, final Map<String, BigDecimal> amounts) {
        final String sql = "SELECT DATE_FORMAT(lt.transaction_date, '%Y-%m') AS bucket, COUNT(lt.id) AS cnt, "
                + "COALESCE(SUM(lt.amount),0) AS amount FROM m_loan_transaction lt "
                + "JOIN m_loan l ON l.id = lt.loan_id " + loanOfficeJoins()
                + " WHERE lt.is_reversed = 0 AND lt.transaction_type_enum = ? AND o.hierarchy LIKE ? AND l.currency_code = ? "
                + "AND lt.transaction_date BETWEEN DATE_SUB(?, INTERVAL ? MONTH) AND ? "
                + "GROUP BY DATE_FORMAT(lt.transaction_date, '%Y-%m')";
        for (final Map<String, Object> row : this.jdbcTemplate.queryForList(sql, TXN_DISBURSEMENT, hierarchyLike, currencyCode, asOf,
                intervals - 1, asOf)) {
            final String key = String.valueOf(row.get("bucket"));
            if (counts.containsKey(key)) {
                counts.put(key, new long[] { toLong(row.get("cnt")) });
                amounts.put(key, toBd(row.get("amount")));
            }
        }
    }

    private List<String> buildTrendBuckets(final String period, final Date asOf, final int intervals) {
        final List<String> buckets = new ArrayList<>();
        final Calendar cal = Calendar.getInstance();
        cal.setTime(asOf);
        truncateTime(cal);
        if (MopaneDashboardApiConstants.TREND_PERIOD_DAY.equals(period)) {
            cal.add(Calendar.DAY_OF_MONTH, -(intervals - 1));
            for (int i = 0; i < intervals; i++) {
                buckets.add(formatDateBucket(cal.getTime()));
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
        } else if (MopaneDashboardApiConstants.TREND_PERIOD_WEEK.equals(period)) {
            cal.add(Calendar.WEEK_OF_YEAR, -(intervals - 1));
            for (int i = 0; i < intervals; i++) {
                buckets.add(yearWeekKey(cal));
                cal.add(Calendar.WEEK_OF_YEAR, 1);
            }
        } else {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.add(Calendar.MONTH, -(intervals - 1));
            for (int i = 0; i < intervals; i++) {
                buckets.add(String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1));
                cal.add(Calendar.MONTH, 1);
            }
        }
        return buckets;
    }

    private List<RecentActivity> loadRecentActivity(final String hierarchyLike, final int limit) {
        final String sql = "SELECT aud.id AS id, aud.action_name AS actionName, aud.entity_name AS entityName, "
                + "aud.resource_id AS resourceId, mk.username AS maker, aud.made_on_date AS madeOnDate "
                + "FROM m_portfolio_command_source aud "
                + "LEFT JOIN m_appuser mk ON mk.id = aud.maker_id "
                + "INNER JOIN m_office o ON o.id = aud.office_id "
                + "WHERE o.hierarchy LIKE ? "
                + "AND (aud.entity_name IN ('LOAN','CLIENT','GROUP','LOANTRANSACTION','CLIENTIDENTIFIER') "
                + " OR aud.loan_id IS NOT NULL OR aud.client_id IS NOT NULL) "
                + "ORDER BY aud.id DESC LIMIT ?";
        return this.jdbcTemplate.query(sql, new Object[] { hierarchyLike, Integer.valueOf(limit) }, new RowMapper<RecentActivity>() {

            @Override
            public RecentActivity mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                final Timestamp ts = rs.getTimestamp("madeOnDate");
                final Object resourceIdObj = rs.getObject("resourceId");
                final Long resourceId = resourceIdObj == null ? null : ((Number) resourceIdObj).longValue();
                return new RecentActivity(rs.getLong("id"), rs.getString("actionName"), rs.getString("entityName"), resourceId,
                        rs.getString("maker"), ts == null ? null : new Date(ts.getTime()));
            }
        });
    }

    private static String loanOfficeJoins() {
        return " LEFT JOIN m_client c ON c.id = l.client_id " + "LEFT JOIN m_group g ON g.id = l.group_id "
                + "INNER JOIN m_office o ON o.id = COALESCE(c.office_id, g.office_id) ";
    }

    private static String normalizeTrendPeriod(final String trendPeriodParam) {
        if (trendPeriodParam == null || trendPeriodParam.trim().isEmpty()) {
            return MopaneDashboardApiConstants.TREND_PERIOD_DAY;
        }
        final String value = trendPeriodParam.trim().toLowerCase(Locale.ENGLISH);
        if (MopaneDashboardApiConstants.TREND_PERIOD_WEEK.equals(value)
                || MopaneDashboardApiConstants.TREND_PERIOD_MONTH.equals(value)
                || MopaneDashboardApiConstants.TREND_PERIOD_DAY.equals(value)) { return value; }
        return MopaneDashboardApiConstants.TREND_PERIOD_DAY;
    }

    private static int normalizeActivityLimit(final Integer activityLimitParam) {
        if (activityLimitParam == null || activityLimitParam <= 0) { return MopaneDashboardApiConstants.DEFAULT_ACTIVITY_LIMIT; }
        return Math.min(activityLimitParam, MopaneDashboardApiConstants.MAX_ACTIVITY_LIMIT);
    }

    private static String agingBucket(final int days) {
        if (days <= 0) { return MopaneDashboardApiConstants.AGING_CURRENT; }
        if (days <= 30) { return MopaneDashboardApiConstants.AGING_1_30; }
        if (days <= 60) { return MopaneDashboardApiConstants.AGING_31_60; }
        if (days <= 90) { return MopaneDashboardApiConstants.AGING_61_90; }
        return MopaneDashboardApiConstants.AGING_90_PLUS;
    }

    private static Date startOfMonth(final Date asOf) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(asOf);
        truncateTime(cal);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        return cal.getTime();
    }

    private static void truncateTime(final Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private static String formatDateBucket(final Date date) {
        if (date == null) { return null; }
        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    private static String yearWeekKey(final Calendar cal) {
        // Align with MySQL YEARWEEK(date, 3) — ISO week: Monday start, week 1 has 4+ days in year
        final Calendar iso = (Calendar) cal.clone();
        iso.setFirstDayOfWeek(Calendar.MONDAY);
        iso.setMinimalDaysInFirstWeek(4);
        final int week = iso.get(Calendar.WEEK_OF_YEAR);
        int year = iso.get(Calendar.YEAR);
        final int month = iso.get(Calendar.MONTH);
        if (month == Calendar.JANUARY && week >= 52) {
            year -= 1;
        } else if (month == Calendar.DECEMBER && week == 1) {
            year += 1;
        }
        return String.format(Locale.US, "%04d%02d", year, week);
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

    private static int toInt(final Object value) {
        if (value == null) { return 0; }
        if (value instanceof Number) { return ((Number) value).intValue(); }
        try {
            return Integer.parseInt(value.toString());
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    private static BigDecimal percent(final BigDecimal numerator, final BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) { return BigDecimal.ZERO; }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private static final class OfficeContext {

        private Long id;
        private String name;
        private String hierarchy;
        private String hierarchyLike;
    }
}
