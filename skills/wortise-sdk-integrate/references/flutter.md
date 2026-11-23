# Flutter: set up the Wortise SDK

Full guide: <https://docs.wortise.com/flutter-sdk/sdk-integration.md>. It is the source of truth
for versions and requirements; read it when a value here looks outdated. The package is `wortise`
on pub.dev; it wraps the Wortise Android and iOS SDKs, so the host app must also meet their
requirements.

## Requirements

| Platform | Item | Minimum | Where |
|---|---|---|---|
| Android | Android Gradle Plugin | 8.9.1 (Gradle 8.11.1) | `android/settings.gradle(.kts)`, `android/gradle/wrapper/gradle-wrapper.properties` |
| Android | `compileSdk` | 36 | `android/app/build.gradle(.kts)` |
| Android | Kotlin | 2.2 | `org.jetbrains.kotlin.android` in `android/settings.gradle(.kts)` |
| Android | `minSdk` | 24 | `android/app/build.gradle(.kts)` |
| iOS | Deployment target | 15.0 | Xcode project and, with CocoaPods, `platform :ios, '15.0'` in `ios/Podfile` |
| Both | Flutter / Dart | 3.24 / 3.5 | `environment` in `pubspec.yaml` |

`flutter.compileSdkVersion` and `flutter.minSdkVersion` meet these values in current Flutter
versions; old projects may hardcode lower ones. Raising the Android Gradle Plugin, Gradle, Kotlin,
`compileSdk` or the Flutter SDK constraint is safe: do it and tell the user. Raising `minSdk` or
the iOS deployment target drops devices: ask the user first.

## Dependency

```yaml
dependencies:
  wortise: ^1.8.3   # the version from the integration page
```

Then `flutter pub get`. Do not add Google Mobile Ads, mediation adapters or ad network SDKs (on
either platform, nor the `google_mobile_ads` package for Wortise): the plugin brings the native
SDKs, which bring them. If the app already uses `google_mobile_ads`, tell the user: both depend on
Google Mobile Ads and the versions must be compatible.

The installed version is in `pubspec.lock`. Pre-releases (`-beta`) are git tags, not pub.dev
versions: do not use them unless the user asks.

## Repositories (Android)

The plugin adds the Maven repositories it needs to every Gradle project of the app, so most apps
need nothing. If `android/settings.gradle(.kts)` has a `dependencyResolutionManagement` block with
`FAIL_ON_PROJECT_REPOS` (the build fails) or `PREFER_SETTINGS` (the repositories are ignored and
`com.wortise:android-sdk` is not found), declare them there. These are the ones the plugin adds:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.wortise.com/artifactory/public") }
        maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
        maven { url = uri("https://artifactory.bidmachine.io/bidmachine") }
        maven { url = uri("https://cboost.jfrog.io/artifactory/chartboost-ads/") }
    }
}
```

## Google App ID

Android, inside `<application>` in `android/app/src/main/AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY" />
```

iOS, in `ios/Runner/Info.plist`: `GADApplicationIdentifier` with the iOS Google App ID, and the
`SKAdNetworkItems` array copied completely from the integration page (do not trim it).

```xml
<key>GADApplicationIdentifier</key>
<string>ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY</string>
```

Without them, Google Mobile Ads crashes the app at launch. If an entry already exists (an app
that used AdMob), do not overwrite it silently: ask the user, because the value must be the one
the Wortise dashboard shows. Ask for the values of each platform: a Wortise app belongs to one
platform, so an app published on both stores has a different app key, Google App ID and ad unit
IDs on Android and on iOS. Pick them with `Platform.isAndroid` / `Platform.isIOS` (`dart:io`).

## Imports

There is a single library; nothing under `package:wortise/src/` may be imported:

```dart
import 'package:wortise/wortise.dart';
```

## Initialization, test mode and consent

Initialize once, from the root widget's `State`, and create no ad and add no `BannerAd` widget
before `initialize`, the consent request and the test mode setting have completed. An ad
requested before the first initialization fails with `SDK_NOT_INITIALIZED`; the SDK stores the
app key, so this only shows on the first launch after a fresh install. Do not `await` the
initialization in `main()` before `runApp`: it waits for the network and delays the first frame.

```dart
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:wortise/wortise.dart';

class _MyAppState extends State<MyApp> {
  bool _adsReady = false;

  @override
  void initState() {
    super.initState();

    _initAds();
  }

  Future<void> _initAds() async {
    await AdSettings.setTestEnabled(kDebugMode);

    await WortiseSdk.initialize(AdIds.appKey);

    await ConsentManager.requestIfRequired();

    if (!mounted) return;

    // Create the ads and add the BannerAd widgets only from now on
    setState(() => _adsReady = true);
  }

  // build(): show the BannerAd widgets only when _adsReady is true
}
```

- Apps for children: see "Apps for children" below before writing this code.
- Calling `initialize` again has no effect.
- The calls do nothing on platforms other than Android and iOS (web, desktop), except
  `NativeAdLoader`, which throws `MissingPluginException` there.

## Test mode and per-build values

`AdSettings.setTestEnabled(true)` makes every request a test request, which never earns revenue.
The value is not saved: set it on every launch, before loading ads, with `kDebugMode` so it is
off in release builds.

Choose the test ad units the same way, so a release build can never ship a `test-` ID. Until the
user has real IDs, leave the release values as obvious placeholders: those ads fail with
`INVALID_PARAMS` instead of earning nothing silently.

```dart
import 'package:flutter/foundation.dart';

class AdIds {
  static bool get _ios => defaultTargetPlatform == TargetPlatform.iOS;

  static String get appKey => _ios ? '<iOS app key>' : '<Android app key>';

  static String get banner => kDebugMode
      ? 'test-banner'
      : (_ios ? '<iOS banner ad unit ID>' : '<Android banner ad unit ID>');
}
```

## Consent

- The SDK never shows the consent dialog by itself. `ConsentManager.requestIfRequired()` shows
  it only when required, and completes with `true` if the dialog was shown: it is not what the
  user chose (use `canRequestPersonalizedAds` or `canCollectData` for that).
- It needs the app's UI running (an `Activity` on Android, the Flutter view controller on iOS):
  call it from a widget, as above, not from a background isolate.
- Give the user a way to change their choice later: a "Privacy choices" option in the app's
  settings or menu that calls `ConsentManager.request()`, which always shows the dialog. Google's
  own dialog tells users to look for it. Do not call `request()` at startup.
- If the app already uses an IAB TCF consent platform, do not add this request: the SDK reads the
  consent that platform stores.

Guide: <https://docs.wortise.com/flutter-sdk/user-consent.md>.

## Apps for children

If the audience includes children, even partially, follow
<https://docs.wortise.com/flutter-sdk/privacy.md> completely (settings that go before
`initialize`, content rating and Android build changes), and confirm the audience with the user
first. Do not improvise a different setup.

## App Tracking Transparency (iOS, optional)

The SDK never shows the tracking prompt and the ads work without it. Only if the user wants it:
add `NSUserTrackingUsageDescription` to `Info.plist` and request the authorization (natively or
with a plugin that exposes `ATTrackingManager.requestTrackingAuthorization`) after the consent
request and before the first ad is loaded.
