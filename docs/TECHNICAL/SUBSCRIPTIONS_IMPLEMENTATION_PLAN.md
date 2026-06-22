# Subscriptions — Implementation Plan

Written before any `subscriptions` code, per the process in
[`BACKEND_TESTING_AND_PACKAGING_PLAYBOOK.md`](BACKEND_TESTING_AND_PACKAGING_PLAYBOOK.md) §7 and
the same documentation-first approach used for
[`METRICS_IMPLEMENTATION_PLAN.md`](METRICS_IMPLEMENTATION_PLAN.md).

## 1. What "subscriptions" covers in this pass

Per the original architecture sketch (`BACKEND_CUSTOM_IMPLEMENTATION.md` §3): *"subscriptions —
billing state, RevenueCat webhook handling."* This pass builds the **billing state** half only —
the `Subscription` entity, its lifecycle transitions, status queries, and a reusable gating
check. It does **not** build any real payment-processor integration (Razorpay, Stripe,
RevenueCat, Apple StoreKit) or a webhook endpoint wired to one.

**Explicit non-goal, per `CLAUDE.md`**: *"Payments — simplified per PRD §6: cards (global) + UPI
only... conflicts with Apple's App Store policy (StoreKit/IAP required for iOS)... this is an
open question, not a resolved decision... Don't build payment integration against either reading
until this is explicitly resolved with the user."* `docs/TECHNICAL_REQUIREMENTS.md` §5 has the
full detail: Apple requires StoreKit for iOS digital subscriptions almost everywhere (15–30%
commission), which the PRD's flat "cards + UPI" claim doesn't address — a real, unresolved
business/legal decision, not a technical one. This pass builds `SubscriptionService.activate(...)`
as the entry point a real payment confirmation calls later — mirroring exactly how
`MetricIngestionService.recordReading(...)` stands in for the not-yet-built Spike API ingestion —
but nothing calls it yet except tests.

## 2. What already exists, and what's moving

`Subscription`/`SubscriptionStatus`/`SubscriptionRepository` were created inside `members` when
`MembersService.completeProfile` needed to start a trial on first Primary signup — they have no
business logic of their own (no transition methods on the entity, no service). They move to a new
top-level `subscriptions` package, matching the established per-feature pattern. `MembersService`
calls `SubscriptionService.startTrial(accountId)` instead of constructing the entity directly —
members triggers the trial at the right moment, subscriptions owns what "starting a trial" means
(including the `TRIAL_DAYS = 7` constant, which moves out of `MembersService` too).

**Files affected by the move** (grepped before writing this plan):
- `members/repository/SubscriptionRepository.java`, and the `Subscription`/`SubscriptionStatus`
  classes already in `members/model/` → become `subscriptions/repository/SubscriptionRepository.java`,
  `subscriptions/model/Subscription.java`, `subscriptions/model/SubscriptionStatus.java`.
- `members/service/MembersService.java` — drop the `SubscriptionRepository` dependency and
  `TRIAL_DAYS` constant, add a `SubscriptionService` dependency, call `startTrial(accountId)`.
- Tests: `members/repository/SubscriptionRepositoryTest.java` moves to
  `subscriptions/repository/`; `members/service/MembersServiceTest.java` and
  `members/integration/MembersFlowIntegrationTest.java` update their imports and mocks/assertions
  to the new package + the `SubscriptionService` call.
- No schema migration needed — the `subscriptions` table itself doesn't move, only the Java
  package. No new columns needed for this pass either (see §4 — no `next_billing_date` concept
  yet, since nothing computes one without a real payment processor).

## 3. PRD requirements this maps to

- **§6.1 Trial**: 7 days, full product access, payment method required upfront (the "payment
  method required upfront" part is itself blocked on the processor question above — not built
  this pass; the trial *starts* regardless, same as today's behavior, just owned by
  `subscriptions` now).
- **§6.2 Trial Expiry**: *"Continued → seamless conversion. Cancelled/lapsed → wearable sync
  paused, data becomes read-only."* Without a real processor, nothing can organically transition
  TRIAL → ACTIVE — so the only transition this pass's scheduled job can correctly perform from
  time passing alone is **TRIAL → EXPIRED** once `trialEndsAt` has passed and no activation
  happened. `markActive`/`markExpiring`/`cancel` are built as callable transition methods (ready
  for a future webhook), not driven by the scheduled job.
- **§7 Pricing**: ₹249/month, monthly recurring — informs `SubscriptionService.activate(...)`'s
  shape (it will need a renewal-period concept once a processor exists) but isn't computed by
  anything in this pass.
- **§12 UX Edge Cases**: *"Subscription lapses: wearable sync pauses, sharing pauses, old data
  remains visible read-only."* This pass builds the reusable `isActive(accountId)` gating check
  feature code can call to enforce this — but does **not** wire it into `members`/`metrics`
  endpoints yet (nothing in those features currently checks subscription state; retrofitting them
  is its own task, not a silent side effect of building `subscriptions`).

### 3.1 The `EXPIRING` status

`SubscriptionStatus` already has an `EXPIRING` value (present before this pass, inherited from
the frontend's `SubscriptionStatus` type in `src/types/index.ts`) that the PRD text doesn't
explicitly define — by convention in subscription billing (and consistent with RevenueCat's
model referenced in `TECHNICAL_REQUIREMENTS.md` §5), it represents a failed-renewal grace period:
access continues, but a payment attempt failed. `isActive(accountId)` treats `TRIAL`, `ACTIVE`,
and `EXPIRING` as active (access continues); only `EXPIRED`/`CANCELLED` are inactive. No
transition *into* `EXPIRING` happens automatically in this pass (it needs a real failed renewal
attempt, which needs a real processor) — `markExpiring()` exists as a transition method for that
future caller.

## 4. Schema (unchanged)

The `subscriptions` table already exists (`V2__create_members_schema.sql`): `id`, `account_id`
(unique), `status`, `trial_ends_at`, `renewed_at`, `created_at`. No migration needed for this
pass — no new columns. `Subscription` gets transition methods added (the same `update`-style
mutator pattern as `MetricBaseline`/`Insight`):

```java
void markActive(Instant renewedAt) { this.status = ACTIVE; this.renewedAt = renewedAt; }
void markExpiring() { this.status = EXPIRING; }
void markExpired() { this.status = EXPIRED; }
void markCancelled() { this.status = CANCELLED; }
```

## 5. Package layout

```
subscriptions/
  controller/  SubscriptionsController — GET /api/v1/accounts/{accountId}/subscription
  service/     SubscriptionService — startTrial(accountId), activate(accountId, renewedAt),
               markExpiring(accountId), markExpired(accountId), cancel(accountId),
               isActive(accountId)
               SubscriptionLifecycleScheduler — nightly job, TRIAL -> EXPIRED past trialEndsAt
  repository/  SubscriptionRepository (moved from members)
  model/       Subscription, SubscriptionStatus (moved from members)
  dto/         SubscriptionResponse
  exception/   SubscriptionNotFoundException (404 for the status query)
  integration/ SubscriptionsFlowIntegrationTest
```

Authorization: the status query endpoint requires the requester to be a Member of the account
(reuses `members.service.MemberPermissions` — same cross-package reuse pattern `metrics` already
established for `SharingScope`/`canViewScope`). Only the account's own Primary or a Secondary
already on that account can query its subscription; no new permission model needed.

## 6. API

- `GET /api/v1/accounts/{accountId}/subscription` → `SubscriptionResponse` (status, trialEndsAt,
  renewedAt). 404 via `SubscriptionNotFoundException` if no subscription row exists for that
  account (shouldn't happen in practice — every account gets one on Primary signup — but the
  query path should fail clearly, not NPE, if it somehow doesn't). 403 via the existing
  `members.exception.ForbiddenMemberAccessException` if the requester isn't a member of that
  account.
- No write endpoint — `SubscriptionService`'s transition methods are internal, called by
  `MembersService` (trial start) and, later, a real payment integration (activation/cancellation)
  — never directly over REST in this pass, same reasoning as `metrics`' no-write-endpoint
  decision.

## 7. Test strategy

Per the playbook, including the database-assertion and full-pipeline principles already banked
there from `metrics`/`auth`/`members` (playbook §2): direct repository assertions in the flow
test, not just API-response checks; TDD red/green verified for every net-new unit test; H2 fast
tier + Postgres Testcontainers tier; JaCoCo gate expanded to `com.bonaca.backend.subscriptions.**`.

- `SubscriptionTest` (model): the four transition methods mutate state correctly.
- `SubscriptionServiceTest`: `startTrial` creates a `TRIAL` row with `trialEndsAt` = now + 7
  days; `activate`/`markExpiring`/`markExpired`/`cancel` transition an existing row; `isActive`
  returns true for TRIAL/ACTIVE/EXPIRING, false for EXPIRED/CANCELLED; behavior when no
  subscription row exists for the account (treat as inactive, don't throw — a not-yet-onboarded
  account has no subscription at all, which is a different case from "subscription expired").
- `SubscriptionLifecycleSchedulerTest`: nightly job transitions exactly the TRIAL rows whose
  `trialEndsAt` has passed, leaves everything else untouched (ACTIVE rows, TRIAL rows not yet
  expired, already-EXPIRED rows).
- `SubscriptionRepositoryTest` (moved + extended): the unique `account_id` constraint, plus a new
  query method for "TRIAL rows past their trialEndsAt" the scheduler needs.
- `SubscriptionsControllerTest` (`@WebMvcTest`, same `addFilters = false` /
  `SecurityContextHolder` pattern as the other controller slices): 200 with the right shape, 403
  when the requester isn't a member of the account, 404 when no subscription row exists.
- `MembersServiceTest` updates: the trial-creation test now verifies `subscriptionService
  .startTrial(accountId)` was called, instead of capturing a `subscriptionRepository.save(...)`
  argument.
- `SubscriptionsFlowIntegrationTest` (Testcontainers/Postgres, `subscriptions/integration/`):
  real sign-up + complete-profile via the actual HTTP flow (reusing the established helper
  pattern), confirm the resulting `TRIAL` row directly via the repository *and* via the GET status
  endpoint (the now-established read-side cross-check pattern — fetch the entity, then assert the
  API response matches it field-by-field, not two independently-plausible values), then call
  `SubscriptionLifecycleScheduler` directly with a backdated `trialEndsAt` (or call
  `subscriptionService.markExpired` directly, since manipulating "now" isn't practical) to
  simulate trial expiry and confirm both the persisted row and the GET response reflect `EXPIRED`.

## 8. Non-goals (explicit)

- Any real payment-processor SDK or webhook (Razorpay, Stripe, RevenueCat, Apple StoreKit).
- Resolving the cards-vs-StoreKit question — that's the user's call, tracked as a known blocker.
- Wiring `isActive(accountId)` into `members`/`metrics` endpoints to actually enforce gating —
  the check is built and tested, but not yet called from anywhere except tests.
- A `next_billing_date`/renewal-period schema concept — not needed until a real processor exists.
- Automatic `EXPIRING` transitions — needs a real failed-renewal-attempt signal this pass doesn't
  have.
