# Bonaca — What's Built, What's Left, and What It Costs

_Last updated 2026-06-26. Source of truth for product decisions: [`docs/PRD.md`](PRD.md) / `docs/PRD.pdf`._

---

## Part 1 — External APIs and Paid Resources

### 1.1 MSG91 — OTP SMS delivery

| | |
|---|---|
| **What it does** | Sends the 4-digit login OTP to the user's phone via SMS |
| **Cost** | ~₹0.15 per OTP |
| **Code status** | ✅ Fully implemented. Activates when `BONACA_MSG91_AUTH_KEY` env var is set on Oracle. Falls back to logging OTP to console when not configured. |

**External setup required (do this now — ~1 week lead time):**

1. Create an account at [msg91.com](https://msg91.com)
2. Go to **Sender ID** → apply for sender ID `BONACA` (or `BNCA` — 6 chars max). Takes 1–3 business days.
3. Go to **DLT Registration** in the MSG91 dashboard. You must register on TRAI's DLT portal (through one of the telecom operators — Airtel, Vodafone/Vi, BSNL, etc.):
   - Entity name: your company name
   - Entity type: Principal Entity
   - Registration takes 2–7 business days
4. Once entity is approved, register the template. Exact text (do not deviate):
   ```
   Your Bonaca OTP is ##VAR1##. Valid for 5 minutes. Do not share with anyone.
   ```
   Template type: **Transactional**. Header: your approved sender ID.
5. After both entity + template are approved, get your **Auth Key** from MSG91 dashboard → API → Auth Key.
6. Add to Oracle `~/bonaca/deploy/.env`:
   ```
   BONACA_MSG91_AUTH_KEY=<your-auth-key>
   BONACA_MSG91_TEMPLATE_ID=<your-template-id>
   ```
7. Restart container (no rebuild needed): `cd ~/bonaca/deploy && docker compose up -d --force-recreate backend`

---

### 1.2 Spike API — Wearable data sync

| | |
|---|---|
| **What it does** | Syncs health data (heart rate, steps, sleep, HRV, SpO₂, workouts etc.) from Garmin, Fitbit, Samsung Health, Oura, Apple Watch, and 30+ other wearables via their cloud |
| **Cost** | Billed per connected device/month. Contact Spike for pricing (~$1–3/device/month at small scale) |
| **Code status** | ✅ Fully implemented. Backend: `WearableController`, `SpikeApiClient`, `SpikeWebhookController`, `WearableService`, `wearable_connections` table. Frontend: `ConnectWearableScreen`, `SelectWearableAccountScreen` open Spike's OAuth URL in the device browser. Activates when `BONACA_SPIKE_API_KEY` env var is set. |

**External setup required:**

1. Sign up at [spike.sh](https://spike.sh) → request API access (it may be invite/waitlist). Email them at their contact page explaining your use case.
2. Once approved, in the Spike dashboard:
   - Go to **API Keys** → create a key. Copy it.
   - Go to **Webhooks** → add a new webhook URL: `https://bonaca.vercel.app/api/v1/webhooks/spike`
   - Note the **Webhook Secret** shown after creation.
3. Create a **Razorpay Plan** (or skip if doing manual billing) — Spike may require you to map users to your system's concept of a "team user". Follow their team API docs.
4. Add to Oracle `~/bonaca/deploy/.env`:
   ```
   BONACA_SPIKE_API_KEY=<your-spike-api-key>
   BONACA_SPIKE_WEBHOOK_SECRET=<your-webhook-secret>
   ```
5. Rebuild and redeploy (new code needed):
   ```bash
   # On your Mac, from the Bonaca project root:
   # Build a fresh tarball and upload (or let CI do it once set up)
   cd ~/bonaca/deploy && bash deploy.sh ~/bonaca/source
   ```

**How the connect flow works after setup:**
- User taps a wearable brand in the app
- App calls `POST /api/v1/members/{memberId}/wearable/connect`
- Backend creates a Spike "team user" and returns a `connectUrl`
- App opens the `connectUrl` in the device browser (Spike's hosted OAuth page)
- User logs in with their Fitbit/Garmin/etc account on Spike's page
- Spike POSTs data to `https://bonaca.vercel.app/api/v1/webhooks/spike`
- Backend receives it, validates the Spike signature, writes `MetricReading` rows, updates baselines

**Spike webhook event mapping (implemented):**

| Spike event type | Bonaca MetricType |
|---|---|
| `heart_rate.*` | `HEART_RATE` (bpm) |
| `daily.*` with `steps` in payload | `STEPS` (steps) |
| `sleep.*` | `SLEEP` (hours) |
| `workout.*` | `WORKOUTS` (count) |

---

### 1.3 Razorpay — Payment processing

| | |
|---|---|
| **What it does** | Charges users ₹249/month via credit/debit card or UPI, handles recurring billing with webhooks |
| **Cost** | ~2% per transaction (~₹5/subscriber/month) |
| **Code status** | ✅ Fully implemented. Backend: `PaymentController` (`POST /payment-link`), `RazorpayWebhookController` (`POST /webhooks/razorpay`), `PaymentService`, `payment_events` table for idempotent event processing. Frontend: `PaymentGatewayScreen` opens Razorpay's hosted payment link in the browser. Mock payment button still present for dev testing. |

**External setup required:**

1. Sign up at [razorpay.com](https://razorpay.com) → complete KYC (requires PAN, bank account, business details — ~2–5 business days for activation).
2. In the Razorpay Dashboard:
   - **Test mode first** (safe, no real charges)
   - Go to **Settings → API Keys** → Generate Key → copy `Key ID` and `Key Secret`
3. Create a **Plan** (one-time):
   - Go to **Subscriptions → Plans → Create Plan**
   - Name: `Bonaca Monthly`
   - Billing amount: `24900` (in paise — ₹249 × 100)
   - Billing period: `Monthly`
   - Copy the **Plan ID** (looks like `plan_XXXXXXXXXXXXXXX`)
4. Set up **Webhooks**:
   - Go to **Settings → Webhooks → Add Webhook**
   - URL: `https://bonaca.vercel.app/api/v1/webhooks/razorpay`
   - Secret: generate a random string (e.g. `openssl rand -hex 32`), save it
   - Events to subscribe: ✅ `subscription.activated`, ✅ `subscription.charged`, ✅ `subscription.halted`, ✅ `subscription.cancelled`, ✅ `subscription.completed`
5. Add to Oracle `~/bonaca/deploy/.env`:
   ```
   BONACA_RAZORPAY_KEY_ID=rzp_test_XXXXXXXXXXXXXXX
   BONACA_RAZORPAY_KEY_SECRET=<your-key-secret>
   BONACA_RAZORPAY_WEBHOOK_SECRET=<your-webhook-secret>
   BONACA_RAZORPAY_PLAN_ID=plan_XXXXXXXXXXXXXXX
   ```
6. Rebuild and redeploy (new code needed — same as Spike above)
7. When ready for production: switch to Live mode in Razorpay, generate Live API keys, replace the env vars.

**iOS caveat (open question):** Apple requires iOS digital subscriptions to use StoreKit/IAP. Razorpay direct checkout may violate App Store policy for a published iOS app. For beta testing with friends via TestFlight, this won't be enforced. Resolve before App Store submission.

**How checkout works after setup:**
- User taps "Pay ₹249/month" in the app
- App calls `POST /api/v1/accounts/{accountId}/subscription/payment-link`
- Backend creates a Razorpay subscription, returns `paymentLink`
- App opens the link in the device browser (Razorpay's hosted checkout)
- User enters card/UPI details and pays
- Razorpay POSTs `subscription.activated` webhook to `https://bonaca.vercel.app/api/v1/webhooks/razorpay`
- Backend verifies signature, calls `SubscriptionService.activate()`, saves `PaymentEvent` (idempotent)

---

### 1.4 Expo Application Services (EAS) — Mobile builds

| | |
|---|---|
| **What it does** | Compiles iOS and Android native binaries in the cloud, delivers OTA JS updates without App Store review |
| **Cost** | Free tier: 15 iOS + 15 Android builds/month. $19/month Starter once real users. |
| **Code status** | ⚠️ `eas.json` not yet created. One command away from a shareable Android APK. |

**Setup (15 minutes):**

1. `npm install -g eas-cli`
2. `eas login` (use your Expo account)
3. `eas build:configure` — this generates `eas.json`. Then edit it:
   ```json
   {
     "build": {
       "preview": {
         "android": { "buildType": "apk" }
       },
       "production": {
         "android": { "buildType": "app-bundle" },
         "ios": {}
       }
     }
   }
   ```
4. Build Android APK (shareable link, no Play Store needed):
   ```bash
   eas build --platform android --profile preview
   ```
   Takes ~10–15 minutes. EAS sends you a download link. Share the `.apk` file directly with testers.
5. For iOS: requires Apple Developer Program ($99/year) + provisioning. Add testers via TestFlight.

---

### 1.5 Apple Developer Program — iOS distribution

| | |
|---|---|
| **Cost** | $99/year |
| **Code status** | ❌ Not enrolled |

**Setup:**
1. Enroll at [developer.apple.com](https://developer.apple.com/programs/enroll/) — requires Apple ID + payment
2. Apple processes enrollment in ~2–3 business days
3. After enrollment: `eas build --platform ios --profile preview` → upload to TestFlight
4. Add testers by email in App Store Connect

---

### 1.6 Expo Push Notification Service

| | |
|---|---|
| **Cost** | Free |
| **Code status** | ❌ Not integrated. In-app notification records are written to DB but nothing wakes the device. |

**When to build:** After Spike is wired and real insights start generating. Priority is lower than Spike + payments for the first test run.

**What needs implementing:**
- Register Expo push token on app launch, store per-user via a new `POST /api/v1/members/me/push-token` endpoint
- `NotificationGenerationService` calls Expo Push API when writing a new `Notification` row
- ~1 day of work

---

### 1.7 Sentry + PostHog — Observability and analytics

| | |
|---|---|
| **Cost** | Both free tier |
| **Code status** | ❌ Not integrated |

Both are ~2–4 hours of setup each. Not blocking the first test run. Add after the core data pipeline is proven.

---

### 1.8 Oracle Cloud + Vercel — Hosting

| | |
|---|---|
| **Cost** | Both free |
| **Code status** | ✅ Deployed and healthy. Backend at `https://bonaca.vercel.app/api/v1/*` |

---

## Part 2 — What's Built vs What's Left

### Backend

#### ✅ Fully built and deployed

| Area | Status |
|---|---|
| Auth (phone+OTP, JWT, rate limiting) | ✅ Live on Oracle |
| Members (profile, pin/hide, nickname) | ✅ Live |
| Invites + sharing grants | ✅ Live |
| Metrics schema + baselines + insights (logic layer) | ✅ Live — awaiting real data from Spike |
| Notifications (in-app records) | ✅ Live — awaiting real insights |
| Subscriptions (trial/active/expiring/expired lifecycle) | ✅ Live |
| Mock payment endpoint (dev only) | ✅ Live |
| Spike API integration | ✅ Code written, needs env vars + redeploy |
| Razorpay payment integration | ✅ Code written, needs env vars + redeploy |
| Proxy secret filter + health endpoint | ✅ Live |

#### ❌ Not built

| Area | Notes |
|---|---|
| Expo push notifications | Backend webhook → device push. ~1 day. |
| Push token storage endpoint | `POST /api/v1/members/me/push-token`. ~2 hours. |
| Sentry / PostHog | ~2–4 hours each. |

#### ⚠️ Built with stale data model (not blocking beta)

| Issue | Impact |
|---|---|
| Role naming inverted (Primary = adult child in code, should be parent) | Labels show wrong names; functionality works |
| No 2-Secondary-Member cap in `InviteService` | Can create more than 2 invites |
| Trial is 7 days in `SubscriptionService.TRIAL_DAYS` | ✅ Already correct |
| Permissions: 4 scopes (incl. `location`) + batched save + role-dependent defaults | Doesn't match PRD §11; functional but UX differs from design |

---

### Frontend

#### ✅ Fully built + connected to real backend

| Screen | Status |
|---|---|
| Splash → Login → OTP → Complete Profile | ✅ Wired to real auth API |
| Home (member list) | ✅ Member data from real API; metric values mocked |
| Profile + Profile Details | ✅ Real data |
| Notifications list | ✅ Pulls from real DB (empty until Spike sends data) |
| Subscriptions screen | ✅ Real subscription state |

#### ✅ Newly wired (this session)

| Screen | Status |
|---|---|
| Connect Wearable (onboarding) | ✅ Calls Spike via API, opens connect URL in browser |
| Select Wearable Account | ✅ Calls Spike via API, shows connection status |
| Payment Gateway | ✅ Opens real Razorpay payment link; mock button kept for dev |

#### ⚠️ Screens built but data still mocked

| Screen | Why |
|---|---|
| Member Details — metric cards | Waiting for Spike data to flow into DB |
| Metric Details — charts | Same; will auto-populate once Spike webhooks deliver readings |

#### ❌ Not built

| Feature | Notes |
|---|---|
| Push notification permission + token registration | On app launch, request permission and POST token to backend |
| Sentry error boundary | Wrap root layout |
| PostHog event tracking | Key funnel events |

---

## Part 3 — First Test Run Path

### Minimum viable path (Android, with a friend)

**Step 1 — DLT registration** (start today, ~1 week):
- While waiting, you can relay OTPs manually: `ssh -i ~/.ssh/oracle_vm.key ubuntu@80.225.223.142 "docker logs -f bonaca-backend 2>&1 | grep dev-otp"`

**Step 2 — Build APK** (once `eas.json` is added):
```bash
eas build --platform android --profile preview
```
Produces a shareable download link. Your friend installs the `.apk` directly.

**Step 3 — Configure env vars on Oracle** (once API keys are ready):

Edit `~/bonaca/deploy/.env` on Oracle, then:
```bash
cd ~/bonaca/deploy && docker compose up -d --force-recreate backend
```
No rebuild needed — Spring picks up env vars at startup.

**What will work on the test run:**
- Full auth flow (OTP via logging until DLT approved)
- Profile, home, notifications screens
- Wearable connect button → opens Spike page in browser
- Payment button → opens Razorpay checkout in browser
- Metric screens (empty until wearable connects and data flows)

**Step 4 — Seed metric data for the demo** (optional, shows the full UI):
```sql
-- SSH to Oracle, then:
docker exec -it infrastructure-postgres-1 psql -U bonaca bonaca

-- Replace <member-uuid> with your member ID from the members table:
INSERT INTO metric_readings (member_id, metric_type, metric_value, unit, recorded_at, source_device_id)
VALUES
  ('<member-uuid>', 'HEART_RATE', 72, 'bpm', NOW(), 'seed'),
  ('<member-uuid>', 'STEPS', 8400, 'steps', NOW(), 'seed'),
  ('<member-uuid>', 'SLEEP', 7.2, 'hours', NOW(), 'seed');
```
The baseline + insight schedulers run at 02:00/03:00/04:00 daily, or trigger manually via `curl -X POST http://localhost:8090/api/v1/admin/metrics/rollup` (if you add that endpoint later).

### iOS path

Same steps, but add before Step 2: enroll in Apple Developer Program ($99/year), configure EAS for iOS, build and upload to TestFlight, invite tester by email.

---

## Quick Reference — Oracle `.env` keys

```bash
# Required — already set
BONACA_DATABASE_URL=jdbc:postgresql://postgres:5432/bonaca
BONACA_DATABASE_USER=bonaca
BONACA_DATABASE_PASSWORD=<set>
JWT_SECRET=<set>
BACKEND_SECRET=<set>

# SMS — set after DLT approval
BONACA_MSG91_AUTH_KEY=
BONACA_MSG91_TEMPLATE_ID=

# Wearable sync — set after Spike access granted
BONACA_SPIKE_API_KEY=
BONACA_SPIKE_WEBHOOK_SECRET=

# Payments — set after Razorpay KYC + plan creation
BONACA_RAZORPAY_KEY_ID=
BONACA_RAZORPAY_KEY_SECRET=
BONACA_RAZORPAY_WEBHOOK_SECRET=
BONACA_RAZORPAY_PLAN_ID=
```

---

## Status Summary

| | Status |
|---|---|
| Auth (OTP login) | ✅ Working (SMS pending DLT, OTP logged to console) |
| Profile, Members, Invites | ✅ Working |
| Spike wearable integration (code) | ✅ Written, needs API key + redeploy |
| Razorpay payment integration (code) | ✅ Written, needs API keys + redeploy |
| Metric data pipeline | ⏳ Waiting for Spike API key |
| Insights + in-app notifications | ⏳ Waiting for Spike data |
| Push notifications | ❌ Not built |
| Android build (APK) | ⚠️ `eas.json` needed, then one command |
| iOS build (TestFlight) | ❌ Needs $99 Apple Developer account |
| MSG91 real SMS | ⏳ Waiting on DLT registration |
| Sentry / PostHog | ❌ Not integrated |
