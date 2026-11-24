---
type: llm
weight: 3
---

The answer identifies at least 4 of these 5 problems:
 1. `LevelUI` creates a new `WortiseBanner` in every level scene and never destroys it (no `OnDestroy` with `banner.Destroy()`); banners survive scene changes, so they stack up.
 2. The anchored adaptive banner size uses `Screen.width`, which is in pixels; the width must be in dp/points (for example 320), otherwise the banner is wider than the screen.
 3. The app open ad is shown from both `OnApplicationPause(false)` and `OnApplicationFocus(true)`, so one return to the game can trigger it twice; use only `OnApplicationPause`.
 4. The app open ad is not skipped after another fullscreen ad closes (on Android, closing an interstitial or rewarded ad also resumes the app), so an app open ad appears right after the interstitial.
 5. The retry coroutine uses `WaitForSeconds`, which depends on `Time.timeScale`; while the game is paused (`timeScale = 0`) the retry never runs. It should use `WaitForSecondsRealtime`.
 Fail if fewer than 4 are identified correctly.
