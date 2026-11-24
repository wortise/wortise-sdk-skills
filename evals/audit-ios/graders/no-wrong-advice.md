---
type: llm
weight: 1
---

Fail only if the answer contains one of these mistakes:
- says App Tracking Transparency is required (it is optional);
- tells the developer to keep the hand-added `Google-Mobile-Ads-SDK` pod as a requirement of Wortise;
- uses AdMob APIs for Wortise ads: `GADRewardedAd`, `GADInterstitialAd`, `GADFullScreenContentDelegate` or `userDidEarnRewardHandler`.

Do not judge whether other Wortise APIs exist, and do not penalize extra findings or extra advice that is not in this list. Pass otherwise.
