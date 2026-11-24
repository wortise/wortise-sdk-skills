using UnityEngine;

// On the HUD of every level scene. Levels are loaded with SceneManager.LoadScene.
public class LevelUI : MonoBehaviour
{
    private WortiseBanner banner;

    private void Start()
    {
        if (!AdsManager.Instance.AdsReady)
        {
            return;
        }

        WortiseAdSize size = WortiseAdSize.GetAnchoredAdaptiveBannerAdSize(Screen.width);
        banner = new WortiseBanner(AdsManager.BannerId, size, WortiseAdPosition.Bottom);
        banner.LoadAd();
    }

    public void OnLevelComplete()
    {
        Time.timeScale = 0f;
        if (!AdsManager.Instance.ShowInterstitial(ResumeAndLoadNext))
        {
            ResumeAndLoadNext();
        }
    }

    public void OnPauseButton()
    {
        Time.timeScale = 0f;
    }

    public void OnResumeButton()
    {
        Time.timeScale = 1f;
    }

    private void ResumeAndLoadNext()
    {
        Time.timeScale = 1f;
        // Load the next level scene
    }
}
