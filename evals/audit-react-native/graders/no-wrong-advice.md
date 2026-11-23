---
type: llm
weight: 1
---

Fail only if the answer contains one of these mistakes:
- tells the developer to run the app in Expo Go (Wortise needs a development build);
- tells the developer to add `react-native-google-mobile-ads` to make Wortise work;
- grants the reward on `onRewardedDismissed` or `onRewardedShown` in its suggested fix.

Do not judge whether other Wortise APIs exist, and do not penalize extra findings or extra advice that is not in this list. Pass otherwise.
