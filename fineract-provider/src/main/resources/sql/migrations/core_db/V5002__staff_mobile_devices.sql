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

CREATE TABLE `m_staff_mobile_device` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `appuser_id` BIGINT(20) NOT NULL,
  `staff_id` BIGINT(20) DEFAULT NULL,
  `fcm_token` VARCHAR(255) DEFAULT NULL,
  `device_uid` VARCHAR(100) NOT NULL,
  `platform` VARCHAR(50) DEFAULT NULL,
  `model` VARCHAR(100) DEFAULT NULL,
  `app_version` VARCHAR(50) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL,
  `activated_on_utc` DATETIME DEFAULT NULL,
  `last_seen_on_utc` DATETIME DEFAULT NULL,
  `created_on_utc` DATETIME NOT NULL,
  `updated_on_utc` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_staff_mobile_device_user_uid` (`appuser_id`, `device_uid`),
  UNIQUE KEY `uk_staff_mobile_device_fcm_token` (`fcm_token`),
  KEY `idx_staff_mobile_device_status` (`status`),
  KEY `idx_staff_mobile_device_staff` (`staff_id`),
  CONSTRAINT `fk_staff_mobile_device_appuser` FOREIGN KEY (`appuser_id`) REFERENCES `m_appuser` (`id`),
  CONSTRAINT `fk_staff_mobile_device_staff` FOREIGN KEY (`staff_id`) REFERENCES `m_staff` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `m_staff_mobile_activation_code` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `appuser_id` BIGINT(20) NOT NULL,
  `code` VARCHAR(32) NOT NULL,
  `expires_on_utc` DATETIME NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `used_on_utc` DATETIME DEFAULT NULL,
  `used_by_device_id` BIGINT(20) DEFAULT NULL,
  `createdby_id` BIGINT(20) NOT NULL,
  `created_on_utc` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_staff_mobile_activation_code` (`code`),
  KEY `idx_staff_mobile_activation_code_user` (`appuser_id`),
  KEY `idx_staff_mobile_activation_code_status` (`status`),
  CONSTRAINT `fk_staff_mobile_activation_code_appuser` FOREIGN KEY (`appuser_id`) REFERENCES `m_appuser` (`id`),
  CONSTRAINT `fk_staff_mobile_activation_code_device` FOREIGN KEY (`used_by_device_id`) REFERENCES `m_staff_mobile_device` (`id`),
  CONSTRAINT `fk_staff_mobile_activation_code_createdby` FOREIGN KEY (`createdby_id`) REFERENCES `m_appuser` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `m_staff_location_ping` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `device_id` BIGINT(20) NOT NULL,
  `appuser_id` BIGINT(20) NOT NULL,
  `latitude` DECIMAL(10,7) NOT NULL,
  `longitude` DECIMAL(10,7) NOT NULL,
  `accuracy_meters` DECIMAL(10,2) DEFAULT NULL,
  `recorded_on_utc` DATETIME NOT NULL,
  `received_on_utc` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_staff_location_ping_user_recorded` (`appuser_id`, `recorded_on_utc`),
  KEY `idx_staff_location_ping_device_recorded` (`device_id`, `recorded_on_utc`),
  CONSTRAINT `fk_staff_location_ping_device` FOREIGN KEY (`device_id`) REFERENCES `m_staff_mobile_device` (`id`),
  CONSTRAINT `fk_staff_location_ping_appuser` FOREIGN KEY (`appuser_id`) REFERENCES `m_appuser` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `m_permission` (`grouping`, `code`, `entity_name`, `action_name`, `can_maker_checker`)
VALUES
('organisation', 'READ_STAFFMOBILEDEVICE', 'STAFFMOBILEDEVICE', 'READ', 0),
('organisation', 'CREATE_STAFFMOBILEDEVICE', 'STAFFMOBILEDEVICE', 'CREATE', 0),
('organisation', 'UPDATE_STAFFMOBILEDEVICE', 'STAFFMOBILEDEVICE', 'UPDATE', 0),
('organisation', 'DELETE_STAFFMOBILEDEVICE', 'STAFFMOBILEDEVICE', 'DELETE', 0);
