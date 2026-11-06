-- report: Written-Off Loans(Pentaho)
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String), startDate (java.sql.Date), endDate (java.sql.Date)

-- query: Branch
Select id, 
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

-- query: fundId
select id, `name`
from m_fund
union all
select -10, 'None'
union 
Select -1 ,'ALL'
order by 2

-- query: CurrencyId
select `code`, `name`
from m_organisation_currency
union 
select -1,'All'
order by `code`

-- query: loanProductId
select p.id, p.`name`
from m_product_loan p
where (p.currency_code = ${CurrencyId} or '-1'= ${CurrencyId})


union 
select -1, 'All'

order by 2

-- query: loanPurposeId
select -10 as id, 'None' as code_value
union all
select * from (select v.id, v.code_value
from m_code c
join m_code_value v on v.code_id = c.id
where c.code_name = "loanPurpose"
order by v.order_position)  x 
union 
select -1 ,'All'

-- query: Written-Off Loans
SELECT 
concat(repeat("..",   
   ((LENGTH(ounder.`hierarchy`) - LENGTH(REPLACE(ounder.`hierarchy`, '.', '')) - 1))), ounder.`name`) as "Office/Branch",ounder.name AS NAME, ounder.id,
ifnull(cur.display_symbol, ml.currency_code) as Currency,  
c.account_no as "Client Account No.",
c.display_name AS 'Client Name',
ml.account_no AS 'Loan Account No.',
mpl.name AS 'Product Name',
ml.disbursedon_date AS 'Disbursed Date',
lt.transaction_date AS 'Written Off date',
ml.principal_amount as "Loan Amount",
ifnull(lt.principal_portion_derived, 0) AS 'Written-Off Principal',
ifnull(lt.interest_portion_derived, 0) AS 'Written-Off Interest',
ifnull(lt.fee_charges_portion_derived,0) AS 'Written-Off Fees',
ifnull(lt.penalty_charges_portion_derived,0) AS 'Written-Off Penalties',
n.note AS 'Reason For Write-Off',
IFNULL(ms.display_name,'-') AS 'Loan Officer Name'
FROM m_office o
JOIN m_office ounder ON ounder.hierarchy like concat(o.hierarchy, '%')
AND ounder.hierarchy like CONCAT(${userhierarchy}, '%')
JOIN m_client c ON c.office_id = ounder.id
JOIN m_loan ml ON ml.client_id = c.id
JOIN m_product_loan mpl ON mpl.id=ml.product_id
LEFT JOIN m_staff ms ON ms.id=ml.loan_officer_id
JOIN m_loan_transaction lt ON lt.loan_id = ml.id
LEFT JOIN m_note n ON n.loan_transaction_id = lt.id
LEFT JOIN m_currency cur on cur.code = ml.currency_code
WHERE lt.transaction_type_enum = 6 /*write-off */
AND lt.is_reversed = 0
AND ml.loan_status_id=601   
AND o.id=${officeId}
AND (mpl.id=${loanProductId} OR ${loanProductId}=-1)
AND lt.transaction_date BETWEEN ${startDate} AND ${endDate}
AND (ml.currency_code = ${currencyId} or -1 = ${currencyId})
ORDER BY ounder.hierarchy, ifnull(cur.display_symbol, ml.currency_code), ml.account_no

