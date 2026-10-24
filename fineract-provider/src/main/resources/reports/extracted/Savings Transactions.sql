-- report: Savings Transactions
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String), accountNo (java.lang.String), fromDate (java.sql.Date), toDate (java.sql.Date)

-- query: AccountSelect
select id,account_no from m_savings_account
where status_enum in (300,600)

-- query: MFIName
select name from m_office
where hierarchy='.'

-- query: Details
select mc.display_name 'Client Name',
msp.name 'Product Name',
sa.account_no 'Account No',
sat.transaction_date as 'Transaction Date',
ev.enum_message_property as Details,
if(ev.enum_type = 0,sat.amount,0) as Deposit,
if(ev.enum_type = 1,sat.amount,0) as Withdrawal,
sat.running_balance_derived as Balance  
from m_savings_account sa 
inner join m_savings_account_transaction sat on sat.savings_account_id = sa.id 
inner join r_enum_value ev on ev.enum_name = 'savings_transaction_type_enum' and ev.enum_id = sat.transaction_type_enum 
inner join m_savings_product msp on msp.id=sa.product_id
inner join m_client mc on mc.id=sa.client_id
where sa.account_no=${accountNo}
and sat.is_reversed=0 
and sat.transaction_date BETWEEN ${fromDate} AND ${toDate} 
order by sat.transaction_date,sat.created_date,sat.id;

