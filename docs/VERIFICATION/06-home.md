# Home — Verification

**Figma reference:** node 188:2859 (`.design-reference/screens/home__188-2859`)
**Implementation:** src/features/home/HomeScreen.tsx
**App screenshot:** /tmp/bonaca-audit/06-home.png

## Verdict
Pass — re-audited from scratch against a freshly-pulled Figma reference (not reusing the earlier live-MCP verification), and every element checked is an exact match. This is the strongest screen in the app so far.

## Pixel-level discrepancies
None found.

Every value cross-referenced below was an exact hex/size/weight match between the Figma JSON and the implementation:
- Greeting "Hello Prasanna!": fontSize 18, fontWeight 600, white — `HomeHeader.tsx` `greeting` style.
- Status "Everything looks stable": fontSize 14, fontWeight 400, color `#e1e7ef` (Figma `rgb(0.882,0.906,0.937)`) — exact match to `Colors.textOnDark`.
- Notification badge circle: `#f05252` (Figma `Ellipse 1` fill `rgb(0.941,0.322,0.322)`) — exact match to `Colors.badge`.
- Badge count text: fontSize 11.67, fontWeight 500, white — exact match (Figma measures `11.666666...`, code uses `11.67`, a rounding-precision non-issue).
- "Prasanna Kumar (You)": fontSize 16, fontWeight 500, color `#1f2d2b` — exact match to `Colors.textPrimary`.
- "Last synced: Just now": fontSize 12, fontWeight 400, color `#4e535a` (Figma `rgb(0.306,0.326,0.353)`) — exact match to `Colors.textSecondary`.
- Member sync card corner radius: 16 (Figma `Card` `cornerRadius: 16`) — exact match to `Radii.card`.
- "Nothing here yet": fontSize 18, fontWeight 500, color `#1f2d2b` — exact match.
- Empty-state body copy: fontSize 14, fontWeight 400, color `#4e535a` — exact match, including identical wrapping text.
- Empty-state card corner radius: 16 — exact match.

## Code quality / responsiveness issues
| # | Issue | File:line | Severity | Notes |
|---|---|---|---|---|
| 1 | `unreadNotificationCount={3}` is a hardcoded literal at the call site | HomeScreen.tsx (prop passed to `HomeHeader`) | low | Reasonable for now (no backend/notification source exists yet per CLAUDE.md), but worth a `// TODO: wire to real unread count` comment so it's not forgotten when notifications are wired up — it's currently indistinguishable from a real value at a glance. |
| 2 | Header height fixed at 125px regardless of device | HomeHeader.tsx:64 | low | Same fixed-pixel-header pattern flagged on auth screens — matches Figma exactly at the reference frame size; on a much taller/shorter device the header won't scale, though since it only contains two short text lines and two icon buttons (no content that could overflow), this is lower risk than the auth screens' taller hero sections. |
| 3 | Empty-state card has a fixed `height: 510` | EmptySharedState.tsx:24 | low | Matches Figma exactly, but a fixed height (rather than `minHeight` or flex-based sizing) means if the empty-state copy were ever localized to a longer language, the text could overflow the fixed box rather than the box growing to fit. |
| 4 | `accessibilityRole={onPress ? 'button' : undefined}` on `MemberSyncCard` | MemberSyncCard.tsx:23 | none (informational) | Good practice — only announces as an interactive button when it actually has a tap handler, rather than always claiming to be one. |

## Missing from implementation (in Figma, not built)
- None found.

## Extra in implementation (not in Figma)
- None found. (One curiosity, not flagged as a gap: Figma's notification/profile icon buttons sit inside a `CTA` frame with a faint near-white fill `rgba(255,246,248,1)` and `cornerRadius: 8`, but the Figma *rendered screenshot* shows no visible box behind those icons either — so this is likely an inactive/hover-state layer property in the source file, not something the rendered design actually shows. The implementation correctly matches the rendered screenshot, not the latent layer property.)

## Confirmed correct
- Header gradient colors/angle, member sync card layout (avatar size 56, 28px radius, card height 80, border `Colors.cardBorder`), "Shared with you" section title (fontSize 20, fontWeight 500, color `#1f2d2b` — exact match), empty-state icon (`IconUsers`, size 80, `Colors.iconMuted`), and the overall ScrollView/padding structure all checked and correct.
- `useSafeAreaInsets` correctly pads the scroll content's bottom rather than using a hardcoded value.
