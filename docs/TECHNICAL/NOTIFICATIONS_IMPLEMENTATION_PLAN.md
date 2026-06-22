# Notifications — Implementation Plan

Written before any `notifications` code, per the process in
[`BACKEND_TESTING_AND_PACKAGING_PLAYBOOK.md`](BACKEND_TESTING_AND_PACKAGING_PLAYBOOK.md) §7 and
the same documentation-first approach used for
[`METRICS_IMPLEMENTATION_PLAN.md`](METRICS_IMPLEMENTATION_PLAN.md) and
[`SUBSCRIPTIONS_IMPLEMENTATION_PLAN.md`](SUBSCRIPTIONS_IMPLEMENTATION_PLAN.md).

## 1. What "notifications" covers in this pass

Per the original architecture sketch (`BACKEND_CUSTOM_IMPLEMENTATION.md` §3): *"notifications —
Insight generation, Expo Push dispatch."* Insight generation already exists (`metrics`); this pass
builds **in-app notification storage, fan-out, and a read API** — the rows a notifications screen
reads — plus the trigger logic that decides when a notification gets created. It does **not**
build a real Expo Push SDK/HTTP call.

**Explicit non-goal, per `CLAUDE.md`**: *"Push notifications (Expo Push service). Not yet built...
Do not install SDKs or scaffold infra for any of the above without an explicit task."* This pass
follows the same "internal entry point" pattern already used twice (`MetricIngestionService
.recordReading`, `SubscriptionService.activate`/`markExpiring`/`cancel`): `NotificationService
.create(...)` persists the row and is the single call site a future real Expo Push integration
hooks into — nothing in this codebase calls an actual push provider yet.

The frontend's mocked `NotificationsScreen.tsx` has 3 notification `type`s — `metric-anomaly`,
`subscription`, `payment-request` — and a `Notification` type in `src/types/index.ts`. All 3 are
in scope this pass.

## 2. The two-member-reference model

`Notification` in `src/types/index.ts` has a single `memberId` field, used as the display subject
("Mom", "Dad", "Prasanna Kumar" — whoever the notification is *about*), not the recipient. The
recipient is implicit: whichever user is logged in sees their own notification list. Backend needs
both:

- **`recipientMemberId`** — whose notification list this row appears in; the GET endpoint's
  authorization boundary. Not exposed in the API (the endpoint is already scoped to "my
  notifications," so the field would be redundant on every row).
- **`subjectMemberId`** — who the notification is about; mapped to the API's `memberId` field, used
  by the frontend to resolve a title/avatar. Equals `recipientMemberId` for self-concerning
  notifications (a payment request addressed to me, my own subscription lapsing) and differs for
  member-concerning ones (a Secondary viewing an anomaly notification about a Primary they have
  visibility into).

`title` is stored as plain text at creation time (the subject member's display name) rather than
resolved client-side from `subjectMemberId` — consistent with DTOs elsewhere mirroring the mock
data shape closely. `displayTime` (`"1 hr ago"`, `"Yesterday, 3:30 PM"`) is **not** built —
that's relative-time formatting, a frontend presentational concern; the API returns `createdAt`
(an `Instant`) and lets the client format it, same restraint already applied to other DTOs.

## 3. The three triggers

### 3.1 `metric-anomaly`

`metrics.service.InsightGenerationService.generateTrendInsight` currently always writes
`InsightKind.TREND` — `InsightKind.ANOMALY` exists in the enum (`metrics/model/InsightKind.java`)
but nothing produces it. This pass adds a second, stricter threshold: `|z-score| >= 2.0` (vs the
existing `TREND_THRESHOLD = 1.0`) writes `ANOMALY` instead of `TREND` for that metric/day — a small
additive change to an existing method, not a new generation path. The composite Routine
Consistency insight is unaffected (stays `TREND` always — it has no single metric to map to a
`SharingScope`, see below).

A new `NotificationGenerationService.generateMetricAnomalyNotifications()` (in `notifications`)
scans `InsightRepository.findByInsightDateAndKind(today, ANOMALY)` (new repository method) once a
night. For each such insight, the subject member's account members are enumerated
(`MemberRepository.findByAccountId`), filtered to whichever ones can actually view that metric's
scope (`MemberPermissions.canViewScope(candidate, subject, metricType.scope())` — `MetricType
.scope()` already exists and maps every metric to VITALS/ACTIVITY/BEHAVIOUR), and a `Notification`
is created for each, excluding the subject themself (a member doesn't get notified about their own
anomaly the same way a Secondary watching them would — their own Member Detail screen already
shows it).

**Idempotency**: a `sourceInsightId` column (nullable FK to `insights`) lets the scan re-run safely
— `existsByRecipientMemberIdAndSourceInsightId` is checked before creating, so a second nightly run
(or a manually re-triggered one) doesn't duplicate. This mirrors the existing `Insight` upsert's
own re-run safety.

### 3.2 `subscription` (lapsed trial only — see §6 for what's deliberately excluded)

The first design considered here had `subscriptions.service.SubscriptionLifecycleScheduler
.expireLapsedTrials()` call `NotificationService` directly right after `markExpired(accountId)`
succeeds — it detects the TRIAL → EXPIRED transition exactly once, atomically, per account, so it
looked like the cleanest possible trigger point with no re-derivation needed. **This was wrong**:
`members` already depends on `subscriptions` (`MembersService.completeProfile` calls
`SubscriptionService.startTrial`), and `notifications` already needs to depend on `members` (for
recipient/account lookups) — so `subscriptions` calling `notifications` would have closed `members
→ subscriptions → notifications → members`, a 3-package cycle, not the 2-package one originally
checked for. Caught by actually tracing the full graph after wiring it, not just the new edge in
isolation — see §4.

Instead, `NotificationGenerationService.generateLapsedSubscriptionNotifications()` (in
`notifications`, alongside the metric-anomaly scan) scans `SubscriptionRepository.findByStatus
(EXPIRED)` (new repository method) once a night and calls `NotificationService.notifyAccountMembers
(accountId, SUBSCRIPTION, body, deepLink)` for each — one self-concerning `Notification` per
account member (`subjectMemberId == recipientMemberId`, `title` = that member's own display name),
using the PRD §6.2 copy verbatim: *"Health tracking is paused. Restart subscription to continue."*
`subscriptions.service.SubscriptionLifecycleScheduler` itself is untouched by this feature — it
still only knows how to expire trials, nothing about notifications.

**Idempotency**: checked via `existsByRecipientMemberIdAndNotificationType(recipientId,
SUBSCRIPTION)` inside `notifyAccountMembers` itself — fires once per account, the first time
EXPIRED is observed (not once per nightly scan — the scan re-finds the same EXPIRED row every
night until idempotency stops it from duplicating). This is correct, not a shortcut, *given the
current actual state space*: no real payment processor exists yet
(`SUBSCRIPTIONS_IMPLEMENTATION_PLAN.md` §1's blocker), so nothing can currently call `activate()`
to bring an account back from EXPIRED — meaning EXPIRED can't currently recur for an account, so
"once ever" and "once per lapse" are the same thing today. Whoever wires up a real payment
processor and makes `activate()` reachable should revisit this check then (it would need to become
re-armable per lapse, not per account).

### 3.3 `payment-request`

A user-triggered notification, not a system-generated one — a member of an account asking another
member of the same account to use the (still unbuilt) Payment Gateway screen. **This does not touch
payment processing in any way** — no money moves, no processor is called; it creates exactly one
row with `deepLinkTarget = "/subscription/payment-gateway"` pointing at a screen that doesn't yet
do anything real either. This is why it's in scope despite the payment-processor blocker still
being open: it's a message between two members, structurally identical to "Secondary nudges
Primary," not a payment feature.

`POST /api/v1/members/{recipientMemberId}/notifications/payment-request` — the caller must be a
member of the same account as `recipientMemberId` and not be the recipient themself (you can't
request payment from yourself). `subjectMemberId == recipientMemberId` (self-concerning, mirrors
§3.2), `title` = recipient's own display name, `body` = *"`{requester display name}` has requested
₹249 to renew your wearable connection. Tap to pay."* (the ₹249 figure is PRD §7's flat price,
hardcoded as a constant — there's no per-account price to look up yet).

## 4. Package dependency direction (and why it can't go the other way)

`notifications` depends on `members` (Member, MemberRepository, MemberPermissions, SharingScope —
same reuse pattern `metrics`/`subscriptions` already established), `metrics` (`InsightRepository`,
read-only, for the anomaly scan), and `subscriptions` (`SubscriptionRepository`, read-only, for the
lapsed scan). All one-directional, `notifications` always the dependent, never depended on.

This was *not* the first design. §3.2 originally had `subscriptions.SubscriptionLifecycleScheduler`
call `NotificationService` directly right after a TRIAL → EXPIRED transition — it looked safe
because it only adds one new edge, `subscriptions → notifications`, and `notifications` wasn't
going to depend back on `subscriptions` in that design. The mistake was checking only that one new
edge for a cycle instead of the whole graph: `members` *already* depends on `subscriptions`
(`MembersService.completeProfile` calls `SubscriptionService.startTrial`, an existing edge from the
subscriptions pass), and `notifications` already has to depend on `members` (every trigger needs
Member/account lookups). Add `subscriptions → notifications` on top of those two pre-existing edges
and the full path is `members → subscriptions → notifications → members` — a 3-package cycle,
invisible if you only check the edge you just added.

This was caught after wiring it (compiled fine — Java doesn't enforce package acyclicity, and there
was no single circular *bean* constructor dependency for Spring to reject either, since the cycle
runs through three different concrete classes) by re-deriving the graph from scratch before writing
tests against it. Fixed by reverting `SubscriptionLifecycleScheduler` to depend on nothing outside
`subscriptions` (back to its pre-notifications form) and instead having `notifications` poll
`subscriptions.SubscriptionRepository` for `EXPIRED` rows on its own nightly scan — the "naive"
design originally passed over, which turned out to be the one that doesn't have a cycle. The
trade-off accepted for this: `notifications` re-derives "is this account lapsed" from a state scan
rather than catching the exact transition moment, which is why §3.2's idempotency has to be
"once per account, ever" rather than "once per lapse" — a real constraint, not free, but the
correct one given EXPIRED can't currently recur (no payment processor to reactivate from it).

Resulting graph (all one-directional, no cycle): `auth` (leaf) ← `members` ← `subscriptions`;
`members` ← `metrics`; `members`, `metrics`, `subscriptions` ← `notifications`.

## 5. Schema — new Flyway migration `V4__create_notifications_schema.sql`

```sql
CREATE TABLE notifications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_member_id UUID NOT NULL REFERENCES members (id),
    subject_member_id   UUID NOT NULL REFERENCES members (id),
    notification_type   VARCHAR(40) NOT NULL,
    title               VARCHAR(120) NOT NULL,
    body                VARCHAR(280) NOT NULL,
    deep_link_target    VARCHAR(200) NOT NULL,
    is_read             BOOLEAN NOT NULL DEFAULT FALSE,
    source_insight_id   UUID REFERENCES insights (id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_recipient_created ON notifications (recipient_member_id, created_at DESC);
```

`type`/`read` are avoided as bare column names — `notification_type`/`is_read` instead — the same
caution already paid for once with `metric_readings.value` → `metric_value` (H2's `MODE=PostgreSQL`
silently breaks schema generation per-table on a reserved word, logged as WARN not ERROR). No
DB-level uniqueness for idempotency (no partial/filtered unique index) — both dedup checks in §3.1
and §3.2 are plain existence queries in application code, the same approach already documented and
justified for `Insight`'s null-`metricType` upsert.

## 6. Package layout (`com.bonaca.backend.notifications`, same shape as the playbook)

```
notifications/
  controller/  NotificationsController — GET .../notifications, PATCH .../read, POST .../payment-request
  service/     NotificationService — create(...), notifyAccountMembers(...), markRead(...), listForMember(...), requestPayment(...)
               NotificationGenerationService — generateMetricAnomalyNotifications(),
               generateLapsedSubscriptionNotifications() (see §3.2/§4 for why this scans
               subscriptions.SubscriptionRepository rather than being called by it)
               NotificationsRollupScheduler — nightly job, calls both generation methods above
  repository/  NotificationRepository
  model/       Notification, NotificationType (METRIC_ANOMALY, SUBSCRIPTION, PAYMENT_REQUEST)
  dto/         NotificationResponse
  exception/   NotificationNotFoundException (404), SelfPaymentRequestException (409);
               reuses members.exception.ForbiddenMemberAccessException, MemberNotFoundException
  integration/ NotificationsFlowIntegrationTest
```

`subscriptions/repository/SubscriptionRepository.java` gets one additive query method this pass:
`findByStatus(SubscriptionStatus)`, used only by `notifications`. `subscriptions.service
.SubscriptionLifecycleScheduler` itself is unmodified.

## 7. API

- `GET /api/v1/members/{memberId}/notifications` → `List<NotificationResponse>`, newest first.
  Self-only — the caller must *be* `memberId` (same "you only see your own list" boundary as the
  rest of the model; not the looser "any member of the account" check `subscriptions` uses, since a
  notification list is personal, not account-shared).
- `PATCH /api/v1/notifications/{id}/read` → `NotificationResponse`. Caller must be the recipient.
- `POST /api/v1/members/{memberId}/notifications/payment-request` → `NotificationResponse` (201).
  Caller must be a member of the same account as `memberId`, and not be `memberId` themself.
- Same JWT + `@AuthenticationPrincipal` pattern as every other controller; 403 via the existing
  `ForbiddenMemberAccessException` → `ApiExceptionHandler`.

## 8. Test strategy

Per the playbook, including the database-assertion and full-pipeline principles banked there from
`metrics`/`subscriptions` (playbook §2): direct repository assertions in the flow test, not just
API-response checks; TDD red/green verified for every net-new unit test; H2 fast tier + Postgres
Testcontainers tier; JaCoCo gate expanded to `com.bonaca.backend.notifications.**`.

- `NotificationTest` (model): construction, `markRead()`.
- `NotificationRepositoryTest` (H2 `@DataJpaTest`): the two existence-check query methods, the
  recipient-ordered list query.
- `NotificationServiceTest`: `create` persists with the right recipient/subject split;
  `notifyAccountMembers` creates one row per account member; `markRead` flips the flag and rejects
  a non-recipient caller; `listForMember` returns newest-first.
- `NotificationGenerationServiceTest`: an `ANOMALY` insight fans out to in-scope account members
  only (a Secondary without a Vitals grant doesn't get one for a heart-rate anomaly), excludes the
  subject themself, and is idempotent on a second run (no duplicate via `sourceInsightId`);
  `generateLapsedSubscriptionNotifications` calls `notifyAccountMembers` for every `EXPIRED`
  account and does nothing when none are expired.
- `InsightGenerationServiceTest` gets new cases: `|z| >= 2.0` writes `ANOMALY`, `1.0 <= |z| < 2.0`
  still writes `TREND` (regression check that the existing threshold logic isn't broken), and a
  same-day re-run reclassifies an existing row's `kind` (not just its text).
- `SubscriptionRepositoryTest` gets one new case for `findByStatus`. `SubscriptionLifecycleScheduler`
  itself is unchanged from the subscriptions pass — no new test needed there.
- `NotificationsRollupSchedulerTest`: the nightly job calls both generation methods.
- `NotificationsControllerTest` (`@WebMvcTest`, same pattern as the rest): 200 for self, 403 for a
  non-self GET; 200 + persisted flag for a valid read-mark, 403 for a non-recipient, 404 for an
  unknown notification; 201 for a valid payment-request, 403 for a non-account-member, 409 for
  self-targeting.
- `NotificationsFlowIntegrationTest` (Testcontainers/Postgres, `notifications/integration/`): a
  Primary + Secondary onboarded via the real HTTP flow (reusing the established helper pattern),
  Secondary granted Vitals; seed ~15-20 days of heart-rate readings via `MetricIngestionService`
  with the last reading several baseline-stddevs off, run `BaselineService`/`InsightGenerationService`
  directly to simulate the nightly rollup producing an `ANOMALY` insight, run
  `NotificationGenerationService.generateMetricAnomalyNotifications()` directly, then assert via
  both the repository and the real GET endpoint that the Secondary received a notification and the
  Primary (the subject) didn't get one about themself; call the real payment-request POST endpoint
  and cross-check; call `SubscriptionLifecycleScheduler.expireLapsedTrials()` then
  `NotificationGenerationService.generateLapsedSubscriptionNotifications()` directly (backdated
  trial, same pattern `SubscriptionsFlowIntegrationTest` already uses) and confirm both members of
  the account get a `SUBSCRIPTION` notification, checked both ways.

## 9. Non-goals (explicit)

- Any real Expo Push SDK call, HTTP request to Expo's push API, or device push token storage —
  `NotificationService.create(...)` is the entry point a future integration calls after persisting,
  same as `SubscriptionService.activate`'s relationship to a future payment webhook.
- A "trial expiring soon" advance-reminder notification (the frontend mock shows one, but the PRD
  doesn't specify pre-expiry reminder copy or timing, and building it needs a new idempotency
  mechanism — a time-windowed "already reminded this account in the last N days" check — not
  justified by anything that already exists, unlike the lapsed-trial trigger which reuses an
  existing, exactly-once transition). Only the lapsed-trial (already-happened) notification is
  built this pass.
- Any actual payment processing behind `payment-request` — it only ever creates a notification row.
- Push read-receipts, delivery tracking, or notification preferences/muting — not requested, no
  PRD section covers them.
