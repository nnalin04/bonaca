# Bonaca — Full Codebase Audit Report

**Date:** 2026-06-26  
**Scope:** Frontend (React Native / Expo) + Backend (Spring Boot) + PRD gap analysis  
**Method:** Line-by-line read of every source file in `src/` and `backend/src/`

---

## Severity Legend

| Label | Meaning |
|---|---|
| 🔴 CRITICAL | Security vulnerability or broken core feature — must fix before any production traffic |
| 🟠 HIGH | Major functional gap, exploitable weakness, or silent data corruption |
| 🟡 MEDIUM | Wrong behavior, missing validation, or notable UX gap |
| 🔵 LOW | Code quality, style, or minor inconsistency |

---

## Part 1 — Security

### 🔴 CRITICAL — Webhook signature bypass when secret is blank

**Files:** `backend/.../payment/controller/RazorpayWebhookController.java:77` and `backend/.../wearable/controller/SpikeWebhookController.java:63–64`

```java
// Both files contain:
if (secret == null || secret.isBlank()) return true;   // accepts unauthenticated requests
```

`application.yml` defaults both `razorpay.webhook-secret` and `spike.webhook-secret` to `""`. In any environment where these env vars are not explicitly set (dev, staging, an accidentally misconfigured prod), **every POST to `/api/v1/webhooks/razorpay` and `/api/v1/webhooks/spike` is accepted without any HMAC verification**. An attacker who discovers the endpoint can:
- POST a fake `subscription.activated` event → activates a subscription for any `account_id` without payment.
- POST a fake Spike daily event → injects arbitrary metric readings (e.g., falsely-alarming heart rate values).

**Fix:** Return `false` (reject) when the secret is not configured, not `true`. For Razorpay and Spike, there is no safe reason to skip signature verification in any deployed environment.

---

### 🔴 CRITICAL — Hard-coded JWT secret fallback published in source control

**File:** `backend/src/main/resources/application.yml:19`

```yaml
secret: ${JWT_SECRET:s98slO6+8H7jnfAEqBtcK2/NVqdSKSWmPjPlqpFrGcs=}
```

The fallback value is a real-looking 44-character base64 string committed to the repository. Any staging or dev server that launches without `JWT_SECRET` set will use this known value. Since the key is public (in git history), anyone can sign valid JWTs for any `userId` and authenticate as any user.

**Fix:** Change the fallback to a clearly invalid sentinel (e.g., `CHANGE_ME`) and throw at startup via `@PostConstruct` if the value is the sentinel in any non-`local` profile.

---

### 🟠 HIGH — Proxy secret filter blocks webhook delivery in remote-dev

**File:** `backend/.../config/ProxySecretFilter.java` + `application-remote-dev.yml`

`remote-dev` profile has `proxy-security.enabled: true`. `ProxySecretFilter` intercepts **all** requests (except `/health`) and requires the `X-Backend-Secret` header. Razorpay and Spike cannot send this header — they don't know it exists. Result: in the remote-dev deployment, all webhook deliveries silently return `403` and no payments activate, no wearable data arrives. There is no `shouldNotFilter` exclusion for `/api/v1/webhooks/**`.

---

### 🟠 HIGH — OTP brute-force window is wider than it appears

**File:** `backend/.../auth/service/OtpService.java`

The stated defenses are:
- 5 OTP requests per phone per hour
- 5 verify attempts per OTP record before lockout

But these compose to allow 25 guesses per hour (5 fresh OTPs × 5 attempts each). On a 4-digit code (10,000 values), an attacker with many source IPs and multiple phones can cycle through accounts at a meaningful rate.

Additionally, the rate-limit check (`countByPhoneNumberAndCreatedAtAfter`) and the subsequent `save` are **not atomic** — under concurrent requests, two simultaneous "5th request" checks can both read count=4 and both succeed.

Recommendations: (1) Raise OTP length to 6 digits (default in `application.yml` is `codeLength: 4`). (2) Add a database `SELECT ... FOR UPDATE` or unique constraint to make the rate check atomic. (3) Consider IP-based rate limiting at the application or infrastructure layer.

---

### 🟠 HIGH — No rate limiting on any endpoint except OTP request

**Files:** All controllers

The following endpoints have zero rate limiting:
- `POST /api/v1/auth/refresh` — refresh token rotation can be called in a tight loop
- `GET /api/v1/members/{id}/metrics` — metric reads can be scraped freely with a stolen token
- All member, notification, and subscription endpoints

There is no Bucket4j, Spring Cloud Gateway, or nginx throttling configured.

---

### 🟡 MEDIUM — Access token not revocable on logout

**File:** `backend/.../auth/controller/AuthController.java` — `POST /logout`

Logout correctly revokes the refresh token in the database. However, the 15-minute JWT access token is not revocable (pure JWT, no DB check). A stolen access token remains valid for up to 15 minutes after the user logs out. Accepted tradeoff at v1, but should be documented and users should be informed to rotate credentials if they suspect compromise.

---

### 🟡 MEDIUM — `InviteService` allows SECONDARY members to send invites

**File:** `backend/.../members/controller/InvitesController.java:29`

```java
inviteService.create(claims.userId(), request.phoneNumber())
```

No check that the calling user's member role is `PRIMARY`. A `SECONDARY` member can POST to `/invites` and add a second Secondary to the account they do not own.

---

### 🟡 MEDIUM — `POST /logout` accepts any refresh token without caller auth

**File:** `AuthController.java` — `/api/v1/auth/logout` is in `permitAll()`

Logout accepts a `refreshToken` body and revokes it. No JWT is required. Because the token is a 256-bit random value this is low practical risk, but architecturally means there is no "revoke only your own token" enforcement.

---

### 🔵 LOW — Webhook JSON extraction is brittle

**Files:** `RazorpayWebhookController.java:123–129`, `SpikeWebhookController.java:89–96`

Both files hand-roll JSON field extraction with `String.indexOf` and `substring`. Signature validation happens first so this is not directly exploitable, but:
- Fails if the key has a space after the colon.
- Extracts the wrong value if the same key appears earlier in a nested structure.
- `hexToBytes` helper is copy-pasted in both files.

Jackson (`ObjectMapper`) is already a Spring Boot transitive dependency. Use it.

---

### 🔵 LOW — CORS not configured

No `CorsConfigurationSource` bean or `@CrossOrigin` annotation exists. Spring Security's default is to block all cross-origin requests. Fine for a pure-mobile API today, but WebView-based payment flows (which the app currently uses) may encounter CORS issues depending on how the redirect is structured.

---

## Part 2 — Backend Functionality Gaps

### 🔴 CRITICAL — Insights feature completely missing in frontend

**File:** `src/features/insights/index.ts`

```ts
export {};
```

The file is empty. There is no Insights screen, component, hook, or API call wired up in the frontend. The Insight model, backend generation service, and API endpoint all exist, but users can never see them.

---

### 🟠 HIGH — 13 of 18 MetricTypes are never ingested from Spike

**File:** `backend/.../wearable/service/WearableService.java:104–112`

Only 5 Spike event types are mapped to MetricTypes: `heart_rate`, `sleep`, `workout`, `daily/steps`, `daily/calories`. The following MetricType values are defined in the enum but have no ingestion path:

`BLOOD_OXYGEN`, `HRV`, `STRESS`, `TEMPERATURE`, `RESPIRATORY_RATE`, `ECG`, `BLOOD_GLUCOSE`, `BLOOD_PRESSURE`, `DISTANCE`, `ACTIVE_MINUTES`, `WEIGHT_CHANGE`, `HYDRATION`, `MOOD`, `SCREEN_TIME`, `OUTDOOR_TIME`, `ROUTINE_ADHERENCE`, `LAST_ACTIVE_LOCATION`

Metrics listed as core in PRD §4 (SpO2, HRV, Respiration, Blood Temperature, Blood Pressure) are silently dropped on every Spike webhook event.

---

### 🟠 HIGH — Push notifications are never delivered to devices

**Backend:** `NotificationGenerationService.java` creates DB rows but makes no call to the Expo push API.  
**Frontend:** No push token registration endpoint exists. No `registerForPushNotificationsAsync` call anywhere.

A user who doesn't open the app will never know about health anomalies, subscription renewals, or invites. The entire notification system is in-app pull only. PRD §12 implies real push delivery.

---

### 🟠 HIGH — Spike DISCONNECTED events not handled

**File:** `backend/.../wearable/service/WearableService.java:74–76`

```java
if ("CONNECTED".equals(eventType) || "connected".equals(eventType)) {
    connection.setStatus("CONNECTED");
}
// No branch for DISCONNECTED, REVOKED, EXPIRED
```

If a user disconnects their Garmin/Fitbit at the vendor side, Bonaca's DB record stays `CONNECTED` permanently. The connection cannot be detected as stale.

---

### 🟠 HIGH — `WearableService.extractValue` silently returns 0.0 on parse failure

**File:** `WearableService.java:126–138`

```java
if (start == end) return 0.0;
```

A Spike event with a missing or malformed `"value":` field stores a 0.0 metric reading. This corrupts baselines and can trigger false anomaly alerts (baseline of, say, 72 bpm vs a 0.0 reading = z-score of ~-36).

---

### 🟠 HIGH — Metrics not gated by subscription status

**File:** `backend/.../metrics/service/MetricsQueryService.java`

The service performs member-level scope authorization but does not check if the account's subscription is `ACTIVE`, `TRIAL`, or `EXPIRING` before returning readings. PRD §6 says health tracking pauses on `EXPIRED`/`CANCELLED`. `SubscriptionInactiveException` exists but is only used in `NotificationGenerationService`, not in the metrics read path.

---

### 🟠 HIGH — Schedulers are not safe for multi-instance deployments

**Files:** `MetricsRollupScheduler.java`, `NotificationsRollupScheduler.java`, `SubscriptionLifecycleScheduler.java`

All three schedulers use `@Scheduled` with no distributed lock. If the app runs on two instances, both fire at 02:00/03:00/04:00. Concurrent insight generation will hit the `UNIQUE(member_id, metric_type, insight_date)` constraint simultaneously and one instance throws `DataIntegrityViolationException`. No ShedLock or DB advisory lock is configured.

Additionally, all member baselines and insights are generated inside a single outer `@Transactional` — one failure rolls back the entire nightly run for all members.

---

### 🟡 MEDIUM — Payment Flow A (trial requires upfront payment method) not implemented

**PRD §9 Flow A:** user connects wearable → adds payment method → trial starts.  
**Current code:** `completeProfile` immediately creates a `TRIAL` subscription with no payment method collection. Payment is a separate later flow. The PRD's intent that day 1 captures billing info is not met.

---

### 🟡 MEDIUM — Payment Flow B (delegated pay / invite-to-pay) not implemented

`NotificationType.PAYMENT_REQUEST` exists in the enum but no code path creates or handles it. The Secondary Member cannot receive a request to start payment on behalf of the Primary Member (PRD §9 Flow B).

---

### 🟡 MEDIUM — `BLOOD_PRESSURE` metric missing from MetricType

**File:** `backend/.../metrics/model/MetricType.java` and `src/types/index.ts`

PRD §4 explicitly lists Blood Pressure as a core vital. It is absent from both the backend enum and the frontend type union.

---

### 🟡 MEDIUM — Metric date range mismatch (frontend `1Y` tab is dead)

**Files:** `backend/.../metrics/dto/MetricRange.java`, `src/features/metrics/components/RangeTabBar.tsx`, `src/features/metrics/MetricDetailsScreen.tsx`

Backend supports `24h`, `7d`, `30d`. Frontend shows tabs `1D`, `7D`, `4W`, `1Y`. The mapping at `MetricDetailsScreen.tsx:41–46` sends both `4W` and `1Y` as `30d`. The `1Y` tab is visually present but returns identical data to `4W`.

---

### 🟡 MEDIUM — Several POST endpoints return 200 instead of 201

`POST /otp/verify`, `POST /complete-profile`, `POST /invites`, `POST /wearable/connect`, `POST /mock-pay` all return `200 OK` for resource creation. REST convention is `201 Created`.

---

### 🟡 MEDIUM — No pagination on any list endpoint

`GET /notifications`, `GET /invites`, `GET /members`, `GET /insights` all return unbounded lists. Insights accumulate daily — after one year this is 365 rows per metric type per member returned in a single response.

---

### 🟡 MEDIUM — No `DELETE /invites/{id}` or `DELETE /members/{id}` endpoint

- A sent invite cannot be revoked. A pending invite counts toward the 2-Secondary-Member cap, so a bad invite permanently consumes a slot.
- A SECONDARY member cannot be removed from an account. PIN/HIDE are workarounds, not removal.

---

### 🟡 MEDIUM — Insight UNIQUE constraint is ineffective for composite (NULL) insights

**File:** `backend/src/main/resources/db/migration/V3__create_metrics_schema.sql`

`UNIQUE(member_id, metric_type, insight_date)` — in PostgreSQL, multiple `NULL` values in a B-tree UNIQUE constraint are treated as distinct. Two concurrent nightly runs can both insert a composite `Routine Consistency` insight (where `metric_type IS NULL`) for the same member and date.

---

### 🟡 MEDIUM — `UpdateMemberRequest` not `@Valid` on controller

**File:** `MembersController.java:50`

```java
@RequestBody UpdateMemberRequest request    // no @Valid
```

Nickname length, pinned/hidden boolean fields, date of birth — none are validated server-side on PATCH.

---

### 🟡 MEDIUM — `AuthService.getMe` throws semantically wrong exception

**File:** `AuthService.java:63`

```java
.orElseThrow(() -> new InvalidRefreshTokenException("User no longer exists"));
```

Called from a JWT-authenticated endpoint, not a refresh flow. Maps to `401 Unauthorized` with the message "User no longer exists" — a confusing error for the client. Should be a dedicated `UserNotFoundException` → `404`.

---

### 🟡 MEDIUM — N+1 queries in `MetricsQueryService.getMemberMetricsSummary`

**File:** `MetricsQueryService.java`

For each metric type in the filtered stream, `metricBaselineRepository.findByMemberIdAndMetricType(...)` is called individually — up to 18 DB round-trips per summary request. Should be a single bulk fetch by `findByMemberIdIn(memberId, types)`.

---

### 🟡 MEDIUM — `next_billing_at` field is never set

**File:** `Subscription.java`, `SubscriptionService.java`

V6 migration adds `next_billing_at` and the Java model has the field, but no code ever populates it — not in `activate()`, not in the `subscription.charged` webhook handler. The column is always `NULL`.

---

### 🔵 LOW — `AuthController` JWT filter errors return no JSON body

The `JwtAuthFilter` silently ignores JWT validation failures (expired, bad signature, malformed) and lets the request fall through as unauthenticated. The `HttpStatusEntryPoint(UNAUTHORIZED)` returns a bare `401` with an empty body — inconsistent with every other error in the API which returns `{"message":"..."}`.

---

### 🔵 LOW — Unhandled `IllegalStateException` and `IOException` surface as 500

**File:** `ApiExceptionHandler.java`

`RazorpayClient.requireConfigured()` and `SpikeApiClient.requireConfigured()` throw `IllegalStateException` when unconfigured. `WearableController` and `PaymentController` can throw `IOException`. None of these are caught by `ApiExceptionHandler` — they produce stack-trace-exposing 500 responses. Should return `503 Service Unavailable` with a user-safe message.

---

### 🔵 LOW — `WearableConnection.status` is a raw `String` field

**File:** `WearableConnection.java`

Status is stored as a free-form string (`"CONNECTED"`, `"PENDING"`, `"DISCONNECTED"`). No enum validation — a Spike webhook event type mismatch (e.g., a trailing space) stores an unknown status silently.

---

### 🔵 LOW — H2 test schema diverges from Flyway Postgres migrations

**File:** `backend/src/test/resources/application-test.yml`

`ddl-auto: create-drop` uses Hibernate's schema generation for unit tests, not Flyway. H2 lacks `TIMESTAMPTZ`, `NUMERIC(10,3)`, partial indexes, and PostgreSQL-specific constraint behaviors. Repository slice tests run against a schema that may not match production — a Flyway migration error would not be caught by unit tests.

---

### 🔵 LOW — JaCoCo 90% coverage gate excludes `wearable` and `payment` packages

**File:** `backend/pom.xml`

The two newest and least-tested modules are exempt from the coverage threshold.

---

## Part 3 — Frontend Functionality Gaps

### 🔴 CRITICAL — `wearableConnection` hardcoded to `null` in ProfileScreen

**File:** `src/features/profile/ProfileScreen.tsx` (near top of component)

```ts
const wearableConnection: WearableConnection | null = null;
```

The real `useWearableConnection` hook exists and is functional, but it is never called here. The profile page always shows "Connect Wearable" even for users who have already connected. This makes the wearable feature appear broken to every user who visits their profile after onboarding.

---

### 🟠 HIGH — Wearable provider selection is silently dropped on connect

**Files:** `src/features/onboarding/ConnectWearableScreen.tsx:54`, `src/features/subscription/SelectWearableAccountScreen.tsx:43–45`, `src/features/wearable/useWearableConnection.ts:50–73`

All four wearable provider cards (Fitbit, Garmin, Samsung, Oura) call the same `connect()` with no argument. `useWearableConnection.connect()` does not accept a provider parameter. The backend call `initiateWearableConnect(accessToken, memberId)` never sends a provider. The user's selection is visually acknowledged but functionally ignored.

---

### 🟠 HIGH — No DM Sans font loaded at app root

**File:** `src/app/_layout.tsx`

`Fonts.family` (`"DMSans"`) is referenced throughout every screen, but `useFonts` is never called in the layout root. On Android in particular, if the font is not pre-bundled, all text falls back to the system sans-serif on first render, producing a visible FOUT (Flash of Unstyled Text) or incorrect typography throughout.

---

### 🟠 HIGH — Invite Member screen is non-functional

**File:** `src/features/members/InviteMemberScreen.tsx`

- Contact list is a hardcoded mock array, not fetched from the device (`expo-contacts` not installed).
- The search `View` contains an icon and placeholder but no `TextInput` — tapping does nothing, typing is impossible.
- The real invite API (`createInvite`) is connected, so the underlying flow works, but the discovery UI is completely fake.

---

### 🟠 HIGH — `WearableConnectionStatus` type conflict

**Files:** `src/types/index.ts` (lowercase: `'connected' | 'disconnected' | 'needs-reauth'`) vs `src/types/wearable.ts:1` (uppercase: `'PENDING' | 'CONNECTED' | 'DISCONNECTED'`)

All wearable status checks in screen code use the uppercase values from `wearable.ts`. The domain type in `index.ts` is dead and would cause silent type errors if ever used. One must be deleted and the other standardized.

---

### 🟠 HIGH — Hardcoded mock/placeholder data throughout production screens

| File | Hardcoded Data |
|---|---|
| `HomeScreen.tsx` | Avatar always `prasanna-kumar.png`; sync label always `"Last synced: Just now"` for all members |
| `HomeScreen.tsx` | Other members: index 0 hardcoded to `"1 hr ago"`, rest to `"Just now"` |
| `SharedMemberCard.tsx` | Alert chips (`2 High / 2 Low / 2 Normal`) fully hardcoded with non-token hex colors |
| `MetricDetailsScreen.tsx` | Date labels `"Wednesday, 14 Jan (Today)"` and `"Tuesday, 13 Jan"` — not computed |
| `ProfileScreen.tsx` | `wearableConnection = null` always (see Critical above) |
| `SubscriptionsScreen.tsx` | `PlaceholderAvatar` never shows real member photo |
| `src/features/members/mockData.ts` | Large mock data file present in production bundle |
| `src/features/metrics/mockData.ts` | Same |

---

### 🟠 HIGH — No push notification token registration in frontend

**Files:** All `src/app/` layout files, `src/features/` screens

There is no call to `registerForPushNotificationsAsync`, no push token storage, and no push token registration API call anywhere in the codebase. Push notifications cannot work until a device token is obtained and sent to the backend.

---

### 🟡 MEDIUM — Multiple no-op `onPress` handlers

| Screen | Missing Implementation |
|---|---|
| `ProfileScreen.tsx` | Documentation, Terms & Conditions, Privacy Policy links — all `() => {}` |
| `ProfileScreen.tsx` | "Disconnect wearable" → `onPressDisconnect: () => {}` |
| `ProfileDetailsScreen.tsx:93` | Edit pencil (`IconPencil`) has no `onPress` |
| `LoginScreen.tsx:87` | Privacy Policy `Pressable` has no handler |
| `CompleteProfileScreen.tsx` | Profile photo edit → `onPressEdit={() => {}}` |
| `MobileNumberField.tsx` | Country code picker chevron shown, not implemented; `+91` hardcoded |

---

### 🟡 MEDIUM — `SplashScreen` auto-advance may race with auth state

**File:** `src/features/auth/SplashScreen.tsx`

`setTimeout` fires after 1800ms and checks `isLoading` before routing. If the `refreshAccessToken` network call takes longer than 1.8s (cold start, slow network), the routing logic executes while auth state is `undefined`, sending the user to the wrong route.

---

### 🟡 MEDIUM — `ProfileScreen.tsx` routes wearable CTA to wrong path

**File:** `ProfileScreen.tsx`

The "Connect your wearable" settings item navigates to `/(auth)/connect-wearable` — back into the onboarding auth flow — instead of `/(subscription)/select-wearable-account` which is the post-onboarding connection screen.

---

### 🟡 MEDIUM — No request timeout or retry in API client

**File:** `src/lib/api/client.ts`

No `AbortController` timeout is set. A stalled network request hangs indefinitely. No retry for transient 5xx errors. On flaky mobile connections this surfaces directly as infinite loading states.

---

### 🟡 MEDIUM — `mockPay` API call not dev-gated in frontend

**File:** `src/lib/api/subscriptions.ts`

`mockPay` is exported from the API layer with no dev/staging guard. It is callable from production builds — the only protection is the backend `@Profile("!prod")` gate.

---

### 🟡 MEDIUM — `MemberListScreen.tsx` renders `HomeHeader` as list header

**File:** `src/features/members/MemberListScreen.tsx`

`HomeHeader` (the greeting + wearable sync badge component) is used as the `ListHeaderComponent` in the member list. "Manage your family members" appears where the sync status badge normally goes. Semantically and visually wrong.

---

### 🟡 MEDIUM — Secondary Member profile variant not implemented

**PRD:** Figma nodes `197:5921`, `197:6270`, `197:7272`, `197:6835`, `197:7049` define four subscription state variants for the Secondary profile (trial, active, expiring, expired).  
**Current code:** Single `ProfileScreen.tsx` for all roles, no subscription-state-specific variants rendered.

---

### 🟡 MEDIUM — Behaviour scope has empty `metricLabels` array

**File:** `src/features/members/model/permissions.ts`

```ts
behaviour: {
  metricLabels: []  // empty — no metric labels shown under Behaviour card
}
```

The `Behaviour` scope card in Edit Permissions renders with no metric labels beneath it, despite PRD §11.2 listing Routine, phone usage, and outside time under this scope.

---

### 🟡 MEDIUM — Notification deep-link target uses unsafe cast

**File:** `src/features/notifications/NotificationsScreen.tsx`

```ts
notification.deepLinkTarget as Href
```

If the backend returns a target that isn't a valid Expo Router `Href`, the navigation call throws an unhandled runtime error. No validation or try-catch.

---

### 🔵 LOW — Hardcoded colors not using design tokens

| File | Hardcoded Value |
|---|---|
| `SharedMemberCard.tsx` | `#e97961`, `rgba(233,121,97,0.1)`, `#5b8def`, `#8b6f9c` (alert chips) |
| `ApplyAllPermissionsRow.tsx` | `#8f8f8f`, `#3b72db` (checkbox) |
| `PermissionScopeCard.tsx` | `#6bc49b`, `#dde3ec`, `#3b72db` (toggle) |
| `metricDisplay.ts` | All `iconColors` array entries |

Design token references should be used for all colors so theming works consistently.

---

### 🔵 LOW — `BarChartCard` uses fixed 326px width

**File:** `src/features/metrics/components/BarChartCard.tsx`

`CHART_WIDTH = 326` is hardcoded. On devices narrower than 375pt the chart overflows; on tablets or landscape there are unexplained margins.

---

### 🔵 LOW — `HomeHeader.paddingTop: 62` not using safe area insets

**File:** `src/features/home/components/HomeHeader.tsx`

`paddingTop: 62` hardcoded — `useSafeAreaInsets` is not called. On devices with non-standard status bar heights (Dynamic Island, older notch sizes) this either clips the header or wastes space.

---

### 🔵 LOW — `ScreenPlaceholder` does not apply `Fonts.family`

**File:** `src/components/ScreenPlaceholder.tsx`

`title` and `section` text styles have no `fontFamily: Fonts.family` — they fall back to the system font, inconsistent with DM Sans everywhere else.

---

### 🔵 LOW — `HiddenMembersScreen.tsx` imports `Image` from `react-native`

**File:** `src/features/members/HiddenMembersScreen.tsx:3`

```ts
import { Image } from 'react-native'
```

All other screens use `expo-image` for better caching and performance.

---

### 🔵 LOW — Dead HealthKit/Health Connect stubs in `src/lib/health/`

**Files:** `src/lib/health/HealthProvider.ts`, `appleHealth.ts`, `healthConnect.ts`

CLAUDE.md documents these as the wrong shape (HealthKit/Health Connect interface, not Spike API). They are not called anywhere in the main flow, but they inflate the bundle and mislead future readers. The `WearableProvider` type in `src/types/index.ts` also still includes `'apple-health'` and `'health-connect'`.

---

## Part 4 — PRD vs. Implementation Gap Table

| PRD Requirement | Status | Notes |
|---|---|---|
| Role model: PRIMARY / SECONDARY / no Tertiary | ✅ Aligned | Both backend and frontend correct |
| Max 2 Secondary Members per account | ✅ Aligned | `MAX_SECONDARY_MEMBERS = 2` enforced in InviteService |
| 3 sharing scopes (VITALS / ACTIVITY / BEHAVIOUR, no LOCATION) | ✅ Aligned | SharingScope.java correct |
| All-on-by-default sharing grants | ✅ Aligned | createDefaultGrants sets all 3 visible=true |
| Instant-apply permission toggles (no Save button) | ✅ Aligned | `useEditPermissions` PATCHes immediately |
| 7-day trial | ✅ Aligned | `TRIAL_DAYS = 7` in SubscriptionService |
| Subscription states: TRIAL→ACTIVE→EXPIRING→EXPIRED/CANCELLED | ✅ Aligned | SubscriptionStatus.java matches |
| Account-level (not member-level) subscription | ✅ Aligned | UNIQUE on account_id in V2 migration |
| Trial requires payment method upfront (Flow A) | ❌ Missing | Trial starts on completeProfile with no payment info collected |
| Flow B delegated pay (Primary asks Secondary to pay) | ❌ Missing | PAYMENT_REQUEST notification type exists but no code creates it |
| Spike API as wearable data source (backend) | ✅ Implemented | SpikeApiClient, SpikeWebhookController wired |
| HealthKit/Health Connect removed (frontend) | ❌ Stubs remain | Dead code still in src/lib/health/ |
| Blood Pressure as a core vitals metric | ❌ Missing | Absent from MetricType.java and src/types/index.ts |
| Metric ranges: 24h / 7d / 30d | ⚠️ Partial | Frontend shows 4 tabs; 1Y maps to 30d (dead tab) |
| Insights visible to user | ❌ Missing | insights/index.ts is empty; no insights screen |
| Push notification delivery to devices | ❌ Missing | No Expo push integration on either side |
| Consent screen before payment / data sharing | ❌ Missing | No dedicated consent screen in onboarding |
| iOS StoreKit / Razorpay conflict resolved | ❌ Open | Unresolved; acknowledged in CLAUDE.md |
| Payment confirmation UX after Razorpay redirect | ⚠️ Gap | No webhook polling; state only updates on re-navigate |
| Wearable status shown correctly in Profile | ❌ Broken | wearableConnection = null hardcoded |
| Real member avatar photos | ⚠️ Placeholder | prasanna-kumar.png hardcoded for all members |
| Secondary Member profile / subscription variants | ⚠️ Partial | No separate Secondary-specific profile screen |
| BLOOD_PRESSURE metric type | ❌ Missing | Not in backend enum or frontend types |
| Spike ingests all 18 metric types | ❌ Partial | Only 5 of 18 types mapped in WearableService |
| Behaviour scope shows metric labels | ⚠️ Gap | metricLabels: [] empty in permissions.ts |
| DELETE / remove secondary member | ❌ Missing | No backend endpoint, no frontend UI |
| Revoke / cancel sent invite | ❌ Missing | No backend endpoint, no frontend UI |
| Edit profile photo | ❌ No-op | onPressEdit={() => {}} |
| Country picker for phone auth | ❌ Hardcoded | +91 hardcoded, picker not implemented |

---

## Part 5 — Screen Implementation Status

| Screen | Figma Node | Status | Notes |
|---|---|---|---|
| Splash | `43:3178` | ✅ Implemented | |
| Login – Mobile No. Entry | `49:268` | ⚠️ Partial | +91 country code hardcoded, privacy policy no-op |
| Login – OTP | `49:364` | ✅ Implemented | Incorrect OTP / Resend states handled |
| Complete Profile | `60:595`, `60:768` | ⚠️ Partial | Photo upload no-op |
| Connect Wearable (onboarding) | `60:634` | ⚠️ Partial | Provider selection silently dropped |
| Home – Primary | `188:2859` | ⚠️ Partial | Avatar + sync labels hardcoded |
| Member Details | `43:4129` | ✅ Implemented | |
| Metric Details | `197:3828`, `197:3909` | ⚠️ Partial | Date labels hardcoded; 1Y tab is dead |
| Notifications | `286:15753` | ⚠️ Partial | Pull-only; push never delivered; deep-link unsafe cast |
| Select Wearable Account | `197:10387` etc. | ⚠️ Partial | Provider selection silently dropped |
| Payment Gateway | `197:10384`, `197:11043` | ⚠️ Partial | No webhook polling after browser redirect |
| Profile – Primary | `39:2025` | 🔴 Broken | Wearable always shows disconnected; links no-op |
| Profile – Secondary variants | `197:5921`, `197:6270` etc. | ❌ Missing | No Secondary-specific screen or subscription variants |
| Insights | — | ❌ Missing | Feature entirely absent |
| Invite Member | — | ⚠️ Partial | Contact list is mock; search field non-functional |
| Edit Permissions | — | ✅ Implemented | Behaviour labels empty |
| Hidden Members | — | ✅ Implemented | |
| Subscriptions overview | — | ✅ Implemented | |
| Profile Details | — | ⚠️ Partial | Edit pencil no-op |

---

## Prioritized Fix List

### Must fix before production

1. 🔴 Webhook signature bypass (Spike + Razorpay) — change `return true` to `return false` when secret blank
2. 🔴 Hard-coded JWT secret — make fallback invalid, throw at startup if not overridden outside local profile
3. 🔴 `wearableConnection = null` in `ProfileScreen.tsx` — call `useWearableConnection` hook
4. 🔴 Insights screen — implement using existing backend API and data model
5. 🟠 Proxy secret filter blocking webhook paths — add `/api/v1/webhooks/**` to `shouldNotFilter`
6. 🟠 OTP length default — change `codeLength` from 4 to 6 in `application.yml`
7. 🟠 `WearableService.extractValue` returning 0.0 silently — throw or log and discard, never store 0.0 as a valid reading
8. 🟠 Wearable provider not passed to `connect()` — add provider param through the call chain
9. 🟠 Spike DISCONNECTED events not handled — add branch in `WearableService`
10. 🟠 `InviteService.create` — add `requireAccountOwner` check (PRIMARY only can invite)
11. 🟠 DM Sans font loading — add `useFonts` in `_layout.tsx`
12. 🟠 Push notification token registration — add `registerForPushNotificationsAsync`, device token endpoint, and Expo push dispatch in `NotificationGenerationService`

### Fix before first external users

13. 🟠 Metrics gated by subscription status — check `isActive()` in `MetricsQueryService`
14. 🟠 Scheduler distributed lock — add ShedLock to all three schedulers
15. 🟠 BLOOD_PRESSURE added to `MetricType` enum and frontend types
16. 🟠 Spike: map all 18 metric types in `WearableService.mapEventTypeToMetric`
17. 🟡 ProfileScreen routing fix — `/(subscription)/select-wearable-account` not `/(auth)/connect-wearable`
18. 🟡 Real member avatars — use member's stored photo, not hardcoded image
19. 🟡 Hardcoded date labels in `MetricDetailsScreen` — compute from `Date`
20. 🟡 Remove mock data files from production bundle (`mockData.ts` in members + metrics)
21. 🟡 `ApiExceptionHandler` — add handlers for `IllegalStateException` (→ 503) and `IOException` (→ 503), and a catchall `Exception` → 500 with JSON body
22. 🟡 `JWT filter` — return JSON error body on 401, not empty response
23. 🟡 `RefreshRequest` — add `@NotBlank` validation
24. 🟡 Insight UNIQUE constraint — use `INSERT ... ON CONFLICT DO UPDATE` (upsert) to handle concurrent inserts on NULL `metric_type`
25. 🟡 Add `next_billing_at` population in `activate()` and `subscription.charged` handler
26. 🟡 Behaviour scope `metricLabels` — populate array in `permissions.ts`
27. 🟡 Request timeout in API client — add `AbortController` with a 30s timeout
28. 🟡 `WearableConnection.status` — convert to enum, add `@Enumerated(EnumType.STRING)`

### Quality / polish

29. 🔵 Replace hardcoded alert chip colors in `SharedMemberCard` with token references
30. 🔵 Replace hardcoded toggle colors in `PermissionScopeCard` and `ApplyAllPermissionsRow` with tokens
31. 🔵 `BarChartCard` — use `Dimensions.get('window').width` instead of 326px
32. 🔵 `HomeHeader.paddingTop` — use `useSafeAreaInsets`
33. 🔵 `ScreenPlaceholder` — add `fontFamily: Fonts.family` to text styles
34. 🔵 `HiddenMembersScreen` — import `Image` from `expo-image`
35. 🔵 Delete `src/lib/health/HealthProvider.ts`, `appleHealth.ts`, `healthConnect.ts` (dead stubs)
36. 🔵 Remove `'apple-health'` and `'health-connect'` from `WearableProvider` type
37. 🔵 Remove `'paypal' | 'amex' | 'mastercard' | 'apple-pay'` from `PaymentMethodType`
38. 🔵 Standardize `WearableConnectionStatus` — delete one of the two conflicting type definitions
39. 🔵 Extract shared `hexToBytes` utility from `RazorpayWebhookController` and `SpikeWebhookController`
40. 🔵 Replace manual JSON parsing in webhook controllers with Jackson `ObjectMapper`
41. 🔵 Add webhook integration tests (Razorpay + Spike) using WireMock
42. 🔵 Add JaCoCo coverage gate for `wearable` and `payment` packages
43. 🔵 Pagination on all list endpoints (`/notifications`, `/insights`, `/invites`)
44. 🔵 `POST` endpoints → return `201 Created` instead of `200 OK`
45. 🔵 `DELETE /invites/{id}` and `DELETE /members/{id}` endpoints
46. 🔵 `noRollbackFor` propagation audit — verify it reaches all outer `@Transactional` callers
47. 🔵 `MetricsQueryService` summary — bulk-fetch all baselines in one query, not per-metric-type
48. 🔵 `BaselineService.recomputeAllBaselines` — use `REQUIRES_NEW` propagation per member so one failure doesn't roll back all

---

*End of audit. 48 findings across 4 severity levels. No code was changed during this audit — this document is read-only.*
