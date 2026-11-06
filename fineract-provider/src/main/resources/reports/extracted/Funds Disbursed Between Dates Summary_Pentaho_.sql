-- report: Funds Disbursed Between Dates Summary(Pentaho)
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String), startDate (java.sql.Date), endDate (java.sql.Date)

-- query: fundId
select id, `name`
from m_fund
union all
select -10, 'None'
union 
Select -1 ,'ALL'
order by 2

-- query: Funds Disbursed Between Dates Summary
select ifnull(f.`name`, '-') as Fund,  ifnull(cur.display_symbol, l.currency_code) as Currency, 
round(sum(l.principal_amount), 4) as disbursed_amount
from m_office ounder 
join m_client c on c.office_id = ounder.id
join m_loan l on l.client_id = c.id
join m_currency cur on cur.`code` = l.currency_code
left join m_fund f on f.id = l.fund_id
where disbursedon_date between ${startDate} and ${endDate}
and (ifnull(l.fund_id, -10) = ${fundId} or -1 = ${fundId})
  and ounder.hierarchy like concat(${userhierarchy}, '%')
  and (l.currency_code = ${CurrencyId} or '-1' = ${CurrencyId})

group by ifnull(f.`name`, '-') , ifnull(cur.display_symbol, l.currency_code)
order by ifnull(f.`name`, '-') , ifnull(cur.display_symbol, l.currency_code)

-- query: CurrencyId
select `code`, `name`
from m_organisation_currency
union 
select -1,'All'
order by `code`

