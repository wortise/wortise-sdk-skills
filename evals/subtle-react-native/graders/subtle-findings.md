---
type: llm
weight: 3
---

The answer identifies at least 4 of these 5 problems:
 1. `RNWortiseInterstitial.isAvailable` is used without `await` in `onRunOver`: it is a Promise, always truthy, so `showAd()` runs even when no ad is loaded and the game never goes back to the menu (stuck). Not handling `onInterstitialFailedToShow` (so nothing continues when the ad cannot be shown) counts as part of this item.
 2. The `addEventListener` calls in the `GameScreen` effect are never removed (no cleanup returning `subscription.remove()`), so every visit to the screen adds more listeners: gems are granted several times and `navigation.goBack()` runs several times.
 3. `onRewardedCompleted` grants the gems without checking `success`.
 4. `setAdUnitId` and `loadAd()` run every time `GameScreen` mounts, which discards an ad that was already loaded (`setAdUnitId` destroys the loaded ad); they should run once, from the root (long-lived) component.
 5. The adaptive banner width is not an integer: `Dimensions.get('window').width` needs `Math.floor` (decimal widths break the iOS banner).
 Fail if fewer than 4 are identified correctly.
