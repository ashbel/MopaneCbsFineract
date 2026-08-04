# SSB / Pensions deduction setup guide

This guide prepares a tenant for **SSB** (Salary Services Bureau) and **Pensions** deduction file export and PAY repayment import.

Flyway migration `V5005__ssb_pensions_deduction.sql` creates permissions, the client datatable, mandate tracking, and PAY import batch tables on each tenant schema at startup.

## What is set up automatically

| Item | Purpose |
|------|---------|
| Permission `READ_SSB_DEDUCTION` | Download export workbooks |
| Permission `CREATE_SSB_PAY_IMPORT` | Upload PAY files + list batches |
| Permission `UPDATE_SSB_PAY_IMPORT` | Approve / reject Needs Review rows |
| Datatable `ssb_client_details` on `m_client` | `IdNumber`, `EcNumber` |
| Table `m_ssb_mandate` | Tracks last NEW/CHANGE/DELETE export per loan+bureau |
| Tables `m_ssb_pay_import_batch` / `m_ssb_pay_import_row` | Import history + review queue |

Super user role is granted these permissions when the role exists.

## Staff data capture

For each borrower on an SSB or Pensions loan product:

1. Open the client → **datatable `ssb_client_details`**
2. Enter:
   - **IdNumber** — national ID (required for export)
   - **EcNumber** — employment / EC number (**required for SSB**; optional for Pensions)

Without these values the loan appears on the export workbook **Skipped** sheet.

## Loan products

Use dedicated loan products (or filter by product id) for:

- Government salary (SSB) loans
- Government pensions loans

Export always requires `loanProductId`. Default statuses included are **Approved (200)** and **Active (300)** so mandates can be registered before disbursement.

## Export behaviour (summary)

| Field | Source |
|-------|--------|
| Reference / REF NO | Loan `accountNo` |
| IdNumber / ID NUMBER | `ssb_client_details.IdNumber` |
| EcNumber | `ssb_client_details.EcNumber` |
| Amount | Next unpaid installment total × 100 (cents) |
| StartDate | `disbursedon_date`, else `expected_disbursedon_date` |
| EndDate / TO DATE | `expected_maturedon_date` |
| Type | `NEW` first export; `CHANGE` if amount or maturity changed; `DELETE` if mandate active but loan no longer approved/active |

Successful export rows update `m_ssb_mandate`.

## PAY import behaviour (summary)

| Match | Result |
|-------|--------|
| Reference = loan `accountNo` | Auto-post repayment; recon fields in transaction **note** |
| Reference missing/unknown + unique Id/EC candidate | **Needs Review** (no auto-post) |
| Ambiguous Id/EC | Failed `AMBIGUOUS_ID_EC` |
| No match | Failed `REFERENCE_MISSING` / `REFERENCE_NOT_FOUND` |

Idempotency uses loan transaction `externalId` = `{bureau}-{Rec id}`.

## APIs

| Method | Path |
|--------|------|
| GET | `/fineract-provider/api/v1/ssb/export` |
| POST | `/fineract-provider/api/v1/ssb/pay/upload` |
| GET | `/fineract-provider/api/v1/ssb/pay/batches` |
| GET | `/fineract-provider/api/v1/ssb/pay/batches/{batchId}` |
| POST | `/fineract-provider/api/v1/ssb/pay/batches/{batchId}/rows/{rowId}?command=approve\|reject` |

See [SSB_PENSIONS_UI_HANDOVER.md](SSB_PENSIONS_UI_HANDOVER.md) for UI integration details.

## Verify in SQL (optional)

```sql
SELECT code FROM m_permission WHERE code LIKE '%SSB%';

SELECT * FROM x_registered_table WHERE registered_table_name = 'ssb_client_details';

DESCRIBE ssb_client_details;
DESCRIBE m_ssb_mandate;
DESCRIBE m_ssb_pay_import_batch;
```
