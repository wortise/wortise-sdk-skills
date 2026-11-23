---
type: llm
weight: 2
---

The answer identifies at least 4 of these problems:
1. `WortiseSdk.Initialize` runs on a background thread (`Task.Run`) instead of the main thread.
2. A failed interstitial load is retried immediately from `OnFailedToLoad`.
3. `AutoRefreshTime = 60` is in milliseconds, so the value is wrong (meant 60000).
4. The ads are never destroyed (`OnDestroy` does not call `Destroy()`).
5. The game is only resumed on `OnDismissed`, not when the ad fails to show.
6. User consent is not handled before ads are requested.
