-- report: Obligation Met Loans Summary(Pentaho)
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String), Startdate (java.sql.Date), Enddate (java.sql.Date)

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

-- query: obligDateType
select * from
(select 1 as id, "Closed" as `name` union all
select 2, "Disbursal" ) x
order by x.`id`

-- query: Obligation Met Loans Summary
select concat(repeat("..",   
   ((LENGTH(ounder.`hierarchy`) - LENGTH(REPLACE(ounder.`hierarchy`, '.', '')) - 1))), ounder.`name`) as "Office/Branch",ounder.name, ounder.id,
ifnull(cur.display_symbol, l.currency_code) as Currency,
concat("From"," ",${startDate}," ","To"," " ,${endDate})as "period",
count(distinct(c.id)) as "No. of Clients",
count(distinct(l.id)) as "No. of Loans",
sum(l.principal_amount) as "Total Loan Amount", 
sum(l.principal_repaid_derived) as "Total Principal Repaid",
sum(l.interest_repaid_derived) as "Total Interest Repaid",
sum(l.fee_charges_repaid_derived) as "Total Fees Repaid",
sum(l.penalty_charges_repaid_derived) as "Total Penalties Repaid",
sum(l.interest_waived_derived) as "Total Interest Waived",
sum(l.fee_charges_waived_derived) as "Total Fees Waived",
sum(l.penalty_charges_waived_derived) as "Total Penalties Waived"

from m_office o 
join m_office ounder on ounder.hierarchy like concat(o.hierarchy, '%')
and ounder.hierarchy like concat(${userhierarchy}, '%')
join m_client c on c.office_id = ounder.id
join m_loan l on l.client_id = c.id
join m_product_loan pl on pl.id = l.product_id
left join m_staff lo on lo.id = l.loan_officer_id
left join m_currency cur on cur.code = l.currency_code
left join m_fund f on f.id = l.fund_id
where o.id = ${Branch}
and (ifnull(l.loan_officer_id, -10) = ${Loan Officer} or "-1" = ${Loan Officer})
and (l.currency_code = ${CurrencyId} or "-1" = ${CurrencyId})
and (l.product_id = ${loanProductId} or "-1" = ${loanProductId})
and (ifnull(l.loanpurpose_cv_id, -10) = ${loanPurposeId} or "-1" = ${loanPurposeId})
and (ifnull(l.fund_id, -10) = ${fundId} or -1 = ${fundId})
and (case
	when ${obligDateType} = 1 then
    l.closedon_date between ${Startdate} and ${Enddate}
	when ${obligDateType} = 2 then
    l.disbursedon_date between ${Startdate} and ${Enddate}
	else 1 = 1
	end)
and l.loan_status_id = 600


group by ounder.hierarchy, l.currency_code
order by ounder.hierarchy, l.currency_code

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

