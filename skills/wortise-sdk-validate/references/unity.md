# Unity: checks

Search with `grep -rn` over `Assets/` (skip `Assets/Wortise` and `Assets/ExternalDependencyManager`,
which are the package's own code), `ProjectSettings/` and `Packages/`. The SDK's own behaviour
behind each check is documented in <https://docs.wortise.com/unity-sdk/best-practices.md>, which
ends with a pre-release checklist.

The installed version is `<unityversion>` in `Assets/Wortise/Editor/WortiseDependencies.xml`.
Check whether `Assets/Wortise/Scripts/WortiseAdError.cs` exists before judging failure handlers
(L10).

## Setup

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| S1 | The Wortise package is imported once, with a stable version, and EDM4U exists once | `grep -n unityversion Assets/Wortise/Editor/WortiseDependencies.xml`; `find Assets -name "Google.JarResolver*.dll"` and `grep -n external-dependency-manager Packages/manifest.json` | Blocker if missing or duplicated | Import the package; keep a single EDM4U |
| S2 | Unity 6000.0+ and Android Minimum API Level 24+ | `cat ProjectSettings/ProjectVersion.txt`; `grep -n -e AndroidMinSdkVersion -e AndroidTargetSdkVersion ProjectSettings/ProjectSettings.asset` | Blocker (the build fails) | Upgrade Unity, raise the level (ask the user first) |
| S3 | The three custom Gradle templates are enabled and resolved: `mainTemplate.gradle` has `com.wortise:android-sdk` with the version of `WortiseDependencies.xml`, and `settingsTemplate.gradle` has the Wortise repositories | `grep -n -e useCustomMainGradleTemplate -e useCustomGradlePropertiesTemplate -e useCustomGradleSettingsTemplate ProjectSettings/ProjectSettings.asset`; `grep -rn -e com.wortise -e maven.wortise.com Assets/Plugins/Android/` | Blocker (Android) | Enable the templates and run the Android Resolver (see the `wortise-sdk-integrate` skill) |
| S4 | `Assets/Resources/WortiseSettings.asset` has the Google App ID of each platform the app ships on, from the Wortise dashboard: not empty, not a placeholder, not Google's sample IDs (`ca-app-pub-3940256099942544~...`) | `grep -n -e googleAndroidAppId -e googleIosAppId Assets/Resources/WortiseSettings.asset` | Blocker (crash at launch) | Set them in **Wortise > Settings** before building |
| S5 | No Google Mobile Ads Unity plugin, mediation adapters or forced `play-services-ads` versions added for Wortise | `grep -rln -e GoogleMobileAds -e play-services-ads -e com.google.ads.mediation Assets Packages/manifest.json --include=*.xml --include=*.gradle --include=*.json` | Blocker if a lower version is forced, otherwise note | Remove what was added for Wortise; ask about the rest |

## Initialization

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| I1 | `WortiseSdk.Initialize` is called once, from a persistent object of the first scene, with the app key | `grep -rn "WortiseSdk.Initialize" Assets --include=*.cs` | Blocker if missing; should fix if called per scene | One call in a `DontDestroyOnLoad` object |
| I2 | `OnInitialized` is subscribed before `Initialize`, and no ad is loaded before it (and the consent callback) has run | `grep -rn -e OnInitialized -e "LoadAd(" Assets --include=*.cs` and compare | Blocker | Load the ads in the consent callback |
| I3 | Objects that subscribe to the static `WortiseSdk.OnInitialized` remove the handler (`-=`) in `OnDestroy` | `grep -rn "OnInitialized" Assets --include=*.cs` | Should fix | Unsubscribe in `OnDestroy` |
| I4 | The SDK is called only from the main thread | `grep -rn -e "Task.Run" -e "new Thread" -e ThreadPool -e ConfigureAwait Assets --include=*.cs`, then look for Wortise calls inside | Should fix | Move the calls to the main thread |
| I5 | The game flow does not wait for SDK events: no scene load, menu or start button gated on `OnInitialized`, the consent callback or `OnLoaded` | Read those handlers | Blocker (the game hangs in the Editor and whenever the event does not come) | Gate only the ad loading on them |

## Consent and privacy

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| C1 | `WortiseConsentManager.RequestIfRequired` runs from `OnInitialized` before the first ad load, or the app has an IAB TCF consent platform | `grep -rn -e "WortiseConsentManager." -e IABTCF Assets --include=*.cs` | Blocker | Request consent and load the ads in its callback |
| C2 | `WortiseConsentManager.Request` (always shows the dialog) runs only from a user action | `grep -rn "WortiseConsentManager.Request(" Assets --include=*.cs` | Should fix | Use `RequestIfRequired` at startup |
| C3 | The app offers a "Privacy choices" option that calls `WortiseConsentManager.Request` | Same search, then find the UI that calls it | Should fix | Add it to the settings or menu |
| C4 | Apps for children follow the Privacy page completely (`IsChildDirected` and `MaxAdContentRating` before `Initialize`, and the rest of the page) | `grep -rn -e IsChildDirected -e MaxAdContentRating Assets --include=*.cs` | Blocker | Follow <https://docs.wortise.com/unity-sdk/privacy.md> |

## Test mode

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| T1 | `WortiseAdSettings.IsTestEnabled` is never `true` in a release build: tied to `Debug.isDebugBuild` (or `DEVELOPMENT_BUILD`), and set on every launch before loading ads | `grep -rn IsTestEnabled Assets --include=*.cs` and read the condition. No log line shows the test mode, so this check is code-only | Blocker | `WortiseAdSettings.IsTestEnabled = Debug.isDebugBuild;` |
| T2 | No test ad unit (`test-banner`, `test-interstitial`, `test-rewarded`, `test-app-open`, or other `test-` IDs) outside `#if UNITY_EDITOR ... DEVELOPMENT_BUILD` or a `Debug.isDebugBuild` branch | `grep -rn "\"test-" Assets --include=*.cs` and check the condition around each hit | Blocker | Per-build values (see the `wortise-sdk-integrate` skill) |

Release builds must be made without **Development Build**, or both checks pass in code and still
ship test ads: ask the user how they build releases.

## Ad lifecycle

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| L1 | No banner `LoadAd()` in its `OnFailedToLoad`, in `Update` or on a timer (`InvokeRepeating`, coroutines) | `grep -rn -e "OnFailedToLoad" -e InvokeRepeating -e "void Update" Assets --include=*.cs` near banners | Should fix | Remove it: the banner retries and refreshes by itself |
| L2 | No immediate `LoadAd()` in the `OnFailedToLoad` of interstitial, rewarded and app open ads; and each of them has a failure handler that retries later (without one, it stays dead after one failure) | `grep -rn -A8 "OnFailedToLoad" Assets --include=*.cs`, and compare with the ads created | Should fix | Retry after `CooldownRemainingMs`, at least 30 s (coroutine or `Invoke`) |
| L3 | `LoadAd()` is not called on every scene load, `OnEnable` or `Update`, or on an ad that is already loaded | `grep -rn -B3 -A3 "LoadAd(" Assets --include=*.cs` | Should fix | Load once, and again after the ad closes |
| L4 | Every `ShowAd()` is preceded by an `IsAvailable` check, and the game continues when it is false | `grep -rn -B4 "ShowAd()" Assets --include=*.cs` | Should fix | Check `IsAvailable`, continue without the ad |
| L5 | The game pauses only after `ShowAd()` was called, and resumes in both `OnDismissed` and `OnFailedToShow` | Read the handlers | Should fix (Blocker if the game can stay frozen) | Handle both |
| L6 | The reward is granted only in `OnCompleted`, and only when `reward.success` is true | `grep -rn -A6 -e OnCompleted -e OnDismissed -e OnShown Assets --include=*.cs` | Blocker | Move it to `OnCompleted` |
| L7 | Every `WortiseBanner`, `WortiseInterstitial`, `WortiseRewarded` and `WortiseAppOpen` is destroyed in the `OnDestroy` of its owner, and its handlers removed | Count `new Wortise` against `.Destroy()` | Should fix | `Destroy()` and `-=` in `OnDestroy` |
| L8 | A banner is not created again on every scene load without destroying the previous one (banners survive scene changes and stack) | Find where `new WortiseBanner(` runs | Should fix | Destroy it first, or keep one banner in a persistent object |
| L9 | No ad is created in `Update`, `OnGUI` or other per-frame code | `grep -rn -B10 "new Wortise" Assets --include=*.cs` | Blocker | Create it once |
| L10 | Failure handlers match the installed version: argument-less without `WortiseAdError.cs`, one `WortiseAdError` parameter with it | `ls Assets/Wortise/Scripts/WortiseAdError.cs`; `grep -rn -e "OnFailedToLoad +=" -e "OnFailedToShow +=" Assets --include=*.cs` | Blocker (does not compile) | Match the signature |
| L11 | `AutoRefreshTime` is in milliseconds (30000 to 120000) | `grep -rn AutoRefreshTime Assets --include=*.cs` | Should fix | Use milliseconds |
| L12 | The adaptive banner width is in dp or points, not `Screen.width` (pixels) | `grep -rn -A2 AdaptiveBannerAdSize Assets --include=*.cs` | Should fix | A width in dp/points, such as 320 |
| L13 | Collapsible banners use an anchored adaptive size and the same position as the banner | `grep -rn -B8 Collapsible Assets --include=*.cs` | Should fix | Match them |
| L14 | No fullscreen ad shown at launch, on exit or right after another ad; interstitials are spaced out | Read where `ShowAd()` and `TryToShowAd()` are called | Blocker (policy) / Should fix (spacing) | Show at natural pauses, with a minimum interval |
| L14b | `ShowAd()` is never called from `OnLoaded`: with the reload in `OnDismissed`, the game shows ads one after another | `grep -rn -A6 "OnLoaded" Assets --include=*.cs` | Blocker (policy) | Show only from the game's own flow |
| L15 | App open: shown from `OnApplicationPause(false)` with `TryToShowAd()`, skipping the resume that follows another fullscreen ad on Android, and not also from `OnApplicationFocus` | `grep -rn -A12 -e OnApplicationPause -e OnApplicationFocus Assets --include=*.cs` | Should fix | See the `wortise-sdk-ad-formats` skill |
| L16 | Banners do not cover buttons or other controls of the game | Compare the banner position with the UI; on a device, look at it | Should fix | Move the controls or the banner |

## Build

Build with the project's own build script or CI if it has one; otherwise ask the user to build
from the Editor (**File > Build Profiles**). A release build (without Development Build) matters:
test mode and test ad units show up there. Do not start the Editor without the user's agreement.

- Android: a `Could not find com.wortise:android-sdk` error means S3 is not done.
- iOS: the console shows `Wortise: raising the iOS target version from <old> to 15.0` when the
  target was lower: expected. In the generated Xcode project, check
  `/usr/libexec/PlistBuddy -c "Print :GADApplicationIdentifier" <build folder>/Info.plist`.
- `Could not load Wortise settings` in the build log means S4 is not done.

## Running

Only on a device, never in the Editor. With the test ad units every format must log "loaded".

```bash
# Android
adb logcat -c
adb shell setprop log.tag.Wortise DEBUG    # detailed logs; resets when the device restarts
# open the game and the screens with ads, then:
adb logcat -d -s Wortise Unity
```

On iOS, run the app from Xcode and filter the console by `<Wortise>`.

| Log line | Meaning |
|---|---|
| `Wortise SDK - Version <version>` | The SDK started (Android) |
| `<Format> ad loaded for ad unit <id>` | The ad loaded |
| `<Format> ad failed to load for ad unit <id>: <error>` | It failed: see the `wortise-sdk-troubleshoot` skill |
| `showAd() called with no ad available` (`showAd(from:)` on iOS) | A show without `IsAvailable` (L4) |
| `... is in cooldown, skipping request` | A retry during the cooldown (L2) |

`Adapter not found: com.wortise.ads.mediation...` warnings at startup on Android are expected:
they list ad networks the app does not include.
