---
type: llm
weight: 2
---

The code grants the coins only in `onRewardedCompleted` when `reward.success` is true (not on dismissed or shown), loads the ad ahead of the tap, checks `isAvailable` before `showAd`, loads the next ad after dismissal, does not retry immediately after a failed load, and calls `destroy()` in `onDestroy`. Fail if the coins are granted anywhere else, or if two or more of the other points are missing.
