-- report: Aging Summary (Arrears in Weeks)(Pentaho)
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String)

-- query: Branch
select id, 
concat(substring("........................................", 1, 
   

((LENGTH(`hierarchy`) - LENGTH(REPLACE(`hierarchy`, '.', '')) - 1) * 4)), 
   `name`) as tc
from m_office
where hierarchy like concat

(${userhierarchy}, '%')
order by hierarchy

-- query: Aging Summary (Arrears in Weeks)
SELECT 
  IFNULL(periods.currencyName, periods.currency) as currency, ars.Name as Name,
  periods.period_no 'Weeks In Arrears (Up To)', 
  IFNULL(ars.loanId, 0) 'No Of Loans', 
  IFNULL(ars.principal,0.0) 'Original Principal', 
  IFNULL(ars.interest,0.0) 'Original Interest', 
  IFNULL(ars.prinPaid,0.0) 'Principal Paid', 
  IFNULL(ars.intPaid,0.0) 'Interest Paid', 
  IFNULL(ars.prinOverdue,0.0) 'Principal Overdue', 
  IFNULL(ars.intOverdue,0.0)'Interest Overdue'
FROM 
	/* full table of aging periods/currencies used combo to ensure each line represented */
  (SELECT curs.code as currency, curs.name as currencyName, pers.* from
	(SELECT 'On Schedule' period_no,1 pid UNION
		SELECT '1',2 UNION
		SELECT '2',3 UNION
		SELECT '3',4 UNION
		SELECT '4',5 UNION
		SELECT '5',6 UNION
		SELECT '6',7 UNION
		SELECT '7',8 UNION
		SELECT '8',9 UNION
		SELECT '9',10 UNION
		SELECT '10',11 UNION
		SELECT '11',12 UNION
		SELECT '12',13 UNION
		SELECT '12+',14) pers,
	(SELECT distinctrow moc.code, moc.name
  	FROM m_office mo2
   	INNER JOIN m_office ounder2 ON ounder2.hierarchy 
				LIKE CONCAT(mo2.hierarchy, '%')
AND ounder2.hierarchy like CONCAT(${userhierarchy}, '%')
   	INNER JOIN m_client mc2 ON mc2.office_id=ounder2.id
   	INNER JOIN m_loan ml2 ON ml2.client_id = mc2.id
	INNER JOIN m_organisation_currency moc ON moc.code = ml2.currency_code
	WHERE ml2.loan_status_id=300 /* active */
	AND mo2.id=${Branch}
AND (ml2.currency_code = ${CurrencyId} or "-1" = ${CurrencyId})) curs) periods


LEFT JOIN /* table of aging periods per currency with gaps if no applicable loans */
(SELECT 
  	z.currency, z.arrPeriod, z.Name as "Name",
	COUNT(z.loanId) as loanId, SUM(z.principal) as principal, SUM(z.interest) as interest, 
	SUM(z.prinPaid) as prinPaid, SUM(z.intPaid) as intPaid, 
	SUM(z.prinOverdue) as prinOverdue, SUM(z.intOverdue) as intOverdue
FROM
	/*derived table just used to get arrPeriod value (was much slower to
	duplicate calc of minOverdueDate in inner query)
might not be now with derived fields but didn’t check */
	(SELECT x.loanId, x.currency, x.principal, x.interest, x.prinPaid, x.intPaid, x.prinOverdue, x.intOverdue,x.Name as "Name",
		IF(DATEDIFF(CURDATE(), minOverdueDate)<1, 'On Schedule', 
		IF(DATEDIFF(CURDATE(), minOverdueDate)<8, '1', 
		IF(DATEDIFF(CURDATE(), minOverdueDate)<15, '2', 
		IF(DATEDIFF(CURDATE(), minOverdueDate)<22, '3', 
		IF(DATEDIFF(CURDATE(), minOverdueDate)<29, '4', 
		IF(DATEDIFF(CURDATE(), minOverdueDate)<36, '5', 
		IF(DATEDIFF(CURDATE(), minOverdueDate)<43, '6', 
		IF(DATEDIFF(CURDATE(), minOverdueDate)<50, '7', 
		IF(DATEDIFF(CURDATE(), minOverdueDate)<57, '8', 
		IF(DATEDIFF(CURDATE(), minOverdueDate)<64, '9', 
		IF(DATEDIFF(CURDATE(), minOverdueDate)<71, '10', 
		IF(DATEDIFF(CURDATE(), minOverdueDate)<78, '11', 
		IF(DATEDIFF(CURDATE(), minOverdueDate)<85, '12',
				 '12+'))))))))))))) AS arrPeriod

	FROM /* get the individual loan details */
		(SELECT ml.id AS loanId, ml.currency_code as currency,ounder.name as "Name",
   			ml.principal_disbursed_derived as principal, 
			   ml.interest_charged_derived as interest, 
   			ml.principal_repaid_derived as prinPaid, 
			   ml.interest_repaid_derived intPaid,

			   laa.principal_overdue_derived as prinOverdue,
			   laa.interest_overdue_derived as intOverdue,

			   IFNULL(laa.overdue_since_date_derived, curdate()) as minOverdueDate
			  
  		FROM m_office mo
   		INNER JOIN m_office ounder ON ounder.hierarchy 
				LIKE CONCAT(mo.hierarchy, '%')
AND ounder.hierarchy like CONCAT(${userhierarchy}, '%')
   		INNER JOIN m_client mc ON mc.office_id=ounder.id
   		INNER JOIN m_loan ml ON ml.client_id = mc.id
		   LEFT JOIN m_loan_arrears_aging laa on laa.loan_id = ml.id
		WHERE ml.loan_status_id=300 /* active */
     		AND mo.id=${Branch}
     AND (ml.currency_code = ${CurrencyId} or "-1" = ${CurrencyId})
  		GROUP BY ml.id) x
	) z 
GROUP BY z.currency, z.arrPeriod ) ars ON ars.arrPeriod=periods.period_no and ars.currency = periods.currency
ORDER BY periods.currency, periods.pid

-- query: CurrencyId
select `code`, `name`
from m_organisation_currency
union 
select -1,'All'
order by `code`

