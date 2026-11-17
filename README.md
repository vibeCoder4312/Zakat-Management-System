# Zakat & Charity Management System

Replacement for Oracle APEX App 151 (schema `ZAKAT`): Spring Boot 3.3 (Java 17) + PostgreSQL 16 + React 18 (TypeScript, Vite, Tailwind).
It tracks yearly Zakat assessments, allocations and payments to beneficiaries, planning against prior years, and Sadaqah kept separate from the Zakat obligation.
All historical data from 2020–2026 (965 allocations, ₹1,26,39,792.93 paid) is imported by Flyway on first start.

---

## 1. Quick start (Docker)

```bash
cp .env.example .env
# edit .env: set JWT_SECRET (e.g. `openssl rand -hex 32`) and a real POSTGRES_PASSWORD
docker compose up --build
```

| What | URL |
|---|---|
| Web app | http://localhost:3000 |
| REST API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |

Nginx in the frontend container serves the React build and proxies `/api/` to the backend, so the browser only ever talks to port 3000.

## 2. Local development (without Docker)

Prerequisites: JDK 17+, Maven 3.9+, Node 20+ with pnpm 10, PostgreSQL 16.

```sql
CREATE DATABASE zakat;
```

```bash
# Backend (port 8080). Flyway creates the schema and imports all history on first run.
cd backend
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/zakat \
SPRING_DATASOURCE_USERNAME=postgres SPRING_DATASOURCE_PASSWORD=<your password> \
mvn spring-boot:run
```

On Windows PowerShell set the variables with `$env:SPRING_DATASOURCE_PASSWORD="..."` first.

```bash
# Frontend (port 3000, proxies /api to localhost:8080)
cd frontend
pnpm install
pnpm dev
```

If the backend runs on another port: `VITE_PROXY_TARGET=http://localhost:8091 pnpm dev`.

> **Timezone note:** some JVMs report `Asia/Calcutta`, which newer PostgreSQL builds reject at connect time
> (`invalid value for parameter "TimeZone"`). If you see that, start Java with `-Duser.timezone=Asia/Kolkata`
> (already set in the Dockerfile and for `mvn test`).

## 3. Users and roles

Seeded by migration `V6` (BCrypt hashes). **Change these passwords before real use.**

| Username | Password | Role | Can do |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | Everything: assessments, planning, masters, beneficiaries, allocations, exports |
| `accountant` | `account123` | ACCOUNTANT | Beneficiaries, allocations, receipts, Excel exports; read everything else |
| `viewer` | `viewer123` | VIEWER | Read-only |

The API returns **401** when the token is missing/expired and **403** when the role is not allowed. The UI hides actions the role cannot perform.

## 4. Business rules (as implemented)

- **Net payable** = Zakat amount − carry forward.
- **Total allocated** (per year) = sum over **Zakat-fund** allocations of: paid amount when PAID, planned amount when PLANNED, 0 when CANCELLED. Sadaqah classifications are excluded. Recalculated in the same transaction whenever an allocation is created, edited or deleted.
- **Remaining balance** = net payable − total allocated. Negative values are kept (2026: −₹665.62).
- Allocations can only be made to beneficiaries that are **Verified and Active**, and not in a **CLOSED** year.
- Entering a paid amount marks the allocation PAID and APPROVED. A paid allocation cannot be cancelled until the paid amount is cleared.
- AED payments: paid amount (INR) = foreign amount × FX rate.
- An allocation's category defaults to the beneficiary's category. **Planning** lines match allocations on year + classification + category; paid totals and counts are live, and CY-1…CY-5 show the same line's paid totals in earlier years. Zero-amount placeholder rows are not counted.
- Blank historical `paid_on` / `payment_method` stay NULL ("Not set") — nothing is invented.

## 5. Data reconciliation (checked by an automated test)

| Year | Rows | Zakat assessment | Total allocated (Zakat fund) | Paid, all funds | Remaining |
|---|---:|---:|---:|---:|---:|
| 2020 | 11 | ₹3,72,940.00 | ₹3,72,940.00 | ₹3,72,940.00 | ₹0.00 |
| 2021 | 52 | ₹3,78,250.00 | ₹3,78,250.00 | ₹4,79,650.00 | ₹0.00 |
| 2022 | 91 | ₹12,47,246.94 | ₹12,47,246.93 | ₹13,02,046.93 | ₹0.01 |
| 2023 | 124 | ₹12,12,970.00 | ₹12,12,970.00 | ₹12,44,370.00 | ₹0.00 |
| 2024 | 130 | ₹17,95,550.00 | ₹17,95,550.00 | ₹18,21,950.00 | ₹0.00 |
| 2025 | 264 | ₹32,53,624.00 | ₹32,53,624.00 | ₹33,13,624.00 | ₹0.00 |
| 2026 | 293 | ₹39,44,546.38 | ₹39,45,212.00 | ₹41,05,212.00 | −₹665.62 |
| **Total** | **965** | **₹1,22,05,127.32** | **₹1,22,05,792.93** | **₹1,26,39,792.93** | **−₹665.61** |

Payment methods: CASH 218 rows / ₹38,58,611 · ONLINE 47 / ₹10,88,548 · BANK 16 / ₹6,42,800 · Not set 684 / ₹70,49,833.93.

## 6. Tests

```bash
cd backend && mvn test        # unit tests + PostgreSQL integration test
cd frontend && pnpm test      # Vitest
cd frontend && pnpm build     # strict TypeScript + production bundle
```

`ReconciliationIntegrationTest` runs every migration against a real PostgreSQL, checks the totals above, then drives the API
(create beneficiary → verify → allocate → pay → delete, checking the year's balance at each step, plus role checks). It uses:

1. `ZAKAT_IT_JDBC_URL` (+ `ZAKAT_IT_DB_USER`, `ZAKAT_IT_DB_PASSWORD`) pointing at an **empty** database, or
2. a throwaway Postgres container via Testcontainers when Docker is running.

With neither available it is skipped, not failed.

## 7. Project layout

```
backend/   Spring Boot API
  src/main/resources/db/migration   V1 schema · V2 masters · V3 history · V4 recalc function · V5 views · V6 users · V7 code sequence fix
frontend/  React app (src/api = typed API client, src/pages = screens, src/pages/masters = shared master screen)
docs/      Source CSV/SQL exports, Excel workbook, DECISIONS.md, PROGRESS.md, screenshots/
```

## 8. API overview

All under `/api/v1`, JSON, `Authorization: Bearer <token>` (from `POST /auth/login`). Full list in Swagger.

| Area | Endpoints |
|---|---|
| Auth | `POST /auth/login`, `GET /auth/me` |
| Dashboard | `GET /dashboard?years=2025&years=2026`, `GET /dashboard/pivot/classification-by-year`, `GET /dashboard/pivot/category-by-year` |
| Assessments | `GET/POST /assessments`, `GET /assessments/year/{year}`, `PUT/DELETE /assessments/{id}` |
| Allocations | `GET /allocations` (filters: years, classificationId, category, status, approvalStatus, paymentMethod, q, page, size), `GET /allocations/summary`, `POST`, `PUT/DELETE /allocations/{id}`, `POST/GET /allocations/{id}/receipt` |
| Beneficiaries | `GET /beneficiaries`, `GET /beneficiaries/typeahead`, `POST`, `PUT/DELETE /beneficiaries/{id}`, `PATCH /beneficiaries/{id}/verification` |
| Planning | `GET /planning?years=`, `GET /planning/{id}/allocations`, `POST`, `PUT/DELETE /planning/{id}` |
| Masters | `/classifications`, `/beneficiary-categories`, `/source-accounts`, `/agents`, `/employees`, `/schools` (list/create/update/delete) |
| Lookups | `/lookups/years`, `/lookups/classifications`, … |
| Exports | `/export/allocations.xlsx`, `/export/beneficiaries.xlsx`, `/export/assessments.xlsx` (ADMIN, ACCOUNTANT) |
