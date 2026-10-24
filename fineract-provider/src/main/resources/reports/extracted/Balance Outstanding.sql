-- report: Balance Outstanding
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String), ondate (java.sql.Date), userid (java.lang.Long)

-- query: User
select concat(firstname,' ',lastname) name from m_appuser
where id=${userid}

-- query: Details
select l.loan_type_enum
,ounder.id branch
,0 as cid
,0 as gid
,l.account_no accountNo
,c.display_name name
,l.principal_disbursed_derived
,tx.outstanding_loan_balance_derived
,ev.enum_value
,case l.loan_type_enum
	when 1 then 'Individual'
	when 2 then 'Group'
	when 3 then 'JLG'
	end as loanType
,l.disbursedon_date disbDate
,ifnull(gn.code_value,'-') gender
,'NA' groupName
,ifnull(s.display_name,'NA') loanOfficer
,ifnull(f.name,'NA') fund
,'NA' as centerName
,ounder.name branchName,IFNULL(mcv.CODE_DESCRIPTION,'NA') LOANPURPOSE
from m_office o 
join m_office ounder on ounder.hierarchy like concat(o.hierarchy, '%') 
and ounder.hierarchy like concat(${userhierarchy}, '%')
inner join m_client c on c.office_id=ounder.id
inner join m_loan l on l.client_id=c.id and l.loan_type_enum=1
and l.loan_status_id in (300,600,601,700)
inner join (select max(id) trxid,tx.loan_id trxlid
from m_loan_transaction tx
where tx.transaction_date <=${ondate}
and tx.transaction_type_enum in (1,2,6,8)
and tx.is_reversed=0
group by tx.loan_id) trx on trx.trxlid=l.id
inner join m_loan_transaction tx on tx.id=trx.trxid
inner join r_enum_value ev on ev.enum_id=l.loan_status_id and ev.enum_name='loan_status_id'
left join m_code_value gn on gn.id=c.gender_cv_id
left join m_staff s on s.id=l.loan_officer_id
left join m_fund f on f.id=l.fund_id
left join m_code_value mcv on mcv.id=l.loanpurpose_cv_id
where o.id=${branch}

union

select l.loan_type_enum
,ounder.id
,ifnull(cn.id,0)
,gr.id
,l.account_no accountNo
,c.display_name name
,l.principal_disbursed_derived
,tx.outstanding_loan_balance_derived
,ev.enum_value
,case l.loan_type_enum
	when 1 then 'Individual'
	when 2 then 'Group'
	when 3 then 'JLG'
	end as loanType
,l.disbursedon_date disbDate
,ifnull(gn.code_value,'-') gender
,gr.display_name groupName
,ifnull(s.display_name,'NA') loanOfficer
,ifnull(f.name,'NA') fund
,ifnull(cn.display_name,'NA') centerName
,ounder.name branchName,IFNULL(mcv.CODE_DESCRIPTION,'NA') LOANPURPOSE
from m_office o 
join m_office ounder on ounder.hierarchy like concat(o.hierarchy, '%') 
and ounder.hierarchy like concat(${userhierarchy}, '%')
inner join m_client c on c.office_id=ounder.id
inner join m_group_client mgc on mgc.client_id=c.id
inner join m_group gr on gr.id=mgc.group_id
left join m_group cn on cn.id=gr.parent_id
inner join m_loan l on l.client_id=c.id and l.group_id=gr.id and l.loan_type_enum=3
and l.loan_status_id in (300,600,601,700)
inner join (select max(id) trxid,tx.loan_id trxlid
from m_loan_transaction tx
where tx.transaction_date <=${ondate}
and tx.transaction_type_enum in (1,2,6,8)
and tx.is_reversed=0
group by tx.loan_id) trx on trx.trxlid=l.id
inner join m_loan_transaction tx on tx.id=trx.trxid
inner join r_enum_value ev on ev.enum_id=l.loan_status_id and ev.enum_name='loan_status_id'
left join m_code_value gn on gn.id=c.gender_cv_id
left join m_staff s on s.id=l.loan_officer_id
left join m_fund f on f.id=l.fund_id
left join m_code_value mcv on l.loanpurpose_cv_id=mcv.id
where o.id=${branch}

union

select l.loan_type_enum
,ounder.id
,ifnull(cn.id,0)
,gr.id
,l.account_no accountNo
,gr.display_name name
,l.principal_disbursed_derived
,tx.outstanding_loan_balance_derived
,ev.enum_value
,case l.loan_type_enum
	when 1 then 'Individual'
	when 2 then 'Group'
	when 3 then 'JLG'
	end as loanType
,l.disbursedon_date disbDate
,'-' gender
,gr.display_name groupName
,ifnull(s.display_name,'NA') loanOfficer
,ifnull(f.name,'NA') fund
,ifnull(cn.display_name,'NA') centerName
,ounder.name branchName,IFNULL(mcv.CODE_DESCRIPTION,'NA') LOANPURPOSE
from m_office o 
join m_office ounder on ounder.hierarchy like concat(o.hierarchy, '%') 
and ounder.hierarchy like concat(${userhierarchy}, '%')
inner join m_group gr on gr.office_id=ounder.id
left join m_group cn on cn.id=gr.parent_id
inner join m_loan l on l.group_id=gr.id and l.loan_type_enum=2
and l.loan_status_id in (300,600,601,700)
inner join (select max(id) trxid,tx.loan_id trxlid
from m_loan_transaction tx
where tx.transaction_date <=${ondate}
and tx.transaction_type_enum in (1,2,6,8)
and tx.is_reversed=0
group by tx.loan_id) trx on trx.trxlid=l.id
inner join m_loan_transaction tx on tx.id=trx.trxid
inner join r_enum_value ev on ev.enum_id=l.loan_status_id and ev.enum_name='loan_status_id'
left join m_staff s on s.id=l.loan_officer_id
left join m_fund f on f.id=l.fund_id
left join m_code_value mcv on l.loanpurpose_cv_id=mcv.id

where o.id=${branch}
order by 1,2,3,4

-- query: Branch
select 
id, 
concat(substring("........................................", 1, 
((LENGTH(`hierarchy`) - LENGTH(REPLACE(`hierarchy`, '.', '')) - 1) * 4)),`name`) as tc 
from m_office 
where hierarchy like concat (${userhierarchy}, '%') 
order by hierarchy

