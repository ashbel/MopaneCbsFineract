-- report: Client Savings Summary
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String), fromDate (java.sql.Date), toDate (java.sql.Date)

-- query: SelectOffice
select id, 
concat(substring("         ", 1, 
((LENGTH(`hierarchy`) - LENGTH(REPLACE(`hierarchy`, '.', '')) - 1) * 4)), 
   `name`) as name
from m_office
order by hierarchy

-- query: selectProduct
select p.id, p.`name`
from m_savings_product p     
union select -1,'All'

-- query: DetailsV2
SELECT sa.account_no AS 'Acc No', IF(sa.client_id is NULL,(
SELECT mg.display_name
FROM m_group mg
WHERE mg.id=sa.group_id),(
SELECT mc.display_name
FROM m_client mc
WHERE mc.id=sa.client_id)) AS 'client/Group Name',IFNULL((
SELECT sat1.running_balance_derived
FROM m_savings_account_transaction sat1
WHERE sat1.savings_account_id=sa.id AND sat1.transaction_date < ${fromDate}
ORDER BY sat1.transaction_date DESC,sat1.created_date DESC,sat1.id DESC
LIMIT 1),0) AS 'Balance B/F', SUM(IF(ev.enum_type = 0,sat.amount,0)) AS Deposit, SUM(IF(ev.enum_type = 1,sat.amount,0)) AS Withdrawal,(
SELECT sat1.running_balance_derived
FROM m_savings_account_transaction sat1
WHERE sat1.savings_account_id=sa.id AND sat1.transaction_date <= ${toDate}
ORDER BY sat1.transaction_date DESC,sat1.created_date DESC,sat1.id DESC
LIMIT 1) AS 'Balance',
msp.name AS 'Product'
FROM m_office mo
 JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%') AND ounder.hierarchy like CONCAT(${userhierarchy}, '%')
 inner join (select sav.id said,sav.client_id cid,sav.group_id gid,of.id oid from m_savings_account sav
left join m_client mc on mc.id=sav.client_id
left join m_group gr on gr.id=sav.group_id
inner join m_office of on of.id=if(mc.id is null,gr.office_id,mc.office_id)) ofid on ofid.oid=ounder.id
INNER JOIN m_savings_account sa on sa.id=ofid.said
INNER JOIN m_savings_account_transaction sat ON sat.savings_account_id = sa.id
INNER JOIN r_enum_value ev ON ev.enum_name = 'savings_transaction_type_enum' AND ev.enum_id = sat.transaction_type_enum
INNER JOIN m_savings_product msp on msp.id=sa.product_id
WHERE #sa.client_id=27 AND 
mo.id=${selectOffice}
AND (msp.id=${selectProduct} or ${selectProduct}=-1)
AND sat.is_reversed=0 AND sat.transaction_date BETWEEN ${fromDate} AND ${toDate}
GROUP BY sa.account_no
ORDER BY sa.product_id,sa.account_no

-- query: MFIName
select name from m_office
where hierarchy='.'

-- query: Details
SELECT sa.account_no AS 'Acc No', IF(sa.client_id = NULL,(
SELECT mg.display_name
FROM m_group mg
WHERE mg.id=sa.group_id),(
SELECT mc.display_name
FROM m_client mc
WHERE mc.id=sa.client_id)) AS 'client/Group Name',(
SELECT sat1.running_balance_derived
FROM m_savings_account_transaction sat1
WHERE sat1.savings_account_id=sa.id AND sat1.transaction_date < ${fromDate}
ORDER BY sat1.transaction_date DESC,sat1.created_date DESC,sat1.id DESC
LIMIT 1) AS 'Balance B/F', SUM(IF(ev.enum_type = 0,sat.amount,0)) AS Deposit, SUM(IF(ev.enum_type = 1,sat.amount,0)) AS Withdrawal,(
SELECT sat1.running_balance_derived
FROM m_savings_account_transaction sat1
WHERE sat1.savings_account_id=sa.id AND sat1.transaction_date <= ${toDate}
ORDER BY sat1.transaction_date DESC,sat1.created_date DESC,sat1.id DESC
LIMIT 1) AS 'Balance',
msp.name AS 'Product'
FROM m_office mo
 JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%') AND ounder.hierarchy like CONCAT(${userhierarchy}, '%')
 inner join (select sav.id said,sav.client_id cid,sav.group_id gid,of.id oid from m_savings_account sav
left join m_client mc on mc.id=sav.client_id
left join m_group gr on gr.id=sav.group_id
inner join m_office of on of.id=if(mc.id is null,gr.office_id,mc.office_id)) ofid on ofid.oid=ounder.id
INNER JOIN m_savings_account sa on sa.id=ofid.said
INNER JOIN m_savings_account_transaction sat ON sat.savings_account_id = sa.id
INNER JOIN r_enum_value ev ON ev.enum_name = 'savings_transaction_type_enum' AND ev.enum_id = sat.transaction_type_enum
INNER JOIN m_savings_product msp on msp.id=sa.product_id
WHERE #sa.client_id=27 AND 
mo.id=${selectOffice}
AND (msp.id=${selectProduct} or ${selectProduct}=-1)
AND sat.is_reversed=0 AND sat.transaction_date BETWEEN ${fromDate} AND ${toDate}
GROUP BY sa.account_no
ORDER BY sa.product_id

-- query: backup
select  sa.account_no as 'Acc No',
        if(sa.client_id is null,
		(select mg.display_name from  m_group mg where mg.id=sa.group_id),
		(select mc.display_name  from  m_client mc where mc.id=sa.client_id)) as 'client/Group Name' ,
		IFNULL((select sat1.running_balance_derived from m_savings_account_transaction sat1 where sat1.savings_account_id=sa.id and sat1.transaction_date < ${fromDate} /*From date*/and sat1.is_reversed=0 order by sat1.transaction_date DESC,sat1.created_date DESC,sat1.id DESC limit 1),0)  as 'Balance B/F', 
		sum(if(ev.enum_type = 0,sat.amount,0)) as Deposit,sum(if(ev.enum_type = 1,sat.amount,0)) as Withdrawal,
		(select sat1.running_balance_derived from m_savings_account_transaction sat1 where sat1.savings_account_id=sa.id and sat1.transaction_date <= ${toDate} /*To Date*/ and sat1.is_reversed=0 order by sat1.transaction_date DESC,sat1.created_date DESC,sat1.id DESC limit 1)  as 'Balance' ,
		msp.name as 'Product'
		
	from m_office mo
	inner JOIN m_office ounder ON ounder.hierarchy LIKE CONCAT(mo.hierarchy, '%') AND ounder.hierarchy like CONCAT('.', '%')
	left join m_client mc on mc.office_id=ounder.id
	left join m_group gr on gr.office_id=ounder.id
	inner join m_savings_account sa on (sa.client_id=mc.id or sa.group_id=gr.id)
	inner join m_savings_account_transaction sat on sat.savings_account_id = sa.id 
	inner join r_enum_value ev on ev.enum_name = 'savings_transaction_type_enum' and ev.enum_id = sat.transaction_type_enum 
	inner join m_savings_product msp on msp.id=sa.product_id
	where mo.id=${selectOffice}
	and (msp.id=${selectProduct} or ${selectProduct}=-1)
	and sat.is_reversed=0 
	and sat.transaction_date 
	BETWEEN ${fromDate} AND ${toDate}
	group by sa.account_no
	order by sa.product_id,sa.account_no

