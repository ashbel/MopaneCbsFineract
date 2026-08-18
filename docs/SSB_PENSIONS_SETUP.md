# SSB / Pensions deduction setup guide

This guide prepares a tenant for **SSB** (Salary Services Bureau) and **Pensions** deduction file export, PAY repayment import, and RES (bureau response) import.

Flyway migration `V5005__ssb_pensions_deduction.sql` creates permissions, the client datatable, mandate tracking, and PAY import batch tables on each tenant schema at startup.

Flyway migration `V5008__ssb_res_import.sql` adds RES import permissions, the `ssb-res-auto-disburse` configuration, and RES batch/row tables.

## What is set up automatically

| Item | Purpose |
|------|---------|
| Permission `READ_SSB_DEDUCTION` | Download export workbooks |
| Permission `CREATE_SSB_PAY_IMPORT` | Upload PAY files + list batches |
| Permission `UPDATE_SSB_PAY_IMPORT` | Approve / reject Needs Review rows |
| Permission `CREATE_SSB_RES_IMPORT` | Upload RES files + list batches |
| Permission `UPDATE_SSB_RES_IMPORT` | Approve / reject Needs Review RES rows |
| Config `ssb-res-auto-disburse` | When enabled (default), SUCCESS + Approved loans auto-disburse |
| Datatable `ssb_client_details` on `m_client` | `IdNumber`, `EcNumber` |
| Table `m_ssb_mandate` | Tracks last NEW/CHANGE/DELETE export per loan+bureau |
| Tables `m_ssb_pay_import_batch` / `m_ssb_pay_import_row` | PAY import history + review queue |
| Tables `m_ssb_res_import_batch` / `m_ssb_res_import_row` | RES import history + review queue |

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

## RES import behaviour (summary)

Upload the bureau response workbook (e.g. `RES26083B173.xlsx`) after submitting an export. Matching is the same as PAY: auto-act only when Reference = `accountNo`; otherwise a unique Id/EC candidate goes to **Needs Review**.

| Bureau Status | Result |
|---------------|--------|
| `SUCCESS` + loan **Approved (200)** + auto-disburse on | Disburse approved principal (cash, or to linked savings if present). RES `Amount` is the monthly deduction and is **not** used. |
| `SUCCESS` + already Active, CHANGE/DELETE, or auto-disburse off | Recorded as **AUTHORISED** (no second disbursement) |
| `FAILED` | Loan **note** with the bureau Message (loan status is not changed). Mandate is corrected so the next export retries (`NEW`/`CHANGE` failed → inactive; `DELETE` failed → active again) |
| Reference unknown + unique Id/EC | **Needs Review** |
| Ambiguous / no match | Failed |

Auto-disburse default is global config `ssb-res-auto-disburse` (enabled). The upload field `autoDisburse` overrides that default. Idempotency skips `rec_id` values already `DISBURSED` / `NOTED` / `AUTHORISED` on a non-dry-run batch.

The uploading user also needs standard loan permissions: `DISBURSE_LOAN` (or `DISBURSETOSAVINGS_LOAN`) and `CREATE_LOANNOTE`.

## APIs

| Method | Path |
|--------|------|
| GET | `/fineract-provider/api/v1/ssb/export` |
| POST | `/fineract-provider/api/v1/ssb/pay/upload` |
| GET | `/fineract-provider/api/v1/ssb/pay/batches` |
| GET | `/fineract-provider/api/v1/ssb/pay/batches/{batchId}` |
| POST | `/fineract-provider/api/v1/ssb/pay/batches/{batchId}/rows/{rowId}?command=approve\|reject` |
| POST | `/fineract-provider/api/v1/ssb/res/upload` |
| GET | `/fineract-provider/api/v1/ssb/res/batches` |
| GET | `/fineract-provider/api/v1/ssb/res/batches/{batchId}` |
| POST | `/fineract-provider/api/v1/ssb/res/batches/{batchId}/rows/{rowId}?command=approve\|reject` |

See [SSB_PENSIONS_UI_HANDOVER.md](SSB_PENSIONS_UI_HANDOVER.md) for UI integration details.

## Verify in SQL (optional)

```sql
SELECT code FROM m_permission WHERE code LIKE '%SSB%';

SELECT * FROM c_configuration WHERE name = 'ssb-res-auto-disburse';

SELECT * FROM x_registered_table WHERE registered_table_name = 'ssb_client_details';

DESCRIBE ssb_client_details;
DESCRIBE m_ssb_mandate;
DESCRIBE m_ssb_pay_import_batch;
DESCRIBE m_ssb_res_import_batch;
```
