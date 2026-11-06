-- report: TxnRunningBalances(Pentaho)
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String), startDate (java.sql.Date), endDate (java.sql.Date)

-- query: Transaction Running Balances
select date(${startDate}) as 'Transaction Date', 'Opening Balance' as `Transaction Type`, ifnull(null,'-') as Office,
	ifnull(Null,'-') as 'Loan Officer', ifnull(null,'-') as `Loan Account No`, ifnull(null,'-') as `Loan Product`, ifnull(null,'-') as `Currency`, 
	ifnull(null,'-') as `Client Account No`, ifnull(null,'-') as Client, 
	ifnull(null,0) as Amount, ifnull(null,0) as Principal, ifnull(null,0) as Interest,
@totalOutstandingPrincipal := 		  
ifnull(round(sum(
	if (txn.transaction_type_enum = 1 /* disbursement */,
		ifnull(txn.amount,0.00), 
		ifnull(txn.principal_portion_derived,0.00) * -1)) 
			,2),0.00)  as 'Outstanding Principal',

@totalInterestIncome := 
ifnull(round(sum(
	if (txn.transaction_type_enum in (2,5,8) /* repayment, repayment at disbursal, recovery repayment */,
		ifnull(txn.interest_portion_derived,0.00), 
		0))
			,2),0.00) as 'Interest Income',

@totalWriteOff :=
ifnull(round(sum(
	if (txn.transaction_type_enum = 6 /* write-off */,
		ifnull(txn.principal_portion_derived,0.00), 
		0)) 
			,2),0.00) as 'Principal Write Off'
from m_office o
join m_office ounder on ounder.hierarchy like concat(o.hierarchy, '%')
                          and ounder.hierarchy like concat(${userhierarchy}, '%')
join m_client c on c.office_id = ounder.id
join m_loan l on l.client_id = c.id
join m_product_loan lp on lp.id = l.product_id
join m_loan_transaction txn on txn.loan_id = l.id
left join m_currency cur on cur.code = l.currency_code
where txn.is_reversed = false  
and txn.transaction_type_enum not in (10,11)
and o.id = ${Branch}
and txn.transaction_date < date(${startDate})

union all

select x.`Transaction Date`, x.`Transaction Type`, ifnull(x.Office,'-')as"Office", ifnull(x.`Loan Officer`,'-')as"Loan Officer", ifnull(x.`Loan Account No`,'-')as "Loan Account No", ifnull(x.`Loan Product`,'-')as"Loan Product" , ifnull(x.`Currency`,'-')as"Currency", 
	ifnull(x.`Client Account No`,'-')as "Client Account No", x.Client, ifnull(x.Amount,0)as "Amount", ifnull(x.Principal,0)as "Principal", ifnull(x.Interest,0)as"Interest",
cast(round( 
	if (x.transaction_type_enum = 1 /* disbursement */,
		@totalOutstandingPrincipal := @totalOutstandingPrincipal + x.`Amount`, 
		@totalOutstandingPrincipal := @totalOutstandingPrincipal - x.`Principal`) 
			,2) as decimal(19,2)) as 'Outstanding Principal',
cast(round(
	if (x.transaction_type_enum in (2,5,8) /* repayment, repayment at disbursal, recovery repayment */,
		@totalInterestIncome := @totalInterestIncome + x.`Interest`, 
		@totalInterestIncome) 
			,2) as decimal(19,2)) as 'Interest Income',
cast(round(
	if (x.transaction_type_enum = 6 /* write-off */,
		@totalWriteOff := @totalWriteOff + x.`Principal`, 
		@totalWriteOff) 
			,2) as decimal(19,2)) as 'Principal Write Off'
from
(select txn.transaction_type_enum, txn.id as txn_id, txn.transaction_date as 'Transaction Date', 
cast(
	ifnull(re.enum_message_property, concat('Unknown Transaction Type Value: ' , txn.transaction_type_enum)) 
	as char) as 'Transaction Type',
ounder.`name` as Office, lo.display_name as 'Loan Officer',
l.account_no  as 'Loan Account No', lp.`name` as 'Loan Product', 
ifnull(cur.display_symbol, l.currency_code) as Currency,
c.account_no as 'Client Account No', c.display_name as 'Client',
ifnull(txn.amount,0.00) as Amount,
ifnull(txn.principal_portion_derived,0.00) as Principal,
ifnull(txn.interest_portion_derived,0.00) as Interest
from m_office o
join m_office ounder on ounder.hierarchy like concat(o.hierarchy, '%')
                          and ounder.hierarchy like concat(${userhierarchy}, '%')
join m_client c on c.office_id = ounder.id
join m_loan l on l.client_id = c.id
left join m_staff lo on lo.id = l.loan_officer_id
join m_product_loan lp on lp.id = l.product_id
join m_loan_transaction txn on txn.loan_id = l.id
left join m_currency cur on cur.code = l.currency_code
left join r_enum_value re on re.enum_name = 'transaction_type_enum'
						and re.enum_id = txn.transaction_type_enum
where txn.is_reversed = false  
and txn.transaction_type_enum not in (10,11)
and o.id = ${Branch}
and (ifnull(l.loan_officer_id, -10) = ${Loan Officer} or "-1" = ${Loan Officer})
and txn.transaction_date >= date(${startDate})
and txn.transaction_date <= date(${endDate})
order by txn.transaction_date, txn.id) x

-- query: Branch
select id, 
concat(substring("........................................", 1, 
   

((LENGTH(`hierarchy`) - LENGTH(REPLACE(`hierarchy`, '.', '')) - 1) * 4)), 
   `name`) as tc
from m_office
where hierarchy like concat

(${userhierarchy}, '%')
order by hierarchy

-- query: Loan Officer
select lo.id, lo.display_name as `Name` 
from m_office o 
join m_office ounder on ounder.hierarchy like concat(o.hierarchy, '%')
join m_staff lo on lo.office_id = ounder.id
where lo.is_loan_officer = 1
and o.id = ${Branch}
union all
(select -10, 'None')
union 
select -1, 'ALL'
order by 2

