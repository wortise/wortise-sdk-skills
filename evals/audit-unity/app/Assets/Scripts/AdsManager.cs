using System.Threading.Tasks;
using UnityEngine;

public class AdsManager : MonoBehaviour
{
    WortiseBanner banner;
    WortiseInterstitial interstitial;
    WortiseRewarded rewarded;
    int gems;

    void Awake()
    {
        banner = new WortiseBanner("3f0c2a1e-7b4d-4c1a-9e2f-5d8b6a0c4e11", WortiseAdSize.Height50, WortiseAdPosition.Bottom);
        banner.AutoRefreshTime = 60;
        banner.LoadAd();
    }

    void Start()
    {
        WortiseAdSettings.IsTestEnabled = true;
        Task.Run(() => WortiseSdk.Initialize("5a1f3c2e-9d47-4b8a-a6e1-0c3d7f2b9e54"));

        interstitial = new WortiseInterstitial("9b1e4d2c-3a5f-4e7b-8c6d-1f2a3b4c5d6e");
        interstitial.OnFailedToLoad += () => interstitial.LoadAd();
        interstitial.OnDismissed += () => Time.timeScale = 1f;
        interstitial.LoadAd();

        rewarded = new WortiseRewarded("test-rewarded");
        rewarded.OnDismissed += () => gems += 10;
        rewarded.LoadAd();
    }

    public void OnLevelEnd()
    {
        Time.timeScale = 0f;
        interstitial.ShowAd();
    }

    public void OnGemsButton()
    {
        rewarded.ShowAd();
    }
}
