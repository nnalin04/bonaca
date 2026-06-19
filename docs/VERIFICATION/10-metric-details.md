# Metric Details — Verification

**Figma reference:** nodes 197:3828 (bar-chart style), 197:3909 (smooth-line-with-dots style — see note below)
**Implementation:** src/features/metrics/MetricDetailsScreen.tsx
**App screenshot:** /tmp/bonaca-audit/13-metric-details.png

## Verdict
✅ **FIXED** — all 3 bar-chart gaps resolved and re-verified visually against Figma; now a near-pixel-perfect match.

## ✅ FIX APPLIED
- Added a full-size background `Rect` (`Colors.chartAreaFill`, the token that already existed) behind the chart in `BarChartCard.tsx`.
- Added 2 dashed vertical gridlines (`strokeDasharray="4,4"`) at the 1/3 and 2/3 width marks, aligning with the 6AM/12PM x-axis labels.
- Fixed bar corner radius from `rx={3}` to `rx={5}` (Figma's actual value).
- Added `chartAxisMin`/`chartAxisMax` to `MetricDetailSummary` (`metrics/mockData.ts`) as a fixed display-scale override, distinct from `rangeMin`/`rangeMax` (which stay correct for the "Highest:/Lowest:" summary-card text). Set to `75`/`180` for Heart Rate per Figma; `MetricDetailsScreen.tsx` now prefers this over the computed data range for the chart's Y-axis labels only.

Re-verified in the simulator and compared side-by-side against `.design-reference/screens/metric-details__197-3828.png` — pink tint, dashed lines, and "180 bpm"/"75 bpm" labels all now present and matching.

## Two Figma frames, two different chart styles — which is "correct"?
`197:3828` renders the metric as a **bar chart** (discrete rectangles per data point). `197:3909` renders the *same* data (identical date, average, highest/lowest) as a **smooth line chart with circular point markers**. Both are cached as real, fully-detailed frames — this isn't one being a stub and one real; they're two distinct visual treatments of the same screen, most likely a design exploration where both got left in the file rather than one being deleted. The implementation chose the bar-chart treatment, which is a legitimate, fully-supported choice (matches `197:3828` closely once the gaps below are fixed) — flagging this only so it's clear the choice was deliberate-compatible, not a guess.

## Bar chart styling gaps — now confirmed with exact values
| # | Element | Figma value | Implementation value | Severity | Notes |
|---|---|---|---|---|---|
| 1 | Chart background tint | A single `325×280` rectangle, same size as the whole chart plot area, filled `#e07a5f` (`Colors.chartLine`) at **10% opacity** — i.e. exactly `Colors.chartAreaFill` (`rgba(224, 122, 95, 0.1)`), a token that **already exists** in `tokens.ts` but isn't used in `BarChartCard.tsx` | No background fill — chart plots directly on the white card | medium | Easy fix: add a `View`/`Rect` sized to the chart plot area with `backgroundColor: Colors.chartAreaFill` behind the gridlines/bars. The exact token needed already exists in the codebase, just unused here. |
| 2 | Vertical gridlines at the 6AM and 12PM x-axis marks | Present, dashed (`strokeDashes: [4,4]`), color `#c5c5c5` (`rgb(0.774,0.774,0.774)`) | Not rendered — `BarChartCard.tsx` only draws horizontal gridlines | medium | Figma has exactly 2 dashed vertical lines (confirmed via `absoluteBoundingBox`: near-zero width, full 280px height) at the 6AM/12PM positions, in addition to 4 solid horizontal lines. |
| 3 | Y-axis labels | Fixed round numbers: `"180 bpm"` / `"75 bpm"` (confirmed via Figma text nodes, not computed from data) | Computed from `summary.average.rangeMax`/`rangeMin`: `"148 bpm"` / `"78 bpm"` (the actual data min/max) | medium | Figma uses a fixed display scale (likely a rounded chart-axis convention), not the literal data range. Current implementation shows real data bounds instead — a legitimate design decision either way, but doesn't match Figma's specific numbers. |
| 4 | Bar corner radius | `5` (Figma `Rectangle 34624484` `cornerRadius: 5`) | `rx={3}` (`BarChartCard.tsx:56`) | low | Small, easy fix. |
| 5 | Bar width | `6px` | `BAR_WIDTH = 6` | none | Exact match — confirmed via Figma bbox `width: 6.0`. |
| 6 | Individual bars are plain solid rectangles, not two-tone "candles" | Confirmed — each `RectangleNNNN` node is a single flat-fill rectangle, no separate wick/cap element | Plain solid `Rect` per bar | none | An earlier pass's "candlestick style" description was inaccurate — corrected here. No actual gap; current implementation's approach is structurally right. |

## 7D/4W/1Y range tabs — real Figma coverage or placeholder?
Confirmed: neither cached Figma frame (`197:3828`, `197:3909`) shows anything but the **1D** tab in its active/selected state — both screenshots show identical "1D" pill highlighted, identical date-stepper text, identical chart data. There is no cached Figma reference for what 7D/4W/1Y actually look like with real data. The implementation's `xAxisLabelsByRange` map (`MetricDetailsScreen.tsx:27-32`) supplies plausible placeholder axis labels per range ("Mon/Wed/Fri/Sun", "Wk 1-4", "Jan/Apr/Jul/Oct") but reuses the same 1D chart/summary data underneath regardless of which range tab is active — so switching to 7D/4W/1Y currently changes only the axis labels, not any actual data shape. This is an honest placeholder, not a wrong implementation of something Figma actually specifies — flagging as a known, scoped-out gap rather than a bug.

## Pixel-level discrepancies (everything outside the chart)
None found — header, range-tab pill bar, date stepper, and the average/highest/lowest summary card (including the insight callout box) were all re-checked and are exact matches to the colors/sizes already confirmed in an earlier pass this session.

## Code quality / responsiveness issues
| # | Issue | File:line | Severity | Notes |
|---|---|---|---|---|
| 1 | `CHART_WIDTH = 326` is a hardcoded constant | BarChartCard.tsx:14 | low | Matches Figma's `325-326px` chart width at the reference device size, but won't adapt to a narrower/wider device — could clip or leave a gap on screens far from the 390pt reference width. |
| 2 | No accessibility labeling on the SVG chart itself (no `accessibilityLabel` summarizing the trend for screen-reader users) | BarChartCard.tsx | low | A screen reader gets nothing from the chart beyond the surrounding text card, which does have the average/highest/lowest as real text — so the gap is specifically "no chart-level summary," not a total accessibility failure. |

## Missing from implementation (in Figma, not built)
- Chart background tint (`Colors.chartAreaFill`, token already exists).
- 2 dashed vertical gridlines.
- Fixed-scale Y-axis labels ("180 bpm"/"75 bpm" vs. computed data range).

## Extra in implementation (not in Figma)
- None found.

## Confirmed correct
- Header: metric name + back chevron, gradient — exact match.
- Range tab pill bar: active state background `#575fb4` (Colors.accent), inactive background `#f0f3ff`/`#edeeff` (tabBarTrack/tabBarBorder) — exact match.
- Date stepper: "Wednesday, 14 Jan (Today)" text and chevron treatment — exact match.
- Average/Highest/Lowest summary card: all values, fonts, colors, and the insight callout box background/icon/text — exact match (re-confirmed, not just trusted from the earlier pass).
- Bar width (6px) and overall chart card border/radius/white background — exact match.
- X-axis labels "12 AM / 6 AM / 12 PM / 6 PM": fontSize 12, fontWeight 400, color `#4e535a` — exact match.
