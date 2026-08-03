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

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;

@Entity
@Table(name = "m_staff_location_ping")
public class StaffLocationPing extends AbstractPersistableCustom<Long> {

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "appuser_id", nullable = false)
    private Long appUserId;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "accuracy_meters", precision = 10, scale = 2)
    private BigDecimal accuracyMeters;

    @Column(name = "recorded_on_utc", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date recordedOnUtc;

    @Column(name = "received_on_utc", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date receivedOnUtc;

    protected StaffLocationPing() {}

    private StaffLocationPing(final Long deviceId, final Long appUserId, final BigDecimal latitude, final BigDecimal longitude,
            final BigDecimal accuracyMeters, final Date recordedOnUtc) {
        this.deviceId = deviceId;
        this.appUserId = appUserId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
        this.recordedOnUtc = recordedOnUtc;
        this.receivedOnUtc = DateUtils.getLocalDateTimeOfTenant().toDate();
    }

    public static StaffLocationPing create(final Long deviceId, final Long appUserId, final BigDecimal latitude,
            final BigDecimal longitude, final BigDecimal accuracyMeters, final Date recordedOnUtc) {
        return new StaffLocationPing(deviceId, appUserId, latitude, longitude, accuracyMeters, recordedOnUtc);
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
