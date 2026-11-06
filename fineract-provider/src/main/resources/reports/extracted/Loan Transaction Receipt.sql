-- report: Loan Transaction Receipt
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String), transactionId (java.lang.Long)

-- query: Query 1
select
    ltxn.id txn_id,
	concat(c.display_name,' - ', c.id) client_name, c.id client_id,
	l.account_no loan_account_number, 
	prod.name loan_account_name,
	ltxn.transaction_date txn_date,
	ltxn.created_date txn_date_time, 
	concat('txn_type.',ltxn.transaction_type_enum) txn_type,
	concat('accounting_txn_type.',ltxn.transaction_type_enum) acc_txn_type,
	l.currency_code currency_code,
	ltxn.amount txn_amount,
   ltxn.transaction_date txn_eff_date,
   staff.display_name staff_name
 from m_loan_transaction ltxn
 left join m_loan l on ltxn.loan_id = l.id
 left join m_product_loan prod on l.product_id = prod.id
 left join m_client c on l.client_id = c.id
 left join m_appuser usr on ltxn.appuser_id = usr.id
 left join m_staff staff on usr.staff_id = staff.id
 where ltxn.id =${transactionId};

