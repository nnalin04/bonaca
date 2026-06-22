# Backend Status & Next Steps

A status report, not an implementation plan — for that level of detail per feature, see
[`METRICS_IMPLEMENTATION_PLAN.md`](METRICS_IMPLEMENTATION_PLAN.md),
[`SUBSCRIPTIONS_IMPLEMENTATION_PLAN.md`](SUBSCRIPTIONS_IMPLEMENTATION_PLAN.md),
[`NOTIFICATIONS_IMPLEMENTATION_PLAN.md`](NOTIFICATIONS_IMPLEMENTATION_PLAN.md), and
[`BACKEND_TESTING_AND_PACKAGING_PLAYBOOK.md`](BACKEND_TESTING_AND_PACKAGING_PLAYBOOK.md). This doc
answers three questions directly: **what's left**, **is the frontend actually wired to any of
this**, and **what, specifically, needs you** (an account, a key, a business decision) **rather
than more code**.

## TL;DR

- **Auth and Members & Sharing are fully wired** frontend-to-backend. Login, OTP, complete-profile,
  invite, member list/detail, edit permissions, hidden members all call the real backend.
- **Metrics, Subscriptions, and Notifications backends exist and are fully tested, but the
  frontend has never been connected to any of them.** Their screens still read hardcoded
  mock arrays — built this session, but zero frontend wiring work has happened yet.
- **Run the app today (simulator + backend + Docker) and**: sign-up/login/OTP, completing your
  profile, inviting a family member, and managing permissions will all genuinely work against
  Postgres. Home, Member Detail's metrics, the Notifications screen, and the Payment Gateway
  screen will all still show static placeholder data or do nothing when tapped — **not because
  anything is broken, but because nobody has connected those specific wires yet.**
- The two genuinely external blockers (real wearable data, real OTP SMS) need you, specifically —
  see §5. Everything else left is just more of the work already underway in this repo.

## 1. Frontend ↔ Backend wiring — what's actually connected right now

| Screen / flow | Calls real backend? | Notes |
|---|---|---|
| Splash, Login (phone entry), OTP verify | ✅ Yes | `src/lib/api/auth.ts` — `/api/v1/auth/otp/*` |
| Complete Profile (onboarding) | ✅ Yes | `/api/v1/members/complete-profile` |
| Home (member list, self + shared) | ✅ Yes | via `useMembers` → `/api/v1/members` |
| Member Details (name/role/pinned/nickname) | ✅ Yes | `/api/v1/members/{id}` — but the metrics/insights shown on this same screen are still `mockData` (see below) |
| Invite Member, Hidden Members, Edit Permissions | ✅ Yes | `/api/v1/invites`, `/api/v1/sharing-grants` |
| Member Detail's **metrics cards / Metric Detail charts** | ❌ No | `src/features/metrics/mockData.ts` — the real `metrics` backend (built this session) is never called |
| Notifications screen | ❌ No | hardcoded array in `NotificationsScreen.tsx` — the real `notifications` backend is never called |
| Profile screen (subscription status, wearable connection) | ❌ No | no `@/lib/api` import at all — purely local/mock `Member`/`WearableConnection` data |
| Payment Gateway screen | ❌ No | "Make Payment" button's `onPress` is an empty stub — see `PaymentGatewayScreen.tsx` |
| Select Wearable Account screen | ❌ No | static mock list of providers |

There is no `src/lib/api/metrics.ts`, `subscriptions.ts`, or `notifications.ts` yet — only
`auth.ts` and `members.ts` exist. **This is the single biggest reason the app won't feel
"finished" even once you wire up Docker and the backend**: three fully-built, fully-tested
backend features have no frontend caller at all yet. That wiring (new `src/lib/api/*.ts` files +
swapping each screen's mock data for the real hook) is itself a real chunk of remaining work, not
a checkbox — it hasn't been scoped or started.

## 2. "Will it work perfectly if I launch the simulator + backend + Docker?" — concrete answer

**Will genuinely work, real data, real Postgres:**
- `docker compose up -d` (in `backend/`) → Postgres on `localhost:5432`.
- `./mvnw spring-boot:run` (or your IDE) → backend on `localhost:8080`, matching `.env`'s
  `EXPO_PUBLIC_API_BASE_URL=http://localhost:8080`.
- Sign up with any phone number → **OTP is not actually sent as an SMS** (see §5.1) — it's printed
  to the backend's console log (`[dev-otp] OTP for +91... is 1234`), which you read and type into
  the app. This is intentional dev behavior, not a bug.
- Complete profile, become Primary, invite a Secondary (needs a second phone number / simulator
  instance to accept), edit permissions, pin/nickname members — all real, all persisted.

**Will not work / will show placeholder data, because nothing's wired yet (not because it's broken):**
- Home's metric-deviation counts and Member Detail's metric cards — static mock numbers, not your
  real Postgres data (there's no real data anyway — see §5.2).
- The Notifications screen — same 4 hardcoded notifications every time, never reflects anything
  the backend actually generated.
- The Profile screen's subscription/wearable status — local mock state, not the real
  `TRIAL`/`ACTIVE`/`EXPIRED` status the backend is actually tracking for your account.
- Tapping "Make Payment" / "Add Payment Method" on the Payment Gateway screen — does nothing at
  all right now (empty `onPress`). See §4 below for the agreed dummy-payment plan.

## 3. Per-feature status

### Members & Sharing

**Built and wired to the frontend (the one feature where this is fully true):** phone+OTP auth,
complete-profile (Primary on first signup, Secondary via invite), 2-Secondary-Member cap, member
list/detail/update (pin, nickname, hidden), invites, sharing grants (3 scopes, instant-apply, no
batched Save step) — all matching `docs/PRD.pdf` already (role naming, scope count, cap, and
default-all-on permissions were all realigned during this work; `CLAUDE.md`'s "Realignment note"
section is now stale on these specific points and should be updated/trimmed).

**Left to build:**
- Subscription gating isn't enforced here — `SubscriptionService.isActive(accountId)` exists and
  is tested, but nothing in `members` calls it. PRD §12 says a lapsed subscription should pause
  sharing; right now sharing keeps working regardless of subscription state.
- Frontend wiring for Profile screen's subscription status (not a members-backend gap, but lives
  in the same screen).

**Missing from PRD:** nothing left in this feature maps to an unbuilt PRD requirement — it's the
most complete piece.

### Metrics

**Built:** schema (`metric_readings`, `metric_baselines`, `insights`), nightly baseline
recompute + insight generation (trend text, Routine Consistency Score, the new ANOMALY
classification), scope-gated read API (member summary, metric detail with 24h/7d/30d ranges,
insights list) — all tested, ~99% coverage.

**Left to build:**
- **No real data source.** `MetricIngestionService.recordReading(...)` is a plain internal method
  — nothing calls it except tests. Until the Spike API client exists (§5.2), the metrics backend
  has correct logic operating on zero real readings.
- No write/ingestion REST endpoint exists by design (per the plan doc) — a real Spike webhook
  handler is what would call `recordReading`, not a public endpoint.
- Frontend wiring (`src/lib/api/metrics.ts` + swapping `mockData.ts` usages in Member Detail /
  Metric Detail screens) — not started.

**Missing from PRD / market research:**
- `docs/MARKET_RESEARCH.md` §"Recommendation" (alert fatigue section) recommends a **severity/
  confidence score** on each `Insight` so the notification feed can rank "3 days elevated" above
  "1 reading elevated" instead of firing everything at equal weight. Not built — `Insight` has no
  confidence/severity field today, only `kind` (TREND/ANOMALY) and free text. Worth doing before
  real wearable data starts producing real anomaly volume.
- Same source recommends time-window aggregation of repeated anomaly triggers — partially already
  true by construction (one `Insight` row per member/metric/day, upserted not duplicated), but
  there's no rolling multi-day "still elevated" awareness beyond that day's row.

### Subscriptions

**Built:** trial start (7 days, on first Primary signup), status query endpoint, lifecycle
transitions (`activate`/`markExpiring`/`markExpired`/`cancel`) as internal entry points, nightly
lapsed-trial expiry job — tested, ~99% coverage.

**Left to build:**
- **No real payment processor** — explicitly blocked pending the cards-vs-Apple-StoreKit decision
  (§5.3). `activate()`/`cancel()` exist for a future payment confirmation to call; nothing calls
  them yet except tests.
- Per your latest message: a **dummy "Make Payment" flow** is now in scope as an interim step —
  see §4, design proposed but not yet built.
- Subscription gating not enforced in `members`/`metrics` (same item as above, cross-feature).
- Frontend wiring (`src/lib/api/subscriptions.ts`, Profile screen, Payment Gateway screen) — not
  started.

**Missing from PRD:** PRD §9 (Flow B, Delegated Pay) describes a Secondary paying on behalf of a
Primary as part of onboarding, with payment interleaved into the flow rather than a separate later
step. The current `payment-request` notification (built this session) covers the "ask someone to
pay" messaging half of that, but the actual onboarding-interleaved payment UX isn't built (depends
on real payments existing first).

### Notifications

**Built:** in-app notification storage, all 3 triggers (metric-anomaly, lapsed-subscription,
user-triggered payment-request), read/unread, scope-gated fan-out — tested, ~99% coverage.

**Left to build:**
- **No real push delivery** — `NotificationService.create(...)` persists a row; nothing calls
  Expo's push API. By design, per `CLAUDE.md`'s explicit blocker (§5.4).
- No "trial expiring soon" advance-reminder notification — deliberately deferred (see the plan
  doc's non-goals); only the already-lapsed notification fires today.
- Frontend wiring (`src/lib/api/notifications.ts`, swapping `NotificationsScreen.tsx`'s hardcoded
  array) — not started.

**Missing from PRD:** nothing notifications-specific beyond what's listed in metrics' confidence-
score gap above (notifications would consume that score once it exists, to badge/sort the list).

## 4. The dummy payment plan (agreed design, not yet built)

Per your instruction: real payment integration is deferred, but the app should *behave* as if
payment succeeded when "Make Payment" is tapped, so the rest of the product (trial → active →
renewal flows) can be built and demoed end-to-end without waiting on a resolved payment-processor
decision.

Proposed shape (will implement once you confirm):
- A new, **clearly dev-only** endpoint — e.g. `POST /api/v1/accounts/{accountId}/subscription/mock-pay`
  — that just calls the already-built `SubscriptionService.activate(accountId, Instant.now())`.
  No card details, no processor call, no real money. Guarded so it can never run in a `prod`
  Spring profile (same `@Profile("!prod")` pattern already used for `LoggingOtpSender`), so
  there's no risk of it accidentally shipping as a real "free activation" button.
- Frontend: `src/lib/api/subscriptions.ts` (new file) gets a `mockPay(accountId)` call; the
  Payment Gateway screen's `onPress` calls it and routes back with a success state.
- Clearly labeled in code (comment + maybe a visible "TEST MODE" badge in the UI, your call) so
  it's never mistaken for the real thing later.

This is a small, well-scoped piece — say the word and I'll build both halves (backend endpoint +
frontend wiring) next.

## 5. What needs *you*, specifically — not more of my work

### 5.1 OTP SMS (MSG91 DLT template registration)

Right now OTPs are logged to the console, not texted — fine for dev, useless for anyone but you
testing on your own machine. To send real SMS in India, MSG91 requires a **DLT (Distributed Ledger
Technology) template** — a government telecom regulatory registration — approved *before* any
template can send. This needs:
- A registered business entity (India DLT registration is tied to a business, not an individual).
- Submitting the exact SMS template text through MSG91's dashboard for approval (multi-day lead
  time, outside anyone's control once submitted).
- An MSG91 account + API key once approved.

I can't do any of this — it requires your business identity and MSG91 account access. Once you
have an API key and an approved template ID, I wire `Msg91OtpSender` (the class already exists as
a stub) in about an hour.

### 5.2 Spike API (real wearable data)

This is the biggest one, and worth explaining plainly since you asked: **Spike isn't a wearable
brand** — it's a third-party cloud service that already integrates with Garmin, Fitbit, Samsung,
Oura, etc., so Bonaca doesn't have to build and maintain a separate OAuth integration with each
vendor. The user connects their wearable through Spike's hosted flow once; Spike syncs that
vendor's data to its own cloud, and pushes it to Bonaca (webhook or polling — to be decided when
building the client).

What needs you:
- Signing up for a Spike developer account and agreeing to their terms (a business decision/
  account I can't create on your behalf).
- Picking a pricing tier — `CLAUDE.md` notes Spike charges per connected device, so this has a
  real cost implication worth your sign-off, not just a technical one.
- Getting an API key/secret from Spike's dashboard and giving it to me (as an environment
  variable, never committed to git, same pattern as `.env`'s `FIGMA_API_TOKEN`).
- Confirming which wearable vendors to support first (all of Garmin/Fitbit/Samsung/Oura, or a
  smaller initial set) — affects how much OAuth-config setup is needed on Spike's side.

Once I have a key, I build the client, the webhook receiver (or polling job), and wire it to the
existing `MetricIngestionService.recordReading(...)` entry point — the metrics backend's logic
side is already done and tested; it's purely waiting on a real data source.

### 5.3 Real payments (when you're ready to move past the dummy flow)

Blocked on one open business question first (not something I can resolve for you): PRD §6 says
"cards (global) + UPI only," but Apple requires StoreKit/in-app-purchase for iOS digital
subscriptions almost everywhere, including India — so either iOS needs a StoreKit carve-out, or
the PRD's claim needs revisiting for iOS specifically. Once you decide:
- **Cards + UPI route (Android, and web if ever built)**: needs a payment processor account —
  Razorpay is the natural fit given the India-first framing (UPI support, used elsewhere in
  Indian fintech) — business KYC verification, API key + secret, and a webhook signing secret.
- **StoreKit route (iOS)**: needs an active Apple Developer Program enrollment, in-app-purchase
  products configured in App Store Connect, and App Store review for the subscription flow.

Either way, I need the resulting API keys/credentials from you before building the real
integration — same as Spike, these are accounts only you can open.

### 5.4 Expo Push (real push notifications)

Lower-effort than the above. If the app's already using EAS (it should be, given the Expo/Router
setup), this mostly needs you to run `eas credentials` once (or confirm you want me to walk you
through it) to provision an Apple Push key and/or FCM credentials through Expo's tooling — Expo
manages most of the complexity here. No new third-party account beyond what Expo/EAS already
needs for builds.

### 5.5 Sentry + PostHog

Smallest lift: free-tier sign-ups for both, then give me the Sentry DSN and PostHog API key (env
vars again). I wire the SDKs in on both frontend and backend once I have them.

## 6. Suggested order of next work

1. **Subscription gating wiring** — smallest, fully self-contained, no external dependency
   (`isActive()` already built/tested; just needs to be called from `members`/`metrics`).
2. **Dummy payment flow** (§4) — small, unblocks demoing the full trial→pay→active loop.
3. **Frontend wiring for metrics/subscriptions/notifications** — the single biggest "make the app
   feel real" lift; doesn't need any of your accounts, purely connecting screens that already
   exist to backends that already exist.
4. **Confidence/severity scoring on Insight** (§3 Metrics) — improves notification quality before
   real anomaly volume starts arriving from Spike.
5. Everything in §5 — sequenced by your priority, each gated on you obtaining an account/key first.
