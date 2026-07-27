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
package org.apache.fineract.integrationtests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.accounting.Account;
import org.apache.fineract.integrationtests.common.accounting.AccountHelper;
import org.apache.fineract.integrationtests.common.accounting.JournalEntry;
import org.apache.fineract.integrationtests.common.accounting.JournalEntryHelper;
import org.apache.fineract.integrationtests.common.charges.ChargesHelper;
import org.apache.fineract.integrationtests.common.loans.LoanApplicationTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanProductTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanStatusChecker;
import org.apache.fineract.integrationtests.common.loans.LoanTransactionHelper;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;

/**
 * Covers the Mambu-style capitalised loan charge feature: a capitalised fee must bump the loan's principal and
 * repayment schedule (not just post a GL entry), and its income must be recognised exactly once, upfront, at the
 * moment of capitalisation - for both disbursement-time and mid-term capitalisation, and for multiple capitalised
 * charges on the same loan.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class LoanCapitalizedFeeIntegrationTest {

    private static final Integer CHARGE_TIME_TYPE_DISBURSEMENT_CAPITALISED = 17;
    private static final Integer CHARGE_TIME_TYPE_SPECIFIED_DUE_DATE = 2;
    private static final Integer CHARGE_CALCULATION_TYPE_FLAT = 1;
    /** LoanTransactionType.FEE — must not be posted for capitalised charges. */
    private static final Integer TXN_TYPE_FEES_CHARGED = 20;
    /** LoanTransactionType.CAPITALIZED_FEE — sole income/principal txn for capitalised charges. */
    private static final Integer TXN_TYPE_CAPITALIZED_FEE = 21;

    private final String DATE_OF_JOINING = "01 January 2011";
    private final Float LP_PRINCIPAL = 10000.0f;
    private final String LP_REPAYMENTS = "5";
    private final String LP_REPAYMENT_PERIOD = "2";
    private final String LP_INTEREST_RATE = "1";
    private final String EXPECTED_DISBURSAL_DATE = "04 March 2011";
    private final String LOAN_APPLICATION_SUBMISSION_DATE = "3 March 2011";
    private final String LOAN_TERM_FREQUENCY = "10";
    private final String INDIVIDUAL_LOAN = "individual";

    private RequestSpecification requestSpec;
    private ResponseSpecification responseSpec;
    private ResponseSpecification responseSpecForDomainRuleViolation;
    private LoanTransactionHelper loanTransactionHelper;
    private AccountHelper accountHelper;
    private JournalEntryHelper journalEntryHelper;

    @Before
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        this.responseSpecForDomainRuleViolation = new ResponseSpecBuilder().expectStatusCode(403).build();

        this.loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.journalEntryHelper = new JournalEntryHelper(this.requestSpec, this.responseSpec);
    }

    /**
     * Two DISBURSEMENT_CAPITALISED charges are attached before disbursal. Regression test for the bug where
     * disbursement-time capitalisation posted a GL entry without ever bumping loan principal/schedule, and for the
     * old double-counting bug when a loan carries more than one capitalised charge.
     */
    @Test
    public void disbursementCapitalisedFeesIncreasePrincipalAndPostFeeIncomeOnce() {
        final Account assetAccount = this.accountHelper.createAssetAccount();
        final Account incomeAccount = this.accountHelper.createIncomeAccount();
        final Account expenseAccount = this.accountHelper.createExpenseAccount();
        final Account overpaymentAccount = this.accountHelper.createLiabilityAccount();

        final Integer loanProductID = createCashBasedLoanProduct(assetAccount, incomeAccount, expenseAccount, overpaymentAccount);

        final Integer chargeIdOne = ChargesHelper.createCharges(this.requestSpec, this.responseSpec,
                getDisbursementCapitalisedChargeJSON("100"));
        final Integer chargeIdTwo = ChargesHelper.createCharges(this.requestSpec, this.responseSpec,
                getDisbursementCapitalisedChargeJSON("50"));

        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec, this.DATE_OF_JOINING);
        final Integer loanID = applyForLoanApplication(clientID, loanProductID);

        HashMap loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanID);
        LoanStatusChecker.verifyLoanIsPending(loanStatusHashMap);

        this.loanTransactionHelper.addChargesForLoan(loanID, getAddChargeJSON(chargeIdOne.toString(), "100", null, false));
        this.loanTransactionHelper.addChargesForLoan(loanID, getAddChargeJSON(chargeIdTwo.toString(), "50", null, false));

        loanStatusHashMap = this.loanTransactionHelper.approveLoan(this.EXPECTED_DISBURSAL_DATE, loanID);
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);

        loanStatusHashMap = this.loanTransactionHelper.disburseLoan(this.EXPECTED_DISBURSAL_DATE, loanID);
        LoanStatusChecker.verifyLoanIsActive(loanStatusHashMap);

        // Principal/schedule must include both capitalised fees once (10000 + 100 + 50), not double-bumped.
        final ArrayList<HashMap> repaymentPeriods = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec,
                this.responseSpec, loanID);
        assertEquals("Disbursement-time capitalised fees must be added to loan principal once", Float.valueOf(this.LP_PRINCIPAL + 150.0f),
                repaymentPeriods.get(0).get("principalLoanBalanceOutstanding"));

        assertCapitalisedFeeTransactionsOnly(loanID, new float[] { 100f, 50f });

        // Each capitalised fee is booked once, in full, as fee income at capitalisation: Dr loan portfolio / Cr fee income.
        this.journalEntryHelper.checkJournalEntryForAssetAccount(assetAccount, this.EXPECTED_DISBURSAL_DATE,
                new JournalEntry(100f, JournalEntry.TransactionType.DEBIT), new JournalEntry(50f, JournalEntry.TransactionType.DEBIT));
        this.journalEntryHelper.checkJournalEntryForIncomeAccount(incomeAccount, this.EXPECTED_DISBURSAL_DATE,
                new JournalEntry(100f, JournalEntry.TransactionType.CREDIT), new JournalEntry(50f, JournalEntry.TransactionType.CREDIT));
    }

    /**
     * A charge flagged isCapitalized=true is added to an already-active loan. Must bump principal, regenerate the
     * schedule, and post the same upfront Dr loan portfolio / Cr fee income entry as the disbursement-time path.
     */
    @Test
    public void midTermCapitalisedFeeIncreasesPrincipalAndPostsFeeIncome() {
        final Account assetAccount = this.accountHelper.createAssetAccount();
        final Account incomeAccount = this.accountHelper.createIncomeAccount();
        final Account expenseAccount = this.accountHelper.createExpenseAccount();
        final Account overpaymentAccount = this.accountHelper.createLiabilityAccount();

        final Integer loanProductID = createCashBasedLoanProduct(assetAccount, incomeAccount, expenseAccount, overpaymentAccount);

        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec, this.DATE_OF_JOINING);
        final Integer loanID = applyForLoanApplication(clientID, loanProductID);

        this.loanTransactionHelper.approveLoan(this.EXPECTED_DISBURSAL_DATE, loanID);
        HashMap loanStatusHashMap = this.loanTransactionHelper.disburseLoan(this.EXPECTED_DISBURSAL_DATE, loanID);
        LoanStatusChecker.verifyLoanIsActive(loanStatusHashMap);

        final ArrayList<HashMap> scheduleBeforeCapitalisation = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec,
                this.responseSpec, loanID);
        assertEquals(this.LP_PRINCIPAL, scheduleBeforeCapitalisation.get(0).get("principalLoanBalanceOutstanding"));

        final String capitalisationDate = "10 March 2011";
        final Integer chargeId = ChargesHelper.createCharges(this.requestSpec, this.responseSpec,
                getSpecifiedDueDateChargeJSON("200"));
        this.loanTransactionHelper.addChargesForLoan(loanID, getAddChargeJSON(chargeId.toString(), "200", capitalisationDate, true));

        final ArrayList<HashMap> scheduleAfterCapitalisation = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec,
                this.responseSpec, loanID);
        assertEquals("Mid-term capitalised fee must bump principal once and regenerate the schedule",
                Float.valueOf(this.LP_PRINCIPAL + 200.0f), scheduleAfterCapitalisation.get(0).get("principalLoanBalanceOutstanding"));

        assertCapitalisedFeeTransactionsOnly(loanID, new float[] { 200f });

        this.journalEntryHelper.checkJournalEntryForAssetAccount(assetAccount, capitalisationDate,
                new JournalEntry(200f, JournalEntry.TransactionType.DEBIT));
        this.journalEntryHelper.checkJournalEntryForIncomeAccount(incomeAccount, capitalisationDate,
                new JournalEntry(200f, JournalEntry.TransactionType.CREDIT));
    }

    /**
     * Multi-disbursement loans explicitly reject mid-term capitalised fees (Loan#applyPrincipalCapitalisingFeeForCharge).
     */
    @Test
    public void multiDisburseLoanRejectsMidTermCapitalisedFee() {
        final Integer loanProductID = createMultiDisburseLoanProduct();

        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec, this.DATE_OF_JOINING);

        final List<HashMap> tranches = new ArrayList<>();
        tranches.add(this.loanTransactionHelper.createTrancheDetail(null, "1 March 2011", "6000"));
        tranches.add(this.loanTransactionHelper.createTrancheDetail(null, "1 April 2011", "4000"));

        final String loanApplicationJSON = new LoanApplicationTestBuilder().withPrincipal(this.LP_PRINCIPAL.toString())
                .withLoanTermFrequency(this.LOAN_TERM_FREQUENCY).withLoanTermFrequencyAsMonths().withNumberOfRepayments(this.LP_REPAYMENTS)
                .withRepaymentEveryAfter(this.LP_REPAYMENT_PERIOD).withRepaymentFrequencyTypeAsMonths()
                .withInterestRatePerPeriod(this.LP_INTEREST_RATE).withInterestTypeAsFlatBalance()
                .withAmortizationTypeAsEqualPrincipalPayments().withInterestCalculationPeriodTypeSameAsRepaymentPeriod()
                .withExpectedDisbursementDate(this.EXPECTED_DISBURSAL_DATE).withSubmittedOnDate(this.LOAN_APPLICATION_SUBMISSION_DATE)
                .withTranches(tranches).withLoanType(this.INDIVIDUAL_LOAN).build(clientID.toString(), loanProductID.toString(), null);
        final Integer loanID = this.loanTransactionHelper.getLoanId(loanApplicationJSON);

        this.loanTransactionHelper.approveLoan(this.EXPECTED_DISBURSAL_DATE, loanID);
        this.loanTransactionHelper.disburseLoan(this.EXPECTED_DISBURSAL_DATE, loanID);

        final Integer chargeId = ChargesHelper.createCharges(this.requestSpec, this.responseSpec, getSpecifiedDueDateChargeJSON("100"));
        this.loanTransactionHelper.addChargesForAllreadyDisursedLoan(loanID,
                getAddChargeJSON(chargeId.toString(), "100", "10 March 2011", true), this.responseSpecForDomainRuleViolation);
    }

    private Integer applyForLoanApplication(final Integer clientID, final Integer loanProductID) {
        final String loanApplicationJSON = new LoanApplicationTestBuilder().withPrincipal(this.LP_PRINCIPAL.toString())
                .withLoanTermFrequency(this.LOAN_TERM_FREQUENCY).withLoanTermFrequencyAsMonths().withNumberOfRepayments(this.LP_REPAYMENTS)
                .withRepaymentEveryAfter(this.LP_REPAYMENT_PERIOD).withRepaymentFrequencyTypeAsMonths()
                .withInterestRatePerPeriod(this.LP_INTEREST_RATE).withInterestTypeAsFlatBalance()
                .withAmortizationTypeAsEqualPrincipalPayments().withInterestCalculationPeriodTypeSameAsRepaymentPeriod()
                .withExpectedDisbursementDate(this.EXPECTED_DISBURSAL_DATE).withSubmittedOnDate(this.LOAN_APPLICATION_SUBMISSION_DATE)
                .withLoanType(this.INDIVIDUAL_LOAN).build(clientID.toString(), loanProductID.toString(), null);
        return this.loanTransactionHelper.getLoanId(loanApplicationJSON);
    }

    private Integer createCashBasedLoanProduct(final Account... accounts) {
        final String loanProductJSON = new LoanProductTestBuilder().withPrincipal(this.LP_PRINCIPAL.toString()).withRepaymentTypeAsMonth()
                .withRepaymentAfterEvery(this.LP_REPAYMENT_PERIOD).withNumberOfRepayments(this.LP_REPAYMENTS).withRepaymentTypeAsMonth()
                .withinterestRatePerPeriod(this.LP_INTEREST_RATE).withInterestRateFrequencyTypeAsMonths()
                .withAmortizationTypeAsEqualPrincipalPayment().withInterestTypeAsFlat().withAccountingRuleAsCashBased(accounts).build(null);
        return this.loanTransactionHelper.getLoanProductId(loanProductJSON);
    }

    private Integer createMultiDisburseLoanProduct() {
        final String loanProductJSON = new LoanProductTestBuilder().withPrincipal(this.LP_PRINCIPAL.toString()).withRepaymentTypeAsMonth()
                .withRepaymentAfterEvery(this.LP_REPAYMENT_PERIOD).withNumberOfRepayments(this.LP_REPAYMENTS).withRepaymentTypeAsMonth()
                .withinterestRatePerPeriod(this.LP_INTEREST_RATE).withInterestRateFrequencyTypeAsMonths()
                .withAmortizationTypeAsEqualPrincipalPayment().withInterestTypeAsFlat().withTranches(true).build(null);
        return this.loanTransactionHelper.getLoanProductId(loanProductJSON);
    }

    private String getDisbursementCapitalisedChargeJSON(final String amount) {
        final HashMap<String, Object> map = ChargesHelper.populateDefaultsForLoan();
        map.put("chargeTimeType", CHARGE_TIME_TYPE_DISBURSEMENT_CAPITALISED);
        map.put("amount", amount);
        map.put("chargeCalculationType", CHARGE_CALCULATION_TYPE_FLAT);
        return new Gson().toJson(map);
    }

    private String getSpecifiedDueDateChargeJSON(final String amount) {
        final HashMap<String, Object> map = ChargesHelper.populateDefaultsForLoan();
        map.put("chargeTimeType", CHARGE_TIME_TYPE_SPECIFIED_DUE_DATE);
        map.put("amount", amount);
        map.put("chargeCalculationType", CHARGE_CALCULATION_TYPE_FLAT);
        map.put("penalty", false);
        return new Gson().toJson(map);
    }

    private String getAddChargeJSON(final String chargeId, final String amount, final String dueDate, final boolean isCapitalized) {
        final HashMap<String, Object> map = new HashMap<>();
        map.put("locale", "en");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("chargeId", chargeId);
        map.put("amount", amount);
        if (dueDate != null) {
            map.put("dueDate", dueDate);
        }
        if (isCapitalized) {
            map.put("isCapitalized", true);
        }
        return new Gson().toJson(map);
    }

    /**
     * Capitalised fees must post exactly one CAPITALIZED_FEE txn per amount and must not also post Fees Charged.
     */
    private void assertCapitalisedFeeTransactionsOnly(final Integer loanID, final float[] expectedCapitalisedAmounts) {
        final ArrayList<HashMap> transactions = (ArrayList<HashMap>) this.loanTransactionHelper.getLoanDetail(this.requestSpec,
                this.responseSpec, loanID, "transactions");

        final List<Float> capitalisedAmounts = new ArrayList<>();
        for (final HashMap transaction : transactions) {
            final HashMap type = (HashMap) transaction.get("type");
            final Integer typeId = ((Number) type.get("id")).intValue();
            final Float amount = Float.valueOf(String.valueOf(transaction.get("amount")));
            if (TXN_TYPE_FEES_CHARGED.equals(typeId)) {
                for (final float expected : expectedCapitalisedAmounts) {
                    assertTrue("Capitalised fee amount " + expected + " must not also appear as Fees Charged",
                            Math.abs(amount - expected) > 0.001f);
                }
            }
            if (TXN_TYPE_CAPITALIZED_FEE.equals(typeId)) {
                capitalisedAmounts.add(amount);
            }
        }

        assertEquals("Expected one Capitalised fee (principal) transaction per capitalised charge", expectedCapitalisedAmounts.length,
                capitalisedAmounts.size());
        for (final float expected : expectedCapitalisedAmounts) {
            boolean found = false;
            for (final Float actual : capitalisedAmounts) {
                if (Math.abs(actual - expected) < 0.001f) {
                    found = true;
                    break;
                }
            }
            assertTrue("Missing Capitalised fee (principal) transaction for amount " + expected, found);
        }
    }
}
