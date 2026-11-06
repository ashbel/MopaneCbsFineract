-- report: Balance Sheet
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String), date (java.sql.Date)

-- query: BranchName
select name from m_office
where id=${branch}

-- query: Branch
select -1 as id,'All' as name
union
select id,name from m_office

