# SSB / Pensions UI Handover (`mopane-cbs-ui`)

Frontend handover for Salary Services Bureau (SSB) and Pensions deduction **export** and PAY **import / review**.

**Backend repo:** `MopaneCbsFineract`  
**UI repo:** `mopane-cbs-ui` (Community App / AngularJS) or React UI  
**API status:** Backend-only in this change — no UI yet  
**Package:** `org.apache.fineract.mopane.ssb`

---

## Goal

Add a Reports-style screen so MFIs can:

1. Filter loans by product (and optional office / status) and download the bureau submission Excel  
2. Upload the bureau PAY return Excel and download a result workbook (`Posted` / `Failed` / `NeedsReview`)  
3. Open an import batch and approve or reject **Needs Review** rows (IdNumber/EcNumber suggested matches)

---

## Auth

| Item | Value |
|------|--------|
| Auth | Basic (same as rest of Community App) |
| Tenant | `Fineract-Platform-TenantId` header **or** `tenantIdentifier` query param |
| Export permission | `READ_SSB_DEDUCTION` |
| Upload / list batches | `CREATE_SSB_PAY_IMPORT` |
| Approve / reject review | `UPDATE_SSB_PAY_IMPORT` |

---

## 1. Export deduction file

| Item | Value |
|------|--------|
| Method | `GET` |
| Path | `/fineract-provider/api/v1/ssb/export` |
| Response | `.xlsx` attachment |

### Query parameters

| Param | Required | Default | Notes |
|-------|----------|---------|--------|
| `bureau` | Yes | — | `SSB` or `PENSION` |
| `loanProductId` | Yes | — | Primary filter |
| `officeId` | No | User’s office | Must be within user’s office hierarchy |
| `loanStatusId` | No | Approved **200** + Active **300** | Pass a single status to narrow |
| `includeUnchanged` | No | `false` | If `true`, include loans with no mandate delta |
| `asOfDate` | No | — | Reserved; installment uses next unpaid schedule period |

### Example

```http
GET /fineract-provider/api/v1/ssb/export?bureau=SSB&loanProductId=12&officeId=1
Authorization: Basic <base64(user:password)>
Fineract-Platform-TenantId: intercrest
Accept: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
```

```bash
curl -sk -u 'USER:PASSWORD' \
  -H 'Fineract-Platform-TenantId: intercrest' \
  -o SSB_DEDUCTION.xlsx \
  'https://fineract.mopane.co.zw/fineract-provider/api/v1/ssb/export?bureau=SSB&loanProductId=12'
```

### Workbook sheets

| Sheet | Content |
|-------|---------|
| `Sheet1` | Bureau rows (SSB or Pensions columns — see setup doc) |
| `Skipped` | Loans excluded with reason (`MISSING_ID_NUMBER`, `MISSING_EC_NUMBER`, `MISSING_DATES`, `MISSING_INSTALLMENT`, `UNCHANGED`) |

### UI sketch

- Route suggestion: `#/reports/ssb/export`  
- Controls: Bureau (SSB / Pensions), Loan product (required), Office, optional Status, Include unchanged checkbox  
- Primary action: **Download**  
- Show short help: IdNumber/EcNumber come from client datatable `ssb_client_details`; Reference is loan account number

---

## 2. Upload PAY file

| Item | Value |
|------|--------|
| Method | `POST` |
| Path | `/fineract-provider/api/v1/ssb/pay/upload` |
| Content-Type | `multipart/form-data` |
| Response | Result `.xlsx` attachment |
| Response header | `X-SSB-Import-Batch-Id: {batchId}` |

### Form fields

| Field | Required | Notes |
|-------|----------|--------|
| `file` | Yes | PAY `.xlsx` |
| `bureau` | Yes | `SSB` or `PENSION` |
| `paymentTypeId` | No | Defaults to first `m_payment_type` |
| `dryRun` | No | `true` = classify only, do not post repayments |

### Example

```bash
curl -sk -u 'USER:PASSWORD' \
  -H 'Fineract-Platform-TenantId: intercrest' \
  -F 'file=@PAY260384E2.xlsx' \
  -F 'bureau=SSB' \
  -F 'paymentTypeId=1' \
  -D - \
  -o PAY_RESULT.xlsx \
  'https://fineract.mopane.co.zw/fineract-provider/api/v1/ssb/pay/upload'
```

### Result workbook sheets

| Sheet | Meaning |
|-------|---------|
| `Posted` | Reference matched `accountNo`; repayment posted (or dry-run success) |
| `Failed` | Could not auto-post / ambiguous / validation error |
| `NeedsReview` | Reference failed, but IdNumber (+ EcNumber if present) found exactly one candidate loan |

### Matching rules (do not re-implement in UI)

1. Auto-post **only** when Reference matches loan `accountNo`  
2. Otherwise fail, and if Id/EC suggests one loan → **Needs Review**  
3. Never auto-post from Id/EC alone — that is the review screen’s job  

### UI sketch

- Route suggestion: `#/reports/ssb/pay-import`  
- Controls: Bureau, Payment type, Dry run checkbox, file picker  
- Action: **Upload** → browser downloads result Excel  
- Capture `X-SSB-Import-Batch-Id` and link to batch detail / review

---

## 3. Review batches

### List batches

```http
GET /fineract-provider/api/v1/ssb/pay/batches?offset=0&limit=50
```

JSON array of:

| Field | Type |
|-------|------|
| `id` | number |
| `bureau` | string |
| `filename` | string |
| `uploadedBy` | number |
| `uploadedOn` | date/datetime |
| `dryRun` | boolean |
| `paymentTypeId` | number \| null |
| `postedCount` | number |
| `failedCount` | number |
| `needsReviewCount` | number |
| `totalCount` | number |

### Batch detail

```http
GET /fineract-provider/api/v1/ssb/pay/batches/{batchId}
```

Same batch object plus `rows[]` with:

| Field | Type | Notes |
|-------|------|--------|
| `id` | number | Row id for approve/reject |
| `rowNumber` | number | Excel row |
| `recId`, `deductionCode`, `reference`, `idNumber`, `ecNumber`, `name` | string | PAY columns |
| `transDate` | date | |
| `amount` | number | Currency units (not cents) |
| `status` | string | `POSTED` \| `FAILED` \| `NEEDS_REVIEW` \| `REJECTED` |
| `reason` | string | e.g. `LIKELY_MATCH_ID_EC`, `REFERENCE_MISSING` |
| `suggestedLoanId` / `suggestedAccountNo` | | Present for review |
| `postedLoanId` / `postedTransactionId` | | Present when posted |
| `note` | string | Recon text written on repayment |

### Approve / reject

```http
POST /fineract-provider/api/v1/ssb/pay/batches/{batchId}/rows/{rowId}?command=approve
POST /fineract-provider/api/v1/ssb/pay/batches/{batchId}/rows/{rowId}?command=reject
```

Optional: `paymentTypeId` on approve.

Approve posts a repayment to `suggestedLoanId` with the same note/externalId rules as auto-post.  
Reject sets status `REJECTED` with reason `REJECTED_BY_USER`.

### UI sketch

- Route: `#/reports/ssb/pay-batches` and `#/reports/ssb/pay-batches/:batchId`  
- List: filename, bureau, counts, uploaded on  
- Detail: filterable table; highlight `NEEDS_REVIEW`  
- Row actions: Approve (confirm suggested account) / Reject  
- Link suggested account to existing loan view `#/viewloanaccount/{loanId}`

---

## Suggested menu placement

**Reports → SSB / Pensions**

- Export deduction file  
- Import PAY file  
- PAY import batches  

---

## Out of scope for UI v1 (backend already supports)

- Editing IdNumber/EcNumber (use existing datatable UI on client)  
- Changing mandate history manually  
- Auto-posting from Id/EC without approve  

## Prerequisites (tenant)

See [SSB_PENSIONS_SETUP.md](SSB_PENSIONS_SETUP.md): permissions, `ssb_client_details` data entry, dedicated loan products.
