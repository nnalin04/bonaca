# Bonaca — Claude Code Project Guide

## Project Overview

Bonaca is a React Native (Expo) mobile app giving families consent-first visibility into an elderly parent's daily health and routine, using wearable and behavioural signals. Full product context lives in [`docs/PRD.md`](docs/PRD.md) — read it before making product decisions; this file is machine/architecture context only.

**Role naming (read this before touching anything role-related):** Primary Member = the person whose data is tracked, typically the elderly parent/data-owner. Secondary Member = the family member who views their data, typically the adult child, may be the payer. There is no Tertiary role — a Primary can share with up to 2 Secondary Members. **This is the opposite of what the Figma file's frame names and labels currently say** ("Profile - Primary Member" in Figma is the old payer/child meaning) — see the Source of Truth section below before assuming any screen already matches this.

## Source of Truth

- **PRD**: [`docs/PRD.md`](docs/PRD.md) mirrors [`docs/PRD.pdf`](docs/PRD.pdf), which is the authoritative product/business spec — read its "Realignment note" section for the full list of what's still out of sync elsewhere in the repo.
- **Figma file**: "Bonaca Designs", `fileKey YnsqSySyT8WTYeJPwjO6iV`, page "Mobile Screens" (`nodeId 0:1`). **Still uses the old, inverted Primary/Secondary labeling** (predates `docs/PRD.pdf`) — when grounding a screen in Figma, mentally swap the role labels rather than assuming the frame name is correct, until the Figma file itself is relabeled (a separate design task, not yet scheduled). Pull fresh via the Figma MCP tools (`get_metadata`, `get_design_context`, `get_screenshot`) rather than re-describing screens from memory — the design is the spec for layout, not for role labels. **If the Figma MCP server returns a "Starter plan tool call limit reached" error, it's a hard plan ceiling, not a transient rate limit** — don't retry it. Instead use the Figma REST API directly (`api.figma.com/v1/files/:key/nodes` and `/v1/images/:key`) with the token in `.env` (`FIGMA_API_TOKEN`, gitignored — confirmed working again as of 2026-06-21, the earlier "expired" note was stale), which has a separate, much higher quota. A full local cache of every screen (JSON layout + 2x PNG screenshot) already exists at `.design-reference/screens/` — see `.design-reference/manifest.md` for the node-ID → file mapping; check there before re-fetching.
- **PRD**: [`docs/PRD.md`](docs/PRD.md) — every functional requirement there is traceable to a Figma screen/state.
- **Market research**: [`docs/MARKET_RESEARCH.md`](docs/MARKET_RESEARCH.md) — competitive landscape and the differentiation bets baked into the PRD's NFRs (confidence-scored alerting, regulatory-safe insight copy, NRI-diaspora GTM).
- **Technical Requirements Document**: [`docs/TECHNICAL_REQUIREMENTS.md`](docs/TECHNICAL_REQUIREMENTS.md) — the backend/infra decisions referenced in Tech Stack below, with full rationale and a build-sequencing milestone plan.
- **Design tokens** (`src/theme/tokens.ts`): DM Sans font; header gradient `#090c2c` → `#555ec2`; background `#fafafa`; card border `#e4e9e7`; 16px card radius.
- **Representative node-IDs** for the highest-traffic screens (jump straight to `get_design_context` instead of re-discovering):

  | Section | nodeId |
  |---|---|
  | Home - Primary | `188:2977` (section) / `188:2859` (Home frame) |
  | Member Details | `196:4233` (section) / `43:4129` (canonical frame) |
  | Metric Details | `197:1137` (section) / `197:3828`, `197:3909` (frames) |
  | Splash | `43:3178` |
  | Login - Mobile No. Entry | `49:268` |
  | Login - OTP | `49:364` (+ `219:1488` Incorrect OTP, `49:483` Resend OTP) |
  | Complete Profile | `60:595`, `60:768` (two states) |
  | Connect Wearable (onboarding) | `60:634` |
  | Notifications | `286:15753` |
  | Select Wearable Account | `197:10387`, `197:11178`, `222:1723` (3 variants) + `225:3615` (Connection Issue - Retry) |
  | Payment Gateway | `197:10384` (trial signup), `197:11043` (renewal) |
  | Profile - Primary Member *(Figma label = old "payer/child" meaning)* | `197:4003` (section) / `39:2025` (frame) |
  | Profile - Secondary Member *(Figma label = old "parent" meaning)* | `197:5916` (section) / `197:5921` (frame), subscription states: `197:6270`/`197:7272`/`197:6835`/`197:7049` |

## Domain Model

Defined in `src/types/index.ts`: `Account` (subscriber, holds `Subscription`), `Member` (role: primary/secondary, pinned/hidden flags), `WearableConnection` (provider, synced via Spike API), `MetricReading` (typed by `MetricType`), `Insight` (trend/anomaly), `Subscription` (trial/active/expiring/expired/cancelled, **account-level not per-member**), `SharingGrant` (vitals/activity/behaviour scopes — no separate `location` scope, it's context within `behaviour`), `Notification`, `Invite`. **The actual `src/types/index.ts` and `backend/.../members/MemberRole.java` still have the old `primary/secondary/tertiary` enum and the 4-scope `SharingGrant` — this description reflects `docs/PRD.pdf`'s target shape, not the current code; realignment is tracked but not yet done.**

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
  lib/health/              # HealthProvider interface + provider stubs — target shape needs to change to a Spike API client, see Wearable Integration Phasing below
  lib/api/                 # real backend client (auth + members are wired up; see Not Set Up Yet)
  theme/                   # design tokens
  types/                   # domain model
docs/PRD.md
```

`@/*` resolves to `src/*` (see `tsconfig.json`).

## Wearable Integration Phasing

- **Per `docs/PRD.pdf` §3.1**: wearable data comes from the **Spike API** — a cloud-based, cross-vendor sync service (Garmin, Fitbit, Samsung, Oura, etc.), cost incurred per connected device. This **replaces** the on-device Apple HealthKit/Google Health Connect plan this section previously described, and removes the old "critical constraint" that the data-owner's own phone had to run Bonaca to sync — Spike syncs from the vendor's cloud once a device is connected, not from local on-device storage.
- `src/lib/health/HealthProvider.ts` and its `apple-health`/`health-connect` stubs are now the **wrong shape** for this — they need to become a Spike API client (auth/webhook/polling pattern, not a `connect()`/`fetchMetrics()` per-OS-provider interface). Not yet rebuilt; do not add real HealthKit/Health Connect native calls to the existing stubs.
- Do not install `react-native-health`, Health Connect native modules, the Spike SDK, or any wearable OAuth SDK without an explicit task to do so.

## Tech Stack

**Mobile (built):**
- Expo (~56) + React Native (0.85) + TypeScript, Expo Router (file-based routing, typed routes enabled).
- React Compiler experiment enabled (`app.json` → `experiments.reactCompiler`).
- ESLint (`eslint-config-expo`) + Prettier (`eslint-config-prettier` disables conflicting style rules).

**Backend (built — custom Java/Spring Boot, see `backend/`, not the Supabase plan `docs/TECHNICAL_REQUIREMENTS.md` §2 still recommends; see `docs/TECHNICAL/BACKEND_COMPARISON_AND_MIGRATION.md` for why):**
- Auth (phone+OTP, JWT access/refresh) and Members & Sharing (accounts/members/invites/sharing_grants) are implemented — **using the old `primary/secondary/tertiary` role naming and 4-scope `SharingGrant`**, not yet realigned to `docs/PRD.pdf`.
- **MSG91** for OTP SMS delivery — India requires DLT template registration before any OTP SMS can be sent.
- **Payments — simplified per `docs/PRD.pdf` §6: cards (global) + UPI only, flat ₹249/month, no platform/region-split billing.** This conflicts with Apple's App Store policy (StoreKit/IAP is required for iOS digital subscriptions almost everywhere, including India) — **this is an open question, not a resolved decision**: either the PDF's "cards" claim needs a StoreKit carve-out for iOS, or the PDF is wrong about iOS specifically. Don't build payment integration against either reading until this is explicitly resolved with the user.
- **Expo's push notification service** (not direct FCM/APNs — natural fit given the Expo client). Not yet built.
- **Sentry** (crash/error reporting) + **PostHog** (product analytics). Not yet built.
- No state management library yet — not decided.

## Dev Conventions

- One feature = one folder under `src/features/*`; route files under `src/app/` stay thin and delegate to feature code.
- Use the `@/*` path alias rather than relative `../../..` imports.
- New domain types go in `src/types/index.ts`, not scattered per-feature.
- Ground new screens in the actual Figma node for that screen — don't invent layout/copy.

## Not Set Up Yet

- **Wearable sync**: Spike API integration — `src/lib/health/*` are still HealthKit/Health Connect-shaped stubs that need rebuilding around Spike (see Wearable Integration Phasing above).
- **Payment**: nothing wired to a processor yet — Figma designs the Payment Gateway UI, but the cards+UPI-vs-StoreKit question above needs resolving before building this.
- **Push notifications** (Expo push service).
- **State management library** — genuinely undecided, not just unbuilt.
- **Role/scope realignment**: the built `Authentication` and `Members & Sharing` backend/frontend use the old `primary/secondary/tertiary` role naming and the old 4-scope (incl. `location`) `SharingGrant` model — both need reworking to match `docs/PRD.pdf` (2-Secondary-Member cap, all-on-by-default instant-apply permissions, 3 scopes). Tracked, not yet started.
- DLT template registration with MSG91 — confirm status before relying on real OTP SMS delivery in any non-dev environment.

Do not install SDKs or scaffold infra for any of the above without an explicit task.
