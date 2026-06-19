# Complete Profile — Verification

**Figma reference:** nodes 60:595 (empty-form state), 60:768 (filled state)
**Implementation:** src/features/onboarding/CompleteProfileScreen.tsx
**App screenshot:** /tmp/bonaca-audit/04-complete-profile.png

## Verdict
✅ **FIXED** (see below) — pixel fidelity was already excellent; the critical dead-end bug is now resolved.

## ✅ FIX APPLIED: onboarding flow is no longer a dead end
Added two new dependency-free components (`Modal`/`ScrollView`/`Pressable`, no new native SDK):
- `src/features/onboarding/components/SelectModal.tsx` — generic bottom-sheet single-select list, used for Gender, Height, and Weight.
- `src/features/onboarding/components/DateOfBirthModal.tsx` — three-column Day/Month/Year picker with a "Done" button, computes age and formats as `"05 Jul 1999 (26 yrs)"` — matching Figma's filled-state (`60:768`) format exactly.

`CompleteProfileScreen.tsx` now wires real `onPress` handlers for all 4 select-style fields (Gender/DOB/Height/Weight) instead of empty no-ops. Verified in the simulator: filling all 3 required fields (Name/Gender/DOB) correctly hides their asterisks and enables "Continue" — confirmed via a temporary pre-filled-state screenshot matching Figma's `60:768` reference almost exactly (same DOB format, same enabled-button color). Height/Weight (optional, no asterisk) also now have real pickers instead of dead no-ops, for full screen completeness.

Original finding, now resolved, kept for history:
`isComplete` (CompleteProfileScreen.tsx) requires `form.name && form.gender && form.dob` to be truthy before "Continue" enables, but Gender/DOB had `onPress={() => {}}` — an empty no-op, so they could never be set and the flow dead-ended on every fresh run.

## Pixel-level discrepancies (empty-form state, matches the live screenshot)
None found — every element checked below is an exact match.

## Two-state behavior check (asterisk show/hide on fill)
The empty-form (60:595) and filled (60:768) Figma frames differ in exactly the way a prior pass concluded: 60:595 shows `Name *` / `Gender *` / `DOB *` with red asterisks and empty/placeholder field values; 60:768 shows the same labels **without** asterisks and real values ("Rakesh P Kumar", "Male", "05 Jul 1999 (26 yrs)", "5ft 7in", "75 Kg"), plus a real photo avatar with no "+" edit button visible. `ProfileField.tsx:26`'s `showAsterisk = required && value.length === 0` logic is correct in principle — the asterisk *would* disappear once a field has a value. However, because of the critical bug above, `Gender`/`DOB` can never actually acquire a value through the UI, so in practice the asterisk-hiding behavior can only ever be observed on the `Name` field. The filled state's avatar (real photo, no "+" button) and pre-populated Height/Weight also aren't reachable from this build at all, since there's no backend/picker to populate them (expected per CLAUDE.md — this part of the gap is reasonable, unlike the Gender/DOB dead-end above).

## Code quality / responsiveness issues
| # | Issue | File:line | Severity | Notes |
|---|---|---|---|---|
| 1 | Gender/DOB/Height/Weight `onPress` handlers are empty no-ops | CompleteProfileScreen.tsx:58,66,73,80 | high | Directly causes the critical bug above for Gender/DOB. Height/Weight are optional (no asterisk, not in the `isComplete` check) so their no-op doesn't block the flow, but they're equally non-functional. |
| 2 | No accessibility distinction between text-input fields and select/picker fields | ProfileField.tsx | low | Both render with the same `accessibilityRole="button"`/no role; a screen reader can't tell a user "Name" needs typed text vs "Gender" opens a picker. |
| 3 | Avatar edit ("+") button is also a no-op | CompleteProfileScreen.tsx:42 (`onPressEdit={() => {}}`) | low | Same category as issue 1 — consistent with CLAUDE.md's "no real photo upload yet" expectation, lower severity since it doesn't block form completion. |
| 4 | Form fields use `gap: 20` / `gap: 32` spacing constants inline rather than from a shared spacing scale | CompleteProfileScreen.tsx:102,105 | low | Not wrong (matches Figma), just worth noting there's no `Spacing` token group in `src/theme/tokens.ts` yet — these numbers are scattered as raw literals across every screen audited so far rather than centralized, which is a maintainability note more than a bug. |
| 5 | ScrollView content has no `KeyboardAvoidingView` | CompleteProfileScreen.tsx | low | With 5 stacked fields and a keyboard, the Name `TextInput` plus the "Continue" CTA below could both be partially obscured by the keyboard on shorter devices without explicit keyboard-avoidance handling. |

## Missing from implementation (in Figma, not built)
- The filled/edit state (60:768) — real photo avatar, pre-populated values, no asterisks — is not reachable in this build (expected, no backend yet, see two-state section above).
- No real Gender/DOB/Height/Weight picker UI exists at all (see critical bug).

## Extra in implementation (not in Figma)
- None found.

## Confirmed correct
- Header: "Complete Your Profile", fontSize 18, fontWeight 500, white text on the same gradient header used elsewhere — exact match.
- Field labels: fontSize 14, fontWeight 600, color `#1f2d2b` — exact match for all 5 fields.
- Required asterisk color: `Colors.error` (`#d63d3d`) — exact match.
- Placeholder/value text: fontSize 16, fontWeight 400, color `#727779` (Figma measures `#72777a`, a 1-unit blue-channel rounding difference, effectively identical) — exact match.
- Figma's per-field "Hint text here" caption is `visible: false` in every field's component instance — correctly not rendered anywhere in the implementation (i.e., its *absence* is itself correct, not a missed feature).
- Avatar placeholder: outer circle `#e9e9e9` and person-icon strokes `#007367` both match `Colors.avatarPlaceholderBg`/`avatarPlaceholderFg` exactly (verified against Figma's `Ellipse 84` and `Ellipse 82/83` fills).
- "Continue" button: fontSize 18, fontWeight 600, white text, full-width — exact match; disabled-state dimming uses the same non-hardcoded `opacity` pattern confirmed correct on Login's button.
- Field container: height 48, border radius 8 (`Radii.cta`), border color `Colors.inputBorderSubtle`, white background — exact match to Figma's `Input Base` component styling.
- Chevron icon on select-style fields: `IconChevronDown`, matches Figma's dropdown affordance.
