# Flutter: ad formats

Each format has a full page: <https://docs.wortise.com/flutter-sdk/sdk-integration/banner.md>,
`interstitial.md`, `rewarded.md`, `app-open.md` and `native.md` in the same folder, and
<https://docs.wortise.com/flutter-sdk/best-practices.md> explains the lifecycle. Everything comes
from `import 'package:wortise/wortise.dart';`.

## How the plugin works

- **Events** arrive in one listener per ad, `void Function(<Format>Event event, dynamic args)`.
  Failure events carry `args['error']` with the error name (`NO_FILL`, ...).
- **Fullscreen ads and native loaders are keyed by ad unit ID** on the native side and in the
  event channel. Two Dart objects with the same ID share one native ad: the newer one takes the
  events of the older one, and `destroy()` on either destroys both. So:
  - Create one object per ad unit ID, once, in `initState` (never in `build()`), keep it in a
    field and `destroy()` it in `dispose()`.
  - An ad unit used on several screens needs a single object for the whole app (for example, in a
    service created once at startup), not one per screen: with `pushReplacement` the new screen's
    `initState` runs before the old screen's `dispose`, whose `destroy()` would kill the new ad.
- **`switch` statements over the event enums need a `default:` case.** Later versions add events,
  and Dart rejects a `switch` that does not cover them all.
- Only what the released version contains exists. `reloadAd`, `autoRefresh` or a `REFRESHED`
  event appear only in later versions: check the installed package
  (`~/.pub-cache/hosted/pub.dev/wortise-<version>/lib/src/`) before using anything not shown here.

## Banner

`BannerAd` is a widget that loads its ad as soon as it is built, so add it only after the SDK is
initialized and consent is resolved (see the `wortise-sdk-integrate` skill).

```dart
SafeArea(
  child: SizedBox(
    height: 50,
    child: BannerAd(adUnitId: AdIds.banner), // AdSize.HEIGHT_50 by default
  ),
)
```

Anchored adaptive and collapsible:

```dart
final width = MediaQuery.sizeOf(context).width.toInt();

ConstrainedBox(
  constraints: BoxConstraints(maxHeight: maxHeight),
  child: BannerAd(
    adUnitId: AdIds.banner,
    adSize: AdSize.getAnchoredAdaptiveBannerAdSize(width),
    requestParameters: const RequestParameters(
      collapsible: CollapsiblePosition.bottom,
    ),
  ),
)
```

Rules:

- **Bounded height.** With `AdSize.MATCH_VIEW` or an adaptive size, the widget takes all the
  height its parent allows until the ad loads, then shrinks to the ad (`args['adHeight']` in
  `LOADED`). In a `Column`, `ListView` or other unbounded parent that is a layout error: wrap it in
  a `SizedBox`, `Container` or `ConstrainedBox` with a height.
- **The widget's state is the banner.** Each new `State` (a changed `key`, a different parent, a
  list item rebuilt after scrolling) creates a new native banner and a new request. Changing
  `adUnitId`, `adSize`, `autoRefreshTime` or `requestParameters` later does nothing: decide them
  before adding it. In scrolling lists set `keepAlive: true`.
- **No destroy call:** the native banner is destroyed when the widget leaves the tree.
- **Refresh and retry are automatic** (every 60 seconds by default, again after a failed load).
  Never rebuild or re-key the widget from `FAILED_TO_LOAD`.
- `autoRefreshTime` is in **milliseconds**, between `30000` and `120000`;
  `BannerAd.AUTO_REFRESH_DISABLED` turns it off.
- Sizes: `AdSize.HEIGHT_50`, `HEIGHT_90`, `HEIGHT_250`, `HEIGHT_280`,
  `AdSize.getAnchoredAdaptiveBannerAdSize(widthDp)` for banners anchored to the top or bottom,
  `AdSize.getInlineAdaptiveBannerAdSize(widthDp, maxHeightDp)` inside scrolling content.
- Collapsible needs the anchored adaptive size, applies to the first ad only, and its position
  (`CollapsiblePosition.top` or `bottom`) must match where the banner is.
- Keep it clear of the system bars: put it in a `SafeArea` (or `Scaffold.bottomNavigationBar`
  with a `SafeArea`).

## Interstitial and rewarded

Both work the same way; rewarded adds `COMPLETED`.

```dart
import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:wortise/wortise.dart';

class _GameScreenState extends State<GameScreen> {
  late final RewardedAd _rewardedAd;

  Timer? _retryTimer;

  @override
  void initState() {
    super.initState();

    _rewardedAd = RewardedAd('<ad unit ID>', listener: _onRewardedEvent)
      ..loadAd();
  }

  @override
  void dispose() {
    _retryTimer?.cancel();
    _rewardedAd.destroy();

    super.dispose();
  }

  Future<void> onWatchAdPressed() async {
    // false: nothing will be shown and no event will arrive
    if (!await _rewardedAd.showAd()) {
      _continueWithoutReward();
    }
  }

  void _onRewardedEvent(RewardedAdEvent event, dynamic args) {
    switch (event) {
      case RewardedAdEvent.COMPLETED:
        if (args['success'] == true) {
          _grantReward();
        }
        break;

      case RewardedAdEvent.DISMISSED:
      case RewardedAdEvent.FAILED_TO_SHOW:
        _resumeGame();
        _rewardedAd.loadAd();
        break;

      case RewardedAdEvent.FAILED_TO_LOAD:
        _retryLater();
        break;

      default:
        // CLICKED, IMPRESSION, LOADED, REVENUE_PAID, SHOWN (and events of later versions)
        break;
    }
  }

  // The ad is in cooldown after a failure: retry later, not now
  Future<void> _retryLater() async {
    final cooldown = await _rewardedAd.cooldownRemainingMs;

    if (!mounted) return;

    _retryTimer?.cancel();
    _retryTimer = Timer(
      Duration(milliseconds: max(cooldown, 30000)),
      _rewardedAd.loadAd,
    );
  }

  // ...
}
```

For an interstitial, use `InterstitialAd('<ad unit ID>', listener: ...)` and `InterstitialAdEvent`
(`CLICKED`, `DISMISSED`, `FAILED_TO_LOAD`, `FAILED_TO_SHOW`, `IMPRESSION`, `LOADED`,
`REVENUE_PAID`, `SHOWN`). Space interstitials out in the app's flow:

```dart
static const _minInterval = Duration(minutes: 1);

static DateTime? _lastShown;

static Future<void> showIfSpaced(InterstitialAd ad, void Function() next) async {
  final last = _lastShown;

  final spaced = last == null || DateTime.now().difference(last) >= _minInterval;

  if (spaced && await ad.showAd()) {
    _lastShown = DateTime.now();
    return; // continue in DISMISSED / FAILED_TO_SHOW
  }

  next();
}
```

Never call `showAd()` from `LOADED`: with the reload in `DISMISSED`, the app would show ads one
after another without any user action.

Rules:

- `showAd()` completes with `false` when there is no ad (or no screen to show it on); then no
  `DISMISSED` or `FAILED_TO_SHOW` arrives, so the app must continue by itself. `isAvailable` is
  also available, as a `Future<bool>`.
- `loadAd()` on an ad that is already loaded discards it. `reloadOnDismissed: true` reloads after
  `DISMISSED` only; `FAILED_TO_SHOW` still needs a `loadAd()`.
- Grant the reward only in `COMPLETED` with `args['success'] == true`. Define the amount in the
  app: `args['amount']` and `args['label']` may be empty.
- `cooldownRemainingMs` and `isInCooldown` exist on interstitial, rewarded and app open ads and
  on `NativeAdLoader`.
- Android: fullscreen ads need the app's `Activity`. `loadAd()` without one (background
  isolate, before the UI exists) throws a `PlatformException` with `ACTIVITY_NOT_AVAILABLE`.

## App open

Choose one of the two ways, never both for the same ad unit.

**`AppOpenManager.register`** shows the ad every time the app returns to the foreground while an
ad is loaded, with no frequency cap; it never loads one, hence `autoReload: true`. The first launch
only loads it. Any return counts, including coming back after tapping an ad. Register it once,
after initialization and consent; a new registration replaces the previous one, and the plain
`AppOpenManager(ad)` constructor never shows anything.

```dart
_appOpenManager = AppOpenManager.register(
  AppOpenAd('<ad unit ID>', autoReload: true)..loadAd(),
);
// When it is no longer wanted: _appOpenManager.destroy();
```

**Manual**: `AppOpenAd('<ad unit ID>', autoReload: true, listener: ...)`, then `loadAd()` and, at
the moment the app chooses, `showAd()` or `tryToShowAd()` (shows the loaded ad, or loads one if
there is none; it completes with `false` if `loadAd()` was never called). Use it when the app needs
to limit how often these ads appear.

## Native

Native ads are native views: the app registers a factory on each platform, with the same ID that
Dart passes to `NativeAdLoader(adUnitId, factoryId, listener)`. Copy the Android
`NativeAdViewFactory` (registered in `MainActivity.configureFlutterEngine` with
`NativeAdManager.registerAdViewFactory`) and the iOS `WortiseNativeAdViewFactory` (registered in
`AppDelegate` with `WortiseNativeAdManager.registerAdViewFactory`) from the native page. An unknown
ID makes `loadAd()` throw a `PlatformException` (`Can't find NativeAdViewFactory with id`).

```dart
class _NativeAdCardState extends State<NativeAdCard> {
  late final NativeAdLoader _loader;

  NativeAd? _nativeAd;

  @override
  void initState() {
    super.initState();

    _loader = NativeAdLoader('<ad unit ID>', 'my-factory', _onNativeEvent);
    _loader.loadAd();
  }

  @override
  void dispose() {
    _nativeAd?.destroy();
    _loader.destroy();

    super.dispose();
  }

  void _onNativeEvent(NativeAdEvent event, dynamic args) {
    if (event == NativeAdEvent.LOADED) {
      final ad = args['ad'] as NativeAd;

      if (!mounted) {
        ad.destroy();
        return;
      }

      final previous = _nativeAd;

      setState(() => _nativeAd = ad);

      // Destroy the replaced ad once its AdWidget has left the screen
      WidgetsBinding.instance.addPostFrameCallback((_) => previous?.destroy());
    } else if (event == NativeAdEvent.FAILED) {
      // Retry later (cooldownRemainingMs), never right away
    }
  }

  @override
  Widget build(BuildContext context) {
    final ad = _nativeAd;

    if (ad == null) return const SizedBox.shrink();

    return SizedBox(
      height: 300,
      child: AdWidget(key: ValueKey(ad.adId), ad: ad),
    );
  }
}
```

Rules:

- The failure event is `NativeAdEvent.FAILED` (not `FAILED_TO_LOAD`); it has the same cooldown.
- Each `LOADED` delivers a new `NativeAd`. Destroy the one it replaces, and destroy both the ad
  and the loader in `dispose()`: destroying the loader does not destroy its ads.
- A loader is keyed by ad unit ID too: several native slots of one ad unit (a feed) need one
  loader, created once outside the slots, whose `LOADED` ads the app hands to the slots. A loader
  per slot makes the slots steal each other's ads.
- `AdWidget` needs a bounded height, like the banner; `keepAlive: true` in scrolling lists.
- The native layout must show an "Ad" label, and its media view must measure at least 120x120
  (dp on Android, points on iOS) to receive video.
- `GoogleNativeAd` is deprecated: use `NativeAdLoader` in new code.
