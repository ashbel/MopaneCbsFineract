-- report: Trial Balance
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String), fromDate (java.sql.Date), toDate (java.sql.Date)

-- query: BranchName
select name from m_office
where id=${branch}

-- query: Branch
select -1 as id,'All' as name
union
select id,name from m_office

-- query: Trial Balance
select *
from
(select debits.glcode as 'glcode', debits.name as 'name',IF(debits.type = 1 or debits.type = 5, ifnull(debits.debitamount,0)-ifnull(credits.creditamount,0),null) as 'debit', IF(debits.type = 4 or debits.type = 3 or debits.type = 2, ifnull(credits.creditamount,0)-ifnull(debits.debitamount,0),null) as 'credit'
from
(select acc_gl_account.gl_code as 'glcode',name,sum(amount) as 'debitamount',acc_gl_account.classification_enum as 'type'
from acc_gl_journal_entry,acc_gl_account
where acc_gl_account.id = acc_gl_journal_entry.account_id
and acc_gl_journal_entry.type_enum=2
and acc_gl_journal_entry.entry_date between ${fromDate} and ${toDate}
and (acc_gl_journal_entry.office_id= ${branch} or ${branch}=1)
group by glcode
order by glcode) debits
LEFT OUTER JOIN
(select acc_gl_account.gl_code as 'glcode',name as 'name',sum(amount) as 'creditamount',acc_gl_account.classification_enum as 'type'
from acc_gl_journal_entry,acc_gl_account
where acc_gl_account.id = acc_gl_journal_entry.account_id
and acc_gl_journal_entry.type_enum=1
and acc_gl_journal_entry.entry_date between ${fromDate} and ${toDate}
and (acc_gl_journal_entry.office_id= ${branch} or ${branch}=1)
group by glcode
order by glcode) credits
on debits.glcode=credits.glcode
union
select credits.glcode as 'glcode', credits.name as 'name',IF(credits.type = 1 or credits.type = 5, ifnull(debits.debitamount,0)-ifnull(credits.creditamount,0),null) as 'debit', IF(credits.type = 4 or credits.type = 3 or credits.type = 2, ifnull(credits.creditamount,0)-ifnull(debits.debitamount,0),null) as 'credit'
from
(select acc_gl_account.gl_code as 'glcode',name,sum(amount) as 'debitamount',acc_gl_account.classification_enum as 'type'
from acc_gl_journal_entry,acc_gl_account
where acc_gl_account.id = acc_gl_journal_entry.account_id
and acc_gl_journal_entry.type_enum=2
and acc_gl_journal_entry.entry_date between ${fromDate} and ${toDate}
and (acc_gl_journal_entry.office_id= ${branch} or ${branch}=1)
group by glcode
order by glcode) debits
RIGHT OUTER JOIN
(select acc_gl_account.gl_code as 'glcode',name as 'name',sum(amount) as 'creditamount',acc_gl_account.classification_enum as 'type'
from acc_gl_journal_entry,acc_gl_account
where acc_gl_account.id = acc_gl_journal_entry.account_id
and acc_gl_journal_entry.type_enum=1
and acc_gl_journal_entry.entry_date between ${fromDate} and ${toDate}
and (acc_gl_journal_entry.office_id= ${branch} or ${branch}=1)
group by glcode
order by glcode) credits
on debits.glcode=credits.glcode) as fullouterjoinresult
order by glcode

