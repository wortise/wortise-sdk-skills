---
type: llm
weight: 3
---

The answer identifies ALL of these release blockers:
1. Test mode is always on: `RNWortiseAdSettings.setTestEnabled(true)` instead of `__DEV__` or another debug-only value.
2. The config plugin in `app.json` has no `iosGoogleAppId`, so the iOS app has no Google App ID.
3. The rewarded ad uses the test ad unit `test-rewarded` in release code.
4. The reward (lives) is granted on `onRewardedDismissed` instead of the completed event when the reward reports success.
5. `RNWortiseRewarded.loadAd()` is followed immediately by `showAd()` (and the interstitial is loaded right before showing), so the ad is never ready: ads must be loaded ahead.
Fail if any of the five is missing or described wrongly.
