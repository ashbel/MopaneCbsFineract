-- report: Income Statement
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String), fromDate (java.sql.Date), toDate (java.sql.Date)

-- query: Branch
select -1 as id,'All' as name
union
select id,name from m_office

-- query: BranchName
select name from m_office
where id=${branch}

