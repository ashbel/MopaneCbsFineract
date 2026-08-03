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

import java.util.Date;

public class StaffMobileActivationCodeData {

    private final Long id;
    private final Long appUserId;
    private final String username;
    private final String userDisplayName;
    private final String code;
    private final Date expiresOnUtc;
    private final String status;
    private final Date usedOnUtc;
    private final Long usedByDeviceId;
    private final Long createdById;
    private final String createdByUsername;
    private final Date createdOnUtc;

    private StaffMobileActivationCodeData(final Long id, final Long appUserId, final String username, final String userDisplayName,
            final String code, final Date expiresOnUtc, final String status, final Date usedOnUtc, final Long usedByDeviceId,
            final Long createdById, final String createdByUsername, final Date createdOnUtc) {
        this.id = id;
        this.appUserId = appUserId;
        this.username = username;
        this.userDisplayName = userDisplayName;
        this.code = code;
        this.expiresOnUtc = expiresOnUtc;
        this.status = status;
        this.usedOnUtc = usedOnUtc;
        this.usedByDeviceId = usedByDeviceId;
        this.createdById = createdById;
        this.createdByUsername = createdByUsername;
        this.createdOnUtc = createdOnUtc;
    }

    public static StaffMobileActivationCodeData instance(final Long id, final Long appUserId, final String username,
            final String userDisplayName, final String code, final Date expiresOnUtc, final String status, final Date usedOnUtc,
            final Long usedByDeviceId, final Long createdById, final String createdByUsername, final Date createdOnUtc) {
        return new StaffMobileActivationCodeData(id, appUserId, username, userDisplayName, code, expiresOnUtc, status, usedOnUtc,
                usedByDeviceId, createdById, createdByUsername, createdOnUtc);
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

    public String getCreatedByUsername() {
        return this.createdByUsername;
    }

    public Date getCreatedOnUtc() {
        return this.createdOnUtc;
    }
}
