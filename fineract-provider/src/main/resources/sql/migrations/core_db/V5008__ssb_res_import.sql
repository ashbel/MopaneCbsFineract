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

-- SSB / Pensions bureau RES (response) import: auto-disburse authorised loans,
-- note rejected loans, and retain batch status for review.

INSERT INTO m_permission (grouping, code, entity_name, action_name, can_maker_checker)
SELECT 'report', p.code, p.entity_name, p.action_name, p.can_maker_checker
FROM (
  SELECT 'CREATE_SSB_RES_IMPORT' AS code, 'SSB_RES_IMPORT' AS entity_name, 'CREATE' AS action_name, 0 AS can_maker_checker UNION ALL
  SELECT 'UPDATE_SSB_RES_IMPORT', 'SSB_RES_IMPORT', 'UPDATE', 0
) p
WHERE NOT EXISTS (SELECT 1 FROM m_permission mp WHERE mp.code = p.code);

INSERT INTO c_configuration (`name`, `value`, `date_value`, `enabled`, `is_trap_door`, `description`)
SELECT 'ssb-res-auto-disburse', NULL, NULL, 1, 0,
       'When enabled, SSB/Pensions RES SUCCESS rows auto-disburse approved loans. Upload autoDisburse can override.'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM c_configuration c WHERE c.name = 'ssb-res-auto-disburse');

CREATE TABLE IF NOT EXISTS `m_ssb_res_import_batch` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `bureau` varchar(20) NOT NULL,
  `filename` varchar(255) DEFAULT NULL,
  `uploaded_by` bigint(20) DEFAULT NULL,
  `uploaded_on` datetime NOT NULL,
  `dry_run` tinyint(1) NOT NULL DEFAULT 0,
  `auto_disburse` tinyint(1) NOT NULL DEFAULT 1,
  `payment_type_id` bigint(20) DEFAULT NULL,
  `disbursed_count` int(11) NOT NULL DEFAULT 0,
  `authorised_count` int(11) NOT NULL DEFAULT 0,
  `noted_count` int(11) NOT NULL DEFAULT 0,
  `failed_count` int(11) NOT NULL DEFAULT 0,
  `needs_review_count` int(11) NOT NULL DEFAULT 0,
  `total_count` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_ssb_res_batch_uploaded_on` (`uploaded_on`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `m_ssb_res_import_row` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `batch_id` bigint(20) NOT NULL,
  `row_number` int(11) NOT NULL,
  `rec_id` varchar(50) DEFAULT NULL,
  `deduction_code` varchar(50) DEFAULT NULL,
  `reference` varchar(100) DEFAULT NULL,
  `id_number` varchar(50) DEFAULT NULL,
  `ec_number` varchar(50) DEFAULT NULL,
  `type` varchar(20) DEFAULT NULL,
  `bureau_status` varchar(30) DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `amount` decimal(19,6) DEFAULT NULL,
  `name` varchar(200) DEFAULT NULL,
  `message` varchar(500) DEFAULT NULL,
  `status` varchar(30) NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `suggested_loan_id` bigint(20) DEFAULT NULL,
  `suggested_account_no` varchar(100) DEFAULT NULL,
  `loan_id` bigint(20) DEFAULT NULL,
  `disbursement_transaction_id` bigint(20) DEFAULT NULL,
  `note_id` bigint(20) DEFAULT NULL,
  `note` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ssb_res_row_batch` (`batch_id`),
  KEY `idx_ssb_res_row_status` (`batch_id`, `status`),
  KEY `idx_ssb_res_row_rec_id` (`rec_id`),
  CONSTRAINT `fk_ssb_res_row_batch` FOREIGN KEY (`batch_id`) REFERENCES `m_ssb_res_import_batch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO m_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM m_role r
JOIN m_permission p ON p.code IN ('CREATE_SSB_RES_IMPORT', 'UPDATE_SSB_RES_IMPORT')
WHERE r.name = 'Super user'
  AND NOT EXISTS (
    SELECT 1 FROM m_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
