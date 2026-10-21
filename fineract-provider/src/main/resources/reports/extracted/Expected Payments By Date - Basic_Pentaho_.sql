-- report: Expected Payments By Date - Basic(Pentaho)
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String), startDate (java.sql.Date), endDate (java.sql.Date)

-- query: Expected Payments By Date - Basic
SELECT 
      ounder.name 'Office', 
      IFNULL(ms.display_name,'-') 'Loan Officer',
	  mc.account_no 'Client Account Number',
	  mc.display_name 'Name',
	  mp.name 'Product',
	  ml.account_no 'Loan Account Number',
	  mr.duedate 'Due Date',
	  mr.installment 'Installment',
	  cu.display_symbol 'Currency',
	  mr.principal_amount- IFNULL(mr.principal_completed_derived,0) 'Principal Due',
	  mr.interest_amount- IFNULL(IFNULL(mr.interest_completed_derived,mr.interest_waived_derived),0) 'Interest Due', 
	  IFNULL(mr.fee_charges_amount,0)- IFNULL(IFNULL(mr.fee_charges_completed_derived,mr.fee_charges_waived_derived),0) 'Fees Due', 
	  IFNULL(mr.penalty_charges_amount,0)- IFNULL(IFNULL(mr.penalty_charges_completed_derived,mr.penalty_charges_waived_derived),0) 'Penalty Due',
      (mr.principal_amount- IFNULL(mr.principal_completed_derived,0)) +
       (mr.interest_amount- IFNULL(IFNULL(mr.interest_completed_derived,mr.interest_waived_derived),0)) + 
       (IFNULL(mr.fee_charges_amount,0)- IFNULL(IFNULL(mr.fee_charges_completed_derived,mr.fee_charges_waived_derived),0)) + 
       (IFNULL(mr.penalty_charges_amount,0)- IFNULL(IFNULL(mr.penalty_charges_completed_derived,mr.penalty_charges_waived_derived),0)) 'Total Due', 
     mlaa.total_overdue_derived 'Total Overdue'
										 
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
  where mo.id = ${Branch} 
  and (ifnull(ml.loan_officer_id, -10) = ${Loan Officer} or "-1" = ${Loan Officer})
   AND mr.duedate BETWEEN ${startDate} AND ${endDate}


 ORDER BY ounder.id,mr.duedate,ml.account_no

-- query: Branch
select id, 
concat(substring("........................................", 1, 
   

((LENGTH(`hierarchy`) - LENGTH(REPLACE(`hierarchy`, '.', '')) - 1) * 4)), 
   `name`) as tc
from m_office
where hierarchy like concat

(${userhierarchy}, '%')
order by hierarchy

-- query: Loan Officer
select lo.id, lo.display_name as `Name` 
from m_office o 
join m_office ounder on ounder.hierarchy like concat(o.hierarchy, '%')
join m_staff lo on lo.office_id = ounder.id
where lo.is_loan_officer = 1
and o.id = ${Branch}
union all
(select -10, 'None')
union 
select -1, 'ALL'
order by 2

