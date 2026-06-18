# Bonaca — Claude Code Project Guide

## Project Overview

Bonaca is a React Native (Expo) mobile app letting adult children remotely monitor an aging parent's health and activity via wearables. Full product context lives in [`docs/PRD.md`](docs/PRD.md) — read it before making product decisions; this file is machine/architecture context only.

## Source of Truth

- **Figma file**: "Bonaca Designs", `fileKey YnsqSySyT8WTYeJPwjO6iV`, page "Mobile Screens" (`nodeId 0:1`). Pull fresh via the Figma MCP tools (`get_metadata`, `get_design_context`, `get_screenshot`) rather than re-describing screens from memory — the design is the spec.
- **PRD**: [`docs/PRD.md`](docs/PRD.md) — every functional requirement there is traceable to a Figma screen/state.
- **Market research**: [`docs/MARKET_RESEARCH.md`](docs/MARKET_RESEARCH.md) — competitive landscape and the differentiation bets baked into the PRD's NFRs (confidence-scored alerting, regulatory-safe insight copy, NRI-diaspora GTM).
- **Technical Requirements Document**: [`docs/TECHNICAL_REQUIREMENTS.md`](docs/TECHNICAL_REQUIREMENTS.md) — the backend/infra decisions referenced in Tech Stack below, with full rationale and a build-sequencing milestone plan.
- **Design tokens** (`src/theme/tokens.ts`): DM Sans font; header gradient `#090c2c` → `#555ec2`; background `#fafafa`; card border `#e4e9e7`; 16px card radius.
- **Representative node-IDs** for the highest-traffic screens (jump straight to `get_design_context` instead of re-discovering):

  | Section | nodeId |
  |---|---|
  | Home - Primary | `188:2977` (section) / `188:2859` (Home frame) |
  | Member Details | `196:4233` |
  | Metric Details | `197:1137` |
  | Connect Wearable (onboarding) | `60:634` |
  | Profile - Primary Member | `197:4003` |
  | Profile - Secondary Member | `197:5916` |

## Domain Model

Defined in `src/types/index.ts`: `Account` (subscriber, holds `Subscription`), `Member` (role: primary/secondary/tertiary, pinned/hidden flags), `WearableConnection` (provider: apple-health/health-connect/fitbit/garmin), `MetricReading` (typed by `MetricType`), `Insight` (trend/anomaly), `Subscription` (trial/active/expiring/expired/cancelled, **account-level not per-member**), `SharingGrant` (vitals/activity/behaviour/location scopes), `Notification`, `Invite`.

## Architecture

```
src/
  app/                    # Expo Router routes — thin, no business logic
    (auth)/               # splash, login, otp, complete-profile, connect-wearable
    (tabs)/               # home, notifications, profile
    member/[memberId]/    # Member Details + metric/[metricType] drill-down
    subscription/         # payment-gateway, select-wearable-account
  features/               # one feature = one folder: auth, onboarding, home, members,
                           # metrics, insights, notifications, subscription, profile
  components/              # shared UI (ScreenPlaceholder, etc.)
  lib/health/              # HealthProvider interface + apple-health/health-connect stubs
  lib/api/                 # stub for future backend client
  theme/                   # design tokens
  types/                   # domain model
docs/PRD.md
```

`@/*` resolves to `src/*` (see `tsconfig.json`).

## Wearable Integration Phasing

- **Phase 1 (current target)**: Apple HealthKit + Google Health Connect only, via `src/lib/health/HealthProvider.ts` (provider-agnostic `connect()`/`disconnect()`/`fetchMetrics()` interface). **Critical constraint**: both are on-device-only with no remote API — a parent's data can't be read from a child's phone directly. The Secondary Member's own device must run Bonaca to sync local health data to a backend; do not design any flow that assumes remote read access to another device's HealthKit/Health Connect store.
- **Phase 2 (do not start without separate scoping)**: Direct per-vendor OAuth — use the **Google Health API** (`health.googleapis.com/v4`) for Fitbit, not the legacy Fitbit Web API (sunsets September 2026, tokens don't carry over). Garmin Health API is separate. Consider a wearable aggregator (e.g. Terra) once 3+ providers are in scope.
- Do not install `react-native-health`, Health Connect native modules, or any wearable OAuth SDK without an explicit task to do so — `src/lib/health/*` are stubs by design.

## Tech Stack

**Mobile (built):**
- Expo (~56) + React Native (0.85) + TypeScript, Expo Router (file-based routing, typed routes enabled).
- React Compiler experiment enabled (`app.json` → `experiments.reactCompiler`).
- ESLint (`eslint-config-expo`) + Prettier (`eslint-config-prettier` disables conflicting style rules).

**Backend (decided in `docs/TECHNICAL_REQUIREMENTS.md`, not yet implemented):**
- **Supabase** (Postgres + Edge Functions, `ap-south-1`/Mumbai region) — chosen primarily because Row Level Security maps directly onto the Primary/Secondary/Tertiary `SharingGrant` permission model. Time-series rollups via plain Postgres + `pg_cron`, not TimescaleDB (deprecated on new Supabase Postgres-17 projects, and not needed at this app's data volume).
- **MSG91** for OTP SMS delivery (called from a Supabase Edge Function, not Supabase's built-in phone auth) — India requires DLT template registration before any OTP SMS can be sent; this has multi-day lead time and should be started early, independent of when the rest of the backend is built.
- **Payments — platform-and-region-specific, not a single integration**: Apple requires StoreKit/IAP on iOS everywhere, including India (their "UPI" option funds Apple's own billing, it is not a bypass). Android in India can use Google's User Choice Billing to route through Razorpay directly at a reduced fee — this is the only path where the Figma "Payment Gateway" screen's direct-billing assumption holds as designed. Use **RevenueCat** to unify entitlements across both rails rather than hand-rolling receipt validation per platform.
- **Expo's push notification service** (not direct FCM/APNs — natural fit given the Expo client).
- **Sentry** (crash/error reporting) + **PostHog** (product analytics) to actually measure the PRD's Success Metrics from day one.
- No state management library yet — not decided.

## Dev Conventions

- One feature = one folder under `src/features/*`; route files under `src/app/` stay thin and delegate to feature code.
- Use the `@/*` path alias rather than relative `../../..` imports.
- New domain types go in `src/types/index.ts`, not scattered per-feature.
- Ground new screens in the actual Figma node for that screen — don't invent layout/copy.

## Not Set Up Yet (decided, but not yet built)

Stack choices for all of these are now decided (see Tech Stack above and `docs/TECHNICAL_REQUIREMENTS.md` §12 for build order) — none are implemented yet:

- Authentication (Supabase Auth + MSG91 OTP delivery, DLT registration not yet started).
- Backend (Supabase project not yet provisioned — no API server, no database).
- Payment SDK (RevenueCat + StoreKit/Play Billing/Razorpay per the platform-and-region split above; Figma designs the UI, none of it is wired to a processor).
- Push notifications (Expo push service).
- Real HealthKit / Health Connect native calls — `src/lib/health/appleHealth.ts` and `healthConnect.ts` are stubs.
- State management library — genuinely undecided, not just unbuilt.

Do not install SDKs or scaffold infra for any of the above without an explicit task — follow the milestone order in `docs/TECHNICAL_REQUIREMENTS.md` §12 rather than building ad hoc.
