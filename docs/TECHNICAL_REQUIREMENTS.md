# Bonaca — Technical Requirements Document (TRD)

Grounded in [`docs/PRD.md`](PRD.md), [`docs/MARKET_RESEARCH.md`](MARKET_RESEARCH.md), [`CLAUDE.md`](../CLAUDE.md), and the actual scaffold (`src/types/index.ts`, `src/lib/health/HealthProvider.ts`, `package.json`). Written for the current team shape — one engineer (you) plus one sales/marketing person, more engineers joining soon — under an explicit "good product first" priority. Every recommendation favors quality and a low-but-not-zero ops burden over the absolute cheapest/fastest option, and avoids team-scale infrastructure the team doesn't have yet.

## Recommended Stack at a Glance

| Layer | Recommendation | Why (one line) |
|---|---|---|
| Mobile client | Keep Expo (~56) + React Native + TypeScript, already scaffolded | Validated, not just assumed — see §1 |
| Backend platform | **Supabase** (Postgres + Auth scaffolding + Edge Functions + Storage), region `ap-south-1` (Mumbai) | Best fit for the relational family/permission model + RLS; lowest ops burden for a solo engineer; real Postgres a future hire will already know |
| Time-series strategy | Plain Postgres: raw `metric_readings` table + scheduled rollup tables via `pg_cron`, **not** TimescaleDB | TimescaleDB is deprecated on new Supabase Postgres-17 projects (§3); data volume doesn't need it anyway |
| OTP/SMS | **MSG91** (India-first pricing/DLT support), called from a Supabase Edge Function — not Supabase's built-in phone-auth SMS provider | Cheapest at ~₹0.15/OTP, India DLT-compliant; Supabase's default providers aren't built for India DLT template registration |
| Payments | **RevenueCat** wrapping StoreKit (iOS) + Play Billing (Android default) + Google **User Choice Billing → Razorpay** for India Android | The Figma "pay Razorpay/UPI directly" assumption only fully holds on Android-in-India; see §5 — this is the finding most likely to surprise you |
| Wearable sync | Parent-side sync companion (already required, on-device-only constraint) pushing batched readings to a Supabase Edge Function endpoint | No change from CLAUDE.md's existing Phase 1 plan — confirmed correct by research |
| Push notifications | Expo Push Notification Service | Simplest path for an Expo app; only worth swapping to direct FCM/APNs if a specific feature needs it later |
| Hosting/CI | Supabase-managed Postgres/Functions (no separate server to run) + EAS Build/Update + GitHub Actions | No infrastructure to operate beyond Supabase's dashboard and EAS |
| Observability | Sentry (crashes/errors) + PostHog (product analytics, funnels) | Together they cover both "what broke" and "where users drop off"; PostHog's free tier is generous enough for pre-revenue stage |
| Repo structure | Single repo, add a `supabase/` directory (migrations + Edge Functions) alongside the existing `src/` Expo app | No second repo needed yet — Supabase removes the case for a separate backend service repo |

---

## 1. Mobile Client

**Validating the existing choice, not re-deciding it.** React Native holds ~42% cross-platform market share in 2026 vs. Flutter, with roughly 2.5x the open job postings and lower hourly rates — relevant once you're hiring. React Native's New Architecture (JSI + Fabric, already the default in RN 0.85 per `package.json`) removed the old bridge performance penalty; Flutter still edges ahead on raw graphics-heavy rendering, which is not Bonaca's workload (forms, lists, charts — not games or heavy animation). Expo specifically is described in 2026 guidance as "the recommended approach" for React Native — managed workflow, EAS, and Expo Router give you nearly everything a custom bare-RN setup would, without the native-tooling maintenance burden a solo engineer can't easily absorb. **Conclusion: no reason to reconsider Expo/React Native.**

- **EAS Build + EAS Update**: cloud-compiled native builds (no local Xcode/Android Studio dependency) plus instant JS/asset OTA updates without App Store review for non-native changes. A dedicated "solo dev playbook" from Expo (Jan 2026) covers exactly this workflow. Free tier covers ~15 iOS + 15 Android builds/month — enough pre-launch; budget for the $19/mo Starter tier (3,000 MAUs) once you have real users.
- **Offline support**: wearable metric data syncs intermittently by nature (parent's phone isn't always online). Use a local SQLite cache (`expo-sqlite` or `@op-engineering/op-sqlite`) on the parent-side sync companion to queue readings when offline, flushing to the backend on reconnect — standard pattern, no exotic offline-sync framework needed at this scale.

## 2. Backend Platform

CLAUDE.md currently states "no backend of any kind" — this is the highest-leverage open decision. Compared against Bonaca's actual requirements (not generic feature lists):

| Requirement | Supabase | Firebase | Custom Node/NestJS + Postgres | Convex |
|---|---|---|---|---|
| Relational family/permission model (`Account`→`Member`→`SharingGrant`) | **Native fit** — real Postgres, foreign keys, joins | Firestore is document/NoSQL — this model gets awkward fast | Native fit, but you build everything | Document-reactive, not relational — same awkwardness as Firestore |
| Row-level access control (a Tertiary member should only see what `SharingGrant` permits) | **Row Level Security (RLS) policies enforced at the DB layer** — exactly this use case | No native RLS equivalent; enforce in app code, easy to get wrong | You write and maintain the authorization layer yourself | No native RLS equivalent |
| TypeScript fit for a solo full-stack engineer | Auto-generated TS types from schema; Edge Functions in TS/Deno | TS SDKs exist, less schema-driven | Full TS, but you own the framework choices too | TS-native by design — best DX here, but at the cost of relational/RLS fit above |
| Ops burden today (1 person) | Low — managed Postgres, managed Auth scaffolding, managed Functions | Low | **High** — you provision, patch, and scale a server yourself | Low |
| Team-readiness soon | Good — Postgres is something every backend hire already knows | Good, but Firestore data-modeling knowledge is more niche | Best long-term flexibility, but means a new hire's first weeks are infra, not features | Smaller hiring pool familiar with Convex's model |
| Cost at early scale | Free tier, then ~$25/mo Pro | Free tier, scales similarly | You pay for compute either way, plus your own time running it | Free tier, then usage-based |

**Recommendation: Supabase.** The RLS fit for Bonaca's exact permission model (Primary/Secondary/Tertiary visibility via `SharingGrant`) is the deciding factor — it's the one requirement where Supabase is structurally right and the alternatives are structurally awkward, not just "also possible." It also keeps you in real Postgres, which de-risks hiring (every backend engineer already knows SQL) versus Firestore or Convex's more specific data models. Project region: `ap-south-1` (Mumbai) — lowest latency for the India-first user base and the safer read of DPDP's ambiguous health-data-localization signal (§10).

**What Supabase does *not* replace**: if a future requirement needs heavy custom compute (e.g., a more sophisticated ML-based insight engine than the rule-based confidence scoring in §6 below), a small standalone Node/NestJS worker service can be added later without throwing away Supabase — Supabase Postgres can still be the database, with a separate compute service reading/writing to it. Don't build that service now; nothing in the current PRD requires it.

## 3. Data Layer / Time-Series Strategy

The PRD's chart-performance NFR requires pre-aggregation for the Metric Details 1Y view, not client-side rollup of raw readings. Three options were compared:

- **TimescaleDB extension**: purpose-built for exactly this, but **deprecated on new Supabase Postgres-17 projects** as of 2026 (still supported on Postgres 15 until ~May 2026, then requires migrating off). Starting a new project on a path that's already being sunset on your chosen platform is the wrong call.
- **Plain Postgres materialized views**: require manual or cron-triggered refresh; fine, but you manage the refresh logic yourself.
- **Plain Postgres scheduled rollup tables + `pg_cron`** (available as a Supabase extension): a `metric_readings` raw table (one row per reading, matching `MetricReading` exactly) plus `metric_daily_rollups` / `metric_weekly_rollups` tables populated by a `pg_cron` job (or a scheduled Supabase Edge Function) running every few minutes to hours. Metric Details' 1D/7D views can query raw `metric_readings` directly (low row count per member per day); 4W/1Y views read the rollup tables.

**Recommendation**: scheduled rollup tables via `pg_cron`, not TimescaleDB. Bonaca's actual data shape — a handful of family members per account, ~7 metric types, readings at most every few minutes — is nowhere near the volume TimescaleDB is justified for (its sweet spot is thousands of sensors reporting every few seconds). This is the "don't over-architect" call explicitly requested.

## 4. Auth & OTP Delivery for India

Compared MSG91, Twilio Verify, Firebase Phone Auth, Gupshup, Exotel on India-specific OTP pricing and compliance:

| Provider | ~Cost per OTP (India) | Notes |
|---|---|---|
| **MSG91** | ~₹0.15 | Cheapest researched; built around India compliance workflows |
| Gupshup | ~₹0.17 | Close second |
| Firebase Phone Auth | ~₹0.83 (~$0.01) | Free up to 50K MAU on broader Firebase Auth, but SMS itself is billed separately and isn't the cheap option for OTP specifically |
| Twilio Verify | ~₹0.45 (after forex margin) | 3x MSG91's cost for the same India OTP |

**Critical compliance step, easy to miss**: India's TRAI mandates **DLT (Distributed Ledger Technology) registration** for *any* entity sending SMS to Indian numbers — including OTP/transactional messages, not just promotional ones. Without registering your entity, sender header, and exact message template (with variables marked) on a DLT portal, telecom operators will **reject the SMS outright**, including OTPs at login. Registration takes ~2–7 business days for the entity, 1–3 days per header/template after that — this needs to happen *before* any real-user testing, not after. MSG91 has built-in tooling for this registration flow.

**Recommendation**: MSG91 for OTP delivery, called from a Supabase Edge Function (not Supabase Auth's built-in phone-auth provider, which isn't built around India's DLT template flow). Pattern: `request-otp` Edge Function generates a code, stores a hash + expiry in a Postgres table, sends via MSG91; `verify-otp` Edge Function checks the hash and, on success, mints a Supabase session for that user (via the Auth Admin API) rather than using Supabase's native phone-auth flow directly.

## 5. Payments — the Real Risk

This is the finding most likely to surprise you, because the Figma "Payment Gateway" screen with a UPI/PayPal/Amex/Mastercard/Apple Pay picker implicitly assumes Bonaca can always charge directly. **That's only fully true on one platform-region combination.**

- **iOS, almost everywhere including India**: Apple requires digital subscriptions to go through StoreKit/Apple's in-app purchase system (15–30% commission, 15% under the Small Business Program for <$1M/year revenue). Apple does support **UPI as a funding method in India** — but that UPI payment still flows *into Apple's own billing*, not around it. There is currently no confirmed India-specific carve-out letting you route iOS subscriptions directly to Razorpay/Stripe and skip Apple's cut.
- **iOS, US only, right now**: following a Ninth Circuit ruling, US apps can currently link out to external payment pages with **no Apple commission** — but this is under active appeal (Apple has petitioned the Supreme Court as of mid-2026) and a December 2025 appellate ruling already suggested Apple *could* reinstate a reduced fee on external purchases. **Do not architect Bonaca's core monetization around this — it's legally unstable and not your primary market anyway.**
- **iOS, EU/Japan/South Korea**: separate regulatory carve-outs exist (DMA in the EU, the Smartphone Act in Japan, a Korea-specific binary requirement) allowing external payment links, each with different fee structures (e.g., EU's "Core Technology Fee" alternative business terms). Not relevant to Bonaca's India + NRI-diaspora launch markets unless EU-resident NRI users become a real segment later.
- **Android, India specifically**: Google's **User Choice Billing** program is live in India (also EEA, Japan, Korea, Australia, Indonesia, others) — apps can offer Razorpay (or Stripe/PayU) as an alternative to Google Play Billing at a **reduced ~4% service fee** instead of the standard 15–30%. This is the one place the Figma design's "charge via Razorpay directly" assumption holds up cleanly and cheaply.
- **Android, elsewhere (including most NRI-diaspora markets unless also UCB-enabled)**: standard Google Play Billing applies.

**Recommendation**: Use **RevenueCat** to manage cross-platform entitlements regardless of which rail actually processes the payment — it unifies "is this user's `Subscription` active" into a single check (`isActive`) across iOS StoreKit and Android Play Billing, with a generous free tier up to $2,500 monthly tracked revenue, then 1% revenue share or a $99/mo flat plan. Concretely:
1. iOS: StoreKit via RevenueCat, full stop — no near-term path around Apple's cut for India/NRI markets.
2. Android in India: enroll in User Choice Billing, route through Razorpay at ~4% — meaningfully cheaper than the alternative, worth the integration effort.
3. Android outside UCB-enabled markets: standard Play Billing via RevenueCat.

This means the Figma "Payment Gateway" UI can stay as designed (the user never needs to see *which* rail processed their payment), but the engineering underneath is platform-and-region-branched, not a single direct Razorpay/Stripe integration as the Figma payment-method picker might suggest at first glance. Update the PRD's "Open gap: no explicit `PaymentMethod` type" note to also record *which processing rail* (`storekit` | `play_billing` | `play_ucb_razorpay`) handled each transaction, for reconciliation.

## 6. Wearable Sync Architecture

No change to the Phase 1 plan already in `CLAUDE.md` and confirmed by `MARKET_RESEARCH.md` §10 (no aggregator is built for Bonaca's parent-syncs-their-own-data topology). Concretely for the sync companion (the Secondary Member's own Bonaca install):

- **Trigger**: a background task (`expo-background-task` / iOS BGTaskScheduler equivalent, Android WorkManager equivalent under the hood) runs periodically (e.g., every 1–4 hours, plus on app foreground) to pull new HealthKit/Health Connect samples since the last successful sync.
- **Batching**: send readings in batches (e.g., up to a few hundred rows per request) to a single Supabase Edge Function endpoint, not one network call per reading.
- **Retry/backoff**: on failure, queue locally (SQLite, per §1) and retry with exponential backoff; surface the **Connection Issue - Retry** and **Card - Disconnected** Figma states based on consecutive-failure count and `lastSyncedAt` staleness, exactly as the PRD's NFR already specifies — this is a UI/state-machine concern wired to a simple failure counter, not new infrastructure.
- **Design the ingestion payload like a normalized aggregator schema now** (per‑metric-type, timestamped, source-tagged) — `MarketResearch.md` §10's specific recommendation — so swapping in Terra/Vital for Phase 2 providers later doesn't require a second data model on the backend.

## 7. Push Notifications

Given the mobile client is Expo: use the **Expo Push Notification Service**. It wraps FCM (Android) and APNs (iOS) behind one API and one set of credentials, which is materially less setup than managing FCM/APNs tokens directly — and Bonaca's notification needs (an Insight/anomaly fired → push the Primary Member) don't require any FCM/APNs-specific feature Expo's service lacks. Reassess only if a specific future need (e.g., very high-volume notification fan-out, or platform-specific rich-notification features Expo's API doesn't expose) requires it — not a present concern.

## 8. Hosting & CI/CD

- **Backend**: Supabase is managed — no server to provision or patch. This is a deliberate ops-burden reduction for a one-person engineering team.
- **Mobile**: EAS Build (cloud-compiled native binaries for both platforms) + EAS Update (OTA JS/asset pushes without App Store review for non-native changes).
- **CI**: GitHub Actions triggering `eas build`/`eas update` on merge to `main` (or a `release` branch once the team grows past one person and you want a review gate before triggering builds). Supabase migrations can run via the Supabase CLI in the same GitHub Actions workflow (`supabase db push`) so schema changes are versioned and reviewed alongside app code, not applied by hand through the dashboard once a second engineer joins.

## 9. Observability & Analytics

To actually measure the PRD's Success Metrics from day one:

- **Sentry**: crash/error reporting with first-class React Native support — answers "what broke and where."
- **PostHog**: product analytics — funnels, event tracking, session replay; free tier is generous (100x the session replays, 200x the events of Sentry's free analytics tier) which matters pre-revenue. Use it to directly instrument the PRD's four Success Metrics: invite-to-connect activation (funnel: Invite sent → Secondary Member Connect Wearable complete), weekly engagement (Home/Member Details view events), trial→paid conversion (subscription state-change events, tracked separately at the *second* renewal per `MARKET_RESEARCH.md` §6's retention-gap finding), and anomaly-to-acknowledgement time (timestamp delta between an `anomaly`-kind `Insight`'s Notification firing and being opened).
- Both have generous free tiers suited to pre-revenue/early-revenue stage; no cost concern blocking adoption from day one.

## 10. Security & Compliance

- **Encryption**: Supabase Postgres encrypts data at rest by default; all client↔Supabase traffic is TLS. No additional work needed for baseline encryption-in-transit/at-rest — verify this is enabled (it is, by default) rather than building anything custom.
- **Secrets management**: Supabase Edge Function secrets (MSG91 API key, RevenueCat webhook secret, Razorpay keys) via Supabase's built-in secrets store — never commit to the repo, never hardcode in the Expo client bundle (anything in client code is extractable).
- **DPDP Act data-residency signal is genuinely ambiguous in current public guidance**: the DPDP Act 2023 itself uses a "negative list" model for cross-border transfer (allowed everywhere except government-restricted destinations, none published as of mid-2026) rather than the older Personal Data Protection Bill's strict localization mandate — but some secondary sources still describe a stricter expectation specifically for "sensitive personal data" categories including health data, and sector-specific health-data rules (National Digital Health Mission) recommend localized storage. Given that ambiguity, **hosting the Supabase project in `ap-south-1` (Mumbai)** is the safe default regardless of which reading turns out correct — it's also simply the lowest-latency choice for an India-first user base, so there's no tradeoff in choosing it now.
- **DPDP consent**: per the PRD's NFR, build Bonaca's own granular consent UI (separate toggles for service delivery vs. personalized insights vs. family sharing) now; do not attempt to become a registered DPDP "Consent Manager" yourselves (a heavy, separately regulated role) — integrate with a third-party registered Consent Manager once that ecosystem matures, as `MARKET_RESEARCH.md` §9 already recommends.

## 11. Repo/Team Structure

**Single repo, evolving as the team grows** — no premature split:
- Keep the existing Expo app at the repo root (`src/`, `app.json`, etc., unchanged).
- Add a `supabase/` directory: `supabase/migrations/` (SQL schema, versioned, matching `src/types/index.ts`), `supabase/functions/` (Edge Functions: `request-otp`, `verify-otp`, wearable-ingest, RevenueCat/Razorpay webhook handlers).
- Generate Supabase's TypeScript types from the schema into `src/types/database.ts` (auto-generated, not hand-maintained) so the mobile client and backend share a single source of truth for table shapes, separate from the existing hand-written domain types in `src/types/index.ts` (which describe the product's conceptual model — keep both, they serve different purposes).
- Revisit a monorepo tool (pnpm workspaces, Turborepo) only if a separate standalone backend service (§2's "what Supabase doesn't replace" case) actually gets built — not needed for a Supabase-as-backend architecture, and not needed for a one-person team today.

## 12. Build Sequencing — "First Review Model" Milestones

A concrete, ordered list to get something genuinely reviewable as fast as possible without skipping the things that are expensive to retrofit (RLS, DLT registration, the confidence-scored insight model):

- **M0 — Foundation**: Supabase project (Mumbai region) + schema migrations matching `src/types/index.ts` + RLS policies enforcing the Account/Member/SharingGrant visibility model; MSG91 DLT entity/template registration (start this immediately — it has multi-day lead time); `request-otp`/`verify-otp` Edge Functions; wire the existing `(auth)` routes (Login, OTP, Incorrect OTP, Resend OTP, Complete Profile) to real auth instead of `ScreenPlaceholder`.
- **M1 — Wearable connect + sync**: real HealthKit/Health Connect calls in `src/lib/health/appleHealth.ts` / `healthConnect.ts` (replacing the stubs); background sync task on the parent-side install; wearable-ingest Edge Function; wire Connect Wearable, Select Wearable Account, Connection Issue - Retry, and Card - Disconnected to real connection state.
- **M2 — Read paths**: Home (Primary/Secondary), Member Details (Vitals/Activity/Behaviour tabs), Metric Details (1D/7D/4W/1Y) wired to real `metric_readings` + rollup tables; Pin/Unpin, Edit Nick Name, Hidden Members as real mutations. **This is the first genuinely demoable slice** — real data flowing from a parent's wearable to a child's screen — and a reasonable point to call the "first review model" complete.
- **M3 — Insights & Notifications**: confidence-scored anomaly detection (time-window collapse + severity scoring per the PRD's NFR) generating real `Insight` rows with regulatory-safe copy templates; Expo push wired to real Notifications with deep-linking into Metric Details.
- **M4 — Monetization**: RevenueCat integration; StoreKit (iOS) + Play Billing (Android default) + Google User Choice Billing/Razorpay (India Android); Free Trial/expiring/expired banner logic wired to real `Subscription` state; Payment Gateway screen wired to real entitlements.
- **M5 — Permissions & Invite**: Invite flow creating real `Invite`/`Member` rows; Edit Permissions wired to real `SharingGrant` CRUD; DPDP consent UI (separate toggles per the §10 requirement) surfaced as part of onboarding and the Edit Permissions screen.

M0–M2 is the recommended target for the "first review model" the founder asked for — it proves the hardest, most novel part of the product (cross-device wearable data flowing through a real backend into the family dashboard) without yet needing payments or notification-tuning work, both of which are easier to iterate on once the core data pipeline is proven.

## Sources

- [App Store & Google Play Policy Changes 2026](https://www.appsonair.com/blogs/2025-mobile-app-store-policy-updates)
- [Google Allows External Payments Following Upheld Injunction](https://technologylaw.fkks.com/post/102lroc/google-allows-external-payments-following-upheld-injunction)
- [Apple Alternative Payment Fees: What Developers Pay (2026)](https://www.neonpay.com/blog/apple-app-store-alternative-payment-fees-what-developers-pay-in-2026)
- [App-to-web: navigating external purchases — RevenueCat](https://www.revenuecat.com/blog/engineering/app-to-web-purchase-guidelines/)
- [Update on apps distributed in the European Union — Apple Developer](https://developer.apple.com/support/dma-and-apps-in-the-eu/)
- [Apple's June 2025 EU update — RevenueCat](https://www.revenuecat.com/blog/growth/apple-eu-dma-update-june-2025/)
- [Japan's Smartphone Act (MSCA) — SCiDA](https://scidaproject.com/2025/12/15/japans-smartphone-act-mscain-the-shadow-of-competition-law-under-enforcement/)
- [StoreKit External Purchase Entitlement Addendum KR — Apple](https://developer.apple.com/contact/request/download/StoreKit_External_Purchase_Entitlement_Addendum_KR.pdf)
- [Epic Games v. Apple — Wikipedia](https://en.wikipedia.org/wiki/Epic_Games_v._Apple)
- [Apple Asks Supreme Court to Review App Store Contempt Ruling — MacRumors](https://www.macrumors.com/2026/05/21/apple-supreme-court-epic-games-case/)
- [How to Get Cross-Platform Subscriptions Right — RevenueCat](https://www.revenuecat.com/blog/engineering/cross-platform-subscriptions-ios-android-web/)
- [RevenueCat Integration Guide 2026 — Lushbinary](https://lushbinary.com/blog/revenuecat-integration-guide-native-vs-revenuecat-2026/)
- [India SMS Regulations, DLT Registration & TRAI Compliance Guide 2026 — Message Central](https://www.messagecentral.com/sms-guideline/india)
- [DLT Registration in India 2026 — Webxion](https://www.webxion.com/dlt-registration-in-india-trai-rules-registration-process/)
- [MSG91 DLT Registration](https://msg91.com/help/dlt-registration-in-india)
- [Top 10 Firebase SMS OTP Alternatives 2026 — Prelude](https://prelude.so/blog/top-10-firebase-sms-otp-alternatives)
- [MSG91 Pricing 2026 — G2](https://www.g2.com/products/msg91/pricing)
- [Enrolling in the user choice billing pilot — Play Console Help](https://support.google.com/googleplay/android-developer/answer/12570971?hl=en)
- [Changes to Google Play's billing requirements for India — Play Console Help](https://support.google.com/googleplay/android-developer/answer/13306652?hl=en)
- [Understanding user choice billing on Google Play](https://support.google.com/googleplay/android-developer/answer/13821247?hl=en)
- [About billing for Apple subscriptions and media products in India — Apple Support](https://support.apple.com/en-us/108110)
- [Convex vs Supabase — Bytebase](https://www.bytebase.com/blog/convex-vs-supabase/)
- [Supabase vs Firebase vs Convex 2026 — Vibestack](https://www.vibestack.io/blog/supabase-vs-firebase-vs-convex-2026)
- [timescaledb: Time-Series data — Supabase Docs](https://supabase.com/docs/guides/database/extensions/timescaledb)
- [TimescaleDB Postgres 17 deprecation discussion — Supabase GitHub](https://github.com/orgs/supabase/discussions/39479)
- [How to Use TimescaleDB Continuous Aggregates](https://oneuptime.com/blog/post/2026-01-27-timescaledb-continuous-aggregates/view)
- [Postgres Materialized Views, The Timescale Way](https://www.tigerdata.com/blog/materialized-views-the-timescale-way)
- [React Native vs Flutter vs Expo vs Lynx 2026 — DEV Community](https://dev.to/krunal_groovy/react-native-vs-flutter-vs-expo-vs-lynx-2026-which-to-choose-for-your-app-30h6)
- [Flutter vs React Native 2026 — Pagepro](https://pagepro.co/blog/react-native-vs-flutter-which-is-better-for-cross-platform-app/)
- [The solo dev playbook: ship faster with Expo, EAS Build, and OTA Updates](https://expo.dev/blog/building-a-cross-platform-app-without-touching-xcode-or-android-studio)
- [EAS Update: RN OTA Updates Guide 2026](https://reactnativerelay.com/article/react-native-ota-updates-eas-update-rollouts-rollbacks-cicd)
- [Expo Pricing 2026 — CheckThat.ai](https://checkthat.ai/brands/expo/pricing)
- [Expo vs Firebase FCM: Push Notification Provider Comparison 2026 — Courier](https://www.courier.com/integrations/compare/expo-vs-firebase-fcm)
- [Using push notifications — Expo Documentation](https://docs.expo.dev/guides/using-push-notifications-services/)
- [Data Localization vs Data Residency in India: 2026 Guide](https://www.questionpro.com/blog/data-localization-vs-data-residency-india/)
- [DPDP Act: India's New Era in Data Protection — Cockroach Labs](https://www.cockroachlabs.com/blog/dpdp-act-data-protection-and-privacy/)
- [DPDP Act 2023 & RBI Data Localization — Techtweek Infotech](https://techtweekinfotech.com/dpdp-act-rbi-data-localization-hosting-india/)
- [PostHog vs Sentry: Key Differences, Pricing & Best Use Cases (2026)](https://vemetric.com/blog/posthog-vs-sentry)
- [PostHog vs Amplitude in-depth tool comparison](https://posthog.com/blog/posthog-vs-amplitude)
