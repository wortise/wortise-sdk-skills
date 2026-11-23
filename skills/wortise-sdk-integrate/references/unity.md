# Unity: set up the Wortise SDK

Full guide: <https://docs.wortise.com/unity-sdk/sdk-integration.md>. It is the source of truth
for versions and requirements; read it when a value here looks outdated.

The Wortise Unity package wraps the native Android and iOS SDKs. **It does nothing in the Unity
Editor**: every call is ignored and no event is raised, including `WortiseSdk.OnInitialized` and
the consent callbacks. Everything below can only be tested in a build running on a device.

## Requirements

| Item | Minimum | Where to check it |
|---|---|---|
| Unity | 6 (6000.0) | `ProjectSettings/ProjectVersion.txt` (`m_EditorVersion`) |
| Android Minimum API Level | 24 | `AndroidMinSdkVersion` in `ProjectSettings/ProjectSettings.asset` |
| Android Target API Level | 36 | `AndroidTargetSdkVersion` in the same file (`0` means the highest installed) |
| iOS target version | 15.0 | Raised automatically when the app is built for iOS |

Unity 2022 and older are not supported: the ad networks bundled with the SDK need a newer Android
Gradle Plugin than those versions ship. Upgrading Unity and raising the Minimum API Level are the
user's decisions: ask first, and say why.

## Install the package

1. Download `Wortise-Unity-<version>.unitypackage` from the link on the integration page (the
   version is in the URL). Use the latest stable version.
2. The user imports it in the Editor (**Assets > Import Package > Custom Package**, or a double
   click on the file). Do not unpack it by hand.
3. The package bundles the External Dependency Manager for Unity (EDM4U), which downloads the
   native SDKs. Keep a single copy: if the project already has it (an
   `Assets/ExternalDependencyManager` folder, or `com.google.external-dependency-manager` in
   `Packages/manifest.json`), uncheck the `ExternalDependencyManager` folder in the import window.

The installed version is in `Assets/Wortise/Editor/WortiseDependencies.xml` (`<unityversion>`).
To update, import the new package over the old one and resolve the dependencies again.

Do not add the Google Mobile Ads Unity plugin, mediation adapters, ad network SDKs or Gradle
dependencies for Wortise: `WortiseDependencies.xml` declares the native SDKs, which bring the
versions they were tested with. If the project already uses another ad plugin, tell the user.

## Android: resolve the dependencies

In **Player Settings > Publishing Settings**, enable **Custom Main Gradle Template**, **Custom
Gradle Properties Template** and **Custom Gradle Settings Template**. Then select **Assets >
External Dependency Manager > Android Resolver > Resolve**.

The result, which you can check without the Editor:

- `ProjectSettings/ProjectSettings.asset` has `useCustomMainGradleTemplate: 1`,
  `useCustomGradlePropertiesTemplate: 1` and `useCustomGradleSettingsTemplate: 1`.
- `Assets/Plugins/Android/mainTemplate.gradle` has `com.wortise:android-sdk:<version>`, the
  version of `WortiseDependencies.xml`.
- `Assets/Plugins/Android/settingsTemplate.gradle` has `https://maven.wortise.com/artifactory/public`
  and the other repositories of `WortiseDependencies.xml`.

Do not edit the lines between `// Android Resolver ... Start` and `... End`: the resolver
rewrites them.

## iOS

CocoaPods installs the iOS SDK when Unity generates the Xcode project. Open the generated
`.xcworkspace`, not the `.xcodeproj`. When the app is built for iOS, the package raises the target
version to 15.0 if it is lower (with a warning in the console) and writes `GADApplicationIdentifier`
and `SKAdNetworkItems` to the `Info.plist`.

## Google App IDs

Set them in **Wortise > Settings**, one for Android and one for iOS (Wortise dashboard, in the
details of each app, under `Google App ID`, format `ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY`). They
are saved in `Assets/Resources/WortiseSettings.asset` (fields `googleAndroidAppId` and
`googleIosAppId`) and applied when the app is built, so set them **before** building.

- If the file exists, you may edit the two values in it. If it does not, the user opens
  **Wortise > Settings**, which creates it.
- Without a valid ID, Google Mobile Ads makes the app crash at launch on that platform.
- Never use Google's sample IDs (`ca-app-pub-3940256099942544~...`).

## Code

The Wortise classes are in the global namespace: no `using` is needed for them. They are
`WortiseSdk`, `WortiseAdSettings`, `WortiseConsentManager`, `WortiseBanner`, `WortiseAdSize`,
`WortiseAdPosition`, `WortiseRequestParameters`, `WortiseCollapsiblePosition`,
`WortiseInterstitial`, `WortiseRewarded`, `WortiseReward`, `WortiseAppOpen` and
`WortiseRevenueData`. There are no native ads in Unity.

Call the SDK only from the Unity main thread (never from `Task.Run` or your own threads). Every
event is raised on the main thread, so handlers can use the Unity API directly. The events are
plain C# events: subscribe with `+=` and unsubscribe with `-=`.

## Ad unit IDs per build

Use the test ad units only in the Editor and in Development Builds, so a release build can never
ship a `test-` ID. Until the user has real IDs, leave the release values as obvious placeholders:
those ads fail to load instead of earning nothing silently.

```csharp
public static class AdUnits
{
#if UNITY_EDITOR || DEVELOPMENT_BUILD
    public const string AppOpen      = "test-app-open";
    public const string Banner       = "test-banner";
    public const string Interstitial = "test-interstitial";
    public const string Rewarded     = "test-rewarded";
#else
    public const string AppOpen      = "<app open ad unit ID>";
    public const string Banner       = "<banner ad unit ID>";
    public const string Interstitial = "<interstitial ad unit ID>";
    public const string Rewarded     = "<rewarded ad unit ID>";
#endif
}
```

A Wortise app belongs to one platform: a game published on Android and iOS has its own app key,
Google App ID and ad unit IDs on each. Ask for both and select them with `#if UNITY_ANDROID` /
`#elif UNITY_IOS`.

## Initialization, test mode and consent

Initialize once, from a persistent object of the first scene. Subscribe to `OnInitialized`
before calling `Initialize`, request consent from that event and load the ads in the consent
callback:

```csharp
using UnityEngine;

// On a GameObject of the first scene. It survives scene changes, and so do the ads it owns.
public class AdsManager : MonoBehaviour
{
    private const string AppKey = "<app key>";

    private static AdsManager instance;

    private void Awake()
    {
        if (instance != null)
        {
            // The first scene was loaded again
            Destroy(gameObject);
            return;
        }

        instance = this;
        DontDestroyOnLoad(gameObject);
    }

    private void Start()
    {
        // Not saved: set it on every launch, before loading ads
        WortiseAdSettings.IsTestEnabled = Debug.isDebugBuild;

        WortiseSdk.OnInitialized += OnSdkInitialized;
        WortiseSdk.Initialize(AppKey);
    }

    private void OnDestroy()
    {
        // OnInitialized is static: always remove the handlers of destroyed objects
        WortiseSdk.OnInitialized -= OnSdkInitialized;
    }

    private void OnSdkInitialized()
    {
        WortiseConsentManager.RequestIfRequired(shown => LoadAds());
    }

    private void LoadAds()
    {
        // Create and load the ads here (see the wortise-sdk-ad-formats skill)
    }
}
```

- `Debug.isDebugBuild` is `true` in the Editor and in builds made with **Development Build**,
  the same condition as `DEVELOPMENT_BUILD` above. Test mode makes every request a test request,
  which never earns revenue: release builds must be made without Development Build.
- Calling `Initialize` again does not initialize again, but raises `OnInitialized` again for every
  subscribed handler: keep a single call.
- Never gate the game on SDK events (for example, loading the next scene from `OnInitialized` or
  from the consent callback): they never arrive in the Editor, so the game would hang there. Gate
  only the ad loading on them.
- Apps for children set `WortiseAdSettings.IsChildDirected` and
  `WortiseAdSettings.MaxAdContentRating` before `Initialize`, among other requirements: see below.

## Consent

The SDK never shows the consent dialog by itself. `RequestIfRequired` shows it only when it is
needed (usually on the first launch in the regions that require it); call it on every launch, from
`OnInitialized` as above, so the first scene is already running.

- The callback receives whether the dialog was shown, not what the user chose.
- Give the user a way to change their choice later: a "Privacy choices" option in the game's
  settings or menu. `Request` always shows the dialog; do not call it at startup.

  ```csharp
  public void OnPrivacyChoicesClicked()
  {
      WortiseConsentManager.Request();
  }
  ```

- If the app already uses an IAB TCF consent platform, do not add this request: the SDK reads the
  consent that platform stores.

Guide: <https://docs.wortise.com/unity-sdk/user-consent.md>.

## Apps for children

If the app's audience includes children, even partially, follow
<https://docs.wortise.com/unity-sdk/privacy.md> completely, after confirming the audience with the
user. Do not improvise a different setup.

## App Tracking Transparency (iOS, optional)

The SDK does not show the App Tracking Transparency prompt, and the integration works without it.
If the user wants it: add `NSUserTrackingUsageDescription` to the `Info.plist` and request the
authorization after the consent request finishes and before the first ad request (for example,
with Unity's iOS 14 Advertising Support package). Do not add it unasked.
