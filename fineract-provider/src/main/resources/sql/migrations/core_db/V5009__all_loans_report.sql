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

-- All Loans: copy of Active Loans - Details (report id 12) without the
-- loan_status_id = 300 filter, so closed, written-off, rejected, and
-- other historical loans are included. Adds Status and Closed Date.

INSERT INTO `stretchy_report` (`report_name`, `report_type`, `report_subtype`, `report_category`, `report_sql`, `description`, `core_report`, `use_report`)
SELECT 'All Loans', 'Table', NULL, 'Loan',
'select concat(repeat("..",
   ((LENGTH(ounder.`hierarchy`) - LENGTH(REPLACE(ounder.`hierarchy`, ''.'', '''')) - 1))), ounder.`name`) as "Office/Branch",
ifnull(cur.display_symbol, l.currency_code) as Currency,
lo.display_name as "Loan Officer",
c.display_name as "Client",
c.account_no as "Client Account No.",
l.account_no as "Loan Account No.",
pl.`name` as "Product",
st.enum_message_property as "Status",
f.`name` as Fund,
l.principal_amount as "Loan Amount",
l.annual_nominal_interest_rate as " Annual Nominal Interest Rate",
date(l.disbursedon_date) as "Disbursed Date",
date(l.expected_maturedon_date) as "Expected Matured On",
date(l.closedon_date) as "Closed Date",

l.principal_repaid_derived as "Principal Repaid",
l.principal_outstanding_derived as "Principal Outstanding",
laa.principal_overdue_derived as "Principal Overdue",

l.interest_repaid_derived as "Interest Repaid",
l.interest_outstanding_derived as "Interest Outstanding",
laa.interest_overdue_derived as "Interest Overdue",

l.fee_charges_repaid_derived as "Fees Repaid",
l.fee_charges_outstanding_derived  as "Fees Outstanding",
laa.fee_charges_overdue_derived as "Fees Overdue",

l.penalty_charges_repaid_derived as "Penalties Repaid",
l.penalty_charges_outstanding_derived as "Penalties Outstanding",
laa.penalty_charges_overdue_derived as "Penalties Overdue"

from m_office o
join m_office ounder on ounder.hierarchy like concat(o.hierarchy, ''%'')
and ounder.hierarchy like concat(''${currentUserHierarchy}'', ''%'')
join m_client c on c.office_id = ounder.id
join m_loan l on l.client_id = c.id
join m_product_loan pl on pl.id = l.product_id
left join r_enum_value st on st.enum_name = ''loan_status_id'' and st.enum_id = l.loan_status_id
left join m_staff lo on lo.id = l.loan_officer_id
left join m_currency cur on cur.code = l.currency_code
left join m_fund f on f.id = l.fund_id
left join m_loan_arrears_aging laa on laa.loan_id = l.id
where o.id = ${officeId}
and (l.currency_code = "${currencyId}" or "-1" = "${currencyId}")
and (l.product_id = "${loanProductId}" or "-1" = "${loanProductId}")
and (ifnull(l.loan_officer_id, -10) = "${loanOfficerId}" or "-1" = "${loanOfficerId}")
and (ifnull(l.fund_id, -10) = ${fundId} or -1 = ${fundId})
and (ifnull(l.loanpurpose_cv_id, -10) = ${loanPurposeId} or -1 = ${loanPurposeId})
group by l.id
order by ounder.hierarchy, l.currency_code, c.account_no, l.account_no',
'All loans past and present (active, closed, written-off, rejected, and other statuses). Based on Active Loans - Details.',
0, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM stretchy_report sr WHERE sr.report_name = 'All Loans');

INSERT INTO stretchy_report_parameter (report_id, parameter_id, report_parameter_name)
SELECT r.id, p.id, NULL
FROM stretchy_report r
JOIN stretchy_parameter p ON p.parameter_name IN (
  'OfficeIdSelectOne',
  'loanOfficerIdSelectAll',
  'currencyIdSelectAll',
  'fundIdSelectAll',
  'loanProductIdSelectAll',
  'loanPurposeIdSelectAll'
)
WHERE r.report_name = 'All Loans'
AND NOT EXISTS (
  SELECT 1 FROM stretchy_report_parameter srp
  WHERE srp.report_id = r.id AND srp.parameter_id = p.id
);

INSERT INTO m_permission (grouping, code, entity_name, action_name, can_maker_checker)
SELECT 'report', 'READ_All Loans', 'All Loans', 'READ', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM m_permission mp WHERE mp.code = 'READ_All Loans');
