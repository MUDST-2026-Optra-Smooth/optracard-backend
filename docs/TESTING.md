# Backend test strategy

## Test layers

| Layer | Purpose | Database | Trigger |
| --- | --- | --- | --- |
| Unit | service rules, validators, mapping, security helpers | Mockito/no database | every PR |
| H2 integration | controller, security, persistence and transaction behaviour | H2 PostgreSQL mode | every PR |
| PostgreSQL contract | mapping/schema/query compatibility with the deployment database | Testcontainers PostgreSQL 16 | nightly/manual |
| Deployed API smoke | black-box status and response-shape checks | deployed service only | after backend rollout |

Run the normal suite with `mvn clean verify`. It produces JaCoCo HTML/XML under `target/site/jacoco/` and Surefire results under `target/surefire-reports/`. Coverage is reported and retained first; no global coverage gate is imposed in this phase.

## Commands

```bash
mvn clean verify
RUN_POSTGRES_TESTS=true mvn clean verify
powershell -ExecutionPolicy Bypass -File scripts/deployed-api-smoke.ps1 -ApiBaseUrl https://api.example.test
powershell -ExecutionPolicy Bypass -File scripts/staging-auction-e2e.ps1 -ApiBaseUrl https://staging-api.example.test -SellerEmail seller@example.test -SellerPassword '...' -AdminEmail admin@example.test -AdminPassword '...' -BuyerEmail buyer@example.test -BuyerPassword '...' -RunId local-001
```

`PostgresPersistenceContractTest` is deliberately disabled unless `RUN_POSTGRES_TESTS=true`; therefore fast pull-request checks retain H2 speed and do not require Docker. The nightly job enables it on a Docker-capable Ubuntu runner.

## Backend test-case matrix

| Area | Unit and H2 integration cases |
| --- | --- |
| Authentication/JWT | register/login/profile lifecycle; duplicate credentials; password and role rules; malformed, expired and tampered JWT; unauthenticated/cross-user denial |
| Cart | positive quantity; product visibility; inactive/unapproved marketplace denial; stock cap; merge duplicate lines; update/remove/clear and buyer ownership |
| Catalog/search | approved/offical visibility; paging; name/price sort; Thai aliases; typo ranking; price/source/store/game/language filters; invalid ranges and filter input |
| Orders | empty cart; single/multi-seller splitting; pickup restriction; stock decrement; cart clearing; snapshots; discount rounding/cap; transaction failure leaves state unchanged |
| Promotions | admin validation; active/date/global limit; minimum order; cap; per-user redemption; preview does not consume; checkout consumes exactly once |
| Reorder | buyer authorization; full/partial/empty outcomes; deleted/inactive/out-of-stock products; cart merge; notification/cart event implications |
| Seller/admin/super-admin | store/listing ownership; approval and status transitions; tracking requirement; destructive actions; report date boundaries; role matrix across every protected route |
| Notifications/stock/reporting | alert threshold; read-state ownership; empty/zero/error data; metrics and reporting intervals |
| Auctions | seller approved-store requirement; one-hour to seven-day duration; draft/edit/submit/cancel/relist; public visibility; bid minimum/ownership; leading/winner selection; checkout address/shipping; admin review/cancel; endpoint delegation and RBAC denial |
| Marketplace fee/settlement | default and versioned fee rates; money rounding; order/auction charge snapshot; paid Marketplace isolation; reversal idempotency; seller isolation; super-admin type/date/store/user filters; invalid input |
| PostgreSQL | promotion rules plus auction lifecycle fields, settlement enum/decimal precision and unique ledger reference against PostgreSQL 16 rather than only H2 |

The promotion integration contract explicitly verifies the customer-facing rule: **50% off capped at ฿100, minimum ฿500, one redemption per customer**. It previews without consuming the code, succeeds for the first buyer checkout, rejects that buyer's second use, and permits another buyer.

## Environment and data governance

- H2 tests create their own fixtures and must clean dependent rows in foreign-key order.
- `scripts/staging-auction-e2e.ps1` is the resettable staging write journey. It creates `E2E-{runId}-auction`, submits, approves and bids using normal seller/admin/buyer APIs, then cancels the created auction in `finally`. It cannot fast-forward a real auction's one-hour duration; an optional pre-prepared winner fixture may be supplied for checkout/ledger verification.
- A staging write suite must create objects through normal authenticated APIs with `E2E-{runId}-` prefixes and delete those objects during cleanup. Never add a test-only production controller or access the database directly.
- Use dedicated, resettable staging accounts for buyer, seller, admin and super-admin; do not reuse a human account.
- Deployed smoke takes `BACKEND_API_URL`, optional `E2E_EMAIL`, and optional `E2E_PASSWORD`. It checks health, OpenAPI, public catalog/search/product and public auction list/detail, then only login/profile/order-history reads. It makes no mutation.
- Keep diagnostics safe: CI may upload JUnit/JaCoCo and Cypress output, but must never print tokens, passwords or full authorization headers.

## CI ownership

- `backend-ci.yml` is the PR fast path: Maven verify/H2 integration, JaCoCo/Surefire artifacts, Docker build, Kubernetes manifest validation, and Compose validation.
- `nightly-integration.yml` is scheduled on weekdays and can be run manually. It enables the PostgreSQL Testcontainers contract and, only when all `STAGING_*` resettable credentials plus the explicit write-enable secret are present, runs the staging seller → admin → buyer auction journey.
- Backend rollout invokes `scripts/deployed-api-smoke.ps1` after Kubernetes reports the deployment ready. A configured smoke failure fails the deploy verification.

When adding a controller endpoint, add an authorization test, one successful contract test, and an invalid-input/ownership test before considering it complete.
