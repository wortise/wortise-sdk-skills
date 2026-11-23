---
type: llm
weight: 2
---

The answer identifies at least 4 of these problems:
1. `WortiseSdk.initialize` is not awaited, and test mode is set after initializing.
2. On `FAILED_TO_LOAD` a new `InterstitialAd` is created and loaded immediately (no delay, and the new object has no listener).
3. The ads are never destroyed (`dispose` does not call `destroy()`).
4. `showAd` is called without checking that the ad is available; when nothing is shown, `waitingForAd` stays `true` and the "Next round" button stays disabled.
5. `autoRefreshTime: 60` is in milliseconds, so the value is wrong (meant 60000).
6. User consent is not handled before ads are requested.
