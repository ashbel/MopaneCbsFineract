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
package org.apache.fineract.portfolio.common.domain;

/**
 * How many weeks a year has when a rate is turned into a weekly charge.
 * <ul>
 * <li>52 — a year is 52 weeks</li>
 * <li>48 — a month is 4 weeks</li>
 * </ul>
 * A missing or unknown value is 52, which is the historical rule.
 */
public enum WeeksInYearType {

    WEEKS_52(52, "WeeksInYearType.weeks52"), //
    WEEKS_48(48, "WeeksInYearType.weeks48");

    private final Integer value;
    private final String code;

    private WeeksInYearType(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    public static WeeksInYearType fromInt(final Integer type) {
        if (type != null && WEEKS_48.value.equals(type)) { return WEEKS_48; }
        return WEEKS_52;
    }

    /**
     * A 4-week month is a count of scheduled weeks. Daily interest uses the
     * days-in-year setting instead, so the two cannot be combined on a weekly loan.
     */
    public static boolean isUnsupportedDailyWeekly(final Integer weeksInYearType, final boolean weeklyRepayment,
            final boolean dailyInterest) {
        return WEEKS_48.value.equals(weeksInYearType) && weeklyRepayment && dailyInterest;
    }
}
