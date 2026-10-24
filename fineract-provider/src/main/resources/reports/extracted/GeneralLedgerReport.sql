-- report: GeneralLedgerReport
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), fromDate (java.sql.Date), toDate (java.sql.Date), userhierarchy (java.lang.String)

-- query: OB
select
je.office_running_balance as openingbalance
from acc_gl_journal_entry je
where je.account_id = ${account}
and je.office_id = ${branch}
and je.reversed = 0
and je.entry_date <= DATE_SUB(${entry_date}, INTERVAL 1 DAY)
and id = 
(select max(id) from acc_gl_journal_entry je
where je.account_id = ${account}
and je.office_id = ${branch}
and je.reversed = 0
and je.entry_date <= DATE_SUB(${entry_date}, INTERVAL 1 DAY)
)

-- query: branch
select id,name
from m_office

-- query: Query 1
select awe.aname as accountname,awe.aid as accountId,awe.cdate edate,ifnull(awe.ccredit,0) credit,ifnull(awe.ddebit,0) dedit, awe.adiscription as 'discription',concat(awe.aref ," " ,awe.aoffice_id) vc
from(
select aga.name aname
, agje.account_id aid
,agje.transaction_id 
,agje.entry_date cdate

,agje.amount as ccredit
,'' ddebit
,agje.type_enum
,mo.external_id aoffice_id
,agje.description adiscription
,agje.ref_num aref

from acc_gl_journal_entry agje 
left join acc_gl_account aga on aga.id=agje.account_id
inner join m_office mo on mo.id=agje.office_id


where agje.reversed=0
and agje.entry_date BETWEEN ${fromDate} and ${endDate} 
and agje.transaction_id in (select agje2.transaction_id
from acc_gl_journal_entry agje2 

where agje2.account_id=15
and agje2.entry_date BETWEEN ${fromDate} and ${endDate} 
and agje2.manual_entry=1)
and agje.account_id!=15
and agje.type_enum=2
and agje.manual_entry=1
and mo.id=2





union 

select aga.name aname
, agje.account_id aid
,agje.transaction_id 
,agje.entry_date cdate

,'' as ccredit
,agje.amount as ddebit

,agje.type_enum
,mo.external_id aoffice_id
,agje.description adiscription
,agje.ref_num aref

from acc_gl_journal_entry agje 
left join acc_gl_account aga on aga.id=agje.account_id
inner join m_office mo on mo.id=agje.office_id


where agje.reversed=0
and agje.entry_date BETWEEN ${fromDate} and ${endDate} 
and agje.transaction_id in (select agje2.transaction_id
from acc_gl_journal_entry agje2 

where agje2.account_id=15
and agje2.entry_date BETWEEN ${fromDate} and ${endDate} 
and agje2.manual_entry=1)
and agje.account_id!=15
and agje.type_enum=1
and agje.manual_entry=1
and mo.id=2



order by  cdate, aid

-- query: account
select id aid,name aname
from acc_gl_account

-- query: Detail
select
details.edate entry_date,
details.acid1,
details.report_header,
details.reportid,
details.account_name,
branch.branchname,
sum(details.debit_amount) debit_amount,
sum(details.credit_amount) credit_amount,
details.aftertxn,
details.description,
ifnull(opb.openingbalance,0) openingbalance,
details.transactionid,
details.actype,
details.manual_entry,
if (details.manual_entry=1,details.id,'0system') transtype,
if (actype in (1,5),
   (sum(details.debit_amount) - sum(details.credit_amount)),
   (sum(details.credit_amount) - sum(details.debit_amount))) as cumulative_sum 
from
(

select
a.account_id acid1
,concat(gl.gl_code,"-",gl.name) as report_header
,gl.classification_enum actype
,gl.gl_code as reportid
,j1.entry_date edate
,concat(gl1.gl_code,"-",gl1.name) as account_name
,if (j1.type_enum=1, j1.amount, 0) as debit_amount
,if (j1.type_enum=2, j1.amount , 0) as credit_amount
,j1.id
,j1.office_id
,j1.transaction_id
, j1.type_enum
,j1.office_running_balance as aftertxn
,j1.description as description
,j1.transaction_id as transactionid
,a.manual_entry

from   acc_gl_journal_entry j1

inner join (select distinct je.transaction_id tid,je.account_id,je.manual_entry 
from m_office o
left join m_office ounder on ounder.hierarchy like concat(o.hierarchy,'%')
and ounder.hierarchy like concat(${userhierarchy},'%')
inner join  acc_gl_journal_entry je on je.office_id = ounder.id
where je.account_id =${account}
and o.id  = ${branch}
and je.entry_date between ${fromDate} and ${toDate})a on a.tid = j1.transaction_id and j1.account_id <> ${account}
left join acc_gl_account gl on gl.id = a.account_id
left join acc_gl_account gl1 on gl1.id = j1.account_id
order by j1.entry_date, j1.id) details
left join
(    
select je.account_id acid2,
if(aga1.classification_enum in (1,5),
(sum(if(je.type_enum=2,ifnull(je.amount,0),0))- sum(if(je.type_enum=1,ifnull(je.amount,0),0))),
(sum(if(je.type_enum=1,ifnull(je.amount,0),0))- sum(if(je.type_enum=2,ifnull(je.amount,0),0)))) openingbalance
from m_office o
left join m_office ounder on ounder.hierarchy like concat(o.hierarchy,'%')
and ounder.hierarchy like concat(${userhierarchy},'%')
left join acc_gl_journal_entry je on je.office_id = ounder.id
left join acc_gl_account aga1 on aga1.id=je.account_id
where je.entry_date <= DATE_SUB(${fromDate},INTERVAL 1 day)
and je.office_running_balance is not null
and (o.id=${branch})
and je.account_id = ${account}
group by je.account_id )opb
on opb.acid2=details.acid1
left join
(
select name branchname
from m_office mo
where
mo.id=1
)branch
on details.office_id=${branch}
group by
details.edate,
details.acid1,
details.report_header,
details.reportid,
details.account_name,
branch.branchname
,transtype

