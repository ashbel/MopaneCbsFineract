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
package org.apache.fineract.mopane.dashboard.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MopaneDashboardLoanMetricsData {

    private Long officeId;
    private String officeName;
    private Date asOfDate;
    private String currencyCode;
    private List<String> availableCurrencies = new ArrayList<>();
    private PortfolioMetrics portfolio = new PortfolioMetrics();
    private PipelineMetrics pipeline = new PipelineMetrics();
    private List<AgingBucket> aging = new ArrayList<>();
    private Trends trends = new Trends();
    private List<RecentActivity> recentActivity = new ArrayList<>();

    public Long getOfficeId() {
        return this.officeId;
    }

    public void setOfficeId(final Long officeId) {
        this.officeId = officeId;
    }

    public String getOfficeName() {
        return this.officeName;
    }

    public void setOfficeName(final String officeName) {
        this.officeName = officeName;
    }

    public Date getAsOfDate() {
        return this.asOfDate;
    }

    public void setAsOfDate(final Date asOfDate) {
        this.asOfDate = asOfDate;
    }

    public String getCurrencyCode() {
        return this.currencyCode;
    }

    public void setCurrencyCode(final String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public List<String> getAvailableCurrencies() {
        return this.availableCurrencies;
    }

    public void setAvailableCurrencies(final List<String> availableCurrencies) {
        this.availableCurrencies = availableCurrencies == null ? new ArrayList<String>() : availableCurrencies;
    }

    public PortfolioMetrics getPortfolio() {
        return this.portfolio;
    }

    public void setPortfolio(final PortfolioMetrics portfolio) {
        this.portfolio = portfolio;
    }

    public PipelineMetrics getPipeline() {
        return this.pipeline;
    }

    public void setPipeline(final PipelineMetrics pipeline) {
        this.pipeline = pipeline;
    }

    public List<AgingBucket> getAging() {
        return this.aging;
    }

    public void setAging(final List<AgingBucket> aging) {
        this.aging = aging;
    }

    public Trends getTrends() {
        return this.trends;
    }

    public void setTrends(final Trends trends) {
        this.trends = trends;
    }

    public List<RecentActivity> getRecentActivity() {
        return this.recentActivity;
    }

    public void setRecentActivity(final List<RecentActivity> recentActivity) {
        this.recentActivity = recentActivity;
    }

    public static class PortfolioMetrics {

        private long activeClients;
        private long activeGroups;
        private long activeLoans;
        private BigDecimal grossLoanBook = BigDecimal.ZERO;
        private BigDecimal portfolioAtRiskPercent = BigDecimal.ZERO;
        private BigDecimal valueAtRisk = BigDecimal.ZERO;
        private long loansInArrears;
        private BigDecimal principalOverdue = BigDecimal.ZERO;
        private BigDecimal interestOutstanding = BigDecimal.ZERO;
        private BigDecimal interestCollectedMtd = BigDecimal.ZERO;
        private long nplCount;
        private BigDecimal nplOutstanding = BigDecimal.ZERO;
        private BigDecimal writeOffsMtd = BigDecimal.ZERO;
        private BigDecimal collectionRateMtdPercent = BigDecimal.ZERO;

        public long getActiveClients() {
            return this.activeClients;
        }

        public void setActiveClients(final long activeClients) {
            this.activeClients = activeClients;
        }

        public long getActiveGroups() {
            return this.activeGroups;
        }

        public void setActiveGroups(final long activeGroups) {
            this.activeGroups = activeGroups;
        }

        public long getActiveLoans() {
            return this.activeLoans;
        }

        public void setActiveLoans(final long activeLoans) {
            this.activeLoans = activeLoans;
        }

        public BigDecimal getGrossLoanBook() {
            return this.grossLoanBook;
        }

        public void setGrossLoanBook(final BigDecimal grossLoanBook) {
            this.grossLoanBook = grossLoanBook;
        }

        public BigDecimal getPortfolioAtRiskPercent() {
            return this.portfolioAtRiskPercent;
        }

        public void setPortfolioAtRiskPercent(final BigDecimal portfolioAtRiskPercent) {
            this.portfolioAtRiskPercent = portfolioAtRiskPercent;
        }

        public BigDecimal getValueAtRisk() {
            return this.valueAtRisk;
        }

        public void setValueAtRisk(final BigDecimal valueAtRisk) {
            this.valueAtRisk = valueAtRisk;
        }

        public long getLoansInArrears() {
            return this.loansInArrears;
        }

        public void setLoansInArrears(final long loansInArrears) {
            this.loansInArrears = loansInArrears;
        }

        public BigDecimal getPrincipalOverdue() {
            return this.principalOverdue;
        }

        public void setPrincipalOverdue(final BigDecimal principalOverdue) {
            this.principalOverdue = principalOverdue;
        }

        public BigDecimal getInterestOutstanding() {
            return this.interestOutstanding;
        }

        public void setInterestOutstanding(final BigDecimal interestOutstanding) {
            this.interestOutstanding = interestOutstanding;
        }

        public BigDecimal getInterestCollectedMtd() {
            return this.interestCollectedMtd;
        }

        public void setInterestCollectedMtd(final BigDecimal interestCollectedMtd) {
            this.interestCollectedMtd = interestCollectedMtd;
        }

        public long getNplCount() {
            return this.nplCount;
        }

        public void setNplCount(final long nplCount) {
            this.nplCount = nplCount;
        }

        public BigDecimal getNplOutstanding() {
            return this.nplOutstanding;
        }

        public void setNplOutstanding(final BigDecimal nplOutstanding) {
            this.nplOutstanding = nplOutstanding;
        }

        public BigDecimal getWriteOffsMtd() {
            return this.writeOffsMtd;
        }

        public void setWriteOffsMtd(final BigDecimal writeOffsMtd) {
            this.writeOffsMtd = writeOffsMtd;
        }

        public BigDecimal getCollectionRateMtdPercent() {
            return this.collectionRateMtdPercent;
        }

        public void setCollectionRateMtdPercent(final BigDecimal collectionRateMtdPercent) {
            this.collectionRateMtdPercent = collectionRateMtdPercent;
        }
    }

    public static class PipelineMetrics {

        private long pendingApprovalCount;
        private BigDecimal pendingApprovalAmount = BigDecimal.ZERO;
        private long pendingDisbursementCount;
        private BigDecimal pendingDisbursementAmount = BigDecimal.ZERO;
        private BigDecimal disbursementsTodayAmount = BigDecimal.ZERO;
        private BigDecimal disbursementsMonthAmount = BigDecimal.ZERO;
        private BigDecimal collectionsExpectedToday = BigDecimal.ZERO;
        private BigDecimal collectionsActualToday = BigDecimal.ZERO;
        private BigDecimal collectionsExpectedMonth = BigDecimal.ZERO;
        private BigDecimal collectionsActualMonth = BigDecimal.ZERO;

        public long getPendingApprovalCount() {
            return this.pendingApprovalCount;
        }

        public void setPendingApprovalCount(final long pendingApprovalCount) {
            this.pendingApprovalCount = pendingApprovalCount;
        }

        public BigDecimal getPendingApprovalAmount() {
            return this.pendingApprovalAmount;
        }

        public void setPendingApprovalAmount(final BigDecimal pendingApprovalAmount) {
            this.pendingApprovalAmount = pendingApprovalAmount;
        }

        public long getPendingDisbursementCount() {
            return this.pendingDisbursementCount;
        }

        public void setPendingDisbursementCount(final long pendingDisbursementCount) {
            this.pendingDisbursementCount = pendingDisbursementCount;
        }

        public BigDecimal getPendingDisbursementAmount() {
            return this.pendingDisbursementAmount;
        }

        public void setPendingDisbursementAmount(final BigDecimal pendingDisbursementAmount) {
            this.pendingDisbursementAmount = pendingDisbursementAmount;
        }

        public BigDecimal getDisbursementsTodayAmount() {
            return this.disbursementsTodayAmount;
        }

        public void setDisbursementsTodayAmount(final BigDecimal disbursementsTodayAmount) {
            this.disbursementsTodayAmount = disbursementsTodayAmount;
        }

        public BigDecimal getDisbursementsMonthAmount() {
            return this.disbursementsMonthAmount;
        }

        public void setDisbursementsMonthAmount(final BigDecimal disbursementsMonthAmount) {
            this.disbursementsMonthAmount = disbursementsMonthAmount;
        }

        public BigDecimal getCollectionsExpectedToday() {
            return this.collectionsExpectedToday;
        }

        public void setCollectionsExpectedToday(final BigDecimal collectionsExpectedToday) {
            this.collectionsExpectedToday = collectionsExpectedToday;
        }

        public BigDecimal getCollectionsActualToday() {
            return this.collectionsActualToday;
        }

        public void setCollectionsActualToday(final BigDecimal collectionsActualToday) {
            this.collectionsActualToday = collectionsActualToday;
        }

        public BigDecimal getCollectionsExpectedMonth() {
            return this.collectionsExpectedMonth;
        }

        public void setCollectionsExpectedMonth(final BigDecimal collectionsExpectedMonth) {
            this.collectionsExpectedMonth = collectionsExpectedMonth;
        }

        public BigDecimal getCollectionsActualMonth() {
            return this.collectionsActualMonth;
        }

        public void setCollectionsActualMonth(final BigDecimal collectionsActualMonth) {
            this.collectionsActualMonth = collectionsActualMonth;
        }
    }

    public static class AgingBucket {

        private final String bucket;
        private long loanCount;
        private BigDecimal outstanding = BigDecimal.ZERO;

        public AgingBucket(final String bucket) {
            this.bucket = bucket;
        }

        public String getBucket() {
            return this.bucket;
        }

        public long getLoanCount() {
            return this.loanCount;
        }

        public void setLoanCount(final long loanCount) {
            this.loanCount = loanCount;
        }

        public BigDecimal getOutstanding() {
            return this.outstanding;
        }

        public void setOutstanding(final BigDecimal outstanding) {
            this.outstanding = outstanding;
        }
    }

    public static class Trends {

        private String period;
        private List<TrendPoint> newClients = new ArrayList<>();
        private List<DisbursementTrendPoint> loansDisbursed = new ArrayList<>();

        public String getPeriod() {
            return this.period;
        }

        public void setPeriod(final String period) {
            this.period = period;
        }

        public List<TrendPoint> getNewClients() {
            return this.newClients;
        }

        public void setNewClients(final List<TrendPoint> newClients) {
            this.newClients = newClients;
        }

        public List<DisbursementTrendPoint> getLoansDisbursed() {
            return this.loansDisbursed;
        }

        public void setLoansDisbursed(final List<DisbursementTrendPoint> loansDisbursed) {
            this.loansDisbursed = loansDisbursed;
        }
    }

    public static class TrendPoint {

        private final String bucket;
        private final long count;

        public TrendPoint(final String bucket, final long count) {
            this.bucket = bucket;
            this.count = count;
        }

        public String getBucket() {
            return this.bucket;
        }

        public long getCount() {
            return this.count;
        }
    }

    public static class DisbursementTrendPoint {

        private final String bucket;
        private final long count;
        private final BigDecimal amount;

        public DisbursementTrendPoint(final String bucket, final long count, final BigDecimal amount) {
            this.bucket = bucket;
            this.count = count;
            this.amount = amount == null ? BigDecimal.ZERO : amount;
        }

        public String getBucket() {
            return this.bucket;
        }

        public long getCount() {
            return this.count;
        }

        public BigDecimal getAmount() {
            return this.amount;
        }
    }

    public static class RecentActivity {

        private final Long id;
        private final String actionName;
        private final String entityName;
        private final Long resourceId;
        private final String maker;
        private final Date madeOnDate;

        public RecentActivity(final Long id, final String actionName, final String entityName, final Long resourceId, final String maker,
                final Date madeOnDate) {
            this.id = id;
            this.actionName = actionName;
            this.entityName = entityName;
            this.resourceId = resourceId;
            this.maker = maker;
            this.madeOnDate = madeOnDate;
        }

        public Long getId() {
            return this.id;
        }

        public String getActionName() {
            return this.actionName;
        }

        public String getEntityName() {
            return this.entityName;
        }

        public Long getResourceId() {
            return this.resourceId;
        }

        public String getMaker() {
            return this.maker;
        }

        public Date getMadeOnDate() {
            return this.madeOnDate;
        }
    }
}
