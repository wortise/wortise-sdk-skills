---
type: llm
weight: 1
---

Fail only if the answer contains one of these mistakes:
- tells the developer to add the `google_mobile_ads` package to make Wortise work;
- grants the reward on `RewardedAdEvent.DISMISSED` or `RewardedAdEvent.SHOWN` in its suggested fix;
- uses AdMob APIs for Wortise ads: `RewardedAd.load(`, `InterstitialAd.load(`, `onUserEarnedReward` or `FullScreenContentCallback`.

Do not judge whether other Wortise APIs exist, and do not penalize extra findings or extra advice that is not in this list. Pass otherwise.
