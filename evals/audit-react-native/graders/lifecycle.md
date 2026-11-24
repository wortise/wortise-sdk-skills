---
type: llm
weight: 2
---

The answer identifies at least 4 of these problems:
1. `RNWortiseSdk.initialize`, `setTestEnabled` and `addEventListener` run in the component body on every render, instead of once in an effect.
2. `setAdUnitId` for the interstitial runs at module scope, and for the rewarded ad on every tap.
3. The event listeners are never removed.
4. `autoRefreshTime={60}` is in milliseconds, so the value is wrong (meant 60000).
5. `isAvailable` is not used correctly before showing (rewarded never checks it; interstitial checks it right after starting the load).
6. User consent is not handled before ads are requested.
