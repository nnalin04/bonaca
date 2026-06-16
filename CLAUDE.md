# Bonaca — Claude Code Project Guide

## Project Overview

Bonaca is a React Native (Expo) mobile app letting adult children remotely monitor an aging parent's health and activity via wearables. Full product context lives in [`docs/PRD.md`](docs/PRD.md) — read it before making product decisions; this file is machine/architecture context only.

## Source of Truth

- **Figma file**: "Bonaca Designs", `fileKey YnsqSySyT8WTYeJPwjO6iV`, page "Mobile Screens" (`nodeId 0:1`). Pull fresh via the Figma MCP tools (`get_metadata`, `get_design_context`, `get_screenshot`) rather than re-describing screens from memory — the design is the spec.
- **PRD**: [`docs/PRD.md`](docs/PRD.md) — every functional requirement there is traceable to a Figma screen/state.
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

- Expo (~56) + React Native (0.85) + TypeScript, Expo Router (file-based routing, typed routes enabled).
- React Compiler experiment enabled (`app.json` → `experiments.reactCompiler`).
- ESLint (`eslint-config-expo`) + Prettier (`eslint-config-prettier` disables conflicting style rules).
- No backend, no state management library, no auth/DB/payment SDK yet.

## Dev Conventions

- One feature = one folder under `src/features/*`; route files under `src/app/` stay thin and delegate to feature code.
- Use the `@/*` path alias rather than relative `../../..` imports.
- New domain types go in `src/types/index.ts`, not scattered per-feature.
- Ground new screens in the actual Figma node for that screen — don't invent layout/copy.

## Not Set Up Yet (explicitly deferred)

- Authentication (real OTP/SMS provider, session handling).
- Backend of any kind (API server, database/ORM).
- Payment SDK (UPI/PayPal/Amex/Mastercard/Apple Pay are designed in Figma, not wired to a processor).
- Push notifications.
- Real HealthKit / Health Connect native calls — `src/lib/health/appleHealth.ts` and `healthConnect.ts` are stubs.
- State management library.

Do not install SDKs or scaffold infra for any of the above without an explicit task — each is a deliberate architectural decision to be scoped on its own.
