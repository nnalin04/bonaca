# Bonaca — Full Screen Verification Summary

Pixel-level + code-quality audit of all 12 built screens against the real Figma design (`.design-reference/`), done screen-by-screen with exact hex/fontSize/fontWeight cross-checks from the Figma JSON, not just visual eyeballing. Individual reports: `docs/VERIFICATION/01-splash.md` through `12-payment-gateway.md`.

**This is a findings document only — nothing has been fixed yet.** Per the review process, this should be read and confirmed/reprioritized before any code changes are made.

## How this was produced
12 screens were audited. 2 (Splash, Login) were done by parallel subagents before a session usage limit cut the rest off; the remaining 10 were done directly, same methodology: live simulator screenshot + Figma reference screenshot + raw Figma JSON (exact `r/g/b` fills converted to hex, `fontSize`/`fontWeight`/`absoluteBoundingBox`) cross-referenced against the actual React Native style values in source.

## Verdicts at a glance

| # | Screen | Verdict | High | Medium | Low |
|---|---|---|---|---|---|
| 1 | Splash | Pass with minor issues | 0 | 2 | 3 |
| 2 | Login | Pass with minor issues | 0 | 1 | 5 |
| 3 | OTP (all states) | Pass with minor issues | 0 | 2 | 3 |
| 4 | Complete Profile | **Needs fixes** | **1 (functional dead-end)** | 0 | 4 |
| 5 | Connect Wearable (onboarding) | Pass with minor issues | 0 | 0 | 4 (+ 1 product/design conflict) |
| 6 | Home | **Pass — no issues** | 0 | 0 | 4 |
| 7 | Notifications | Pass with minor issues | 0 | 1 | 2 |
| 8 | Profile (Primary + Secondary) | **Needs fixes** | 0 | 6 | 3 |
| 9 | Member Details (3 tabs) | **Needs fixes** | **2 (systematic)** | 5 | 0 |
| 10 | Metric Details | **Needs fixes** | 0 | 3 | 2 |
| 11 | Select Wearable Account (3 variants) | Pass with minor issues | 0 | 1 | 0 |
| 12 | Payment Gateway | **Pass — no issues** | 0 | 0 | 3 |

## 🔴 Critical — fix before anything else

**1. Onboarding is unfinishable.** (`04-complete-profile.md`) Complete Profile's "Continue" button can never enable — Gender and DOB fields are `editable={false}` with empty no-op `onPress` handlers (no picker exists), and `isComplete` requires both to be truthy. Every fresh user hits a permanent dead end on the only path through onboarding. Needs either a real picker, a relaxed completion check, or a skip path before this is demoed end-to-end.

## 🟠 High — systematic bugs, each a one-place fix that corrects many screens at once

**2. Settings-row icons are the wrong color, system-wide.** (`08-profile.md`) Every icon in Profile's settings list (`SettingsListItem.tsx`) is colored gray (`labelColor`) when Figma specifies accent purple (`#575fb4`) for every row's icon, with only the text staying gray. One-line fix, corrects all 7 rows.

**3. Metric card icons are the wrong color, system-wide.** (`09-member-details.md`) Every metric card icon across Vitals/Activity/Behaviour (`MetricCard.tsx`) is hardcoded to `Colors.accent` purple on a blue-tinted background, when Figma gives each metric its own distinct icon color (Heart Rate coral, HRV teal, SpO2 blue, Workouts gold, etc.) on a neutral gray `#f5f5f5` background. Real hex values for 9 metrics are documented in `09-member-details.md`'s table. Root-cause fix: add a per-metric `iconColor` to `metricDisplayConfig` (`metricDisplay.ts`) and have `MetricCard` use it instead of a hardcoded constant — fixes all 18 cards (10 Vitals + 4 Activity + 4 Behaviour) at once.

## 🟡 Medium — real, worth fixing soon

- **Member Details header 3-dot menu is missing on all 3 tabs** — previously assumed out of scope, but newly-discovered Figma reference frames for Activity/Behaviour both clearly show it. (`09-member-details.md`)
- **Member Details: Heart Rate/HRV sparkline charts have no x-axis labels** (6AM/12PM/6PM/12AM, 1W/2W/3W/4W) — `MiniSparkline.tsx` has no axis-label support at all. (`09-member-details.md`)
- **Member Details: Training Load's caption text is wrong** — Figma wants the custom string "Within optimal range"; the 3-value generic trend enum can't represent it, so it falls back to "Same as usual". Needs a structural type extension (custom caption override), not just a copy fix. (`09-member-details.md`)
- **Metric Details bar chart is missing 3 details**, now precisely identified: background tint (`Colors.chartAreaFill` — token already exists, just unused), 2 dashed vertical gridlines at 6AM/12PM, and fixed-scale Y-axis labels ("180 bpm"/"75 bpm" vs. the computed data range currently shown). (`10-metric-details.md`)
- **Profile's wearable-connect card has 3 real color/size mismatches**: card background should be neutral `#101010` not navy `#090c2c`; label should be white 16px not muted 14px; "Connect" button should be `#555ec2`/weight 600 not `Colors.accent`/weight 500. (`08-profile.md`)
- **Notification rows aren't tappable** despite copy implying actions ("Tap to pay") — `Notification.deepLinkTarget` exists in the domain type but is never read. (`07-notifications.md`)
- **OTP: the phone number in "OTP sent to 9742657712" is bold+gray when Figma wants Medium-weight+dark**, and the 4 digit boxes are 12px apart when Figma wants ~28px. (`03-otp.md`)

## 🟢 Low / cosmetic — worth a cleanup pass, not urgent

- Splash: gradient angle slightly off Figma's spec; missing light-content status bar override. (`01-splash.md`)
- Several screens reuse one ~2-unit-of-255 "drift" border/placeholder color (`#e3e4e6` vs Figma's `#e3e5e5`/`#e5e4e4`) — same systemic rounding across Login/OTP/Complete Profile, never exact but never wrong enough to notice. (`02-login.md`, `03-otp.md`, `04-complete-profile.md`)
- "Skip For Now" should be "Skip for Now" (casing) on the onboarding Connect Wearable screen. (`05-connect-wearable.md`)
- A handful of fixed-pixel hero/header heights throughout the auth flow won't scale on much shorter/taller devices — consistent pattern across Splash/Login/OTP/Home, flagged individually in each report, not separately re-listed here.
- Various missing `accessibilityLabel`s on compound controls (OTP digit boxes, country-code field, notification rows).

## ⚠ Not bugs — product/process flags worth separate attention

- **Connect Wearable (onboarding) lists Fitbit/Garmin/Samsung Health/Oura — all explicitly Phase 2 per CLAUDE.md** (Phase 1 is Apple HealthKit + Google Health Connect only). The implementation is pixel-faithful to Figma; the Figma file and the documented roadmap disagree. (`05-connect-wearable.md`)
- **A separate, not-yet-built "Subscriptions" list screen** (4 states: active/empty/cancelled/expired, per-member renewal cards) exists in the Figma file's cache but has no implementation or task tracking it yet. Confirmed independently this is genuinely a different screen, not part of Profile. (`08-profile.md`)
- **Metric Details has 2 different Figma chart treatments** (bar chart vs. smooth line-with-dots) for the same data — the implementation picked the bar style, a legitimate but worth-confirming choice. (`10-metric-details.md`)
- **Metric Details' 7D/4W/1Y range tabs have no real Figma reference at all** — only 1D was ever designed; the other 3 ranges are honest placeholders reusing 1D's data under different axis labels. (`10-metric-details.md`)

## What's genuinely excellent
Home and Payment Gateway came back with zero real issues. Splash, Login, OTP, Connect Wearable (onboarding), and Select Wearable Account are all "pass with minor issues" — meaning the auth/onboarding flow and the wearable-connection flow are both in very good shape pixel-wise, modulo the Complete Profile dead-end. Every screen's text content, copy, and overall layout structure matched Figma; nearly all issues found are color/size precision or missing secondary affordances, not structural rebuilds.

## Suggested fix order
1. Complete Profile dead-end (blocks demoing the whole onboarding flow).
2. The 2 systematic icon-color bugs (Profile settings rows, Member Details metric cards) — high-value, low-effort, single-file fixes each.
3. Member Details 3-dot menu + Metric Details chart gaps (now precisely specified, ready to implement).
4. Everything else in the medium/low lists, roughly in the order listed above.
5. Raise the Phase 2 vendor / Subscriptions-screen flags with whoever owns the Figma file and roadmap — these need a product decision, not just code.
