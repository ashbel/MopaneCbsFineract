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
package org.apache.fineract.mopane.mobiledevice.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;

@Entity
@Table(name = "m_staff_mobile_activation_code", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "code" }, name = "uk_staff_mobile_activation_code") })
public class StaffMobileActivationCode extends AbstractPersistableCustom<Long> {

    @Column(name = "appuser_id", nullable = false)
    private Long appUserId;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "expires_on_utc", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiresOnUtc;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "used_on_utc")
    @Temporal(TemporalType.TIMESTAMP)
    private Date usedOnUtc;

    @Column(name = "used_by_device_id")
    private Long usedByDeviceId;

    @Column(name = "createdby_id", nullable = false)
    private Long createdById;

    @Column(name = "created_on_utc", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdOnUtc;

    protected StaffMobileActivationCode() {}

    private StaffMobileActivationCode(final Long appUserId, final String code, final Date expiresOnUtc, final Long createdById) {
        this.appUserId = appUserId;
        this.code = code;
        this.expiresOnUtc = expiresOnUtc;
        this.status = StaffMobileActivationCodeStatus.ACTIVE;
        this.createdById = createdById;
        this.createdOnUtc = DateUtils.getLocalDateTimeOfTenant().toDate();
    }

    public static StaffMobileActivationCode create(final Long appUserId, final String code, final Date expiresOnUtc,
            final Long createdById) {
        return new StaffMobileActivationCode(appUserId, code, expiresOnUtc, createdById);
    }

    public void markUsed(final Long deviceId) {
        this.status = StaffMobileActivationCodeStatus.USED;
        this.usedByDeviceId = deviceId;
        this.usedOnUtc = DateUtils.getLocalDateTimeOfTenant().toDate();
    }

    public void revoke() {
        this.status = StaffMobileActivationCodeStatus.REVOKED;
    }

    public void markExpired() {
        this.status = StaffMobileActivationCodeStatus.EXPIRED;
    }

    public boolean isActiveAndNotExpired() {
        if (!StaffMobileActivationCodeStatus.ACTIVE.equals(this.status)) {
            return false;
        }
        final Date now = DateUtils.getLocalDateTimeOfTenant().toDate();
        return this.expiresOnUtc != null && !this.expiresOnUtc.before(now);
    }

    public Long getAppUserId() {
        return this.appUserId;
    }

    public String getCode() {
        return this.code;
    }

    public Date getExpiresOnUtc() {
        return this.expiresOnUtc;
    }

    public String getStatus() {
        return this.status;
    }

    public Date getUsedOnUtc() {
        return this.usedOnUtc;
    }

    public Long getUsedByDeviceId() {
        return this.usedByDeviceId;
    }

    public Long getCreatedById() {
        return this.createdById;
    }

    public Date getCreatedOnUtc() {
        return this.createdOnUtc;
    }
}
