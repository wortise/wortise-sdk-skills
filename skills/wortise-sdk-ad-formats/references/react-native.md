# React Native: ad formats

Each format has a full page: <https://docs.wortise.com/react-native/sdk-integration/banner.md>,
`interstitial.md`, `rewarded.md`, `app-open.md` and `native.md` in the same folder, plus
<https://docs.wortise.com/react-native/best-practices.md>.

The examples import `AD_UNITS` and `adsReady` from the `ads.ts` module of the
`wortise-sdk-integrate` skill's React Native reference: every ad waits for initialization and
consent. They are TypeScript; in JavaScript drop the types.

## Rules that apply to every format

- **Getters are Promises.** `isAvailable`, `isShowing`, `isInCooldown`, `cooldownRemainingMs`
  (and `RNWortiseSdk.isInitialized`, `isReady`, `RNWortiseConsentManager.canCollectData`,
  `exists`) return a `Promise`, typed `any`. Always `await` them: `if (RNWortiseInterstitial.isAvailable)`
  is always true, and TypeScript does not catch it.
- **Listeners are subscriptions.** `addEventListener` adds a new listener on every call and
  returns a subscription: register it in `useEffect` and call `subscription.remove()` in the
  cleanup. Never call `removeAllListeners` from a component: it also removes other components'
  listeners.
- **Failure events** carry `{ name, message }`: `name` is the error code (`NO_FILL`, ...).

## Banner

`<WortiseBanner>` does not load by itself: call `loadAd()` through a ref, once.

```tsx
import { useEffect, useRef, useState } from 'react';
import { Dimensions } from 'react-native';
import { RNWortiseAdSize, WortiseBanner } from '@wortise/react-native-sdk';
import { AD_UNITS, adsReady } from './ads';

type BannerError = { message: string; name: string };

// 1.8.1 passes a native event: the data is in `nativeEvent`. Later versions also pass it at the
// top level, but keep `nativeEvent`, so reading it works with both.
function bannerError(event: unknown): BannerError {
  return (event as { nativeEvent: BannerError }).nativeEvent;
}

export function BottomBanner() {
  const ref = useRef<WortiseBanner>(null);

  // Adaptive sizes need an integer width
  const [adSize] = useState(() =>
    RNWortiseAdSize.getAnchoredAdaptiveBannerAdSize(Math.floor(Dimensions.get('window').width)),
  );

  useEffect(() => {
    adsReady.then(() => ref.current?.loadAd());
  }, []);

  return (
    <WortiseBanner
      ref={ref}
      adSize={adSize}
      adUnitId={AD_UNITS.banner}
      onFailedToLoad={event => {
        // The banner retries by itself: never call loadAd() here
        console.log('Banner failed to load', bannerError(event).name);
      }}
    />
  );
}
```

Rules:

- Call `loadAd()` once per mounted banner, after `adsReady`. Never from `onFailedToLoad`: the
  banner retries one minute after a failure and refreshes by itself.
- Listener data is in `event.nativeEvent` (`onFailedToLoad`, `onRevenuePaid`, `onSizeChange`).
  The 1.8.1 TypeScript types of `onFailedToLoad` and `onRevenuePaid` do not declare
  `nativeEvent`, so read it through a cast as above.
- Keep the banner mounted while its screen lives. Each mount (list rows, conditional blocks,
  remounts through a changing `key`) creates a new banner and a new request.
- `autoRefreshTime` is in **milliseconds**: 60000 by default, kept between 30000 and 120000;
  `WortiseBanner.AUTO_REFRESH_DISABLED` turns it off.
- Sizes: `RNWortiseAdSize.HEIGHT_50` (the default), `HEIGHT_90`, `HEIGHT_250`, `HEIGHT_280`;
  `getAnchoredAdaptiveBannerAdSize(width)` for banners at the top or bottom (prefer it there),
  `getInlineAdaptiveBannerAdSize(width, maxHeight)` inside scrolling content. A width with
  decimals makes the iOS banner never load: `Math.floor` it.
- Keep the banner clear of the system bars and the iOS home indicator, with the safe-area
  handling the app already uses (for example `react-native-safe-area-context`). On Android 15+,
  apps targeting SDK 35+ are drawn edge to edge, so a bottom banner otherwise sits under the
  navigation bar and gets accidental taps.
- Collapsible: anchored adaptive size plus `requestParameters={{ collapsible: 'bottom' }}` (or
  `'top'`, matching where the banner is). Only the first ad of the banner is collapsible.

## Interstitial and rewarded

`RNWortiseInterstitial`, `RNWortiseRewarded` and `RNWortiseAppOpen` are app-wide singletons that
hold one ad each:

- Call `setAdUnitId()` once, then `loadAd()`. `loadAd()` without an ad unit does nothing, and
  every `setAdUnitId()` call discards the loaded ad. Use one ad unit per format.
- Do it from an effect of a component that lives for the whole app (the root), never at module
  scope: on Android the ad is released when the activity is destroyed, and module code does not
  run again when it is recreated.
- `loadAd()` on an ad that is already loaded discards it. Load the next ad on dismissed.

```typescript
import { useEffect } from 'react';
import { RNWortiseInterstitial, RNWortiseRewarded } from '@wortise/react-native-sdk';
import { AD_UNITS, adsReady } from './ads';

const MIN_INTERSTITIAL_INTERVAL_MS = 60_000;

let lastInterstitialAt = 0;
let pendingReward: ((amount: number) => void) | null = null;
// Set before a fullscreen ad, read by the app open logic
export const appOpenGuard = { skipNext: false };

// Call it once, in the root component
export function useFullscreenAds() {
  useEffect(() => {
    let cancelled = false;
    const timers: ReturnType<typeof setTimeout>[] = [];

    // After a failed load the ad is in cooldown: retry later, never immediately
    const retryLater = async (ad: typeof RNWortiseInterstitial | typeof RNWortiseRewarded) => {
      const remaining: number = await ad.cooldownRemainingMs;
      timers.push(setTimeout(() => ad.loadAd(), Math.max(remaining, 30_000)));
    };

    const subscriptions = [
      RNWortiseInterstitial.addEventListener('onInterstitialDismissed', () =>
        RNWortiseInterstitial.loadAd(),
      ),
      RNWortiseInterstitial.addEventListener('onInterstitialFailedToShow', () =>
        RNWortiseInterstitial.loadAd(),
      ),
      RNWortiseInterstitial.addEventListener('onInterstitialFailedToLoad', () =>
        retryLater(RNWortiseInterstitial),
      ),
      RNWortiseRewarded.addEventListener('onRewardedCompleted', ({ amount, success }) => {
        if (success) {
          pendingReward?.(amount);
        }
        pendingReward = null;
      }),
      RNWortiseRewarded.addEventListener('onRewardedDismissed', () => RNWortiseRewarded.loadAd()),
      RNWortiseRewarded.addEventListener('onRewardedFailedToShow', () =>
        RNWortiseRewarded.loadAd(),
      ),
      RNWortiseRewarded.addEventListener('onRewardedFailedToLoad', () =>
        retryLater(RNWortiseRewarded),
      ),
    ];

    adsReady.then(() => {
      if (cancelled) {
        return;
      }
      RNWortiseInterstitial.setAdUnitId(AD_UNITS.interstitial);
      RNWortiseInterstitial.loadAd();

      RNWortiseRewarded.setAdUnitId(AD_UNITS.rewarded);
      RNWortiseRewarded.loadAd();
    });

    return () => {
      cancelled = true;
      timers.forEach(clearTimeout);
      subscriptions.forEach(subscription => subscription.remove());
    };
  }, []);
}

// Resolves when the app can continue: after the ad is dismissed, or at once if none was shown
export async function showInterstitial(): Promise<void> {
  if (Date.now() - lastInterstitialAt < MIN_INTERSTITIAL_INTERVAL_MS) {
    return;
  }
  if (!(await RNWortiseInterstitial.isAvailable)) {
    return;
  }

  lastInterstitialAt = Date.now();
  appOpenGuard.skipNext = true;

  await new Promise<void>(resolve => {
    const subscriptions = [
      RNWortiseInterstitial.addEventListener('onInterstitialDismissed', done),
      RNWortiseInterstitial.addEventListener('onInterstitialFailedToShow', done),
    ];

    function done() {
      subscriptions.forEach(subscription => subscription.remove());
      resolve();
    }

    // `true` does not mean that the ad was shown; `false` means that nothing happened
    RNWortiseInterstitial.showAd().then((called: boolean) => {
      if (!called) {
        done();
      }
    });
  });
}
```

`showRewarded(onReward)` is the same as `showInterstitial` with the `RNWortiseRewarded` events and
no interval, plus `pendingReward = onReward` before showing (in the same file). Offer the rewarded ad (the button)
only when `await RNWortiseRewarded.isAvailable` is true, and state the reward beforehand.

Rules:

- Call `await showInterstitial()` from the app's own flow, at a natural pause. Never call
  `showAd()` from an `on...Loaded` listener: with the reload on dismissed, the app would show ads
  one after another without any user action.
- `showAd()` with no ad available shows nothing and sends no event, and the value it resolves
  with does not tell whether the ad was shown. Check `isAvailable` first and continue the flow on
  dismissed and on failed to show.
- Grant the reward only in `onRewardedCompleted` when `success` is `true`, never on dismissed or
  shown. Define the amount in the app: `amount` and `label` may be empty.
- The interval above (one minute) is a starting point: tell the user the value you chose.
- Events: `onInterstitialClicked`, `Dismissed`, `FailedToLoad`, `FailedToShow`, `Impression`,
  `Loaded`, `RevenuePaid`, `Shown` (same suffixes with `onRewarded...` and `onAppOpen...`, plus
  `onRewardedCompleted`).

## App open

There is no automatic manager in React Native: show the ad with an `AppState` listener when the
app returns from the background.

```typescript
import { useEffect, useRef } from 'react';
import { AppState } from 'react-native';
import { RNWortiseAppOpen } from '@wortise/react-native-sdk';
import { AD_UNITS, adsReady } from './ads';
import { appOpenGuard } from './fullscreen';

// Call it once, in the root component
export function useAppOpenAds() {
  const appState = useRef(AppState.currentState);

  useEffect(() => {
    adsReady.then(() => {
      RNWortiseAppOpen.setAdUnitId(AD_UNITS.appOpen);
      RNWortiseAppOpen.setAutoReload(true);
      RNWortiseAppOpen.loadAd();
    });

    const subscription = AppState.addEventListener('change', nextState => {
      if (appState.current === 'background' && nextState === 'active') {
        // On Android, closing an interstitial or rewarded ad also returns from the background
        if (!appOpenGuard.skipNext) {
          RNWortiseAppOpen.tryToShowAd();
        }
        appOpenGuard.skipNext = false;
      }
      appState.current = nextState;
    });

    return () => subscription.remove();
  }, []);
}
```

- `appOpenGuard.skipNext` is the flag that `showInterstitial`/`showRewarded` set before showing: without
  it, Android shows an app open ad right after every fullscreen ad.
- `setAutoReload(true)` (after `setAdUnitId`) loads a new ad after each one closes.
  `tryToShowAd()` shows the ad if it is loaded and loads one otherwise.
- Any return counts, including coming back after tapping an ad. If the app needs to limit how
  often these ads appear, add a minimum interval as for interstitials.

## Native

The ad view is native: register a factory on **both** platforms with the same id, then display
it with `<WortiseNativeAdView factoryId>`. An unknown id renders an empty view with no error.
The native page has the factories: Android implements
`com.wortise.ads.react.WortiseNativeAdViewFactory` (returning `NativeAdView(context, binder)`)
and registers it with `WortiseNativeAds.registerFactory("my-factory", ...)` in
`MainApplication.onCreate`; iOS subclasses `WANativeAdView`, implements
`WortiseNativeAdViewFactory` (`import RNWortiseSdk`) and registers it in the `AppDelegate`'s
`application(_:didFinishLaunchingWithOptions:)`.

```tsx
import { useEffect, useState } from 'react';
import { WortiseNativeAd, WortiseNativeAdView } from '@wortise/react-native-sdk';
import { AD_UNITS, adsReady } from './ads';

export function NativeAdSlot() {
  const [nativeAd, setNativeAd] = useState<WortiseNativeAd | null>(null);

  useEffect(() => {
    let cancelled = false;
    let current: WortiseNativeAd | null = null;
    let retry: ReturnType<typeof setTimeout> | undefined;

    const load = () => {
      WortiseNativeAd.createForAdRequest(AD_UNITS.native)
        .then(ad => {
          if (cancelled) {
            ad.destroy();
            return;
          }
          current = ad;
          setNativeAd(ad);
        })
        .catch(() => {
          // Every request creates a new loader, so no cooldown protects it: wait before retrying
          if (!cancelled) {
            retry = setTimeout(load, 30_000);
          }
        });
    };

    adsReady.then(() => {
      if (!cancelled) {
        load();
      }
    });

    return () => {
      cancelled = true;
      clearTimeout(retry);
      current?.destroy();
    };
  }, []);

  if (!nativeAd) {
    return null;
  }

  return (
    <WortiseNativeAdView
      factoryId="my-factory"
      nativeAd={nativeAd}
      style={{ height: 300, width: '100%' }}
    />
  );
}
```

Rules:

- `destroy()` every native ad that is no longer displayed, including on unmount; destroy the
  previous one before replacing it.
- Give `WortiseNativeAdView` an explicit size: the native view fills it.
- The media view of the native layouts must measure at least 120x120 dp (points on iOS; it needs
  an explicit height or aspect ratio there), and the layouts must show an "Ad" label.
- In Expo, the factories live in `android/` and `ios/`: `npx expo prebuild --clean` deletes them.
  If the project does not keep those folders in version control, ask the user how to proceed
  before adding native ads.
