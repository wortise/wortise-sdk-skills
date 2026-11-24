---
type: llm
weight: 2
---

The answer identifies at least 4 of these problems:
1. The SDK is initialized on a background queue (`DispatchQueue.global`) instead of the main thread at launch.
2. The banner delegate is a temporary `BannerLogger()` object; the delegate reference is weak, so it is released immediately and the callbacks never arrive.
3. `Google-Mobile-Ads-SDK` is added by hand to the Podfile next to `WortiseSDK`, which brings it already (possible version conflict).
4. A failed interstitial load is retried immediately from `didFailToLoad`.
5. The ads are never destroyed when the screen closes (`destroy()`).
6. `showAd` is called without checking `isAvailable`.
7. User consent is not handled before ads are requested.
