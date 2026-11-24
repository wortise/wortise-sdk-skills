---
type: llm
weight: 3
---

The answer identifies ALL of these release blockers:
1. Test mode is always on: `WortiseAdSettings.IsTestEnabled = true` with no debug-only condition.
2. The banner is created and loaded in `Awake`, before the SDK is initialized in `Start`.
3. The rewarded ad uses the test ad unit `test-rewarded` in release code.
4. The reward (gems) is granted on `OnDismissed` instead of the completed event when the reward reports success.
5. `ShowAd()` is called without checking `IsAvailable`; when nothing is shown, `Time.timeScale` stays at 0 and the game is frozen.
Fail if any of the five is missing or described wrongly.
