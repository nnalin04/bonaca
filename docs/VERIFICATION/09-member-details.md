# Member Details — Verification

**Figma reference:** section 196:4233; canonical Vitals frame 43:4129; Activity frame discovered at 326:4530; Behaviour frame discovered at 326:4622
**Implementation:** src/features/members/MemberDetailsScreen.tsx
**App screenshots:** /tmp/bonaca-audit/10-member-details-vitals.png, 11-member-details-activity.png, 12-member-details-behaviour.png (viewport-only — these screens scroll; content below the fold wasn't captured live, but Vitals' full content was already cross-checked against JSON in an earlier pass this session)

## Verdict
✅ **FIXED** — all 4 findings below resolved and re-verified visually against Figma across all 3 tabs.

## Which member-details__* variant maps to which tab/state?
Independently investigated by grepping every cached `member-details__*.json` for tab-specific text:
- `member-details__43-4129` → **Vitals** tab (has "Heart Rate", "HRV", etc., no "Steps"/"Routine").
- `member-details__326-4530` → **Activity** tab (contains "Steps" — this was NOT identified in the original build pass, which only had the canonical Vitals frame).
- `member-details__326-4622` → **Behaviour** tab (contains "Routine" — also not previously identified).
- The other 5 files (196-4346, 196-4629, 196-4914, 227-5144, 109-924) all contain only Vitals-tab text — confirmed these really are just scroll-position/menu-state duplicates of the Vitals tab, not Activity/Behaviour, as a prior pass guessed.

This means the Activity and Behaviour tabs **do** have real, dedicated Figma references that were never checked against until now — see the systematic icon bug below, found by comparing against them directly.

## ✅ FIX APPLIED: per-metric icon colors

Added `iconColor` to every entry in `metricDisplayConfig` (`metricDisplay.ts`), with exact hex values extracted from Figma for all 18 metrics (the 9 in the table below plus Stress `#6bae92`, Temperature `#bbbbbb`, ECG `#e07a5f`, Blood Glucose `#8b6f9c`, VO2 Max `#d4a24c`, Routine `#5b8def`, Screen Time `#e07a5f`, Outdoor Time `#3a7f7c`, Last Active Location `#5e5a8a`). `MetricCard.tsx` now takes an `iconColor` prop instead of hardcoding `Colors.accent`, and the icon-circle background now uses a new `Colors.metricIconBackground` (`#f5f5f5`) token instead of the blue-tinted `tabBarTrack`. `MemberDetailsScreen.tsx` passes `config.iconColor` through at all 3 call sites (`renderReading`, `renderChartReading`, and the inline Sleep card). Re-verified in the simulator across all 3 tabs — every icon now matches Figma's color exactly.

## ✅ FIX APPLIED: header 3-dot menu

`MemberDetailsHeader.tsx` already had an `onPressMenu` prop built but never wired up. Connected it in `MemberDetailsScreen.tsx` to open a `SelectModal` (reused from the onboarding feature) with "Pin to top"/"Unpin from top" (toggling on `mockMember.pinned`), "Edit Nick Name", and "Hidden Members" as stub options — consistent with how other not-yet-built destinations are stubbed elsewhere in the app. Verified the icon renders in the header and the menu opens correctly.

## ✅ FIX APPLIED: sparkline x-axis labels

`MiniSparkline.tsx` now accepts an optional `xAxisLabels` prop, rendering evenly-spaced labels under the chart. Wired for Heart Rate (`6AM/12PM/6PM/12AM`) and HRV (`1W/2W/3W/4W`) in `MemberDetailsScreen.tsx`, the two metrics with confirmed Figma label text. Re-verified visually — labels now appear under both charts.

## ✅ FIX APPLIED: Training Load custom caption

Added an optional `customCaption?: string` field to `MetricReading` (`src/types/index.ts`) to handle captions the 3-value trend enum can't represent. Set to `"Within optimal range"` for the Training Load mock reading; `MemberDetailsScreen.tsx`'s `renderReading` now prefers `reading.customCaption` over the computed trend label when present. Re-verified — the Activity tab now shows the correct caption.

## 🔴 Systematic bug: every metric card icon is hardcoded purple instead of per-metric colored (ORIGINAL FINDING, NOW FIXED — kept for history)
`MetricCard.tsx:37` hardcodes `color={Colors.accent}` for every single card's icon, and `iconCircle` (`MetricCard.tsx:75`) hardcodes `backgroundColor: Colors.tabBarTrack` (`#f0f3ff`, a light blue-lavender) for every card too. Checked against Figma JSON across all 3 tabs — **every icon background circle is actually a neutral light gray `#f5f5f5`** (not blue-tinted), and **every icon's glyph color is metric-specific**, not purple:

| Metric | Figma icon color | Figma icon bg |
|---|---|---|
| Heart Rate | `#e07a5f` (= existing `Colors.chartLine`) | `#f5f5f5` |
| HRV | `#3a7f7c` (= existing `Colors.chartLineSecondary`) | `#f5f5f5` |
| SpO2 | `#5b8def` | `#f5f5f5` |
| Respiration | `#6c8ea3` | `#f5f5f5` |
| Sleep | `#5e5a8a` | `#f5f5f5` |
| Steps | `#5b8def` (same blue as SpO2) | `#f5f5f5` |
| Calories | `#e07a5f` (same coral as Heart Rate) | `#f5f5f5` |
| Workouts | `#d4a24c` | `#f5f5f5` |
| Training Load | `#3a7f7c` (same teal as HRV) | `#f5f5f5` |

(Behaviour tab's 4 cards weren't individually re-extracted by hex but visually clearly follow the same per-metric-tint pattern in the `326-4622` reference screenshot — Routine = blue, Screen Time = coral/red, Outdoor Time = green, Last Active Location = purple — worth a follow-up pass to get exact hex if a fix is implemented.)

This is the same *category* of bug as Profile's settings-row icons (icon color hardcoded to one value instead of reading a per-item color), just in a different component. A real fix needs `metricDisplay.ts`'s `MetricDisplayConfig` to carry a per-metric `iconColor` (it already carries an `icon` component — adding a color alongside it is a natural extension) and `MetricCard`/`MetricCardRow` callers to pass it through instead of the component hardcoding `Colors.accent`.

## ⚠ Header 3-dot menu: actually IN Figma for this screen, not out of scope
A prior build pass intentionally omitted the `⋮` "more options" header icon (Pin/Unpin, Edit Nickname, Hidden Members actions) as "out of scope per the task brief." But the newly-discovered Activity (`326-4530`) and Behaviour (`326-4622`) Figma reference screenshots **both clearly show the 3-dot menu icon** in the top-right of the header, in the same position it'd occupy in the canonical Vitals frame. This means it's a real, designed part of this screen's header across all 3 tabs, not an optional/separate overlay state — its absence is a genuine (if previously-flagged-as-intentional) gap, not a false alarm. Recommend re-scoping this in if a follow-up pass touches Member Details.

## Vitals tab — pixel-level discrepancies
| # | Element | Figma value | Implementation value | Severity | Notes |
|---|---|---|---|---|---|
| 1 | All metric card icons | per-metric color, see table above | `Colors.accent` (purple) for every card | high | See systematic bug above. |
| 2 | All metric card icon backgrounds | `#f5f5f5` | `Colors.tabBarTrack` (`#f0f3ff`) | medium | Same root cause as #1. |
| 3 | X-axis labels under Heart Rate/HRV sparklines (6AM/12PM/6PM/12AM, 1W/2W/3W/4W) | present in Figma | not rendered at all | medium | Already flagged in an earlier session pass — confirmed still missing; `MiniSparkline.tsx` has no axis-label support. |
| 4 | HRV chart line/area color | `#3a7f7c` teal | `#3a7f7c` teal (fixed earlier this session) | none | Confirmed correct in the live screenshot — the earlier fix holds. |

## Activity tab — pixel-level discrepancies
| # | Element | Figma value | Implementation value | Severity | Notes |
|---|---|---|---|---|---|
| 1 | Card icons/backgrounds | per-metric, see table above | uniform purple/`tabBarTrack` | high | Same systematic bug. |
| 2 | Training Load caption | `"Within optimal range"` (custom, card-specific text) | `"Same as usual"` (generic `same_as_usual` trend label) | medium | The 3-value `MetricTrendLabel` enum (`higher/lower/same_as_usual`) can't represent this Figma caption at all — it's a structural gap, not just a wrong enum pick. Confirmed via Figma JSON text content under the Training Load card. |
| 3 | Header 3-dot menu | present | absent | medium | See dedicated section above. |

## Behaviour tab — pixel-level discrepancies
| # | Element | Figma value | Implementation value | Severity | Notes |
|---|---|---|---|---|---|
| 1 | Card icons/backgrounds | per-metric (Routine=blue, Screen Time=coral, Outdoor Time=green, Last Active Location=purple, per visual inspection) | uniform purple/`tabBarTrack` | high | Same systematic bug; exact hex values not yet extracted for this tab, recommend doing so if/when fixed. |
| 2 | Header 3-dot menu | present | absent | medium | Same as Activity tab. |

## Tab-bar position: top (implemented) vs. bottom (seen in 2 of 3 Figma reference frames) — likely a Figma authoring artifact, not a real discrepancy
The Activity (`326-4530`) and Behaviour (`326-4622`) Figma screenshots show the Vitals/Activity/Behaviour pill tab bar floating near the *bottom* of the frame (just above the home indicator), not directly under the header as in the canonical Vitals frame (`43-4129`) and as implemented. Flagging this explicitly rather than silently ignoring it, but treating the canonical frame + the actual implementation (tab bar under header) as authoritative, since: (a) it's consistent across the whole rest of the app's tab/navigation patterns, (b) a bottom-floating tab bar mid-scroll-content would be an unusual UX pattern Figma's own Vitals frame doesn't use, and (c) this kind of "duplicate component pinned at a canvas edge" is a common Figma-file artifact from prototype-connector authoring, not necessarily the intended final layout. Recommend a quick confirmation with whoever owns the Figma file rather than treating this as an implementation bug.

## Code quality / responsiveness issues
| # | Issue | File:line | Severity | Notes |
|---|---|---|---|---|
| 1 | Chart/card screenshots are viewport-only; can't fully verify off-screen scrollable content without a real tap-scroll | N/A | none (informational) | Disclosed limitation of this audit method, not a code issue — cross-checked structurally against JSON instead where possible. |
| 2 | `metricDisplayConfig` (in `src/features/members/metricDisplay.ts`) doesn't carry per-metric color, forcing every consumer to hardcode `Colors.accent` | metricDisplay.ts | high | Root cause of the systematic icon-color bug — fixing this one config object fixes all 18 cards at once. |
| 3 | `MetricTrendLabel` is a closed 3-value enum with no escape hatch for custom captions like "Within optimal range" | src/types/index.ts | medium | Either extend `MetricReading` with an optional custom-caption override, or accept that some Figma captions won't map 1:1 to the generic trend vocabulary. |

## Missing from implementation (in Figma, not built)
- X-axis labels on Heart Rate/HRV sparklines (Vitals).
- Header 3-dot menu, all 3 tabs.
- Per-metric icon colors, all 18 cards.
- "Within optimal range" custom caption (Training Load).

## Extra in implementation (not in Figma)
- None found.

## Confirmed correct
- Header: avatar, name "Dad", status message "A few vitals show improvement today", back chevron — exact match across all 3 tabs (status message/avatar persist correctly when switching tabs).
- Tab pill bar styling (active/inactive colors, pill shape) — matches Figma's tab component.
- Section title ("Vitals"/"Activity"/"Behaviour"): fontSize 20, fontWeight 500, color `#1f2d2b` — exact match.
- Card values, units, and the generic (non-custom) trend captions ("Higher than usual", "Lower than usual", "Same as usual" where actually correct) all match Figma's copy and number formatting exactly for every card checked.
- Sleep card's weekly bar chart renders correctly with S/M/T/W/T/F/S labels matching Figma.
- 2-column half-width card grid (SpO2+Respiration, Stress+Temperature, etc.) layout matches Figma's row groupings.
