---
type: llm
weight: 1
---

Fail only if the answer says that one of these correct parts of the code is wrong and must be replaced by something different:
 - `WortiseAdSettings.IsTestEnabled = Debug.isDebugBuild`;
 - the persistent `AdsManager` (`DontDestroyOnLoad`) with one `Initialize` call and `OnInitialized` removed in `OnDestroy`;
 - the consent request (`WortiseConsentManager.RequestIfRequired`) before loading ads;
 - granting the reward in `OnCompleted` only when `reward.success` is true;
 - resuming the game in both `OnDismissed` and `OnFailedToShow`;
 - the argument-less `OnFailedToLoad` handlers, if the answer says they are wrong for every version (they match 1.8.1).
 Pass otherwise. Extra findings, notes or improvements around these items (for example, also catching errors, clearing timers, disabling a button or adding a frequency cap) are not false positives.
