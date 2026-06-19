# Select Wearable Account (Subscription flow) — Verification

**Figma reference:** nodes 197:10387 (initial), 197:11178 (mid-flow), 225:3615 (retry — confirmed NOT 222:1723, see note)
**Implementation:** src/features/subscription/SelectWearableAccountScreen.tsx
**App screenshots:** /tmp/bonaca-audit/14-select-wearable-initial.png, 15-select-wearable-midflow.png, 16-select-wearable-retry.png

## Verdict
Pass with minor issues — layout, colors, and the real brand logos are all pixel-exact across all 3 variants; one font-size mismatch found on the retry toast.

## Initial variant — pixel-level discrepancies
None found — exact match to `197:10387`, including the correctly-absent back chevron.

## Mid-flow variant — pixel-level discrepancies
None found — exact match to `197:11178`, including the back chevron, identical row content/logos.

## Retry variant — pixel-level discrepancies
| # | Element | Figma value | Implementation value | Severity | Notes |
|---|---|---|---|---|---|
| 1 | Toast message font size | 16 (Figma `style.fontSize` on "Failed to connect, please try again") | `fontSize: 14` (`ConnectionErrorToast.tsx:37`) | low | Confirmed via JSON; everything else on the toast (background `#f6e7e7`, border/text/icon `#d63d3d`, fontWeight 500, the `IconDeviceWatchX` icon matching Figma's literal layer name `tabler-icon-device-watch-x`) is an exact match. |
| 2 | "I'll Connect Later" link below the toast | Present in the Figma node tree (`characters: "I'll Connect Later"`) but not visible in the rendered Figma screenshot — likely clipped/overflowed in the source file's retry-state layout | Visible and rendered below the toast in the implementation | none (informational) | Not flagging this as an implementation bug — the implementation showing a working, visible "I'll Connect Later" link is arguably more correct/usable than whatever layout issue caused Figma's own rendered export to cut it off. |

## Brand logo verification
All 4 logos (Fitbit, Garmin, Samsung Health, Oura) render as the real cropped brand assets from `assets/images/wearables/`, confirmed visually identical to Figma's logos across all 3 variants — consistent with the fix made earlier this session.

## Code quality / responsiveness issues
| # | Issue | File:line | Severity | Notes |
|---|---|---|---|---|
| 1 | `variant` is read from a URL query param (`?variant=`) with a `mid-flow` fallback for anything invalid | select-wearable-account.tsx | none (informational) | Reasonable routing approach; confirmed the 3 valid values match exactly what the Figma cache has real frames for. |
| 2 | Stray "Profile Settings" text layer found in the Figma source for this screen too (same pattern as onboarding's Connect Wearable) | N/A (Figma authoring artifact) | none | Not an implementation gap — confirmed via JSON text dump, consistent with the same kind of leftover/duplicated component layer seen elsewhere in this Figma file. |

## Missing from implementation (in Figma, not built)
- None found.

## Extra in implementation (not in Figma)
- None found.

## Confirmed correct
- Header: "Connect Your Wearable", correct back-chevron presence/absence per variant (absent on `initial`, present on `mid-flow`/`retry`) — exact match.
- Intro copy: "Link a wearable account to track health and activity" — exact match (Figma's forced line-break after "health" isn't reproduced, same low-severity stylistic note as the onboarding Connect Wearable screen, not re-flagged here).
- All 4 provider rows: real brand logos, label color `#4e535a`, fontSize 14/weight 500 — exact match.
- "I'll Connect Later" link: color `#575fb4` (`Colors.accent`), fontSize/weight — exact match.
- "Continue" CTA correctly absent in all 3 variants (Figma has it `visible: false`) — confirmed still correct.
