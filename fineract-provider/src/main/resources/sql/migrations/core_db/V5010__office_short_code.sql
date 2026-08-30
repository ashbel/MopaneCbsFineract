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

ALTER TABLE `m_office` ADD COLUMN `short_code` VARCHAR(3) NULL AFTER `external_id`;

UPDATE `m_office`
SET `short_code` = CASE
    WHEN `id` = 1 THEN 'HO1'
    WHEN `id` < 100 THEN CONCAT('O', LPAD(`id`, 2, '0'))
    ELSE LPAD(CONV(`id`, 10, 36), 3, '0')
END;

ALTER TABLE `m_office`
  MODIFY COLUMN `short_code` VARCHAR(3) NOT NULL,
  ADD UNIQUE KEY `shortcode_org` (`short_code`);
