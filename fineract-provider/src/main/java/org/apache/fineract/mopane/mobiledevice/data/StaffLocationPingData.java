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

public class StaffLocationPingData {

    private final Long id;
    private final Long deviceId;
    private final Long appUserId;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final BigDecimal accuracyMeters;
    private final Date recordedOnUtc;
    private final Date receivedOnUtc;

    private StaffLocationPingData(final Long id, final Long deviceId, final Long appUserId, final BigDecimal latitude,
            final BigDecimal longitude, final BigDecimal accuracyMeters, final Date recordedOnUtc, final Date receivedOnUtc) {
        this.id = id;
        this.deviceId = deviceId;
        this.appUserId = appUserId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
        this.recordedOnUtc = recordedOnUtc;
        this.receivedOnUtc = receivedOnUtc;
    }

    public static StaffLocationPingData instance(final Long id, final Long deviceId, final Long appUserId, final BigDecimal latitude,
            final BigDecimal longitude, final BigDecimal accuracyMeters, final Date recordedOnUtc, final Date receivedOnUtc) {
        return new StaffLocationPingData(id, deviceId, appUserId, latitude, longitude, accuracyMeters, recordedOnUtc, receivedOnUtc);
    }

    public Long getId() {
        return this.id;
    }

    public Long getDeviceId() {
        return this.deviceId;
    }

    public Long getAppUserId() {
        return this.appUserId;
    }

    public BigDecimal getLatitude() {
        return this.latitude;
    }

    public BigDecimal getLongitude() {
        return this.longitude;
    }

    public BigDecimal getAccuracyMeters() {
        return this.accuracyMeters;
    }

    public Date getRecordedOnUtc() {
        return this.recordedOnUtc;
    }

    public Date getReceivedOnUtc() {
        return this.receivedOnUtc;
    }
}
