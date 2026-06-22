# 👨‍👩‍👧 Bonaca

A family health & routine awareness app — consent-first visibility into an elderly parent's daily wellbeing for their adult children, using the wearable device the parent already wears. No extra hardware, no daily check-in calls required.

---

## 🚀 Overview

Families want assurance and early awareness, not raw health data or medical alarms. **Bonaca** closes the distance gap by reading data from a parent's existing wearable (Garmin, Fitbit, Samsung, Oura, and others via the Spike API) and surfacing it to family members who've been explicitly granted access: vitals, activity, daily routine, screen time, time outdoors, and last known location — described in relative, non-medical terms ("higher than usual," not a clinical reading).

Full product spec: [`docs/PRD.md`](docs/PRD.md) (mirrors [`docs/PRD.pdf`](docs/PRD.pdf), the authoritative spec).

### 🎯 Key Objectives

- **Passive monitoring**: the Primary Member (the parent/data-owner) just wears their device — no daily logging required.
- **Low-friction onboarding**: phone number + OTP only, no passwords.
- **Family sharing with permissions**: a Primary can share with up to 2 Secondary Members, with revocable, instantly-applied visibility.
- **Actionable insights, not raw data dumps**: trend insights surfaced via Notifications, deterministic and explainable — no ML, no diagnosis.

---

## ✨ Features

- **🔐 OTP-only authentication** for both roles. **Primary Member** = the person whose data is tracked (typically the elderly parent, owns the data, controls sharing). **Secondary Member** = the family member who views it (typically the adult child, may be the payer) — up to 2 per account, no third role.
- **⌚ Wearable connection**: via the **Spike API** (cross-vendor cloud sync — Garmin, Fitbit, Samsung, Oura, etc.), not on-device HealthKit/Health Connect.
- **🏠 Home dashboard**: own metrics plus a list of family members shared with you.
- **📊 Member Details**: Vitals / Activity / Behaviour tabs — routine adherence, screen time, outdoor time, last active location.
- **📈 Metric Details**: 24h/7d/30d trend charts with auto-generated, non-medical insight text.
- **🔔 Notifications**: trend alerts that deep-link straight to the relevant metric.
- **💳 Subscription**: flat ₹249/month, cards (global) + UPI, 7-day free trial requiring a payment method upfront.
- **🛡️ Permission controls**: all access on by default, instant-apply Edit Permissions (Vitals/Activity/Behaviour/All), Pin/Unpin, Hidden Members.

---

## 🛠 Tech Stack

- **Mobile**: Expo + React Native + TypeScript, Expo Router (file-based, typed routes)
- **Backend**: custom Java/Spring Boot (`backend/`) — auth (phone+OTP, JWT) and members/sharing are built; see `docs/TECHNICAL/BACKEND_COMPARISON_AND_MIGRATION.md` for why custom over Supabase
- **Design source**: Figma — see [`CLAUDE.md`](CLAUDE.md) for the file key and node-ID map. **Figma still uses the old, inverted Primary/Secondary labeling** — not yet relabeled to match the spec above.

Auth and Members & Sharing backend/frontend are built, but still use the **old** role naming (`primary`/`secondary`/`tertiary`) and an older permissions model — not yet realigned to the spec above. Wearable sync, payments, push notifications, and insights are not yet built. See the "Not Set Up Yet" list in [`CLAUDE.md`](CLAUDE.md).

## Get started

```bash
npm install
npx expo start
```

## Learn more

- [Expo documentation](https://docs.expo.dev/)
- [Expo Router](https://docs.expo.dev/router/introduction/)
