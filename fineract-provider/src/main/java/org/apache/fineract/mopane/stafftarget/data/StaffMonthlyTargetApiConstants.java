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
package org.apache.fineract.mopane.stafftarget.data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class StaffMonthlyTargetApiConstants {

    public static final String RESOURCE_NAME = "STAFFMONTHLYTARGET";

    public static final String staffIdParamName = "staffId";
    public static final String yearMonthParamName = "yearMonth";
    public static final String currencyCodeParamName = "currencyCode";
    public static final String collectionsTargetAmountParamName = "collectionsTargetAmount";
    public static final String disbursementsTargetAmountParamName = "disbursementsTargetAmount";
    public static final String newClientsTargetParamName = "newClientsTarget";

    public static final Set<String> CREATE_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(staffIdParamName, yearMonthParamName,
            currencyCodeParamName, collectionsTargetAmountParamName, disbursementsTargetAmountParamName, newClientsTargetParamName));

    public static final Set<String> UPDATE_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(collectionsTargetAmountParamName,
            disbursementsTargetAmountParamName, newClientsTargetParamName, currencyCodeParamName, yearMonthParamName));

    private StaffMonthlyTargetApiConstants() {}
}
