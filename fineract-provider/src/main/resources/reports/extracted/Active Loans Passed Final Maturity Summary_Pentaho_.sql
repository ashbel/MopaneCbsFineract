-- report: Active Loans Passed Final Maturity Summary(Pentaho)
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String)

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

-- query: parType
select * from
(select 1 as id, "Principal Only" as `name` union all
select 2, "Principal + Interest" union all
select 3, "Principal + Interest + Fees" union all
select 4, "Principal + Interest + Fees + Penalties") x
order by x.`id`

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

-- query: Active Loans Passed Final Maturity Summary
select concat(repeat("..",   
   ((LENGTH(mo.`hierarchy`) - LENGTH(REPLACE(mo.`hierarchy`, '.', '')) - 1))), mo.`name`) as "Office/Branch",x.bname as name,x.currency as Currency,
 x.client_count as "No. of Clients", x.active_loan_count as "No. Active Loans", x. arrears_loan_count as "No. of Loans in Arrears",
x.principal as "Total Loans Disbursed", x.principal_repaid as "Principal Repaid", x.principal_outstanding as "Principal Outstanding", x.principal_overdue as "Principal Overdue",
x.interest as "Total Interest", x.interest_repaid as "Interest Repaid", x.interest_outstanding as "Interest Outstanding", x.interest_overdue as "Interest Overdue",
x.fees as "Total Fees", x.fees_repaid as "Fees Repaid", x.fees_outstanding as "Fees Outstanding", x.fees_overdue as "Fees Overdue",
x.penalties as "Total Penalties", x.penalties_repaid as "Penalties Repaid", x.penalties_outstanding as "Penalties Outstanding", x.penalties_overdue as "Penalties Overdue",
(case
	when ${parType} = 1 then
    cast(round((x.principal_overdue * 100) / x.principal_outstanding, 2) as char)
	when ${parType} = 2 then
    cast(round(((x.principal_overdue + x.interest_overdue) * 100) / (x.principal_outstanding + x.interest_outstanding), 2) as char)
	when ${parType} = 3 then
    cast(round(((x.principal_overdue + x.interest_overdue + x.fees_overdue) * 100) / (x.principal_outstanding + x.interest_outstanding + x.fees_outstanding), 2) as char)
	when ${parType} = 4 then
    cast(round(((x.principal_overdue + x.interest_overdue + x.fees_overdue + x.penalties_overdue) * 100) / (x.principal_outstanding + x.interest_outstanding + x.fees_outstanding + x.penalties_overdue), 2) as char)
	else "invalid PAR Type"
	end) as "Portfolio at Risk %"

from m_office mo
join 
(

select ounder.id as branch,ounder.name as bname,
ifnull(cur.display_symbol, l.currency_code) as currency,
count(distinct(c.id)) as client_count, 
count(distinct(l.id)) as  active_loan_count,
count(distinct(laa.loan_id)  ) as arrears_loan_count,

sum(l.principal_disbursed_derived) as principal,
sum(l.principal_repaid_derived) as principal_repaid,
sum(l.principal_outstanding_derived) as principal_outstanding,
sum(ifnull(laa.principal_overdue_derived,0)) as principal_overdue,

sum(l.interest_charged_derived) as interest,
sum(l.interest_repaid_derived) as interest_repaid,
sum(l.interest_outstanding_derived) as interest_outstanding,
sum(ifnull(laa.interest_overdue_derived,0)) as interest_overdue,

sum(l.fee_charges_charged_derived) as fees,
sum(l.fee_charges_repaid_derived) as fees_repaid,
sum(l.fee_charges_outstanding_derived)  as fees_outstanding,
sum(ifnull(laa.fee_charges_overdue_derived,0)) as fees_overdue,

sum(l.penalty_charges_charged_derived) as penalties,
sum(l.penalty_charges_repaid_derived) as penalties_repaid,
sum(l.penalty_charges_outstanding_derived) as penalties_outstanding,
sum(ifnull(laa.penalty_charges_overdue_derived,0)) as penalties_overdue

from m_office o 
join m_office ounder on ounder.hierarchy like concat(o.hierarchy, '%')
and ounder.hierarchy like concat(${userhierarchy}, '%')
join m_client c on c.office_id = ounder.id
join m_loan l on l.client_id = c.id
left join m_currency cur on cur.code = l.currency_code
left join m_loan_arrears_aging laa on laa.loan_id = l.id


where o.id = ${Branch} 
and (ifnull(l.loan_officer_id, -10) = ${Loan Officer} or "-1" = ${Loan Officer})
and (l.currency_code = ${CurrencyId} or "-1" = ${CurrencyId})
and (l.product_id = ${loanProductId} or "-1" = ${loanProductId})
and (ifnull(l.fund_id, -10) = ${fundId} or -1 = ${fundId})
and (ifnull(l.loanpurpose_cv_id, -10) = ${loanPurposeId} or "-1" = ${loanPurposeId})
and l.loan_status_id = 300
and l.expected_maturedon_date < curdate()
group by ounder.id, l.currency_code) x on x.branch = mo.id
order by mo.hierarchy, x.Currency

