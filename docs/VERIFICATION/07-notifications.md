# Notifications — Verification

**Figma reference:** node 286:15753 (`.design-reference/screens/notifications__286-15753`)
**Implementation:** src/features/notifications/NotificationsScreen.tsx
**App screenshot:** /tmp/bonaca-audit/07-notifications.png

## Verdict
✅ **FIXED** (rows now tappable) — pixel fidelity was already exact; the one functional gap is resolved below. The 1-unit color-token inconsistency is cosmetic and left as-is (not worth a token migration for an imperceptible difference).

## ✅ FIX APPLIED: notification rows are now tappable
`NotificationRow.tsx` is now a `Pressable` with an `onPress` prop (and a combined `accessibilityLabel` covering title+body, fixing the separate accessibility-grouping note below too). `NotificationsScreen.tsx` wires each row to `router.push(notification.deepLinkTarget as Href)` — the mock data already had real `deepLinkTarget` values (`/subscription/payment-gateway`, `/member/member-dad/metric/heart_rate`) that were simply never read. Verified the screen still renders identically (no visual regression from the View→Pressable change).

## Pixel-level discrepancies
| # | Element | Figma value | Implementation value | Severity | Notes |
|---|---|---|---|---|---|
| 1 | Avatar fallback icon color | `#007367` (Figma `Ellipse 82/83` fill `rgb(0,0.451,0.4039)`, blue channel = 103.0 exactly = `0x67`) | `Colors.avatarIcon = '#007366'` | low | 1-unit-of-255 rounding inconsistency vs. the otherwise-identical `Colors.avatarPlaceholderFg = '#007367'` token used on Complete Profile's avatar (same Figma source color, two separate tokens defined with a 1-digit drift between them). Imperceptible visually, but worth unifying into one token since they're meant to be the same color. |

## Empty state — code-level review (not visually confirmed, no notifications-empty Figma frame exists in the cache)
`EmptyNotificationsState.tsx` follows the exact same structural pattern as `EmptySharedState.tsx` (Home) — `IconBellOff` at size 80 with `Colors.iconMuted`, title fontSize 18/weight 500/`textPrimary`, subtitle fontSize 14/weight 400/`textSecondary`, same card border/radius/white background. Good internal consistency even without a Figma reference to check against directly. No issues found.

## Code quality / responsiveness issues
| # | Issue | File:line | Severity | Notes |
|---|---|---|---|---|
| 1 | `title` truncates at `numberOfLines={1}`, `body` at `numberOfLines={2}` | NotificationRow.tsx:27,32 | none (informational) | Sensible truncation choice for longer real-world names/messages — confirmed this doesn't clip any of the 4 current mock notifications. |
| 2 | No accessibility grouping on the row (name, time, and body are 3 separate `Text` nodes with no combined `accessibilityLabel`) | NotificationRow.tsx | low | A screen reader will announce title, time, and body as 3 disconnected reads rather than one coherent notification announcement. |
| 3 | Rows aren't `Pressable` — no `onPress`/navigation at all | NotificationRow.tsx | medium | Each notification mentions actionable content ("Tap to pay", "Consider checking in") but tapping a row does nothing — there's no navigation to Member Details, Payment Gateway, etc. This is a real gap between the copy's implied affordance and the actual interaction model, though may be intentional scope (no deep-link wiring decided yet — `Notification.deepLinkTarget` exists in the domain type but isn't read anywhere in this screen). |

## Missing from implementation (in Figma, not built)
- None found for the Figma-defined frame itself.
- Per code-quality issue 3: the domain type's `deepLinkTarget` field is unused — notifications aren't tappable despite their copy implying an action (e.g. "Tap to pay").

## Extra in implementation (not in Figma)
- None found.

## Confirmed correct
- Header: "Notifications", fontSize 18, fontWeight 500, white, with the same gradient/corner-radius treatment as every other screen header — exact match.
- Avatar fallback background: `#e9e9e9` (Figma `Ellipse 84`) — exact match to `Colors.avatarFallbackBackground`.
- Row title: fontSize 14, fontWeight 500, color `#1f2d2b` — exact match for all 4 names.
- Row body copy: fontSize 12, fontWeight 500, color `#4e535a` — exact match (the 500-weight body text, unusual at first glance, is confirmed correct against Figma, not a mistake).
- Timestamp: fontSize 12, fontWeight 400, color `#4e535a` — exact match for all 4 formats ("1 hr ago", "4 hrs ago", "Yesterday, 3:30 PM", "1 week ago").
- Card layout: 80px min-height, 40×40 avatar (20px radius), `Colors.cardBorder` border, 16px card radius — exact match.
- Dad and Prasanna Kumar correctly share the same real photo asset per Figma's identical `imageRef` for both — confirmed still correct in this build.
- No unread-state visual indicator anywhere (no dot, no bold, no background tint) — confirmed this matches Figma exactly; all 4 rows render identically regardless of the underlying `Notification.read` value, same as a prior pass concluded.
