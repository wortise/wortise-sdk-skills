---
type: llm
weight: 3
---

The answer identifies ALL of these release blockers:
1. Test mode is always on: `WAAdSettings.testEnabled = true` with no `#if DEBUG` or similar condition.
2. `Info.plist` has no `GADApplicationIdentifier` with the app's Google App ID (the app crashes at launch).
3. The banner gets no `rootViewController` before `loadAd()`.
4. The rewarded ad uses the test ad unit `test-rewarded` in release code.
5. The reward (hints) is granted in `didDismiss(rewardedAd:)` instead of the completion callback when the reward reports success.
Fail if any of the five is missing or described wrongly.
