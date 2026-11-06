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
package org.apache.fineract.mopane.dashboard.data;

public final class MopaneDashboardApiConstants {

    public static final String RESOURCE_NAME = "MOPANE_DASHBOARD";

    public static final String TREND_PERIOD_DAY = "day";
    public static final String TREND_PERIOD_WEEK = "week";
    public static final String TREND_PERIOD_MONTH = "month";

    public static final int DEFAULT_ACTIVITY_LIMIT = 20;
    public static final int MAX_ACTIVITY_LIMIT = 50;

    public static final String AGING_CURRENT = "CURRENT";
    public static final String AGING_1_30 = "1_30";
    public static final String AGING_31_60 = "31_60";
    public static final String AGING_61_90 = "61_90";
    public static final String AGING_90_PLUS = "90_PLUS";

    private MopaneDashboardApiConstants() {}
}
