# Mopane CBS Accounting Module
## User Handbook

**Product:** Mopane Core Banking System (CBS)
**Interface:** Mopane CBS web app
**Platform:** Apache Fineract / Mifos X — release **17.07.01.RELEASE**
**Audience:** Accountants, finance officers, branch operations staff, and system administrators

---

## About this handbook

This handbook explains how accounting works in Mopane CBS: what the system posts for you, what you post by hand, and how to close a period and produce financial statements.

Screen names, buttons and field labels match what you see in the Mopane CBS web app. Where your institution's chart of accounts, account codes, products or branch names differ from the examples, use your own — the examples here are illustrative only.

| If you are | Start with |
| --- | --- |
| **New to the module** | Part 1 (concepts) and Part 2 (where everything lives), then work forward |
| **Posting day to day** | Part 5 (manual journals and teller cash) and section 7.2 (daily rhythm) |
| **Setting the system up** | Part 3 (chart of accounts and go-live) and Part 4 (product mappings) |
| **Closing a period** | Part 6 (accruals, provisioning, closures) and Part 7 (reports and month-end) |
| **Investigating a problem** | Section 7.3 (troubleshooting) and the Appendix (screen map) |

**What you need before you can post anything:**

- A login to your institution's Mopane CBS system, with accounting permissions granted by your administrator.
- Knowledge of how to open a client and a loan account.
- A basic understanding that every entry has an equal debit and credit.

---

# Part 1 — Accounting concepts you need

## 1. Double-entry, always

Every journal in Mopane CBS is a balanced set of debit and credit lines that share one **transaction ID**. The system will not save a manual journal unless total debits equal total credits.

Typical natural balances:

| Account type | Increases with | Decreases with | Examples |
| --- | --- | --- | --- |
| **Asset** | Debit | Credit | Cash, bank, loan portfolio, interest receivable |
| **Liability** | Credit | Debit | Savings control, overpayment liability, provisions |
| **Equity** | Credit | Debit | Capital, opening-balance contra, retained earnings |
| **Income** | Credit | Debit | Interest income, fee income, penalty income |
| **Expense** | Debit | Credit | Interest on savings, write-offs, provisioning expense |

## 2. Header vs Detail accounts

| Usage | Purpose | Can you post to it? |
| --- | --- | --- |
| **Header** | Folder / grouping (for example “Cash and Bank”) | No |
| **Detail** | The actual GL account (for example “Cash on Hand — Main Branch”) | Yes |

Only **Detail** accounts appear in journal-entry pickers. If a parent needs children under it, the parent must stay a Header.

## 3. Automatic vs manual journals

| Source | When it posts | Where you see it |
| --- | --- | --- |
| Loan / savings / deposit / share products | When you disburse, repay, deposit, withdraw, charge, write off, accrue, and so on | Loan or savings transaction → journal; also **Accounting → Search Journal Entries** |
| Manual journal | When a user clicks **Save** on **Add Journal Entries** | Search Journal Entries (filter **Manual**) |
| Frequent posting | When a user posts using a predefined **Accounting Rule** | Same as a manual journal, but constrained to the rule’s accounts |
| Opening balances | Once, per office and currency, at go-live | Opening-balance contra plus each GL |
| Provisioning entry | When you create a provisioning run and tick **Create Journal Entries** | Provisioning journals |

## 4. Four accounting rules on products

Set on the **Accounting** step of a loan, savings, fixed deposit, recurring deposit, or share product.

| Option in the UI | Meaning | Typical use |
| --- | --- | --- |
| **None** | Portfolio works; **no GL** | Testing, or products you do not want on the books |
| **Cash** | Income is recognised when cash is received; expenses when cash is paid | The most common choice at microfinance institutions |
| **Accrual (periodic)** | Interest, fees, and penalties are accrued up to a chosen date (a scheduled job, or **Accounting → Accruals**) | When the books must show earned-but-unpaid income |
| **Accrual (upfront)** | Accrual journals are posted at **disbursement** for the expected income | Less common; only if finance has chosen this policy |

## 5. Office and currency

- Journals belong to an **office** (branch). Closures are also per office.
- Journals belong to a **currency**. A USD loan does not post to a ZWL cash account.
- Your institution has its own chart of accounts, products and books, separate from every other institution on the platform.

## 6. Accounting closures freeze the past

When an office is closed as at a date, **no** journal (automatic or manual) may be posted on or before that date. To backdate, the closure must be deleted first. Closures are a control, not a report.

---

# Part 2 — Finding Accounting in the web app

## Top menu

On the black toolbar, click **Accounting**. That opens the Accounting home page (`#/accounting`).

The **Reports** menu (same toolbar) has a sub-item **Accounting** (`#/reports/accounting`) for Trial Balance, Income Statement, Balance Sheet, and General Ledger.

## Accounting home — what each tile does

The home page is two columns of tiles. Users only see tiles their role is allowed to open.

**Left column (daily work)**

| Tile (screen title) | What it is for | Permission |
| --- | --- | --- |
| **Frequent Postings** | Post a recurring journal using a saved rule (cashier-friendly) | `CREATE_JOURNALENTRY` |
| **Add Journal Entries** | Free-form debit/credit journal | `CREATE_JOURNALENTRY` |
| **Search Journal Entries** | Find, inspect, and reverse journals | `READ_JOURNALENTRY` |
| **Accounts Linked to Financial Activities** | Map system activities (cash at teller, opening-balance contra, transfers) to GL accounts | `READ_FINANCIALACTIVITYACCOUNT` |
| **Migrate Opening Balances (Office-wise)** | Load starting GL balances at go-live | `READ_JOURNALENTRY` to open; `DEFINEOPENINGBALANCE_JOURNALENTRY` to save |

**Right column (setup and period-end)**

| Tile (screen title) | What it is for | Permission |
| --- | --- | --- |
| **Chart of Accounts** | Create and maintain GL accounts | `READ_GLACCOUNT` |
| **Closing Entries** | Freeze journals up to a date, per office | `READ_GLCLOSURE` |
| **Accounting Rules** | Templates used by Frequent Postings | `READ_ACCOUNTINGRULE` |
| **Accruals** | Run periodic accruals up to a date | `EXECUTE_PERIODICACCRUALACCOUNTING` |
| **Provisioning Entries** | Create and review loan-loss provisioning runs | `VIEW_PROVISIONING_ENTRIES` |

## Shortcuts on the left navigation

The left rail also has **Frequent Postings**, **Add Journal Entries**, **Closing Entries**, and **Chart of Accounts**. Same screens, fewer clicks.

## Breadcrumbs

Every Accounting screen starts with **Accounting** in the breadcrumb. Use it to return to the home page.

---

# Part 3 — Chart of Accounts and go-live setup

This part must be completed before any live lending with accounting enabled. Work through it in this order:

1. Create the Chart of Accounts.
2. Map the **Opening Balance Transfer Contra** under Accounts Linked to Financial Activities.
3. Migrate opening balances (office by office, currency by currency).
4. Map products (Part 4).

Do not enable accounting on products, and do not post journals, **before** opening balances are in — the system will refuse opening balances once accounting-enabled products already have accounts.

---

## 3.1. Chart of Accounts

**Path:** Accounting → **Chart of Accounts**

### List view vs Tree view

- **List view** (default): table of Account, GL Code, Account type, Disabled?, Manual entries allowed, Used as (Header/Detail). Filter box: *Filter by Account / GL Code / Account type*.
- **Tree view** (blue button, top right): hierarchy. Click a node to see GL code, type, usage, manual-entries flag, and description.

Click a row to open the account detail page.

### Create a GL account

**Path:** Chart of Accounts → **Add Account**

| Field | Required? | How to fill it |
| --- | --- | --- |
| **Account Type** | Yes | Asset, Liability, Equity, Income, or Expense |
| **Parent** | No | The Header this account sits under. Parent must be the same type. |
| **GL Code** | Yes | Unique code (for example `1110`, `4100`). Agree a numbering scheme before you start creating accounts. |
| **Account name** | Yes | Short name as it should appear on statements |
| **Account Usage** | Yes | **Header** or **Detail** |
| **Tag** | No | Optional classification (Cash, Bank, Portfolio). Tags are maintained under **Admin → System → Manage Codes** (`AssetAccountTags`, `LiabilityAccountTags`, `EquityAccountTags`, `IncomeAccountTags`, `ExpenseAccountTags`). |
| **Manual entries allowed** | — | Tick if accountants may post to this account by hand. **Untick** for control accounts that should only be posted by the system (loan portfolio, savings control). |
| **Description** | No | What the account is for |

**Rules to teach:**

- Build **all Headers for a type first**, then Detail accounts under them. Repeat for Asset, Liability, Equity, Income, Expense.
- You cannot create a child under a Detail account. If you need a child, the parent must be a Header.
- Do not reuse GL codes. Codes are unique.
- Disable an account instead of deleting it once it has history.

### Edit, disable, enable, delete, add child

On the account detail page:

| Button | Use |
| --- | --- |
| **Add Child Account** | Only on Header accounts |
| **Disable / Enable** | Stops or resumes new use of the account |
| **Edit** | Change name, parent, tag, manual-entries flag, description (use care if already mapped) |
| **Delete** | Only if unused. Confirm in the popup. |

### A minimum workable chart

This is the smallest chart that supports lending, savings and a trial balance. Adapt the codes and names to your institution's own chart.

| Type | Header | Detail examples |
| --- | --- | --- |
| Asset | Cash and Bank | Cash on Hand; Bank — Current; Cash at Teller |
| Asset | Loan Portfolio | Gross Loan Portfolio |
| Asset | Receivables | Interest Receivable; Fees Receivable; Penalties Receivable |
| Liability | Customer Deposits | Savings Control |
| Liability | Other Liabilities | Overpayments; Loan Loss Provision |
| Equity | Capital and Reserves | Opening Balances Contra; Retained Earnings |
| Income | Lending income | Interest on Loans; Fees; Penalties; Recoveries |
| Expense | Lending expense | Write-offs; Interest on Savings; Provisioning expense |

---

## 3.2. Accounts Linked to Financial Activities

**Path:** Accounting → **Accounts Linked to Financial Activities** → **Define New Mapping**

The system needs certain GL accounts for **internal** movements (not product mappings). Each financial activity maps to **one** GL account of the correct type.

| Financial activity | Must map to | When it is used |
| --- | --- | --- |
| **Opening Balances Transfer Contra** | Equity | Contra side of opening-balance journals. **Required before migrating opening balances.** |
| **Cash at Main Vault** | Asset | Teller / vault cash movements |
| **Cash at Teller** | Asset | Teller cash |
| **Asset Transfer** | Asset | Loan-to-loan and similar asset transfers |
| **Liability Transfer** | Liability | Account transfers involving liability accounts (for example savings-funded disbursement) |
| **Fund Source** | Asset | Default funding source in some transfer flows |
| **Payable Dividends** | Liability | Share-product dividend payables |

---

## 3.3. Migrate Opening Balances (Office-wise)

**Path:** Accounting → **Migrate Opening Balances (Office-wise)**

Use this **once per office and currency** when moving from a previous ledger (or from zero) onto Mopane CBS.

### Steps

1. Select **Office** and **Currency**.
2. Click **Search Data**.
3. Confirm the **Opening balances contra account** shown is the Equity account you mapped.
4. Enter **Date of opening balances**.
5. For each Detail GL, enter either a **Debit** or a **Credit** (not both on the same line).
6. Watch the running totals in the column headings. **Total debits must equal total credits.** The contra account absorbs the balancing figure when you submit.
7. Click **Save** (`DEFINEOPENINGBALANCE_JOURNALENTRY`).

### Hard rules (teach these)

- Opening balances must be set **before** any other journals, and **before** accounting-enabled loan or savings products have live accounts.
- Repeat the screen for each office and each currency (USD separately from ZWL).
- Do not treat this as a monthly recast. After go-live, use normal journals and period close.

---

## 3.4. Bulk import (optional, admins)

For a large chart, use **Admin → Organisation → Bulk Import**:

- **Import Chart of Accounts** — download the Excel template, fill, upload, refresh the import grid.
- **Import Journal Entries** — same pattern for a batch of manuals.

There is also a Mopane-side workbook at `docs/templates/CBS_Accounting_Import_Template.xlsx` for structured GL loading. Prefer the in-app template when importing through the UI so column names match what the server expects.

---

## 3.5. Payment types (do this before product mappings)

**Path:** Admin → Organisation → **Manage Payment Types** (`#/viewpaymenttype/`)

Payment types are the channels money can arrive or leave on: Cash, Cheque, Bank transfer, EcoCash, OneMoney, Payroll / SSB deduction.

| Field | Notes |
| --- | --- |
| **Payment type name** | What the teller picks on a repayment, deposit or journal |
| **Description** | Optional |
| **Is cash payment** | Tick for physical cash. Cash and teller reports rely on this flag. |
| **Position** | Order in the dropdown |

Do this **before** you map products in Part 4. The product-level **fund source for payment channel** mapping lists exactly these payment types. If they do not exist, every channel posts to the product's single default fund source, and separating bank from cash becomes a manual reconciliation each month.

---

# Part 4 — Product accounting and automatic journals

This is the most important part of the handbook. If products are mapped correctly, most of the general ledger looks after itself.

**Path for loans:** Admin → Products → **Loan Products** → create or edit → wizard step **Accounting**

The same **Accounting** step exists on Savings, Fixed Deposit, Recurring Deposit, and Share products.

---

## 4.1. Loan product — Accounting step

Four radio buttons: **None**, **Cash**, **Accrual (periodic)**, **Accrual (upfront)**.

When anything other than None is selected, mapping dropdowns appear.

### Asset mappings (all enabled modes)

| UI label | Typical GL | What it does |
| --- | --- | --- |
| **Fund source** | Bank or Cash | Credited on disbursement; debited on repayment (cash in) |
| **Loan portfolio** | Gross loans | Debited on disbursement; credited when principal is repaid or written off |
| **Transfers in suspense** | Suspense asset | Used while a loan is in transfer between offices |
| **Receivable interest** | Interest receivable | **Accrual only** — holds accrued interest until paid |
| **Receivable fees** | Fees receivable | **Accrual only** |
| **Receivable penalties** | Penalties receivable | **Accrual only** |

### Income mappings

| UI label | Typical GL | What it does |
| --- | --- | --- |
| **Income from interest** | Interest income | Credited when interest is recognised (on cash receipt, or on accrual then reversed into cash on payment) |
| **Income from fees** | Fee income | Credited when fees are recognised |
| **Income from penalties** | Penalty income | Credited when penalties are recognised |
| **Recovery payments** | Recovery income | Credited on recovery of a written-off loan |

### Expense and liability

| UI label | Typical GL | What it does |
| --- | --- | --- |
| **Losses written off** | Write-off expense | Debited on principal write-off (accrual also writes off interest/fees/penalties through this account) |
| **Over payment liability** | Overpayments | Credited when the client pays more than is due; debited when you refund |

### Advanced accounting rules (click to expand)

Use these when one product needs **exceptions**:

1. **Configure fund sources for payment channels** — Payment type (Cash, Cheque, EcoCash, Bank transfer) → a specific asset account. Example: Cheque repayments debit **Bank**, cash repayments debit **Cash on Hand**.
2. **Map fees to income accounts** — a named fee (arrangement fee, insurance) to its own income (or liability) account instead of the product default.
3. **Map penalties to specific income accounts** — same idea for penalties.

Every mapping field on this screen carries a **?** icon. Hover it before you guess: the tooltips describe exactly what the account is used for.

---

## 4.2. Worked example — cash accounting, USD 1,000 loan

Assume:

- Fund source = Bank 1120  
- Loan portfolio = 1210  
- Income from interest = 4100  
- Income from fees = 4200  
- Losses written off = 5100  
- Overpayment liability = 2300  

### Disbursement of 1,000

| Debit | Credit |
| --- | --- |
| Loan portfolio 1,000 | Bank 1,000 |

### Repayment of 120 (100 principal + 20 interest)

| Debit | Credit |
| --- | --- |
| Bank 120 | Loan portfolio 100 |
|  | Interest income 20 |

### Fee of 15 paid with the repayment (total cash 135)

| Debit | Credit |
| --- | --- |
| Bank 135 | Loan portfolio 100 |
|  | Interest income 20 |
|  | Fee income 15 |

### Write-off of remaining principal 900

| Debit | Credit |
| --- | --- |
| Write-off expense 900 | Loan portfolio 900 |

### Recovery of 50 after write-off

| Debit | Credit |
| --- | --- |
| Bank 50 | Recovery income 50 |

### Overpayment of 30 on a fully paid loan

The extra 30 credits **Over payment liability**. A later refund debits that liability and credits Bank.

---

## 4.3. Mopane-specific: capitalised fees

Mopane CBS can add a **capitalised fee** that increases loan principal instead of being collected as a separate charge.

**Cash journals for a capitalised fee:**

| Debit | Credit |
| --- | --- |
| Loan portfolio (fee amount) | Income from fees (fee amount) |

There is no cash movement at that moment. Principal (and therefore the schedule) goes up; fee income is recognised immediately under cash accounting.

---

## 4.4. Other Mopane-specific behaviour finance should know

| Feature | What accounting sees | Where to look |
| --- | --- | --- |
| **Capitalised fee** | A distinct **Capitalised fee** loan transaction (type 21). Journal: **Dr Loan portfolio / Cr Income from fees**, under both cash and accrual. No cash moves. | Loan → transactions; then Search Journal Entries by that transaction ID |
| **SSB / Pensions RES import** | When the bureau response is SUCCESS and the loan is Approved, the loan **auto-disburses** (config `ssb-res-auto-disburse`). That disbursement posts the normal journal: Dr Loan portfolio / Cr Fund source. | Reports → SSB/Pensions RES import batches; then the loan's disbursement journals |
| **SSB / Pensions PAY import** | Imported repayments post as ordinary loan repayments, with their normal journals. Rows in *Needs Review* post **nothing** until approved. | PAY import batch → review queue |
| **Office short codes** | Every office carries a mandatory short code that prefixes client and account numbers. It does not change any journal, but it is how you tell at a glance which branch an account belongs to. | Admin → Organisation → Manage Offices |
| **Dashboard aging** | The Mopane dashboard ages portfolio on **overdue principal**. It will not agree line-for-line with a provisioning run that uses different buckets. | Dashboard vs Provisioning Criteria |

---

## 4.5. Accrual (periodic) — extra journals

Same disbursement as cash (portfolio vs fund source).

**When you run accruals** (section 6.1), for interest earned but not yet paid:

| Debit | Credit |
| --- | --- |
| Interest receivable | Interest income |

**When the client later pays that interest:**

| Debit | Credit |
| --- | --- |
| Bank | Interest receivable |

Income was already recognised; cash now clears the receivable. Fees and penalties follow the same pattern with Fees Receivable / Penalties Receivable.

**Accrual (upfront)** posts those receivable/income journals at **disbursement** for the expected amounts, then adjusts as the loan runs. Only use this if finance policy requires it.

---

## 4.6. Savings product — Accounting step

**Path:** Admin → Products → **Savings Products** → Accounting

Savings products typically offer **None** or **Cash**.

| UI label | Type | What it does |
| --- | --- | --- |
| **Saving reference** | Asset (cash/bank) | Debited on deposit; credited on withdrawal |
| **Overdraft portfolio control** | Asset | Increases when the client draws an overdraft |
| **Saving control** | Liability | The customer deposit book: credited on deposit, debited on withdrawal |
| **Savings transfers in suspense** | Liability | Transfers in progress |
| **Escheat liability** | Liability | Unclaimed dormant funds (only if dormancy tracking is on) |
| **Interest on savings** | Expense | Debited when interest is posted to the client |
| **Write off** | Expense | Overdraft write-off |
| **Income from fees / penalties / overdraft interest** | Income | Charged to the saver |

**Deposit 200 cash:**

| Debit | Credit |
| --- | --- |
| Savings reference (cash) 200 | Savings control 200 |

**Withdrawal 50:** opposite. **Interest posting 5:** debit Interest on savings, credit Savings control.

Fixed deposit and recurring deposit products use the same cash pattern. Share products (if used) map **Shares reference**, **Shares suspense**, **Income from fees**, and **Shares equity**.

---

## 4.7. Seeing automatic journals in the UI

1. Open the **loan account**.
2. On a transaction row, open the action that views **journal entries** (the app routes to **View Transaction**, `#/viewtransactions/{id}`).
3. You should see:
   - Banner: *These journal entries were created by the portfolio transaction…*
   - Office, transaction date, created by / on
   - Lines: Entry ID, Type (Asset/Income/…), Account (name and GL code), Debit, Credit
4. Click a line for payment details, reference, comments.
5. **Reverse** is offered only for **manual** journals. To undo a loan repayment, reverse the **loan transaction**, and the system reverses the journals.

**Also:** Accounting → **Search Journal Entries** → filter by date and office → click the **Transaction ID**. Automatic and manual journals share this screen.

---

# Part 5 — Manual journals, rules, frequent postings and teller cash

---

## 5.1. Add Journal Entries

**Path:** Accounting → **Add Journal Entries** (or left nav)

Use this for anything that is not generated by a product: office rent, salaries, bank charges, petty cash, suspense clearing, year-end adjustments.

### Fields

| Field | Required | Notes |
| --- | --- | --- |
| **Office** | Yes | Branch the entry belongs to |
| **Currency** | Yes | Must match the accounts’ currency context |
| **Debit** account + amount | Yes | Green **+** adds another debit line |
| **Credit** account + amount | Yes | Green **+** adds another credit line |
| **Reference number** | No | Cheque number, invoice number, batch ID — encourage a standard |
| **Transaction date** | Yes | Cannot be after today; cannot fall on a closed period |
| **Show Payment Details** | No | Payment type, account number, cheque, routing code, receipt, bank number |
| **Comments** | No | Always write *why* |

Only **Detail** accounts with **Manual entries allowed** appear in the lists.

### Compound entries

Debits and credits may be many-to-one or many-to-many. Example: pay rent 800 and electricity 200 from bank 1,000 — two debits, one credit.

Click **Save**. The next screen is **View Transaction** with the new transaction number.

### Reverse a manual journal

On View Transaction, if it is a manual entry and not already reversed, **Reverse** (`REVERSE_JOURNALENTRY`) asks for comments and posts a reversing journal. The original is marked reversed. You cannot reverse a reversal.

---

## 5.2. Accounting Rules

**Path:** Accounting → **Accounting Rules** → **Add Rule**

A rule is a **template**: which accounts (or tagged lists of accounts) may be debited and credited. Frequent Postings uses rules so non-accountants cannot pick arbitrary GLs.

### Fields

| Field | Meaning |
| --- | --- |
| **Accounting rule name** | For example `Pay office rent`, `Cash to bank`, `Petty cash replenish` |
| **Office** | Scope of the rule |
| **Description** | Shown to the person posting |
| **Debit rule type** | **Fixed** = one account, or **List** = accounts that have selected **tags** |
| **Credit rule type** | Same |
| **Allow multiple debit / credit entries** | When using tags, allow more than one line |

---

## 5.3. Frequent Postings

**Path:** Accounting → **Frequent Postings**

This is the same journal engine as Add Journal Entries, but:

1. Choose **Office**.
2. Choose an **Accounting rule**.
3. Choose **Currency**.
4. The debit and credit account lists shrink to what the rule allows.
5. Enter amounts, date, optional payment details and comments.
6. **Save**.

---

## 5.4. Search Journal Entries

**Path:** Accounting → **Search Journal Entries**

### Filters

- Account name or GL code  
- Office  
- Manual only vs all  
- From date / To date  
- Transaction ID  

Click **Search**. Columns: Entry ID, Office, Transaction date, Transaction ID, Type, Created by, Account, Debit, Credit.

Click any cell to open the full transaction. Use this to answer: “Why is Bank 1120 short today?”

---

## 5.5. Teller and cashier cash management (branch cash control)

**Path:** Admin → Organisation → **Tellers / Cashiers** (`#/tellers`)

Mopane CBS can track physical branch cash — vault versus each cashier's drawer — and posts the movement to the GL automatically.

### Setup (once per branch)

1. **Accounting → Accounts Linked to Financial Activities** — map **Cash at Main Vault** and **Cash at Teller** to asset GL accounts. **Both** are required; if either is missing, allocation and settlement fail with a "financial activity account not found" error.
2. **Admin → Organisation → Tellers → Create Teller** — name, office, description, valid-from / valid-to, status.
3. Open the teller → **Cashiers** → **Create Cashier** — pick the staff member and the period they hold the drawer.

### Daily use

| Action | Where | Permission | Journal posted |
| --- | --- | --- | --- |
| **Allocate Cash** to a cashier | Teller → Cashiers → cashier → Allocate Cash | `ALLOCATECASHIER_TELLER` | Dr **Cash at Teller** / Cr **Cash at Main Vault** |
| **Settle Cash** from a cashier | same screen → Settle Cash | `SETTLECASHFROMCASHIER_TELLER` | Dr **Cash at Main Vault** / Cr **Cash at Teller** |
| **Transactions** | Teller → Cashiers → cashier → Transactions (per currency) | `READ_TELLER` | View only |

**Teach these points:**

- Allocation and settlement journals are **system entries, not manual entries**. They carry no **Reverse** button. Fix a mistake with an opposite allocate/settle and write the reason in the note field.
- Cashier transactions are **per currency**. USD and ZWL are separate running balances on separate screens.
- End of day: settle the cashier back to the vault, then agree **Cash at Teller** to zero (or to the agreed float) before anyone goes home.

### Daily Teller Cash Report

**Path:** Reports → **Daily Teller Cash Report** (a Mopane addition, Pentaho, backed by the `CashierTransactionSummary` procedure)

Run it by office, teller, cashier, currency and date. Use it as the cash-up sheet before settling, not after.

---

# Part 6 — Accruals, provisioning and closures

---

## 6.1. Periodic accruals

**Path:** Accounting → **Accruals**

Screen title: **Execute Periodic Accrual Accounting**. One field: **Accrue till** date. Button: **Run Periodic Accruals**.

This only affects loans whose product is **Accrual (periodic)**. It posts interest / fee / penalty accruals up to that date.

Also scheduled under **Admin → System → Scheduler Jobs**:

| Job | Role |
| --- | --- |
| **Add Accrual Transactions** | Accrual processing |
| **Add Periodic Accrual Transactions** | Daily periodic accruals (typically shortly after midnight) |
| **Add Accrual Transactions For Loans With Income Posted As Transactions** | Loans that post income as transactions |
| **Update Accounting Running Balances** | Keeps GL running balances current for reports |

After a run: Search Journal Entries for the date; filter the receivable accounts. On the loan, tick **Accrual** on the transactions filter to see accrual rows.

---

## 6.2. Loan-loss provisioning

Two stages.

### Stage 1 — Criteria (setup, once)

**Path:** Admin → Organisation → **Provisioning Criteria**

For each arrears bucket (provisioning category):

- Minimum age / maximum age in **days** overdue  
- Provisioning **percentage**  
- **Liability account** (Loan Loss Provision)  
- **Expense account** (Provisioning expense)  
- Which **loan products** the criteria apply to  

Align buckets with the institution’s policy and, where relevant, RBZ expectations.

### Stage 2 — Entries (usually month-end)

**Path:** Accounting → **Provisioning Entries** → **Create Provisioning Entry**

1. Choose the **date**.
2. Optionally tick **Create Journal Entries** (debit expense, credit provision liability).
3. **Save**.

On the list you can **View** the calculated report, **Recreate** (only if journals were not yet created), or **View journals** once they exist.

A scheduler job **Generate Loan Loss Provisioning** can automate the calculation; journals still need a controlled month-end process.

---

## 6.3. Closing Entries

**Path:** Accounting → **Closing Entries** → **Create Closure**

| Field | Notes |
| --- | --- |
| **Office** | One closure per office. The form shows **Previously closed on** when you pick the office. |
| **Closing date** | Last date included in the closed period |
| **Comments** | For example “August 2026 month-end” |

After close, any journal (including a loan repayment) dated on or before that date fails with an accounting-closed error.

**To backdate:** Closing Entries → open the closure → **Delete** → confirm → post the backdated transaction → **recreate the closure**. Deleting a closure is a controlled exception, not a habit.

**Suggested policy:** restrict the delete-closure permission to head office finance, and have branches request a reopening in writing. A closure that anyone can delete is not a control.

---

# Part 7 — Reports, month-end and troubleshooting

---

## 7.1. Accounting reports

**Path:** Reports → **Accounting** (`#/reports/accounting`)

Four reports are registered in the **Accounting** category. All four are **Pentaho** reports, so the Pentaho reporting plugin and the `.prpt` files must be installed on the server. If a report opens but returns an error or a blank page, that is an IT/server issue, not a data issue.

| Report name (exactly as listed) | Parameters | Use |
| --- | --- | --- |
| **Trial Balance** | Office, from / to date | Debits and credits by GL for a period; must balance |
| **Income Statement** | Office, from / to date | Profit and loss (income and expense) |
| **Balance Sheet** | Office, as-at date | Assets, liabilities, equity at a point in time |
| **GeneralLedgerReport** | Office, **GL account** (or ALL), from / to date | Movements and running balance on selected accounts |

Open a report, fill office / dates / account / output format (HTML, PDF, Excel, CSV), then run it.

**Trial Balance is the control report.** If it does not balance, stop and investigate before closing the period.

### Operational reports finance also needs

These sit under other **Reports** categories, not under Accounting:

| Report | Category | Why finance wants it |
| --- | --- | --- |
| **Daily Teller Cash Report** | Mopane addition | Cash-up per teller and cashier before settlement |
| **All Loans** | Loan (Mopane addition) | Every loan including closed, written-off and rejected — reconciles portfolio movement, unlike *Active Loans - Details* |
| **Written-Off Loans** | Loan | Agrees write-off expense to the portfolio |
| **Portfolio at Risk / Aging** | Loan | Feeds provisioning percentages |
| **Collection and disbursal reports** | Loan / Client | Ties cash movement to portfolio activity |

**RBZ Form MFI-1** and the **SSB / Pensions** screens are Mopane-specific regulatory and payroll items under **Reports**. They read the portfolio; they are not the general ledger and they do not post journals.

---

## 7.2. Recommended operating rhythm

### Daily (branch)

1. Post teller and cash movements with **Frequent Postings** (or Add Journal Entries if no rule exists).
2. Complete all loan disbursements, repayments, savings deposits and withdrawals — journals follow automatically.
3. **Search Journal Entries** for the branch and today’s date. Investigate anything unexpected.
4. If using tellers, reconcile **Cash at Teller** to physical cash.

### Month-end (finance)

1. Confirm all portfolio transactions for the month are posted (no pending collection sheets).
2. If using accrual products: **Accruals** with **Accrue till** = last day of the month (or rely on the scheduler, then spot-check).
3. **Provisioning Entries** as at month-end; create journals if policy requires.
4. Post remaining manuals (accrued expenses, bank charges, depreciation if used).
5. Run **Trial Balance**, **Income Statement**, **Balance Sheet**, **General Ledger**.
6. Investigate unbalances, suspense, and unusual income.
7. **Closing Entries** for each office as at month-end.
8. Archive PDF statements.

### Year-end

- Same as month-end, with extra care on income/expense close to retained earnings if you post that manually.
- Do not “re-migrate opening balances” for a new year. Opening balances are a go-live tool.

---

## 7.3. Troubleshooting

| Symptom | Likely cause | What to do |
| --- | --- | --- |
| Loan disbursed but GL did not move | Product Accounting = **None**, or mappings blank | Edit the **product** (future loans). Existing loans follow the product they were created with. |
| “Accounting is closed as of …” | Office has a closing entry on or after that date | Delete the closure (authorised user), post, recreate closure |
| Cannot save opening balances | Journals already exist, or accounting-enabled products already have accounts, or contra mapping missing | Complete the contra mapping first; opening balances can only be loaded before any other journals exist |
| Debits ≠ credits on a manual journal | Amounts do not match; extra line not removed | Fix amounts; the server rejects unbalanced entries |
| Account missing from journal dropdown | Header account, disabled, or **Manual entries allowed** is off | Use a Detail account; enable manual entries if appropriate |
| Tile missing on Accounting home | Role lacks permission | Admin → Users → Roles; grant the codes in the appendix |
| Wrong bank account on EcoCash vs cash | Advanced **payment channel** mapping not set | Map payment types on the product |
| Accrual ran but no journals | Products are Cash, or accrue-till date has no unaccrued income | Check product rule; check loan schedule |
| Trial Balance off by currency | Mixed USD and ZWL in one mental total | Always filter by currency |
| Capitalised fee not in “Fees Charged” | Mopane posts **Capitalised fee**, not a cash fee | Inspect that transaction’s journals: Dr Portfolio, Cr Fee income |
| **Allocate / Settle Cash** button fails | **Cash at Main Vault** or **Cash at Teller** financial activity not mapped | Map both under Accounts Linked to Financial Activities |
| Teller allocation posted to the wrong cashier, no Reverse button | Teller journals are system entries, not manual | Post the opposite allocate/settle with a note; do not hand-journal around it |
| Accounting report errors or returns a blank page | Pentaho plugin or `.prpt` file missing on the server | Raise with IT — not a data problem |
| Journal saved but “pending approval”, GL unchanged | Maker-checker is enabled on that permission | A checker must approve it under Admin → System → Audit Trails |

---

## 7.4. Permissions (for administrators)

Grant via **Admin → Users → Roles**. Accounting tiles hide when the user lacks the read/create permission.

**Typical branch accountant**

- `READ_GLACCOUNT`, `READ_JOURNALENTRY`, `CREATE_JOURNALENTRY`, `REVERSE_JOURNALENTRY`
- `READ_ACCOUNTINGRULE`
- `READ_GLCLOSURE` (create/delete only if policy allows)
- Report reads: `READ_Trial Balance`, `READ_Income Statement`, `READ_Balance Sheet`, `READ_REPORT`

**Typical Head Office finance / GL admin** — all of the above, plus:

- `CREATE_GLACCOUNT`, `UPDATE_GLACCOUNT`, `DELETE_GLACCOUNT`
- `CREATE_ACCOUNTINGRULE`, `UPDATE_ACCOUNTINGRULE`, `DELETE_ACCOUNTINGRULE`
- `CREATE_GLCLOSURE`, `DELETE_GLCLOSURE`
- `CREATE_FINANCIALACTIVITYACCOUNT`, `READ_FINANCIALACTIVITYACCOUNT`
- `DEFINEOPENINGBALANCE_JOURNALENTRY`
- `EXECUTE_PERIODICACCRUALACCOUNTING`
- Provisioning: `CREATE_PROVISIONING_CRITERIA`, `VIEW_PROVISIONING_ENTRIES`, `CREATE_PROVISIONING_ENTRIES`
- `UPDATERUNNINGBALANCE_JOURNALENTRY` if they run the running-balance job

Maker-checker can be enabled on several of these codes. If a journal sits in **Tasks**, a checker must approve it before it hits the GL.

---

## 7.5. Maker-checker and the audit trail

### Maker-checker (four eyes)

**Path:** Admin → System → **Configure Maker Checker Tasks** (`#/makerchecker`)

Choose a grouping (for example Accounting or Portfolio) and tick the permissions that must be checked. Once a permission is under maker-checker:

1. The maker saves as normal and gets a *pending approval* result.
2. **Nothing reaches the GL yet.**
3. A checker reviews and **Approves** or **Rejects**.

Worth enabling at most institutions: `CREATE_JOURNALENTRY`, `REVERSE_JOURNALENTRY`, `DELETE_GLCLOSURE`, `DEFINEOPENINGBALANCE_JOURNALENTRY`.

The checker needs the matching checker permission, and no one can check their own entry. Remember this when an entry seems not to have saved: a pending journal has saved, it is simply waiting for its checker.

### Audit trails

**Path:** Admin → System → **Audit Trails** (`#/audit`)

Filter by user, action, entity, resource ID and date. Every accounting command — create journal, reverse, create or delete a closure, GL account changes — is recorded with the exact payload that was submitted. This is the screen that answers "who posted this, when, and what did they send".

---

# Part 8 — Glossary

| Term | Meaning in Mopane CBS |
| --- | --- |
| **GL / Chart of Accounts** | The list of Header and Detail accounts the books use |
| **Journal entry** | One balanced set of debit and credit lines sharing a transaction ID |
| **Transaction ID** | Identifier that groups all lines of one event (one disbursement, one manual, one reversal) |
| **Fund source** | Asset account representing the money that leaves or arrives (bank, cash, mobile money) |
| **Loan portfolio** | Asset account for outstanding principal |
| **Savings control** | Liability account for customer deposit balances |
| **Accounting rule (product)** | None / Cash / Accrual periodic / Accrual upfront on the product |
| **Accounting rule (posting template)** | Saved debit/credit pattern used by Frequent Postings |
| **Closure** | Freeze of an office’s books up to a date |
| **Accrual** | Recognising income or expense before cash moves |
| **Provisioning** | Booking expected loan losses from arrears buckets |
| **Running balance** | Balance stored on journal lines for reporting; updated by the running-balance job |
| **Manual entries allowed** | Whether users may pick this GL on Add Journal Entries |
| **Teller / Cashier** | Branch cash desk and the staff member holding the drawer; allocations and settlements post GL journals automatically |
| **Maker-checker** | Four-eyes control — the maker's entry waits for a checker before it reaches the GL |
| **Payment type** | The channel a transaction arrived on (cash, cheque, EcoCash), used for channel-specific fund-source mapping |
| **Capitalised fee** | A Mopane fee that increases loan principal: Dr Loan portfolio / Cr Income from fees, no cash movement |

---

# Appendix — Screen map (for system administrators)

| User-facing title | Route | Main permission |
| --- | --- | --- |
| Accounting home | `#/accounting` | (tiles gated individually) |
| Frequent Postings | `#/freqposting` | `CREATE_JOURNALENTRY` |
| Add Journal Entries | `#/journalentry` | `CREATE_JOURNALENTRY` |
| Search Journal Entries | `#/searchtransaction` | `READ_JOURNALENTRY` |
| View Transaction | `#/viewtransactions/:id` | `READ_JOURNALENTRY` / `REVERSE_JOURNALENTRY` |
| Accounts Linked to Financial Activities | `#/financialactivityaccountmappings` | `READ_FINANCIALACTIVITYACCOUNT` |
| Migrate Opening Balances | `#/openingbalances` | `DEFINEOPENINGBALANCE_JOURNALENTRY` |
| Chart of Accounts | `#/accounting_coa` | `READ_GLACCOUNT` |
| Create GL Account | `#/createglaccount` | `CREATE_GLACCOUNT` |
| Closing Entries | `#/accounts_closure` | `READ_GLCLOSURE` |
| Create Closure | `#/createclosure` | `CREATE_GLCLOSURE` |
| Accounting Rules | `#/accounting_rules` | `READ_ACCOUNTINGRULE` |
| Accruals | `#/run_periodic_accrual` | `EXECUTE_PERIODICACCRUALACCOUNTING` |
| Provisioning Entries | `#/viewprovisioningentries` | `VIEW_PROVISIONING_ENTRIES` |
| Provisioning Criteria | `#/viewallprovisionings` | Organisation menu |
| Accounting reports | `#/reports/accounting` | `READ_REPORT` |
| Bulk import COA | `#/bulkimportcoa` | Bulk import permissions |
| Bulk import journals | `#/bulkimportjournalentries` | Bulk import permissions |
| Tellers / Cashiers | `#/tellers` | `READ_TELLER` |
| Allocate / Settle cashier cash | `#/tellers/:tellerId/cashiers/:cashierId/actions/:action` | `ALLOCATECASHIER_TELLER` / `SETTLECASHFROMCASHIER_TELLER` |
| Cashier transactions | `#/tellers/:tellerId/cashiers/:cashierId/txns/:currencyCode` | `READ_TELLER` |
| Manage Payment Types | `#/viewpaymenttype/` | `READ_PAYMENTTYPE` |
| Configure Maker Checker Tasks | `#/makerchecker` | `READ_PERMISSION` / `PERMISSIONS_ROLE` |
| Audit Trails | `#/audit` | `READ_AUDIT` |
| Currency configuration | `#/currencyconfig` | `READ_CURRENCY` |

---

## References — the upstream manuals

Mopane CBS accounting behaviour follows Mifos X / Apache Fineract 17.07. Official user-manual pages that match these screens:

- [Accounting (setup overview)](https://docs.mifos.org/mifosx/user-manual/for-administrators-mifos-x-platform/accounting)
- [Chart of Accounts](https://docs.mifos.org/mifosx/user-manual/for-administrators-mifos-x-platform/accounting/chart-of-accounts-general-ledger-setup)
- [Opening balances](https://docs.mifos.org/mifosx/user-manual/for-administrators-mifos-x-platform/accounting/how-to-enter-gl-opening-balance)
- [Add Journal Entries](https://docs.mifos.org/mifosx/user-manual/for-operational-users-mifos-x-web-app/accounting-operations/add-journal-entries)
- [Frequent Postings](https://docs.mifos.org/mifosx/user-manual/for-operational-users-mifos-x-web-app/accounting-operations/frequent-postings)
- [Closing Entries](https://docs.mifos.org/mifosx/user-manual/for-operational-users-mifos-x-web-app/accounting-operations/closing-entries)
- [Accruals](https://docs.mifos.org/mifosx/user-manual/for-operational-users-mifos-x-web-app/accounting-operations/accruals)
- [Loan products — Accounting](https://docs.mifos.org/mifosx/user-manual/for-administrators-mifos-x-platform/administration/products/loan-products)
- [Accounts linked to Financial Activities](https://docs.mifos.org/mifosx/user-manual/for-administrators-mifos-x-platform/accounting/accounts-linked-to-financial-activities)
- [Accounting Rules](https://docs.mifos.org/mifosx/user-manual/for-administrators-mifos-x-platform/accounting/accounting-rules)
- [Accounting operations (web app)](https://docs.mifos.org/user-manual/for-operational-users-mifos-x-web-app/accounting-operations)

The upstream setup order is the same one used in Part 3: chart of accounts → map the opening-balance contra → migrate opening balances office by office → map products.

Where Mopane differs (capitalised fees, teller cash reports, office short codes, RBZ / SSB features), this guide takes precedence.


---

*Mopane CBS Accounting Module — User Handbook. Screen labels and posting rules reflect the Mopane CBS release named on the cover. If a screen in your system differs from this handbook, your system may be on a different release — check with your administrator.*
