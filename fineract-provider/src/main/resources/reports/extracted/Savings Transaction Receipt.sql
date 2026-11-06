-- report: Savings Transaction Receipt
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String), transactionId (java.lang.Long)

-- query: SavingTxnReceiptQuery
select
     stxn.id txn_id,
	concat(c.display_name,' - ', c.id) client_name, c.id client_id,
	s.account_no sav_account_number, 
	prod.name sav_account_name,
	stxn.transaction_date txn_date,
	stxn.created_date txn_date_time, 
	concat('txn_type.',stxn.transaction_type_enum) txn_type,
	concat('accounting_txn_type.',stxn.transaction_type_enum) acc_txn_type,
	s.currency_code currency_code,
	stxn.amount txn_amount,
   stxn.transaction_date txn_eff_date,
   staff.display_name staff_name
 from m_savings_account_transaction stxn
 left join m_savings_account s on stxn.savings_account_id = s.id
 left join m_savings_product prod on s.product_id = prod.id
 left join m_client c on s.client_id = c.id
 left join m_appuser usr on stxn.appuser_id = usr.id
 left join m_staff staff on usr.staff_id = staff.id
 where stxn.id = ${transactionId};

