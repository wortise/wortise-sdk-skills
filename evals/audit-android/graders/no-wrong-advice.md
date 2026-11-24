---
type: llm
weight: 1
---

Fail only if the answer contains one of these mistakes:
- says that `bannerAd.destroy()` in `onDestroy` is wrong or should be removed;
- tells the developer to call `loadAd()` on the banner declared in the XML layout (it loads by itself);
- tells the developer to add the Google Mobile Ads SDK or ad network adapters as dependencies;
- uses AdMob APIs for Wortise ads: `onUserEarnedReward`, `RewardedAdLoadCallback`, `FullScreenContentCallback` or `InterstitialAd.load(`.

Do not judge whether other Wortise APIs exist, and do not penalize extra findings or extra advice that is not in this list. Pass otherwise.
