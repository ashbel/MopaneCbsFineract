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
package org.apache.fineract.portfolio.loanaccount.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeCalculationType;
import org.apache.fineract.portfolio.charge.domain.ChargePaymentMode;
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for Mambu-style capitalised fee flags on {@link LoanCharge}.
 */
public class LoanCapitalizedFeeTest {

    @Before
    public void setUpMoneyRoundingMode() throws Exception {
        Field field = MoneyHelper.class.getDeclaredField("roundingMode");
        field.setAccessible(true);
        field.set(null, RoundingMode.HALF_EVEN);
    }

    @Test
    public void disbursementCapitalisedChargeTimeIsPrincipalCapitalizing() {
        Charge charge = Mockito.mock(Charge.class);
        Mockito.when(charge.getChargeTimeType()).thenReturn(ChargeTimeType.DISBURSEMENT_CAPITALISED.getValue());
        Mockito.when(charge.getChargeCalculation()).thenReturn(ChargeCalculationType.FLAT.getValue());
        Mockito.when(charge.getChargePaymentMode()).thenReturn(ChargePaymentMode.REGULAR.getValue());
        Mockito.when(charge.isPenalty()).thenReturn(false);
        Mockito.when(charge.getMinCap()).thenReturn(null);
        Mockito.when(charge.getMaxCap()).thenReturn(null);
        Mockito.when(charge.getAmount()).thenReturn(BigDecimal.TEN);

        LoanCharge lc = LoanCharge.createNewWithoutLoan(charge, new BigDecimal("1000"), BigDecimal.TEN, null, null, null, null, null);
        assertTrue(lc.isPrincipalCapitalizingFee());
        assertTrue(lc.isCapitalisedAtDisbursement());
    }

    @Test
    public void isCapitalizedFlagMakesSpecifiedDueDatePrincipalCapitalizing() {
        Charge charge = Mockito.mock(Charge.class);
        Mockito.when(charge.getChargeTimeType()).thenReturn(ChargeTimeType.SPECIFIED_DUE_DATE.getValue());
        Mockito.when(charge.getChargeCalculation()).thenReturn(ChargeCalculationType.FLAT.getValue());
        Mockito.when(charge.getChargePaymentMode()).thenReturn(ChargePaymentMode.REGULAR.getValue());
        Mockito.when(charge.isPenalty()).thenReturn(false);
        Mockito.when(charge.getMinCap()).thenReturn(null);
        Mockito.when(charge.getMaxCap()).thenReturn(null);
        Mockito.when(charge.getAmount()).thenReturn(BigDecimal.TEN);

        LoanCharge lc = LoanCharge.createNewWithoutLoan(charge, new BigDecimal("1000"), BigDecimal.TEN, null, null,
                LocalDate.parse("2025-06-01"), null, null);
        assertFalse(lc.isPrincipalCapitalizingFee());
        lc.setCapitalized(true);
        assertTrue(lc.isPrincipalCapitalizingFee());
    }

    @Test
    public void capitalizedFeeTransactionCarriesPrincipalPortion() {
        org.apache.fineract.organisation.office.domain.Office office = Mockito
                .mock(org.apache.fineract.organisation.office.domain.Office.class);
        Loan loan = Mockito.mock(Loan.class);
        MonetaryCurrency usd = new MonetaryCurrency("USD", 2, 0);
        Money hundred = Money.of(usd, new BigDecimal("100"));
        LoanTransaction tx = LoanTransaction.capitalizedFee(loan, office, hundred, LocalDate.parse("2025-03-01"),
                org.apache.fineract.infrastructure.core.service.DateUtils.getLocalDateTimeOfTenant(), null);
        assertTrue(tx.isCapitalisedFee());
        assertTrue(tx.getPrincipalPortion(usd).isEqualTo(hundred));
    }
}
