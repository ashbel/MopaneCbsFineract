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
package org.apache.fineract.mopane.mobileofficer.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MobileOfficerDashboardData {

    private Long staffId;
    private String staffDisplayName;
    private Long officeId;
    private String officeName;
    private Date asOfDate;
    private String yearMonth;
    private String currencyCode;
    private List<String> availableCurrencies = new ArrayList<>();

    private long activeClients;
    private long activeLoans;
    private BigDecimal parPercent = BigDecimal.ZERO;
    private BigDecimal valueAtRisk = BigDecimal.ZERO;
    private long loansInArrears;
    private BigDecimal principalOverdue = BigDecimal.ZERO;
    private BigDecimal collectionsDueToday = BigDecimal.ZERO;
    private BigDecimal collectionsDueMonth = BigDecimal.ZERO;
    private BigDecimal collectionsActualMonth = BigDecimal.ZERO;
    private BigDecimal collectionRateMtdPercent = BigDecimal.ZERO;
    private BigDecimal disbursementsActualMonth = BigDecimal.ZERO;
    private long newClientsActualMonth;

    private TargetMetrics targets = new TargetMetrics();

    public Long getStaffId() {
        return this.staffId;
    }

    public void setStaffId(final Long staffId) {
        this.staffId = staffId;
    }

    public String getStaffDisplayName() {
        return this.staffDisplayName;
    }

    public void setStaffDisplayName(final String staffDisplayName) {
        this.staffDisplayName = staffDisplayName;
    }

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

    public String getYearMonth() {
        return this.yearMonth;
    }

    public void setYearMonth(final String yearMonth) {
        this.yearMonth = yearMonth;
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

    public long getActiveClients() {
        return this.activeClients;
    }

    public void setActiveClients(final long activeClients) {
        this.activeClients = activeClients;
    }

    public long getActiveLoans() {
        return this.activeLoans;
    }

    public void setActiveLoans(final long activeLoans) {
        this.activeLoans = activeLoans;
    }

    public BigDecimal getParPercent() {
        return this.parPercent;
    }

    public void setParPercent(final BigDecimal parPercent) {
        this.parPercent = parPercent;
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

    public BigDecimal getCollectionsDueToday() {
        return this.collectionsDueToday;
    }

    public void setCollectionsDueToday(final BigDecimal collectionsDueToday) {
        this.collectionsDueToday = collectionsDueToday;
    }

    public BigDecimal getCollectionsDueMonth() {
        return this.collectionsDueMonth;
    }

    public void setCollectionsDueMonth(final BigDecimal collectionsDueMonth) {
        this.collectionsDueMonth = collectionsDueMonth;
    }

    public BigDecimal getCollectionsActualMonth() {
        return this.collectionsActualMonth;
    }

    public void setCollectionsActualMonth(final BigDecimal collectionsActualMonth) {
        this.collectionsActualMonth = collectionsActualMonth;
    }

    public BigDecimal getCollectionRateMtdPercent() {
        return this.collectionRateMtdPercent;
    }

    public void setCollectionRateMtdPercent(final BigDecimal collectionRateMtdPercent) {
        this.collectionRateMtdPercent = collectionRateMtdPercent;
    }

    public BigDecimal getDisbursementsActualMonth() {
        return this.disbursementsActualMonth;
    }

    public void setDisbursementsActualMonth(final BigDecimal disbursementsActualMonth) {
        this.disbursementsActualMonth = disbursementsActualMonth;
    }

    public long getNewClientsActualMonth() {
        return this.newClientsActualMonth;
    }

    public void setNewClientsActualMonth(final long newClientsActualMonth) {
        this.newClientsActualMonth = newClientsActualMonth;
    }

    public TargetMetrics getTargets() {
        return this.targets;
    }

    public void setTargets(final TargetMetrics targets) {
        this.targets = targets == null ? new TargetMetrics() : targets;
    }

    public static class TargetMetrics {

        private Long targetId;
        private BigDecimal collectionsTarget = BigDecimal.ZERO;
        private BigDecimal disbursementsTarget = BigDecimal.ZERO;
        private long newClientsTarget;
        private BigDecimal collectionsAchievementPercent = BigDecimal.ZERO;
        private BigDecimal disbursementsAchievementPercent = BigDecimal.ZERO;
        private BigDecimal newClientsAchievementPercent = BigDecimal.ZERO;

        public Long getTargetId() {
            return this.targetId;
        }

        public void setTargetId(final Long targetId) {
            this.targetId = targetId;
        }

        public BigDecimal getCollectionsTarget() {
            return this.collectionsTarget;
        }

        public void setCollectionsTarget(final BigDecimal collectionsTarget) {
            this.collectionsTarget = collectionsTarget == null ? BigDecimal.ZERO : collectionsTarget;
        }

        public BigDecimal getDisbursementsTarget() {
            return this.disbursementsTarget;
        }

        public void setDisbursementsTarget(final BigDecimal disbursementsTarget) {
            this.disbursementsTarget = disbursementsTarget == null ? BigDecimal.ZERO : disbursementsTarget;
        }

        public long getNewClientsTarget() {
            return this.newClientsTarget;
        }

        public void setNewClientsTarget(final long newClientsTarget) {
            this.newClientsTarget = newClientsTarget;
        }

        public BigDecimal getCollectionsAchievementPercent() {
            return this.collectionsAchievementPercent;
        }

        public void setCollectionsAchievementPercent(final BigDecimal collectionsAchievementPercent) {
            this.collectionsAchievementPercent = collectionsAchievementPercent;
        }

        public BigDecimal getDisbursementsAchievementPercent() {
            return this.disbursementsAchievementPercent;
        }

        public void setDisbursementsAchievementPercent(final BigDecimal disbursementsAchievementPercent) {
            this.disbursementsAchievementPercent = disbursementsAchievementPercent;
        }

        public BigDecimal getNewClientsAchievementPercent() {
            return this.newClientsAchievementPercent;
        }

        public void setNewClientsAchievementPercent(final BigDecimal newClientsAchievementPercent) {
            this.newClientsAchievementPercent = newClientsAchievementPercent;
        }
    }
}
