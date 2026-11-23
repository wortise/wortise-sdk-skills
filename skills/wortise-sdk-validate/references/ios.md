# iOS: checks

Search with `grep -rn` (or Xcode's Find) over the app's sources, `Info.plist`, storyboards, XIBs,
`Podfile` and the Xcode project. Skip `Pods/`, `DerivedData/` and other generated folders. The
SDK's own behaviour behind each check is documented in
<https://docs.wortise.com/ios-sdk/best-practices.md>, which ends with a pre-release checklist.

## Setup

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| S1 | Wortise comes from one dependency manager only (`pod 'WortiseSDK'` or the `wortise-ios-sdk-spm` package, not both), with a stable version (no `-alpha`, `-beta` or `-rc` unless the user wants it) | `grep -rn -e WortiseSDK -e wortise-ios-sdk-spm --include=Podfile --include=Podfile.lock --include=Package.resolved --include=project.pbxproj .` | Blocker if both or none | Keep the one the project already uses |
| S2 | Google Mobile Ads and Google mediation adapters are not added by hand for Wortise | `grep -rn -e Google-Mobile-Ads-SDK -e GoogleMobileAdsMediation -e swift-package-manager-google-mobile-ads --include=Podfile --include=Package.resolved --include=project.pbxproj .` | Should fix if pinned to a conflicting version, otherwise note | Remove them unless the app uses them directly |
| S3 | The app's `Info.plist` has `GADApplicationIdentifier` with the app's Google App ID (`ca-app-pub-...~...`), not a placeholder and not Google's sample ID (`ca-app-pub-3940256099942544~...`) | `grep -rn -A1 GADApplicationIdentifier --include=*.plist .`; after building, `plutil -p <App>.app/Info.plist` | Blocker (crash at launch) | Use the Google App ID from the Wortise dashboard |
| S4 | `Info.plist` has the `SKAdNetworkItems` list of the integration page, complete | `grep -c SKAdNetworkIdentifier <Info.plist>` and compare with <https://docs.wortise.com/ios-sdk/sdk-integration.md> | Should fix | Copy the missing identifiers from that page |
| S5 | iOS deployment target 15.0+ in the target and the `Podfile`, Xcode 15+ | `grep -rn IPHONEOS_DEPLOYMENT_TARGET --include=project.pbxproj .`; `grep -n "platform :ios" Podfile` | Blocker (the build fails) | Raise it, after asking the user |

## Initialization

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| I1 | `WortiseAds.shared.initialize(assetKey:)` is called once, at launch (`application(_:didFinishLaunchingWithOptions:)`), with the app key | `grep -rn "WortiseAds.shared.initialize" --include=*.swift .` | Blocker if missing; should fix if it runs after the first ad request | Move it to the app delegate |
| I2 | No ad is loaded before the initialization runs (for example, a first screen that loads ads while the SDK is initialized later elsewhere) | Compare where ads load with where `initialize` runs | Blocker | Initialize at launch |
| I3 | The SDK is called on the main thread: no `initialize`, ad creation, `loadAd` or `showAd` inside `DispatchQueue.global`, `Task.detached`, network callbacks or other background queues | `grep -rn -B6 -e "loadAd(" -e "showAd(" -e "WortiseAds.shared" --include=*.swift .` | Should fix | Move the call to the main thread |

## Consent and privacy

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| C1 | `WAConsentManager.shared.request(ifRequired:)` runs from a view controller on screen before the first ad load, and the ads load in its completion; or the app has an IAB TCF consent platform | `grep -rn -e WAConsentManager -e IABTCF -e UserMessagingPlatform --include=*.swift .` | Blocker | Request consent and load the ads in its completion |
| C2 | `WAConsentManager.shared.request(_:)` (always shows the dialog) runs only from a user action, such as a settings option | `grep -rn "WAConsentManager.shared.request(" --include=*.swift .` and look at the calls without `ifRequired:` | Should fix | Use `request(ifRequired:)` at startup |
| C3 | The app offers a way to change the consent choice later (a "Privacy choices" option calling `request(_:)`) when it uses `WAConsentManager` | Same search as C2 | Should fix | Add the option to the settings or menu |
| C4 | Apps for children: `WAAdSettings.childDirected` and `WAAdSettings.maxAdContentRating` set before `initialize`, as the Privacy page describes | `grep -rn -e childDirected -e maxAdContentRating --include=*.swift .` | Blocker | Follow <https://docs.wortise.com/ios-sdk/privacy.md>; ask the user about the audience |

## Test mode

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| T1 | `WAAdSettings.testEnabled = true` only inside `#if DEBUG`, and `DEBUG` is not defined for the Release configuration | `grep -rn -B3 testEnabled --include=*.swift .`; `grep -n SWIFT_ACTIVE_COMPILATION_CONDITIONS *.xcodeproj/project.pbxproj`. No log line shows the test mode, so this check is code-only | Blocker | Wrap it in `#if DEBUG` |
| T2 | No test ad unit reaches a release build (`test-banner`, `test-interstitial`, `test-rewarded`, `test-app-open`, `test-native`, or other `test-` IDs), in code, storyboards or XIBs | `grep -rn "test-" --include=*.swift --include=*.storyboard --include=*.xib --include=*.plist .`, then check that each Swift hit is inside `#if DEBUG` | Blocker | Per-configuration values (see the `wortise-sdk-integrate` skill) |

## Ad lifecycle

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| L1 | No `loadAd()` inside a banner's `didFailToLoad(bannerAd:error:)` | `grep -rn -A8 "didFailToLoad(bannerAd" --include=*.swift .` | Should fix | Remove it: the banner retries by itself |
| L2 | No immediate `loadAd()` inside `didFailToLoad` of interstitial, rewarded, app open or native ads; and every fullscreen or native ad has a failure handler that retries later (without one, the ad stays dead after a single failure) | `grep -rn -A8 "didFailToLoad(" --include=*.swift .`, and compare with the ads created | Should fix | Retry after `cooldownRemaining` (at least 30 s) |
| L3 | `loadAd()` is not called in `viewWillAppear`/`viewDidAppear` on every appearance, or on an ad that is already loaded | `grep -rn -B4 "loadAd(" --include=*.swift .` | Should fix | Load once, and again after the ad is dismissed |
| L4 | Every `showAd(from:` is preceded by an `isAvailable` check, and the flow continues when it is false | `grep -rn -B4 "showAd(" --include=*.swift .` | Should fix | Check `isAvailable`, continue without the ad |
| L5 | The app continues in both `didDismiss` and `didFailToShow` | Read the delegates | Should fix | Handle both |
| L6 | The reward is granted only in `didComplete(rewardedAd:reward:)`, and only when `reward.success` is true | `grep -rn -A6 -e "didComplete(rewardedAd" -e "didDismiss(rewardedAd" -e "didShow(rewardedAd" --include=*.swift .` | Blocker | Move the reward to `didComplete` |
| L7 | Every `WABannerAd`, fullscreen ad, `WANativeAd` and `WANativeAdLoader` is destroyed when its screen closes, banners also get `rootViewController = nil`, and the native loader reference is set to `nil` | Count creations (`WABannerAd(`, `customClass="WABannerAd"` in storyboards and XIBs, `WAInterstitialAd(`, `WARewardedAd(`, `WAAppOpenAd(`, `WANativeAdLoader(`) against `.destroy()` calls | Should fix (memory leak) | Release them as the `wortise-sdk-ad-formats` skill shows |
| L8 | Every banner gets `rootViewController` before `loadAd()` | `grep -rn -e "WABannerAd(" -e rootViewController --include=*.swift .`, plus banner outlets | Blocker (Google demand cannot load) | Set it to the view controller that shows the banner |
| L9 | Every ad's `delegate` is an object that something keeps alive, not one created inline (`ad.delegate = SomeHandler()`): banner, interstitial, rewarded and app open ads hold it weakly | `grep -rn "delegate = " --include=*.swift .` near the ads | Blocker (no events) | Use the view controller or a stored object |
| L10 | Delegate methods match the documented signatures exactly (they are optional, so a typo compiles and is never called) | Read each delegate against the format pages; look for Xcode's "nearly matches optional requirement" warnings | Blocker if it hides a reward or a reload | Fix the signature |
| L11 | Ads are created once per screen: not in `viewWillAppear`, `viewDidAppear`, `viewDidLayoutSubviews`, `layoutSubviews` or cell configuration | `grep -rn -B10 -e "WABannerAd(" -e "WAInterstitialAd(" -e "WARewardedAd(" -e "WANativeAdLoader(" --include=*.swift .` | Should fix | Create them in `viewDidLoad` or after consent |
| L12 | `WAAppOpenManager` and a manually shown `WAAppOpenAd` are not used together; the manager's `loadAd()` runs once, after the consent (and the App Tracking Transparency prompt, if any) | `grep -rn -e WAAppOpenManager -e "WAAppOpenAd(" --include=*.swift .` | Should fix | Keep one of the two; start the manager after the prompts |
| L13 | Native views: `headlineLabel` connected, media container at least 120x120 points with an explicit size, and a visible "Ad" label | Read the native XIB and its constraints | Blocker (no label, no headline) / Note (video) | Connect the outlet, enlarge the view, add the label |
| L14 | No fullscreen ad shown at launch, on exit or right after another ad | Read where `showAd` is called | Blocker (ad network policy) | Show it at a natural pause, after a user action |
| L14b | `showAd` is never called from a `didLoad` method: combined with the reload in `didDismiss`, it shows ads one after another without any user action | `grep -rn -A6 "func didLoad(" --include=*.swift .` and look for `showAd` | Blocker (ad network policy) | Show only from the app's own flow, checking `isAvailable` |
| L14c | Interstitials are spaced out (see the `wortise-sdk-ad-formats` skill) | Read where `showAd` is called | Should fix | Add a minimum interval |
| L15 | Banners are constrained to the safe area, not under the home indicator or the status bar | Read the banner's constraints (`safeAreaLayoutGuide`) | Should fix | Pin them to the safe area |
| L16 | On the screen that requests consent, banners and other ads load in the consent completion, not in `viewDidLoad` | Compare with C1 | Should fix | Move `loadAd()` to the completion |

## Build

Find the workspace (CocoaPods) or project and the scheme with `xcodebuild -list`, then build both
configurations:

```bash
xcodebuild -workspace MyApp.xcworkspace -scheme MyApp -configuration Debug \
  -destination 'generic/platform=iOS Simulator' -derivedDataPath build build
xcodebuild -workspace MyApp.xcworkspace -scheme MyApp -configuration Release \
  -destination 'generic/platform=iOS' -derivedDataPath build CODE_SIGNING_ALLOWED=NO build
plutil -p build/Build/Products/Release-iphoneos/MyApp.app/Info.plist   # S3, S4
```

Use `-project MyApp.xcodeproj` when there is no workspace. Run `pod install` first if `Pods/` is
missing or out of date. Warnings inside the `Pods` project come from third-party code: do not
patch them. Any build error, or a crash at launch, is a real problem.

## Running

Only on a simulator or device the user provides. Run the Debug build, open the screens with ads
and read the SDK logs (subsystem `com.wortise.ads`, messages starting with `<Wortise>`) in the
Xcode console, in the Console app, or with:

```bash
xcrun simctl spawn booted log stream --level debug --predicate 'subsystem == "com.wortise.ads"'
```

| Log line | Meaning |
|---|---|
| `Device identifier: <id>` | The SDK started |
| `<Format> ad loaded for ad unit <id>` | The ad loaded |
| `<Format> ad failed to load for ad unit <id>: <error>` | It failed: see the `wortise-sdk-troubleshoot` skill |
| `showAd(from:) called with no ad available` | A show without `isAvailable` (L4) |
| `... is in cooldown, skipping request` (debug level) | A retry during the cooldown (L2) |
| `Native ad media view for ad unit <id> measures ...` (warning) | Media container too small (L13) |

`<Format>` is `Banner`, `Interstitial`, `Rewarded`, `App open` or `Native`. With the test ad units
every format must log "loaded".
