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
package org.apache.fineract.mopane.mobiledevice.data;

import java.math.BigDecimal;
import java.util.Date;

public class StaffMobileDeviceData {

    private final Long id;
    private final Long appUserId;
    private final String username;
    private final String userDisplayName;
    private final Long staffId;
    private final String staffDisplayName;
    private final Long officeId;
    private final String officeName;
    private final String deviceUid;
    private final String platform;
    private final String model;
    private final String appVersion;
    private final String status;
    private final boolean hasFcmToken;
    private final Date activatedOnUtc;
    private final Date lastSeenOnUtc;
    private final Date createdOnUtc;
    private final Date updatedOnUtc;
    private final BigDecimal lastLatitude;
    private final BigDecimal lastLongitude;
    private final Date lastLocationOnUtc;

    private StaffMobileDeviceData(final Long id, final Long appUserId, final String username, final String userDisplayName,
            final Long staffId, final String staffDisplayName, final Long officeId, final String officeName, final String deviceUid,
            final String platform, final String model, final String appVersion, final String status, final boolean hasFcmToken,
            final Date activatedOnUtc, final Date lastSeenOnUtc, final Date createdOnUtc, final Date updatedOnUtc,
            final BigDecimal lastLatitude, final BigDecimal lastLongitude, final Date lastLocationOnUtc) {
        this.id = id;
        this.appUserId = appUserId;
        this.username = username;
        this.userDisplayName = userDisplayName;
        this.staffId = staffId;
        this.staffDisplayName = staffDisplayName;
        this.officeId = officeId;
        this.officeName = officeName;
        this.deviceUid = deviceUid;
        this.platform = platform;
        this.model = model;
        this.appVersion = appVersion;
        this.status = status;
        this.hasFcmToken = hasFcmToken;
        this.activatedOnUtc = activatedOnUtc;
        this.lastSeenOnUtc = lastSeenOnUtc;
        this.createdOnUtc = createdOnUtc;
        this.updatedOnUtc = updatedOnUtc;
        this.lastLatitude = lastLatitude;
        this.lastLongitude = lastLongitude;
        this.lastLocationOnUtc = lastLocationOnUtc;
    }

    public static StaffMobileDeviceData instance(final Long id, final Long appUserId, final String username, final String userDisplayName,
            final Long staffId, final String staffDisplayName, final Long officeId, final String officeName, final String deviceUid,
            final String platform, final String model, final String appVersion, final String status, final boolean hasFcmToken,
            final Date activatedOnUtc, final Date lastSeenOnUtc, final Date createdOnUtc, final Date updatedOnUtc,
            final BigDecimal lastLatitude, final BigDecimal lastLongitude, final Date lastLocationOnUtc) {
        return new StaffMobileDeviceData(id, appUserId, username, userDisplayName, staffId, staffDisplayName, officeId, officeName,
                deviceUid, platform, model, appVersion, status, hasFcmToken, activatedOnUtc, lastSeenOnUtc, createdOnUtc, updatedOnUtc,
                lastLatitude, lastLongitude, lastLocationOnUtc);
    }

    public Long getId() {
        return this.id;
    }

    public Long getAppUserId() {
        return this.appUserId;
    }

    public String getUsername() {
        return this.username;
    }

    public String getUserDisplayName() {
        return this.userDisplayName;
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

    public boolean isHasFcmToken() {
        return this.hasFcmToken;
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

    public BigDecimal getLastLatitude() {
        return this.lastLatitude;
    }

    public BigDecimal getLastLongitude() {
        return this.lastLongitude;
    }

    public Date getLastLocationOnUtc() {
        return this.lastLocationOnUtc;
    }
}
