# Android: ad formats

Each format has a full page with every listener method: <https://docs.wortise.com/android-sdk/sdk-integration/banner.md>,
`interstitial.md`, `rewarded.md`, `app-open.md` and `native.md` in the same folder. The examples
below are in Kotlin; the pages also have Java.

Imports: see the `wortise-sdk-integrate` skill's Android reference.

## Banner

Pick one of the three ways and keep the banner while its screen lives. For a banner at the top
or bottom of the screen, prefer the anchored adaptive size (below) to a fixed `HEIGHT_50`.

**XML layout.** A banner with `app:adUnitId` loads by itself as soon as the layout is inflated:
do not call `loadAd()` on it. That happens before the consent request finishes, so on a screen
that requests consent (usually the first one), create the banner in code once consent is resolved
instead.

```xml
<com.wortise.ads.banner.BannerAd
    android:id="@+id/banner"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:adSize="HEIGHT_50"
    app:adUnitId="<ad unit ID>" />
```

**Code**, called after the consent request finishes:

```kotlin
private var bannerAd: BannerAd? = null

private fun loadBanner() {
    val container = findViewById<FrameLayout>(R.id.banner_container)
    val metrics   = resources.displayMetrics
    val widthDp   = (metrics.widthPixels / metrics.density).toInt()

    bannerAd = BannerAd(this).also {
        it.adSize   = AdSize.getAnchoredAdaptiveBannerAdSize(this, widthDp)
        it.adUnitId = "<ad unit ID>"

        container.addView(it)

        it.loadAd()
    }
}

override fun onDestroy() {
    bannerAd?.destroy()
    super.onDestroy()
}
```

**Jetpack Compose.** Create the banner with `remember`, load it in `factory` (never in `update`,
which runs on every recomposition) and destroy it in `onDispose`:

```kotlin
@Composable
fun WortiseBanner(adUnitId: String) {
    val context = LocalContext.current
    val banner  = remember { BannerAd(context) }

    AndroidView(
        factory  = {
            banner.apply {
                adSize        = AdSize.HEIGHT_50
                this.adUnitId = adUnitId
                loadAd()
            }
        },
        modifier = Modifier.fillMaxWidth().wrapContentHeight()
    )

    DisposableEffect(Unit) {
        onDispose { banner.destroy() }
    }
}
```

Rules:

- Always call `destroy()` when the screen is destroyed: the banner keeps system listeners and the
  network's ad view until then. `pause()` and `resume()`, which the documentation calls in
  `onPause`/`onResume`, are optional: the refresh already pauses while the banner is not visible.
- Keep the banner clear of the system bars. Apps targeting SDK 35 or higher are drawn edge to
  edge on Android 15+, so a banner at the bottom ends up under the navigation bar unless the
  layout handles the insets (for example, `android:fitsSystemWindows="true"` on the root view or
  `ViewCompat.setOnApplyWindowInsetsListener`). An ad under the system bars causes accidental
  taps.
- `adUnitId` can be set only once per instance: assigning a different one throws an
  `IllegalArgumentException`. Do not recycle banners between list rows with different ad units.
- The banner refreshes every 60 seconds by default (between 30 and 120), pauses while it is not
  visible, and retries one minute after a failed load. `autoRefreshTime` is in seconds in XML and
  in milliseconds in code; `BannerAd.AUTO_REFRESH_DISABLED` turns it off.
- Adaptive sizes: `AdSize.getAnchoredAdaptiveBannerAdSize(context, widthDp)` for banners anchored
  to the top or bottom, and `AdSize.getInlineAdaptiveBannerAdSize(context, widthDp, maxHeightDp)`
  for banners inside scrolling content.
- Collapsible: needs an anchored adaptive size, and applies to the first ad only:
  `bannerAd.loadAd(RequestParameters(collapsible = CollapsiblePosition.BOTTOM))`.

## Interstitial and rewarded

Both work the same way; rewarded adds `onRewardedCompleted`.

```kotlin
class GameActivity : AppCompatActivity() {

    private var rewardedAd: RewardedAd? = null

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ...
        rewardedAd = RewardedAd(this, "<ad unit ID>").also {
            it.listener = rewardedListener
            it.loadAd()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        rewardedAd?.destroy()
        super.onDestroy()
    }

    fun onWatchAdClicked() {
        val ad = rewardedAd

        if (ad?.isAvailable == true) {
            ad.showAd(this)
        } else {
            continueWithoutReward()
        }
    }

    private val rewardedListener = object : RewardedAd.Listener {

        override fun onRewardedCompleted(ad: RewardedAd, reward: Reward) {
            if (reward.success) {
                grantReward()
            }
        }

        override fun onRewardedDismissed(ad: RewardedAd) {
            resumeGame()
            ad.loadAd()
        }

        override fun onRewardedFailedToShow(ad: RewardedAd, error: AdError) {
            resumeGame()
            ad.loadAd()
        }

        override fun onRewardedFailedToLoad(ad: RewardedAd, error: AdError) {
            // The ad is in cooldown after a failure: retry later, not now
            val delay = maxOf(ad.cooldownRemainingMs, 30_000L)

            handler.postDelayed({ ad.loadAd() }, delay)
        }

        // The other methods (clicked, impression, loaded, revenue paid, shown) are optional
    }
}
```

For an interstitial, use `InterstitialAd` and `InterstitialAd.Listener`, whose methods are
`onInterstitialClicked`, `onInterstitialDismissed`, `onInterstitialFailedToLoad`,
`onInterstitialFailedToShow`, `onInterstitialImpression`, `onInterstitialLoaded`,
`onInterstitialRevenuePaid` and `onInterstitialShown`. App open ads follow the same pattern with
`AppOpenAd.Listener` (`onAppOpen...`).

Never call `showAd()` from `on...Loaded`: with the reload in `on...Dismissed`, the app would show
ads one after another without any user action.

Rules:

- `showAd()` with no ad available does nothing and invokes no callback.
- `cooldownRemainingMs` and `isInCooldown` are available on interstitial, rewarded and app open
  ads, although the format pages do not list them.
- `loadAd()` on an ad that is already loaded discards it.
- Define the reward amount in the app: `reward.amount` and `reward.label` may be empty.
- An ad shared by several screens should be created with the `Application` context, with the
  current `Activity` passed to `showAd(activity)`. Never keep an ad created with an `Activity` in
  a static field, a singleton or a `companion object`: it keeps the `Activity` alive.

## App open

Choose one of the two ways, never both at once.

**`AppOpenManager`** shows an ad every time the app comes back to the foreground, if one is
loaded. The first launch only loads it, so the first ad appears on the next return.

```kotlin
class MyApplication : Application() {

    private val appOpenManager by lazy {
        AppOpenManager.getInstance(this, "<ad unit ID>")
    }

    override fun onCreate() {
        super.onCreate()
        // ... SDK initialization
        appOpenManager.loadAd()
    }
}
```

Any return counts, including coming back after tapping an ad. If the app needs to limit how often
these ads appear, use `AppOpenAd` instead.

**`AppOpenAd`** gives control over when to show it: `AppOpenAd(context, "<ad unit ID>")`, with
`autoReload = true` to load a new ad after each one closes, `isAvailable` + `showAd(activity)`, or
`tryToShowAd(activity)`, which shows the ad if it is loaded and loads one otherwise. An app open
ad expires four hours after loading.

## Native

```kotlin
private val binder = NativeAdViewBinder.Builder(R.layout.native_ad)
    .setHeadlineTextViewId(R.id.ad_headline)
    .setCallToActionButtonId(R.id.ad_call_to_action)
    .setMediaContentViewId(R.id.ad_media)
    // ... one setter per asset of the layout
    .build()

private var nativeAd: NativeAd? = null
private var nativeAdLoader: NativeAdLoader? = null

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // ...
    nativeAdLoader = NativeAdLoader(this, "<ad unit ID>", nativeListener).also {
        it.loadAd()
    }
}

override fun onDestroy() {
    nativeAd?.destroy()
    nativeAdLoader?.destroy()
    super.onDestroy()
}
```

In `onNativeLoaded(ad)`: destroy the previous `NativeAd`, keep the new one, create a
`NativeAdView(context, binder)`, call `nativeAdLoader.renderAd(adView, ad)` and add the view to the
layout. The native page has the complete listener and layout.

Rules:

- The media view must measure at least 120x120 dp; smaller ones do not receive video, and the SDK
  logs a warning.
- The layout must show that the content is an ad, for example with an "Ad" label.
- `GoogleNativeAd` is deprecated: use `NativeAdLoader` in new code.
