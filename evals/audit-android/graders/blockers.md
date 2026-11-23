---
type: llm
weight: 3
---

The answer identifies ALL of these release blockers in the app:
1. Test mode is always on: `AdSettings.testEnabled = true` with no debug-only condition (MainActivity.kt).
2. The manifest uses Google's sample application ID (`ca-app-pub-3940256099942544~...`) instead of the app's own Google App ID from the Wortise dashboard.
3. The rewarded ad uses the test ad unit `test-rewarded` in release code.
4. The reward (coins) is granted in `onRewardedDismissed` instead of `onRewardedCompleted` when the reward reports success.
Fail if any of the four is missing or described wrongly.
