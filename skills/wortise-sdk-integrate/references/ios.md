# iOS: set up the Wortise SDK

Full guide: <https://docs.wortise.com/ios-sdk/sdk-integration.md>. It is the source of truth for
versions and requirements; read it when a value here looks outdated. The examples are in Swift
with UIKit, as in the documentation.

## Requirements

| Item | Minimum |
|---|---|
| Xcode | 15.0 |
| iOS deployment target | 15.0 |

Check the deployment target in the target's build settings (`IPHONEOS_DEPLOYMENT_TARGET` in
`project.pbxproj`) and, with CocoaPods, in the `platform :ios` line of the `Podfile`. Raising it
drops devices: ask the user first. Below the minimum, `pod install`, the package resolution or
the build fails.

## Dependency

Use the dependency manager the project already uses: a `Podfile` means CocoaPods; packages in the
Xcode project (`Package.resolved`) or a `Package.swift` mean Swift Package Manager. Add Wortise
with only one of them.

**CocoaPods.** In the `Podfile`, with the version from the integration page:

```ruby
platform :ios, '15.0'

target 'MyApp' do
  use_frameworks!

  pod 'WortiseSDK', '>= <version>'
end
```

Then run `pod install` and open the `.xcworkspace`, not the `.xcodeproj`.

**Swift Package Manager.** Add the package `https://github.com/wortise/wortise-ios-sdk-spm` with
the version from the integration page and link the `WortiseSDK` library to the app target. It
includes every mediation adapter, like the pod. The package also offers one library per adapter:
use them only if the user asks for a subset.

- Do not add `Google-Mobile-Ads-SDK`, Google mediation adapters (`GoogleMobileAdsMediation...`)
  or ad network SDKs for Wortise: the SDK already brings the versions it was tested with. If the
  app already uses Google Mobile Ads directly (for example, for AdMob), tell the user: a version
  pinned in the `Podfile` or the package dependencies can conflict with the one Wortise needs.
- Ignore versions with `-alpha`, `-beta` or `-rc` unless the user asks for them.

## Info.plist

Add the Google App ID to the app target's `Info.plist`:

```xml
<key>GADApplicationIdentifier</key>
<string>ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY</string>
```

Without it, Google Mobile Ads crashes the app at launch. If the key already exists (an app that
used AdMob), do not overwrite it silently: ask the user, because the value must be the Google App
ID that the Wortise dashboard shows for the app.

In the same file, add the `SKAdNetworkItems` array exactly as the integration page lists it:
<https://docs.wortise.com/ios-sdk/sdk-integration.md#setup-infoplist>. Copy it from that page,
which has the current list, rather than from memory. If the app already has `SKAdNetworkItems`,
merge the identifiers without duplicates.

Projects created with recent Xcode versions may have no `Info.plist` file (`GENERATE_INFOPLIST_FILE`
with `INFOPLIST_KEY_...` build settings). Add these keys in the target's **Info** tab, which
creates the file, or point `INFOPLIST_FILE` to a new one: the `INFOPLIST_KEY_` settings do not
support them.

## Imports

Every public type is in one module:

```swift
import WortiseSDK
```

The main ones are `WortiseAds`, `WAAdSettings`, `WAConsentManager`, `WABannerAd`, `WAAdSize`,
`WARequestParameters`, `WAInterstitialAd`, `WARewardedAd`, `WAReward`, `WAAppOpenAd`,
`WAAppOpenManager`, `WANativeAdLoader`, `WANativeAd`, `WANativeAdView`, `WAAdError`,
`WARevenueData` and `WortiseLog`, with the delegate protocols `WABannerDelegate`,
`WAInterstitialDelegate`, `WARewardedDelegate`, `WAAppOpenDelegate` and `WANativeDelegate`.

## Initialization

Initialize once, at launch, in `application(_:didFinishLaunchingWithOptions:)`, on the main
thread: the SDK works with UIKit and does not switch threads by itself.

```swift
import UIKit
import WortiseSDK

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        #if DEBUG
        WAAdSettings.testEnabled = true
        #endif

        WortiseAds.shared.initialize(assetKey: "<app key>")

        return true
    }
}
```

- Apps for children set `WAAdSettings.childDirected` and `WAAdSettings.maxAdContentRating`
  before this call: see <https://docs.wortise.com/ios-sdk/privacy.md> and ask the user which
  values apply.
- Calling `initialize` again has no effect.
- `WortiseAds.shared.wait { }` runs its closure once the initialization finishes, or right away
  if it already has.
- An ad requested before the first `initialize` fails with `sdkNotInitialized`
  (`SDK_NOT_INITIALIZED`). The SDK stores the app key, so this usually shows only on a fresh
  install.

## Test mode

`WAAdSettings.testEnabled = true` makes every request a test request, which never earns revenue.
The value is not saved, so set it on every launch before loading ads, and only in debug builds,
inside `#if DEBUG` as above. Xcode defines `DEBUG` for the Debug configuration
(`SWIFT_ACTIVE_COMPILATION_CONDITIONS`); check that the project still does, and that the Release
configuration does not.

To use the test ad units only in debug builds, define the ad unit IDs per build configuration,
so a release build can never ship a `test-` ID. Until the user has real IDs, leave the release
value as an obvious placeholder: those ads fail to load instead of earning nothing silently.

```swift
enum AdUnits {
    #if DEBUG
    static let banner       = "test-banner"
    static let interstitial = "test-interstitial"
    #else
    static let banner       = "<banner ad unit ID>"
    static let interstitial = "<interstitial ad unit ID>"
    #endif
}
```

## Consent

The SDK never shows the consent dialog by itself. Request it from the first view controller once
it is on screen (the dialog is presented from it), and load the ads when it finishes:

```swift
final class MainViewController: UIViewController {

    private var isConsentRequested = false

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        if isConsentRequested {
            return
        }

        isConsentRequested = true

        WortiseAds.shared.wait { [weak self] in
            guard let self else { return }

            WAConsentManager.shared.request(ifRequired: self) { [weak self] _ in
                self?.loadAds()
            }
        }
    }

    private func loadAds() {
        // Create and load the ads of this screen
    }
}
```

- The completion is always invoked. Its `Bool` tells whether the dialog was shown, not what the
  user chose.
- Give the user a way to change their choice later: a "Privacy choices" option in the app's
  settings or menu that calls `WAConsentManager.shared.request(self)`, which always shows the
  dialog. Google's own dialog tells users to look for it. Do not call it at startup.
- If the app already uses an IAB TCF consent platform, do not add this request: the SDK reads the
  consent that platform stores.

Guide: <https://docs.wortise.com/ios-sdk/user-consent.md>.

## App Tracking Transparency (optional)

The SDK never shows the App Tracking Transparency prompt and works without it. Add it only if the
user wants it; do not suggest it. Then:

1. Add `NSUserTrackingUsageDescription` to `Info.plist`, with the text of the prompt. Requesting
   the authorization without it crashes the app.
2. Request it after the consent request finishes and before the first ad request, from a view
   controller on screen:

```swift
import AppTrackingTransparency

WAConsentManager.shared.request(ifRequired: self) { [weak self] _ in
    ATTrackingManager.requestTrackingAuthorization { _ in
        DispatchQueue.main.async {
            self?.loadAds()
        }
    }
}
```

Guide: <https://docs.wortise.com/ios-sdk/sdk-integration.md#app-tracking-transparency>.
