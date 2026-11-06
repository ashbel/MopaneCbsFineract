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

CREATE TABLE IF NOT EXISTS `m_staff_monthly_target` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `staff_id` BIGINT(20) NOT NULL,
  `office_id` BIGINT(20) NOT NULL,
  `year_month` VARCHAR(7) NOT NULL,
  `currency_code` VARCHAR(3) NOT NULL,
  `collections_target_amount` DECIMAL(19,6) NOT NULL DEFAULT 0,
  `disbursements_target_amount` DECIMAL(19,6) NOT NULL DEFAULT 0,
  `new_clients_target` INT(11) NOT NULL DEFAULT 0,
  `createdby_id` BIGINT(20) NOT NULL,
  `created_on_utc` DATETIME NOT NULL,
  `updatedby_id` BIGINT(20) DEFAULT NULL,
  `updated_on_utc` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_staff_monthly_target` (`staff_id`, `year_month`, `currency_code`),
  KEY `idx_staff_monthly_target_office_month` (`office_id`, `year_month`),
  CONSTRAINT `fk_staff_monthly_target_staff` FOREIGN KEY (`staff_id`) REFERENCES `m_staff` (`id`),
  CONSTRAINT `fk_staff_monthly_target_office` FOREIGN KEY (`office_id`) REFERENCES `m_office` (`id`),
  CONSTRAINT `fk_staff_monthly_target_createdby` FOREIGN KEY (`createdby_id`) REFERENCES `m_appuser` (`id`),
  CONSTRAINT `fk_staff_monthly_target_updatedby` FOREIGN KEY (`updatedby_id`) REFERENCES `m_appuser` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO m_permission (grouping, code, entity_name, action_name, can_maker_checker)
SELECT 'organisation', p.code, p.entity_name, p.action_name, p.can_maker_checker
FROM (
  SELECT 'READ_STAFFMONTHLYTARGET' AS code, 'STAFFMONTHLYTARGET' AS entity_name, 'READ' AS action_name, 0 AS can_maker_checker UNION ALL
  SELECT 'CREATE_STAFFMONTHLYTARGET', 'STAFFMONTHLYTARGET', 'CREATE', 0 UNION ALL
  SELECT 'UPDATE_STAFFMONTHLYTARGET', 'STAFFMONTHLYTARGET', 'UPDATE', 0 UNION ALL
  SELECT 'DELETE_STAFFMONTHLYTARGET', 'STAFFMONTHLYTARGET', 'DELETE', 0 UNION ALL
  SELECT 'READ_MOBILE_OFFICER_DASHBOARD', 'MOBILE_OFFICER_DASHBOARD', 'READ', 0
) p
WHERE NOT EXISTS (SELECT 1 FROM m_permission mp WHERE mp.code = p.code);
