# Profile & Settings — Verification

**Figma reference:** nodes 39:2025 (Primary), 197:5921 (Secondary)
**Implementation:** src/features/profile/ProfileScreen.tsx
**App screenshots:** /tmp/bonaca-audit/08-profile-primary.png, /tmp/bonaca-audit/09-profile-secondary.png

## Verdict
✅ **FIXED** — all findings below resolved and re-verified visually against the Figma reference.

## ✅ FIX APPLIED: settings-row icons now use accent purple
`SettingsListItem.tsx` now colors the leading icon with `Colors.accent` (`#575fb4`) instead of reusing the gray text color, for all 7 rows. Also caught and fixed an additional issue found while fixing this (not in the original audit pass): the row label `fontWeight` was `'400'`, but Figma specifies `500` — corrected alongside the color fix.

## ✅ FIX APPLIED: WearableConnectCard now matches Figma exactly
Added two new tokens (`Colors.wearableCardBackground = '#101010'`, `Colors.wearableCardIconBackground = '#1c1d2b'`) and updated `WearableConnectCard.tsx`:

| # | Element | Figma value | Was | Now | Status |
|---|---|---|---|---|---|
| 1 | Card background | `#101010` | `Colors.headerGradientStart` (`#090c2c`) | `Colors.wearableCardBackground` | fixed |
| 2 | Label text color | `#ffffff` | `Colors.textOnDark` (`#e1e7ef`) | `Colors.white` | fixed |
| 3 | Label font size | 16 | 14 | 16 | fixed |
| 4 | "Connect" button background | `#555ec2` | `Colors.accent` (`#575fb4`) | `Colors.headerGradientEnd` (`#555ec2`) | fixed |
| 5 | "Connect" button text weight | 600 | 500 | 600 | fixed |
| 6 | Icon-wrap circle background | `#1c1d2b` | `rgba(255,255,255,0.12)` | `Colors.wearableCardIconBackground` | fixed |

Re-verified in the simulator: re-screenshotted Profile Primary and compared side-by-side against `.design-reference/screens/profile-settings__39-2025.png` — icons, card background, label, and button now all match.

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
