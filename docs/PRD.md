# Bonaca — Product Requirements Document

**Source of truth:** [`docs/PRD.pdf`](PRD.pdf) for every product/business rule in this document — this file is a markdown mirror of that PDF (kept in sync so the rest of the docs/codebase can reference it as text), not an independent source. Figma file "Bonaca Designs" (`fileKey YnsqSySyT8WTYeJPwjO6iV`, page "Mobile Screens") remains the source of truth for visual layout, but **the Figma screens and the existing codebase currently use the old, inverted Primary/Secondary terminology and predate this PDF** — see "Realignment note" at the bottom of this file before assuming any screen or code path already matches what's written here.

## 0. Executive Summary

Families don't want raw health data. They want **assurance, context, and early awareness** — without panic.

Bonaca enables safe, consent-first family visibility into daily health and routine changes using wearable and behavioural signals, designed especially for **elderly parents living independently** and **adult children living elsewhere (often globally)**.

## 1. Product Vision & Philosophy

### 1.1 Vision

A family health & routine awareness platform that helps people understand how their loved ones are doing day-to-day, without:

- medicalising daily life
- creating anxiety or false alarms
- invading privacy
- exposing elderly users to coercion or misuse

The product is about **awareness, not diagnosis**.

### 1.2 Core Insight

Families fundamentally ask **"Is today normal for them?"** — not "What was their heart rate at 3:12 PM?"

### 1.3 Phase-1 Principles

1. Wearable-first for credibility
2. Behavioural signals for context, not surveillance
3. Relative comparisons, not medical thresholds
4. Deterministic, explainable logic (no ML)
5. Primary-controlled data sharing only
6. Consent always precedes payment
7. Paid-only model with free trial
8. Trust > growth > virality

## 2. User Roles & Mental Model

### 2.1 Canonical Roles

**Primary Member**
- The person whose data is tracked — typically the **elderly parent / dependent**
- Owns all data
- Controls sharing & permissions
- May be low-tech

**Secondary Member**
- Family member who views another member's data — typically the **adult child** (often outside India)
- May be the payer
- Cannot access anything without explicit consent

Every user is always a Primary Member for their own data. A user may also be a Secondary Member for others.

There is no Tertiary role — see §9 Flow C for the cap on how many Secondary Members a Primary can share with.

### 2.2 Mental Model

"You always see yourself first. You only see others if they explicitly share."

No access requests, no search/discovery, no social graph.

## 3. Data Sources

### 3.1 Wearable Data (Trial + Paid Only)

- **Source: Spike API** (cloud-based sync; cost incurred per connected device)
- Devices: Garmin, Fitbit, Samsung, Oura, etc.
- Used for: Vitals, Sleep, Activity, Training load

This **replaces** the on-device-only Apple HealthKit / Google Health Connect plan in `CLAUDE.md`/`TECHNICAL_REQUIREMENTS.md` — Spike syncs from the vendor's cloud, so there is no longer a hard requirement that the Primary's own phone run Bonaca to sync their data (see Realignment note).

### 3.2 Smartphone Data (Supportive Only)

Used only for behaviour & context: smartphone usage time (daily total), outside time (derived), last active location (context), steps (fallback only).

## 4. Metric Taxonomy

- **Vitals** (wearable only): Heart Rate, HRV, Stress, SpO₂, Respiration, Sleep, Body Temperature, Blood Pressure, ECG (if supported), Blood Glucose (if supported), VO₂ Max.
- **Activity**: Steps, Calories, Workouts, Training load.
- **Behaviour** (derived): Routine Consistency, Smartphone usage time, Outside time, Last active location (context only).

## 5. Derived Logic & Baselines

- Rolling baseline over **14–21 valid days**; a day is excluded if the wearable wasn't worn, sync gaps exceed a threshold, or phone data is missing. Baseline recalculated daily.
- **Comparison language is relative only**: Higher than usual / Same as usual / Lower than usual. No colours, no medical thresholds. (Matches the existing `MetricTrendLabel` union already in `src/types/index.ts` — that part of the domain model already aligns.)
- **Outside Time**: identify a home cluster, measure time outside it, compare with baseline, output a relative state only.
- **Routine Consistency Score**: normalizes smartphone usage, outside time, steps, and sleep (if connected) against baseline; behaviour weighted higher than vitals; outputs a stability band — Stable / Slightly different / Noticeably different. Explainable, deterministic, no ML.

## 6. Business Model (Paid-Only)

**No perpetual free tier in Phase-1.** Rationale: core value requires a wearable; phone-only users misjudge the product; Spike + infra costs are real; target users already pay for wearables. Freemium is deferred to a future funded phase, used as top-of-funnel only.

### 6.1 Trial

- **7 days**, full product access, **payment method required upfront** (not 5 days as earlier research-driven drafts assumed).
- Supported payments: Credit/Debit Cards (global), UPI (India). No PayPal/Apple Pay/Amex-Mastercard-specific handling, no platform/region-split billing rail (StoreKit/Play Billing/Razorpay UCB) — materially simpler than `TECHNICAL_REQUIREMENTS.md` §5 currently describes.
- UX copy: *"Try the full experience free for 7 days. No charge during trial. Cancel anytime."*

### 6.2 Trial Expiry

- Continued → seamless conversion.
- Cancelled/lapsed → wearable sync paused, data becomes read-only, copy: *"Health tracking is paused. Restart subscription to continue."* No deletion, no surprise charges.

## 7. Pricing

- **₹249/month**: 1 wearable, full insights, **2 family member sharing** (i.e. up to 2 Secondary Members per Primary). Monthly recurring.

## 8. Consent & Payment Model (Final)

**Consent always comes before payment.** Payment only unlocks already-approved capability — there is no post-payment approval anywhere.

## 9. UX Flows — Payment & Onboarding

**Flow A — Add Yourself (Self-Pay)**: Profile → Add Device → "Connect your wearable — ₹249/month, Try free for 7 days" → Start trial → Connect device → Sync begins.

**Flow B — Add Family Member's Device (Delegated Pay)**: Primary (parent) sees "To connect a wearable, a trusted family member needs to enable this" with a CTA to ask the family member (implicit consent). Secondary (adult child) sees "[Parent's name] wants to connect a wearable," starts the trial → subscription active, then the Primary connects the device.

**Flow C — Add Additional Family Member (up to 2 Secondary Members only)**: Primary invites → Secondary accepts. The invite itself is the consent.

This is a meaningfully different onboarding shape than the existing Complete Profile → Connect Wearable → (later) Invite sequence built so far — payment is now interleaved into onboarding, not a separate later subscription milestone, and there's a hard cap of 2 Secondary Members.

## 10. Core App UX

**Home (Members Overview)**: self first, others only if shared, metric deviation counts, last updated time.

**Member Detail**: daily NLP summary, last active location, metrics grouped & ordered by deviation.

**Metric Detail**: **24h / 7d / 30d** ranges (not the 1D/7D/4W/1Y ranges currently built), graph + explanation, non-medical guidance.

## 11. Permissions Model (Final)

### 11.1 Default Permission

**✅ All access is enabled by default.** Rationale: most families want full visibility, this reduces setup friction, and it matches the emotional intent of sharing. This replaces the narrower default-grant rules implemented so far (which only auto-granted Secondary→Primary by default and required explicit Edit Permissions action for anything wider).

### 11.2 Permission Categories

| Category | Access |
|---|---|
| All | Everything |
| Vitals | Health metrics |
| Activity | Steps, workouts |
| Behaviour | Routine, phone usage |

Multi-select supported. **There is no separate "Location" category** — location is context within Behaviour, not its own toggle (the current `SharingScope` type has a 4th `'location'` value that this PDF doesn't have).

### 11.3 Permission Modification UX

A "Manage access for [Name]" screen with checkboxes: All / Vitals / Activity / Behaviour, with individual metrics listed under their category. **Changes apply instantly** — there is no Save step (the Edit Permissions screen built so far uses a batched Save button, which doesn't match this).

## 12. UX Edge Cases

- **Primary revokes access**: Secondary loses access instantly, no payment refund, slot freed.
- **Subscription lapses**: wearable sync pauses, sharing pauses, old data remains visible read-only.
- **Device disconnected**: show "No recent data," never show inferred values.
- **Permission reduced**: Secondary sees "Some data is no longer shared," no prompts, no upsell.
- **Multiple Secondary Members**: each evaluated independently; permissions are per-Secondary.

## 13. GTM Rationale

Paid-first works because wearable owners already pay, family health is high-intent, paid users give better feedback, there's less noise/better PMF signal, and it allows controlled burn during bootstrap. Freemium is introduced post-funding as top-of-funnel only.

## 14. Non-Goals (Phase-1)

Medical diagnosis, emergency alerts, ML predictions, continuous GPS, doctor marketplace, fear-based nudges.

## 15. One-Line Product Summary

A wearable-first family health awareness platform that helps people understand daily health and routine changes of loved ones — safely, clearly, and without medicalisation.

---

## Realignment note (not part of the PDF — tracking doc/code drift)

This PDF is now authoritative and supersedes conflicting content in `CLAUDE.md`, `docs/TECHNICAL_REQUIREMENTS.md`, `docs/MARKET_RESEARCH.md`, the Figma file's role labeling, and the Members & Sharing backend/frontend already built. None of those have been updated yet. Known conflicts to resolve:

- **Role naming is inverted everywhere else.** Figma screens ("Profile - Primary Member" / "Profile - Secondary Member"), `backend/.../members/MemberRole.java`, `src/types/index.ts`'s `MemberRole`, and every screen built so far treat Primary = adult child/payer, Secondary = parent. This PDF treats Primary = parent/data owner, Secondary = adult child/payer. No Tertiary role exists in this PDF at all.
- **2-Secondary-Member cap** isn't enforced in `InviteService`.
- **Trial is 7 days**, not the 5 days in `MembersService.TRIAL_DAYS` / `MARKET_RESEARCH.md`'s 5+-day conversion research.
- **Permissions default to all-on, applied instantly, 3 categories (All/Vitals/Activity/Behaviour)** — the built `EditPermissionsScreen` has narrower role-dependent defaults, a 4th "Location" scope, and a batched Save button.
- **Wearable data source is Spike API** (cloud, cross-vendor), not Apple HealthKit/Google Health Connect — this changes the on-device-only NFR and the `src/lib/health/HealthProvider.ts` stub design CLAUDE.md currently documents.
- **Payment is interleaved into onboarding** (Flow A/B), not a separate later subscription milestone, and trial start requires a payment method upfront — not yet built anywhere.
- **Metric Detail ranges are 24h/7d/30d**, not 1D/7D/4W/1Y.
- Payment methods simplify to cards + UPI — `TECHNICAL_REQUIREMENTS.md`'s StoreKit/Play Billing/Razorpay-UCB/RevenueCat platform-split plan is likely overbuilt against this PDF, pending confirmation.
