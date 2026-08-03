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

public final class StaffMobileDeviceApiConstants {

    public static final String RESOURCE_NAME = "STAFFMOBILEDEVICE";

    public static final String userIdParamName = "userId";
    public static final String expiresInHoursParamName = "expiresInHours";
    public static final String activationCodeParamName = "activationCode";
    public static final String fcmTokenParamName = "fcmToken";
    public static final String deviceUidParamName = "deviceUid";
    public static final String platformParamName = "platform";
    public static final String modelParamName = "model";
    public static final String appVersionParamName = "appVersion";
    public static final String locationsParamName = "locations";
    public static final String latitudeParamName = "latitude";
    public static final String longitudeParamName = "longitude";
    public static final String accuracyMetersParamName = "accuracyMeters";
    public static final String recordedOnParamName = "recordedOn";

    public static final int DEFAULT_CODE_EXPIRY_HOURS = 24;
    public static final int ACTIVATION_CODE_LENGTH = 8;
    public static final int MAX_LOCATION_BATCH_SIZE = 100;

    public static final String REVOKE_COMMAND = "revoke";

    private StaffMobileDeviceApiConstants() {}
}
