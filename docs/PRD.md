# Bonaca — Product Requirements Document

**Source of truth:** Figma file "Bonaca Designs" (`fileKey YnsqSySyT8WTYeJPwjO6iV`, page "Mobile Screens"). Every requirement below is traceable to a named screen or state in that file — none are speculative additions.

## 1. Overview

Adult children increasingly live away from their aging parents — in another city or country — and have no easy way to know how their parent is actually doing day to day. Phone calls only catch what a parent chooses (or remembers) to mention; by the time a real problem surfaces, it's often already serious.

Bonaca closes that gap by reading the health and activity data already being collected by a parent's wearable (smartwatch, fitness band, or ring — Apple Watch, Fitbit, Garmin, Samsung Galaxy Watch, etc.) and surfacing it to the adult child in a single dashboard: vitals, activity, daily routine, screen time, time spent outdoors, and last known location. The child gets a continuously updated picture of their parent's wellbeing without needing the parent to do anything beyond wearing their device and completing a one-time, low-friction (OTP-only) connection.

## 2. Target Users

- **Primary Member** — the subscriber and account owner, typically the adult child. Pays for the subscription, invites other members, manages permissions, and can optionally monitor their own metrics too (Home - Primary screen shows "own device card" alongside the family list).
- **Secondary Member** — typically the parent. Authenticates via phone number + OTP only (no password), completes a minimal profile, and connects their own wearable from their own device (Profile - Secondary Member → Connect Wearable). UX for this role assumes low tech literacy: short flows, large touch targets, no jargon.
- **Tertiary / invited viewer members** — other family (siblings, other relatives) invited via the Invite flow, granted visibility into specific data scopes only (vitals / activity / behaviour / location) through Edit Permissions, not full account control.

## 3. Core User Journeys

1. **Onboarding** — Splash → Login - Mobile No. Entry → Login - OTP (with Incorrect OTP and Login - Resend OTP error/retry states) → Complete Profile → Connect Wearable.
2. **Connecting a wearable** — Select Wearable Account (list of supported providers) → pairing → Connection Issue - Retry on failure → confirmation Toast. Runs on whichever member's device is physically paired with the wearable (see NFR on HealthKit/Health Connect below) — this is why Secondary Member has its own independent Connect Wearable screen rather than the Primary Member configuring it remotely.
3. **Inviting a family member** — Primary Member sends an Invite (phone number + role offered); invitee accepts and lands in onboarding as a Tertiary member with default permissions.
4. **Daily monitoring loop** — Home (Primary or Secondary variant) → tap a family member's card → Member Details (Vitals / Activity / Behaviour tabs: routine adherence, screen time, outdoor time, last active location, Pin/Unpin, Edit Nick Name, Hidden Members for decluttering) → tap a metric → Metric Details (1D/7D/4W/1Y trend chart, min/max, auto-generated insight text, date stepper).
5. **Alerts and insights → Notifications** — anomaly or trend insights generated against MetricReading data surface as Notifications, deep-linking into the relevant Metric Details screen.
6. **Subscription lifecycle** — Free Trial → Banner - Subscription expiring soon → Banner - Subscription expired → Regular Subscription (Active) or Subscriptions - Cancelled, with Payment Gateway and a payment-method picker (UPI, PayPal, American Express, Mastercard, Apple Pay). Subscription is **account-level**, not per-member — one Primary Member's subscription covers all their connected Secondary/Tertiary members.
7. **Permission management** — Primary Member uses Edit Permissions to control what each invited member can see (SharingGrant scopes: vitals/activity/behaviour/location); Pin/Unpin and Hidden Members control Home-screen ordering/visibility without affecting underlying access.

## 4. Functional Requirements

Organized by Figma section, with every named state listed explicitly (no state invented or omitted):

- **Login & Onboarding**: Splash; Login - Mobile No. Entry; Login - OTP; Incorrect OTP (error state); Login - Resend OTP (retry state); Complete Profile (two states — initial entry and pre-filled edit); Connect Wearable.
- **Notifications**: single feed screen, deep-links into Metric Details.
- **Connecting a Wearable**: Payment Gateway (two variants — trial signup, renewal); Select Wearable Account (three variants — initial list, mid-flow, retry-entry); Connection Issue - Retry; confirmation Toasts (three variants for connect-success, connect-fail, generic).
- **Member List / Home - Primary**: own-device card, "Shared with you" family card list, empty state when no members yet connected, Banner - Free Trial / Subscription Active, Card+CTA pattern for "add a member."
- **Home - Secondary**: parent-facing home variant, Banner - When Trial Expired, own Regular Subscription / Payment Gateway access (parent can also manage billing, not only the child).
- **Member Details**: Vitals/Activity/Behaviour tabs across 7 distinct content states; Actions - Pin to top / Unpin to top; Actions - Edit Nick Name; Hidden Members list; Card - Disconnected (wearable lost connection — must be visually distinct from a normal data card).
- **Metric Details**: time-series chart with 1D/7D/4W/1Y range toggle, min/max display, auto-generated insight text, date-stepper navigation.
- **Profile - Primary Member**: Profile Details, Connect Wearable (for self-monitoring), Documentation (help/legal), Member list management, Drawer navigation, Edit Permissions, Subscriptions (two states — has plan / management view).
- **Profile - Secondary Member**: Profile Details, Connect Wearable, Documentation, full Subscriptions state machine — Subscriptions / Subscriptions - Empty / Subscriptions - Cancelled / Subscriptions - Expired / Subscriptions - Active — plus payment method selector (UPI, PayPal, American Express, Mastercard, Apple Pay).
- **Banner components** (cross-cutting, appear on Home and Profile): Free Trial, Subscription expiring soon, Subscription expired, Subscription Active, Subscription ended, When Trial Expired.

## 5. Data Model Sketch

Already implemented in `src/types/index.ts` — this section documents intent, not a new design:

- `Account` — the subscriber's billing entity, holds a `Subscription`.
- `Member` — `role: 'primary' | 'secondary' | 'tertiary'`, `pinned`/`hidden` flags for Home ordering.
- `WearableConnection` — `provider: 'apple-health' | 'health-connect' | 'fitbit' | 'garmin'`, `status: 'connected' | 'disconnected' | 'needs-reauth'`.
- `MetricReading` — typed by `MetricType` (heart_rate, steps, sleep, screen_time, outdoor_time, routine_adherence, last_active_location), tagged with `sourceDeviceId`.
- `Insight` — `kind: 'trend' | 'anomaly'`, generated text tied to a metric and date.
- `Subscription` — account-level, `status: 'trial' | 'active' | 'expiring' | 'expired' | 'cancelled'`.
- `SharingGrant` — `scope: 'vitals' | 'activity' | 'behaviour' | 'location'`, links a granter to a grantee member.
- `Notification`, `Invite` — as named.

**Open gap**: no explicit `PaymentMethod` type yet for UPI/PayPal/Amex/Mastercard/Apple Pay — currently implicit. Add when the Payment Gateway screen is actually implemented; not blocking for this PRD.

## 6. Non-Functional Requirements

- **Wearable data is on-device only — there is no remote API for HealthKit or Health Connect.** Apple HealthKit and Google Health Connect both store data locally to the phone they're installed on; there is no cloud endpoint a child's app can query to read a parent's data directly. This means the Secondary Member's own phone must run Bonaca (or a lightweight sync companion) to read local HealthKit/Health Connect data and push it to a backend, which the Primary Member's app then reads. The Figma design already assumes this — Profile - Secondary Member has its own independent Connect Wearable screen — but it must be called out explicitly so onboarding copy and support flows account for "your parent needs the app installed on their own phone too," which is easy to get wrong.
- **Fitbit API migration risk**: Fitbit's legacy Web API sunsets in September 2026; existing OAuth tokens do not carry over to its replacement, the Google Health API (`health.googleapis.com/v4`), which requires fresh user re-consent. Any Phase 2 Fitbit integration should be built against the Google Health API directly, not the legacy Fitbit Web API. Garmin and Samsung remain separate, vendor-specific OAuth APIs; a multi-wearable aggregator (e.g. Terra) is a viable build-vs-buy option once more than 2–3 providers are needed.
- **India DPDP Act 2023 compliance**: because the data subject (parent, Secondary Member) and the account holder (child, Primary Member) are different people, consent must be captured from *both* — the parent must explicitly consent to their health data being collected and shared with named family members, separately from the child's own account-creation consent. Consent must be revocable at any time via an accessible "consent dashboard" (maps naturally onto the existing Edit Permissions screen). Full DPDP enforcement (consent-manager registration, notice, and security obligations) takes effect May 13, 2027 — there is runway, but the consent model should be designed in from MVP rather than retrofitted.
- **Low-literacy / accessibility UX**: Secondary Member flows (OTP-only auth, Complete Profile, Connect Wearable) must use large touch targets, minimal text, and avoid technical jargon, per the design's phone-number-first approach.
- **Wearable sync reliability and staleness**: Card - Disconnected and Connection Issue - Retry states must be reachable from real sync failures, not just designed-but-unused; Home/Member Details should visibly indicate when a metric's `lastSyncedAt` is stale.
- **Chart performance**: Metric Details' 1Y rollup view must remain responsive — this implies pre-aggregation of `MetricReading` rather than client-side rollup of raw readings at query time.
- **Anomaly alerting without alert fatigue**: Insight generation (`kind: 'anomaly'`) needs a threshold/dedup strategy so Notifications don't overwhelm the Primary Member.

## 7. Success Metrics

- Invite-to-connect activation rate (Invite sent → Secondary Member completes Connect Wearable).
- Weekly engagement (Primary Member opens Home / views a Member Details screen).
- Trial → paid conversion rate, and time-to-cancel for Subscriptions - Cancelled.
- Anomaly-to-acknowledgement time (Insight of kind `anomaly` generated → Notification opened).

## 8. Phasing Plan

- **MVP** — Apple HealthKit + Google Health Connect only, via the parent-side sync-companion model described in the NFRs above. Core journeys: onboarding, connect wearable, daily monitoring loop, notifications, account-level billing with UPI-first payment (matches India-first user base), Edit Permissions for invited members.
- **Phase 2** — Direct wearable integrations beyond Apple/Google: Google Health API for Fitbit devices (not the legacy Fitbit Web API), Garmin Health API. Evaluate a multi-wearable aggregator (e.g. Terra) once a third or fourth provider is in scope, as a build-vs-buy call against maintaining N separate OAuth integrations.
- **Future** — Multi-account membership (one Secondary Member shared across multiple Primary Members' accounts), clinician/caregiver professional roles, web dashboard for Primary Members who want a larger-screen view.
