# Capitalised fee (Mambu-style) – API examples

## Add a loan charge that capitalises into principal (mid-term)

`POST /fineract-provider/api/v1/loans/{loanId}/charges`

Charge definition must be a **fee** (not penalty) with charge time **specified due date** or **disbursement (capitalised)**.

```json
{
  "chargeId": 1,
  "amount": "100.00",
  "dueDate": "18 March 2026",
  "dateFormat": "dd MMMM yyyy",
  "locale": "en",
  "isCapitalized": true
}
```

- Creates a **loan charge** (fee applied).
- On an **active, disbursed** loan, also posts **`CAPITALIZED_FEE`** (principal increase), marks the charge settled, bumps scheduled principal, and regenerates the repayment schedule.
- **Multi-disbursement** loans: capitalised fees are rejected until supported.

## Disbursement-time capitalised fees

1. Use a charge with **Disbursement (capitalised)** timing, **or** a disbursement-time fee with `"isCapitalized": true` in the loan application `charges[]` payload.
2. On disbursement, scheduled principal increases by the fee total (cash disbursement unchanged); schedule regenerates; one **`CAPITALIZED_FEE`** transaction is posted per applicable charge.

## Accounting

- **Cash / accrual (loan products):** Dr **Loan portfolio**, Cr **Income from fees** for `CAPITALIZED_FEE`.
- Global config `capitalized-fee-accounting` (`direct-income` | future `unearned-income`) is seeded in migration V344; implementation currently follows **direct income**.

## Statement

- **Fees charged** (`FEE` / charge applied) when upfront accrual path runs.
- **Capitalised fee** (`CAPITALIZED_FEE`) with principal portion = fee amount.
