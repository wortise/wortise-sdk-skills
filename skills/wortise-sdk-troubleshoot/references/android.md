# Android: troubleshooting

Full page: <https://docs.wortise.com/android-sdk/troubleshooting.md>.

## Logs

The SDK logs with the `Wortise` tag. To also get its debug messages, raise the level on the
device, or in code before initializing the SDK:

```bash
adb shell setprop log.tag.Wortise DEBUG
adb logcat -s Wortise
```

```kotlin
WortiseLog.level = WortiseLog.Level.DEBUG
```

| Log line | Meaning |
|---|---|
| `Wortise SDK - Version <version>` | The SDK version in the app |
| `Device identifier: <id>` | The device's identifier, to register it as a test device in the dashboard |
| `<Format> ad loaded for ad unit <id>` | The ad loaded |
| `<Format> ad failed to load for ad unit <id>: <error>` | It failed, with the error code |
| `showAd() called with no ad available` | `showAd()` on an ad that is not loaded |
| `Ad load for <id> is in cooldown, skipping request` | A load during the cooldown after a failure. Each ad object has its own cooldown, so several lines for one ad unit may come from several objects (for example, after the screen was recreated) |
| `Could not show consent form: ...` | Why the consent dialog was not shown |
| `Adapter not found: com.wortise.ads.mediation...` (warning) | An ad network the app does not include; expected, the SDK uses the others |

## Error codes

The failure callbacks receive an `AdError`:

| Error | Meaning | What to do |
|---|---|---|
| `NO_FILL` | No ad network had an ad for this request | Normal. Check with the test ad units (see the skill) |
| `SDK_NOT_INITIALIZED` | The ad was requested before `WortiseSdk.initialize` was ever called | Initialize in `Application.onCreate`. The SDK stores the app key on the first initialization, so the error usually appears only on the first launch after installing: test with a clean install |
| `INVALID_PARAMS` | Invalid or missing values in the request | Check that the ad unit ID is set and correct |
| `NO_NETWORK` | No connection | Load again when the connection is back |
| `TIMEOUT` | The ad took too long | Usually temporary: load again later |
| `LOAD_FAILED` | The ad content could not be loaded | Temporary: load a new ad later |
| `RENDER_ERROR`, `RENDER_PROCESS_GONE` | The ad could not be rendered, or the system killed its WebView | Continue the app flow and load a new ad |
| `SERVER_ERROR` | Wortise's servers returned an error | Load again later; contact Wortise if it persists |
| `ADAPTER_MISSING` | A network needs an adapter that is not in the app | Nothing: the SDK moves on to the next network |
| `UNSPECIFIED` | Unknown | Read the logs |

## Common problems

| Symptom | Cause | Fix |
|---|---|---|
| Crash at launch, Google Mobile Ads mentions the application ID | `com.google.android.gms.ads.APPLICATION_ID` missing or invalid in the manifest | Add the Google App ID from the Wortise dashboard |
| `Could not find com.wortise:android-sdk` (or another dependency) | A Maven repository is missing | Add the three repositories of the integration guide |
| `AAR metadata` errors | Android Gradle Plugin or `compileSdk` too old | Android Gradle Plugin 8.9.1+ and `compileSdk` 36+ |
| Kotlin metadata or binary version errors | Kotlin below 2.2 | Kotlin 2.2+ |
| Manifest merger error on `minSdkVersion` | `minSdk` below 24 | `minSdk` 24 (ask the user first) |
| `Unresolved reference: BuildConfig` after adding test mode | `BuildConfig` generation is disabled | `buildFeatures { buildConfig = true }`, or use `FLAG_DEBUGGABLE` |
| R8 warnings in the release build (Kotlin metadata, missing `billingclient` or `bytedance` classes) | They come from the SDK and its ad networks | Expected while the build succeeds: add no rules |
| Banner hidden under the navigation bar | Edge-to-edge drawing with `targetSdk` 35+ on Android 15+ | Handle the window insets in the layout |
| `showAd()` does nothing | No ad loaded: still loading, failed, already shown, or an app open ad older than four hours | Check `isAvailable`, load in advance, reload after dismissed |
| `loadAd()` gets no callback | The ad is already loading, or it is in cooldown after a failure | Check `isInCooldown` and `cooldownRemainingMs`; retry later |
| Banner crashes with `IllegalArgumentException` about the ad unit ID | A different `adUnitId` assigned to a banner that already had one | One banner instance per ad unit |
| The consent dialog never appears | Not required for this user (outside the regions that need it, or already answered), or no `Activity` was passed | Expected when not required; read `Could not show consent form` otherwise |
| Native ads never show video | Media view smaller than 120x120 dp | Enlarge it (the SDK logs a warning with the size) |
| Lower fill in an app for children | Child-directed treatment limits the demand: no personalized ads, and `android-sdk-families` bundles fewer ad networks | Expected; check the setup against <https://docs.wortise.com/android-sdk/families.md> |
