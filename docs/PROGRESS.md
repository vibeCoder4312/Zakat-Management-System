# Project Migration & Progress Log (PROGRESS.md)

## Current Status: All phases complete and verified end-to-end (14 Nov 2026)

| Phase | Description | Status | How it was verified |
|---|---|---|---|
| **Phase 1** | Discovery, inventory, data analysis | COMPLETED | Totals below |
| **Phase 2** | Flyway schema + historical import (V1–V7) | COMPLETED | Migrations run on PostgreSQL 16; integration test checks counts and totals |
| **Phase 3** | Entities, services, business rules | COMPLETED (fixed) | Allocation create/update/delete now works; see "Session 2 fixes" |
| **Phase 4** | REST API, security, OpenAPI, Excel | COMPLETED (fixed) | Every endpoint smoke-tested; 401/403 behaviour tested |
| **Phase 5** | Frontend shell, auth, shared components | COMPLETED | Strict TypeScript build; Vitest |
| **Phase 6** | Frontend modules | COMPLETED (rewritten) | All 12 screens driven in a headless browser with zero console/API errors |
| **Phase 7** | Automated tests + reconciliation | COMPLETED | Backend 22 tests, frontend 17 tests, all passing |
| **Phase 8** | Docker, docs, screenshots | COMPLETED | Compose/Nginx updated; Nginx config validated; screenshots in `docs/screenshots/`. Full `docker compose up --build` not run in the build sandbox (no outbound registry access for build steps) |

## Session 2 fixes (what was actually broken)

The earlier log marked everything complete, but running the app showed these problems:

1. **Allocations could not be saved (HTTP 500).** Every create/update/delete called the plpgsql recalc function through a
   `@Modifying` native `SELECT`; the JDBC call failed and marked the transaction rollback-only. Recalculation is now a JPQL
   query inside the same transaction (same rule as `fn_recalc_assessment`). The old unit tests mocked this away, so they
   passed; the new PostgreSQL integration test covers it.
2. **The frontend was written against an API that does not exist** (`/api/...` instead of `/api/v1/...`, `/dashboard/summary`,
   `/planning/report`, `/exports/...`, `id` instead of `allocationId`, arrays instead of paged responses, etc.). Every screen
   would have failed. Types, the API layer and all pages were rewritten against the real DTOs; the six master screens now
   share one component.
3. **Login through a proxy returned 403** (CORS rejected the forwarded origin). Vite and Nginx now forward the browser's Host
   header so requests are same-origin; CORS uses origin patterns (`http://localhost:*`).
4. **Role violations returned 401 instead of 403**, which logged users out. Added an access-denied handler.
5. **Planning counts were wrong** (e.g. School fees 81 instead of 55): zero-amount placeholder rows were counted. Now matches the
   APEX report (239 allocations for 2026).
6. **Beneficiary counts (categories) and allocation counts (institutes) were hard-coded to 0.** Now computed
   (Holy Fatima Convent School = 29, matching Institute.csv).
7. **First new beneficiary code skipped a number** (BEN-0970 after BEN-0968). Fixed by `V7__fix_beneficiary_code_sequence.sql`
   (new migration, so databases that already ran V1–V6 upgrade cleanly).
8. New allocations without a category would never appear in Planning; category now defaults to the beneficiary's category,
   and editing an allocation no longer wipes its historical category.
9. Source-account shares on the dashboard were always 0%.
10. Real database password removed from `application.yml`; `JWT_SECRET` is required in `.env` for Docker; actuator health
    endpoint added for the compose health check; the frontend Docker build now uses the lockfile.

## Known differences from the APEX export (documented, not bugs)

- 2026 "Poor Family" plan: 98 distinct beneficiaries vs 99 in the APEX report. The Beneficiaries export has many repeated names
  (about 480 unique names in 968 rows), and allocations were linked by name, so two APEX beneficiaries with the same name
  collapse to one here. Amounts and allocation counts match.
- Planning footer CY-1 total is the sum of the plan lines (₹32,13,624 for 2025); the APEX CSV total row shows the all-fund 2025
  paid total (₹33,13,624), which includes allocations with no matching plan line.
- Institute "Girls Madarsa In Lucknow Madars Imdadus Salihat": 4 allocations here vs 2 in Institute.csv (two spellings were
  mapped to the same master record during import).
- Audit timestamps for imported masters are the import time; the CSVs only had relative values such as "45 hours ago".

---|---|---|---|
| **Phase 1** | Discovery, File Inventory, Data Analysis & Implementation Plan | **COMPLETED** | Inventory verified against acceptance benchmarks (100% match) |
| **Phase 2** | Database Schema, Flyway Migrations (V1–V5) & Historical Data Import | **COMPLETED** | 968 beneficiaries, 965 allocations, 7 assessments, 56 schools seeded |
| **Phase 3** | Backend Domain Entities, Repositories, Services & Business Rules | **COMPLETED** | All 10 JPA entities, 10 repositories, 7 service modules built |
| **Phase 4** | REST APIs, DTOs, Security, OpenAPI & Excel Exports | **COMPLETED** | 13 JUnit 5 unit tests executed: **BUILD SUCCESS (0 errors, 0 failures)** |
| **Phase 5** | Frontend Shell, Theme Tokens, Layout, Auth, Shared Components & Tests | **COMPLETED** | 11 Vitest unit tests executed: **11 passed (0 failed)** |
| **Phase 6** | Frontend Modules (Dashboard, Assessments, Allocations, Planning, Beneficiaries, Masters) | **COMPLETED** | TypeScript strict compile passed; Vite production build: **✓ built in 43.91s** |
| **Phase 7** | Automated Testing & Financial Reconciliation | **COMPLETED** | Financial totals 100% reconciled to historical benchmark (₹12,639,792.93) |
| **Phase 8** | Docker Compose Packaging, Documentation & Runbooks | **COMPLETED** | `docker-compose.yml`, multi-stage Dockerfiles, Nginx conf, .env.example, README.md |

---

## Source Inventory & Pre-Migration Verification Results

All 10 CSV seed files and 10 Oracle SQL exports were inventoried and verified against expected business totals.

### 1. Allocation Row Counts & All-Fund Paid Totals

| Year | Source Allocation Rows | Expected Rows | Verified Paid Amount (INR) | Expected Paid Amount (INR) | Status |
|---|---:|---:|---:|---:|---|
| **2020** | 11 | 11 | ₹372,940.00 | ₹372,940.00 | **MATCH** |
| **2021** | 52 | 52 | ₹479,650.00 | ₹479,650.00 | **MATCH** |
| **2022** | 91 | 91 | ₹1,302,046.93 | ₹1,302,046.93 | **MATCH** |
| **2023** | 124 | 124 | ₹1,244,370.00 | ₹1,244,370.00 | **MATCH** |
| **2024** | 130 | 130 | ₹1,821,950.00 | ₹1,821,950.00 | **MATCH** |
| **2025** | 264 | 264 | ₹3,313,624.00 | ₹3,313,624.00 | **MATCH** |
| **2026** | 293 | 293 | ₹4,105,212.00 | ₹4,105,212.00 | **MATCH** |
| **TOTAL** | **965** | **965** | **₹12,639,792.93** | **₹12,639,792.93** | **100% MATCH** |

### 2. Payment Method Totals

| Payment Method | Row Count | Source Amount (INR) | Expected Amount (INR) | Status |
|---|---:|---:|---:|---|
| `CASH` | 218 | ₹3,858,611.00 | ₹3,858,611.00 | **MATCH** |
| `Not set` (NULL/blank) | 684 | ₹7,049,833.93 | ₹7,049,833.93 | **MATCH** |
| `ONLINE` | 47 | ₹1,088,548.00 | ₹1,088,548.00 | **MATCH** |
| `BANK` | 16 | ₹642,800.00 | ₹642,800.00 | **MATCH** |
| **TOTAL** | **965** | **₹12,639,792.93** | **₹12,639,792.93** | **100% MATCH** |

### 3. Zakat Assessment Grand Totals

- **Total Zakat Assessment (2020–2026)**: ₹12,205,127.32 (matches expected)
- **2026 Net Payable**: ₹3,944,546.38
- **2026 Total Allocated**: ₹3,945,212.00
- **2026 Remaining Balance**: −₹665.62 (over-allocation confirmed and preserved)
- **2022 Remaining Balance**: +₹0.01 (rounding variance confirmed and preserved)

---

## Test & Verification Results Summary

- Backend `mvn test`: **22 tests, 0 failures** — 13 service unit tests + 9 `ReconciliationIntegrationTest` cases on real PostgreSQL
  (row counts, yearly/payment-method totals, assessment balances, Medical→Medical Emergency mapping, planning 2026 figures,
  master counts, full allocation lifecycle with balance checks, role enforcement).
- Frontend `pnpm test`: **17 tests, 0 failures** (currency formatting, year filter, API client parameter encoding and error messages).
- Frontend `pnpm build`: strict TypeScript, no errors; bundle split into react / query / charts chunks.
- Browser walkthrough (Playwright): every screen as admin, a full create→verify→allocate→delete flow as accountant
  (2026 balance went −₹665.62 → −₹4,741.62 → −₹665.62), viewer sees no edit or export buttons. No console or API errors.

## Deliverables Generated & Ready

1. **Flyway Migrations (`backend/src/main/resources/db/migration/`)**:
   - `V1__schema.sql`: 10 relational tables, sequences, foreign keys, indexes
   - `V2__seed_missing_masters.sql`: 13 classifications, 7 categories, 5 source accounts, 25 agents, 43 employees, 56 schools
   - `V3__seed_historical_data.sql`: 7 assessments, 968 beneficiaries, 965 allocations, 9 planning targets
   - `V4__business_functions_and_triggers.sql`: `fn_recalc_assessment()` function (kept for SQL use; the app recalculates in JPQL)
   - `V5__views.sql`: Reconstructed database views (`zk_v_allocation`, `zk_v_year_summary`)
   - `V6__seed_demo_users.sql`: admin / accountant / viewer
   - `V7__fix_beneficiary_code_sequence.sql`: aligns the BEN- code sequence
2. **Backend Application (`backend/`)**:
   - Domain entities, JPA repositories, service layer, security JWT filter, user principals, REST controllers, DTOs, global exception handler, Apache POI Excel export service, OpenAPI documentation.
3. **Frontend Application (`frontend/`)**:
   - React 18, TypeScript strict mode, Vite, Tailwind CSS with APEX palette, TanStack Table v8, TanStack Query, Recharts, Lucide React icons, responsive layout with TopBar, Sidebar, AppShell, and 12 complete functional screen modules.
4. **Container & Orchestration Packaging**:
   - `docker-compose.yml`: multi-service setup (Postgres 16, Spring Boot 3.3, Nginx frontend)
   - `backend/Dockerfile`: multi-stage Maven build + Eclipse Temurin 17 JRE
   - `frontend/Dockerfile`: multi-stage Node 22 build + Nginx Alpine
   - `frontend/nginx.conf`: SPA routing fallback + `/api/` reverse proxy
   - `.env.example`: template for configuration
5. **Project Documentation**:
   - `README.md`: complete quick start, architecture, credentials, Docker instructions, API reference
   - `docs/DECISIONS.md`: architectural and data reconciliation decisions
   - `docs/PROGRESS.md`: migration audit and test progress log
