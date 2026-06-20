# End-to-end flow tests (Maestro)

Real tap/gesture regression tests covering the app's full navigation graph: onboarding (Login → OTP → Complete Profile → Connect Wearable), Home (header icons + bottom tabs), Member Details (tabs + 3-dot menu), Metric Details (range tabs), and the Subscription flow.

## Setup

```bash
curl -Ls "https://get.maestro.mobile.dev" | bash
export PATH="$PATH:$HOME/.maestro/bin"
```

Requires the app already built and running in the iOS Simulator (`npx expo run:ios`) with Metro started.

## Running

```bash
maestro test .maestro/flows/01-onboarding-flow.yaml
maestro test .maestro/flows/02-home-and-member-flow.yaml
maestro test .maestro/flows/03-subscription-flow.yaml
```

`_login-to-home.yaml` is a shared subflow (Login → OTP → Complete Profile → Connect Wearable → Home) reused via `runFlow` at the top of each independent flow, since every flow starts from `clearState: true` for reliability — resuming a running app's mid-navigation state between separate `maestro test` invocations turned out to be unreliable.

## Known quirks (already handled in these flows, documented here for future maintainers)

- **iOS auto-quotes accessibility labels for tab bar items**: a bottom tab bar item's accessible text is `"<Title>, tab, N of 3"`, not just `"<Title>"`. Maestro matches the *full* string by default (not substring), so tab taps need `.*Notifications, tab.*` style regex, not plain `"Notifications"`.
- **Maestro text matching is a full-string regex match by default**, not substring search. Anything reading a combined/derived accessibility label (e.g. a notification row exposing `"Mom: Subscription expiring..."` as one label) needs a `.*substring.*` pattern.
- **Profile screen's back button**: in this harness, `tapOn: "Go back"` reliably resolves to a stale/incorrect coordinate on this specific screen (confirmed via a temporary debug build that the app's `onPress` never fired, while a raw point tap at the same visual position worked every time) — `02-home-and-member-flow.yaml` uses `tapOn: { point: "6%, 6%" }` there instead. This was confirmed to be a test-tooling timing quirk, not an app bug (the app's plain `router.back()` is correct).
