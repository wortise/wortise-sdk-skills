---
type: llm
weight: 3
---

The answer identifies at least 4 of these 5 problems:
 1. The Compose banner calls `loadAd()` in the `update` block of `AndroidView`, which runs again on recompositions, so the banner is loaded again and again; it should be created once (with `remember`) and loaded in `factory`. Mentioning that the banner is also never destroyed (no `DisposableEffect`/`onDispose`) counts as part of this item, not instead of it.
 2. `interstitialAd?.loadAd()` in `onResume` loads the interstitial again on every resume (including when the user comes back from the ad or from another app), which discards an ad that was already loaded; this explains impressions much lower than requests.
 3. `onRewardedCompleted` grants the coins without checking `reward.success`.
 4. `AppOpenManager` (in `WordTilesApp`) and a manually shown `AppOpenAd` (`tryToShowAd` in `onStart`) are used together, so app open ads are requested and shown twice; keep only one of the two.
 5. The `AppOpenAd` shown from `onStart` also appears right after an interstitial or rewarded ad closes (or on every return), with no skip after another fullscreen ad.
 Fail if fewer than 4 are identified correctly.
