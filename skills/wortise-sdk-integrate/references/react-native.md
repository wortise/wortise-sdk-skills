# React Native: set up the Wortise SDK

Covers bare React Native and Expo projects. Full guide:
<https://docs.wortise.com/react-native/sdk-integration.md>. It is the source of truth for versions,
requirements and the iOS `SKAdNetworkItems` list; read it when a value here looks outdated.

The package is `@wortise/react-native-sdk`. It wraps the native Android and iOS SDKs, so it does
not work in Expo Go: an Expo app needs a development build (`npx expo run:android`,
`npx expo run:ios` or an EAS development build).

## Requirements

| Item | Minimum | Bare project | Expo project |
|---|---|---|---|
| Android Gradle Plugin | 8.9.1 (Gradle 8.11.1) | `android/build.gradle`, `android/gradle/wrapper/gradle-wrapper.properties` | comes with the Expo SDK |
| `compileSdk` | 36 | `ext` block of `android/build.gradle` | `expo-build-properties`: `android.compileSdkVersion` |
| Kotlin | 2.2 | `kotlinVersion` in the same `ext` block | `expo-build-properties`: `android.kotlinVersion` |
| `minSdk` | 24 | `minSdkVersion` in the same `ext` block | `expo-build-properties`: `android.minSdkVersion` |
| iOS deployment target | 15.0 | `platform :ios` in `ios/Podfile` | `ios.deploymentTarget` (app config in Expo SDK 56+, `expo-build-properties` before) |

The Android Gradle Plugin version often comes from React Native itself (a `classpath` without a
version). Check the one that resolves with `cd android && ./gradlew buildEnvironment | grep
com.android.tools.build:gradle`. If it is older, declare
`classpath("com.android.tools.build:gradle:<version>")` in `android/build.gradle` and update the
Gradle wrapper it needs; in Expo, a newer Expo SDK is the usual way: tell the user.

Raising the Android Gradle Plugin, Gradle, Kotlin or `compileSdk` is safe: do it and tell the
user. Raising `minSdk` or the iOS deployment target drops devices: ask the user first. Below these
minimums the Android build fails (`AAR metadata`, Kotlin metadata or `minSdkVersion` manifest
merger errors) and `pod install` fails on iOS.

## Package

```bash
npm install @wortise/react-native-sdk    # or yarn add / npx expo install, as the project does
```

- Use the version stated on the integration page. Check the installed one with
  `node -p "require('@wortise/react-native-sdk/package.json').version"`.
- Do not add Google Mobile Ads, mediation adapters or ad network SDKs for Wortise: the native SDKs
  bring the versions they were tested with. If the app already uses another Google Mobile Ads
  package (such as `react-native-google-mobile-ads`), tell the user and do not remove it.
- Apps whose audience includes children, even partially: read
  <https://docs.wortise.com/react-native/privacy.md>, follow it completely and confirm the setup
  with the user. Do not improvise a children's setup.

## Expo project

Add the config plugin to `app.json` (or `app.config.js`/`ts`), with the Google App ID of each
platform (Wortise dashboard, app details, `Google App ID`):

```json
{
  "expo": {
    "plugins": [
      [
        "@wortise/react-native-sdk",
        {
          "androidGoogleAppId": "ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY",
          "iosGoogleAppId": "ca-app-pub-XXXXXXXXXXXXXXXX~ZZZZZZZZZZ"
        }
      ]
    ]
  }
}
```

- The plugin only writes the two Google App IDs (it replaces an existing Android value) and the
  three Android Maven repositories. Without a Google App ID, the app crashes at launch.
- Add `SKAdNetworkItems` yourself under `expo.ios.infoPlist`, with the identifiers listed in the
  integration guide (as an array of `{ "SKAdNetworkIdentifier": "..." }` objects).
- The plugin runs during `npx expo prebuild` (and EAS builds): run it again after changing these
  values. `prebuild --clean` regenerates `android/` and `ios/` and wipes any hand-written native
  code, such as the native ad factories.

## Bare project

1. **Maven repositories** (Android). Add them to the `allprojects { repositories { } }` block of
   `android/build.gradle` (create the block if it does not exist). If `android/settings.gradle`
   declares repositories in `dependencyResolutionManagement`, add them there instead.

   ```groovy
   allprojects {
       repositories {
           maven { url 'https://maven.wortise.com/artifactory/public' }
           maven { url 'https://artifact.bytedance.com/repository/pangle' }
           maven { url 'https://artifactory.bidmachine.io/bidmachine' }
       }
   }
   ```

2. **Google App ID** (Android), inside `<application>` in
   `android/app/src/main/AndroidManifest.xml`:

   ```xml
   <meta-data
       android:name="com.google.android.gms.ads.APPLICATION_ID"
       android:value="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY" />
   ```

3. **Info.plist** (iOS): `GADApplicationIdentifier` with the iOS Google App ID, and the
   `SKAdNetworkItems` array of the integration guide.

4. `cd ios && pod install`.

If the manifest or `Info.plist` already has a Google App ID (an app that used AdMob), do not
overwrite it silently: ask the user, because it must be the one the Wortise dashboard shows.

## Imports

Everything comes from the package root:

```typescript
import {
  RNWortiseAdContentRating, RNWortiseAdSettings, RNWortiseAdSize, RNWortiseAppOpen,
  RNWortiseConsentManager, RNWortiseInterstitial, RNWortiseRewarded, RNWortiseSdk,
  WortiseBanner, WortiseNativeAd, WortiseNativeAdView,
} from '@wortise/react-native-sdk';
```

## Initialization, test mode and consent

Initialize once, from an effect of the root component, never in a component body (it would run
on every render). Request consent right after, and load ads only when both are done. A module
like this one keeps that order for the whole app:

```typescript
// ads.ts
import { Platform } from 'react-native';
import {
  RNWortiseAdSettings,
  RNWortiseConsentManager,
  RNWortiseSdk,
} from '@wortise/react-native-sdk';

const APP_KEY = Platform.select({ android: '<Android app key>', default: '<iOS app key>' });

let markReady!: () => void;

// Resolved once the SDK is initialized and the consent request has finished
export const adsReady = new Promise<void>(resolve => {
  markReady = resolve;
});

let started = false;

// Call it once, from an effect of the root component
export async function startAds(): Promise<void> {
  if (started) {
    return adsReady;
  }
  started = true;

  RNWortiseAdSettings.setTestEnabled(__DEV__);

  await RNWortiseSdk.initialize(APP_KEY);
  await RNWortiseConsentManager.requestIfRequired();

  markReady();
}
```

```tsx
// App.tsx
useEffect(() => {
  startAds();
}, []);
```

- Every ad waits for `adsReady` (see the `wortise-sdk-ad-formats` skill). Child effects run
  before the root's, so an ad loaded straight from a screen's effect on the first launch fails
  with `SDK_NOT_INITIALIZED`. `RNWortiseSdk.wait()` resolves when initialization finishes, but
  not after the consent request.
- A Wortise app belongs to one platform: Android and iOS each have their own app key, Google App
  ID and ad unit IDs. Ask for the values of both platforms and pick them with `Platform.select`.
- `WortiseSdk.initialize` in `MainApplication` (documented as an option) initializes Android only:
  the JavaScript call is still needed for iOS, and calling it again has no effect.
- Apps for children set `RNWortiseAdSettings.setChildDirected` and
  `setMaxAdContentRating` before `initialize`, as the Privacy page says.

## Test mode

`RNWortiseAdSettings.setTestEnabled(true)` makes every request a test request, which never earns.
It is not saved: set it on every launch before loading ads. `__DEV__` is `true` in development
builds and `false` in release builds, so `setTestEnabled(__DEV__)` is the idiomatic switch.

Use the test ad units only in development the same way, so a release build can never ship a
`test-` ID. Until the user has real IDs, leave the release values as obvious placeholders: those
ads fail with `INVALID_PARAMS` instead of earning nothing silently.

```typescript
export const AD_UNITS = __DEV__
  ? { banner: 'test-banner', interstitial: 'test-interstitial', rewarded: 'test-rewarded',
      appOpen: 'test-app-open', native: 'test-native' }
  : Platform.select({
      android: { banner: '<banner ad unit ID>', interstitial: '<interstitial ad unit ID>',
                 rewarded: '<rewarded ad unit ID>', appOpen: '<app open ad unit ID>',
                 native: '<native ad unit ID>' },
      default: { banner: '<banner ad unit ID>', interstitial: '<interstitial ad unit ID>',
                 rewarded: '<rewarded ad unit ID>', appOpen: '<app open ad unit ID>',
                 native: '<native ad unit ID>' },
    });
```

## Consent

- `requestIfRequired()` resolves with a `boolean` that tells whether the dialog was shown, not
  what the user chose. It needs a visible screen: called at module scope, before the app has a
  screen, it resolves `false` and shows nothing. That is why `startAds` runs from an effect.
- Give the user a way to change their choice later: a "Privacy choices" option in the app's
  settings that calls `RNWortiseConsentManager.request()`, which always shows the dialog. Google's
  dialog tells users to look for it. Do not call `request()` at startup.
- If the app already uses an IAB TCF consent platform, do not add this request: the SDK reads the
  consent that platform stores.

Guide: <https://docs.wortise.com/react-native/user-consent.md>.

## App Tracking Transparency (iOS, optional)

The SDK never shows the tracking prompt, and the app works without it (the SDK then uses the
vendor identifier). Only if the user wants it: add `NSUserTrackingUsageDescription` (bare:
`Info.plist`; Expo: `ios.infoPlist`), and request the authorization with a module the app already
uses or the user chooses, after the consent request and before the first ad request (in
`startAds`, between `requestIfRequired()` and `markReady()`).
