-- report: Client Saving Transactions
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userid (java.lang.Long), savingsAccountId (java.lang.String), startDate (java.sql.Date), endDate (java.sql.Date)

-- query: user
select concat(firstname,' ',lastname) name from m_appuser
where id=${userid}

-- query: Saving Transaction
select st.id as 'Transaction ID',
       st.transaction_date as 'Transaction Date',
       concat(': ',msp.name,'  (#',msa.account_no,')')as name_no ,
       concat(': ',IF(msa.client_id is NULL,(SELECT mg.display_name FROM m_group mg WHERE mg.id=msa.group_id),(SELECT mc.display_name FROM m_client mc WHERE mc.id=msa.client_id))) as client_name,
       re.enum_value as 'Transaction Type',
       ifnull(case st.transaction_type_enum
                  when 2 then st.amount
                  end ,0) as 'Debit',
       ifnull(case st.transaction_type_enum
                  when 1 then st.amount
                  when 3 then st.amount
                  end,0) as 'Credit',
       ifnull(st.running_balance_derived,0) as 'Balance',
       case st.is_reversed
           when 0 then 'No'
           else 'Yes'
           end as 'Reversed'
      from m_savings_account_transaction st
      inner join m_savings_account msa on msa.account_no=st.savings_account_id
      inner join m_savings_product msp on msp.id=msa.product_id
      inner join r_enum_value re
           on re.enum_id=st.transaction_type_enum and re.enum_name='savings_transaction_type_enum'
where msa.account_no=${savingsAccountId}
and (st.transaction_date between ${startDate} and ${endDate})
order by 2 desc,is_reversed

-- query: Savings Transaction 2
select 
st.id as 'Transaction ID',
st.transaction_date as 'Transaction Date',
concat(': ',o.name) 'Branch',
concat(': ',msp.name,'  (#',msa.account_no,')')as name_no ,
#concat(': ',IF(msa.client_id is NULL,(SELECT mg.display_name FROM m_group mg WHERE mg.id=msa.group_id),(SELECT mc.display_name FROM m_client mc WHERE mc.id=msa.client_id))) as client_name,
concat(': ',if (msa.group_id is null,cl.display_name,gr.display_name)) client_name,
re.enum_value as 'Transaction Type',
ifnull(case st.transaction_type_enum
                  when 2 then st.amount
                  end ,0) as 'Debit',
ifnull(case st.transaction_type_enum
                  when 1 then st.amount
                  when 3 then st.amount
                  end,0) as 'Credit',
ifnull(st.running_balance_derived,0) as 'Balance',
       case st.is_reversed
           when 0 then 'No'
           else 'Yes'
           end as 'Reversed',
           concat(': ',msa.account_balance_derived) 'Balance Amount',
           concat(': ',msa.nominal_annual_interest_rate) 'Interest Rate',
           concat(': ',ifnull(sf.display_name,'N/A')) Staff,
           c.name 'Currency'
from m_savings_account_transaction st
inner join m_savings_account msa on msa.id =st.savings_account_id
inner join m_savings_product msp on msp.id=msa.product_id
inner join r_enum_value re
           on re.enum_id=st.transaction_type_enum and re.enum_name='savings_transaction_type_enum'
left join m_group gr on gr.id=msa.group_id
left join m_client cl on cl.id=msa.client_id
inner join m_office o on o.id=cl.office_id or o.id = gr.office_id
left join m_staff sf on sf.id=msa.field_officer_id
inner join m_currency c on c.code = msa.currency_code
where msa.account_no=${savingsAccountId}
#where msa.account_no='000000005'
and (st.transaction_date between ${startDate} and ${endDate})
#and (st.transaction_date between '2015-01-01' and '2015-05-26')
order by 2 desc,is_reversed

