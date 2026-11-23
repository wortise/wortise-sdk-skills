---
name: wortise-sdk-ad-formats
description: Adds or changes Wortise ad placements - banners (adaptive and collapsible), interstitials, rewarded ads, app open ads and native ads - following the SDK lifecycle, so ads load ahead of time, show only when available, grant rewards correctly and are released with their screens. Use it whenever the user wants to add, move, change or remove a Wortise ad, or asks how a Wortise ad format works. The SDK must be set up first (wortise-sdk-integrate).
---

# Add Wortise ad placements

The SDK must already be set up and initialized: if it is not, follow the `wortise-sdk-integrate`
skill first. Then read the platform reference for the code:

| Platform | Reference |
|---|---|
| Android | [references/android.md](references/android.md) |
| iOS | [references/ios.md](references/ios.md) |
| Flutter | [references/flutter.md](references/flutter.md) |
| React Native | [references/react-native.md](references/react-native.md) |
| Unity | [references/unity.md](references/unity.md) |

## Choose the format and the moment

The ads come from Google Ad Manager and other networks, whose policies apply to the app. A badly
placed ad causes accidental clicks, which the networks treat as invalid traffic.

| Format | Show it | Avoid |
|---|---|---|
| Banner | In an area of the screen reserved for it, usually at the top or bottom | Covering content or buttons; many banners on one screen |
| Interstitial | At a natural pause: between levels, after finishing a task | At launch, when leaving the app, in the middle of an action, right after another ad, on every navigation |
| Rewarded | When the user chooses to watch it in exchange for a reward stated beforehand | Showing it without the user asking; an unclear reward |
| App open | When the user comes back to the app | On top of another ad; right after a system dialog |
| Native | Inside the app's content, with the look of the app | Hiding that it is an ad: it must carry a visible "Ad" label |

When the user asks for a placement in the "avoid" column, explain the risk and suggest an
alternative before writing it.

- **Space interstitials out.** Even at natural pauses, do not show one at every pause: keep a
  minimum time between interstitials (for example, a minute) and tell the user the value you chose
  so they can tune it. Never block the user's action to wait for an ad.
- **Keep ads clear of the system bars and of tappable controls.** On screens drawn edge to edge,
  handle the insets so a banner does not sit under the navigation or status bar.

## Lifecycle rules

These hold on every platform. The platform reference shows how to apply them.

1. **One ad object per placement, created once.** Never create ads in render, layout or
   recomposition code: every new object sends new requests.
2. **Load ahead.** Load fullscreen ads after the SDK is initialized and consent is resolved, and
   show them later. An ad requested at the moment it should appear will not be ready.
3. **Do not load an ad that is already loaded.** It discards an ad that was ready. Load the next
   one when the current one is dismissed (or let app open ads reload by themselves).
4. **Show only when available.** Check that the ad is available before showing it, and make the
   app continue when it is not, when the ad is dismissed and when it fails to show: no callback
   arrives when there is nothing to show.
5. **Reward only on completion.** Grant the reward in the "completed" callback (when it reports
   success), never on "dismissed" or "shown".
6. **No immediate retries.** After a failed load, fullscreen and native ads enter a cooldown
   (30 seconds by default) during which new loads are ignored. Retry later, never in a loop.
   Banners retry and refresh by themselves: never call their load method from their failure
   callback. Use a retry timer that keeps running while the app or game is paused.
7. **Release with the screen.** Destroy banners, fullscreen ads, native ads and native ad loaders
   when the screen that owns them is destroyed.
8. **Banner refresh times** are kept between 30 and 120 seconds: a value outside the range, for
   example seconds written where milliseconds are expected, ends up at one of the limits.
9. **Collapsible banners** need an anchored adaptive size, and only the first ad of the banner is
   collapsible.
10. **Native ads** need a media view of at least 120x120 dp (points on iOS) to receive video.

Use the test ad units during development: `test-banner`, `test-interstitial`, `test-rewarded`,
`test-app-open` and `test-native`.

## Finish

Run the `wortise-sdk-validate` skill on the code you changed and fix what it finds.
