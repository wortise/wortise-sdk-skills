---
type: llm
weight: 1
---

Fail only if the answer contains one of these mistakes:
- tells the developer to add the Google Mobile Ads Unity plugin to make Wortise work;
- grants the reward on `OnDismissed` or `OnShown` in its suggested fix;
- uses AdMob Unity APIs for Wortise ads: `RewardedAd.Load(`, `InterstitialAd.Load(`, `OnUserEarnedReward` or `MobileAds.Initialize`.

Do not judge whether other Wortise APIs exist, and do not penalize extra findings or extra advice that is not in this list. Pass otherwise.
