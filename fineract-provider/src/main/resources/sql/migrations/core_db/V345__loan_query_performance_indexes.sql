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

-- Supports GET /loans status filtering and the Disbursal_Vs_Awaitingdisbursal report's
-- "expected_disbursedon_date = today AND loan_status_id in (...)" subquery, which previously
-- had to scan every row in m_loan.
ALTER TABLE `m_loan`
ADD INDEX `m_loan_expected_disbursedon_date_status_idx` (`expected_disbursedon_date` ASC, `loan_status_id` ASC);

-- m_loan_transaction previously had no index beyond the primary key and the loan_id FK, so the
-- Disbursal_Vs_Awaitingdisbursal report's "transaction_date = today" subquery forced a full
-- table scan proportional to a tenant's entire transaction history.
ALTER TABLE `m_loan_transaction`
ADD INDEX `m_loan_transaction_date_type_reversed_idx` (`transaction_date` ASC, `transaction_type_enum` ASC, `is_reversed` ASC);

-- m_office.hierarchy is used for "LIKE 'prefix%'" office-scoping filters throughout the
-- codebase (including GET /loans) but was never indexed.
ALTER TABLE `m_office`
ADD INDEX `m_office_hierarchy_idx` (`hierarchy` ASC);
