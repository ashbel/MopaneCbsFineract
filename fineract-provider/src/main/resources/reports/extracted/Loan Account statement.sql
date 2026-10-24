-- report: Loan Account statement
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), ondate (java.sql.Date), todate (java.sql.Date)

-- query: Account No
select ml.id,mc.account_no
from m_loan ml
left join m_client mc
on ml.client_id=mc.id
  union all
select  -1,'ALL'
order by 2

-- query: Loan Account
select mlt.transaction_date 'Date',
case mlt.transaction_type_enum
when 1 then 'Disbursement'
when 2 then 'Repayment'
when 3 then 'Contra'
when 4 then 'Waive Interest'
when 5 then 'Repayment At Disbursement'
when 6 then 'Write-Off'
when 7 then 'Marked for Rescheduling'
when 8 then 'Recovery Repayment'
when 9 then 'Waive Charges'
when 10 then 'Apply Charges'
when 11 then 'Apply Interest'
end as 'type',

if(mlt.transaction_type_enum = 1, mlt.amount,0) as 'Disbursement',
if(mlt.transaction_type_enum = 2, mlt.amount,0) as 'Repayment',
if(mlt.transaction_type_enum = 3, mlt.amount,0) as 'Contra',
if(mlt.transaction_type_enum = 4, mlt.amount,0) as 'Waive Interest',
if(mlt.transaction_type_enum = 5, mlt.amount,0) as 'Repayment At Disbursement',
if(mlt.transaction_type_enum = 6, mlt.amount,0) as 'Write-Off',
if(mlt.transaction_type_enum = 7, mlt.amount,0) as 'Marked for Rescheduling',
if(mlt.transaction_type_enum = 8, mlt.amount,0) as 'Recovery Repayment',
if(mlt.transaction_type_enum = 9, mlt.amount,0) as 'Waive Charges',
if(mlt.transaction_type_enum = 10, mlt.amount,0) as 'Apply Charges',
if(mlt.transaction_type_enum = 11, mlt.amount,0) as 'Apply Interest',
ifnull(concat('Ch: ', mpd.check_number,',','Re: ', mpd.receipt_number,',','B.No: ',mpd.bank_number,',','R.NO: ',mpd.routing_code,',','Note: ',mn.note,''), '') 'Description',
ifnull(mlt.amount,0) 'Amount',
ifnull(mlt.principal_portion_derived,0) 'Principal',
ifnull(mlt.interest_portion_derived,0) 'Intrest',
ifnull(mpd.receipt_number,0) 'Recepit No',
case mpd.payment_type_cv_id
when 19 then 'Cash'
when 20 then 'check'
when 21 then 'Airtel Money'
else ' '
end as 'transaction type',
concat(mc.display_name,'(','ID',mc.id,')') 'Name',
ml.account_no
 
from m_loan_transaction mlt
left join m_payment_detail mpd
on mpd.id=mlt.payment_detail_id
left join m_loan ml
on ml.id=mlt.loan_id
left join m_product_loan mpl
on mpl.id=ml.product_id
left join m_client mc
on ml.client_id=mc.id
left join m_note mn
on mn.loan_transaction_id=mlt.id
where ml.account_no=${Account} and mlt.transaction_date between ${ondate} and ${todate}

