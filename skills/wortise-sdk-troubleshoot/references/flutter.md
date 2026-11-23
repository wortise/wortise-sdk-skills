# Flutter: troubleshooting

Full page: <https://docs.wortise.com/flutter-sdk/troubleshooting.md>.

## Logs

The plugin wraps the native SDKs, which log outside Dart: `flutter run` may not show them.

- **Android**: Logcat, tag `Wortise`. `adb shell setprop log.tag.Wortise DEBUG` adds the debug
  messages, then `adb logcat -s Wortise`.
- **iOS**: system log, lines prefixed `<Wortise>`, subsystem `com.wortise.ads`: the Xcode console,
  or the macOS Console app filtered by that subsystem.

| Log line | Meaning |
|---|---|
| `Wortise SDK - Version <version>` (Android) | The native SDK version in the app |
| `Device identifier: <id>` | The device's identifier, to register it as a test device in the dashboard |
| `<Format> ad loaded for ad unit <id>` | The ad loaded |
| `<Format> ad failed to load for ad unit <id>: <error>` | It failed, with the error code |
| `Ad load for <id> is in cooldown, skipping request` (`Native ad load ...` for native; debug level) | A load during the cooldown after a failure |
| `Could not show consent form: ...` (Android, debug level) | Why the consent dialog was not shown |
| `Adapter not found: com.wortise.ads.mediation...` (Android warning) | An ad network the app does not include; expected |

## Error codes

Failure events (`FAILED_TO_LOAD`, `FAILED_TO_SHOW`, `NativeAdEvent.FAILED`) carry the error name
in `args['error']`. Errors of the method calls themselves are not events: the `Future` completes
with a `PlatformException`.

| Error | Platform | Meaning | What to do |
|---|---|---|---|
| `NO_FILL` | Both | No ad network had an ad for this request | Normal. Check with the test ad units (see the skill) |
| `SDK_NOT_INITIALIZED` | Both | The ad was requested before `WortiseSdk.initialize` was ever called | Await `initialize` before creating ads or adding `BannerAd` widgets. The SDK stores the app key, so the error usually appears only on the first launch after installing: test with a clean install |
| `INVALID_PARAMS` | Both | Invalid or missing values in the request | Check that the ad unit ID is set and correct |
| `TIMEOUT` | Both | The ad took too long | Usually temporary: load again later |
| `LOAD_FAILED` | Both | The ad content could not be loaded | Temporary: load a new ad later |
| `RENDER_ERROR` | Both | The ad could not be rendered (native: the factory's view could not render it) | Continue the app flow and load a new ad |
| `SERVER_ERROR` | Both | Wortise's servers returned an error | Load again later; contact Wortise if it persists |
| `UNSPECIFIED` | Both | Unknown | Read the logs |
| `NO_NETWORK` | Android | No connection | Load again when the connection is back |
| `RENDER_PROCESS_GONE` | Android | The system killed the ad's WebView | Continue the app flow and load a new ad |
| `ADAPTER_MISSING` | Android | A network needs an adapter that is not in the app | Nothing: the SDK moves on to the next network |
| `PlatformException` `ACTIVITY_NOT_AVAILABLE` | Android | `loadAd()` of a fullscreen ad without an `Activity` | Load once the UI is running, not from a background isolate |
| `PlatformException` `Can't find NativeAdViewFactory with id` (`WortiseNativeAdViewFactory` on iOS) | Both | No native factory registered with that ID | Register the factory on both platforms with the ID passed to `NativeAdLoader` |

## Common problems

| Symptom | Cause | Fix |
|---|---|---|
| Crash at launch, Google Mobile Ads mentions the application ID | `APPLICATION_ID` missing in `AndroidManifest.xml`, or `GADApplicationIdentifier` missing in `Info.plist` | Add the Google App ID of each platform from the Wortise dashboard |
| `Could not find com.wortise:android-sdk` | `dependencyResolutionManagement` with `PREFER_SETTINGS` ignores the repositories the plugin adds (`FAIL_ON_PROJECT_REPOS` fails the build instead) | Declare the repositories in `android/settings.gradle(.kts)` (see the `wortise-sdk-integrate` skill) |
| `AAR metadata`, Kotlin metadata or `minSdkVersion` manifest merger errors | Android Gradle Plugin, `compileSdk`, Kotlin or `minSdk` below the requirements | AGP 8.9.1+, `compileSdk` 36+, Kotlin 2.2+, `minSdk` 24+ (ask before raising `minSdk`) |
| iOS `pod install` or SPM resolution fails on the deployment target | Target below iOS 15.0 | iOS 15.0 in the Xcode project and in the `Podfile` |
| `SDK_NOT_INITIALIZED` on the first launch only | A `BannerAd` or an ad created before `initialize` completed | Gate the ads behind the initialization (see the `wortise-sdk-integrate` skill) |
| Banner: infinite-height layout error, or it covers the screen | `MATCH_VIEW` or adaptive size in an unbounded parent | Wrap it in a `SizedBox`/`ConstrainedBox` with a height |
| Banner reloads, or makes a new request, on every scroll or rebuild | A new widget `State`: changing `key`, different parent, list item without `keepAlive` | Keep a stable widget; `keepAlive: true` in lists |
| Banner ignores a new `adSize`, `adUnitId`, `autoRefreshTime` or `requestParameters` | They are read only when the banner is created | Decide them before adding the widget |
| Banner refreshes too fast or never | `autoRefreshTime` given in seconds | Milliseconds, `30000` to `120000` |
| `showAd()` returns `false`, no event arrives | No ad loaded: still loading, failed, already shown, or no screen to show it on | Load in advance, continue the flow on `false`, reload after `DISMISSED` |
| A listener stops receiving events, or an ad dies when another screen closes | Two Dart objects with the same ad unit ID: the newer takes the events, and `destroy()` on either destroys both | One object per ad unit ID for the whole app |
| `loadAd()` gets no event | The ad is in cooldown after a failure, or already loading | Check `isInCooldown` / `cooldownRemainingMs`; retry later |
| App open ad never shows with `AppOpenManager` | Created with the plain constructor instead of `AppOpenManager.register`, no ad loaded, no `autoReload`, or another registration replaced it | `AppOpenManager.register(AppOpenAd(id, autoReload: true)..loadAd())`, once |
| App open ad after every return to the app (also after tapping an ad) | That is what `AppOpenManager` does: no frequency cap | Use the manual `AppOpenAd` integration |
| `AdWidget` shows the text `No ad is available for id ...` | The `AdWidget` was built for a `NativeAd` that had already been destroyed | Remove the `AdWidget` from the tree before destroying its ad, and never build one for a destroyed ad |
| Native ads never show video | Media view smaller than 120x120 dp/pt | Enlarge it |
| The consent dialog never appears | Not required for this user (outside the regions that need it, or already answered) | Expected; `requestIfRequired()` returns `true` only when it was shown |
| `PlatformException` from `ConsentManager` on Android, or `false` at once on iOS | Called without the app's UI (no `Activity` / view controller yet) | Call it from a widget after the first frame, not from a background isolate |
| `MissingPluginException` from `NativeAdLoader` on web or desktop | The plugin supports Android and iOS only | Do not create native ads on other platforms |
| Lower fill in an app for children | Child-directed treatment limits the demand | Expected; check the setup against <https://docs.wortise.com/flutter-sdk/privacy.md> |
