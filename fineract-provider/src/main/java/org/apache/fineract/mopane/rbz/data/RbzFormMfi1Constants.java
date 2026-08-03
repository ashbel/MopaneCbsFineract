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
package org.apache.fineract.mopane.rbz.data;

public final class RbzFormMfi1Constants {

    public static final String RESOURCE_NAME = "RBZ_FORM_MFI1";
    public static final String TEMPLATE_RESOURCE = "/rbz/FORM_MFI1.xlsx";
    public static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public static final String SHEET_INCOME = "COMPREHENSIVE INCOME";
    public static final String SHEET_SFP = "STATEMENT OF FIN POSITION";
    public static final String SHEET_INSIDER = "SCHEDULE 1 INSIDER LOANS";
    public static final String SHEET_LT_DEBT = "SCHEDULE 2 LONG TERM DEB";
    public static final String SHEET_ASSET_QUALITY = "SCHEDULE 3 ASSET QUALITY";
    public static final String SHEET_DISTRIBUTION = "SCHEDULE 4 DISTRIBUTION ";
    public static final String SHEET_GENDER = "SCHEDULE 5 GENDER DISTRIBUTION";
    public static final String SHEET_MATURITY = "SCHEDULE 6 MATURITY PROF";
    public static final String SHEET_TOP20 = "SCHEDULE 7 TOP 20 BORROW";
    public static final String SHEET_DISTRICT = "SCHEDULE 8 - ACCESS BY DISTRICT";
    public static final String SHEET_PORTFOLIO = "SCHEDULE 9 PORT MNGMNT";
    public static final String SHEET_PROFILE = "SCHEDULE 9 CO. PROFILE";

    public static final String DT_LOAN_DETAILS = "rbz_loan_details";
    public static final String DT_OFFICE_CHANNELS = "rbz_office_channels";
    public static final String DT_INSTITUTION_PROFILE = "rbz_institution_profile";
    public static final String DT_LONG_TERM_DEBT = "rbz_long_term_debt";

    public static final String CODE_LOAN_CLASS = "RbzLoanClass";
    public static final String CODE_RELATED_PARTY = "RbzRelatedPartyType";
    public static final String CODE_LOCATION_TYPE = "RbzLocationType";

    public static final String[] RBZ_PURPOSES = new String[] { "Manufacturing", "Retail", "Consumption", "Services", "Health",
            "Education", "Mining", "Agriculture", "Cross Border Traders", "Vendors", "Funeral Assistance", "Other" };

    private RbzFormMfi1Constants() {}
}
