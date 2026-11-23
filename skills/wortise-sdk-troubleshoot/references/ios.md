# iOS: troubleshooting

Full page: <https://docs.wortise.com/ios-sdk/troubleshooting.md>.

## Logs

The SDK writes to the unified logging system with the subsystem `com.wortise.ads` and the category
`sdk`; every message starts with `<Wortise>`. Filter by it in the Xcode console or the Console app
(enable "Include Debug Messages" there), or on a simulator:

```bash
xcrun simctl spawn booted log stream --level debug --predicate 'subsystem == "com.wortise.ads"'
```

By default the SDK logs the main ad events and the errors. To also get its debug messages, raise
the level before initializing the SDK, in debug builds:

```swift
#if DEBUG
WortiseLog.level = .debug
#endif
```

| Log line | Meaning |
|---|---|
| `Device identifier: <id>` | The SDK was initialized; the device's identifier, to register it as a test device in the dashboard |
| `<Format> ad loaded for ad unit <id>` | The ad loaded |
| `<Format> ad failed to load for ad unit <id>: <error>` | It failed, with the error name |
| `Ad request failed for ad unit <id>: <description>` | Wortise's server rejected the request, with the reason |
| `showAd(from:) called with no ad available` | `showAd(from:)` on an ad that is not loaded |
| `Ad load for <id> is in cooldown, skipping request` (debug) | A load during the cooldown after a failure. Native ads log `Native ad load for <id> is in cooldown...`. Each ad object has its own cooldown, so several lines for one ad unit may come from several objects |
| `Native ad failed to render for ad unit <id> (...)` | `render(ad:into:)` failed; `Missing required view` means `headlineLabel` is not connected |
| `Native ad media view for ad unit <id> measures <w>x<h>pt; ...` (warning) | The media container is smaller than 120x120 points |
| `App open ad could not be shown due to missing window` | `WAAppOpenManager` found no window to present from |

`<Format>` is `Banner`, `Interstitial`, `Rewarded`, `App open` or `Native`.

## Error codes

The failure delegate methods receive a `WAAdError`. Its `name` is the value in the logs, and
`message` a short description:

| Error | Name | Meaning | What to do |
|---|---|---|---|
| `.noFill` | `NO_FILL` | No ad network had an ad for this request | Normal. Check with the test ad units (see the skill) |
| `.sdkNotInitialized` | `SDK_NOT_INITIALIZED` | The ad was requested before `WortiseAds.shared.initialize` was ever called | Initialize at launch. The SDK stores the app key on the first initialization, so the error usually appears only on the first launch after installing: test with a clean install |
| `.invalidParams` | `INVALID_PARAMS` | Invalid or missing values, such as no ad unit ID or one the server rejects | Check that the ad unit ID is set, correct and of the format being loaded |
| `.serverError` | `SERVER_ERROR` | Wortise's servers could not be reached (also with no connection) or returned an error | Check the connection; load again later; contact Wortise if it persists |
| `.timeout` | `TIMEOUT` | The ad took too long | Usually temporary: load again later |
| `.loadFailed` | `LOAD_FAILED` | The ad content could not be loaded | Temporary: load a new ad later |
| `.renderError` | `RENDER_ERROR` | The ad could not be rendered or presented | Show from a view controller on screen that is not presenting another one; load a new ad |
| `.unspecified` | `UNSPECIFIED` | Unknown | Read the logs |

## Common problems

| Symptom | Cause | Fix |
|---|---|---|
| Crash at launch, Google Mobile Ads mentions the application ID | `GADApplicationIdentifier` missing or invalid in `Info.plist` | Add the Google App ID from the Wortise dashboard |
| Crash when the App Tracking Transparency prompt is requested | `NSUserTrackingUsageDescription` missing in `Info.plist` | Add it (only apps that request the prompt need it) |
| `No such module 'WortiseSDK'` | `pod install` not run, the `.xcodeproj` opened instead of the `.xcworkspace`, or the SPM library not linked to the app target | Fix the dependency setup of the integration guide |
| `pod install` or package resolution fails on `Google-Mobile-Ads-SDK` | The app pins its own Google Mobile Ads version, incompatible with the SDK's | Let the SDK bring it; ask the user before changing their pin |
| Deployment target errors for `WortiseSDK` | The app targets an iOS below 15.0 | Raise it to 15.0 (ask the user first) |
| No delegate method is ever called | The delegate object was released (they are held weakly), `delegate` was never assigned, or the method signature differs from the documented one | Keep the delegate alive; copy the exact signatures |
| `showAd(from:)` does nothing | No ad loaded (still loading, failed, already shown), or the view controller is not on screen or is presenting another one | Check `isAvailable`, load in advance, reload after `didDismiss` |
| `loadAd()` gets no delegate call | The ad is already loading, it is in cooldown after a failure, or it was destroyed | Check `isInCooldown` and `cooldownRemaining`; retry later |
| Banners fill little or never with Google demand | `rootViewController` not set before `loadAd()` | Set it to the view controller that shows the banner |
| Banner does not refresh | It is not in a window, the app is in the background, or the refresh is disabled | Expected; it resumes when visible again |
| The consent dialog never appears | Not required for this user (outside the regions that need it, or already answered), or the form failed to load: the completion receives `false` in both cases | Expected when not required; request it from a view controller on screen |
| App open ads never show | `WAAppOpenManager.loadAd()` was never called; the manager only shows an ad already loaded | Call `loadAd()` once after getting the manager |
| An app open ad appears right after the consent or another system prompt | The manager was started before the prompts | Start it after the consent (and the App Tracking Transparency prompt, if any) |
| `render(ad:into:)` returns `false` | `headlineLabel` not connected, or the loader was destroyed | Connect the outlet; keep the loader until the screen closes |
| Native ads never show video | Media container smaller than 120x120 points, or without an explicit height | Enlarge it and give it a size constraint |
| A screen with ads is never freed (`deinit` not called) | Ads not destroyed, the native loader (which holds its delegate strongly) not set to `nil`, or a banner still holding its `rootViewController` | Release them as the `wortise-sdk-ad-formats` skill shows |
| Lower fill in an app for children | Child-directed treatment limits the demand: no personalized ads | Expected; check the setup against <https://docs.wortise.com/ios-sdk/privacy.md> |
