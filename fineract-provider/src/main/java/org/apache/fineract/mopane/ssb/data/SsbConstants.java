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
package org.apache.fineract.mopane.ssb.data;

public final class SsbConstants {

    public static final String RESOURCE_EXPORT = "SSB_DEDUCTION";
    public static final String RESOURCE_PAY_IMPORT = "SSB_PAY_IMPORT";

    public static final String BUREAU_SSB = "SSB";
    public static final String BUREAU_PENSION = "PENSION";

    public static final String DT_CLIENT_DETAILS = "ssb_client_details";
    public static final String COL_ID_NUMBER = "IdNumber";
    public static final String COL_EC_NUMBER = "EcNumber";

    public static final String TYPE_NEW = "NEW";
    public static final String TYPE_CHANGE = "CHANGE";
    public static final String TYPE_DELETE = "DELETE";

    public static final String TYPE_NEW_SHORT = "N";
    public static final String TYPE_CHANGE_SHORT = "C";
    public static final String TYPE_DELETE_SHORT = "D";

    public static final String STATUS_POSTED = "POSTED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_NEEDS_REVIEW = "NEEDS_REVIEW";
    public static final String STATUS_REJECTED = "REJECTED";

    public static final String REASON_REFERENCE_MISSING = "REFERENCE_MISSING";
    public static final String REASON_REFERENCE_NOT_FOUND = "REFERENCE_NOT_FOUND";
    public static final String REASON_LIKELY_MATCH_ID_EC = "LIKELY_MATCH_ID_EC";
    public static final String REASON_AMBIGUOUS_ID_EC = "AMBIGUOUS_ID_EC";
    public static final String REASON_ALREADY_POSTED = "ALREADY_POSTED";
    public static final String REASON_INVALID_AMOUNT = "INVALID_AMOUNT";
    public static final String REASON_INVALID_DATE = "INVALID_DATE";
    public static final String REASON_LOAN_NOT_REPAYABLE = "LOAN_NOT_REPAYABLE";

    public static final int LOAN_STATUS_APPROVED = 200;
    public static final int LOAN_STATUS_ACTIVE = 300;

    public static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String HEADER_BATCH_ID = "X-SSB-Import-Batch-Id";

    public static final String DATE_FORMAT_SSB = "dd/MMM/yyyy";
    public static final String DATE_FORMAT_API = "dd MMMM yyyy";
    public static final String LOCALE = "en";

    private SsbConstants() {}
}
