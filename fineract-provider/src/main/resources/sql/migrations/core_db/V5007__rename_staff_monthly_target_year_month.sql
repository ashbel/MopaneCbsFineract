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

-- MySQL treats YEAR_MONTH as a reserved interval unit. Unquoted year_month columns
-- break OpenJPA INSERT/UPDATE and JDBC SELECT statements.

SET @has_year_month := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'm_staff_monthly_target'
    AND COLUMN_NAME = 'year_month'
);

SET @sql := IF(
  @has_year_month > 0,
  'ALTER TABLE `m_staff_monthly_target` CHANGE COLUMN `year_month` `target_year_month` VARCHAR(7) NOT NULL',
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
