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

INSERT INTO `stretchy_report` (`report_name`, `report_type`, `report_subtype`, `report_category`, `report_sql`, `description`, `core_report`, `use_report`)
VALUES ('Field Collection Sheet', 'Table', NULL, 'Loan',
'SELECT
  l.account_no AS `Loan Number`,
  IFNULL(c.display_name, g.display_name) AS `Client Name`,
  c.mobile_no AS `Client Phone Number`,
  ROUND(l.principal_amount, 2) AS `Principal`,
  ROUND(l.interest_charged_derived, 2) AS `Interest`,
  ROUND(l.penalty_charges_charged_derived, 2) AS `Penalty`,
  ROUND(l.fee_charges_charged_derived, 2) AS `Fees`,
  ROUND(l.principal_outstanding_derived, 2) AS `Principal Balance`,
  ROUND(l.interest_outstanding_derived, 2) AS `Interest Balance`,
  ROUND(l.penalty_charges_outstanding_derived, 2) AS `Penalty Balance`,
  ROUND(l.fee_charges_outstanding_derived, 2) AS `Fees Balance`,
  DATE_FORMAT(l.disbursedon_date, ''%d/%m/%Y'') AS `Disbursement Date`,
  DATE_FORMAT(l.submittedon_date, ''%d/%m/%Y'') AS `Application Date`,
  DATE_FORMAT((SELECT MIN(ls0.duedate) FROM m_loan_repayment_schedule ls0 WHERE ls0.loan_id = l.id), ''%d/%m/%Y'') AS `First Repayment Date`,
  DATE_FORMAT(l.expected_maturedon_date, ''%d/%m/%Y'') AS `Maturity Date`,
  ''Open'' AS `Loan Status`,
  ROUND(due.installment_amount, 2) AS `Total Installment Amount`,
  ROUND(due.arrears_amount, 2) AS `Total Arrears Amount`,
  ROUND(due.installment_amount + due.arrears_amount, 2) AS `Total Payment`,
  DATE_FORMAT(due.oldest_due, ''%d/%m/%Y'') AS `Oldest Installment Date`,
  DATE_FORMAT(due.newest_due, ''%d/%m/%Y'') AS `Newest Installment Date`
FROM m_loan l
LEFT JOIN m_client c ON c.id = l.client_id
LEFT JOIN m_group g ON g.id = l.group_id
JOIN m_office ounder ON ounder.id = IFNULL(c.office_id, g.office_id)
JOIN m_office o ON ounder.hierarchy LIKE CONCAT(o.hierarchy, ''%'')
  AND ounder.hierarchy LIKE CONCAT(''${currentUserHierarchy}'', ''%'')
JOIN (
  SELECT
    ls.loan_id,
    SUM(CASE WHEN ls.duedate = DATE(''${collectionDate}'') THEN
      (IFNULL(ls.principal_amount, 0) - IFNULL(ls.principal_completed_derived, 0) - IFNULL(ls.principal_writtenoff_derived, 0)
       + IFNULL(ls.interest_amount, 0) - IFNULL(ls.interest_completed_derived, 0) - IFNULL(ls.interest_writtenoff_derived, 0) - IFNULL(ls.interest_waived_derived, 0)
       + IFNULL(ls.fee_charges_amount, 0) - IFNULL(ls.fee_charges_completed_derived, 0) - IFNULL(ls.fee_charges_writtenoff_derived, 0) - IFNULL(ls.fee_charges_waived_derived, 0)
       + IFNULL(ls.penalty_charges_amount, 0) - IFNULL(ls.penalty_charges_completed_derived, 0) - IFNULL(ls.penalty_charges_writtenoff_derived, 0) - IFNULL(ls.penalty_charges_waived_derived, 0))
    ELSE 0 END) AS installment_amount,
    SUM(CASE WHEN ls.duedate < DATE(''${collectionDate}'') THEN
      (IFNULL(ls.principal_amount, 0) - IFNULL(ls.principal_completed_derived, 0) - IFNULL(ls.principal_writtenoff_derived, 0)
       + IFNULL(ls.interest_amount, 0) - IFNULL(ls.interest_completed_derived, 0) - IFNULL(ls.interest_writtenoff_derived, 0) - IFNULL(ls.interest_waived_derived, 0)
       + IFNULL(ls.fee_charges_amount, 0) - IFNULL(ls.fee_charges_completed_derived, 0) - IFNULL(ls.fee_charges_writtenoff_derived, 0) - IFNULL(ls.fee_charges_waived_derived, 0)
       + IFNULL(ls.penalty_charges_amount, 0) - IFNULL(ls.penalty_charges_completed_derived, 0) - IFNULL(ls.penalty_charges_writtenoff_derived, 0) - IFNULL(ls.penalty_charges_waived_derived, 0))
    ELSE 0 END) AS arrears_amount,
    MIN(ls.duedate) AS oldest_due,
    MAX(ls.duedate) AS newest_due
  FROM m_loan_repayment_schedule ls
  WHERE ls.completed_derived = 0
    AND ls.duedate <= DATE(''${collectionDate}'')
  GROUP BY ls.loan_id
  HAVING installment_amount + arrears_amount > 0
) due ON due.loan_id = l.id
WHERE l.loan_status_id = 300
  AND o.id = ${officeId}
  AND (IFNULL(l.loan_officer_id, -10) = ${loanOfficerId} OR ${loanOfficerId} = -1)
  AND (c.id IS NULL OR c.is_deleted = 0)
ORDER BY `Client Name`, `Loan Number`',
'Active loans due or in arrears on a collection date, for officers to call and visit clients.',
0, 1);

INSERT INTO `stretchy_parameter` (`parameter_name`, `parameter_variable`, `parameter_label`, `parameter_displayType`, `parameter_FormatType`, `parameter_default`, `special`, `selectOne`, `selectAll`, `parameter_sql`, `parent_id`)
VALUES ('collectionDateSelect', 'collectionDate', 'Collection Date', 'date', 'date', 'today', NULL, NULL, NULL, NULL, NULL);

INSERT INTO `stretchy_report_parameter` (`report_id`, `parameter_id`, `report_parameter_name`)
VALUES (
  (SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = 'Field Collection Sheet'),
  (SELECT sp.id FROM stretchy_parameter sp WHERE sp.parameter_name = 'OfficeIdSelectOne'),
  'officeId'
);

INSERT INTO `stretchy_report_parameter` (`report_id`, `parameter_id`, `report_parameter_name`)
VALUES (
  (SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = 'Field Collection Sheet'),
  (SELECT sp.id FROM stretchy_parameter sp WHERE sp.parameter_name = 'loanOfficerIdSelectAll'),
  'loanOfficerId'
);

INSERT INTO `stretchy_report_parameter` (`report_id`, `parameter_id`, `report_parameter_name`)
VALUES (
  (SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = 'Field Collection Sheet'),
  (SELECT sp.id FROM stretchy_parameter sp WHERE sp.parameter_name = 'collectionDateSelect'),
  'collectionDate'
);

INSERT INTO `m_permission` (`grouping`, `code`, `entity_name`, `action_name`, `can_maker_checker`)
VALUES ('report', 'READ_Field Collection Sheet', 'Field Collection Sheet', 'READ', 0);

INSERT INTO `m_role_permission` (`role_id`, `permission_id`)
SELECT DISTINCT rp.role_id, new_perm.id
FROM m_role_permission rp
JOIN m_permission old_perm ON old_perm.id = rp.permission_id AND old_perm.code = 'READ_COLLECTIONSHEET'
JOIN m_permission new_perm ON new_perm.code = 'READ_Field Collection Sheet';
