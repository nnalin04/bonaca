# Connect Wearable (Onboarding) — Verification

**Figma reference:** node 60:634 (`.design-reference/screens/connect-wearable__60-634`)
**Implementation:** src/features/onboarding/ConnectWearableScreen.tsx
**App screenshot:** /tmp/bonaca-audit/05-connect-wearable.png

## Verdict
✅ **FIXED** — casing typo corrected and re-verified; tagline line-break left as-is (cosmetic, current single-line wrap looks clean); the Phase 2 vendor conflict remains an open product flag, not a code bug.

## ✅ FIX APPLIED
"Skip For Now" → "Skip for Now" (both the rendered `Text` and the explanatory code comment). Re-verified in the simulator.

## Pixel-level discrepancies (historical)
| # | Element | Figma value | Implementation value | Severity | Notes |
|---|---|---|---|---|---|
| 1 | "Skip For Now" link copy | `'Skip for Now'` (lowercase "for") | `'Skip For Now'` (capital "For") | low | ✅ Fixed — see above. |
| 2 | Tagline line break | Figma forces a line break after "health": `"Link a wearable account to track health \nand activity"` (two lines) | One unbroken string, wraps naturally based on container width | low | At the implemented font size/container width the text happens to fit on one line in the live screenshot, which arguably looks cleaner than Figma's forced break — flagging for awareness, not recommending a change. |

## ⚠ Product/design conflict: Phase 2 vendors in a Phase 1 screen
Confirmed still present and already self-documented in code (`ConnectWearableScreen.tsx:16-21` has a comment explicitly calling this out). Figma node 60:634 lists Fitbit, Garmin, Samsung Health, and Oura as the connectable providers. Per CLAUDE.md's "Wearable Integration Phasing" section, Phase 1 scope is Apple HealthKit + Google Health Connect ONLY (on-device, no remote API) — these 4 vendors are explicitly Phase 2 (direct per-vendor OAuth, "do not start without separate scoping"), and none of them even existed in the `WearableProvider` domain type until the subscription-flow rebuild added them. The implementation is pixel-faithful to Figma and intentionally does not wire any real OAuth (`setHasSelection(true)` on tap is a pure UI stub) — this is a **design-file vs. documented-roadmap conflict**, not an implementation bug. Worth resolving with whoever owns both the Figma file and the technical roadmap before this screen is treated as "done" in a Phase 1 release.

## Code quality / responsiveness issues
| # | Issue | File:line | Severity | Notes |
|---|---|---|---|---|
| 1 | Tapping ANY provider just sets `hasSelection = true` and reveals a generic "Continue" — no provider-specific state, no actual connection attempt | ConnectWearableScreen.tsx:54 | none (informational) | Reasonable given no real wearable SDK is wired (per CLAUDE.md). Just noting the UI doesn't distinguish which provider was "selected" if a user taps multiple. |
| 2 | `ctaBlock` uses `marginTop: 'auto'` to pin Skip/Continue to the bottom of a `flexGrow: 1` ScrollView content container | ConnectWearableScreen.tsx:92 | low | Works correctly when content is shorter than the viewport (as here, 4 rows), but if more wearable options were ever added such that content exceeds viewport height, `marginTop: auto` inside a scrolling flex container can behave unexpectedly (no longer pins to visual bottom, just sits after content) — worth a comment or a different bottom-pinning approach (e.g. `ScrollView` footer outside the scroll area) if this list grows. |
| 3 | No accessibility distinction for the unimplemented Phase 2 providers (they're tappable but lead nowhere real) | ConnectWearableScreen.tsx | low | Same category noted on other screens — a screen reader user gets no signal these aren't yet functional. |

## Missing from implementation (in Figma, not built)
- None found — every visible Figma element (header, tagline, 4 provider rows, Skip link) is implemented; the hidden `CTA`/`Continue` button (`visible: false` in Figma) is correctly only shown after a selection, which is a reasonable interpretation since Figma has no "connected" state screenshot cached to confirm the exact trigger.

## Extra in implementation (not in Figma)
- None found.

## Confirmed correct
- Header: "Connect Your Wearable", fontSize 18, fontWeight 500, white on gradient — exact match, consistent with every other onboarding screen's header.
- "CTA" frame `visible: false` in Figma is correctly mirrored by the `hasSelection &&` conditional render in code.
- "Skip for Now"/"Continue" link color: `rgb(0.341,0.373,0.706)` = `#575FB4` = `Colors.accent` — exact match (only the casing differs, see discrepancy #1).
- Provider row label color: `#4e535a` (`Colors.textSecondary`), fontSize 14, fontWeight 500 — exact match for all 4 rows.
- Provider row card: 56px height, white background, `Colors.cardBorder` border — matches Figma's row card styling.
- All 4 brand logo images (Fitbit, Garmin, Samsung Health, Oura) render as real cropped brand assets from `assets/images/wearables/`, not generic icons — confirmed visually identical to Figma's logos in this and an earlier verification pass.
- Chevron-right affordance on each row matches Figma's row pattern.
- A stray "Profile Settings" text layer found in the same Figma parent structure (`Frame 2121453960`) appears to be unused/leftover authoring cruft from a duplicated component instance, not a real element this screen needs — correctly not implemented.
