-- report: Client Loan Account Schedule
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String), userid (java.lang.Long), selectLoan (java.lang.String), startDate (java.sql.Date), endDate (java.sql.Date)

-- query: activityDetails2
SELECT det.id,
det.dates 'Date',
ifnull(comments.Note,'-') Note,
det.Principle,
det.Interest,
det.Fees,
det.Penalty,
det.Total_Paid,
det.Outstanding_Amount,
det.Transaction_Type,
det.Reversed,det.enu
FROM (select mtx.id, mtx.transaction_type_enum enu
,mtx.transaction_date 'dates'
,case mtx.transaction_type_enum when 1 then IFNULL(mtx.amount,0) 
 else ifnull(mtx.principal_portion_derived,0) end 'Principle'

,IFNULL(mtx.interest_portion_derived,0) Interest
,IFNULL(mtx.fee_charges_portion_derived,0) Fees
,IFNULL(mtx.penalty_charges_portion_derived,0) Penalty
,IF(mtx.transaction_type_enum = 1,0, IFNULL(mtx.amount,0)) Total_Paid
,
(ml.principal_disbursed_derived + ml.interest_charged_derived + 
ml.fee_charges_charged_derived + 
ml.penalty_charges_charged_derived) 'Outstanding_Amount'
,ev.enum_value 'Transaction_Type'
,case mtx.is_reversed when 1 then 'Yes' else 'No' end Reversed
 from m_loan ml
 inner join m_loan_transaction mtx ON mtx.loan_id = ml.id
 inner join r_enum_value ev on ev.enum_id=mtx.transaction_type_enum and ev.enum_name="transaction_type_enum"
 where mtx.loan_id = (select ml.id from m_loan ml where ml.account_no=${selectLoan})
 and mtx.transaction_type_enum in (1,3,5,2,4)
and mtx.is_reversed=0
#group by 1
) det
LEFT JOIN 
 (SELECT a.id comment_id, IFNULL(a.tid,(
SELECT MIN(mtx.id)
FROM m_loan_transaction mtx
WHERE mtx.loan_id=(select ml.id from m_loan ml where ml.account_no=${selectLoan}))) trx_id,
a.note note
FROM (
SELECT mn.id id,
 mn.loan_transaction_id tid,
		mn.loan_id lid,
		mn.note Note
FROM m_note mn
WHERE mn.id IN (
SELECT MAX(mn.id)
FROM m_note mn
WHERE mn.note_type_enum=200
GROUP BY mn.loan_id) UNION
SELECT mn2.id,
 mn2.loan_transaction_id,
 mn2.loan_id,
 mn2.note
FROM m_note mn2
WHERE mn2.note_type_enum=300) a
WHERE a.lid=(select ml.id from m_loan ml where ml.account_no=${selectLoan})
ORDER BY a.lid,a.id) 
comments ON comments.trx_id=det.id
WHERE det.dates between ${startDate} and ${endDate}
ORDER BY CASE WHEN det.enu = 1 THEN 1
 WHEN det.enu = 5 THEN 2
 WHEN det.enu = 2 THEN 3 END,1,2

-- query: selectLoan
SELECT distinct ml.account_no
 FROM m_office mo
  JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%')
  AND ounder.hierarchy like CONCAT(${userhierarchy}, '%')
  INNER JOIN m_client mc on mc.office_id=ounder.id
  LEFT JOIN m_loan ml ON ml.client_id=mc.id AND ml.loan_status_id=300

-- query: otherDetails
select ifnull(ms.display_name,'No Loan Officer Assigned') loanOfficer,
mc.display_name 'client',
mc.account_no fileId,
ml.account_no accountNo,
ml.principal_amount disbAmt,
ml.disbursedon_date disbDate,
ml.number_of_repayments noOfInst,
mo.name,
concat(left(ml.nominal_interest_rate_per_period,5),if(ml.interest_period_frequency_enum=2,'% Per month','% Per year')) intRate
 from m_loan ml
left join m_client mc on mc.id=ml.client_id
left join m_staff ms on ms.id=ml.loan_officer_id
left join m_office mo on mo.id=mc.office_id
where ml.account_no=${selectLoan}

-- query: MFIName
select name from m_office
where hierarchy='.'

-- query: loanScheduleDetails
SELECT mr.installment 'Installment',
moc.name 'Currency',
(ifnull(mr.principal_amount,0)- IFNULL(mr.principal_completed_derived,0)) 'Principle Due',
IFNULL((mr.interest_amount)- IFNULL(mr.interest_completed_derived,0),0) 'Interest Due',
(IFNULL(mr.fee_charges_amount,0)- IFNULL(mr.fee_charges_completed_derived,0)) 'Fees Due',
(IFNULL(mr.penalty_charges_amount,0)- IFNULL(mr.penalty_charges_completed_derived,0)) 'Penalty Due',
((ifnull(mr.principal_amount,0)- IFNULL(mr.principal_completed_derived,0))+
IFNULL((mr.interest_amount)- IFNULL(mr.interest_completed_derived,0),0)+
(IFNULL(mr.fee_charges_amount,0)- IFNULL(mr.fee_charges_completed_derived,0))+
(IFNULL(mr.penalty_charges_amount,0)- IFNULL(mr.penalty_charges_completed_derived,0))) 'Total Due',
mr.duedate 'Date Expected', IF((((mr.principal_amount)- IFNULL(mr.principal_completed_derived,0))+
(IFNULL(mr.interest_amount,0)- IFNULL(mr.interest_completed_derived,0))+
(IFNULL(mr.fee_charges_amount,0)- IFNULL(mr.fee_charges_completed_derived,0))+
(IFNULL(mr.penalty_charges_amount,0)- IFNULL(mr.penalty_charges_completed_derived,0)))=0,'Paid','Unpaid') 'Status'
FROM
m_loan_repayment_schedule mr
INNER JOIN m_loan ml ON ml.id=mr.loan_id
INNER JOIN m_organisation_currency moc ON moc.code=ml.currency_code
WHERE ml.id=(select ml.id from m_loan ml where ml.account_no=${selectLoan})

-- query: activityDetails
SELECT 
det.dates 'Date',
ifnull(comments.Note,'-') Note,
det.Principle,
det.Interest,
det.Fees,
det.Penalty,
det.Total_Paid,
det.Outstanding_Amount
FROM (
SELECT mtx.id id,
mtx.loan_id,
mtx.transaction_date 'dates', 
IFNULL(mtx.amount,0) Principle, 
IFNULL(mtx.interest_portion_derived,0) Interest, 
IFNULL(mtx.fee_charges_portion_derived,0) Fees, 
IFNULL(mtx.penalty_charges_portion_derived,0) Penalty,
0.000000 Total_Paid,
(ml.principal_amount + ml.interest_charged_derived + ml.fee_charges_charged_derived + ml.penalty_charges_charged_derived) Outstanding_Amount
FROM
m_loan_transaction mtx
INNER JOIN m_loan ml on ml.id=mtx.loan_id
WHERE mtx.transaction_type_enum=1
AND ml.id=(select ml.id from m_loan ml where ml.account_no=${selectLoan}) 

UNION

SELECT mtx.id,
mtx.loan_id,
mtx.transaction_date 'dates', 
IFNULL(mtx.principal_portion_derived,0) Principal, 
IFNULL(mtx.interest_portion_derived,0) Interest, 
IFNULL(mtx.fee_charges_portion_derived,0) Fees, 
IFNULL(mtx.penalty_charges_portion_derived,0) Penalty,
mtx.amount Total_Paid,
Outs.outs Outstanding_Amount

FROM
m_loan_transaction mtx
INNER JOIN m_loan ml on ml.id=mtx.loan_id
INNER JOIN (select m.id outId,
(ml.principal_amount + ml.interest_charged_derived + ml.fee_charges_charged_derived + ml.penalty_charges_charged_derived - ml.interest_waived_derived - ml.fee_charges_waived_derived - ml.penalty_charges_waived_derived)-(m.amount + ifnull((select sum(amount)
from m_loan_transaction mt
where mt.id < m.id and mt.loan_id=(select ml.id from m_loan ml where ml.account_no=${selectLoan}) and mt.transaction_type_enum =2 and is_reversed=0),0) ) outs
from m_loan_transaction m
inner join m_loan ml on ml.id=m.loan_id
where m.transaction_type_enum =2
and m.loan_id=(select ml.id from m_loan ml where ml.account_no=${selectLoan})) Outs on Outs.outId=mtx.id

WHERE mtx.transaction_type_enum=2
and mtx.is_reversed=0
and ml.id=(select ml.id from m_loan ml where ml.account_no=${selectLoan})) det

LEFT JOIN 
 (SELECT a.id comment_id, IFNULL(a.tid,(
SELECT MIN(mtx.id)
FROM m_loan_transaction mtx
WHERE mtx.loan_id=(select ml.id from m_loan ml where ml.account_no=${selectLoan}))) trx_id,
a.note note
FROM (
SELECT mn.id id,
 mn.loan_transaction_id tid,
		mn.loan_id lid,
		mn.note Note
FROM m_note mn
WHERE mn.id IN (
SELECT MAX(mn.id)
FROM m_note mn
WHERE mn.note_type_enum=200
GROUP BY mn.loan_id) UNION
SELECT mn2.id,
 mn2.loan_transaction_id,
 mn2.loan_id,
 mn2.note
FROM m_note mn2
WHERE mn2.note_type_enum=300) a
WHERE a.lid=(select ml.id from m_loan ml where ml.account_no=${selectLoan})
ORDER BY a.lid,a.id) 
comments ON comments.trx_id=det.id
WHERE det.dates between ${startDate} and ${endDate}

-- query: user
select concat(firstname,' ',lastname) name from m_appuser
where id=${userid}

