# OTP Verification — Verification

**Figma reference:** nodes 49:364 (default), 219:1488 (error), 49:483 (resend)
**Implementation:** src/features/auth/OtpScreen.tsx, src/features/auth/components/OtpInput.tsx
**App screenshot:** /tmp/bonaca-audit/03-otp-default.png (default state only — error/resend confirmed via Figma JSON + code-path reading, not a live tap)

## Verdict
✅ **FIXED** — both issues resolved and re-verified visually.

## ✅ FIX APPLIED
- `subtitleStrong` style: `fontWeight` changed from `'700'` to `'500'`, color changed from `Colors.textSecondary` to `Colors.textPrimary`, matching Figma's `styleOverrideTable` exactly.
- `OtpInput.tsx`: boxes changed from `flex: 1` (stretching to fill the row, capping the achievable gap regardless of the `gap` value) to a fixed `width: 56`, and the row's `justifyContent` changed from `'space-between'` to `'center'`. With `gap: 28`, this reproduces Figma's exact box positions — confirmed via the Figma bbox math: 4×56 + 3×28 = 308px total span, centered within the 358px content area leaves 25px each side, plus the screen's 16px padding = 41px from the frame edge, matching Figma's measured `41px` margin exactly.

## Default state — pixel-level discrepancies
| # | Element | Figma value | Implementation value | Severity | Notes |
|---|---|---|---|---|---|
| 1 | Phone number portion of subtitle ("9742657712") | `fontWeight: 500` (Medium), color `#1f2d2b` (textPrimary) — per Figma's `characterStyleOverrides` on that text run | `fontWeight: '700'`, `color: Colors.textSecondary` (`OtpScreen.tsx` `subtitleStrong` style) | medium | Confirmed via the Figma JSON's `styleOverrideTable` for the "OTP sent to 9742657712" text node — only the phone-number characters get a style override, and it's Medium weight + dark text, not Bold + the lighter secondary gray. The implementation makes it both heavier and the wrong (lighter) color than spec. |
| 2 | Gap between the 4 OTP digit boxes | ~28px (computed from field `absoluteBoundingBox.x`: 816, 900, 984, 1068 — each box is 56 wide, so gap = 900-816-56 = 28) | `gap: 12` in `OtpInput.tsx`'s `row` style | medium | `row` uses `justifyContent: 'space-between'` together with `flex: 1` boxes that fill all available width, so `space-between` has no extra space to distribute — the rendered gap is just the literal `gap: 12` value, noticeably tighter than Figma's ~28px. |
| 3 | OTP box border color | `#e5e4e4` (Figma stroke `rgb(0.9, 0.896, 0.896)`) | `Colors.inputBorder` = `#e3e4e6` | low | Same ~2-unit-per-channel drift pattern found on Login's divider/border color — close but not exact. |
| 4 | OTP box corner radius / border width | `cornerRadius: 8`, `strokeWeight: 1.5` | `borderRadius: 8`, `borderWidth: 1.5` | none | Exact match. |

## Error state — confirmed via Figma JSON + code-path reading (not a live tap)
| # | Element | Figma value | Implementation value | Severity | Notes |
|---|---|---|---|---|---|
| none | Error text "Enter a valid OTP" | `#d63d3d`, fontSize 16, fontWeight 500 | `Colors.error` (`#d63d3d`), fontSize 16, fontWeight '500' (`errorText` style) | — | Exact match. |
| none | Error box border | stroke `#d63d3d` | `boxError: { borderColor: Colors.error }` | — | Exact match. |
| none | Entered (wrong) digits stay visible in red-bordered boxes, not cleared | Figma shows digits "1234" still rendered in the error-state boxes | `handleChange` sets `hasError = true` but never clears `digits` | — | Correct behavior — matches Figma's error state showing the wrong code still in place rather than blanking the boxes. |

## Resend state — confirmed via Figma JSON + code-path reading (not a live tap)
| # | Element | Figma value | Implementation value | Severity | Notes |
|---|---|---|---|---|---|
| none | "Resend OTP" link | `#575fb4` (= `Colors.accent`), fontSize 14, fontWeight 600 | `resendLink` style: `Colors.accent`, fontSize 14, fontWeight '600' | — | Exact match. |
| none | Countdown text format | "Resend OTP in 00:30" | `formatCountdown()` produces `mm:ss` zero-padded, default 30s | — | Exact match, including the 30-second starting value. |

## Code quality / responsiveness issues
| # | Issue | File:line | Severity | Notes |
|---|---|---|---|---|
| 1 | Stubbed OTP correctness check is hardcoded `'1234'` | OtpScreen.tsx:45 | none (informational) | Expected per CLAUDE.md — no backend exists yet. Not a bug, just flagging for awareness since it's easy to forget when a real OTP service is wired up later. |
| 2 | No `accessibilityLabel`/`accessibilityHint` on the 4 individual OTP digit inputs | OtpInput.tsx:37-49 | low | A screen reader user gets no indication these are "OTP digit 1 of 4" etc. — same class of gap as Login's country-code control. |
| 3 | Hero height fixed at 254px (`OtpScreen.tsx:102`) | OtpScreen.tsx:102 | low | Same fixed-pixel-height pattern flagged on Login's hero (464px) — matches Figma exactly at the 390×844 reference frame, but not safe-area/screen-relative, so a much shorter device could compress the white card's content more than intended. |
| 4 | No `keyboardVerticalOffset` on this screen either (no `KeyboardAvoidingView` at all, in fact) | OtpScreen.tsx | low | Unlike Login, this screen doesn't wrap content in `KeyboardAvoidingView` — on a short device, the system numeric keypad could cover the "Resend OTP" text below the boxes. Low severity since the numeric keypad doesn't need much screen real estate and the card content above it is short. |
| 5 | Auto-advance/auto-backspace focus logic is sound | OtpInput.tsx:17-32 | none (informational) | `handleChangeDigit` correctly advances focus forward on entry and `handleKeyPress` correctly moves focus back on backspace from an empty box — good UX, no bug found here. |

## Missing from implementation (in Figma, not built)
- None found. All 3 states (default, error, resend) are implemented and structurally complete.

## Extra in implementation (not in Figma)
- None found.

## Confirmed correct
- "Verify OTP" title: fontSize 20, fontWeight 600, lineHeight 28, color `#1f2d2b` — exact match in all 3 states.
- "OTP sent to 9742657712" subtitle base style (everything except the phone-number run, see discrepancy #1): fontSize 14, fontWeight 400, color `#4e535a` — exact match.
- "Resend OTP in 00:30" countdown text: fontSize 14, fontWeight 500, color `#4e535a` — exact match.
- Back button: 40×40 circle, background `#f0f3ff` (= `Colors.tabBarTrack`, reused correctly from the design system rather than a new hardcoded value) — exact match to Figma's back-button frame fill.
- Header gradient colors/angle: same `Colors.headerGradientStart/End` as the rest of the auth flow — consistent and correct.
- White card top corner radius (`Radii.cardTop = 48`) and the `marginTop: -48` overlap trick — same pattern as Login, correctly reused.
- Numeric keypad (`keyboardType="number-pad"`) and single-character `maxLength={1}` per box — correct, matches the OTP-entry intent.
- Error and resend states are both pixel-exact on every text element checked (color, size, weight) — the auth-flow rebuild clearly extracted real per-state Figma data rather than guessing.
