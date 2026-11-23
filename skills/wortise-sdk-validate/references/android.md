# Android: checks

Search with `grep -rn` (or the IDE) over the app's `src/` folders and Gradle files. The SDK's own
behaviour behind each check is documented in
<https://docs.wortise.com/android-sdk/best-practices.md>.

## Setup

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| S1 | Exactly one of `com.wortise:android-sdk` and `com.wortise:android-sdk-families`, with a stable version (no `-alpha`, `-beta` or `-rc` unless the user wants it) | `grep -rn "com.wortise" --include=*.gradle* --include=*.toml .` | Blocker if both or none | Keep the right one for the audience |
| S2 | The Wortise Maven repositories are declared (three for `android-sdk`, the first for `android-sdk-families`) | `grep -rn -e maven.wortise.com -e bytedance -e bidmachine --include=*.gradle* .` | Blocker | Add them where the project declares repositories |
| S3 | No version of `play-services-ads` is forced (`strictly`, `force`, `resolutionStrategy`), and Google mediation adapters are not added by hand | `grep -rn -e play-services-ads -e com.google.ads.mediation -e resolutionStrategy -e strictly --include=*.gradle* --include=*.toml .` | Blocker if forced lower, otherwise note | Remove the forced version or the duplicate adapter |
| S4 | The merged manifest has `com.google.android.gms.ads.APPLICATION_ID` with the app's Google App ID, not a placeholder and not Google's sample ID (`ca-app-pub-3940256099942544~...`) | Build, then `grep -rn "gms.ads.APPLICATION_ID" -A1 app/build/intermediates/merged_manifest*` | Blocker | Use the Google App ID from the Wortise dashboard |
| S5 | Android Gradle Plugin 8.9.1+, `compileSdk` 36+, Kotlin 2.2+, `minSdk` 24+ | Gradle files, `libs.versions.toml`, wrapper | Blocker (the build fails) | Raise them; ask before raising `minSdk` |

## Initialization

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| I1 | `WortiseSdk.initialize` is called once, in `Application.onCreate`, and that class is registered with `android:name` | `grep -rn "WortiseSdk.initialize" src/`; check the manifest | Blocker if missing; should fix if it runs after the first ad request | Move it to `Application.onCreate` |
| I2 | No ad is created before the initialization runs (for example, in an `Activity` whose layout has a banner while the SDK is initialized elsewhere later) | Compare where ads are created with where `initialize` runs | Blocker | Initialize in `Application.onCreate` |
| I3 | No ads in components with `android:process` | `grep -n "android:process" src/main/AndroidManifest.xml` | Should fix | Load ads only in the main process |

## Consent and privacy

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| C1 | `ConsentManager.requestIfRequired` runs from an `Activity` before the first ad load, or the app has an IAB TCF consent platform | `grep -rn -e "ConsentManager." -e IABTCF -e UserMessagingPlatform src/` | Blocker | Request consent and load the ads in its listener |
| C2 | `ConsentManager.request` (always shows the dialog) runs only from a user action, such as a settings option | `grep -rn "ConsentManager.request(" src/` | Should fix | Use `requestIfRequired` at startup |
| C3 | Apps for children: `android-sdk-families`, `AdSettings.setChildDirected` and `AdSettings.setMaxAdContentRating` before `initialize`, and the Families checklist complete | `grep -rn -e setChildDirected -e setMaxAdContentRating src/` and S1 | Blocker | Follow <https://docs.wortise.com/android-sdk/families.md> |

## Test mode

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| T1 | `AdSettings.testEnabled` is never `true` in a release build | `grep -rn -e testEnabled -e setTestEnabled src/` and read the condition. No log line shows the test mode, so this check is code-only | Blocker | Tie it to the debug build |
| T2 | No test ad unit reaches a release build (`test-banner`, `test-interstitial`, `test-rewarded`, `test-app-open`, `test-native`, or other `test-` IDs) | `grep -rn "test-" src/ --include=*.kt --include=*.java --include=*.xml` (excluding `src/debug`) and `grep -rn "test-" --include=*.gradle* .`, then check which build type each value belongs to | Blocker | Per-build-type values (see the `wortise-sdk-integrate` skill) |

## Ad lifecycle

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| L1 | No `loadAd()` inside a banner's `onBannerFailedToLoad` | `grep -rn -A8 "onBannerFailedToLoad" src/` | Should fix | Remove it: the banner retries by itself |
| L2 | No immediate `loadAd()` inside `onInterstitialFailedToLoad`, `onRewardedFailedToLoad`, `onAppOpenFailedToLoad` or `onNativeFailedToLoad`; and every fullscreen or native ad has a failure handler that retries later (without one, the ad stays dead after a single failure) | `grep -rn -A8 "FailedToLoad" src/`, and compare with the ads created | Should fix | Retry after `cooldownRemainingMs` (at least 30 s) |
| L3 | `loadAd()` is not called on every `onResume`/`onStart`, or on an ad that is already loaded | `grep -rn -B3 -A3 "loadAd()" src/` | Should fix | Load once, and again after the ad is dismissed |
| L4 | Every `showAd(` is preceded by an `isAvailable` check, and the flow continues when it is false | `grep -rn -B4 "showAd(" src/` | Should fix | Check `isAvailable`, continue without the ad |
| L5 | The app continues in both `on...Dismissed` and `on...FailedToShow` | Read the listeners | Should fix | Handle both |
| L6 | The reward is granted only in `onRewardedCompleted`, and only when `reward.success` is true | `grep -rn -A6 -e onRewardedCompleted -e onRewardedDismissed -e onRewardedShown src/` | Blocker | Move the reward to `onRewardedCompleted` |
| L7 | Every `BannerAd`, fullscreen ad, `NativeAd` and `NativeAdLoader` is destroyed with its screen | Count creations (`BannerAd(`, `InterstitialAd(`, `RewardedAd(`, `AppOpenAd(`, `NativeAdLoader(`) against `.destroy()` calls | Should fix | Call `destroy()` in `onDestroy`/`onDispose` |
| L8 | A banner declared in XML with `app:adUnitId` is not loaded again in code | `grep -rln "app:adUnitId" src/main/res` and `grep -rn "loadAd()" src/` for the same banner | Should fix | Remove the extra `loadAd()` |
| L9 | Compose: the banner is created with `remember` and loaded in `factory`, not in `update` | `grep -rn -A12 "AndroidView" src/` | Should fix | Move `loadAd()` to `factory` |
| L10 | `adUnitId` is not reassigned on a banner that already has one (list adapters reusing views) | `grep -rn "adUnitId =" src/` in adapters and view holders | Blocker (crash) | One banner per ad unit, no recycling |
| L11 | No ad created with an `Activity` is stored in a static field, singleton or `companion object` | `grep -rn -A6 -e "companion object" -e "^object " src/` and look for ad-typed properties, then find where they are assigned | Should fix | A normal property destroyed in `onDestroy`, or shared ads created with the `Application` context |
| L12 | `AppOpenManager` and a manually shown `AppOpenAd` are not used together | `grep -rn -e AppOpenManager -e "AppOpenAd(" src/` | Should fix | Keep one of the two |
| L13 | Native media views measure at least 120x120 dp, and the native layout shows an "Ad" label | Read the native layout | Note (video) / Blocker (no label) | Enlarge the view; add the label |
| L14 | No fullscreen ad shown at launch, on exit or right after another ad | Read where `showAd` is called | Blocker (ad network policy) | Show it at a natural pause, after a user action |
| L14b | `showAd` is never called from an `on...Loaded` callback: combined with the reload in `on...Dismissed`, it shows ads one after another without any user action | `grep -rn -A6 "Loaded(" src/` and look for `showAd` | Blocker (ad network policy) | Show only from the app's own flow, checking `isAvailable` |
| L14c | Interstitials are spaced out (see the `wortise-sdk-ad-formats` skill) | Read where `showAd` is called | Should fix | Add a minimum interval |
| L15 | Banners do not sit under the system bars: with `targetSdk` 35+ the app is drawn edge to edge on Android 15+ | Check the banner's container for `fitsSystemWindows` or an insets listener; on a device, look at the bottom of the screen | Should fix | Handle the window insets |
| L16 | A banner declared in XML with `app:adUnitId` is not on the screen that requests consent (it would load before consent) | Compare the layout of the consent screen with S/C1 | Should fix | Create that banner in code after consent |
| L17 | The app offers a way to change the consent choice later (a "Privacy choices" option calling `ConsentManager.request`) when it uses `ConsentManager` | `grep -rn "ConsentManager.request(" src/` | Should fix | Add the option to the settings or menu |

## Build

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease   # also checks minification; may need the release signing config
```

No ProGuard or R8 rules are needed for the SDK. A minified release build prints R8 warnings
that come from the SDK and its bundled ad networks, such as `An error occurred when parsing
kotlin metadata`, `Missing class com.android.billingclient...` or `Missing class
com.bytedance.sdk...`, and companion-object warnings for obfuscated `com.wortise.ads` classes.
They do not break the build: do not add rules for them. Any R8 error, or a crash in the release
build, is a real problem.

## Running

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell setprop log.tag.Wortise DEBUG    # detailed logs; resets when the device restarts
# open the app and the screens with ads, then:
adb logcat -d -s Wortise
```

| Log line | Meaning |
|---|---|
| `Wortise SDK - Version <version>` | The SDK started |
| `<Format> ad loaded for ad unit <id>` | The ad loaded |
| `<Format> ad failed to load for ad unit <id>: <error>` | It failed: see the `wortise-sdk-troubleshoot` skill |
| `showAd() called with no ad available` | A show without `isAvailable` (L4) |
| `... is in cooldown, skipping request` | A retry during the cooldown (L2) |

With the test ad units every format must log "loaded". `Adapter not found: com.wortise.ads.mediation...`
warnings at startup are expected: they list ad networks the app does not include.
