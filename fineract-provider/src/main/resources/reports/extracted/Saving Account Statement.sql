-- report: Saving Account Statement
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), ondate (java.sql.Date), todate (java.sql.Date)

-- query: Account No
select msa.id,msa.account_no
from m_savings_account msa
  union all
select  -1,'ALL'
order by 2

-- query: Saving  account
select 
mst.transaction_date,
case mst.transaction_type_enum
when 1 then 'Deposit'
when 2 then 'Withdrawl'
end as 'type',
  IFnull(mpd.receipt_number, 0) 'Recept_no',
ifnull(concat('Ch: ', mpd.check_number,',','Re: ', mpd.receipt_number,',','B.N0: ',mpd.bank_number,',','R.NO: ',mpd.routing_code, ',','Note: ',ifnull(mn.note,' ')), '') 'Description',

if(mst.transaction_type_enum = 1, mst.amount,0) as 'Credited',
if(mst.transaction_type_enum = 2, mst.amount,0) as 'Debited',
mst.running_balance_derived 'Blance',
case mpd.payment_type_cv_id
when 19 then 'Cash'
when 20 then 'check'
when 21 then 'Airtel Money'
else ' '
end as 'transaction type',
concat(mc.display_name,'(','ID',mc.id,')'),
msa.account_no


from m_savings_account_transaction mst
left join m_savings_account msa
on msa.id=mst.savings_account_id
left join m_payment_detail mpd
on mst.payment_detail_id=mpd.id
left join m_savings_product msp
on msp.id=msa.product_id
left join m_client mc
on msa.client_id=mc.id
left join m_note mn
on mn.savings_account_id=mst.id
where  mst.savings_account_id=${Account} and mst.transaction_date between ${ondate} and ${todate}

-- query: client name
select mc.display_name,
ifnull(msa.account_no,0)'Account no'
from  m_client mc
left join m_savings_account msa
on msa.client_id=mc.id

