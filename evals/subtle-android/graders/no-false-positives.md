---
type: llm
weight: 1
---

Fail only if the answer says that one of these correct parts of the code is wrong and must be replaced by something different:
 - `AdSettings.testEnabled = BuildConfig.DEBUG` (it is the right way to enable test mode);
 - `WortiseSdk.initialize` in `WordTilesApp.onCreate`;
 - the consent flow (`WortiseSdk.wait` + `ConsentManager.requestIfRequired`) before loading ads;
 - the delayed retries in `onInterstitialFailedToLoad` and `onRewardedFailedToLoad` (`postDelayed` with `cooldownRemainingMs`, at least 30 s);
 - the `isAvailable` checks before `showAd`;
 - the `destroy()` calls of the interstitial, rewarded and app open ads in `onDestroy`.
 Pass otherwise. Extra findings, notes or improvements around these items (for example, also catching errors, clearing timers, disabling a button or adding a frequency cap) are not false positives.
