# Unity: ad formats

Each format has a page with its events: <https://docs.wortise.com/unity-sdk/sdk-integration/banner.md>,
`interstitial.md`, `rewarded.md` and `app-open.md` in the same folder. The practices behind the
code below: <https://docs.wortise.com/unity-sdk/best-practices.md>. The Unity package has no
native ads.

Set-up, `AdUnits` and `AdsManager`: see the `wortise-sdk-integrate` skill's Unity reference.

## Things that hold for every format

- **Nothing happens in the Editor**: `IsAvailable` is always `false` and no event is raised. Write
  every flow so it continues without the ad, and test on a device.
- **The failure events depend on the installed version.** In version 1.8.1, `OnFailedToLoad` and
  `OnFailedToShow` are argument-less `Action` events. A later version passes a `WortiseAdError`.
  Before writing a handler, check whether `Assets/Wortise/Scripts/WortiseAdError.cs` exists:
  without it, use `void OnFailedToLoad()`; with it, `void OnFailedToLoad(WortiseAdError error)`.
  A handler with the wrong signature does not compile. Never copy a handler from code written for
  another version.
- In 1.8.1 the events carry no error reason: read it in the native logs (see the
  `wortise-sdk-troubleshoot` skill).
- **Ads are not tied to scenes.** Banners are drawn on top of the game view and stay on screen,
  refreshing, after a scene change. Call `Destroy()` on every ad in the `OnDestroy` of the object
  that created it, and remove the handlers with `-=`.
- Create ads once (never in `Update`, `OnGUI` or other per-frame code) and only after the consent
  callback of `AdsManager` has run.

## Banner

```csharp
using UnityEngine;

// On an object of a scene that is loaded after the consent request has finished.
// For a banner in the first scene, create it from AdsManager.LoadAds instead.
public class BottomBanner : MonoBehaviour
{
    private WortiseBanner bannerAd;

    private void Start()
    {
        // Width in dp (Android) or points (iOS), not pixels; 320 fits every phone
        WortiseAdSize size = WortiseAdSize.GetAnchoredAdaptiveBannerAdSize(320);

        bannerAd = new WortiseBanner(AdUnits.Banner, size, WortiseAdPosition.Bottom);
        bannerAd.LoadAd();
    }

    private void OnDestroy()
    {
        bannerAd?.Destroy();
        bannerAd = null;
    }
}
```

Rules:

- Call `LoadAd()` once. The banner refreshes by itself and retries by itself after a failed load:
  never call `LoadAd()` from `OnFailedToLoad`, from `Update` or on a timer.
- `Destroy()` the banner before creating another one. A banner created in every scene load
  without being destroyed stacks several banners that keep requesting ads.
- `AutoRefreshTime` is in **milliseconds**, between 30000 and 120000.
  `bannerAd.AutoRefreshTime = 60` is a mistake.
- Sizes: `WortiseAdSize.Height50`, `Height90`, `Height250`, `Height280`;
  `WortiseAdSize.GetAnchoredAdaptiveBannerAdSize(width)` for a banner at the top or bottom of the
  screen (preferred); `WortiseAdSize.GetInlineAdaptiveBannerAdSize(width, maxHeight)`. The width is
  in dp or points: `Screen.width` is in pixels and gives a banner wider than the screen. Unity has
  no exact conversion; 320 is safe.
- Positions: `WortiseAdPosition.Top`, `Bottom`, `TopLeft`, `TopRight`, `Center`, `BottomLeft`,
  `BottomRight`. The banner covers whatever the game draws there: keep buttons and other controls
  out of that area. After `OnLoaded`, `AdHeightPx` gives its height in pixels to reserve the space.
- Collapsible: needs an anchored adaptive size, and applies to the first ad only. Its position
  must match the banner's:

  ```csharp
  bannerAd.LoadAd(new WortiseRequestParameters
  {
      Collapsible = WortiseCollapsiblePosition.Bottom
  });
  ```

## Interstitial, rewarded and app open

Keep the fullscreen ads on the persistent `AdsManager` object, so a loaded ad survives scene
changes. Add this component to the same `GameObject` and call `Load()` from `AdsManager.LoadAds`.
The failure handlers are written for 1.8.1 (see above).

```csharp
using System;
using System.Collections;
using UnityEngine;

public class GameAds : MonoBehaviour
{
    // Minimum time between interstitials. Tell the user the value so they can tune it.
    private const float InterstitialIntervalSeconds = 60f;

    private WortiseAppOpen      appOpenAd;
    private WortiseInterstitial interstitialAd;
    private WortiseRewarded     rewardedAd;

    private float lastInterstitialTime = float.NegativeInfinity;
    private bool  returningFromAd;

    private Action onInterstitialClosed;
    private Action onRewardedClosed;
    private Action pendingReward;

    // Called once, from the consent callback
    public void Load()
    {
        if (interstitialAd != null)
        {
            return;
        }

        interstitialAd = new WortiseInterstitial(AdUnits.Interstitial);
        interstitialAd.OnDismissed    += OnInterstitialClosed;
        interstitialAd.OnFailedToShow += OnInterstitialFailedToShow;
        interstitialAd.OnFailedToLoad += OnInterstitialFailedToLoad;
        interstitialAd.LoadAd();

        rewardedAd = new WortiseRewarded(AdUnits.Rewarded);
        rewardedAd.OnCompleted    += OnRewardedCompleted;
        rewardedAd.OnDismissed    += OnRewardedClosed;
        rewardedAd.OnFailedToShow += OnRewardedFailedToShow;
        rewardedAd.OnFailedToLoad += OnRewardedFailedToLoad;
        rewardedAd.LoadAd();

        appOpenAd = new WortiseAppOpen(AdUnits.AppOpen);
        appOpenAd.AutoReload      = true;
        appOpenAd.OnFailedToLoad += OnAppOpenFailedToLoad;
        appOpenAd.LoadAd();
    }

    // At a natural pause. Returns false when no ad was shown: continue right away.
    // When it returns true, pause the game and resume it in onClosed.
    public bool ShowInterstitial(Action onClosed)
    {
        if (interstitialAd == null || !interstitialAd.IsAvailable
            || Time.realtimeSinceStartup - lastInterstitialTime < InterstitialIntervalSeconds)
        {
            return false;
        }

        lastInterstitialTime = Time.realtimeSinceStartup;
        onInterstitialClosed = onClosed;

        ShowingFullscreenAd();
        interstitialAd.ShowAd();
        return true;
    }

    // When the user chooses to watch an ad for a reward. Same contract as ShowInterstitial.
    public bool ShowRewarded(Action grantReward, Action onClosed)
    {
        if (rewardedAd == null || !rewardedAd.IsAvailable)
        {
            return false;
        }

        pendingReward    = grantReward;
        onRewardedClosed = onClosed;

        ShowingFullscreenAd();
        rewardedAd.ShowAd();
        return true;
    }

    private void OnApplicationPause(bool paused)
    {
        if (paused || appOpenAd == null)
        {
            return;
        }

        // On Android, closing a fullscreen ad also resumes the app: no app open ad then
        if (returningFromAd)
        {
            returningFromAd = false;
            return;
        }

        // Shows the ad if it is loaded, or loads one for the next time
        appOpenAd.TryToShowAd();
    }

    private void OnDestroy()
    {
        if (interstitialAd != null)
        {
            interstitialAd.OnDismissed    -= OnInterstitialClosed;
            interstitialAd.OnFailedToShow -= OnInterstitialFailedToShow;
            interstitialAd.OnFailedToLoad -= OnInterstitialFailedToLoad;
            interstitialAd.Destroy();
        }

        if (rewardedAd != null)
        {
            rewardedAd.OnCompleted    -= OnRewardedCompleted;
            rewardedAd.OnDismissed    -= OnRewardedClosed;
            rewardedAd.OnFailedToShow -= OnRewardedFailedToShow;
            rewardedAd.OnFailedToLoad -= OnRewardedFailedToLoad;
            rewardedAd.Destroy();
        }

        if (appOpenAd != null)
        {
            appOpenAd.OnFailedToLoad -= OnAppOpenFailedToLoad;
            appOpenAd.Destroy();
        }
    }

    private void OnInterstitialClosed()
    {
        interstitialAd.LoadAd();

        Action callback = onInterstitialClosed;
        onInterstitialClosed = null;
        callback?.Invoke();
    }

    private void OnRewardedCompleted(WortiseReward reward)
    {
        if (reward.success)
        {
            pendingReward?.Invoke();
        }

        pendingReward = null;
    }

    private void OnRewardedClosed()
    {
        rewardedAd.LoadAd();

        Action callback = onRewardedClosed;
        onRewardedClosed = null;
        callback?.Invoke();
    }

    // 1.8.1 signatures. With WortiseAdError.cs installed, every ...FailedToLoad and
    // ...FailedToShow handler takes a WortiseAdError parameter instead.
    private void OnInterstitialFailedToShow()
    {
        OnInterstitialClosed();
    }

    private void OnRewardedFailedToShow()
    {
        OnRewardedClosed();
    }

    private void OnInterstitialFailedToLoad()
    {
        StartCoroutine(LoadLater(interstitialAd.CooldownRemainingMs, interstitialAd.LoadAd));
    }

    private void OnRewardedFailedToLoad()
    {
        StartCoroutine(LoadLater(rewardedAd.CooldownRemainingMs, rewardedAd.LoadAd));
    }

    private void OnAppOpenFailedToLoad()
    {
        StartCoroutine(LoadLater(appOpenAd.CooldownRemainingMs, appOpenAd.LoadAd));
    }

    // After a failure, LoadAd() is ignored during the cooldown: wait for it, at least 30 s
    private IEnumerator LoadLater(long cooldownRemainingMs, Action loadAd)
    {
        yield return new WaitForSecondsRealtime(Mathf.Max(cooldownRemainingMs / 1000f, 30f));

        loadAd();
    }

    private void ShowingFullscreenAd()
    {
#if UNITY_ANDROID
        returningFromAd = true;
#endif
    }
}
```

The caller decides what "pause" and "resume" mean, and never waits for an ad that was not shown:

```csharp
if (!gameAds.ShowInterstitial(ResumeGame))
{
    ContinueGame();
}
```

Rules:

- `ShowAd()` with no ad available does nothing and raises no event: check `IsAvailable` first.
  Pause the game only when `ShowAd()` was called, and resume it in both `OnDismissed` and
  `OnFailedToShow`.
- Never call `ShowAd()` from `OnLoaded`: with the reload in `OnDismissed`, the game would show
  ads one after another without any user action.
- `LoadAd()` on an ad that is already loaded discards it. Load once, and again after it closes.
- After a failed load the ad enters a cooldown (30 seconds by default) in which `LoadAd()` is
  ignored without any event. Retry later, as above; never call `LoadAd()` directly from
  `OnFailedToLoad`. `IsInCooldown` and `CooldownRemainingMs` exist on the three formats.
- Grant the reward only in `OnCompleted`, and only when `reward.success` is `true`; never in
  `OnDismissed` or `OnShown`. Define the reward in the game: `reward.amount` and `reward.label`
  may be empty.
- Events: interstitial and app open have `OnClicked`, `OnDismissed`, `OnFailedToLoad`,
  `OnFailedToShow`, `OnImpression`, `OnLoaded`, `OnRevenuePaid` and `OnShown`; rewarded adds
  `OnCompleted`.

## App open

There is no app open manager in Unity: the `OnApplicationPause(false)` handler above shows the ad
when the user comes back to the game, with `AutoReload` loading the next one after each ad
closes. On Android, `OnApplicationPause` also runs when any fullscreen ad closes, so the flag set
before every `ShowAd()` skips the app open ad that would follow it. The flag is only needed on
Android.
Do not also show the app open ad from `OnApplicationFocus`, at launch or from `OnLoaded`.
