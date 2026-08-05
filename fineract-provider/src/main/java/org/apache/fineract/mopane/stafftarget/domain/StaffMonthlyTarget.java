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
package org.apache.fineract.mopane.stafftarget.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;

@Entity
@Table(name = "m_staff_monthly_target", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "staff_id", "year_month", "currency_code" }, name = "uk_staff_monthly_target") })
public class StaffMonthlyTarget extends AbstractPersistableCustom<Long> {

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "office_id", nullable = false)
    private Long officeId;

    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "collections_target_amount", nullable = false, scale = 6, precision = 19)
    private BigDecimal collectionsTargetAmount;

    @Column(name = "disbursements_target_amount", nullable = false, scale = 6, precision = 19)
    private BigDecimal disbursementsTargetAmount;

    @Column(name = "new_clients_target", nullable = false)
    private Integer newClientsTarget;

    @Column(name = "createdby_id", nullable = false)
    private Long createdById;

    @Column(name = "created_on_utc", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdOnUtc;

    @Column(name = "updatedby_id")
    private Long updatedById;

    @Column(name = "updated_on_utc")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedOnUtc;

    protected StaffMonthlyTarget() {}

    private StaffMonthlyTarget(final Long staffId, final Long officeId, final String yearMonth, final String currencyCode,
            final BigDecimal collectionsTargetAmount, final BigDecimal disbursementsTargetAmount, final Integer newClientsTarget,
            final Long createdById) {
        final Date now = DateUtils.getLocalDateTimeOfTenant().toDate();
        this.staffId = staffId;
        this.officeId = officeId;
        this.yearMonth = yearMonth;
        this.currencyCode = currencyCode;
        this.collectionsTargetAmount = collectionsTargetAmount;
        this.disbursementsTargetAmount = disbursementsTargetAmount;
        this.newClientsTarget = newClientsTarget;
        this.createdById = createdById;
        this.createdOnUtc = now;
        this.updatedById = createdById;
        this.updatedOnUtc = now;
    }

    public static StaffMonthlyTarget create(final Long staffId, final Long officeId, final String yearMonth, final String currencyCode,
            final BigDecimal collectionsTargetAmount, final BigDecimal disbursementsTargetAmount, final Integer newClientsTarget,
            final Long createdById) {
        return new StaffMonthlyTarget(staffId, officeId, yearMonth, currencyCode, collectionsTargetAmount, disbursementsTargetAmount,
                newClientsTarget, createdById);
    }

    public Map<String, Object> update(final BigDecimal collectionsTargetAmount, final BigDecimal disbursementsTargetAmount,
            final Integer newClientsTarget, final String yearMonth, final String currencyCode, final Long updatedById) {
        final Map<String, Object> changes = new LinkedHashMap<>();
        if (collectionsTargetAmount != null && this.collectionsTargetAmount.compareTo(collectionsTargetAmount) != 0) {
            changes.put("collectionsTargetAmount", collectionsTargetAmount);
            this.collectionsTargetAmount = collectionsTargetAmount;
        }
        if (disbursementsTargetAmount != null && this.disbursementsTargetAmount.compareTo(disbursementsTargetAmount) != 0) {
            changes.put("disbursementsTargetAmount", disbursementsTargetAmount);
            this.disbursementsTargetAmount = disbursementsTargetAmount;
        }
        if (newClientsTarget != null && !newClientsTarget.equals(this.newClientsTarget)) {
            changes.put("newClientsTarget", newClientsTarget);
            this.newClientsTarget = newClientsTarget;
        }
        if (yearMonth != null && !yearMonth.equals(this.yearMonth)) {
            changes.put("yearMonth", yearMonth);
            this.yearMonth = yearMonth;
        }
        if (currencyCode != null && !currencyCode.equalsIgnoreCase(this.currencyCode)) {
            changes.put("currencyCode", currencyCode);
            this.currencyCode = currencyCode;
        }
        if (!changes.isEmpty()) {
            this.updatedById = updatedById;
            this.updatedOnUtc = DateUtils.getLocalDateTimeOfTenant().toDate();
        }
        return changes;
    }

    public Long getStaffId() {
        return this.staffId;
    }

    public Long getOfficeId() {
        return this.officeId;
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
}
