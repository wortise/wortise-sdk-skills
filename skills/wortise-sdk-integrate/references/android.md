# Android: set up the Wortise SDK

Full guide: <https://docs.wortise.com/android-sdk/sdk-integration.md>. It is the source of truth
for versions and requirements; read it when a value here looks outdated.

## Requirements

Check these in `gradle/libs.versions.toml`, the Gradle build files and
`gradle/wrapper/gradle-wrapper.properties`:

| Item | Minimum |
|---|---|
| Android Gradle Plugin | 8.9.1 (Gradle 8.11.1) |
| `compileSdk` | 36 |
| Kotlin | 2.2 |
| `minSdk` | 24 |

Raising the Android Gradle Plugin, Gradle, Kotlin or `compileSdk` is safe: do it and tell the
user. Prefer the latest stable Android Gradle Plugin over the bare minimum, and update the Gradle
wrapper it requires. Raising `minSdk` drops devices: ask the user first. Below these minimums the
build fails (`AAR metadata` errors, Kotlin metadata errors or a manifest merger error on
`minSdkVersion`).

Kotlin 2.2 deprecates the `kotlinOptions { }` block: the build still works, with a warning.
Migrating it to `kotlin { compilerOptions { } }` is optional; mention it instead of rewriting the
user's build files unasked.

## Artifact

| The app's audience | Artifact | Guide |
|---|---|---|
| Does not include children | `com.wortise:android-sdk` | this file |
| Includes children (exclusively or not) | `com.wortise:android-sdk-families` | <https://docs.wortise.com/android-sdk/families.md>, followed completely |

- Add only one of the two, and no other `com.wortise` artifact.
- Do not add `com.google.android.gms:play-services-ads`, Google mediation adapters or ad network
  SDKs for Wortise: the artifact already brings the versions it was tested with. If the app
  already declares `play-services-ads` (for example, for AdMob), tell the user: Gradle uses the
  highest version, and forcing a lower one (`strictly`, `force`, `resolutionStrategy`) breaks the
  SDK.
- No ProGuard or R8 rules are needed: they ship with the SDK.

## Repositories

`android-sdk` needs three Maven repositories. Add them where the project declares its
repositories: `dependencyResolutionManagement` in `settings.gradle(.kts)` in current projects, or
`allprojects` in the root `build.gradle` in old ones.

```kotlin
dependencyResolutionManagement {
    repositories {
        // ... existing repositories
        maven { url = uri("https://maven.wortise.com/artifactory/public") }
        maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
        maven { url = uri("https://artifactory.bidmachine.io/bidmachine") }
    }
}
```

`android-sdk-families` only needs the first one.

## Dependency

In the app module, with the version from the integration page (a version catalog entry is fine
if the project uses one):

```kotlin
dependencies {
    implementation("com.wortise:android-sdk:<version>")
}
```

## Google App ID

Inside `<application>` in the app's `AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY" />
```

Without it, Google Mobile Ads crashes the app at launch. If the manifest already has this entry
(an app that used AdMob), do not overwrite it silently: ask the user, because the value must be
the Google App ID that the Wortise dashboard shows for the app.

## Imports

The documentation omits them. The public classes are:

```kotlin
import com.wortise.ads.AdError
import com.wortise.ads.AdSettings
import com.wortise.ads.AdSize
import com.wortise.ads.WortiseSdk
import com.wortise.ads.consent.ConsentManager
import com.wortise.ads.banner.BannerAd
import com.wortise.ads.interstitial.InterstitialAd
import com.wortise.ads.rewarded.RewardedAd
import com.wortise.ads.rewarded.models.Reward
import com.wortise.ads.appopen.AppOpenAd
import com.wortise.ads.appopen.AppOpenManager
import com.wortise.ads.natives.NativeAd
import com.wortise.ads.natives.NativeAdLoader
import com.wortise.ads.natives.NativeAdView
import com.wortise.ads.natives.NativeAdViewBinder
```

## Initialization

Initialize once, in `Application.onCreate`. If the app has no `Application` class, create one and
register it with `android:name` in the `<application>` element.

```kotlin
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        AdSettings.testEnabled = BuildConfig.DEBUG

        WortiseSdk.initialize(this, "<app key>")
    }
}
```

- Apps for children set `AdSettings.setChildDirected` and `AdSettings.setMaxAdContentRating`
  before this call (see the Families guide).
- Calling `initialize` again has no effect.
- The SDK only runs in the main process of the app: do not load ads from components declared
  with `android:process`.
- An ad requested before the first `initialize` fails with `SDK_NOT_INITIALIZED`.

## Test mode

`AdSettings.testEnabled = true` makes every request a test request, which never earns revenue.
The value is not saved, so set it on every launch before loading ads, and only in debug builds.

`BuildConfig.DEBUG` needs `BuildConfig` generation, which the Android Gradle Plugin disables by
default since version 8. If the project does not use `BuildConfig` yet, either enable it in the
app module:

```kotlin
android {
    buildFeatures {
        buildConfig = true
    }
}
```

or use the debuggable flag instead:

```kotlin
AdSettings.testEnabled =
    (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
```

To use the test ad units only in debug builds, define the ad unit IDs per build type, so a
release build can never ship a `test-` ID. Until the user has real IDs, leave the release value
as an obvious placeholder: those ads fail with `INVALID_PARAMS` instead of earning nothing
silently.

```kotlin
android {
    buildTypes {
        debug {
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"test-banner\"")
        }
        release {
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"<banner ad unit ID>\"")
        }
    }
}
```

## Consent

The SDK never shows the consent dialog by itself. Request it from the first `Activity` (it needs
one) and load the ads when it finishes:

```kotlin
WortiseSdk.wait {
    ConsentManager.requestIfRequired(this) {
        // The Activity may have been closed or recreated meanwhile
        if (!isFinishing && !isDestroyed) {
            loadAds()
        }
    }
}
```

- The listener is always invoked. Its `Boolean` tells whether the dialog was shown, not what the
  user chose.
- Give the user a way to change their choice later: a "Privacy choices" option in the app's
  settings or menu that calls `ConsentManager.request(activity)`, which always shows the dialog.
  Google's own dialog tells users to look for it. Do not call `request` at startup.
- If the app already uses an IAB TCF consent platform, do not add this request: the SDK reads the
  consent that platform stores.

Guide: <https://docs.wortise.com/android-sdk/user-consent.md>.
