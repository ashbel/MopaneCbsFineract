# Quotation: RBZ-Compliant Core Banking System for Credit-Only Microfinance Institutions

**Submitted by:** Mopane CBS
**Product:** Mopane CBS (Core Banking System), built on Apache Fineract
**Date:** 14 August 2026
**Quotation validity:** 90 days from date of issue
**Prepared for:** [Client institution name]

> This response follows the headings requested in your invitation to quote. Fields marked **[TBC]** are commercial or client-specific inputs to be completed by Mopane CBS before submission — pricing, references, and support-package terms are deliberately left blank here rather than estimated, so the final figures you submit are accurate.

---

## 1. Executive Summary

Mopane CBS is a core banking platform for microfinance institutions, built on Apache Fineract — a mature, widely deployed open-source core banking engine used by MFIs, credit-only lenders, and financial-inclusion programmes globally. Mopane CBS packages Fineract with a modern web front end, Zimbabwe-specific configuration, hosting, implementation, and support, delivered as a managed service to local institutions.

Because the core platform is open-source (Apache License 2.0), there is no per-seat core software licence fee — your costs are for implementation, hosting/infrastructure, configuration, customisation, and ongoing support, which we detail in Section 3.

---

## 2. Regulatory Compliance — Reserve Bank of Zimbabwe Requirements

### 2.1 Licensing
Mopane CBS does not itself hold an RBZ microfinance licence — licensing is obtained by the institution, not the software vendor. The system supports the licensing process by producing the client, loan, and financial records typically required for RBZ Microfinance Unit registration and ongoing compliance reviews (audit trail, chart of accounts, loan books, provisioning schedules). **[TBC — confirm any specific RBZ licence-condition documentation the client needs generated from the system.]**

### 2.2 Regulatory reporting
The platform includes a built-in reporting engine (Pentaho/BIRT-based SQL reporting) covering the full transactional database — clients, loans, savings, GL, and audit data. Standard reports (portfolio-at-risk, loan status, disbursement/collection, trial balance, client listings, etc.) are available out of the box.

RBZ- and Zimbabwe-specific regulatory returns (e.g. Microfinance Unit prudential returns, credit registry/Credit Reference Bureau submissions) are **not pre-built** in the base platform — we have not identified an existing RBZ report pack in this codebase. These are delivered as a **custom report development scope item** during implementation: because all underlying data is captured and queryable, building the exact RBZ return formats is a configuration/report-writing exercise, not a system limitation. We ask the client to supply current RBZ return templates so these can be quoted precisely and built during implementation.

Reports export to PDF, Excel (XLSX), CSV, and HTML. Reports run on-demand through the admin console and reporting API; scheduled/automatic generation and email delivery of specific returns can be configured as part of implementation.

### 2.3 Audit trail
Every create/update/delete action in the system is logged as a discrete, immutable audit entry: acting user, timestamp, action, affected entity, and the full request payload (enabling before/after reconstruction). Audit entries are searchable and filterable by user, date range, action type, and entity from the admin console, and are exportable for regulator or internal-audit review.

The platform supports **maker–checker (four-eyes)** workflow configuration on sensitive transaction types (e.g. loan approval, disbursement, write-off), so a second authorised user must review and approve actions flagged for dual control — a control RBZ examiners typically look for.

### 2.4 Data security
- All access is over HTTPS/TLS (TLS termination configured at the reverse proxy in our reference deployment).
- **Role-based access control (RBAC):** granular permissions per module and action, assignable per staff role/office.
- **Maker–checker** controls (2.3 above) for sensitive transactions.
- **Multi-tenant data isolation:** each institution's data is held in a separate tenant database, logically (and, if required, physically) isolated from other clients on shared infrastructure.
- Configurable password policy and session timeout.
- **[TBC]** Formal certifications (e.g. ISO 27001) or third-party penetration test reports — none are currently held for this deployment; can be scoped as an add-on (independent security audit) if required for the client's due diligence.

### 2.5 Customer records (KYC/CDD)
The client module captures standard KYC data (identity, contact, address, employment, next-of-kin/guardian for groups), supports multiple ID document types and numbers, image/document upload and storage against a client file, and **datatables** — a no-code mechanism to add institution-specific or regulator-mandated custom fields (e.g. additional CDD questions, source-of-funds, risk classification) without code changes. Duplicate-client detection is supported via configurable identifier matching.

### 2.6 Loan administration
Full loan lifecycle support: product configuration, application, appraisal/approval workflow, disbursement, automatic repayment-schedule generation, multiple interest calculation methods and repayment frequencies, restructuring, write-off, and provisioning by days-past-due (NPL) buckets. Group and center-based lending (common in credit-only microfinance) is natively supported, alongside individual lending. Loan officer portfolio assignment and tracking is built in.

### 2.7 Anti-money laundering (AML)
The platform provides the building blocks for an AML/CDD programme:
- Custom AML/CDD data capture via datatables (risk rating, source-of-funds notes, screening outcomes).
- Configurable transaction/loan-size limits per product.
- Full audit trail of all client and transaction activity for suspicious-activity investigation.

**What is not built in:** automated screening against sanctions/PEP watchlists (UN, OFAC, or a Zimbabwe-specific list) is **not a native feature**. This would be delivered as an integration to a third-party AML/watchlist screening API during implementation, and should be scoped explicitly if the client requires automated screening rather than manual/procedural AML controls. We flag this as a dependency rather than claim out-of-box coverage.

### 2.8 Interest and fee calculations
Interest and fees are fully configurable per loan product: flat or declining-balance methods, calculation on disbursed or outstanding balance, in-advance or in-arrears, multiple compounding/repayment frequencies, and a fee/charge engine supporting flat or percentage-based fees applied upfront, periodically, or as penalties. This configurability supports transparent, disclosed pricing consistent with RBZ conduct requirements — the specific rates/caps applied are a product-configuration decision made by the institution, not a system constraint.

### 2.9 Summary compliance position
Mopane CBS provides the technical controls (audit trail, RBAC, maker-checker, data isolation, configurable products, reporting engine) that underpin RBZ compliance. RBZ-specific report templates and AML watchlist screening are **implementation/customisation items**, not out-of-box features — we have been explicit about this rather than overstating readiness.

---

## 3. Pricing Schedule

*(All figures **[TBC]** — to be completed with current Mopane CBS commercial rates before submission. Currency: USD unless otherwise agreed.)*

### 3.1 Once-off costs

| Line item | Description | Amount (USD) |
|---|---|---|
| Core platform licence fee | Apache Fineract core is open-source (no licence fee); Mopane CBS packaging/branding fee if applicable | **[TBC]** |
| Implementation & project management | End-to-end delivery: discovery, configuration, PM | **[TBC]** |
| Configuration | Loan/savings products, chart of accounts, offices, roles | **[TBC]** |
| Data migration | Migration from existing system(s) — scope-dependent | **[TBC]** |
| Custom RBZ report development | Regulator-specific return formats (Section 2.2) | **[TBC]** |
| AML/watchlist screening integration | If required (Section 2.7) | **[TBC]** |
| Training | Initial user, admin, and IT training (Section 8) | **[TBC]** |
| Initial setup/customisation | Any bespoke workflow or integration build | **[TBC]** |
| **Total once-off** | | **[TBC]** |

### 3.2 Recurring costs

| Line item | Frequency | Amount (USD) |
|---|---|---|
| Hosting/infrastructure | Monthly | **[TBC]** |
| Platform maintenance & upgrades | Monthly/Annual | **[TBC]** |
| Support subscription | Monthly/Annual (see Section 9 for tiers) | **[TBC]** |
| Compliance/regulatory reporting maintenance | Annual | **[TBC]** |
| User access subscription (if licensed per-seat rather than flat) | Monthly | **[TBC]** |
| **Total recurring** | | **[TBC]** |

### 3.3 Usage-related charges

| Line item | Basis | Amount (USD) |
|---|---|---|
| Transaction fee | Per transaction (if applicable) | **[TBC]** |
| Per-user fee | Per active user/month | **[TBC]** |
| Per-branch fee | Per branch/office/month | **[TBC]** |
| SMS/notification cost | Per SMS sent (pass-through gateway cost + margin) | **[TBC]** |
| Integration charges | Per integration (accounting, payment gateway, etc.) | **[TBC]** |
| Storage charges | Per GB beyond included allowance | **[TBC]** |
| Report charges | Per custom/ad-hoc report build | **[TBC]** |
| Other variable costs | **[TBC — specify]** | **[TBC]** |

---

## 4. Client References

*(To be completed with real or anonymised references before submission — none fabricated here.)*

| # | Institution type | Implementation size (branches/users/clients) | Duration of use | Scope of services provided |
|---|---|---|---|---|
| 1 | **[TBC]** | **[TBC]** | **[TBC]** | **[TBC]** |
| 2 | **[TBC]** | **[TBC]** | **[TBC]** | **[TBC]** |
| 3 | **[TBC]** | **[TBC]** | **[TBC]** | **[TBC]** |
| 4 | **[TBC]** | **[TBC]** | **[TBC]** | **[TBC]** |
| 5 | **[TBC]** | **[TBC]** | **[TBC]** | **[TBC]** |

Our current production reference deployment is Mopane CBS itself (fineract.mopane.co.zw), operated as a live Fineract-based core banking instance. **[TBC — add detail/permission from the institution before naming it as a reference.]**

---

## 5. System Requirements

### 5.1 Software/operating environment
- **Application server:** Apache Tomcat (8.5+); Java runtime (current production stack: Java 8 — a modernised Java 17 / current Fineract stack is available and recommended for new deployments).
- **Database:** MySQL (current stack: 5.5 for legacy compatibility; MySQL 8 or PostgreSQL supported on the modern stack).
- **Front end:** Mopane CBS React web application (modern browsers — current two major versions of Chrome, Edge, Firefox) plus REST API for any additional/mobile clients.
- **Containerisation:** Docker/Docker Compose used for deployment and environment reproducibility.

### 5.2 Hardware — minimum vs recommended
| Component | Minimum (pilot/small MFI) | Recommended (production) |
|---|---|---|
| App server | 2 vCPU / 4 GB RAM | 4–8 vCPU / 16 GB RAM |
| Database server | 2 vCPU / 4 GB RAM | 4 vCPU / 16 GB RAM, SSD storage |
| Storage | 40 GB SSD | 200 GB+ SSD with growth headroom, backed by automated backup |

Sizing scales with client/loan volume, concurrent users, and reporting load; final sizing is confirmed during discovery based on the client's expected transaction volumes.

### 5.3 Connectivity
- Standard broadband internet connectivity (HTTPS/TLS 1.2+); the system is usable over 3G/4G for branch access, though a stable fixed-line/fibre connection is recommended for high-volume branches.
- No dedicated leased-line requirement for cloud/hosted deployment.

### 5.4 Processing speed, user and branch capacity
Apache Fineract (the underlying engine) is used by institutions ranging from small pilots to large-scale deployments serving hundreds of thousands of clients, with capacity determined by infrastructure sizing rather than a hard platform ceiling. End-of-day batch processing (interest posting, overdue/penalty calculation, provisioning) runs via the platform's internal job scheduler. **[TBC — specific throughput/benchmark figures for the proposed hardware tier, to be confirmed during technical due diligence/sizing.]**

### 5.5 Database requirements
Relational database (MySQL/PostgreSQL), one logical tenant database per institution for data isolation, with standard relational backup/restore tooling.

### 5.6 Cybersecurity requirements
TLS-terminated access, RBAC, maker-checker controls, full audit logging, configurable password policy (see Section 2.4). **[TBC — any client-mandated controls such as MFA, IP allow-listing, or VPN-only access should be specified; these can be added as implementation items.]**

### 5.7 Third-party software/licences
The core platform (Apache Fineract) and its standard dependencies are open-source (Apache 2.0/similar licences) — no proprietary core licence is required. Optional paid third-party services depend on client requirements, e.g.:
- Commercial SMS gateway subscription (for client notifications).
- AML/sanctions-screening API subscription, if automated screening is required (Section 2.7).
- TLS certificate (can be free via Let's Encrypt or client-procured).

---

## 6. Integrations

- **Accounting:** Fineract includes a built-in general ledger and chart-of-accounts engine — journal entries are generated automatically from client transactions, so the platform can serve as the institution's accounting system of record. For institutions using a separate accounting package (e.g. Pastel, Sage, QuickBooks), data can be exported (CSV/API) or a direct integration built as a scoped customisation. **[TBC — confirm client's accounting package for integration scoping.]**
- **Payments/mobile money:** Zimbabwe mobile money integrations (e.g. EcoCash, OneMoney) are not built in by default and would be delivered as a scoped integration against the platform's payment gateway hooks/API.
- **SMS/Email:** Native campaign module for client notifications (loan reminders, disbursement/repayment confirmations), configurable against a chosen SMS/email gateway provider.
- **General integration mechanism:** REST API (JSON) covering all core entities (clients, loans, savings, accounting, users), suitable for integration with credit bureaux, national ID verification services, or other regulator systems as required.

---

## 7. Reporting Capabilities

- **Scope:** All transactional and reference data — clients, loans, savings, GL, staff, and audit trail.
- **Format:** PDF, Excel (XLSX), CSV, HTML.
- **Customisation:** SQL-based report builder for bespoke reports (including RBZ-specific formats — Section 2.2); no code deployment required to add a new report.
- **Scheduling:** Reports run on-demand by default; scheduled generation and delivery (e.g. automated monthly regulatory export) is an implementation configuration item.
- **Dashboards:** Standard operational dashboards (portfolio, disbursements, collections, PAR); additional dashboard views can be configured as part of implementation.
- **Regulatory reporting:** Supported via custom report development against the underlying data, as described in Section 2.2 — templates to be supplied by the client's compliance team for exact-format development.

---

## 8. Backup and Disaster Recovery

Current production practice on the reference deployment uses containerised database volumes on hosted infrastructure. For a client engagement, we propose the following as a configurable, contractable service level — **figures below are proposed defaults, to be agreed and stated in the SLA, not existing guarantees**:

| Item | Proposed default | Notes |
|---|---|---|
| Backup frequency | Daily automated database backup | Configurable to more frequent if required |
| Backup location | Offsite/separate storage from production host | Encrypted at rest |
| Recovery Time Objective (RTO) | **[TBC — to be agreed]** | Depends on hosting tier selected |
| Recovery Point Objective (RPO) | **[TBC — to be agreed, target ≤24h with daily backups]** | Lower RPO available with more frequent backup, at additional cost |
| Data retention period | **[TBC]** | To align with RBZ record-retention requirements |
| Backup encryption | Yes (encrypted at rest and in transit) | |
| Restoration testing | **[TBC — proposed quarterly restore test, to be committed in SLA]** | |
| Redundancy/failover | **[TBC — single-server by default; multi-server/HA available as a premium hosting tier]** | |
| Included vs. charged separately | **[TBC — confirm whether backup/DR is bundled in hosting fee or a separate line item in Section 3]** | |

We recommend this section be finalised jointly with the client's IT/compliance team so RTO/RPO/retention figures meet RBZ record-keeping obligations before being committed contractually.

---

## 9. Support Model

*(Structure provided; specific hours/SLA figures and package pricing **[TBC]**.)*

| Item | Detail |
|---|---|
| Helpdesk availability | **[TBC — e.g. business hours / extended hours]** |
| Support hours | **[TBC]** |
| Emergency/critical support | **[TBC — e.g. after-hours escalation for system-down incidents]** |
| Escalation procedure | **[TBC — tiered escalation path to be defined]** |
| Service Level Agreement | **[TBC — response/resolution targets by severity, see below]** |
| Implementation support | Included in implementation scope (Section 3.1) |
| Training support | Initial training included; refresher/ongoing training **[TBC — package or per-session]** |
| On-site support | **[TBC — availability and cost, Zimbabwe-based]** |
| Remote support | Standard channel (remote/ticketing) |

**Proposed severity-based response/resolution targets (to be confirmed and contracted):**

| Severity | Example | Target response | Target resolution |
|---|---|---|---|
| Critical (system down) | Core system unavailable | **[TBC]** | **[TBC]** |
| High | Major function impaired | **[TBC]** | **[TBC]** |
| Medium | Non-critical defect | **[TBC]** | **[TBC]** |
| Low | Query/enhancement request | **[TBC]** | **[TBC]** |

**Support package cost:** **[TBC — to align with Section 3.2 recurring costs.]**

---

## 10. Strengths, Limitations, Risks, and Dependencies

### Strengths
- Built on Apache Fineract — a mature, actively used open-source core banking engine with broad functional coverage for microfinance (individual and group lending, savings, GL/accounting, audit, RBAC).
- No proprietary core-platform licence fee, reducing long-term cost of ownership versus closed-source vendors.
- Configurable product engine (interest, fees, schedules) without code changes for most changes.
- Full audit trail and maker-checker controls suited to regulated environments.
- Local (Zimbabwe-based) implementation and support team, reducing timezone/logistics friction versus offshore vendors.

### Known limitations
- No pre-built RBZ-specific regulatory report pack in the current codebase — must be developed during implementation (Section 2.2/7).
- No built-in AML/sanctions watchlist screening — requires third-party integration if automated screening is required (Section 2.7).
- No formal third-party security certification (e.g. ISO 27001) currently held (Section 2.4).
- Current reference production stack runs on a legacy Java 8/MySQL 5.5 configuration for backward compatibility; a modernised stack is available and would be the default for a new client deployment.

### Implementation risks
- Data migration quality from the client's existing system (if any) is typically the highest-risk implementation item — requires clean source data and a defined migration/reconciliation window.
- Regulatory report accuracy depends on the client supplying current RBZ return templates early in implementation.
- AML/CDD process design (manual vs automated screening) needs to be agreed with the client's compliance function before go-live.

### Dependencies
- Client-supplied RBZ report templates and any AML watchlist provider account (if automated screening is required).
- Client-side network/connectivity meeting the requirements in Section 5.3.
- Timely client sign-off at key implementation stages (see Section 11 timeline).

### Areas likely requiring customisation for this client
- RBZ/Microfinance Unit regulatory return formats.
- AML/CDD workflow and (optionally) watchlist screening integration.
- Chart of accounts and loan product configuration to match the client's existing product set.
- Any accounting-package or mobile-money integration required (Section 6).

---

## 11. Implementation Timeline

**[TBC — indicative; to be confirmed against the client's final scope.]**

| Phase | Indicative duration |
|---|---|
| Discovery & requirements confirmation | **[TBC]** |
| Configuration (products, COA, offices, roles) | **[TBC]** |
| Data migration (if applicable) | **[TBC]** |
| Custom regulatory report development | **[TBC]** |
| User acceptance testing | **[TBC]** |
| Training | **[TBC]** |
| Go-live & hypercare | **[TBC]** |

---

## 12. Exclusions and Assumptions

- Pricing excludes third-party subscription costs not detailed above (e.g. SMS gateway usage charged by the gateway provider, unless bundled) — see Section 3.3.
- AML watchlist screening, mobile money/payment gateway integration, and accounting-package integration are scoped items, not included by default (Section 6, 2.7).
- RBZ-specific regulatory report formats will be built against templates supplied by the client; delays in supplying templates may affect the timeline in Section 11.
- This quotation assumes standard cloud/VPS hosting; on-premises hosting at the client's site would be quoted separately if required.
- Backup/DR figures in Section 8 are proposed defaults pending agreement, not yet contracted SLA terms.

---

## 13. Quotation Validity

This quotation is valid for **90 days** from the date of issue above. Pricing and delivery timelines are subject to confirmation of final scope following discovery.

---

*Prepared by Mopane CBS. Contact: **[TBC]***
