# RBZ FORM_MFI1 setup guide (for users)

This guide explains how to prepare a tenant so **Reports → RBZ FORM_MFI1** can fill regulatory sheets that depend on codes and datatables.

After deploy of migration `V5004`, codes and datatable **structures** are created automatically on each tenant. Staff still need to **enter data** (and optionally extend code values).

## What the report uses

| Source | Used for |
|--------|----------|
| Core loans / clients / offices | Portfolio, arrears, top borrowers, most of Schedules 3–7 & 9 |
| Client **Gender** + loan **LoanPurpose** | Schedules 4 & 5 |
| Datatable `rbz_loan_details` | Loan class, insider loans, litigation |
| Datatable `rbz_office_channels` | Access by district / urban–rural channels |
| Datatable `rbz_institution_profile` | Company profile (HQ) |
| Datatable `rbz_long_term_debt` | Schedule 2 long-term debt + liability maturity |

API: `GET /fineract-provider/api/v1/rbz/form-mfi1`  
UI: `#/reports/rbz/form-mfi1`  
Permission: `READ_RBZ_FORM_MFI1`

## 1. Codes (Admin → System → Manage Codes)

If migration `V5004` has run, these already exist. Verify under **Admin → System → Manage Codes**:

### Create / verify these codes (exact names)

| Code name | Values (exact spelling) |
|-----------|-------------------------|
| `RbzLoanClass` | `Consumer`, `Commercial`, `Other` |
| `RbzRelatedPartyType` | `Shareholder`, `Director`, `Related Party`, `Other` |
| `RbzLocationType` | `Urban`, `Rural` |
| `LoanPurpose` | Keep existing purposes; ensure RBZ list is present (see below) |
| `Gender` | `Female`, `Male` (used on clients) |

### Extend LoanPurpose

Add these values if missing (case-insensitive match is OK for the report; prefer Title Case):

- Manufacturing  
- Retail  
- Consumption  
- Services  
- Health  
- Education  
- Mining  
- Agriculture  
- Cross Border Traders  
- Vendors  
- Funeral Assistance  
- Other  

When capturing a loan, set **Purpose** to one of these so Schedules 4/5 classify correctly.

### Verify in SQL (optional)

```sql
SELECT id, code_name FROM m_code
WHERE code_name LIKE 'Rbz%' OR code_name IN ('LoanPurpose','Gender');

SELECT c.code_name, cv.code_value, cv.order_position
FROM m_code c
JOIN m_code_value cv ON cv.code_id = c.id
WHERE c.code_name IN ('RbzLoanClass','RbzRelatedPartyType','RbzLocationType','LoanPurpose','Gender')
ORDER BY c.code_name, cv.order_position, cv.id;
```

## 2. Datatables (Admin → System → Manage Data Tables)

Migration `V5004` creates and registers the tables below. **Do not recreate them with different column names** unless you also change the backend.

If you create datatables manually (new environment without migration):

1. Go to **Admin → System → Manage Data Tables → Create Data Table**
2. Use the **exact table names** and **application tables** below
3. For dropdown columns, set:
   - **Column name** = the field name in the “Field” column (e.g. `loan_class`)
   - **Type** = Dropdown  
   - **Code** = the code name (e.g. `RbzLoanClass`)  
4. Fineract stores the physical column as `{Code}_cd_{field}`  
   Example: field `loan_class` + code `RbzLoanClass` → `RbzLoanClass_cd_loan_class`

### `rbz_loan_details` → application table `m_loan` (multi-row)

| Field (column name in UI) | Type | Code / notes |
|---------------------------|------|----------------|
| `loan_class` | Dropdown | `RbzLoanClass` |
| `related_party_type` | Dropdown | `RbzRelatedPartyType` |
| `related_party_name` | String | Optional display name for insider |
| `is_insider_loan` | Boolean | Mark related-party / insider loans |
| `is_under_litigation` | Boolean | |
| `litigation_amount` | Decimal | |

### `rbz_office_channels` → application table `m_office` (one row per office)

| Field | Type | Code / notes |
|-------|------|----------------|
| `province` | String | |
| `district` | String | |
| `location_type` | Dropdown | `RbzLocationType` |
| `num_pos` | Number | |
| `num_atms` | Number | |
| `num_agencies` | Number | |
| `num_mobile_branches` | Number | |
| `num_agents` | Number | |
| `num_banking_kiosks` | Number | |

### `rbz_institution_profile` → application table `m_office` (one row — use HQ office)

| Field | Type |
|-------|------|
| `institution_name` | String |
| `licence_number` | String |
| `date_commenced` | Date |
| `physical_address` | String |
| `postal_address` | String |
| `contact_telephones` | String |
| `contact_person` | String |
| `num_employees_female` | Number |
| `num_employees_male` | Number |
| `num_loan_officers_female` | Number |
| `num_loan_officers_male` | Number |
| `external_auditors` | String |
| `bankers` | String |
| `lawyers` | String |

### `rbz_long_term_debt` → application table `m_office` (multi-row on HQ)

| Field | Type |
|-------|------|
| `source_of_finance` | String |
| `amount_borrowed` | Decimal |
| `outstanding_balance` | Decimal |
| `interest_rate` | Decimal |
| `maturity_date` | Date |

### Verify registration (optional)

```sql
SELECT registered_table_name, application_table_name
FROM x_registered_table
WHERE registered_table_name LIKE 'rbz_%';

SELECT table_name, column_name, column_type
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name LIKE 'rbz_%'
ORDER BY table_name, ordinal_position;
```

Expected dropdown columns:

- `rbz_loan_details.RbzLoanClass_cd_loan_class`
- `rbz_loan_details.RbzRelatedPartyType_cd_related_party_type`
- `rbz_office_channels.RbzLocationType_cd_location_type`

## 3. Enter data (day-to-day)

1. **Loans**  
   - Open the loan → data table **rbz_loan_details**  
   - Set **loan_class** (Consumer / Commercial / Other)  
   - For insiders: `is_insider_loan = Yes`, set related party type/name  
   - For litigation: flags + amount  

2. **Offices**  
   - Each branch/office → **rbz_office_channels** (province, district, Urban/Rural, channel counts)  

3. **Head office**  
   - **rbz_institution_profile** — one row on HQ  
   - **rbz_long_term_debt** — one row per debt facility on HQ  

4. **Clients**  
   - Set Gender (Female/Male)  
   - On loans, set Purpose from the RBZ list  

5. **Roles**  
   - Role needs `READ_RBZ_FORM_MFI1`  
   - Plus CREATE/READ/UPDATE on the `rbz_*` datatables for staff who capture data  

## 4. Run the report

1. Open the tenant UI (e.g. `https://pivots.mopane.co.zw` — tenant id is the subdomain)  
2. **Reports → RBZ FORM_MFI1**  
3. Choose start/end date (and office if needed) → **Download FORM_MFI1 Excel**  

Income / Statement of Financial Position lines that are not loan-driven stay at 0 until a future GL mapping is added.

## 5. Common mistakes

| Mistake | Result |
|---------|--------|
| Dropdown column named the same as the code (e.g. field `RbzLoanClass`) | Wrong DB column (`RbzLoanClass_cd_RbzLoanClass`); report ignores it |
| Recreating tables with different names | Report cannot find `rbz_*` tables |
| Leaving `loan_class` empty | Loan class falls back to **Other** |
| Filling profile/debt on a branch instead of HQ | HQ-scoped sheets look empty when office filter is head office |
| Missing `READ_RBZ_FORM_MFI1` | Download fails / button hidden |

## 6. Tenant notes (Mopane Contabo)

| UI host | Tenant identifier | Schema |
|---------|-------------------|--------|
| `pivots.mopane.co.zw` | `pivots` | `mifostenant-pivot` |
| `million.mopane.co.zw` | `million` | `mifostenant-milliondollar` |
| `fineract.mopane.co.zw` | `default` (unless `?tenantIdentifier=`) | `mifostenant-default` |
