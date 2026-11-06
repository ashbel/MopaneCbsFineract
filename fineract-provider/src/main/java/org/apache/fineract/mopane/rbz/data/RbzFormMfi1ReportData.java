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
package org.apache.fineract.mopane.rbz.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RbzFormMfi1ReportData {

    private String institutionName;
    private String financialYear;
    private Date startDate;
    private Date endDate;
    private Long officeId;

    private final Map<String, BigDecimal> loanClassOutstanding = new HashMap<>();
    private final Map<String, BigDecimal> loanClassInterestIncome = new HashMap<>();
    private final Map<String, BigDecimal> feeIncome = new HashMap<>();
    private BigDecimal penaltyIncome = BigDecimal.ZERO;

    /** band -> class -> outstanding */
    private final Map<String, Map<String, BigDecimal>> assetQuality = new LinkedHashMap<>();

    private final Map<String, PurposeRow> purposeDistribution = new LinkedHashMap<>();
    private final Map<String, PurposeRow> purposeGender = new LinkedHashMap<>();

    private final List<TopBorrowerRow> topBorrowers = new ArrayList<>();
    private final List<InsiderLoanRow> insiderLoans = new ArrayList<>();
    private final List<LongTermDebtRow> longTermDebt = new ArrayList<>();
    private final List<OfficeChannelRow> officeChannels = new ArrayList<>();

    private final Map<String, BigDecimal> assetMaturityInflows = new LinkedHashMap<>();
    private final Map<String, BigDecimal> liabilityMaturityOutflows = new LinkedHashMap<>();

    private PortfolioManagementRow portfolio = new PortfolioManagementRow();
    private InstitutionProfileRow profile = new InstitutionProfileRow();

    public String getInstitutionName() {
        return this.institutionName;
    }

    public void setInstitutionName(final String institutionName) {
        this.institutionName = institutionName;
    }

    public String getFinancialYear() {
        return this.financialYear;
    }

    public void setFinancialYear(final String financialYear) {
        this.financialYear = financialYear;
    }

    public Date getStartDate() {
        return this.startDate;
    }

    public void setStartDate(final Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return this.endDate;
    }

    public void setEndDate(final Date endDate) {
        this.endDate = endDate;
    }

    public Long getOfficeId() {
        return this.officeId;
    }

    public void setOfficeId(final Long officeId) {
        this.officeId = officeId;
    }

    public Map<String, BigDecimal> getLoanClassOutstanding() {
        return this.loanClassOutstanding;
    }

    public Map<String, BigDecimal> getLoanClassInterestIncome() {
        return this.loanClassInterestIncome;
    }

    public Map<String, BigDecimal> getFeeIncome() {
        return this.feeIncome;
    }

    public BigDecimal getPenaltyIncome() {
        return this.penaltyIncome;
    }

    public void setPenaltyIncome(final BigDecimal penaltyIncome) {
        this.penaltyIncome = penaltyIncome == null ? BigDecimal.ZERO : penaltyIncome;
    }

    public Map<String, Map<String, BigDecimal>> getAssetQuality() {
        return this.assetQuality;
    }

    public Map<String, PurposeRow> getPurposeDistribution() {
        return this.purposeDistribution;
    }

    public Map<String, PurposeRow> getPurposeGender() {
        return this.purposeGender;
    }

    public List<TopBorrowerRow> getTopBorrowers() {
        return this.topBorrowers;
    }

    public List<InsiderLoanRow> getInsiderLoans() {
        return this.insiderLoans;
    }

    public List<LongTermDebtRow> getLongTermDebt() {
        return this.longTermDebt;
    }

    public List<OfficeChannelRow> getOfficeChannels() {
        return this.officeChannels;
    }

    public Map<String, BigDecimal> getAssetMaturityInflows() {
        return this.assetMaturityInflows;
    }

    public Map<String, BigDecimal> getLiabilityMaturityOutflows() {
        return this.liabilityMaturityOutflows;
    }

    public PortfolioManagementRow getPortfolio() {
        return this.portfolio;
    }

    public void setPortfolio(final PortfolioManagementRow portfolio) {
        this.portfolio = portfolio;
    }

    public InstitutionProfileRow getProfile() {
        return this.profile;
    }

    public void setProfile(final InstitutionProfileRow profile) {
        this.profile = profile;
    }

    public static class PurposeRow {
        public long numberOfClients;
        public long numberOfLoans;
        public BigDecimal value = BigDecimal.ZERO;
        public long femaleClients;
    }

    public static class TopBorrowerRow {
        public String name;
        public BigDecimal amountBorrowed = BigDecimal.ZERO;
        public BigDecimal outstanding = BigDecimal.ZERO;
        public Date maturityDate;
        public String securityType;
        public BigDecimal securityValue = BigDecimal.ZERO;
    }

    public static class InsiderLoanRow {
        public String borrowerName;
        public String relationship;
        public BigDecimal totalLimit = BigDecimal.ZERO;
        public BigDecimal outstanding = BigDecimal.ZERO;
        public BigDecimal interestRate = BigDecimal.ZERO;
        public String securityType;
        public BigDecimal securityValue = BigDecimal.ZERO;
    }

    public static class LongTermDebtRow {
        public String sourceOfFinance;
        public BigDecimal amountBorrowed = BigDecimal.ZERO;
        public BigDecimal outstanding = BigDecimal.ZERO;
        public BigDecimal interestRate = BigDecimal.ZERO;
        public Date maturityDate;
    }

    public static class OfficeChannelRow {
        public String province;
        public String district;
        public String locationType;
        public long numPos;
        public long numAtms;
        public long numAgencies;
        public long numMobileBranches;
        public long numAgents;
        public long numBankingKiosks;
        public boolean isBranch = true;
    }

    public static class PortfolioManagementRow {
        public long loansDisbursedCount;
        public BigDecimal loansDisbursedValue = BigDecimal.ZERO;
        public long firstLoanClients;
        public long repeatLoanClients;
        public long activeClientsStart;
        public long activeClientsEnd;
        public long outstandingLoans;
        public BigDecimal averageLoanTermMonths = BigDecimal.ZERO;
        public long totalClients;
        public long urbanBranches;
        public long ruralBranches;
        public long borrowingGroups;
        public long groupLoanClients;
        public long individualLoanClients;
        public BigDecimal writeOffs = BigDecimal.ZERO;
        public long femaleBorrowers;
        public long maleBorrowers;
        public long rescheduledLoans;
        public BigDecimal litigationAmount = BigDecimal.ZERO;
    }

    public static class InstitutionProfileRow {
        public String institutionName;
        public String licenceNumber;
        public Date dateCommenced;
        public String physicalAddress;
        public String postalAddress;
        public String contactTelephones;
        public String contactPerson;
        public Long numEmployeesFemale;
        public Long numEmployeesMale;
        public Long numLoanOfficersFemale;
        public Long numLoanOfficersMale;
        public String externalAuditors;
        public String bankers;
        public String lawyers;
        public long branchCount;
        public long loanOfficerCount;
    }
}
