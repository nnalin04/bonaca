# Payment Gateway — Verification

**Figma reference:** nodes 197:10384 (trial signup), 197:11043 (renewal)
**Implementation:** src/features/subscription/PaymentGatewayScreen.tsx
**App screenshot:** /tmp/bonaca-audit/17-payment-gateway.png

## Verdict
Pass — independently confirmed both Figma frames are genuinely just a text label, and the implementation's "honest stub" approach is the right engineering call given that.

## Figma content audit (what's actually in these frames)
Dumped the **complete** node tree for both `197:10384` and `197:11043` (not just a sample) — both are byte-for-byte structurally identical:
```
Payment Gateway [FRAME]
  Text [TEXT] "Payment Gateway"
  StatusBar [INSTANCE]  (the standard mock status bar, not real content)
```
That's it. No card, no form, no payment-method list, no CTA button, no back button — nothing else exists in either frame. This independently confirms a prior pass's conclusion was accurate, not just trusted.

## Pixel-level discrepancies (for what IS in Figma)
None — the one real element, the "Payment Gateway" label, is an exact match: fontSize 18, fontWeight 400 (Regular), color `#4e535a` (`Colors.textSecondary`), centered. `PaymentGatewayScreen.tsx`'s `title` style matches all four properties exactly.

## Judgment: is the "honest stub" approach correct here?
Yes. With nothing to build from in Figma, the implementation reasonably added: a back button (necessary for navigation — without it this would be a dead end, unlike the Complete Profile dead-end bug found earlier, which was a *blocking* gap with no escape, whereas here the back button is a sensible addition rather than a missing required field), a credit-card icon, explanatory body copy, and an explicit "isn't wired up yet" note plus a stub CTA. This is a transparent, non-misleading placeholder rather than inventing a fake-functional payment form — exactly the right call per CLAUDE.md's "design is the spec" principle when the spec is empty: build the minimum reasonable scaffolding and say so, don't fabricate UI Figma never specified.

One thing worth checking against the PRD: `docs/PRD.md`/`docs/TECHNICAL_REQUIREMENTS.md` do describe the intended payment methods (UPI/PayPal/Amex/Mastercard/Apple Pay, per the Select Wearable Account screen's earlier-built `PaymentMethodType`) and the platform-specific billing split (StoreKit/Razorpay per CLAUDE.md's Tech Stack section) — but neither document specifies this *screen's* visual layout, only the backend/processor decisions. So there's nothing actionable in the PRD that the current stub is missing visually; the gap is purely "no processor is wired up yet," which is explicitly out of scope per CLAUDE.md's "Not Set Up Yet" list, not a missed requirement.

## Code quality / responsiveness issues
| # | Issue | File:line | Severity | Notes |
|---|---|---|---|---|
| 1 | No payment SDK installed | package.json | none (informational, confirmed) | Checked `package.json` — no RevenueCat/Stripe/Razorpay/StoreKit-related dependency present. Correctly stays UI-only per CLAUDE.md. |
| 2 | CTA `onPress` is an empty function with only a comment explaining why | PaymentGatewayScreen.tsx:54-59 | none (informational) | Honest, well-documented stub — the comment correctly points to the real follow-up work (RevenueCat/StoreKit/Razorpay) rather than silently doing nothing unexplained. |
| 3 | `variant` prop (`trial-signup`/`renewal`) only changes body copy/CTA label, not any visual structure | PaymentGatewayScreen.tsx:14-25 | none (informational) | Reasonable given Figma provides no visual distinction between the two states either (both frames are identical). |

## Missing from implementation (in Figma, not built)
- None — Figma has nothing beyond the label, and the label is implemented correctly.

## Extra in implementation (not in Figma)
- Back button, credit-card icon, body copy, "not wired up yet" note, and CTA button — all absent from Figma's essentially-empty frames, but justified additions for a minimally functional, non-dead-end screen (see judgment section above).

## Confirmed correct
- "Payment Gateway" label: fontSize 18, fontWeight 400, color `#4e535a` — exact match, the only element Figma actually specifies.
