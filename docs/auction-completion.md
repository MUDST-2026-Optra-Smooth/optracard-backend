# Auction completion: policy, rollout and verification

This branch extends the existing auction system. WeeklyReport fixes remain on the separate `fix/first-admin-weekly-report` branch; no merge or deployment is performed by these changes.

## User-facing additions

- `/my-auctions`: authenticated buyers/sellers see bids, current leading/outbid state, wins, checkout links and a dispute submission form.
- `/auction-notifications`: persisted per-user review/outbid/win/deadline/payment-recording notices and owner-only read state. This is an in-app inbox; no email/SMS delivery is configured.
- `/superadmin/auctions`: all-status search with store/date filters, pagination, bid counts/value, item GMV, sell-through, expiry counts, seller incidents, audit, reason-required suspend/close, default settings, bidder restrictions and dispute resolution.
- Detail and bid history refresh every five seconds. Countdown updates every second. Hub refreshes every fifteen seconds and has search/game/source/status/price/sort controls and twelve-item pages. Hub pagination currently occurs after loading published listings; oversight pagination is returned by its API.
- New auction listings link to an inventory product with condition/grading and up to eight HTTP(S) evidence image URLs. The form requires an inventory selection. Existing listings without a link must be linked before they can be submitted/opened again.
- Customer order history identifies auction wins, exposes `auctionId`, and presents an auction item even when the legacy auction checkout created no normal order-item row.

## State and concurrency policy

- Seller edits: DRAFT, PENDING_REVIEW or REJECTED only. Approved scheduled and live listings are locked against edits.
- Seller cancellation: DRAFT, PENDING_REVIEW, REJECTED, SCHEDULED, or LIVE with zero bids. Live auctions with bids and winner/terminal states cannot be cancelled by sellers.
- Admin review: pending marketplace listings only. Rejection requires a reason. Admin cancellation of live bids requires a reason; the UI asks for reasons for all cancellations.
- Superadmin suspend/close requires a reason and audit. Winner checkout and completed orders are excluded from this tool and require dispute investigation rather than automatic order/refund changes. SUSPENDED is hidden publicly and keeps its inventory reservation; CLOSE releases it.
- No self-service bid withdrawal. A participant can file a dispute with reasons/evidence; superadmin decisions are recorded. Resolving a dispute does not silently change an order or payment.
- Bidders must be USER or SELLER, cannot be the seller, and cannot be restricted. Inputs must be finite positive monetary amounts, at most THB 1,000,000,000 and two decimal places.
- Auction row locks serialize bids, checkout and seller/admin changes. The lifecycle job locks current rows in a separate transaction per auction. Two same-minimum concurrent bids must produce one accepted bid and one rejected bid.
- Default bid increment is THB 50, checkout is 24 hours, anti-sniping is 5 minutes, and duration is 1–168 hours. Increment/checkout/anti-sniping terms are stored per listing. Default edits apply to newly created listings; they do not retroactively change live terms. The duration validation uses current platform bounds at create/submission/review.
- A bid within the last anti-sniping window extends the previous ending time by that window. A delayed lifecycle run uses the auction end time for the checkout deadline and processes overdue transitions within one run.
- Stock is reserved at submission/opening, not merely when a draft is saved. The product row is locked, one unit is withheld, the normal listing is hidden and its reservation flag prevents normal checkout. A second auction cannot reserve it. Rejection/cancellation/no-bid/expiry/close restores the unit exactly once; a completed auction consumes it. Normal cart checkout takes product locks in product-ID order.
- Public list/detail responses redact winning/leading IDs, checkout deadlines and private review notes unless the viewer has the corresponding right. A buyer sees their own identity; the related seller and admin/superadmin can see required participant information.
- The bid endpoint has a separate IP quota of 30 requests/minute enabled by default via `app.auction.rate-limit.enabled`. This limiter is process-local; deployments with multiple replicas need a shared gateway/Redis quota for a single global rate limit. IP quotas can be shared by buyers behind the same NAT.

## Metrics definitions

Filters apply to all overview metrics and use listing creation dates. Bid value is the sum of submitted bid amounts, not sales revenue. Paid item GMV sums auction-order subtotal, excluding delivery. Sell-through is COMPLETED divided by COMPLETED + ENDED_NO_BIDS + CHECKOUT_EXPIRED. Seller incidents count cancellations, suspension/close and checkout expiry; the count is an investigation signal, not an automatic guilt or reliability score.

The existing project's checkout records payments as paid on accepted checkout. Notification/GMV semantics follow that demo payment model. This work does not integrate a payment provider or initiate real charges.

## Deployment and migration

1. Back up and test the additive PostgreSQL script `database/auction-completion-migration.sql` against a disposable copy.
2. Deploy backend and frontend from the reviewed auction branch together. Settings/events/restrictions are new tables; auction/product fields are additive.
3. Link and review legacy listings manually; the migration deliberately does not infer historical stock reservations.
4. Run the read-only smoke against the actual API URL. No staging/deployed URL has been supplied at implementation time.

Do not enable arbitrary schema changes or apply a production migration as part of a read-only smoke run.

## Verification commands

Backend: `mvn test` includes fast H2 concurrency/journey/RBAC/stock/notification/settings/dispute tests plus unit regressions. For real PostgreSQL locking, run `RUN_AUCTION_POSTGRES_TESTS=true mvn test -Dtest=AuctionPostgresIntegrationTest` with Docker. The existing nightly Testcontainers workflow enables this flag and uploads Surefire/JaCoCo reports.

Frontend: `npm test`, `npm run lint`, `npm run build`; `npm run test:auction` selects auction suites. `npm run test:e2e` includes `13-auction-journey.cy.ts` in the critical lane. The Cypress navigation test uses intercepted responses; backend integration exercises real persistence and state transitions separately.

Staging/deployed: set `CYPRESS_API_BASE_URL` and run `npm run test:e2e:staging` or `npm run test:e2e:deployed`. Alternatively run `scripts/auction-api-smoke.ps1 -ApiBaseUrl <url>` from PowerShell. These read-only tests do not mutate user auctions. Missing URLs are skipped explicitly; an empty auction list does not exercise detail/history checks.

Full frontend TypeScript validation has pre-existing failures in SPAD_Overview monthly metrics and AuthContext test mocks. Changed auction code must introduce no additional TypeScript failures. Record actual test results before merging; the existence of test files is not evidence they passed.
