---
type: llm
weight: 3
---

The answer identifies ALL of these release blockers:
1. Test mode is always on: `AdSettings.setTestEnabled(true)` instead of `kDebugMode` or another debug-only value.
2. The rewarded ad uses the test ad unit `test-rewarded` in release code.
3. The reward (coins) is granted on `RewardedAdEvent.DISMISSED` instead of the completed event when the reward reports success.
4. The interstitial and rewarded ads are created and loaded inside `build()`, so every rebuild creates new ad objects and new requests.
Fail if any of the four is missing or described wrongly.
