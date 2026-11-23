# React Native: troubleshooting

Full page: <https://docs.wortise.com/react-native/troubleshooting.md>. The plugin wraps the native
SDKs, so their native logs are the evidence; the JavaScript console only shows what the app logs.

## Logs

```bash
# Android: add DEBUG to also get the debug messages (resets when the device restarts)
adb shell setprop log.tag.Wortise DEBUG
adb logcat -s Wortise

# iOS simulator; on a device, use the Xcode console or the Console app (subsystem com.wortise.ads)
xcrun simctl spawn booted log stream --level debug --predicate 'subsystem == "com.wortise.ads"'
```

iOS messages carry the `<Wortise>` prefix.

| Log line | Meaning |
|---|---|
| `Wortise SDK - Version <version>` (Android) | The native SDK version in the app |
| `Device identifier: <id>` | The SDK initialized; give this identifier to Wortise support |
| `<Format> ad loaded for ad unit <id>` | The ad loaded |
| `<Format> ad failed to load for ad unit <id>: <error>` | It failed, with the error code |
| `showAd() called with no ad available` (iOS: `showAd(from:)`) | `showAd()` with no ad loaded |
| `Ad load for <id> is in cooldown, skipping request` (debug) | A load during the cooldown after a failure |
| `Could not show consent form: ...` (Android, debug) | Why the consent dialog was not shown |
| `Adapter not found: com.wortise.ads.mediation...` (Android warning) | An ad network the app does not include; expected |

## Error codes

`on...FailedToLoad` and `on...FailedToShow` receive `{ name, message }` (banners: in
`event.nativeEvent`). `name` is the code:

| Error | Meaning | What to do |
|---|---|---|
| `NO_FILL` | No ad network had an ad for this request | Normal. Check with the test ad units (see the skill) |
| `SDK_NOT_INITIALIZED` | The ad was requested before `RNWortiseSdk.initialize` was ever called. Child effects run before the root's, so it usually appears only on the first launch after installing | Load ads after the init and consent Promise; test with a clean install |
| `INVALID_PARAMS` | Invalid or missing values | Check the ad unit ID and, on banners, the size |
| `NO_NETWORK` (Android) | No connection | Load again when the connection is back |
| `TIMEOUT` | The ad took too long | Load again later |
| `LOAD_FAILED` | The ad content could not be loaded | Load a new ad later |
| `RENDER_ERROR`, `RENDER_PROCESS_GONE` (Android) | The ad could not be rendered, or its WebView was killed | Continue the app flow and load a new ad |
| `SERVER_ERROR` | Wortise's servers returned an error | Load again later; contact Wortise if it persists |
| `ADAPTER_MISSING` (Android) | A network needs an adapter that is not in the app | Nothing: the SDK moves on |
| `UNSPECIFIED` | Unknown | Read the logs |

Native ads have no event for this: the Promise of `WortiseNativeAd.createForAdRequest` rejects
with code `AD_LOAD_FAILED` and the error's message (not its name); the logs have the name.

## Common problems

| Symptom | Cause | Fix |
|---|---|---|
| Crash at launch, Google Mobile Ads mentions the application ID | Google App ID missing in `AndroidManifest.xml` or `Info.plist` (Expo: `androidGoogleAppId`/`iosGoogleAppId` missing, or `prebuild` not run since) | Add it and rebuild |
| At import or first call: ``new NativeEventEmitter()` requires a non-null argument`` (iOS) or `Cannot read property '...' of undefined` | The native module is missing: running in Expo Go, or the native app was not rebuilt after installing the package | Development build; rebuild (`pod install` on bare iOS) |
| `Could not find com.wortise:android-sdk` | Maven repositories missing (Expo prints `Could not inject maven repository` during prebuild when it finds no block to add them to) | Add the three repositories to `allprojects { repositories { } }` |
| `AAR metadata`, Kotlin metadata or `minSdkVersion` errors | Android Gradle Plugin, `compileSdk`, Kotlin or `minSdk` too old | See the requirements of the `wortise-sdk-integrate` skill |
| `pod install` asks for a higher deployment target | iOS target below 15.0 | iOS 15.0 (ask the user first) |
| Banner empty, no event | `loadAd()` never called through the ref, or called before the ref was set; on iOS, a decimal width in an adaptive size | `ref.current?.loadAd()` from an effect; `Math.floor(width)` |
| Fullscreen ad never loads, no event | `setAdUnitId()` not called before `loadAd()`; on Android, called only at module scope and the activity was recreated; or in cooldown after a failure | `setAdUnitId` from a root effect; check `await isInCooldown` |
| Loaded ad lost | `setAdUnitId()` or `loadAd()` called again (every mount, two ad units for one format) | Set the ad unit once, load again only after dismissed |
| `showAd()` does nothing | No ad available, or `isAvailable` not awaited | `if (await RNWortiseInterstitial.isAvailable)`; wait for the loaded event |
| Listeners fire several times; reward granted twice | `addEventListener` in a component body, or subscriptions never removed | Subscribe in `useEffect`, `remove()` in the cleanup |
| App open ad right after an interstitial or rewarded ad (Android) | Closing a fullscreen ad changes `AppState` to `background` and back | Skip the next app open after showing another fullscreen ad |
| Native ad view empty | No factory registered natively with that `factoryId`, or registered on one platform only, or the component has no size | Register on both platforms with the same id; give it a height |
| Native factories gone in Expo | `expo prebuild --clean` regenerated `android/` and `ios/` | Restore them; ask the user how they keep native code |
| Consent dialog never appears | Not required for this user (region, or already answered), or requested before a screen was visible (resolves `false`) | Expected when not required; request it from a mounted component |
| Lower fill in an app for children | Child-directed treatment limits the demand | Expected; check the setup against <https://docs.wortise.com/react-native/privacy.md> |
