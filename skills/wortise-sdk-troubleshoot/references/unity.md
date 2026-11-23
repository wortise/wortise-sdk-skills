# Unity: troubleshooting

Full page: <https://docs.wortise.com/unity-sdk/troubleshooting.md>.

## Logs

The SDK does not work in the Unity Editor: there are no logs or events there. Use a device build.
In version 1.8.1 the failure events carry no reason, so the native SDK logs are the source.
`Debug.Log(WortiseSdk.Version)` prints the native SDK version on a device.

Android, with the `Wortise` tag (`Unity` adds the game's own `Debug.Log` lines):

```bash
adb shell setprop log.tag.Wortise DEBUG    # debug messages too; run it before starting the app
adb logcat -s Wortise Unity
```

iOS: run the app from Xcode and filter the console by `<Wortise>`. The logs are also in the
macOS Console app, under the `com.wortise.ads` subsystem.

| Log line | Meaning |
|---|---|
| `Wortise SDK - Version <version>` | The native SDK version (Android) |
| `Device identifier: <id>` | The device's identifier, to register it as a test device in the dashboard |
| `<Format> ad loaded for ad unit <id>` | The ad loaded |
| `<Format> ad failed to load for ad unit <id>: <error>` | It failed, with the error code |
| `showAd() called with no ad available` (`showAd(from:)` on iOS) | `ShowAd()` on an ad that is not loaded |
| `Ad load for <id> is in cooldown, skipping request` (debug) | A load during the cooldown after a failure. Each ad object has its own cooldown |
| `Could not show consent form: ...` (debug, Android) | Why the consent dialog was not shown |
| `Adapter not found: com.wortise.ads.mediation...` (warning, Android) | An ad network the app does not include; expected |

## Error codes

They appear at the end of the "failed to load" log line:

| Error | Meaning | What to do |
|---|---|---|
| `NO_FILL` | No ad network had an ad for this request | Normal. Check with the test ad units (see the skill) |
| `SDK_NOT_INITIALIZED` | The ad was requested before `WortiseSdk.Initialize` was ever called | Initialize at startup and load the ads after `OnInitialized` and consent |
| `INVALID_PARAMS` | Invalid or missing values in the request | Check that the ad unit ID is set and correct |
| `NO_NETWORK` (Android) | No connection | Load again when the connection is back |
| `TIMEOUT` | The ad took too long | Usually temporary: load again later |
| `LOAD_FAILED` | The ad content could not be loaded | Temporary: load a new ad later |
| `RENDER_ERROR`, `RENDER_PROCESS_GONE` (the second, Android only) | The ad could not be rendered, or the system killed its WebView | Continue the game and load a new ad |
| `SERVER_ERROR` | Wortise's servers returned an error | Load again later; contact Wortise if it persists |
| `ADAPTER_MISSING` (Android) | A network needs an adapter that is not in the app | Nothing: the SDK moves on to the next network |
| `UNSPECIFIED` | Unknown | Read the rest of the log |

## Common problems

| Symptom | Cause | Fix |
|---|---|---|
| Nothing happens in the Editor: no `OnInitialized`, no consent callback, `IsAvailable` always `false` | The SDK does nothing in the Editor | Expected. Test on a device; make sure the game does not wait for those events |
| The game hangs at startup in the Editor | The game flow waits for `OnInitialized`, the consent callback or `OnLoaded` | Gate only the ad loading on SDK events |
| `CS0123` or `CS1593` on an `OnFailedToLoad` / `OnFailedToShow` handler | The handler is written for another version: 1.8.1 events take no argument, a later version passes a `WortiseAdError` | Check whether `Assets/Wortise/Scripts/WortiseAdError.cs` exists and match the signature |
| `Could not find com.wortise:android-sdk` | The custom Gradle templates are not enabled, or the Android Resolver was not run | Enable the three templates and run **Assets > External Dependency Manager > Android Resolver > Resolve** |
| `ClassNotFoundException: com.wortise.ads.WortiseSdk` in logcat | The native Android SDK is not in the build | Resolve the dependencies and build again |
| Android build fails on `minSdkVersion` | Minimum API Level below 24 | 24 or higher (ask the user first) |
| Android build fails with Android Gradle Plugin or AAR metadata errors | Unity older than 6 | Unity 6 (6000.0) or higher |
| Errors about multiple assemblies with the same name (`Google.*Resolver`) | Two copies of the External Dependency Manager | Keep one: remove the extra folder or package |
| `Could not load Wortise settings` in the build log | `Assets/Resources/WortiseSettings.asset` does not exist | Open **Wortise > Settings**, set the IDs and build again |
| Crash at launch, Google Mobile Ads mentions the application ID (Android) or `GADApplicationIdentifier` (iOS) | The Google App ID of that platform is missing or wrong in **Wortise > Settings** | Set the ID from the Wortise dashboard and build again |
| `pod install` fails with "required a higher minimum deployment target" | iOS target below 15.0 | Build from Unity again (it raises the target), or set 15.0 in Player Settings |
| `ShowAd()` does nothing | No ad loaded: still loading, failed, or already shown | Check `IsAvailable`, load in advance, reload after `OnDismissed` |
| `LoadAd()` raises no event | The ad is already loading, or in cooldown after a failure | Check `IsInCooldown` and `CooldownRemainingMs`; retry later |
| `OnFailedToLoad` every time | Read the error code in the logs | Compare with the test ad units |
| The consent dialog never appears | Not required for this user (outside the regions that need it, or already answered) | Expected; `Request()` always shows it |
| The consent callback never runs (on a device) | The request was made before the game had a visible window | Request it from `OnInitialized` |
| Several banners on screen, or a banner in a scene that should have none | Banners survive scene changes until `Destroy()` | Destroy each banner in `OnDestroy`; destroy the previous one before creating another |
| The banner is wider than the screen | The adaptive width was given in pixels (`Screen.width`) | Use dp/points, such as 320 |
| The banner covers game buttons | Banners are drawn on top of the game view | Move the controls or the banner; `AdHeightPx` gives its height |
| An app open ad appears right after every interstitial or rewarded ad (Android) | `OnApplicationPause(false)` also runs when a fullscreen ad closes | Skip that resume (see the `wortise-sdk-ad-formats` skill) |
| `MissingReferenceException` from a Wortise handler | A handler of a destroyed object is still subscribed (for example, to the static `OnInitialized`) | Unsubscribe with `-=` in `OnDestroy` |
| Errors calling the SDK from a background thread | The SDK must be called from the main thread | Move the call to the main thread |
| Lower fill in an app for children | Child-directed treatment limits the demand | Expected; check the setup against <https://docs.wortise.com/unity-sdk/privacy.md> |
