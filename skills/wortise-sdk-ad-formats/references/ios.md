# iOS: ad formats

Each format has a full page with every delegate method: <https://docs.wortise.com/ios-sdk/sdk-integration/banner.md>,
`interstitial.md`, `rewarded.md`, `app-open.md` and `native.md` in the same folder. The examples
below are in Swift with UIKit. Everything runs on the main thread.

Imports and per-configuration ad unit IDs (`AdUnits`): see the `wortise-sdk-integrate` skill's
iOS reference.

## Closing a screen

Every ad must be destroyed when the screen that owns it closes: the ad and its internal module
reference each other, so neither releasing the ad nor removing its view frees it. The format
pages destroy ads in `willMove(toParent:)`, which only runs when a navigation controller pops the
screen. This check also covers modal screens:

```swift
override func viewDidDisappear(_ animated: Bool) {
    super.viewDidDisappear(animated)

    if isMovingFromParent || isBeingDismissed || navigationController?.isBeingDismissed == true {
        releaseAds()
    }
}
```

Screens that live as long as the app (the root screen, a tab) do not need it.

## Delegates

`WABannerAd`, `WAInterstitialAd`, `WARewardedAd`, `WAAppOpenAd` and `WAAppOpenManager` hold
their `delegate` weakly: use an object that something else keeps alive, usually the view
controller. An object created only to be the delegate is released at once and no event arrives.
Every method except `WANativeDelegate.didLoad(nativeAd:)` is optional, so a method with a
slightly different signature compiles and is never called: copy the signatures from the format
pages.

## Banner

Create the banner once, after the consent request finishes (or in `viewDidLoad` on a later
screen), never in `viewWillAppear`, `viewDidLayoutSubviews` or cell configuration code. For a
banner at the top or bottom of the screen, prefer the anchored adaptive size to `height50`.
Without an `adSize`, the banner uses `matchView`, the size of its own view: a banner placed in a
storyboard or XIB then needs constraints that give it a width and a height, or the ad is invisible.

```swift
final class HomeViewController: UIViewController {

    private var bannerAd: WABannerAd?

    private func loadBanner() {
        if bannerAd != nil {
            return
        }

        let width  = view.bounds.inset(by: view.safeAreaInsets).width
        let adSize = WAAdSize.getAnchoredAdaptiveBannerAdSize(width: width)
        let banner = WABannerAd(frame: .zero)

        banner.adUnitId           = AdUnits.banner
        banner.adSize             = adSize
        banner.delegate           = self
        banner.rootViewController = self

        banner.translatesAutoresizingMaskIntoConstraints = false

        view.addSubview(banner)

        NSLayoutConstraint.activate([
            banner.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor),
            banner.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor),
            banner.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            banner.heightAnchor.constraint(equalToConstant: adSize.height)
        ])

        banner.loadAd()

        bannerAd = banner
    }

    private func releaseAds() {
        bannerAd?.destroy()
        bannerAd?.rootViewController = nil
        bannerAd = nil
    }
}

extension HomeViewController: WABannerDelegate {

    func didFailToLoad(bannerAd: WABannerAd, error: WAAdError) {
        // Nothing to do: the banner retries by itself
    }
}
```

A banner can also be placed in a storyboard or XIB (custom class `WABannerAd`, module
`WortiseSDK`) and configured through an outlet. It does not load until `loadAd()` is called.

Rules:

- Set `rootViewController` before `loadAd()`. Without it Google's demand cannot load and the
  banner fills less, or not at all. The banner keeps a strong reference to it: set it to `nil`
  after `destroy()`, as `releaseAds()` does, so the view controller can be freed.
- Keep the banner inside the safe area, as above, so it does not sit under the home indicator or
  the status bar.
- The banner refreshes by itself: every 60 seconds by default, or `autoRefreshTime` in seconds
  (between 30 and 120; `WABannerAd.autoRefreshDisabled` turns it off). The refresh pauses while
  the banner is not in a window or the app is in the background.
- After a failed load it retries 60 seconds later. Never call `loadAd()` in `didFailToLoad`.
- Adaptive sizes: `WAAdSize.getAnchoredAdaptiveBannerAdSize(width:)` (or `(container:)`) for
  banners anchored to the top or bottom, and `WAAdSize.getInlineAdaptiveBannerAdSize(width:maxHeight:)`
  for banners inside scrolling content.
- Collapsible: needs an anchored adaptive size, and applies to the first ad only:
  `banner.loadAd(parameters: WARequestParameters(collapsible: .bottom))` (`.top` for a banner at
  the top).

## Interstitial and rewarded

Both work the same way; rewarded adds `didComplete(rewardedAd:reward:)`.

```swift
final class GameViewController: UIViewController {

    private var rewardedAd: WARewardedAd?

    override func viewDidLoad() {
        super.viewDidLoad()
        // ...
        let ad = WARewardedAd(adUnitId: AdUnits.rewarded)

        ad.delegate = self
        ad.loadAd()

        rewardedAd = ad
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)

        if isMovingFromParent || isBeingDismissed || navigationController?.isBeingDismissed == true {
            rewardedAd?.destroy()
            rewardedAd = nil
        }
    }

    func watchAdTapped() {
        guard let ad = rewardedAd, ad.isAvailable else {
            continueWithoutReward()
            return
        }

        ad.showAd(from: self)
    }
}

extension GameViewController: WARewardedDelegate {

    func didComplete(rewardedAd: WARewardedAd, reward: WAReward) {
        if reward.success {
            grantReward()
        }
    }

    func didDismiss(rewardedAd: WARewardedAd) {
        resumeGame()
        rewardedAd.loadAd()
    }

    func didFailToShow(rewardedAd: WARewardedAd, error: WAAdError) {
        resumeGame()
        rewardedAd.loadAd()
    }

    func didFailToLoad(rewardedAd: WARewardedAd, error: WAAdError) {
        // The ad is in cooldown after a failure: retry later, not now
        let delay = max(rewardedAd.cooldownRemaining, 30)

        DispatchQueue.main.asyncAfter(deadline: .now() + delay) { [weak self] in
            self?.rewardedAd?.loadAd()
        }
    }

    // The other methods (didClick, didImpress, didLoad, didPayRevenue, didShow) are optional
}
```

For an interstitial, use `WAInterstitialAd` and `WAInterstitialDelegate`, whose methods are
`didClick(interstitialAd:)`, `didDismiss(interstitialAd:)`, `didFailToLoad(interstitialAd:error:)`,
`didFailToShow(interstitialAd:error:)`, `didImpress(interstitialAd:)`, `didLoad(interstitialAd:)`,
`didPayRevenue(interstitialAd:data:)` and `didShow(interstitialAd:)`. App open ads follow the
same pattern with `WAAppOpenDelegate` (`...(appOpenAd:)`).

Never call `showAd(from:)` from `didLoad`: with the reload in `didDismiss`, the app would show
ads one after another without any user action.

Rules:

- `showAd(from:)` with no ad available does nothing and invokes no delegate method (it logs
  `showAd(from:) called with no ad available`).
- Show it from a view controller that is on screen and not presenting another one.
- `loadAd()` on an ad that is already loaded discards it and requests another one. `loadAd()`
  while a load is in progress, or during the cooldown (30 seconds by default after a failure), is
  ignored without any delegate call. `isInCooldown` and `cooldownRemaining` (seconds) tell when it
  can load again.
- `WAReward` has `success`, `amount` (`Int`) and `label` (`String?`). Define the reward amount in
  the app: depending on the ad, `amount` may be `0` and `label` `nil`.
- An ad can be shown once: load the next one after `didDismiss`.

## App open

Choose one of the two ways, never both at once.

**`WAAppOpenManager`** shows an ad every time the app becomes active, if one is loaded, and loads
the next one when it closes. Call `loadAd()` once, after the consent request (and the App Tracking
Transparency prompt, if the app shows it) has finished: closing a system dialog also makes the
app active, so a manager started earlier could show an ad right after it.

```swift
// For example, in the consent completion of the first screen
WAAppOpenManager.getInstance(adUnitId: AdUnits.appOpen).loadAd()
```

`getInstance(adUnitId:)` always returns the same manager for an ad unit, so the app does not need
to keep it. The first ad appears on the next return to the app. Any return counts, including
coming back after tapping an ad: if the app needs to limit how often these ads appear, use
`WAAppOpenAd` instead.

**`WAAppOpenAd`** gives control over when to show it: `WAAppOpenAd(adUnitId:)`, with
`autoReload = true` to load a new ad after each one closes, `isAvailable` + `showAd(from:)`, or
`tryToShowAd(from:)`, which shows the ad if it is loaded and loads one otherwise. Destroy it with
its screen like the other fullscreen ads.

## Native

The native page has the complete example: <https://docs.wortise.com/ios-sdk/sdk-integration/native.md>.
In short:

1. A view whose class is `WANativeAdView` (usually a XIB), with its outlets connected:
   `headlineLabel`, `bodyLabel`, `callToActionButton`, `iconImageView`, `mediaContainer`, and
   the others the design needs.
2. A `WANativeAdLoader(adUnitId:delegate:)` with `rootViewController = self`, kept in a property,
   and `loadAd()`.
3. In `didLoad(nativeAd:)`: destroy the previous `WANativeAd`, keep the new one and call
   `nativeAdLoader?.render(ad: nativeAd, into: adView)`, which returns `false` if it could not
   render.
4. In `didFailToLoad(nativeAd:)` (it receives only the `WAAdError`), retry later as with the
   fullscreen ads, using `nativeAdLoader.cooldownRemaining`.
5. When the screen closes: destroy the `WANativeAd` and the loader, and set the loader to `nil`:

```swift
nativeAd?.destroy()
nativeAdLoader?.destroy()
nativeAdLoader = nil
```

Rules:

- The loader keeps a strong reference to its delegate. When the delegate is the view controller
  that owns the loader, `deinit` never runs: release the loader explicitly as above.
- `headlineLabel` must be connected: without it `render(ad:into:)` returns `false` and logs
  `Native ad failed to render`.
- In this version `optionsContainer` is declared as `UILabel?`, although the native page lists
  it as `UIView`: connect a `UILabel` to it.
- The media container must measure at least 120x120 points, with an explicit height or aspect
  ratio constraint (it has no intrinsic size). Smaller ones do not receive video, and the SDK
  logs a warning with the size.
- The layout must show that the content is an ad, for example with an "Ad" label.
- `WAGoogleNativeAd` is deprecated: use `WANativeAdLoader` in new code.
