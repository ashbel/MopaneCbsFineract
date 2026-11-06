-- report: Expected Payments By Date - Formatted
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String), startDate (java.sql.Date), endDate (java.sql.Date)

-- query: SelectedOffice
select name from m_office
where id=${officeId}

-- query: selectOffice
select id, 
concat(substring("         ", 1, 
((LENGTH(`hierarchy`) - LENGTH(REPLACE(`hierarchy`, '.', '')) - 1) * 4)), 
   `name`) as name
from m_office
order by hierarchy

-- query: MFIName
select name from m_office
where hierarchy='.'

-- query: Details
SELECT 
      ounder.name 'Office', 
      IFNULL(ms.display_name,'-') 'Loan Officer',
	  mc.account_no 'Client Account Number',
	  mc.display_name 'Client',
	  mp.name 'Product',
	  ml.account_no 'Loan Account Number',
	  mr.duedate 'Due Date',
	  mr.installment 'Installment',
	  cu.name 'Currency',
	  mr.principal_amount- IFNULL(mr.principal_completed_derived,0) 'Principal Due',
	  IFNULL(mr.interest_amount- IFNULL(IFNULL(mr.interest_completed_derived,mr.interest_waived_derived),0),0) 'Interest Due', 
	  IFNULL(mr.fee_charges_amount,0)- IFNULL(IFNULL(mr.fee_charges_completed_derived,mr.fee_charges_waived_derived),0) 'Fees Due', 
	  IFNULL(mr.penalty_charges_amount,0)- IFNULL(IFNULL(mr.penalty_charges_completed_derived,mr.penalty_charges_waived_derived),0) 'Penalty Due',
      (mr.principal_amount- IFNULL(mr.principal_completed_derived,0)) +
       IFNULL(mr.interest_amount- IFNULL(IFNULL(mr.interest_completed_derived,mr.interest_waived_derived),0),0) + 
       (IFNULL(mr.fee_charges_amount,0)- IFNULL(IFNULL(mr.fee_charges_completed_derived,mr.fee_charges_waived_derived),0)) + 
       (IFNULL(mr.penalty_charges_amount,0)- IFNULL(IFNULL(mr.penalty_charges_completed_derived,mr.penalty_charges_waived_derived),0)) 'Total Due', 
      
     ifnull(mlaa.total_overdue_derived,0) 'Total Overdue'
										 
 FROM m_office mo
  JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
  AND ounder.hierarchy like CONCAT(${userhierarchy}, '%')
	
  LEFT JOIN m_client mc ON mc.office_id=ounder.id
  LEFT JOIN m_loan ml ON ml.client_id=mc.id AND ml.loan_status_id=300
  LEFT JOIN m_loan_arrears_aging mlaa ON mlaa.loan_id=ml.id
  LEFT JOIN m_loan_repayment_schedule mr ON mr.loan_id=ml.id AND mr.completed_derived=0
  LEFT JOIN m_product_loan mp ON mp.id=ml.product_id
  LEFT JOIN m_staff ms ON ms.id=ml.loan_officer_id
  LEFT JOIN m_currency cu ON cu.code=ml.currency_code
 WHERE mo.id=${officeId}
 AND (IFNULL(ml.loan_officer_id, -10) = ${loanOfficerId} OR -1 = ${loanOfficerId})
 AND mr.duedate BETWEEN ${startDate} AND ${endDate}
 ORDER BY ounder.id,mr.duedate,cu.display_symbol,ml.account_no

-- query: selectLoanOfficer
(select id, 

display_name as `Name` from m_staff
where is_loan_officer = true)
union all
(select -10, '-')
union all
(select -1,'All')
order by 2

