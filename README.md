# 👨‍👩‍👧 Bonaca

A mobile app letting adult children remotely monitor an aging parent's health and activity through the wearable device their parent already wears — no extra hardware, no daily check-in calls required.

---

## 🚀 Overview

Adult children increasingly live far from their aging parents and have no easy way to know how they're really doing day to day. **Bonaca** closes that gap by reading data from a parent's existing wearable (Apple Watch, Fitbit, Garmin, Samsung Galaxy Watch, fitness band, or ring) and surfacing it to the child in a single dashboard: vitals, activity, daily routine, screen time, time outdoors, and last known location.

Full product spec: [`docs/PRD.md`](docs/PRD.md).

### 🎯 Key Objectives

- **Passive monitoring**: parent just wears their device — no daily logging required.
- **Low-friction onboarding for the parent**: phone number + OTP only, no passwords.
- **Family sharing with permissions**: invite other relatives with scoped, revocable visibility.
- **Actionable insights, not raw data dumps**: trend and anomaly insights surfaced via Notifications.

---

## ✨ Features

- **🔐 OTP-only authentication** for the parent (Secondary Member); fuller profile for the subscribing child (Primary Member).
- **⌚ Wearable connection**: Apple HealthKit and Google Health Connect today; direct Fitbit/Garmin OAuth planned for Phase 2.
- **🏠 Home dashboard**: own metrics plus a "Shared with you" list of connected family members.
- **📊 Member Details**: Vitals / Activity / Behaviour tabs — routine adherence, screen time, outdoor time, last active location.
- **📈 Metric Details**: 1D/7D/4W/1Y trend charts with auto-generated insight text.
- **🔔 Notifications**: anomaly and trend alerts that deep-link straight to the relevant metric.
- **💳 Subscription**: account-level billing (UPI, PayPal, Amex, Mastercard, Apple Pay) covering the whole family.
- **🛡️ Permission controls**: Edit Permissions, Pin/Unpin, Hidden Members.

---

## 🛠 Tech Stack

- **Framework**: Expo + React Native + TypeScript
- **Navigation**: Expo Router (file-based, typed routes)
- **Design source**: Figma — see [`CLAUDE.md`](CLAUDE.md) for the file key and node-ID map

No backend, auth provider, database, or payment processor is wired up yet — see the "Not Set Up Yet" list in [`CLAUDE.md`](CLAUDE.md).

## Get started

```bash
npm install
npx expo start
```

## Learn more

- [Expo documentation](https://docs.expo.dev/)
- [Expo Router](https://docs.expo.dev/router/introduction/)
