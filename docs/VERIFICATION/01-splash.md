# Splash — Verification

**Figma reference:** node 43:3178 (`.design-reference/screens/splash__43-3178`)
**Implementation:** src/features/auth/SplashScreen.tsx
**App screenshot:** /tmp/bonaca-audit/01-splash.png

## Verdict
Pass with minor issues

## Pixel-level discrepancies
| # | Element | Figma value | Implementation value | Severity (low/med/high) | Notes |
|---|---|---|---|---|---|
| 1 | Gradient angle | Handle vector (normalized, frame-space) start `(0.971, -0.432)` → end `(0.206, 1.214)`; in 390×844 pixel space this resolves to ≈102° (steep, nearly top-to-bottom, slightly right-leaning) | `start={{x:0.95,y:0.29}}`, `end={{x:0.05,y:0.71}}` → in pixel space ≈135° (shallower diagonal) | medium | The implementation's gradient is visibly shallower/more diagonal than Figma's steeper near-vertical sweep. Both screenshots read as "dark top-right to lighter bottom-left" at a glance, but the angle is off by ~30°, so the dark navy patch in the implementation covers noticeably less of the top edge than in Figma. |
| 2 | Gradient stop colors | Stop 1 `#090c2c` (pos 0.0), Stop 2 `#555ec2` (pos ≈0.950) | `Colors.headerGradientStart = #090c2c`, `Colors.headerGradientEnd = #555ec2`, `locations={[0.03, 0.81]}` | low | Hex colors are an exact match. Stop *positions* differ slightly (0.0/0.95 in Figma vs 0.03/0.81 in code) — a minor contributor to the visual gradient-spread difference noted in row 1, but trivial on its own. |
| 3 | Logo size & position | Logo group `absoluteBoundingBox`: 80×80, centered exactly in the 390×844 frame (offset from true center = 0,0) | `mark: { width: 80, height: 80 }` inside a `flex:1, alignItems:'center', justifyContent:'center'` container | none | Exact match — size and centering are pixel-correct relative to Figma's reference frame. |

## Code quality / responsiveness issues
| # | Issue | File:line | Severity | Notes |
|---|---|---|---|---|
| 1 | No `StatusBar` style override | src/features/auth/SplashScreen.tsx (whole file); confirmed absent from src/app/_layout.tsx and src/app/(auth)/_layout.tsx too | medium | The screenshot shows the iOS status bar (time, signal/wifi/battery icons) rendering in dark/default style against the dark navy (`#090c2c`) top-right corner of the gradient, producing poor contrast. `expo-status-bar` (`~56.0.4`) is already a project dependency but unused anywhere in the app — a `<StatusBar style="light" />` on this screen (or globally, given the header gradient pattern recurs per CLAUDE.md) would fix it cheaply. |
| 2 | Missing accessibility props | src/features/auth/SplashScreen.tsx:29-35 | low | No `accessible`, `accessibilityRole`, or `accessibilityLabel` on the root view or logo `Image`. Low impact since this is a 1.8s transient auto-advancing screen with no interactive elements, but a screen reader landing here gets no announcement (e.g. "Bonaca, loading"). |
| 3 | Auto-advance timer duration unspecified by design | src/features/auth/SplashScreen.tsx:9 (`AUTO_ADVANCE_DELAY_MS = 1800`) | low | Figma is a static frame with no prototype/transition metadata in the JSON, and PRD.md only specifies the flow order (Splash → Login), not timing. 1800ms is a reasonable, harmless implementation choice — flagging only because it's an invented value with no design source of truth, not because it's wrong. |
| 4 | Fixed-pixel logo size | src/features/auth/SplashScreen.tsx:51-52 (`width: 80, height: 80`) | low | Not scaled relative to screen width/height or safe-area-relative sizing. Acceptable for a brand mark (logos are conventionally fixed-size across devices and this matches Figma's literal 80×80 spec at the reference 390×844 frame), but flagging since CLAUDE.md calls out fixed-pixel dimensions assuming one screen size as a pattern to watch for. On much smaller (SE) or larger (Pro Max) devices the logo will occupy a different proportion of the screen than on the 390×844 reference, though this is unlikely to look broken in practice. |

## Missing from implementation (in Figma, not built)
- None found. The Figma frame for this screen contains only the gradient background and the centered logo mark (decomposed into several vector paths in the JSON) — both are represented in the implementation (gradient + single flattened `bonaca-mark.png` image standing in for the vector group).

## Extra in implementation (not in Figma)
- None found. No additional text, buttons, or decorative elements beyond the gradient and logo are present in the rendered screenshot that aren't accounted for by the Figma frame.

## Confirmed correct
- Gradient stop colors: exact hex match (`#090c2c` → `#555ec2`) between Figma fill stops and `Colors.headerGradientStart`/`headerGradientEnd` in `src/theme/tokens.ts`.
- Logo size (80×80) and centering: pixel-exact match to Figma's logo group bounding box, centered in the 390×844 reference frame.
- Container layout uses `flex: 1` with `alignItems`/`justifyContent: 'center'` (no hardcoded screen height/width), so it will responsively adapt and stay centered across different iPhone screen sizes.
- Navigation target (`router.replace('/(auth)/login')`) matches the PRD's documented flow: "Splash → Login - Mobile No. Entry" (docs/PRD.md line 19).
- Logo asset (`bonaca-mark.png`, 170×170 native) is rendered with `contentFit="contain"` at 80×80, a reasonable downscale that will stay crisp on @2x/@3x devices.
- Colors are correctly sourced from `src/theme/tokens.ts` (`Colors.headerGradientStart`/`End`) rather than hardcoded hex literals in the component.
