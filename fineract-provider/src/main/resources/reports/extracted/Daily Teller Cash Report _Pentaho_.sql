-- report: Daily Teller Cash Report (Pentaho)
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String), userid (java.lang.Long), asOnDate (java.sql.Date)

-- query: teller
select id, name from m_tellers where office_id = ${officeId}

-- query: currency
select `code`, `name` from m_organisation_currency order by `code`

-- query: office
select id, 
name
from m_office
order by id

-- query: cashier
select c.id, s.display_name from m_cashiers as c left join m_staff as s on c.staff_id = s.id where c.teller_id = ${tellerId}

-- query: callprocedure
call CashierTransactionSummary(${officeId},${tellerId},${cashierId},${currencyCode},${asOnDate})

