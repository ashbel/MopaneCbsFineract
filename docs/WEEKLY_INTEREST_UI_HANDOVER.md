# Weekly interest calculation — UI handover (`mopane-cbs-ui`)

Frontend handover for a loan-product setting that chooses how a weekly flat rate is derived.

**Backend repo:** `MopaneCbsFineract`  
**UI repo:** `mopane-cbs-ui` (Community App / AngularJS)  
**API status:** In the backend. Not yet called out as deployed.  
**Screen:** Loan product create / edit. There is no product screen in the backend repo.

Place the control next to **Days in year**. Use the same pattern as `daysInYearType`: load options from the template, show `value` as the label, submit `id` as an integer.

---

## What the user is choosing

Both methods stay flat interest on the original principal. The only switch is how many weeks a year has when a rate becomes a weekly charge.

| `weeksInYearType` | Label | Meaning |
|-------------------|--------|---------|
| `52` | 52 weeks | Mopane. Current behaviour. Default for every existing product and loan. |
| `48` | 4-week month | Lenda. A month is 4 weeks, so a year is 48 week-slots. |

Worked example. Principal $200, 8 weekly repayments, 12% per month, flat interest, interest calculation period **same as repayment period**:

| Setting | Each week | Last week | Total interest |
|---------|-----------|-----------|----------------|
| 52 weeks | $5.54 | $5.53 | $44.31 |
| 4-week month | $6.00 | $6.00 | $48.00 |

The leftover cent on the 52-week loan is the existing last-installment adjustment. The UI does not round this itself.

`annualInterestRate` on the product stays monthly × 12. For 12% per month that is still **144** under both settings. The setting changes the week divisor, not that stored annual figure.

A rate quoted **per week** uses the same week count. 3% per week stays 3% per week (3 × 52 ÷ 52, or 3 × 48 ÷ 48). If the product form computes an annual figure live, multiply a weekly quote by the selected `weeksInYearType`. Do not hardcode 52. A monthly quote is still × 12.

`repaymentEvery` is unchanged. “Every 2 weeks” is two of those week-slots.

---

## Where it applies

The divisor is used only when both are true:

- repayment frequency is **Weeks** (`repaymentFrequencyType` = `1`)
- interest calculation period is **Same as repayment period** (`interestCalculationPeriodType` = `1`)

It applies to flat and declining interest. Daily interest (`interestCalculationPeriodType` = `0`) keeps using days-in-year.

For any other repayment frequency the value is still stored. It has no effect on the schedule until repayments are weekly and interest is same-as-repayment.

---

## Product screen

### Read

| Call | What you get |
|------|----------------|
| `GET /loanproducts/template` | New product. `weeksInYearType` is 52. `weeksInYearTypeOptions` is the dropdown. |
| `GET /loanproducts/{productId}` | Saved value in `weeksInYearType`. |
| `GET /loanproducts/{productId}?template=true` | Saved value plus `weeksInYearTypeOptions`. |

Options, in this order:

```json
"weeksInYearTypeOptions": [
  { "id": 52, "code": "WeeksInYearType.weeks52", "value": "52 weeks" },
  { "id": 48, "code": "WeeksInYearType.weeks48", "value": "4-week month" }
]
```

Saved value has the same shape. Display `value`. Submit `id`.

```json
"weeksInYearType": {
  "id": 48,
  "code": "WeeksInYearType.weeks48",
  "value": "4-week month"
}
```

### Write

`POST /loanproducts` and `PUT /loanproducts/{productId}`.

Send an integer, same as `daysInYearType`:

```json
"weeksInYearType": 48
```

Optional on create. Omitted means 52. Allowed values are only `52` and `48`.

On update, omitted fields keep the stored product value. The server checks the **resulting** combination. A PUT that only switches interest calculation to daily will fail if the product is already 48 and weekly.

Changing the product does not recalculate loans already on the book. Say that on the product screen if the control is edited.

### Suggested control

- Label: **Weeks in year**
- Options: the template list. Do not hardcode the labels if `value` is present. Fallback labels are “52 weeks” and “4-week month”.
- Default selection: `52` when `weeksInYearType` is missing.
- Help text: “Used when repayments are weekly and interest is calculated the same as the repayment period. 52 weeks is the existing rule. 4-week month treats each month as 4 weeks.”

Keep the control enabled for non-weekly products so the choice is saved for later. Block only the combination below.

### Validation to mirror

Reject in the form, and handle the same error if the API returns it:

**48 + weekly repayments + daily interest** is invalid. Daily interest uses days-in-year and would ignore this setting.

| Field | Invalid value |
|-------|----------------|
| `weeksInYearType` | `48` |
| `repaymentFrequencyType` | `1` (Weeks) |
| `interestCalculationPeriodType` | `0` (Daily) |

52 with weekly and daily is allowed. 48 with monthly, or 48 with weekly and same-as-repayment, is allowed.

API error codes (`userMessageGlobalisationCode`):

| Code | When |
|------|------|
| `validation.msg.loanproduct.weeksInYearType.cannot.be.used.when.interest.is.calculated.daily` | 48 + weekly + daily |
| `validation.msg.loanproduct.weeksInYearType.is.not.one.of.expected.enumerations` | Anything other than 52 or 48 |

Suggested message for the daily case: “4-week month cannot be used when interest is calculated daily. Choose 52 weeks, or calculate interest the same as the repayment period.”

---

## Loan application and loan view

Do **not** add an editable control on the loan application.

- Do not send `weeksInYearType` on `POST /loans` or on the calculate-schedule command. It is not an accepted loan parameter, and the request will be rejected as an unsupported parameter.
- The schedule is built from the **product** at application time.
- `GET /loans/{loanId}` returns the snapshotted `weeksInYearType` in the same enum shape as the product.
- `GET /loans/template?templateType=individual&productId={id}` (and the group / jlg variants) also returns the product’s `weeksInYearType` on the loan payload. Show it read-only if staff should see which rule the new loan will use.
- There is no `weeksInYearTypeOptions` on the loan template. The loan cannot choose.

Editing the product later does not change an existing loan’s schedule, reschedule, or prepayment. Those keep the value stored on the loan.

---

## UI checklist

1. Product template loads `weeksInYearTypeOptions` and selects 52 for a new product.
2. Product edit shows the saved `weeksInYearType.value`.
3. Create and update submit `weeksInYearType` as `52` or `48`.
4. 48 cannot be saved with weekly repayments and daily interest.
5. Loan application does not send the field.
6. Loan view can show the snapshotted label and does not offer a way to change it.
7. A weekly flat preview for $200, 8 weeks, 12% per month matches the table above: $44.31 (52) or $48.00 (48).
