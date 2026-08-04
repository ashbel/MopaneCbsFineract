--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements. See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership. The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License. You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied. See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

-- RBZ FORM_MFI1 supporting codes + datatables.
-- Dropdown physical columns follow Fineract naming: {CodeName}_cd_{fieldName}
-- (constraint_approach_for_datatables disabled).

-- ---------------------------------------------------------------------------
-- Codes
-- ---------------------------------------------------------------------------
INSERT INTO m_code (code_name, is_system_defined)
SELECT 'RbzLoanClass', 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM m_code WHERE code_name = 'RbzLoanClass');

INSERT INTO m_code (code_name, is_system_defined)
SELECT 'RbzRelatedPartyType', 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM m_code WHERE code_name = 'RbzRelatedPartyType');

INSERT INTO m_code (code_name, is_system_defined)
SELECT 'RbzLocationType', 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM m_code WHERE code_name = 'RbzLocationType');

INSERT INTO m_code (code_name, is_system_defined)
SELECT 'LoanPurpose', 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM m_code WHERE code_name = 'LoanPurpose');

INSERT INTO m_code (code_name, is_system_defined)
SELECT 'Gender', 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM m_code WHERE code_name = 'Gender');

-- RbzLoanClass values
INSERT INTO m_code_value (code_id, code_value, order_position, code_score, is_active, is_mandatory)
SELECT c.id, v.code_value, v.ord, NULL, 1, 0
FROM m_code c
JOIN (
  SELECT 'Consumer' AS code_value, 1 AS ord UNION ALL
  SELECT 'Commercial', 2 UNION ALL
  SELECT 'Other', 3
) v
WHERE c.code_name = 'RbzLoanClass'
  AND NOT EXISTS (
    SELECT 1 FROM m_code_value cv WHERE cv.code_id = c.id AND cv.code_value = v.code_value
  );

-- RbzRelatedPartyType values
INSERT INTO m_code_value (code_id, code_value, order_position, code_score, is_active, is_mandatory)
SELECT c.id, v.code_value, v.ord, NULL, 1, 0
FROM m_code c
JOIN (
  SELECT 'Shareholder' AS code_value, 1 AS ord UNION ALL
  SELECT 'Director', 2 UNION ALL
  SELECT 'Related Party', 3 UNION ALL
  SELECT 'Other', 4
) v
WHERE c.code_name = 'RbzRelatedPartyType'
  AND NOT EXISTS (
    SELECT 1 FROM m_code_value cv WHERE cv.code_id = c.id AND cv.code_value = v.code_value
  );

-- RbzLocationType values
INSERT INTO m_code_value (code_id, code_value, order_position, code_score, is_active, is_mandatory)
SELECT c.id, v.code_value, v.ord, NULL, 1, 0
FROM m_code c
JOIN (
  SELECT 'Urban' AS code_value, 1 AS ord UNION ALL
  SELECT 'Rural', 2
) v
WHERE c.code_name = 'RbzLocationType'
  AND NOT EXISTS (
    SELECT 1 FROM m_code_value cv WHERE cv.code_id = c.id AND cv.code_value = v.code_value
  );

-- Extend LoanPurpose with RBZ purpose list (keep any existing values)
INSERT INTO m_code_value (code_id, code_value, order_position, code_score, is_active, is_mandatory)
SELECT c.id, v.code_value, v.ord, NULL, 1, 0
FROM m_code c
JOIN (
  SELECT 'Manufacturing' AS code_value, 100 AS ord UNION ALL
  SELECT 'Retail', 101 UNION ALL
  SELECT 'Consumption', 102 UNION ALL
  SELECT 'Services', 103 UNION ALL
  SELECT 'Health', 104 UNION ALL
  SELECT 'Education', 105 UNION ALL
  SELECT 'Mining', 106 UNION ALL
  SELECT 'Agriculture', 107 UNION ALL
  SELECT 'Cross Border Traders', 108 UNION ALL
  SELECT 'Vendors', 109 UNION ALL
  SELECT 'Funeral Assistance', 110 UNION ALL
  SELECT 'Other', 111
) v
WHERE c.code_name = 'LoanPurpose'
  AND NOT EXISTS (
    SELECT 1 FROM m_code_value cv
    WHERE cv.code_id = c.id AND LOWER(cv.code_value) = LOWER(v.code_value)
  );

-- Gender Female / Male if missing
INSERT INTO m_code_value (code_id, code_value, order_position, code_score, is_active, is_mandatory)
SELECT c.id, v.code_value, v.ord, NULL, 1, 0
FROM m_code c
JOIN (
  SELECT 'Female' AS code_value, 1 AS ord UNION ALL
  SELECT 'Male', 2
) v
WHERE c.code_name = 'Gender'
  AND NOT EXISTS (
    SELECT 1 FROM m_code_value cv
    WHERE cv.code_id = c.id AND LOWER(cv.code_value) = LOWER(v.code_value)
  );

-- ---------------------------------------------------------------------------
-- Datatables (recreate with expected columns; empty stubs are safe to replace)
-- ---------------------------------------------------------------------------
DELETE FROM x_registered_table WHERE registered_table_name IN (
  'rbz_loan_details', 'rbz_office_channels', 'rbz_institution_profile', 'rbz_long_term_debt'
);

DROP TABLE IF EXISTS `rbz_loan_details`;
DROP TABLE IF EXISTS `rbz_office_channels`;
DROP TABLE IF EXISTS `rbz_institution_profile`;
DROP TABLE IF EXISTS `rbz_long_term_debt`;

CREATE TABLE `rbz_loan_details` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `loan_id` bigint(20) NOT NULL,
  `RbzLoanClass_cd_loan_class` int(11) DEFAULT NULL,
  `RbzRelatedPartyType_cd_related_party_type` int(11) DEFAULT NULL,
  `related_party_name` varchar(100) DEFAULT NULL,
  `is_insider_loan` tinyint(1) DEFAULT 0,
  `is_under_litigation` tinyint(1) DEFAULT 0,
  `litigation_amount` decimal(19,6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_rbz_loan_details_loan_id` (`loan_id`),
  CONSTRAINT `fk_rbz_loan_details_loan_id` FOREIGN KEY (`loan_id`) REFERENCES `m_loan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `rbz_office_channels` (
  `office_id` bigint(20) NOT NULL,
  `province` varchar(100) DEFAULT NULL,
  `district` varchar(100) DEFAULT NULL,
  `RbzLocationType_cd_location_type` int(11) DEFAULT NULL,
  `num_pos` int(11) DEFAULT 0,
  `num_atms` int(11) DEFAULT 0,
  `num_agencies` int(11) DEFAULT 0,
  `num_mobile_branches` int(11) DEFAULT 0,
  `num_agents` int(11) DEFAULT 0,
  `num_banking_kiosks` int(11) DEFAULT 0,
  PRIMARY KEY (`office_id`),
  CONSTRAINT `fk_rbz_office_channels_office_id` FOREIGN KEY (`office_id`) REFERENCES `m_office` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `rbz_institution_profile` (
  `office_id` bigint(20) NOT NULL,
  `institution_name` varchar(200) DEFAULT NULL,
  `licence_number` varchar(100) DEFAULT NULL,
  `date_commenced` date DEFAULT NULL,
  `physical_address` varchar(500) DEFAULT NULL,
  `postal_address` varchar(500) DEFAULT NULL,
  `contact_telephones` varchar(200) DEFAULT NULL,
  `contact_person` varchar(200) DEFAULT NULL,
  `num_employees_female` int(11) DEFAULT 0,
  `num_employees_male` int(11) DEFAULT 0,
  `num_loan_officers_female` int(11) DEFAULT 0,
  `num_loan_officers_male` int(11) DEFAULT 0,
  `external_auditors` varchar(500) DEFAULT NULL,
  `bankers` varchar(500) DEFAULT NULL,
  `lawyers` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`office_id`),
  CONSTRAINT `fk_rbz_institution_profile_office_id` FOREIGN KEY (`office_id`) REFERENCES `m_office` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `rbz_long_term_debt` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `office_id` bigint(20) NOT NULL,
  `source_of_finance` varchar(200) DEFAULT NULL,
  `amount_borrowed` decimal(19,6) DEFAULT NULL,
  `outstanding_balance` decimal(19,6) DEFAULT NULL,
  `interest_rate` decimal(19,6) DEFAULT NULL,
  `maturity_date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_rbz_long_term_debt_office_id` (`office_id`),
  CONSTRAINT `fk_rbz_long_term_debt_office_id` FOREIGN KEY (`office_id`) REFERENCES `m_office` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO x_registered_table (registered_table_name, application_table_name, category) VALUES
  ('rbz_loan_details', 'm_loan', 100),
  ('rbz_office_channels', 'm_office', 100),
  ('rbz_institution_profile', 'm_office', 100),
  ('rbz_long_term_debt', 'm_office', 100);

-- Datatable permissions (idempotent)
INSERT INTO m_permission (grouping, code, entity_name, action_name, can_maker_checker)
SELECT 'datatable', p.code, p.entity_name, p.action_name, p.can_maker_checker
FROM (
  SELECT 'CREATE_rbz_loan_details' AS code, 'rbz_loan_details' AS entity_name, 'CREATE' AS action_name, 1 AS can_maker_checker UNION ALL
  SELECT 'CREATE_rbz_loan_details_CHECKER', 'rbz_loan_details', 'CREATE', 0 UNION ALL
  SELECT 'READ_rbz_loan_details', 'rbz_loan_details', 'READ', 0 UNION ALL
  SELECT 'UPDATE_rbz_loan_details', 'rbz_loan_details', 'UPDATE', 1 UNION ALL
  SELECT 'UPDATE_rbz_loan_details_CHECKER', 'rbz_loan_details', 'UPDATE', 0 UNION ALL
  SELECT 'DELETE_rbz_loan_details', 'rbz_loan_details', 'DELETE', 1 UNION ALL
  SELECT 'DELETE_rbz_loan_details_CHECKER', 'rbz_loan_details', 'DELETE', 0 UNION ALL
  SELECT 'CREATE_rbz_office_channels', 'rbz_office_channels', 'CREATE', 1 UNION ALL
  SELECT 'CREATE_rbz_office_channels_CHECKER', 'rbz_office_channels', 'CREATE', 0 UNION ALL
  SELECT 'READ_rbz_office_channels', 'rbz_office_channels', 'READ', 0 UNION ALL
  SELECT 'UPDATE_rbz_office_channels', 'rbz_office_channels', 'UPDATE', 1 UNION ALL
  SELECT 'UPDATE_rbz_office_channels_CHECKER', 'rbz_office_channels', 'UPDATE', 0 UNION ALL
  SELECT 'DELETE_rbz_office_channels', 'rbz_office_channels', 'DELETE', 1 UNION ALL
  SELECT 'DELETE_rbz_office_channels_CHECKER', 'rbz_office_channels', 'DELETE', 0 UNION ALL
  SELECT 'CREATE_rbz_institution_profile', 'rbz_institution_profile', 'CREATE', 1 UNION ALL
  SELECT 'CREATE_rbz_institution_profile_CHECKER', 'rbz_institution_profile', 'CREATE', 0 UNION ALL
  SELECT 'READ_rbz_institution_profile', 'rbz_institution_profile', 'READ', 0 UNION ALL
  SELECT 'UPDATE_rbz_institution_profile', 'rbz_institution_profile', 'UPDATE', 1 UNION ALL
  SELECT 'UPDATE_rbz_institution_profile_CHECKER', 'rbz_institution_profile', 'UPDATE', 0 UNION ALL
  SELECT 'DELETE_rbz_institution_profile', 'rbz_institution_profile', 'DELETE', 1 UNION ALL
  SELECT 'DELETE_rbz_institution_profile_CHECKER', 'rbz_institution_profile', 'DELETE', 0 UNION ALL
  SELECT 'CREATE_rbz_long_term_debt', 'rbz_long_term_debt', 'CREATE', 1 UNION ALL
  SELECT 'CREATE_rbz_long_term_debt_CHECKER', 'rbz_long_term_debt', 'CREATE', 0 UNION ALL
  SELECT 'READ_rbz_long_term_debt', 'rbz_long_term_debt', 'READ', 0 UNION ALL
  SELECT 'UPDATE_rbz_long_term_debt', 'rbz_long_term_debt', 'UPDATE', 1 UNION ALL
  SELECT 'UPDATE_rbz_long_term_debt_CHECKER', 'rbz_long_term_debt', 'UPDATE', 0 UNION ALL
  SELECT 'DELETE_rbz_long_term_debt', 'rbz_long_term_debt', 'DELETE', 1 UNION ALL
  SELECT 'DELETE_rbz_long_term_debt_CHECKER', 'rbz_long_term_debt', 'DELETE', 0
) p
WHERE NOT EXISTS (SELECT 1 FROM m_permission mp WHERE mp.code = p.code);

-- Grant to Super user role when present
INSERT INTO m_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM m_role r
JOIN m_permission p ON p.code IN (
  'READ_RBZ_FORM_MFI1',
  'CREATE_rbz_loan_details', 'READ_rbz_loan_details', 'UPDATE_rbz_loan_details', 'DELETE_rbz_loan_details',
  'CREATE_rbz_office_channels', 'READ_rbz_office_channels', 'UPDATE_rbz_office_channels', 'DELETE_rbz_office_channels',
  'CREATE_rbz_institution_profile', 'READ_rbz_institution_profile', 'UPDATE_rbz_institution_profile', 'DELETE_rbz_institution_profile',
  'CREATE_rbz_long_term_debt', 'READ_rbz_long_term_debt', 'UPDATE_rbz_long_term_debt', 'DELETE_rbz_long_term_debt'
)
WHERE r.name = 'Super user'
  AND NOT EXISTS (
    SELECT 1 FROM m_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
