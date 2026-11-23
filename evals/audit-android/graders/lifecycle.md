---
type: llm
weight: 2
---

The answer identifies at least 5 of these problems:
1. The interstitial is shown as soon as it loads (`showAd` in `onInterstitialLoaded`), so it appears at launch or at random moments instead of at a natural pause.
2. Failed loads are retried immediately from the failure callbacks (`ad.loadAd()` in `onInterstitialFailedToLoad` and in `onBannerFailedToLoad`), which hits the cooldown or loops; banners retry by themselves.
3. The interstitial is kept in a `companion object`, which leaks the Activity and survives screen recreation.
4. The interstitial and the rewarded ad are never destroyed in `onDestroy`.
5. The SDK is initialized in the Activity instead of `Application.onCreate`.
6. User consent (Wortise `ConsentManager`, Google UMP or another TCF consent tool) is not handled before ads are requested.
7. `showAd` is called without checking `isAvailable`, so the buttons do nothing when no ad is loaded.
