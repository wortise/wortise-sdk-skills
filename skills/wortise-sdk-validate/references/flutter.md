# Flutter: checks

Search with `grep -rn` over `lib/`, `pubspec.yaml`, `android/` and `ios/` (skip `build/`, `.dart_tool/`
and `ios/Pods/`). The SDK's own behaviour behind each check is documented in
<https://docs.wortise.com/flutter-sdk/best-practices.md>.

## Setup

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| S1 | `wortise` is in `pubspec.yaml` with a stable version (not a `-beta` git ref unless the user wants it), and the locked version matches | `grep -n -A3 wortise pubspec.yaml pubspec.lock` | Blocker if missing | Use the version from the integration page |
| S2 | No Google Mobile Ads, mediation adapters or ad network SDKs added for Wortise (Gradle, Podfile, Swift packages), and no forced `play-services-ads` version | `grep -rn -e play-services-ads -e com.google.ads.mediation -e Google-Mobile-Ads-SDK -e resolutionStrategy android/ ios/Podfile` | Blocker if forced lower, otherwise note | Remove them: the SDK brings them |
| S3 | If `android/settings.gradle(.kts)` has `dependencyResolutionManagement` with `FAIL_ON_PROJECT_REPOS` or `PREFER_SETTINGS`, it declares the Wortise, Pangle, BidMachine and Chartboost repositories | `grep -rn -e RepositoriesMode -e maven.wortise.com android/settings.gradle*` | Blocker (build fails) | Add the repositories (see the `wortise-sdk-integrate` skill) |
| S4 | `com.google.android.gms.ads.APPLICATION_ID` in `android/app/src/main/AndroidManifest.xml` and `GADApplicationIdentifier` in `ios/Runner/Info.plist`, with the app's Google App IDs, not placeholders nor Google's sample ID (`ca-app-pub-3940256099942544~...`) | `grep -rn -A1 -e gms.ads.APPLICATION_ID -e GADApplicationIdentifier android/app/src ios/Runner` | Blocker | Use the Google App IDs from the Wortise dashboard |
| S5 | `Info.plist` has the complete `SKAdNetworkItems` list of the integration page | `grep -c skadnetwork ios/Runner/Info.plist`, compare with the page | Should fix | Copy the list |
| S6 | Android Gradle Plugin 8.9.1+, Gradle 8.11.1+, `compileSdk` 36+, Kotlin 2.2+, `minSdk` 24+; iOS deployment target 15.0+ (and `platform :ios, '15.0'` if there is a `Podfile`) | `android/settings.gradle*`, `android/app/build.gradle*`, wrapper, `ios/Podfile`, `IPHONEOS_DEPLOYMENT_TARGET` in `ios/Runner.xcodeproj/project.pbxproj` | Blocker (the build fails) | Raise them; ask before raising `minSdk` or the iOS target |

## Initialization

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| I1 | `WortiseSdk.initialize` is called once, at startup, and awaited before any ad is created | `grep -rn -e "WortiseSdk.initialize" -e "WortiseSdk.wait" lib/` | Blocker if missing; should fix if ads can start first | Initialize from the root widget's `State` |
| I2 | No ad object and no `BannerAd` widget exists before the initialization completes (for example, a banner in the first screen's `build()` while the SDK initializes in `initState`) | Compare where `BannerAd(`, `InterstitialAd(`, `RewardedAd(`, `AppOpenAd(` and `NativeAdLoader(` appear with where `initialize` completes | Blocker | Gate them behind a "ready" flag set after initialization and consent |
| I3 | `initialize` is not awaited in `main()` before `runApp` | `grep -rn -B3 -A6 "void main" lib/` | Note | Move it to the root widget |
| I4 | Only `package:wortise/wortise.dart` is imported | `grep -rn "package:wortise/" lib/` | Should fix | Import the library, never `src/` |

## Consent and privacy

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| C1 | `ConsentManager.requestIfRequired()` is awaited before the first ad load, or the app has an IAB TCF consent platform | `grep -rn -e "ConsentManager." -e IABTCF -e user_messaging_platform lib/ pubspec.yaml` | Blocker | Request consent after `initialize` and load the ads after it |
| C2 | `ConsentManager.request()` (always shows the dialog) runs only from a user action | `grep -rn "ConsentManager.request()" lib/` | Should fix | Use `requestIfRequired` at startup |
| C3 | The app offers a "Privacy choices" option calling `ConsentManager.request()` | Same search | Should fix | Add it to the settings or menu |
| C4 | Apps for children: the setup of <https://docs.wortise.com/flutter-sdk/privacy.md> is complete (`AdSettings.setChildDirected` and `setMaxAdContentRating` before `initialize`, and the Android build changes) | `grep -rn -e setChildDirected -e setMaxAdContentRating lib/` and `grep -rn exclude android/app/build.gradle*` | Blocker | Follow the Privacy page; ask the user about the audience |
| C5 | The result of `requestIfRequired` is not used as the user's choice | Read what the code does with the returned `bool` | Should fix | Use `canRequestPersonalizedAds` / `canCollectData` |

## Test mode

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| T1 | `AdSettings.setTestEnabled` is never `true` in a release build | `grep -rn setTestEnabled lib/` and read the argument (`kDebugMode` is right). No log line shows the test mode, so this check is code-only | Blocker | `setTestEnabled(kDebugMode)` |
| T2 | No test ad unit reaches a release build (`test-banner`, `test-interstitial`, `test-rewarded`, `test-app-open`, `test-native`, or other `test-` IDs) | `grep -rn "'test-" lib/` and `grep -rn '"test-' lib/`, then check each is under `kDebugMode` or another debug-only switch | Blocker | Per-build values (see the `wortise-sdk-integrate` skill) |

## Ad lifecycle

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| L1 | No ad object is created in `build()` | `grep -rn -A30 "Widget build(" lib/` and look for `InterstitialAd(`, `RewardedAd(`, `AppOpenAd(`, `NativeAdLoader(` | Should fix | Create it once in `initState` |
| L2 | One Dart object per ad unit ID for the whole app: no two live objects with the same ID (two screens, list items, or a screen that is replaced by another using the same ID) | `grep -rn -e "InterstitialAd(" -e "RewardedAd(" -e "AppOpenAd(" -e "NativeAdLoader(" lib/` and compare the IDs | Should fix (events lost, ads destroyed) | One shared object per ad unit ID |
| L3 | No immediate `loadAd()` on `FAILED_TO_LOAD` (`FAILED` for native), and every fullscreen or native ad has a failure handler that retries later | `grep -rn -A8 -e FAILED_TO_LOAD -e NativeAdEvent.FAILED lib/` | Should fix | Retry after `cooldownRemainingMs` (at least 30 s) with a `Timer` cancelled in `dispose()` |
| L4 | The result of `showAd()` (or `isAvailable`) is checked and the flow continues when it is `false` | `grep -rn -B4 -A4 -e "showAd()" -e tryToShowAd lib/` | Should fix | `if (!await ad.showAd()) next();` |
| L5 | The app continues in both `DISMISSED` and `FAILED_TO_SHOW`, and reloads after both | Read the listeners | Should fix | Handle both |
| L6 | The reward is granted only in `RewardedAdEvent.COMPLETED` with `args['success'] == true` | `grep -rn -A6 -e RewardedAdEvent.COMPLETED -e RewardedAdEvent.DISMISSED -e RewardedAdEvent.SHOWN lib/` | Blocker | Move the reward to `COMPLETED` |
| L7 | Every fullscreen ad, `NativeAd` and `NativeAdLoader` is destroyed in the owner's `dispose()` (and replaced `NativeAd`s too) | Count creations against `.destroy()` calls | Should fix | Call `destroy()` in `dispose()` |
| L8 | `loadAd()` is not called in `build()`, `didChangeDependencies()` or on every resume, nor on an ad that is already loaded | `grep -rn -B3 "loadAd(" lib/` | Should fix | Load once, and again after the ad is dismissed |
| L9 | `showAd()` is never called from a `LOADED` event | `grep -rn -A6 "LOADED" lib/` and look for `showAd` | Blocker (ad network policy) | Show only from the app's own flow |
| L10 | No fullscreen ad shown at launch, on exit or right after another ad, and interstitials are spaced out | Read where `showAd` is called | Blocker (at launch, on exit) / should fix (spacing) | Show at natural pauses, with a minimum interval |
| L11 | Every `BannerAd` and `AdWidget` has a bounded height (critical with `MATCH_VIEW` and adaptive sizes) and sits inside a `SafeArea` or equivalent | `grep -rn -B6 -e "BannerAd(" -e "AdWidget(" lib/` | Should fix (layout error, ad under the system bars) | Wrap it in a `SizedBox`/`ConstrainedBox`; add a `SafeArea` |
| L12 | Banners are not recreated: no changing `key`, no reload by rebuilding from `FAILED_TO_LOAD`, `keepAlive: true` inside scrolling lists, and props are not expected to change a live banner | Read the banner's parents and listener | Should fix | Keep one stable `BannerAd` |
| L13 | `autoRefreshTime`, if set, is in milliseconds between `30000` and `120000` (or `BannerAd.AUTO_REFRESH_DISABLED`) | `grep -rn autoRefreshTime lib/` | Should fix | Use milliseconds |
| L14 | Collapsible banners use `AdSize.getAnchoredAdaptiveBannerAdSize` and a position that matches the layout | `grep -rn -B6 "collapsible:" lib/` | Should fix | Use the anchored adaptive size |
| L15 | App open: `AppOpenManager.register` (not the plain constructor) with `autoReload: true` and a loaded ad, registered once, and not combined with manual shows of the same ad unit | `grep -rn -e AppOpenManager -e "AppOpenAd(" lib/` | Should fix | Keep one of the two ways |
| L16 | Native: factories registered on Android (`NativeAdManager.registerAdViewFactory`) and iOS (`WortiseNativeAdManager.registerAdViewFactory`) with the ID passed to `NativeAdLoader` | `grep -rn registerAdViewFactory android/app/src ios/Runner` and `grep -rn "NativeAdLoader(" lib/` | Blocker (no native ads) | Register both, same ID |
| L17 | Native layouts show an "Ad" label and a media view of at least 120x120 dp/pt | Read the Android layout and the iOS XIB | Note (video) / Blocker (no label) | Enlarge the view; add the label |
| L18 | `switch` statements over Wortise event enums have a `default:` | `grep -rn -A20 "switch (event)" lib/` | Note | Add `default: break;` |

## Build

```bash
flutter analyze
flutter build apk --debug
flutter build apk --release    # or appbundle; may need the release signing config
flutter build ios --release --no-codesign
```

No ProGuard or R8 rules are needed for the SDK. A minified release build prints R8 warnings
that come from the SDK and its ad networks (Kotlin metadata, missing `billingclient` or
`bytedance` classes): do not add rules for them. Any R8 error, or a crash in the release build,
is a real problem.

## Running

The native SDKs log outside Dart: `flutter run` may not show them.

Android:

```bash
adb logcat -c
adb shell setprop log.tag.Wortise DEBUG    # detailed logs; resets when the device restarts
# open the app and the screens with ads, then:
adb logcat -d -s Wortise
```

iOS: Xcode console, or the macOS Console app filtered by subsystem `com.wortise.ads`; lines start
with `<Wortise>`.

| Log line | Meaning |
|---|---|
| `Wortise SDK - Version <version>` (Android only) | The SDK started |
| `<Format> ad loaded for ad unit <id>` | The ad loaded |
| `<Format> ad failed to load for ad unit <id>: <error>` | It failed: see the `wortise-sdk-troubleshoot` skill |
| `... is in cooldown, skipping request` (debug level) | A retry during the cooldown (L3) |

With the test ad units every format must log "loaded". On Android, `Adapter not found:
com.wortise.ads.mediation...` warnings at startup are expected: they list ad networks the app does
not include.
