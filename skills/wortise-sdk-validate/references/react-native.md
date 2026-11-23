# React Native: checks

`gjs` below stands for
`grep -rn --exclude-dir=node_modules --exclude-dir=android --exclude-dir=ios --include='*.[jt]s' --include='*.[jt]sx'`,
run from the project root. The behaviour behind each check is documented in
<https://docs.wortise.com/react-native/best-practices.md>, which ends with a pre-release
checklist.

## Setup

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| S1 | `@wortise/react-native-sdk` in `package.json` with a stable version (no `-alpha`, `-beta` or `-rc` unless the user wants it), and no Google Mobile Ads or adapter package added for Wortise | `grep -n -e wortise -e google-mobile-ads package.json`; `node -p "require('@wortise/react-native-sdk/package.json').version"` | Blocker if missing; note for another ads package | Install the stable version; tell the user about other ad SDKs |
| S2 | Expo: the config plugin is in the app config with `androidGoogleAppId` and `iosGoogleAppId`; the app runs as a development build, not in Expo Go | `grep -rn -A5 "@wortise/react-native-sdk" app.json app.config.*` | Blocker | Add the plugin entry; use `expo run` or an EAS development build |
| S3 | Bare: the three Maven repositories in `android/build.gradle` (or `settings.gradle`) | `grep -rn -e maven.wortise.com -e bytedance -e bidmachine android/*.gradle*` | Blocker | Add them to `allprojects { repositories { } }` |
| S4 | The Google App IDs are the ones the dashboard shows: not placeholders, not Google's sample ID (`ca-app-pub-3940256099942544~...`), different for Android and iOS | Expo: S2. Bare: `grep -rn -A1 "gms.ads.APPLICATION_ID" android/app/src/main/AndroidManifest.xml` and `grep -rn -A1 GADApplicationIdentifier ios/*/Info.plist` | Blocker (crash at launch) | Use the dashboard values |
| S5 | iOS `Info.plist` has `SKAdNetworkItems` (Expo: `ios.infoPlist`) | `grep -rn -c SKAdNetworkIdentifier app.json ios/*/Info.plist` | Should fix | Add the list of the integration guide |
| S6 | Android Gradle Plugin 8.9.1+, `compileSdk` 36+, Kotlin 2.2+, `minSdk` 24+, iOS deployment target 15.0+ | `android/build.gradle` `ext` block, `cd android && ./gradlew buildEnvironment`, `ios/Podfile`; Expo: `expo-build-properties` in the app config | Blocker (the build fails) | Raise them; ask before raising `minSdk` or the iOS target |

## Initialization

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| I1 | `RNWortiseSdk.initialize` runs once, from an effect of the root component, not in a component body, a screen or a render path | `gjs "RNWortiseSdk.initialize" .` and read the surrounding code | Blocker if missing; should fix if repeated | Move it to the root effect |
| I2 | No ad loads before initialization and consent finish (`loadAd`, `createForAdRequest` and banner `loadAd()` wait for them) | `gjs -e "loadAd(" -e createForAdRequest -e "RNWortiseSdk.wait" .` and compare with I1 | Blocker | Wait for the init and consent Promise (see the `wortise-sdk-integrate` skill) |
| I3 | If Android is initialized natively in `MainApplication`, JavaScript still initializes (for iOS) | `grep -rn "WortiseSdk.initialize" android/app/src` and I1 | Blocker (no ads on iOS) | Initialize from JavaScript too |

## Consent and privacy

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| C1 | `RNWortiseConsentManager.requestIfRequired()` runs before the first ad load, from a mounted component (not at module scope), or the app has an IAB TCF consent platform | `gjs -e "RNWortiseConsentManager." -e IABTCF .`, and look in `package.json` for another consent SDK | Blocker | Request it after `initialize` and load ads when it resolves |
| C2 | `RNWortiseConsentManager.request()` (always shows the dialog) runs only from a user action | `gjs "RNWortiseConsentManager.request()" .` | Should fix | Use `requestIfRequired()` at startup |
| C3 | The app offers a "Privacy choices" option that calls `request()` | Same search as C2 | Should fix | Add it to the settings or menu |
| C4 | Apps for children: the setup of <https://docs.wortise.com/react-native/privacy.md>, with `setChildDirected` and `setMaxAdContentRating` before `initialize` | `gjs -e setChildDirected -e setMaxAdContentRating .` | Blocker | Follow the Privacy page; confirm with the user |

## Test mode

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| T1 | `setTestEnabled` is never `true` in a release build (`__DEV__` or an equivalent build flag), and it runs on every launch | `gjs setTestEnabled .` and read the argument | Blocker | `RNWortiseAdSettings.setTestEnabled(__DEV__)` |
| T2 | No `test-` ad unit reaches a release build | `gjs "'test-" .` and `gjs '"test-' .`, then check that each value sits behind `__DEV__` | Blocker | Choose the IDs with `__DEV__` |

## Ad lifecycle

| # | Check | How to find it | Severity | Fix |
|---|---|---|---|---|
| L1 | Every `<WortiseBanner>` has a ref whose `loadAd()` is called once, after initialization and consent | `gjs -A6 "<WortiseBanner" .` and `gjs "loadAd()" .` | Blocker (empty banner) | Call `ref.current?.loadAd()` from an effect |
| L2 | No `loadAd()` inside `onFailedToLoad` of a banner | `gjs -A6 onFailedToLoad .` | Should fix | Remove it: the banner retries by itself |
| L3 | Banner listeners read `event.nativeEvent`; `autoRefreshTime` is in milliseconds (30000 to 120000); adaptive widths are integers | `gjs -e onFailedToLoad -e onRevenuePaid -e autoRefreshTime -e AdaptiveBannerAdSize .` | Should fix (no iOS banner with a decimal width) | `event.nativeEvent`; milliseconds; `Math.floor(width)` |
| L4 | Banners stay mounted: not re-created through list rows, conditional rendering or changing `key`s | Read where `<WortiseBanner>` is rendered | Should fix | Mount it once per screen |
| L5 | `setAdUnitId` runs once per format, from an effect of a long-lived component, before `loadAd()`; not at module scope; one ad unit per format | `gjs -B3 -A3 "setAdUnitId(" .` | Blocker if missing or at module scope (no ads on Android after the activity is recreated) | Move it to the root effect |
| L6 | No `loadAd()` on every mount or focus, or on an ad that is already loaded | `gjs -B3 -A3 "loadAd(" .` | Should fix | Load once, and again on dismissed |
| L7 | Every `isAvailable`, `isShowing`, `isInCooldown`, `cooldownRemainingMs`, `isInitialized`, `isReady`, `canCollectData` and `exists` is awaited | `gjs -e "\.isAvailable" -e "\.isShowing" -e "\.isInCooldown" -e cooldownRemainingMs -e "\.isInitialized" -e "\.isReady" -e canCollectData -e "\.exists" .` and look for a missing `await` or `.then` | Blocker (the condition is always true) | `await` it |
| L8 | Every `showAd()` is preceded by an awaited `isAvailable`, and the flow continues when it is false, on dismissed and on failed to show; nothing relies on the value `showAd()` resolves with to know if the ad was shown | `gjs -B6 -A4 "showAd()" .` | Should fix | Check `isAvailable`; continue on both events |
| L9 | `addEventListener` runs in an effect whose cleanup calls `remove()`; no `removeAllListeners` in components | `gjs -B3 -A3 -e addEventListener -e removeAllListeners .` | Should fix (duplicated callbacks and rewards) | Subscribe in `useEffect`, remove in its cleanup |
| L10 | The reward is granted only in `onRewardedCompleted`, and only when `success` is true | `gjs -A6 -e onRewardedCompleted -e onRewardedDismissed -e onRewardedShown .` | Blocker | Move the reward there |
| L11 | No immediate `loadAd()` in `on...FailedToLoad`, and every fullscreen ad has a failure handler that retries later; native requests are retried with a delay, not in their `catch` | `gjs -A6 -e FailedToLoad -e createForAdRequest .` | Should fix | Retry after `cooldownRemainingMs` (at least 30 s) |
| L12 | `showAd` is never called from an `on...Loaded` listener | `gjs -A6 "Loaded'" .` and look for `showAd` | Blocker (ad network policy) | Show from the app's own flow |
| L13 | No fullscreen ad at launch, on exit or right after another ad; interstitials spaced out (see the `wortise-sdk-ad-formats` skill) | Read where `showAd` and `tryToShowAd` are called | Blocker (at launch, right after another ad); should fix (spacing) | Show at natural pauses, with a minimum interval |
| L14 | App open: shown from an `AppState` change to `active`, and skipped right after another fullscreen ad (Android also reports those as a return from the background) | `gjs -B4 -A8 -e tryToShowAd -e "RNWortiseAppOpen.showAd" .` | Should fix | Add the skip flag |
| L15 | Native: a factory registered natively on Android and iOS with the id passed to `<WortiseNativeAdView factoryId>`; media view at least 120x120; an "Ad" label | `gjs factoryId .`, `grep -rn registerFactory android/app/src ios` and read the native layouts | Blocker (empty view; no label); note (media size) | Register both, with the same id |
| L16 | Every `WortiseNativeAd` is destroyed when replaced and when its component unmounts | `gjs -e createForAdRequest -e "\.destroy()" .` | Should fix | `destroy()` in the effect cleanup |
| L17 | Banners clear of the system bars and the home indicator | Read the banner's container | Should fix | Use the app's safe-area handling |
| L18 | Expo with native ads: the factory code survives `expo prebuild --clean` (native folders kept, or a config plugin writes it) | `git ls-files android ios`, `grep -n prebuild package.json` | Should fix | Ask the user how they generate the native projects |

## Build

```bash
npx tsc --noEmit                         # TypeScript projects
cd android && ./gradlew assembleRelease  # bare; Expo: npx expo run:android --variant release
npx expo run:ios --configuration Release # Expo; bare: build the Release scheme in Xcode
```

A release build matters: `__DEV__` is false there, so test mode and test ad units must be gone.
Minified Android builds print R8 warnings from the SDK and its ad networks (Kotlin metadata,
missing `billingclient` or `bytedance` classes): add no rules for them. Errors are real problems.

## Running

```bash
adb logcat -c && adb shell setprop log.tag.Wortise DEBUG
# open the screens with ads, then:
adb logcat -d -s Wortise
xcrun simctl spawn booted log stream --level debug --predicate 'subsystem == "com.wortise.ads"'
```

On iOS the messages also appear in the Xcode console, prefixed with `<Wortise>`.

| Log line | Meaning |
|---|---|
| `Wortise SDK - Version <version>` (Android) | The SDK started |
| `Device identifier: <id>` | The SDK initialized |
| `<Format> ad loaded for ad unit <id>` | The ad loaded |
| `<Format> ad failed to load for ad unit <id>: <error>` | It failed: see the `wortise-sdk-troubleshoot` skill |
| `showAd() called with no ad available` (iOS: `showAd(from:)`) | A show without `isAvailable` (L8) |
| `... is in cooldown, skipping request` (debug) | A retry during the cooldown (L11) |

With the test ad units every format must log "loaded". `Adapter not found` warnings at startup on
Android are expected: they list ad networks the app does not include.
