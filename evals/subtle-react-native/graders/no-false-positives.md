---
type: llm
weight: 1
---

Fail only if the answer says that one of these correct parts of the code is wrong and must be replaced by something different:
 - `RNWortiseAdSettings.setTestEnabled(__DEV__)`;
 - `startAds()` called once from the root component's effect, with `initialize` and `requestIfRequired` awaited before ads load;
 - the delayed retries (`retryLater` with `cooldownRemainingMs`, at least 30 s) as a retry strategy (saying that duplicated listeners also duplicate the retries, or that startup errors are not caught, is a valid finding, not a false positive);
 - `autoRefreshTime={60000}` (it is in milliseconds);
 - `bannerRef.current?.loadAd()` called once after `adsReady`;
 - `if (await RNWortiseRewarded.isAvailable)` in `onFreeGems`.
 Pass otherwise. Extra findings, notes or improvements around these items (for example, also catching errors, clearing timers, disabling a button or adding a frequency cap) are not false positives.
