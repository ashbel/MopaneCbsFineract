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
package org.apache.fineract.portfolio.loanaccount.loanschedule.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.common.domain.DaysInMonthType;
import org.apache.fineract.portfolio.common.domain.DaysInYearType;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.common.domain.WeeksInYearType;
import org.apache.fineract.portfolio.loanproduct.domain.AmortizationMethod;
import org.apache.fineract.portfolio.loanproduct.domain.InterestCalculationPeriodMethod;
import org.apache.fineract.portfolio.loanproduct.domain.InterestMethod;
import org.apache.fineract.portfolio.loanproduct.serialization.LoanProductDataValidator;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

public class WeeksInYearInterestTest {

    private static final BigDecimal TWELVE_PERCENT = new BigDecimal("12");
    private static final BigDecimal THREE_PERCENT = new BigDecimal("3");
    private static final MathContext MATH_CONTEXT = new MathContext(8, RoundingMode.HALF_UP);

    private final AprCalculator aprCalculator = new AprCalculator();
    private final MonetaryCurrency dollars = new MonetaryCurrency("USD", 2, null);

    @Before
    public void setRoundingMode() throws Exception {
        final Field field = MoneyHelper.class.getDeclaredField("roundingMode");
        field.setAccessible(true);
        field.set(null, RoundingMode.HALF_UP);
    }

    @Test
    public void monthlyRateStaysTimesTwelveForEitherWeekCount() {
        assertEquals(0, new BigDecimal("144").compareTo(this.aprCalculator.calculateFrom(PeriodFrequencyType.MONTHS, TWELVE_PERCENT,
                WeeksInYearType.WEEKS_52)));
        assertEquals(0, new BigDecimal("144").compareTo(this.aprCalculator.calculateFrom(PeriodFrequencyType.MONTHS, TWELVE_PERCENT,
                WeeksInYearType.WEEKS_48)));
    }

    @Test
    public void weekQuotedRateUsesTheSameWeekCount() {
        assertEquals(0, new BigDecimal("156").compareTo(this.aprCalculator.calculateFrom(PeriodFrequencyType.WEEKS, THREE_PERCENT,
                WeeksInYearType.WEEKS_52)));
        assertEquals(0, new BigDecimal("144").compareTo(this.aprCalculator.calculateFrom(PeriodFrequencyType.WEEKS, THREE_PERCENT,
                WeeksInYearType.WEEKS_48)));
    }

    @Test
    public void missingOrUnknownWeekCountIsFiftyTwo() {
        assertEquals(WeeksInYearType.WEEKS_52, WeeksInYearType.fromInt(null));
        assertEquals(WeeksInYearType.WEEKS_52, WeeksInYearType.fromInt(99));
        assertEquals(WeeksInYearType.WEEKS_48, WeeksInYearType.fromInt(48));
    }

    @Test
    public void twoHundredDollarLoanForEightWeeks() throws Exception {
        final LoanApplicationTerms fiftyTwoWeeks = weeklyFlatTerms(WeeksInYearType.WEEKS_52, InterestCalculationPeriodMethod.SAME_AS_REPAYMENT_PERIOD,
                DaysInYearType.DAYS_365);
        final LoanApplicationTerms fourWeekMonth = weeklyFlatTerms(WeeksInYearType.WEEKS_48, InterestCalculationPeriodMethod.SAME_AS_REPAYMENT_PERIOD,
                DaysInYearType.DAYS_365);

        assertEquals(52L, periodsInOneYear(fiftyTwoWeeks));
        assertEquals(48L, periodsInOneYear(fourWeekMonth));

        assertInstallments(fiftyTwoWeeks, "44.31", "5.54", "5.53");
        assertInstallments(fourWeekMonth, "48.00", "6.00", "6.00");
    }

    @Test
    public void dailyInterestKeepsDaysInYear() throws Exception {
        final LoanApplicationTerms terms = weeklyFlatTerms(WeeksInYearType.WEEKS_48, InterestCalculationPeriodMethod.DAILY,
                DaysInYearType.DAYS_360);
        assertEquals(360L, periodsInOneYear(terms));
    }

    @Test
    public void fourWeekMonthIsRejectedWhenWeeklyInterestIsDaily() {
        final LoanProductDataValidator validator = new LoanProductDataValidator(new FromJsonHelper());
        final PlatformApiDataValidationException exception = validationErrors(validator,
                "{\"locale\":\"en\",\"weeksInYearType\":48,\"repaymentFrequencyType\":1,\"interestCalculationPeriodType\":0}");
        assertTrue(hasErrorCode(exception.getErrors(), "weeksInYearType.cannot.be.used.when.interest.is.calculated.daily"));
    }

    @Test
    public void omittedWeekCountDoesNotRejectDailyWeeklyInterest() {
        final LoanProductDataValidator validator = new LoanProductDataValidator(new FromJsonHelper());
        final PlatformApiDataValidationException exception = validationErrors(validator,
                "{\"locale\":\"en\",\"repaymentFrequencyType\":1,\"interestCalculationPeriodType\":0}");
        assertFalse(hasErrorCode(exception.getErrors(), "weeksInYearType.cannot.be.used.when.interest.is.calculated.daily"));
    }

    @Test
    public void weekCountMustBeFiftyTwoOrFortyEight() {
        final LoanProductDataValidator validator = new LoanProductDataValidator(new FromJsonHelper());
        final PlatformApiDataValidationException exception = validationErrors(validator,
                "{\"locale\":\"en\",\"weeksInYearType\":99,\"repaymentFrequencyType\":1,\"interestCalculationPeriodType\":1}");
        assertTrue(hasErrorCode(exception.getErrors(), "weeksInYearType.is.not.one.of.expected.enumerations"));
    }

    private void assertInstallments(final LoanApplicationTerms terms, final String total, final String regularWeek, final String lastWeek) {
        final PaymentPeriodsInOneYearCalculator calculator = new DefaultPaymentPeriodsInOneYearCalculator();
        final Money totalInterest = terms.calculateTotalInterestCharged(calculator, MATH_CONTEXT);
        assertEquals(0, new BigDecimal(total).compareTo(totalInterest.getAmount()));

        Money cumulative = totalInterest.zero();
        Money firstWeek = null;
        Money finalWeek = null;
        for (int period = 1; period <= 8; period++) {
            Money installment = terms.calculateTotalInterestForPeriod(calculator, 0, period, MATH_CONTEXT, totalInterest.zero(),
                    terms.getPrincipal(), null, null).interest();
            cumulative = cumulative.plus(installment);
            installment = terms.adjustInterestIfLastRepaymentPeriod(installment, cumulative, totalInterest, period);
            if (period == 1) {
                firstWeek = installment;
            }
            if (period == 8) {
                finalWeek = installment;
            }
        }
        assertEquals(0, new BigDecimal(regularWeek).compareTo(firstWeek.getAmount()));
        assertEquals(0, new BigDecimal(lastWeek).compareTo(finalWeek.getAmount()));
    }

    private long periodsInOneYear(final LoanApplicationTerms terms) throws Exception {
        final Method method = LoanApplicationTerms.class.getDeclaredMethod("calculatePeriodsInOneYear", PaymentPeriodsInOneYearCalculator.class);
        method.setAccessible(true);
        return (Long) method.invoke(terms, new DefaultPaymentPeriodsInOneYearCalculator());
    }

    private LoanApplicationTerms weeklyFlatTerms(final WeeksInYearType weeksInYearType,
            final InterestCalculationPeriodMethod interestCalculationPeriodMethod, final DaysInYearType daysInYearType) {
        final LocalDate disbursementDate = new LocalDate(2026, 1, 5);
        final Money principal = Money.of(this.dollars, new BigDecimal("200"));
        return LoanApplicationTerms.assembleFrom(null, 8, PeriodFrequencyType.WEEKS, 8, 1, PeriodFrequencyType.WEEKS, null, null,
                AmortizationMethod.EQUAL_PRINCIPAL, InterestMethod.FLAT, TWELVE_PERCENT, PeriodFrequencyType.MONTHS, new BigDecimal("144"),
                interestCalculationPeriodMethod, false, principal, disbursementDate, null, null, 0, 0, 0, 0, null,
                principal.zero(), false, null, null, null, 0, DaysInMonthType.ACTUAL, daysInYearType, weeksInYearType, false, null, null,
                null, null, null, null, null, null, null, BigDecimal.ZERO, null, Boolean.FALSE, null, false, null, false, false);
    }

    private PlatformApiDataValidationException validationErrors(final LoanProductDataValidator validator, final String json) {
        try {
            validator.validateForCreate(json);
        } catch (final PlatformApiDataValidationException exception) {
            return exception;
        }
        throw new AssertionError("Expected validation errors");
    }

    private boolean hasErrorCode(final List<ApiParameterError> errors, final String codeSuffix) {
        for (final ApiParameterError error : errors) {
            if (error.getUserMessageGlobalisationCode() != null && error.getUserMessageGlobalisationCode().endsWith(codeSuffix)) {
                return true;
            }
        }
        return false;
    }
}
