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
@Table(name = "m_staff_mobile_device", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "appuser_id", "device_uid" }, name = "uk_staff_mobile_device_user_uid"),
        @UniqueConstraint(columnNames = { "fcm_token" }, name = "uk_staff_mobile_device_fcm_token") })
public class StaffMobileDevice extends AbstractPersistableCustom<Long> {

    @Column(name = "appuser_id", nullable = false)
    private Long appUserId;

    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "fcm_token", length = 255)
    private String fcmToken;

    @Column(name = "device_uid", nullable = false, length = 100)
    private String deviceUid;

    @Column(name = "platform", length = 50)
    private String platform;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "app_version", length = 50)
    private String appVersion;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "activated_on_utc")
    @Temporal(TemporalType.TIMESTAMP)
    private Date activatedOnUtc;

    @Column(name = "last_seen_on_utc")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastSeenOnUtc;

    @Column(name = "created_on_utc", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdOnUtc;

    @Column(name = "updated_on_utc", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedOnUtc;

    protected StaffMobileDevice() {}

    private StaffMobileDevice(final Long appUserId, final Long staffId, final String deviceUid, final String platform, final String model,
            final String appVersion, final String fcmToken, final String status) {
        final Date now = DateUtils.getLocalDateTimeOfTenant().toDate();
        this.appUserId = appUserId;
        this.staffId = staffId;
        this.deviceUid = deviceUid;
        this.platform = platform;
        this.model = model;
        this.appVersion = appVersion;
        this.fcmToken = fcmToken;
        this.status = status;
        this.createdOnUtc = now;
        this.updatedOnUtc = now;
        if (StaffMobileDeviceStatus.ACTIVE.equals(status)) {
            this.activatedOnUtc = now;
            this.lastSeenOnUtc = now;
        }
    }

    public static StaffMobileDevice createActive(final Long appUserId, final Long staffId, final String deviceUid, final String platform,
            final String model, final String appVersion, final String fcmToken) {
        return new StaffMobileDevice(appUserId, staffId, deviceUid, platform, model, appVersion, fcmToken, StaffMobileDeviceStatus.ACTIVE);
    }

    public void activate(final String fcmToken, final String platform, final String model, final String appVersion, final Long staffId) {
        final Date now = DateUtils.getLocalDateTimeOfTenant().toDate();
        this.fcmToken = fcmToken;
        this.platform = platform;
        this.model = model;
        this.appVersion = appVersion;
        this.staffId = staffId;
        this.status = StaffMobileDeviceStatus.ACTIVE;
        this.activatedOnUtc = now;
        this.lastSeenOnUtc = now;
        this.updatedOnUtc = now;
    }

    public void updateFcmToken(final String fcmToken) {
        final Date now = DateUtils.getLocalDateTimeOfTenant().toDate();
        this.fcmToken = fcmToken;
        this.lastSeenOnUtc = now;
        this.updatedOnUtc = now;
    }

    public void touchLastSeen() {
        final Date now = DateUtils.getLocalDateTimeOfTenant().toDate();
        this.lastSeenOnUtc = now;
        this.updatedOnUtc = now;
    }

    public void revoke() {
        final Date now = DateUtils.getLocalDateTimeOfTenant().toDate();
        this.status = StaffMobileDeviceStatus.REVOKED;
        this.fcmToken = null;
        this.updatedOnUtc = now;
    }

    public void clearInvalidToken() {
        final Date now = DateUtils.getLocalDateTimeOfTenant().toDate();
        this.fcmToken = null;
        this.updatedOnUtc = now;
    }

    public Long getAppUserId() {
        return this.appUserId;
    }

    public Long getStaffId() {
        return this.staffId;
    }

    public String getFcmToken() {
        return this.fcmToken;
    }

    public String getDeviceUid() {
        return this.deviceUid;
    }

    public String getPlatform() {
        return this.platform;
    }

    public String getModel() {
        return this.model;
    }

    public String getAppVersion() {
        return this.appVersion;
    }

    public String getStatus() {
        return this.status;
    }

    public Date getActivatedOnUtc() {
        return this.activatedOnUtc;
    }

    public Date getLastSeenOnUtc() {
        return this.lastSeenOnUtc;
    }

    public Date getCreatedOnUtc() {
        return this.createdOnUtc;
    }

    public Date getUpdatedOnUtc() {
        return this.updatedOnUtc;
    }

    public boolean isActive() {
        return StaffMobileDeviceStatus.ACTIVE.equals(this.status);
    }
}
