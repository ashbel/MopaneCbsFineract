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
package org.apache.fineract.mopane.stafftarget.data;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.staff.data.StaffData;

public class StaffMonthlyTargetData {

    private final Long id;
    private final Long staffId;
    private final String staffDisplayName;
    private final Long officeId;
    private final String officeName;
    private final String yearMonth;
    private final String currencyCode;
    private final BigDecimal collectionsTargetAmount;
    private final BigDecimal disbursementsTargetAmount;
    private final Integer newClientsTarget;
    private final Long createdById;
    private final Date createdOnUtc;
    private final Long updatedById;
    private final Date updatedOnUtc;

    private final Collection<StaffData> staffOptions;
    private final List<CurrencyData> currencyOptions;

    private StaffMonthlyTargetData(final Long id, final Long staffId, final String staffDisplayName, final Long officeId,
            final String officeName, final String yearMonth, final String currencyCode, final BigDecimal collectionsTargetAmount,
            final BigDecimal disbursementsTargetAmount, final Integer newClientsTarget, final Long createdById, final Date createdOnUtc,
            final Long updatedById, final Date updatedOnUtc, final Collection<StaffData> staffOptions,
            final List<CurrencyData> currencyOptions) {
        this.id = id;
        this.staffId = staffId;
        this.staffDisplayName = staffDisplayName;
        this.officeId = officeId;
        this.officeName = officeName;
        this.yearMonth = yearMonth;
        this.currencyCode = currencyCode;
        this.collectionsTargetAmount = collectionsTargetAmount;
        this.disbursementsTargetAmount = disbursementsTargetAmount;
        this.newClientsTarget = newClientsTarget;
        this.createdById = createdById;
        this.createdOnUtc = createdOnUtc;
        this.updatedById = updatedById;
        this.updatedOnUtc = updatedOnUtc;
        this.staffOptions = staffOptions;
        this.currencyOptions = currencyOptions;
    }

    public static StaffMonthlyTargetData instance(final Long id, final Long staffId, final String staffDisplayName, final Long officeId,
            final String officeName, final String yearMonth, final String currencyCode, final BigDecimal collectionsTargetAmount,
            final BigDecimal disbursementsTargetAmount, final Integer newClientsTarget, final Long createdById, final Date createdOnUtc,
            final Long updatedById, final Date updatedOnUtc) {
        return new StaffMonthlyTargetData(id, staffId, staffDisplayName, officeId, officeName, yearMonth, currencyCode,
                collectionsTargetAmount, disbursementsTargetAmount, newClientsTarget, createdById, createdOnUtc, updatedById, updatedOnUtc,
                null, null);
    }

    public static StaffMonthlyTargetData template(final Collection<StaffData> staffOptions, final List<CurrencyData> currencyOptions) {
        return new StaffMonthlyTargetData(null, null, null, null, null, null, null, null, null, null, null, null, null, null, staffOptions,
                currencyOptions);
    }

    public Long getId() {
        return this.id;
    }

    public Long getStaffId() {
        return this.staffId;
    }

    public String getStaffDisplayName() {
        return this.staffDisplayName;
    }

    public Long getOfficeId() {
        return this.officeId;
    }

    public String getOfficeName() {
        return this.officeName;
    }

    public String getYearMonth() {
        return this.yearMonth;
    }

    public String getCurrencyCode() {
        return this.currencyCode;
    }

    public BigDecimal getCollectionsTargetAmount() {
        return this.collectionsTargetAmount;
    }

    public BigDecimal getDisbursementsTargetAmount() {
        return this.disbursementsTargetAmount;
    }

    public Integer getNewClientsTarget() {
        return this.newClientsTarget;
    }

    public Long getCreatedById() {
        return this.createdById;
    }

    public Date getCreatedOnUtc() {
        return this.createdOnUtc;
    }

    public Long getUpdatedById() {
        return this.updatedById;
    }

    public Date getUpdatedOnUtc() {
        return this.updatedOnUtc;
    }

    public Collection<StaffData> getStaffOptions() {
        return this.staffOptions;
    }

    public List<CurrencyData> getCurrencyOptions() {
        return this.currencyOptions;
    }
}
