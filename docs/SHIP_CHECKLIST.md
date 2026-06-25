# Bonaca — What's Built, What's Left, and What It Costs

_Written 2026-06-26. Source of truth for product decisions: [`docs/PRD.md`](PRD.md) / `docs/PRD.pdf`._

---

## Part 1 — External APIs and Paid Resources

Every third-party integration the product needs, with cost and blocking status.

### 1.1 MSG91 — OTP SMS delivery

| | |
|---|---|
| **What it does** | Sends the 4-digit login OTP to the user's phone number via SMS |
| **Why MSG91** | Cheapest for Indian numbers (~₹0.15/OTP), built-in DLT workflow |
| **Cost** | ~₹0.15 per OTP. 1,000 test users × ~5 OTPs each = ~₹750 to launch |
| **Status** | ✅ Code written and deployed. Activates when `BONACA_MSG91_AUTH_KEY` env var is set on Oracle. Currently falling back to logging. |
| **Blocker** | **DLT registration is mandatory before any SMS can be sent to Indian numbers.** TRAI will silently reject SMS without it. Steps: (1) Create MSG91 account, (2) Register your entity on DLT portal (~2–7 business days), (3) Apply for sender ID `BONACA`, (4) Register the exact OTP template: `"Your Bonaca OTP is ##VAR1##. Valid for 5 minutes. Do not share with anyone."` (~1–3 days). Only after approval: add `BONACA_MSG91_AUTH_KEY` and `BONACA_MSG91_TEMPLATE_ID` to Oracle `.env` and restart the container. |
| **Signup** | msg91.com |

---

### 1.2 Spike API — Wearable data sync

| | |
|---|---|
| **What it does** | Syncs health data from Garmin, Fitbit, Samsung Health, Oura, Apple Watch etc. from the vendor's cloud into Bonaca — no requirement for the parent to run a sync task on their own phone |
| **Why Spike** | Only cross-vendor cloud aggregator that doesn't require on-device SDK per platform; one API covers all major wearables |
| **Cost** | Billed per connected device/month. Pricing on request — budget ~$1–3/device/month at small scale |
| **Status** | ❌ Not integrated. Backend has `MetricIngestionService`, `MetricReading`, `MetricBaseline`, `InsightGenerationService` models and DB schema ready, but no Spike API client calling them. All metric data is mocked in the frontend. |
| **What needs building** | Spike OAuth device-connect flow → webhook/polling ingest → `MetricIngestionService` → `MetricsRollupScheduler` (already scheduled at 02:00 daily) → baseline recalculation. Also the `ConnectWearableScreen` / `SelectWearableAccountScreen` front-end flows need wiring to real Spike auth URLs. |
| **Signup** | spike.sh — requires contacting them for API access and pricing |

---

### 1.3 Razorpay — Payment processing (cards + UPI)

| | |
|---|---|
| **What it does** | Charges users ₹249/month via credit/debit card or UPI, handles recurring billing |
| **Why Razorpay** | Best-in-class for India UPI + card support, webhook-driven subscription lifecycle, no per-transaction minimum |
| **Cost** | ~2% per transaction. At ₹249/month: ~₹5/subscriber/month |
| **Status** | ❌ Not integrated. Only `MockPaymentController` exists on the backend which creates a free trial subscription without charging. `PaymentGatewayScreen` is built visually but calls the mock. |
| **What needs building** | Razorpay Subscription API wired to `SubscriptionsController`, webhook handler for payment success/failure/expiry → `SubscriptionLifecycleScheduler` already runs at 03:00 daily. Payment Gateway screen wired to Razorpay's checkout SDK. |
| **iOS caveat** | **Unresolved:** Apple requires iOS digital subscriptions to go through StoreKit/IAP, not a direct card charge. Using Razorpay directly for iOS subscriptions likely violates App Store policy. Options: (a) StoreKit for iOS + Razorpay for Android/web, (b) web-only payment flow that sidesteps the App Store (user subscribes at bonaca.vercel.app, app checks entitlement), (c) accept the risk at small beta scale. This decision must be made before building payment integration. |
| **Signup** | razorpay.com |

---

### 1.4 Expo Application Services (EAS) — Mobile builds and OTA

| | |
|---|---|
| **What it does** | Compiles native iOS and Android binaries in the cloud (no local Xcode/Android Studio), delivers OTA JavaScript updates without App Store review |
| **Cost** | Free tier: 15 iOS + 15 Android builds/month — sufficient pre-launch. $19/month Starter once you have real users. |
| **Status** | ⚠️ EAS CLI configured, but no `eas.json` exists yet — one `eas build` command away from a testable binary. |
| **What needs building** | Add `eas.json`, run `eas build --platform android --profile preview` for a shareable APK. iOS requires the $99/year Apple Developer account for TestFlight distribution. |
| **Signup** | expo.dev — free, account exists via existing Expo usage |

---

### 1.5 Apple Developer Program — iOS distribution

| | |
|---|---|
| **What it does** | Required to distribute iOS builds (TestFlight for testing, App Store for production) |
| **Cost** | $99/year |
| **Status** | ❌ Not enrolled. Required before any friend/tester can install on iOS. |
| **What needs building** | Enroll at developer.apple.com, create App ID, provisioning profile, then configure EAS with the certificate. |

---

### 1.6 Expo Push Notification Service — In-app push alerts

| | |
|---|---|
| **What it does** | Delivers push notifications to iOS (via APNs) and Android (via FCM) from a single Expo API — no per-platform credentials needed |
| **Cost** | Free |
| **Status** | ❌ Not integrated. `NotificationGenerationService` and `NotificationsRollupScheduler` (04:00 daily) exist on the backend and generate in-database notification records, but nothing pushes to the device. `NotificationsScreen` shows in-app notifications from the DB; the push layer that wakes the user doesn't exist yet. |
| **What needs building** | Expo push token registration in the app, stored per-user on the backend, called from `NotificationGenerationService` when an insight fires. |

---

### 1.7 Sentry — Crash and error reporting

| | |
|---|---|
| **What it does** | Captures and reports crashes, unhandled exceptions, and errors from both the mobile app and backend |
| **Cost** | Free tier (5,000 errors/month) — sufficient pre-revenue |
| **Status** | ❌ Not integrated |
| **What needs building** | `@sentry/react-native` in the Expo app, Sentry Spring Boot SDK in the backend, DSN configured in environment. ~1 hour of work each. |
| **Signup** | sentry.io |

---

### 1.8 PostHog — Product analytics

| | |
|---|---|
| **What it does** | Tracks user funnels, events, and session data to measure PRD success metrics: invite-to-connect activation, weekly engagement, trial→paid conversion |
| **Cost** | Free tier (1M events/month) — sufficient pre-revenue |
| **Status** | ❌ Not integrated |
| **What needs building** | `posthog-react-native` in the Expo app, event calls on key actions (OTP verified, profile complete, wearable connected, metric viewed, invitation sent). ~2–4 hours. |
| **Signup** | posthog.com |

---

### 1.9 Oracle Cloud — Backend hosting

| | |
|---|---|
| **What it does** | Runs the Spring Boot backend container + PostgreSQL on an ARM64 VM |
| **Cost** | Free (Always Free tier: 4 OCPUs, 24 GB RAM ARM64). No cost as long as the account remains active. |
| **Status** | ✅ Deployed and healthy. Backend running at port 8090, proxied via Vercel at `https://bonaca.vercel.app/api/v1/*`. |

---

### 1.10 Vercel — HTTPS proxy

| | |
|---|---|
| **What it does** | Provides a TLS-terminated public HTTPS endpoint (`bonaca.vercel.app`) that forwards API calls to the Oracle VM (which has no public TLS of its own) |
| **Cost** | Free (Hobby plan) |
| **Status** | ✅ Deployed. Serverless function at `api/[...path].js` injects `X-Backend-Secret` header and forwards to Oracle. |

---

## Part 2 — What's Left to Build

### 2.1 Backend

#### ✅ Fully built and live

| Area | What's working |
|---|---|
| Auth | Phone+OTP login, JWT access/refresh tokens, OTP rate limiting and lockout, `/me` endpoint |
| Members | Create account on first login, complete profile (name, DOB, gender), list members, pin/unpin, hide/show, nickname, role (primary/secondary/tertiary — old naming) |
| Invites | Create invite, accept invite, creates Member + SharingGrant rows |
| Sharing permissions | GET/PUT sharing grants per member, per scope |
| Metrics schema | `metric_readings`, `metric_baselines`, `insights` tables in Postgres via Flyway |
| Metrics read API | `GET /api/v1/members/{id}/metrics` (summary) and `GET /api/v1/members/{id}/metrics/{type}` (detail with range) — returns data from DB |
| Baselines | `BaselineService` computes rolling 14–21 day baselines, runs via `MetricsRollupScheduler` at 02:00 daily |
| Insight generation | `InsightGenerationService` evaluates deviations against baselines, writes `Insight` rows |
| Notifications (in-app) | `NotificationGenerationService` creates notification records from insights, `NotificationsRollupScheduler` at 04:00 daily, `GET /api/v1/notifications` and PATCH mark-read |
| Subscriptions | `Subscription` model (trial/active/expiring/expired/cancelled), `SubscriptionLifecycleScheduler` at 03:00 daily, `GET /api/v1/subscriptions/me` |
| Infrastructure | Proxy secret filter, JWT filter, `/health` endpoint, Docker + docker-compose on Oracle, Vercel proxy |

#### ❌ Not built — blocking for first real test

| Area | What's missing | Effort |
|---|---|---|
| **Spike API integration** | Nothing calls Spike. `MetricIngestionService.ingest()` exists but has no source. Without this, all metric data is mocked or empty. The entire read path (Home metric summaries, Member Details, Metric Details) shows nothing real. | Large — need to read Spike docs, implement OAuth device-connect, set up webhook or polling loop, map Spike's response schema to `MetricReading` |
| **Real payment (Razorpay)** | `MockPaymentController` creates free trial subscriptions. No real charge, no webhook, no subscription lifecycle from payment events. | Large — but can be deferred for beta testing with friends using the mock |
| **Expo push notifications** | In-app notification records exist in DB. Nothing wakes the user's device when an insight fires. | Medium |
| **MSG91 DLT approval** | OTP code works but SMS won't deliver to Indian numbers until DLT entity + template registered. Currently falls back to logging OTP to Oracle console — workable for testing if you can see the logs. | Waiting on MSG91 (external, ~1 week turnaround) |

#### ⚠️ Built but using wrong data model (realignment needed, not blocking for beta)

| Area | Current state | PRD.pdf target |
|---|---|---|
| Role naming | `PRIMARY` = adult child/payer, `SECONDARY` = parent, `TERTIARY` exists | `PRIMARY` = parent/data owner, `SECONDARY` = adult child, no `TERTIARY` |
| Secondary Member cap | No cap enforced | Max 2 Secondary Members per Primary |
| Trial duration | 5 days (`MembersService.TRIAL_DAYS`) | 7 days |
| Sharing scopes | 4 scopes: `vitals`, `activity`, `behaviour`, `location` | 3 scopes: `vitals`, `activity`, `behaviour` (location is context within behaviour) |
| Permissions default | Role-dependent, not all-on | All-on by default |

These don't break a beta test — they just mean the role labels are inverted. Since you're testing with people you know and will explain the app to, this is fine to defer until post-beta.

---

### 2.2 Frontend

#### ✅ Fully built and connected to real backend

| Screen | Status |
|---|---|
| Splash | ✅ Built, navigates on auth state |
| Login (phone number entry) | ✅ Built, calls real OTP request API |
| OTP verification | ✅ Built, calls real verify API, handles wrong OTP error, resend |
| Complete Profile | ✅ Built, calls real complete-profile API |
| Connect Wearable (onboarding) | ✅ Screen built, but no real Spike connection — tapping a wearable doesn't do anything real yet |

#### ✅ Built but data is mocked

| Screen | Status |
|---|---|
| Home | ✅ Layout built, member list pulls from real API, but metric summaries (heart rate, steps etc.) are placeholder values |
| Member Details | ✅ Layout built, member identity pulls from real API, metric cards show mock data |
| Metric Details | ✅ Layout built with charts, but reads mock data |
| Notifications | ✅ Layout built, pulls from real notifications API (which will be empty until insights fire from real data) |
| Profile | ✅ Built, pulls real member data |
| Profile Details | ✅ Built, shows real profile fields |

#### ⚠️ Built but not wired

| Screen | Status |
|---|---|
| Invite Member | ✅ Screen built, calls invite API — but real Spike wearable connection is required for the invited member's data to appear |
| Edit Permissions | ✅ Screen built, calls sharing grants API — but uses old 4-scope model with batched save (doesn't match PRD's instant-apply 3-scope model) |
| Hidden Members | ✅ Screen built and wired |
| Payment Gateway | ✅ Screen built, calls `MockPaymentController` — no real Razorpay charge |
| Select Wearable Account | ✅ Screen built, no real Spike OAuth flow |
| Subscriptions | ✅ Screen built, reads real subscription state from backend |

#### ❌ Not built

| Screen / Feature | Notes |
|---|---|
| Spike OAuth device-connect flow | The "Connect Wearable" screen needs to open a Spike OAuth URL in a browser, handle the callback, and store the connection. Not yet built anywhere. |
| Real payment checkout | Razorpay SDK checkout sheet in-app, or web-based checkout redirect. Not built. |
| Push notification permission + token registration | App needs to request push permission at login and send Expo push token to backend. Not built. |
| Metric detail 24h/7d/30d range tabs | Currently built as 1D/7D/4W/1Y. PRD says 24h/7d/30d. Minor rename + data range fix. |

---

## Part 3 — What You Need for the First Test Run

**The minimum viable path to give this app to a friend and have them experience it:**

### If your friend has Android

1. **Register for DLT** (do this now — 1 week lead time). Until approved, you can test with a phone number you control by watching the Oracle logs for the OTP: `ssh -i ~/.ssh/oracle_vm.key ubuntu@80.225.223.142 "docker logs -f bonaca-backend 2>&1 | grep dev-otp"`

2. **Build an APK**:
   ```bash
   npx eas build --platform android --profile preview
   ```
   Requires `eas.json` (takes ~15 minutes to configure + build). Produces a shareable `.apk` link.

3. **What will work**: Splash → Login → OTP (you relay the code from logs) → Complete Profile → Home (empty, no wearable data) → Profile → Notifications (empty).

4. **What won't work**: Wearable data, real SMS OTP, payment.

### If your friend has iOS

Add step 0: Enroll in the Apple Developer Program ($99), then use EAS to add them as a TestFlight tester. Takes a few days for Apple to process.

### Spike workaround for the test

To show real metric data during a test without Spike integration, you can seed the database directly:

```sql
-- On Oracle, in the bonaca Postgres DB:
INSERT INTO metric_readings (member_id, metric_type, value, unit, recorded_at, source)
VALUES ('<member-uuid>', 'HEART_RATE', 72, 'bpm', NOW(), 'manual');
```

This lets you demo the metrics read path without waiting for Spike.

---

## Summary Table

| | Status |
|---|---|
| Auth (OTP login) | ✅ Working (SMS pending DLT) |
| Profile | ✅ Working |
| Members + Invites + Permissions | ✅ Working (old role naming) |
| Metric data pipeline | ❌ No data source (Spike not integrated) |
| Insights + Notifications (in-app) | ⚠️ Logic built, no data flowing |
| Push notifications | ❌ Not built |
| Payments | ❌ Mock only |
| Android build (APK) | ⚠️ `eas.json` needed, then one command |
| iOS build (TestFlight) | ❌ Needs $99 Apple Developer account |
| MSG91 real SMS | ❌ Waiting on DLT registration |
| Spike wearable sync | ❌ Not integrated |
| Razorpay billing | ❌ Not integrated |
| Sentry error reporting | ❌ Not integrated |
| PostHog analytics | ❌ Not integrated |
