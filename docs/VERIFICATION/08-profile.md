# Profile & Settings — Verification

**Figma reference:** nodes 39:2025 (Primary), 197:5921 (Secondary)
**Implementation:** src/features/profile/ProfileScreen.tsx
**App screenshots:** /tmp/bonaca-audit/08-profile-primary.png, /tmp/bonaca-audit/09-profile-secondary.png

## Verdict
Needs fixes — found a systematic icon-color bug (every settings-row icon should be accent purple, not gray) and several real color/size mismatches on the wearable-connect card, on top of confirming two earlier fixes (Log Out color, Primary/Secondary row parity) are now correct.

## 🔴 Systematic bug: all settings-row icons use the wrong color
`SettingsListItem.tsx:33` colors the leading icon with `labelColor` (= `Colors.textSecondary`, gray `#4e535a`, after the earlier Log Out fix). But checking the Figma JSON for every row's icon vector (`tabler-icon-list-check`, `tabler-icon-cash`, `tabler-icon-logout`, `tabler-icon-file-description`, `tabler-icon-file-text-shield`, etc.) — **every single icon's stroke color is `rgb(0.341,0.373,0.706)` = `#575fb4` = `Colors.accent`**, while only the *text label* is gray. The implementation currently makes icon and text the same gray color for all 7 rows; per Figma, icons should be purple/accent and only the text should be gray. This affects every row equally (not just Log Out) — a one-line fix (`<Icon size={24} color={Colors.accent} ... />` instead of reusing `labelColor`) would resolve all 7 at once.

## Pixel-level discrepancies — WearableConnectCard ("Connect your wearable account")
| # | Element | Figma value | Implementation value | Severity | Notes |
|---|---|---|---|---|---|
| 1 | Card background | `#101010` (Figma `Card` frame fill `rgb(0.0627,0.0627,0.0627)`, a neutral near-black) | `Colors.headerGradientStart` = `#090c2c` (navy-tinted black) | medium | Visually subtle but a real, measurable mismatch — Figma's card is neutral gray-black, the implementation reuses the header's navy-purple-tinted dark color instead of a dedicated token. |
| 2 | Label text color | `#ffffff` (pure white) | `Colors.textOnDark` = `#e1e7ef` (light gray-blue) | medium | Confirmed via Figma JSON fill on the "Connect your wearable account" text node — it's solid white, not the muted `textOnDark` tone used for Home's header subtitle. |
| 3 | Label font size | 16 | 14 (`WearableConnectCard.tsx:51`) | medium | Confirmed via Figma `style.fontSize` on the same text node. |
| 4 | "Connect" button background | `#555ec2` (Figma `Button` frame fill `rgb(0.333,0.369,0.761)` — this is exactly `Colors.headerGradientEnd`) | `Colors.accent` = `#575fb4` | low | Close but measurably different (Figma's button reuses the header-gradient-end color, not the standalone accent token); ~14-unit difference in the blue channel. |
| 5 | "Connect" button text weight | 600 | `fontWeight: '500'` (`WearableConnectCard.tsx:66`) | low | Confirmed via Figma `style.fontWeight` on the "Connect" text node. |
| 6 | Icon-wrap circle background | `#1c1d2b`-ish (Figma `Frame 2121454029` fill `rgb(0.111,0.116,0.168)`, a solid navy-tinted dark color) | `rgba(255,255,255,0.12)` (white at 12% opacity over the card background) | low | Different approach entirely — Figma uses a flat dark navy fill, not a translucent white overlay. Low severity since the resulting visual contrast against the card background is similar, but technically not the same color/technique. |

## Primary vs Secondary row-structure comparison
Confirmed independently (not just trusting the earlier fix): both Figma frames list the exact same 7 rows in the exact same order — Members & Permissions, Manage Subscription, Hidden Members, Documentation, Terms & Conditions, Privacy Policy, Log Out — all at the same `#4e535a` text color with no styling differences between Primary (`39:2025`) and Secondary (`197:5921`). (Note: Figma's row list technically contains "Hidden Members" as two perfectly-overlapping duplicate text layers at the identical bounding box — this is Figma source-file authoring redundancy, e.g. an accidental layer duplication, not a second visible row; only 7 distinct rows actually render.) The earlier fix removing the `isPrimary`-conditional row-hiding was correct and is confirmed still in place in `ProfileScreen.tsx`.

## subscriptions__*/-empty/-cancelled/-expired — what are these actually for?
Independently confirmed: these 4 cached frames (`subscriptions__197-6270`, `-empty__197-7272`, `-cancelled__197-6835`, `-expired__197-7049`) live in the same "Connecting a Wearable" Figma section as Profile, but their content (per-member renewal/expiry cards for "Dad"/"Mom"/"Brother" etc.) doesn't match Profile & Settings' structure at all — they're states of a separate, not-yet-built "Subscriptions" list screen reachable from the "Manage Subscription" row, not states of the Profile screen itself. The "Manage Subscription" row's Figma styling is identical in both Primary and Secondary frames regardless of any subscription state, confirming no state-driven appearance change belongs on Profile itself. Building that separate Subscriptions list screen (with its 4 sub-states) is out of scope for this screen and isn't tracked anywhere yet — worth a follow-up task if it's in the roadmap.

## Code quality / responsiveness issues
| # | Issue | File:line | Severity | Notes |
|---|---|---|---|---|
| 1 | All `SettingsListItem` `onPress` handlers are empty no-ops except Manage Subscription and Log Out | ProfileScreen.tsx | none (informational) | Expected — no backend/screens exist yet for Members & Permissions, Hidden Members, Documentation, Terms & Conditions, Privacy Policy. |
| 2 | `WearableConnectCard`/`WearableConnectedCard` toggle is driven by a hardcoded `wearableConnection = null` constant, not real state | ProfileScreen.tsx | none (informational) | Reasonable per CLAUDE.md — `src/lib/health/*` are stubs by design, nothing to wire up yet. |
| 3 | Fixed card heights (80px profile summary, 80px wearable card, 56px rows) throughout | various component files | low | Consistent with the rest of the app's fixed-pixel-at-reference-size pattern already flagged on other screens — not new, just noting it's systemic. |

## Missing from implementation (in Figma, not built)
- None structurally — all 7 rows, the profile summary card, and the wearable-connect card are present for both Primary and Secondary.

## Extra in implementation (not in Figma)
- None found.

## Confirmed correct
- Header: "Profile & Settings", fontSize 18, fontWeight 500, white — exact match for both variants.
- Profile summary card: name fontSize 16/weight 500/color `#1f2d2b`, phone fontSize 12/weight 400/color `#4e535a` — exact match for both "Prasanna Kumar" (Primary) and "Rakesh P Kumar" (Secondary).
- Settings row **text** color: `#4e535a` (`Colors.textSecondary`) for all 7 rows including Log Out — confirmed the earlier fix (removing the red `destructive` treatment) is correct and still in place.
- Row chevron color (`Colors.textSecondary`) — matches Figma's chevron treatment.
- 7-row list order and content identical between Primary and Secondary — confirmed independently above.
