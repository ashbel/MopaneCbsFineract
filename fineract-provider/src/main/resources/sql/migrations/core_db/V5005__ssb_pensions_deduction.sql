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

-- SSB / Pensions salary-deduction export + PAY import

-- ---------------------------------------------------------------------------
-- Permissions
-- ---------------------------------------------------------------------------
INSERT INTO m_permission (grouping, code, entity_name, action_name, can_maker_checker)
SELECT 'report', p.code, p.entity_name, p.action_name, p.can_maker_checker
FROM (
  SELECT 'READ_SSB_DEDUCTION' AS code, 'SSB_DEDUCTION' AS entity_name, 'READ' AS action_name, 0 AS can_maker_checker UNION ALL
  SELECT 'CREATE_SSB_PAY_IMPORT', 'SSB_PAY_IMPORT', 'CREATE', 0 UNION ALL
  SELECT 'UPDATE_SSB_PAY_IMPORT', 'SSB_PAY_IMPORT', 'UPDATE', 0
) p
WHERE NOT EXISTS (SELECT 1 FROM m_permission mp WHERE mp.code = p.code);

-- ---------------------------------------------------------------------------
-- Client datatable: IdNumber / EcNumber
-- ---------------------------------------------------------------------------
DELETE FROM x_registered_table WHERE registered_table_name = 'ssb_client_details';

DROP TABLE IF EXISTS `ssb_client_details`;

CREATE TABLE `ssb_client_details` (
  `client_id` bigint(20) NOT NULL,
  `IdNumber` varchar(50) DEFAULT NULL,
  `EcNumber` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`client_id`),
  KEY `idx_ssb_client_details_idnumber` (`IdNumber`),
  KEY `idx_ssb_client_details_ecnumber` (`EcNumber`),
  CONSTRAINT `fk_ssb_client_details_client_id` FOREIGN KEY (`client_id`) REFERENCES `m_client` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO x_registered_table (registered_table_name, application_table_name, category)
VALUES ('ssb_client_details', 'm_client', 100);

INSERT INTO m_permission (grouping, code, entity_name, action_name, can_maker_checker)
SELECT 'datatable', p.code, p.entity_name, p.action_name, p.can_maker_checker
FROM (
  SELECT 'CREATE_ssb_client_details' AS code, 'ssb_client_details' AS entity_name, 'CREATE' AS action_name, 1 AS can_maker_checker UNION ALL
  SELECT 'CREATE_ssb_client_details_CHECKER', 'ssb_client_details', 'CREATE', 0 UNION ALL
  SELECT 'READ_ssb_client_details', 'ssb_client_details', 'READ', 0 UNION ALL
  SELECT 'UPDATE_ssb_client_details', 'ssb_client_details', 'UPDATE', 1 UNION ALL
  SELECT 'UPDATE_ssb_client_details_CHECKER', 'ssb_client_details', 'UPDATE', 0 UNION ALL
  SELECT 'DELETE_ssb_client_details', 'ssb_client_details', 'DELETE', 1 UNION ALL
  SELECT 'DELETE_ssb_client_details_CHECKER', 'ssb_client_details', 'DELETE', 0
) p
WHERE NOT EXISTS (SELECT 1 FROM m_permission mp WHERE mp.code = p.code);

-- ---------------------------------------------------------------------------
-- Mandate tracking (NEW / CHANGE / DELETE)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `m_ssb_mandate` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `loan_id` bigint(20) NOT NULL,
  `bureau` varchar(20) NOT NULL,
  `account_no` varchar(100) NOT NULL,
  `last_type` varchar(10) DEFAULT NULL,
  `last_amount_cents` bigint(20) DEFAULT NULL,
  `last_end_date` date DEFAULT NULL,
  `last_exported_on` datetime DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ssb_mandate_loan_bureau` (`loan_id`, `bureau`),
  KEY `idx_ssb_mandate_account_no` (`account_no`),
  KEY `idx_ssb_mandate_active_bureau` (`active`, `bureau`),
  CONSTRAINT `fk_ssb_mandate_loan_id` FOREIGN KEY (`loan_id`) REFERENCES `m_loan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ---------------------------------------------------------------------------
-- PAY import batches / rows
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `m_ssb_pay_import_batch` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `bureau` varchar(20) NOT NULL,
  `filename` varchar(255) DEFAULT NULL,
  `uploaded_by` bigint(20) DEFAULT NULL,
  `uploaded_on` datetime NOT NULL,
  `dry_run` tinyint(1) NOT NULL DEFAULT 0,
  `payment_type_id` bigint(20) DEFAULT NULL,
  `posted_count` int(11) NOT NULL DEFAULT 0,
  `failed_count` int(11) NOT NULL DEFAULT 0,
  `needs_review_count` int(11) NOT NULL DEFAULT 0,
  `total_count` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_ssb_pay_batch_uploaded_on` (`uploaded_on`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `m_ssb_pay_import_row` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `batch_id` bigint(20) NOT NULL,
  `row_number` int(11) NOT NULL,
  `rec_id` varchar(50) DEFAULT NULL,
  `deduction_code` varchar(50) DEFAULT NULL,
  `reference` varchar(100) DEFAULT NULL,
  `id_number` varchar(50) DEFAULT NULL,
  `ec_number` varchar(50) DEFAULT NULL,
  `trans_date` date DEFAULT NULL,
  `amount` decimal(19,6) DEFAULT NULL,
  `name` varchar(200) DEFAULT NULL,
  `status` varchar(30) NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `suggested_loan_id` bigint(20) DEFAULT NULL,
  `suggested_account_no` varchar(100) DEFAULT NULL,
  `posted_loan_id` bigint(20) DEFAULT NULL,
  `posted_transaction_id` bigint(20) DEFAULT NULL,
  `note` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ssb_pay_row_batch` (`batch_id`),
  KEY `idx_ssb_pay_row_status` (`batch_id`, `status`),
  KEY `idx_ssb_pay_row_rec_id` (`rec_id`),
  CONSTRAINT `fk_ssb_pay_row_batch` FOREIGN KEY (`batch_id`) REFERENCES `m_ssb_pay_import_batch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Grant to Super user role when present
INSERT INTO m_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM m_role r
JOIN m_permission p ON p.code IN (
  'READ_SSB_DEDUCTION', 'CREATE_SSB_PAY_IMPORT', 'UPDATE_SSB_PAY_IMPORT',
  'CREATE_ssb_client_details', 'READ_ssb_client_details', 'UPDATE_ssb_client_details', 'DELETE_ssb_client_details'
)
WHERE r.name = 'Super user'
  AND NOT EXISTS (
    SELECT 1 FROM m_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
