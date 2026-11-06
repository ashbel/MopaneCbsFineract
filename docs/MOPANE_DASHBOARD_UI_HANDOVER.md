# Mopane Dashboard UI Handover (`mopane-cbs-ui`)

Frontend handover for replacing the classic Community App home widgets with a single LMS dashboard API.

**Backend repo:** `MopaneCbsFineract` (branch `mopane-features-upgrade`)  
**UI repo:** `mopane-cbs-ui` (Community App / AngularJS)  
**API status:** Deployed on production VPS (`https://fineract.mopane.co.zw`)  
**Commit:** `9c54f2f` — aggregated loan-metrics endpoint

---

## Goal

Build (or upgrade) the dashboard in `mopane-cbs-ui` so it loads **one** JSON call instead of many `GET /runreports/{name}` requests.

Keep the familiar layout from the classic home:

1. **Portfolio Performance** KPI row  
2. **Portfolio Pipeline** KPI row  
3. **Client / loan trends** chart  
4. **Collections today** (expected vs actual)  
5. **Arrears / aging** chart  
6. **Recent activity** list  

Add LMS-focused metrics that were not prominent before: NPL, interest collected MTD, write-offs MTD, collection rate MTD, interest outstanding.

---

## Endpoint

| Item | Value |
|------|--------|
| Method | `GET` |
| Path | `/fineract-provider/api/v1/mopane/dashboard/loan-metrics` |
| Auth | Basic auth (same as rest of Community App) |
| Tenant | `Fineract-Platform-TenantId` header **or** `tenantIdentifier` query param |
| Permission | `READ_MOPANE_DASHBOARD` |

### Query parameters

| Param | Required | Default | Notes |
|-------|----------|---------|--------|
| `officeId` | No | Authenticated user’s office | Must be within the user’s office hierarchy |
| `currencyCode` | No | Resolved (see below) | Filters all loan/money metrics to one currency (e.g. `USD`, `ZWL`) |
| `trendPeriod` | No | `day` | `day` \| `week` \| `month` — controls trend bucket labels and series |
| `activityLimit` | No | `20` | Max `50` recent audit rows |

**Default `currencyCode` (when omitted):** currencies that have loans in the selected office hierarchy; prefer `USD` if present, otherwise the first code alphabetically. If the office has no loans, `currencyCode` is `null` and loan/money metrics are zero.

**Invalid `currencyCode`:** HTTP `404` (`CurrencyNotFoundException`) when the code is not among currencies with loans in that office.

### Example request

```http
GET /fineract-provider/api/v1/mopane/dashboard/loan-metrics?officeId=1&currencyCode=USD&trendPeriod=day&activityLimit=20
Authorization: Basic <base64(user:password)>
Fineract-Platform-TenantId: million
Accept: application/json
```

Tenant subdomain mapping (already in `mopane-cbs-ui`):  
`million.mopane.co.zw` → tenant `million`, `intercrest.mopane.co.zw` → `intercrest`, etc.

### Example curl (prod)

```bash
curl -sk -u 'USER:PASSWORD' \
  -H 'Fineract-Platform-TenantId: intercrest' \
  'https://fineract.mopane.co.zw/fineract-provider/api/v1/mopane/dashboard/loan-metrics?currencyCode=USD&trendPeriod=day'
```

---

## Response contract

Top-level object:

| Field | Type | Description |
|-------|------|-------------|
| `officeId` | number | Resolved office |
| `officeName` | string | Office display name |
| `asOfDate` | string (date) | Tenant “today” (serialized by Fineract Gson — often `"MMM d, yyyy h:mm:ss a"`) |
| `currencyCode` | string \| null | Currency used for all loan/money aggregates (echo of filter / default); `null` if office has no loans |
| `availableCurrencies` | string[] | Currencies that have loans in this office hierarchy; sorted with `USD` first then A–Z — use for the currency switcher |
| `portfolio` | object | Portfolio Performance + LMS extras |
| `pipeline` | object | Pipeline / ops KPIs |
| `aging` | array | Always 5 buckets in fixed order |
| `trends` | object | Chart series |
| `recentActivity` | array | Newest first |

**Currency scope:** all monetary fields and loan counts in `portfolio` / `pipeline` / `aging` / `trends.loansDisbursed` are filtered by `currencyCode`. Office-scoped only (not currency-filtered): `portfolio.activeClients`, `portfolio.activeGroups`, `trends.newClients`, `recentActivity`.

### `portfolio`

| Field | Type | UI mapping / notes |
|-------|------|--------------------|
| `activeClients` | number | Clients card |
| `activeGroups` | number | Groups card |
| `activeLoans` | number | Recommended extra card or subtitle |
| `grossLoanBook` | number | Gross Loan Book — principal outstanding (active) |
| `portfolioAtRiskPercent` | number | PAR % (principal overdue / principal outstanding × 100) |
| `valueAtRisk` | number | Value at Risk — principal overdue total |
| `loansInArrears` | number | In Arrears count |
| `principalOverdue` | number | Detail / tooltip |
| `interestOutstanding` | number | LMS addition |
| `interestCollectedMtd` | number | LMS addition — interest portion of repayments MTD |
| `nplCount` | number | LMS addition — `is_npa = 1` |
| `nplOutstanding` | number | LMS addition |
| `writeOffsMtd` | number | LMS addition |
| `collectionRateMtdPercent` | number | LMS addition — actual / expected schedule dues MTD × 100 |

### `pipeline`

| Field | Type | UI mapping |
|-------|------|------------|
| `pendingApprovalCount` | number | Optional badge on Pending Approval |
| `pendingApprovalAmount` | number | Pending Approval $ |
| `pendingDisbursementCount` | number | Optional badge |
| `pendingDisbursementAmount` | number | Pending Disbursement $ |
| `disbursementsTodayAmount` | number | Disbursements (Today) |
| `disbursementsMonthAmount` | number | Disbursements (Month) |
| `collectionsExpectedToday` | number | Collections Expected (Today) |
| `collectionsActualToday` | number | Collections Actual (Today) / “Amount Collected for Today” |
| `collectionsExpectedMonth` | number | Month Collection Expected |
| `collectionsActualMonth` | number | Month Collection Actual |

### `aging[]` (fixed order)

| `bucket` | Meaning |
|----------|---------|
| `CURRENT` | Not in arrears (0 days) |
| `1_30` | 1–30 days |
| `31_60` | 31–60 days |
| `61_90` | 61–90 days |
| `90_PLUS` | 90+ days |

Each item: `{ bucket, loanCount, outstanding }`.

- `outstanding` here is **principal overdue** in that aging band (same basis as `portfolio.valueAtRisk`), not principal outstanding.
- Non-`CURRENT` bucket amounts sum to `portfolio.valueAtRisk`.
- `CURRENT` is typically `0` for the money chart (no overdue principal).

Use for the **Arrears Chart** (bar/stacked). Prefer `outstanding` for money chart; `loanCount` for count chart.

### `trends`

| Field | Type | Notes |
|-------|------|--------|
| `period` | string | Echo of `trendPeriod` |
| `newClients` | `{ bucket, count }[]` | Always **12** points |
| `loansDisbursed` | `{ bucket, count, amount }[]` | Always **12** points |

Bucket label format by period:

| `trendPeriod` | `bucket` format | Example |
|---------------|-----------------|---------|
| `day` | `YYYY-MM-DD` | `2026-08-03` |
| `week` | `YYYYWW` (ISO year-week) | `202631` |
| `month` | `YYYY-MM` | `2026-08` |

Wire the Week / Month / Day toggles on the classic “Client Trends” chart to refetch with `trendPeriod`.

### `recentActivity[]`

| Field | Type | Notes |
|-------|------|--------|
| `id` | number | Audit id |
| `actionName` | string | e.g. `REPAYMENT`, `CREATE` |
| `entityName` | string | e.g. `LOAN`, `CLIENT` |
| `resourceId` | number \| null | Entity id when present |
| `maker` | string | Username |
| `madeOnDate` | string (datetime) | Gson date string |

Display like classic: `{maker} - {actionName}` / `{entityName}` with date.

“View All” can deep-link to existing Audit / Checker screens (`/#/audits` or equivalent in Community App).

---

## Sample response (truncated, live)

```json
{
  "officeId": 1,
  "officeName": "Head Office",
  "asOfDate": "Aug 3, 2026 12:00:00 AM",
  "currencyCode": "USD",
  "availableCurrencies": ["USD", "ZWL"],
  "portfolio": {
    "activeClients": 369,
    "activeGroups": 0,
    "activeLoans": 48,
    "grossLoanBook": 10283.05,
    "portfolioAtRiskPercent": 100.0,
    "valueAtRisk": 10283.05,
    "loansInArrears": 48,
    "principalOverdue": 10283.05,
    "interestOutstanding": 3177.01,
    "interestCollectedMtd": 0.0,
    "nplCount": 48,
    "nplOutstanding": 10283.05,
    "writeOffsMtd": 0.0,
    "collectionRateMtdPercent": 0
  },
  "pipeline": {
    "pendingApprovalCount": 1,
    "pendingApprovalAmount": 200.0,
    "pendingDisbursementCount": 0,
    "pendingDisbursementAmount": 0.0,
    "disbursementsTodayAmount": 0.0,
    "disbursementsMonthAmount": 0.0,
    "collectionsExpectedToday": 0.0,
    "collectionsActualToday": 0.0,
    "collectionsExpectedMonth": 0.0,
    "collectionsActualMonth": 0.0
  },
  "aging": [
    { "bucket": "CURRENT", "loanCount": 0, "outstanding": 0 },
    { "bucket": "1_30", "loanCount": 0, "outstanding": 0 },
    { "bucket": "31_60", "loanCount": 0, "outstanding": 0 },
    { "bucket": "61_90", "loanCount": 0, "outstanding": 0 },
    { "bucket": "90_PLUS", "loanCount": 48, "outstanding": 10283.05 }
  ],
  "trends": {
    "period": "day",
    "newClients": [{ "bucket": "2026-07-23", "count": 0 }],
    "loansDisbursed": [{ "bucket": "2026-07-23", "count": 0, "amount": 0 }]
  },
  "recentActivity": [
    {
      "id": 12345,
      "actionName": "REPAYMENT",
      "entityName": "LOAN",
      "resourceId": 10,
      "maker": "administrator",
      "madeOnDate": "Jul 27, 2026 10:00:00 AM"
    }
  ]
}
```

---

## Suggested UI layout (map from classic home)

### Row 1 — Portfolio Performance

| Card | Field |
|------|--------|
| Clients | `portfolio.activeClients` |
| Groups | `portfolio.activeGroups` |
| Gross Loan Book | `portfolio.grossLoanBook` + `currencyCode` |
| Portfolio at Risk | `portfolio.portfolioAtRiskPercent` (format as `%`) |
| Value at Risk | `portfolio.valueAtRisk` |
| In Arrears | `portfolio.loansInArrears` |

**Recommended additions (same row or second strip):**

| Card | Field |
|------|--------|
| Active Loans | `portfolio.activeLoans` |
| NPL | `portfolio.nplCount` / `nplOutstanding` |
| Interest Collected (MTD) | `portfolio.interestCollectedMtd` |
| Collection Rate (MTD) | `portfolio.collectionRateMtdPercent` |

### Row 2 — Portfolio Pipeline

| Card | Field |
|------|--------|
| Pending Approval | `pipeline.pendingApprovalAmount` (+ count) |
| Pending Disbursement | `pipeline.pendingDisbursementAmount` (+ count) |
| Disbursements (Today) | `pipeline.disbursementsTodayAmount` |
| Collections (Expected / Actual) | `collectionsExpectedToday` / `collectionsActualToday` |
| Disbursements (Month) | `pipeline.disbursementsMonthAmount` |
| Month Collection (Expected / Actual) | `collectionsExpectedMonth` / `collectionsActualMonth` |

### Charts

| Widget | Data |
|--------|------|
| Client Trends | `trends.newClients` + `trends.loansDisbursed` (dual series). Office dropdown → pass `officeId`. Day/Week/Month → `trendPeriod`. |
| Amount Collected for Today | Bar/compare `pipeline.collectionsActualToday` vs `collectionsExpectedToday` (or small chart; often just two numbers). |
| Arrears Chart | `aging[]` by `bucket`, value = `outstanding` or `loanCount`. |
| Recent Activity | `recentActivity[]`. |

---

## Integration notes for Community App (`mopane-cbs-ui`)

1. **Replace** home report fan-out (`ClientTrendsByDay`, `Demand_Vs_Collection`, `Active Loans - Summary`, etc.) with this single Resource call.
2. Reuse existing HTTP stack (`ResourceFactory`, `$http`, Basic auth + tenant header) — do not invent a new client.
3. Office filter: load offices from existing `GET /offices` (or user office) and pass selected `officeId`.
4. Currency filter: on first load omit `currencyCode` (API defaults to USD if present among office loan currencies). Populate a switcher from `availableCurrencies` and refetch with `currencyCode` when the user switches (e.g. USD → ZWL).
5. Format money with existing currency filters using response `currencyCode`; treat amounts as numbers (not strings).
6. Parse `asOfDate` / `madeOnDate` carefully — Fineract Gson date format is **not** ISO-8601 by default.
7. On office, currency, or trend period change, refetch the same endpoint (one call).
8. Empty / zero data is valid — show zeros and empty charts, not hard errors.
9. Handle HTTP errors:
   - `401` — session/credentials
   - `403` — missing `READ_MOPANE_DASHBOARD` (show permission message)
   - `404` — wrong base path / old WAR, or invalid `currencyCode` for that office
   - Network — toast + retry

### Permission

Role needs **`READ_MOPANE_DASHBOARD`**. Users with `ALL_FUNCTIONS` / `REPORTING_SUPER_USER` already pass.  
Grant via Administration → Roles for other roles after deploy.

### TypeScript / JS sketch

```js
// Pseudocode for mopane-cbs-ui resource
ResourceFactory.mopaneDashboardLoanMetrics.get(
  { officeId: officeId, currencyCode: currencyCode, trendPeriod: period, activityLimit: 20 },
  function (data) {
    $scope.currencyCode = data.currencyCode;
    $scope.availableCurrencies = data.availableCurrencies;
    $scope.portfolio = data.portfolio;
    $scope.pipeline = data.pipeline;
    $scope.aging = data.aging;
    $scope.trends = data.trends;
    $scope.recentActivity = data.recentActivity;
  }
);
```

Register path: `mopane/dashboard/loan-metrics`.

---

## Supporting APIs (optional drill-down)

Not required for first paint; use for click-through:

| Action | API |
|--------|-----|
| Office list | `GET /offices` |
| Pending approvals list | `GET /runreports/Loans Pending Approval` or maker-checker |
| Loans in arrears | Advanced search / loan list filters |
| Full audit trail | `GET /audits` |
| Classic reports | `GET /runreports/{reportName}` |

---

## Out of scope (v1 backend)

Do not expect these fields yet:

- Staff / loan-officer leaderboard  
- Savings KPIs  
- FX conversion / cross-currency totals (switch currency instead; never sum across codes)  
- RBZ Form MFI-1 (separate: `GET /rbz/form-mfi1`)

---

## Backend source (for reference)

| Piece | Path |
|-------|------|
| API | `fineract-provider/.../mopane/dashboard/api/MopaneDashboardApiResource.java` |
| DTOs | `.../mopane/dashboard/data/MopaneDashboardLoanMetricsData.java` |
| Service | `.../mopane/dashboard/service/MopaneDashboardReadPlatformServiceImpl.java` |
| Permission migration | `.../sql/migrations/core_db/V5003__mopane_dashboard.sql` |
| Deploy notes | `deploy/docker/README.md` |

---

## Acceptance checklist for UI

- [ ] Dashboard loads with a single `loan-metrics` request after login  
- [ ] Initial load defaults to USD when that currency has loans in the office  
- [ ] Currency switcher uses `availableCurrencies` and refetches with `currencyCode`  
- [ ] Office change refetches metrics  
- [ ] Day / Week / Month trend toggle works  
- [ ] All classic KPI cards populated from `portfolio` / `pipeline`  
- [ ] At least NPL + collection rate MTD shown  
- [ ] Aging chart uses all five buckets  
- [ ] Recent activity renders maker / action / entity / date  
- [ ] Graceful handling when user lacks `READ_MOPANE_DASHBOARD`  
- [ ] Works on tenant subdomains (`million.mopane.co.zw`, etc.) against prod API  

---

## Contact / ownership

- Backend API owned in `MopaneCbsFineract` (`org.apache.fineract.mopane.dashboard`)  
- UI implementation owned in `mopane-cbs-ui` home / dashboard module  
- Prod verify: `https://fineract.mopane.co.zw/fineract-provider/api/v1/mopane/dashboard/loan-metrics` with a tenant that has the permission applied (Flyway `V5003`)
