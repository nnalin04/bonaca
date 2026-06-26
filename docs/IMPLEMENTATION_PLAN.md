# Bonaca — Comprehensive Implementation Plan

**Generated:** 2026-06-26  
**Based on:** 9 feature-flow audits + full codebase audit  
**Goal:** After executing every task here, the only remaining work is plugging in API keys.

---

## API Keys / Credentials Required (after all code tasks done)

| Variable | Service | Where Set |
|---|---|---|
| `MSG91_AUTH_KEY` | OTP SMS delivery | Backend env |
| `MSG91_TEMPLATE_ID` | OTP SMS template (DLT-approved) | Backend env |
| `BONACA_SPIKE_API_KEY` | Wearable data sync | Backend env |
| `BONACA_SPIKE_WEBHOOK_SECRET` | Spike webhook HMAC auth | Backend env |
| `BONACA_RAZORPAY_KEY_ID` | Payment gateway | Backend env |
| `BONACA_RAZORPAY_KEY_SECRET` | Payment gateway | Backend env |
| `BONACA_RAZORPAY_WEBHOOK_SECRET` | Razorpay webhook HMAC auth | Backend env |
| `BONACA_RAZORPAY_PLAN_ID` | Monthly billing plan (₹249/month, created in Razorpay dashboard) | Backend env |
| `JWT_SECRET` | Production token signing (64-char base64) | Backend env |
| `EXPO_PUBLIC_API_BASE_URL` | Frontend → backend URL | Frontend `.env` |
| `EXPO_ACCESS_TOKEN` | Expo push notifications | Frontend `.env` |

**Dashboard config (not code):**
- Razorpay webhook URL: `POST https://<backend>/api/v1/webhooks/razorpay`
- Spike webhook URL: `POST https://<backend>/api/v1/webhooks/spike`
- MSG91 DLT template must be registered and approved with TRAI before OTP SMS delivers in India

---

## Execution Phases

| Phase | What | Why first |
|---|---|---|
| **P0 — Critical Security** | Fix webhook bypasses, JWT secret, proxy filter, exception handler | Exploitable in any deployed env |
| **P1 — Broken Core Flows** | OTP routing, wearable null in profile, font loading, wearable disconnect | App doesn't work end-to-end |
| **P2 — Missing Features** | Insights screen, push notifications, invite fixes, edit profile, secondary profile | Users can't access these features at all |
| **P3 — Real Data** | Replace hardcoded avatars, sync labels, alert chips, date labels | App looks fake with placeholder data |
| **P4 — Data Pipeline** | Wearable Spike event mappings, dedup, DateStepper wiring, 1Y range | Data doesn't flow correctly to screens |
| **P5 — Polish** | Token colors, chart width, safe area, dead code removal | Consistency and quality |

---

## PHASE 0 — Critical Security

### SEC-1 · Fix Razorpay webhook signature bypass
**File:** `backend/src/main/java/com/bonaca/backend/payment/controller/RazorpayWebhookController.java:77`

```java
// Change:
if (secret == null || secret.isBlank()) return true;
// To:
if (secret == null || secret.isBlank()) return false;
```

### SEC-2 · Fix Spike webhook signature bypass
**File:** `backend/src/main/java/com/bonaca/backend/wearable/controller/SpikeWebhookController.java:62–64`

```java
// Change:
if (secret == null || secret.isBlank()) { return true; }
// To:
if (secret == null || secret.isBlank()) { return false; }
```

### SEC-3 · Fix JWT secret fallback
**File:** `backend/src/main/resources/application.yml:19`

```yaml
# Change:
secret: ${JWT_SECRET:s98slO6+8H7jnfAEqBtcK2/NVqdSKSWmPjPlqpFrGcs=}
# To:
secret: ${JWT_SECRET:CHANGE_ME_NOT_FOR_PROD}
```

In `JwtProperties.java` add `@PostConstruct` validation:
```java
@PostConstruct
public void validate() {
    if ("CHANGE_ME_NOT_FOR_PROD".equals(secret) && !isLocalProfile()) {
        throw new IllegalStateException("JWT_SECRET env var must be set in non-local environments");
    }
}
```

### SEC-4 · Fix proxy filter blocking webhook endpoints in remote-dev
**File:** `backend/src/main/java/com/bonaca/backend/config/ProxySecretFilter.java`

Override `shouldNotFilter`:
```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.equals("/health") || path.startsWith("/api/v1/webhooks/");
}
```

### SEC-5 · Add catch-all exception handler + map IllegalStateException → 503
**File:** `backend/src/main/java/com/bonaca/backend/common/ApiExceptionHandler.java`

Add three handlers:
```java
@ExceptionHandler(IllegalStateException.class)
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public ErrorResponse handleIllegalState(IllegalStateException ex) {
    return new ErrorResponse("Service not configured: " + ex.getMessage());
}

@ExceptionHandler(IOException.class)
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public ErrorResponse handleIo(IOException ex) {
    return new ErrorResponse("External service error");
}

@ExceptionHandler(Exception.class)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public ErrorResponse handleGeneric(Exception ex) {
    log.error("Unhandled exception", ex);
    return new ErrorResponse("An unexpected error occurred");
}
```

### SEC-6 · OTP length default: 4 → 6
**Backend:** `application.yml:26` — change `code-length: 4` to `code-length: 6`

**Frontend:** Change `OTP_LENGTH = 4` to `OTP_LENGTH = 6` in:
- `src/features/auth/hooks/useOtpVerification.ts:8`
- `src/features/auth/components/OtpInput.tsx:6`

---

## PHASE 1 — Broken Core Flows

### AUTH-1 · Replace `router.push` with `router.replace` after OTP verify
**File:** `src/features/auth/OtpScreen.tsx:36`

```ts
// Change:
router.push(profileCompleted ? '/(tabs)/home' : '/(auth)/complete-profile')
// To:
router.replace(profileCompleted ? '/(tabs)/home' : '/(auth)/complete-profile')
```
Prevents OTP screen staying in the back-stack after login.

### AUTH-2 · Surface specific OTP error messages
**File:** `src/features/auth/hooks/useOtpVerification.ts:48–51`

Replace the catch that always shows "Enter a valid OTP" with:
```ts
} catch (err) {
  setError(err instanceof ApiError ? err.message : 'Enter a valid OTP');
  setCode('');
}
```
This surfaces backend messages like "OTP has expired — please request a new one", "Too many attempts", etc.

### AUTH-3 · Load DM Sans font in root layout + add SafeAreaProvider
**File:** `src/app/_layout.tsx`

```ts
import { useFonts } from 'expo-font';
import { SafeAreaProvider } from 'react-native-safe-area-context';

// Inside component:
const [fontsLoaded] = useFonts({
  'DMSans': require('../assets/fonts/DMSans-Regular.ttf'),
  'DMSans-Medium': require('../assets/fonts/DMSans-Medium.ttf'),
  'DMSans-Bold': require('../assets/fonts/DMSans-Bold.ttf'),
  'DMSans-SemiBold': require('../assets/fonts/DMSans-SemiBold.ttf'),
});
if (!fontsLoaded) return null;

// Wrap return with <SafeAreaProvider>
```

### AUTH-4 · Remove misleading country picker chevron (or implement picker)
**File:** `src/features/auth/components/MobileNumberField.tsx`

**Minimum:** Remove the `>` chevron icon from the country code `View` so `+91` renders as plain non-interactive text.  
**Full:** Implement a `Modal`-based country picker with E.164 prefix selection. Update `useLoginOtpRequest` to use the selected prefix instead of hardcoded `+91`.

### AUTH-5 · Wire Privacy Policy link on Login screen
**File:** `src/features/auth/LoginScreen.tsx:84`

```ts
onPress={() => Linking.openURL('https://bonaca.in/privacy')}
```

### PROFILE-1 · Fix wearable status hardcoded to null (critical)
**File:** `src/features/profile/ProfileScreen.tsx`

**Remove:**
```ts
const wearableConnection: WearableConnection | null = null;
```

**Replace with:**
```ts
const { connection: wearableConn, refresh: refreshWearableConn } = useWearableConnection(self?.id ?? null);
const isConnected = wearableConn?.status === 'CONNECTED';
```

Use `isConnected` to conditionally render `WearableConnectedCard` vs `WearableConnectCard`.

### PROFILE-2 · Fix wearable connect routing in Profile
**File:** `src/features/profile/ProfileScreen.tsx:135`

```ts
// Change:
router.push('/(auth)/connect-wearable')
// To:
router.push('/subscription/select-wearable-account')
```

### PROFILE-3 · Wire wearable disconnect button
**File:** `src/features/wearable/useWearableConnection.ts`

Add `disconnect` to the hook's return value:
```ts
const disconnect = useCallback(async () => {
  if (!accessToken || !memberId) return;
  setIsLoading(true);
  try {
    await disconnectWearable(accessToken, memberId);
    await refresh();
  } finally {
    setIsLoading(false);
  }
}, [accessToken, memberId, refresh]);

return { connection, isLoading, error, connect, refresh, disconnect };
```

**File:** `src/features/profile/ProfileScreen.tsx:131`
Pass `onPressDisconnect={disconnect}` to `WearableConnectedCard`.

### PROFILE-4 · Fix wearable status in ProfileDetailsScreen
**File:** `src/features/profile/ProfileDetailsScreen.tsx:117–122`

Replace hardcoded "No wearable connected" with real hook data:
```ts
const { connection } = useWearableConnection(self?.id ?? null);
// In JSX: show connection.status === 'CONNECTED' ? provider name : 'No wearable connected'
```

### WEARABLE-1 · Handle Spike DISCONNECTED events
**File:** `backend/src/main/java/com/bonaca/backend/wearable/service/WearableService.java:74–77`

```java
} else if ("DISCONNECTED".equals(eventType) || "disconnected".equals(eventType)
        || "revoked".equals(eventType) || "expired".equals(eventType)) {
    connection.setStatus("DISCONNECTED");
    connection.setConnectedAt(null);
}
```

### WEARABLE-2 · Fix silent 0.0 on extractValue parse failure
**File:** `backend/src/main/java/com/bonaca/backend/wearable/service/WearableService.java`

Change `extractValue` to return `Double` (nullable). Return `null` instead of `0.0` when the field is missing or unparseable. In the caller, skip the metric reading if value is `null` with a `log.warn`.

### WEARABLE-3 · Fix disconnect: delete DB row before Spike API call
**File:** `backend/.../wearable/service/WearableService.java` — `disconnect` method

Delete the DB row first, then attempt Spike `deleteUser`. Wrap the Spike call in try-catch and log the failure — do not propagate the exception. User should not be stuck with an undeleteable wearable.

### WEARABLE-4 · Replace manual JSON parsing with Jackson
**Files:** `SpikeApiClient.java`, `SpikeWebhookController.java`, `RazorpayWebhookController.java`

Inject `ObjectMapper objectMapper` (already a Spring Boot bean). Replace all `extractJsonString` / `extractField` / `extractSubscriptionId` / `extractValue` string-scan methods with `objectMapper.readTree(body)` and proper node traversal. This fixes all silent parse failures.

### PAYMENT-1 · Add "Check payment status" button after Razorpay redirect
**File:** `src/features/subscription/PaymentGatewayScreen.tsx`

After `Linking.openURL(result.paymentLink)`, show a "I've completed payment — check status" button that calls `refresh()` from `useCurrentSubscription()`, shows a loading indicator, then navigates to the subscriptions screen when status becomes `ACTIVE`.

### PAYMENT-2 · Handle `subscription.renewed` + set `next_billing_at`
**File:** `backend/.../payment/service/PaymentService.java`

Add to `handleWebhookEvent`:
```java
case "subscription.renewed" -> {
    String nextEndTs = extractField(payload, "current_end");
    Instant nextBilling = nextEndTs != null
        ? Instant.ofEpochSecond(Long.parseLong(nextEndTs)) : null;
    subscriptionService.recordRenewal(accountId, nextBilling);
}
```

Add `recordRenewal(UUID accountId, Instant nextBillingAt)` to `SubscriptionService` setting both `renewedAt` and `nextBillingAt`.

---

## PHASE 2 — Missing Features

### INSIGHTS-1 · Build Insights feed screen
**Files to create:**
- `src/features/insights/components/InsightCard.tsx` — renders one insight (type badge, metric name, text, date)
- `src/features/insights/hooks/useInsights.ts` — calls `listInsights(accessToken, memberId)` from `src/lib/api/metrics.ts` (already implemented)
- `src/features/insights/InsightsFeedScreen.tsx` — list of `InsightCard` with loading/empty/error states
- `src/app/member/[memberId]/insights.tsx` — route delegate

Link from `MemberDetailsScreen` (add "Insights" section or tab). Also add a 2–3 item "Recent Insights" preview on `HomeScreen` for `self`.

### NOTIF-1 · Implement push notifications end-to-end

**Frontend steps:**
1. `package.json`: add `expo-notifications`
2. `app.json`: add plugin and iOS `NSUserNotificationUsageDescription`
3. New `src/features/notifications/usePushSetup.ts` hook (called from `_layout.tsx` after login):
   - `Notifications.requestPermissionsAsync()` → get permission
   - `Notifications.getExpoPushTokenAsync()` → get Expo push token
   - `POST /api/v1/members/{memberId}/push-token` with token + platform
   - `Notifications.setNotificationHandler(...)` for foreground display
   - `addNotificationResponseReceivedListener` for tap → deep-link navigation
4. `src/lib/api/notifications.ts`: add `registerPushToken(accessToken, memberId, token, platform)`
5. `_layout.tsx` or tabs layout: add `tabBarBadge={unreadCount || undefined}` on notifications tab; call new `GET /members/{id}/notifications/unread-count` endpoint

**Backend steps:**
1. New `V8__add_push_tokens.sql` migration:
   ```sql
   CREATE TABLE device_push_tokens (
       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
       member_id UUID NOT NULL REFERENCES members(id) ON DELETE CASCADE,
       token TEXT NOT NULL,
       platform VARCHAR(10) NOT NULL,
       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
       UNIQUE(member_id, token)
   );
   CREATE INDEX idx_push_tokens_member_id ON device_push_tokens(member_id);
   ```
2. `DevicePushToken` entity + `DevicePushTokenRepository`
3. `POST /api/v1/members/{id}/push-token` in new `PushTokenController` — upserts token
4. `GET /api/v1/members/{id}/notifications/unread-count` in `NotificationsController`
5. New `ExpoPushService` — calls `https://exp.host/--/api/v2/push/send` (no auth, plain JSON) with `{to, title, body, data: {deepLinkTarget}}`
6. In `NotificationService.create()` after `notificationRepository.save(notification)`:
   ```java
   devicePushTokenRepository.findByMemberId(notification.getRecipientMemberId())
       .forEach(token -> expoPushService.send(token.getToken(), notification));
   ```

### MEMBERS-1 · Fix InviteMemberScreen — replace mock with real phone entry
**File:** `src/features/members/InviteMemberScreen.tsx`

Replace the hardcoded mock contact list with:
1. A `TextInput` for entering a phone number manually (with +91 prefix UI)
2. An "Invite" button that calls the existing `inviteContact(phoneNumber)` API
3. Below it: read-only list of already-sent invites from `listInvites` API (already implemented) showing status (PENDING/ACCEPTED/CANCELLED)

Optionally add `expo-contacts` for device contact picker as an enhancement.

### MEMBERS-2 · Add invite cancellation (backend + frontend)
**Backend:**
- Add `CANCELLED` to `InviteStatus.java`
- Add `cancelInvite(UUID inviteId, UUID callerId)` to `InviteService` — verify caller is PRIMARY account owner, mark `CANCELLED`
- Add `DELETE /api/v1/invites/{inviteId}` to `InvitesController`

**Frontend:**
- Add "Cancel" action on each PENDING invite row in `InviteMemberScreen`

### MEMBERS-3 · Add remove secondary member (backend + frontend)
**Backend:**
- Add `removeMember(UUID memberId, UUID callerId)` to `MembersService` — verify caller is PRIMARY of same account, verify target is SECONDARY, delete sharing grants, delete member row
- Add `DELETE /api/v1/members/{memberId}` to `MembersController` with PRIMARY-role guard

**Frontend:**
- Add "Remove Member" to `menuOptions` in `MemberDetailsScreen.tsx`

### MEMBERS-4 · Add "Hide Member" to member detail menu
**File:** `src/features/members/MemberDetailsScreen.tsx:99–104`

```ts
{
  label: member.hidden ? 'Unhide Member' : 'Hide Member',
  onPress: () => updateMember(memberId, { hidden: !member.hidden }).then(refresh)
}
```

### MEMBERS-5 · Fix InviteService role check — PRIMARY only can invite
**File:** `backend/.../members/controller/InvitesController.java`

Before forwarding to service:
```java
Member me = memberRepository.findByUserId(claims.userId()).orElseThrow(...);
if (me.getRole() != MemberRole.PRIMARY) {
    throw new ForbiddenMemberAccessException("Only primary members can send invites");
}
```

### PROFILEDET-1 · Wire edit profile pencil → inline form → PATCH
**File:** `src/features/profile/ProfileDetailsScreen.tsx`

1. Add `isEditing` state. Tapping pencil sets `isEditing = true`.
2. Render `TextInput` / picker components for name, DOB, height, weight, gender when editing.
3. Save button calls `updateMember(accessToken, memberId, { name, dob, heightCm, weightKg, gender })` via the existing `PATCH /api/v1/members/{id}` backend endpoint.
4. On success: `setIsEditing(false)`, `refresh()`.

### PROFILEDET-2 · Secondary Member profile variant
**File:** `src/features/profile/ProfileScreen.tsx`

Add role-based layout:
```ts
const isSecondary = self?.role === 'secondary';
```
- If secondary: hide "Connect Wearable" section, hide "Members & Permissions" row, show a "Watching [Primary name]" section with CTA to view their data
- Show subscription state (trial/active/expiring/expired) consistent with Figma nodes `197:6270` etc. — the display logic already exists in `subscriptionDisplay.ts`, just needs to be exposed here

### ONBOARD-1 · Profile photo upload
**Files:** `src/features/onboarding/components/ProfileAvatar.tsx`, `CompleteProfileScreen.tsx`

1. Install `expo-image-picker`
2. `onPressEdit` → `ImagePicker.launchImageLibraryAsync({ allowsEditing: true, aspect: [1,1], quality: 0.8 })`
3. Upload via new `POST /api/v1/members/{id}/avatar` multipart endpoint (stores in DB or object storage)
4. On success, refresh member to show real photo everywhere

### ONBOARD-2 · Spike deep-link redirect: backend + frontend
**Backend — `SpikeApiClient.java:36`:**
Add `redirect_url` to the Spike user creation payload:
```java
String body = "{\"external_id\":\"" + memberId
    + "\",\"redirect_url\":\"bonaca://wearable/connected\"}";
```

**Frontend — `src/app/_layout.tsx` or `ConnectWearableScreen.tsx`:**
```ts
import * as Linking from 'expo-linking';
useEffect(() => {
  const sub = Linking.addEventListener('url', ({ url }) => {
    if (url.startsWith('bonaca://wearable/connected')) {
      refreshWearableConnection();
      router.replace('/(tabs)/home');
    }
  });
  return () => sub.remove();
}, []);
```

---

## PHASE 3 — Real Data Everywhere

### HOME-1 · Real member avatars everywhere (replace prasanna-kumar.png)
**Files:** `HomeScreen.tsx`, `SharedMemberCard.tsx`, `MemberListScreen.tsx`, `MemberDetailsScreen.tsx`, `HiddenMembersScreen.tsx`, `SubscriptionsScreen.tsx`, `ProfileScreen.tsx`, `ProfileDetailsScreen.tsx`, `ProfileSummaryCard.tsx`

Create `src/components/MemberAvatar.tsx`:
```tsx
export function MemberAvatar({ member, size = 44 }: { member: Pick<Member, 'name' | 'avatarUrl'>, size?: number }) {
  if (member.avatarUrl) {
    return <Image source={{ uri: member.avatarUrl }} style={{ width: size, height: size, borderRadius: size / 2 }} />;
  }
  const initial = (member.name ?? '?')[0].toUpperCase();
  return (
    <View style={{ width: size, height: size, borderRadius: size / 2, backgroundColor: Colors.primary, justifyContent: 'center', alignItems: 'center' }}>
      <Text style={{ color: '#fff', fontFamily: Fonts.family, fontWeight: '600' }}>{initial}</Text>
    </View>
  );
}
```

Replace all hardcoded avatar `require(...)` calls with `<MemberAvatar member={member} />`.

Backend: add `avatarUrl` field to `MemberResponse` DTO (currently not included). Map from `member.avatarUrl` if the avatar upload endpoint (ONBOARD-1) stores a URL.

### HOME-2 · Real sync labels from wearable connection data
**Files:** `HomeScreen.tsx`, `MemberListScreen.tsx`

Add `lastSyncedAt` to `MemberResponse` or make a separate `GET /members/{id}/wearable/connection` call per member. Compute relative time: "Just now" / "X min ago" / "X hr ago". Show "No wearable" if no connection exists.

### HOME-3 · Real alert chip counts from metric summaries
**File:** `src/features/home/components/SharedMemberCard.tsx`

Replace hardcoded `2 High / 2 Low / 2 Normal` with computed counts from the member's metric summaries (available from `useMemberMetricSummaries`). Count readings by their `trendLabel` value (`HIGH` / `LOW` / `NORMAL`). Pass computed counts as props from `HomeScreen`.

### HOME-4 · Fix HomeHeader safe area top padding
**File:** `src/features/home/components/HomeHeader.tsx`

```ts
const insets = useSafeAreaInsets();
// Replace: paddingTop: 62
// With:    paddingTop: insets.top + 16
```

### METRICS-1 · Fix hardcoded date label
**File:** `src/features/metrics/MetricDetailsScreen.tsx:65`

```ts
const targetDate = new Date();
targetDate.setDate(targetDate.getDate() - dateOffset);
const isToday = dateOffset === 0;
const formatted = new Intl.DateTimeFormat('en-GB', { weekday: 'long', day: 'numeric', month: 'short' }).format(targetDate);
const dateLabel = isToday ? `${formatted} (Today)` : formatted;
```

### METRICS-2 · Wire DateStepper to actually refetch data
**Files:** `src/features/metrics/MetricDetailsScreen.tsx`, `src/features/metrics/hooks/useMetricDetail.ts`, backend `MetricsController.java`

1. Pass `dateOffset` to `useMetricDetail(memberId, metricType, range, dateOffset)`.
2. Hook includes `dateOffset` in the API query params.
3. Backend: add optional `?offsetDays=N` to `GET /members/{id}/metrics/{type}`. Shift query window: `until = now().minusDays(offsetDays)`, `since = until.minus(rangeInDays)`.

### METRICS-3 · Fix 1Y tab — add 365d backend range
**Backend:** `MetricRange.java` — add `YEAR("365d")` case. Add `MetricReadingRepository` query for 365-day range.

**Frontend:** `MetricDetailsScreen.tsx:45` — change mapping `'1Y': '365d'` (or `'1y'` matching new enum).

### MEMBERS-6 · Replace HomeHeader on MemberListScreen
**File:** `src/features/members/MemberListScreen.tsx`

Replace `ListHeaderComponent={<HomeHeader ... />}` with a simple page header:
```tsx
<View style={styles.header}>
  <Text style={styles.title}>Family Members</Text>
  <Pressable onPress={() => router.push('/members/invite')}><IconPlus /></Pressable>
</View>
```

---

## PHASE 4 — Data Pipeline Fixes

### WEARABLE-5 · Extend Spike metric type mappings (5 → full set)
**File:** `backend/.../wearable/service/WearableService.java:104–113`

Add known Spike event-type → MetricType mappings (verify exact strings against Spike API docs):
```java
case "hrv", "heart_rate_variability" -> MetricType.HEART_RATE_VARIABILITY;
case "spo2", "blood_oxygen"          -> MetricType.BLOOD_OXYGEN;
case "respiration", "breathing_rate" -> MetricType.RESPIRATION_RATE;
case "stress"                        -> MetricType.STRESS_LEVEL;
case "body_temperature", "skin_temp" -> MetricType.BODY_TEMPERATURE;
case "blood_glucose"                 -> MetricType.BLOOD_GLUCOSE;
case "vo2_max"                       -> MetricType.VO2_MAX;
case "training_load"                 -> MetricType.TRAINING_LOAD;
// Note: SCREEN_TIME, OUTDOOR_TIME, ROUTINE_ADHERENCE, LAST_ACTIVE_LOCATION
// have no Spike source — no device sends these. Mark as TODO with a comment.
```

Also add `BLOOD_PRESSURE` to `MetricType.java` enum and `src/types/index.ts` MetricType union (it was absent from both).

### WEARABLE-6 · Add Spike event deduplication
**Backend:**
1. New `V9__add_spike_events.sql`:
   ```sql
   CREATE TABLE spike_events (
       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
       spike_event_id VARCHAR(255) UNIQUE NOT NULL,
       processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
   );
   ```
2. `SpikeEvent` entity + `SpikeEventRepository`.
3. In `SpikeWebhookController`: extract Spike's event ID from header or payload. If `spikeEventRepository.existsBySpikeEventId(eventId)`, return 200 immediately. Otherwise save and process.

### WEARABLE-7 · Convert `WearableConnection.status` from String to enum
**Backend:**
```java
public enum WearableStatus { PENDING, CONNECTED, DISCONNECTED }
```
Add `@Enumerated(EnumType.STRING)` on `WearableConnection.status`. Update all string comparisons across `WearableService` to use the enum.

### PAYMENT-3 · Add DB index on `razorpay_subscription_id`
New migration `V7__add_indexes.sql`:
```sql
CREATE INDEX idx_subscriptions_razorpay_sub_id
    ON subscriptions (razorpay_subscription_id);
```

### PAYMENT-4 · Map `razorpay_plan_id` in Subscription entity
**File:** `backend/.../subscriptions/model/Subscription.java`

Add field:
```java
@Column(name = "razorpay_plan_id")
private String razorpayPlanId;
```
Set in `PaymentService.initiatePayment` after creating the Razorpay subscription.

### MEMBERS-7 · Add behaviour scope metric labels
**File:** `src/features/members/model/permissions.ts:27`

```ts
// Change:
behaviour: { metricLabels: [] }
// To:
behaviour: { metricLabels: ['Routine Adherence', 'Screen Time', 'Outside Time'] }
```

### BACKEND-1 · Fix scheduler: per-member transaction isolation
**Files:** `backend/.../metrics/service/BaselineService.java`, `InsightGenerationService.java`

Change inner per-member methods to `@Transactional(propagation = Propagation.REQUIRES_NEW)` so one member's failure doesn't roll back all others' computations.

### BACKEND-2 · Fix N+1 baseline queries in MetricsQueryService
**File:** `backend/.../metrics/service/MetricsQueryService.java`

Before the metric-type stream, bulk-fetch all baselines:
```java
Map<MetricType, MetricBaseline> baselines = metricBaselineRepository
    .findAllByMemberId(memberId).stream()
    .collect(Collectors.toMap(MetricBaseline::getMetricType, Function.identity()));
```
Use `baselines.get(metricType)` inside the stream instead of per-type DB calls.

---

## PHASE 5 — Polish & Quality

### NOTIF-2 · Add pull-to-refresh to NotificationsScreen
**File:** `src/features/notifications/NotificationsScreen.tsx`

Add `refreshControl={<RefreshControl refreshing={isRefreshing} onRefresh={refresh} />}` to the `ScrollView`.

### NOTIF-3 · Fix unsafe deep-link cast in NotificationsScreen
**File:** `src/features/notifications/NotificationsScreen.tsx:24`

```ts
try {
  if (notification.deepLinkTarget) router.push(notification.deepLinkTarget as Href);
} catch {
  console.warn('Invalid deep link target:', notification.deepLinkTarget);
}
```

### PROFILE-5 · Wire settings links
**File:** `src/features/profile/ProfileScreen.tsx:79–92`

```ts
// Documentation:
onPress={() => Linking.openURL('https://bonaca.in/docs')}
// Terms & Conditions:
onPress={() => Linking.openURL('https://bonaca.in/terms')}
// Privacy Policy:
onPress={() => Linking.openURL('https://bonaca.in/privacy')}
```

### PAYMENT-5 · Hide mock pay button in production builds
**File:** `src/features/subscription/PaymentGatewayScreen.tsx`

```tsx
{__DEV__ && <MockPayButton onPress={activateMockPayment} />}
```

### AUTH-API-1 · Add request timeout to API client
**File:** `src/lib/api/client.ts`

```ts
const controller = new AbortController();
const timeout = setTimeout(() => controller.abort(), 30_000);
try {
  const response = await fetch(url, { ...options, signal: controller.signal });
  // ...
} finally {
  clearTimeout(timeout);
}
```

### METRICS-4 · Fix BarChartCard fixed 326px width
**File:** `src/features/metrics/components/BarChartCard.tsx:14`

```ts
import { Dimensions } from 'react-native';
const { width } = Dimensions.get('window');
const CHART_WIDTH = width - 64; // 32px card padding each side
```

### BACKEND-3 · Fix AuthService.getMe wrong exception type
**File:** `backend/.../auth/service/AuthService.java:63`

Create `UserNotFoundException` → maps to 404. Replace `InvalidRefreshTokenException` with it.

### BACKEND-4 · Add `@NotBlank` to RefreshRequest
**File:** `backend/.../auth/dto/RefreshRequest.java`

Add `@NotBlank private String refreshToken;` and ensure `@Valid` annotation on controller.

### BACKEND-5 · Fix POST endpoints returning 200 instead of 201
**Files:** `AuthController.java`, `MembersController.java`, `InvitesController.java`, `WearableController.java`, `MockPaymentController.java`

Change resource-creation responses to `ResponseEntity.status(HttpStatus.CREATED).body(...)`.

### BACKEND-6 · Add pagination to list endpoints
**Files:** `NotificationsController.java`, `MetricsController.java` (insights), `InvitesController.java`

Add `page` / `size` query params using Spring's `Pageable`. Default `size=50`. Prevents unbounded result sets growing over time.

### STYLE-1 · Replace hardcoded hex colors with design token references
**Files:** `SharedMemberCard.tsx`, `ApplyAllPermissionsRow.tsx`, `PermissionScopeCard.tsx`, `metricDisplay.ts`

Add missing tokens to `src/theme/tokens.ts` if needed (alertHigh, alertLow, success, checkbox), then replace all hardcoded hex values with token references.

### STYLE-2 · Fix ScreenPlaceholder missing DM Sans font
**File:** `src/components/ScreenPlaceholder.tsx`

Add `fontFamily: Fonts.family` to all `Text` styles.

### STYLE-3 · Fix HiddenMembersScreen Image import
**File:** `src/features/members/HiddenMembersScreen.tsx:4`

```ts
import { Image } from 'expo-image'; // was react-native
```

### STYLE-4 · Fix SubscriptionsScreen close button position
**File:** `src/features/subscription/SubscriptionsScreen.tsx:296`

Replace `bottom: 358` hardcoded value with a flex-based or `position: 'relative'` layout within the drawer container so it positions correctly on all screen heights.

### DEAD-1 · Delete HealthKit/Health Connect stubs
**Delete:**
- `src/lib/health/HealthProvider.ts`
- `src/lib/health/appleHealth.ts`
- `src/lib/health/healthConnect.ts`
- `src/lib/health/index.ts`

Remove `'apple-health'` and `'health-connect'` from `WearableProvider` type in `src/types/index.ts`.  
Remove `providerLabels['apple-health']` and `providerLabels['health-connect']` from `ProfileScreen.tsx`.

### DEAD-2 · Resolve WearableConnectionStatus type conflict
**Files:** `src/types/index.ts`, `src/types/wearable.ts`

Delete the `WearableConnectionStatus` from `src/types/index.ts` (the lowercase `'connected' | 'disconnected' | 'needs-reauth'` version). The correct type is in `src/types/wearable.ts` (uppercase `'PENDING' | 'CONNECTED' | 'DISCONNECTED'`). Update all usages.

### DEAD-3 · Remove mock data files from production bundle
**Delete:**
- `src/features/members/mockData.ts`
- `src/features/metrics/mockData.ts`

Remove all imports of these files.

### DEAD-4 · Remove invalid PaymentMethodType entries
**File:** `src/types/index.ts`

Remove `'paypal' | 'amex' | 'mastercard' | 'apple-pay'` — only `'upi'` and `'card'` per PRD §6.1.

---

## Task Count by Phase

| Phase | Tasks | Scope |
|---|---|---|
| P0 — Critical Security | 6 | Backend |
| P1 — Broken Core Flows | 14 | Frontend + Backend |
| P2 — Missing Features | 11 | Frontend + Backend |
| P3 — Real Data | 7 | Frontend + Backend |
| P4 — Data Pipeline | 8 | Backend |
| P5 — Polish | 16 | Frontend + Backend |
| **Total** | **62** | **All pure code changes — no API keys needed** |

---

## Feature Readiness After Each Phase

| Phase complete | Working features |
|---|---|
| After P0 | Security safe to deploy; webhooks auth'd |
| After P0 + P1 | Full auth flow; wearable status shows correctly; OTP error messages clear |
| After P0–P2 | Insights feed visible; push notifications delivered; member management fully functional; edit profile works |
| After P0–P3 | All screens show real member data (no placeholders); home screen live |
| After P0–P4 | Wearable data flows for all metric types; DateStepper and 1Y tab work; payment fully functional |
| After P0–P5 | Production-grade code quality; dead code removed; consistent styling |
| **After all + API keys** | **Fully production-ready** |
