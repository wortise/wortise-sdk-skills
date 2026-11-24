using System;
using System.Collections;
using UnityEngine;

// Lives in the boot scene and survives scene changes
public class AdsManager : MonoBehaviour
{
    public static AdsManager Instance { get; private set; }

#if UNITY_ANDROID
    private const string AppKey = "5a1f3c2e-9d47-4b8a-a6e1-0c3d7f2b9e54";
    public const string BannerId = "3f0c2a1e-7b4d-4c1a-9e2f-5d8b6a0c4e11";
    private const string InterstitialId = "9b1e4d2c-3a5f-4e7b-8c6d-1f2a3b4c5d6e";
    private const string RewardedId = "c47a8e15-2f9b-4d63-a1e0-5b7c3d9f2e86";
    private const string AppOpenId = "6d2b9f41-0c7e-4a35-b8d2-93e1f5a7c604";
#else
    private const string AppKey = "e08b6c3d-71a4-4f29-9b5e-2d6f8a1c4e37";
    public const string BannerId = "81d4e6a2-5c3b-4f70-9e1d-a62b8c4f0d93";
    private const string InterstitialId = "2e9f7b1c-4d6a-4853-b0c2-7f1e3a5d9b48";
    private const string RewardedId = "f5c3a9d7-8e2b-41f6-a3d0-6b4e1c7f2a95";
    private const string AppOpenId = "0a7e2c94-6b1d-4f38-8c5a-d3e9f1b7a260";
#endif

    public bool AdsReady { get; private set; }

    private WortiseAppOpen appOpen;
    private WortiseInterstitial interstitial;
    private WortiseRewarded rewarded;

    private Action onInterstitialClosed;
    private Action onRewardedClosed;
    private Action pendingReward;

    private void Awake()
    {
        if (Instance != null)
        {
            Destroy(gameObject);
            return;
        }

        Instance = this;
        DontDestroyOnLoad(gameObject);
    }

    private void Start()
    {
        WortiseAdSettings.IsTestEnabled = Debug.isDebugBuild;

        WortiseSdk.OnInitialized += OnSdkInitialized;
        WortiseSdk.Initialize(AppKey);
    }

    private void OnDestroy()
    {
        WortiseSdk.OnInitialized -= OnSdkInitialized;
        interstitial?.Destroy();
        rewarded?.Destroy();
        appOpen?.Destroy();
    }

    private void OnSdkInitialized()
    {
        WortiseConsentManager.RequestIfRequired(shown => LoadAds());
    }

    private void LoadAds()
    {
        interstitial = new WortiseInterstitial(InterstitialId);
        interstitial.OnDismissed += CloseInterstitial;
        interstitial.OnFailedToShow += CloseInterstitial;
        interstitial.OnFailedToLoad += () => StartCoroutine(Retry(interstitial.CooldownRemainingMs, interstitial.LoadAd));
        interstitial.LoadAd();

        rewarded = new WortiseRewarded(RewardedId);
        rewarded.OnCompleted += reward =>
        {
            if (reward.success)
            {
                pendingReward?.Invoke();
            }
            pendingReward = null;
        };
        rewarded.OnDismissed += CloseRewarded;
        rewarded.OnFailedToShow += CloseRewarded;
        rewarded.OnFailedToLoad += () => StartCoroutine(Retry(rewarded.CooldownRemainingMs, rewarded.LoadAd));
        rewarded.LoadAd();

        appOpen = new WortiseAppOpen(AppOpenId);
        appOpen.AutoReload = true;
        appOpen.LoadAd();

        AdsReady = true;
    }

    // Returns false when no ad was shown
    public bool ShowInterstitial(Action onClosed)
    {
        if (interstitial == null || !interstitial.IsAvailable)
        {
            return false;
        }

        onInterstitialClosed = onClosed;
        interstitial.ShowAd();
        return true;
    }

    public bool ShowRewarded(Action grantReward, Action onClosed)
    {
        if (rewarded == null || !rewarded.IsAvailable)
        {
            return false;
        }

        pendingReward = grantReward;
        onRewardedClosed = onClosed;
        rewarded.ShowAd();
        return true;
    }

    private void OnApplicationPause(bool paused)
    {
        if (!paused)
        {
            appOpen?.TryToShowAd();
        }
    }

    private void OnApplicationFocus(bool focused)
    {
        if (focused)
        {
            appOpen?.TryToShowAd();
        }
    }

    private void CloseInterstitial()
    {
        interstitial.LoadAd();
        onInterstitialClosed?.Invoke();
        onInterstitialClosed = null;
    }

    private void CloseRewarded()
    {
        rewarded.LoadAd();
        onRewardedClosed?.Invoke();
        onRewardedClosed = null;
    }

    private IEnumerator Retry(long cooldownRemainingMs, Action loadAd)
    {
        yield return new WaitForSeconds(Mathf.Max(cooldownRemainingMs / 1000f, 30f));
        loadAd();
    }
}
