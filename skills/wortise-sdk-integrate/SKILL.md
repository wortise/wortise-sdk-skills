---
name: wortise-sdk-integrate
description: Adds the Wortise ads SDK to a mobile app, or updates an existing integration, the way Wortise requires it - dependency, repositories, Google App ID, initialization, user consent and test mode - on Android, iOS, Flutter, React Native and Unity. Use it whenever the user wants to integrate, install, set up, update or upgrade the Wortise SDK, or to monetize an app with Wortise ads, before writing any Wortise code.
---

# Integrate the Wortise SDK

Wortise is a mobile ad SDK that mediates Google Ad Manager and other ad networks. A wrong
integration does not always fail visibly: it can serve no ads, earn nothing, get the app flagged
for invalid traffic or break store policies. Follow these steps in order and use only the APIs
the documentation describes.

## 1. Identify the platform

Check from top to bottom: cross-platform projects also contain native Android and iOS folders.

| The project contains | Platform | Reference |
|---|---|---|
| `pubspec.yaml` with a `flutter` section | Flutter | [references/flutter.md](references/flutter.md) |
| `package.json` with `react-native` or `expo` | React Native | [references/react-native.md](references/react-native.md) |
| `ProjectSettings/ProjectVersion.txt` and `Assets/` | Unity | [references/unity.md](references/unity.md) |
| `build.gradle(.kts)` and `AndroidManifest.xml` | Android | [references/android.md](references/android.md) |
| `*.xcodeproj`, `Podfile` or `Package.swift` | iOS | [references/ios.md](references/ios.md) |

Read the platform reference before editing anything. It links to the platform's documentation
for the details: <https://docs.wortise.com/llms.txt> lists it, and every page is available as
Markdown by appending `.md` to its URL.

## 2. Look at what is already there

- **Wortise already integrated**: note its version and keep it unless the user asks to update.
  Review the existing code with the `wortise-sdk-validate` skill instead of adding a second setup.
- **Other ad SDKs** (Google Mobile Ads used directly, AppLovin, ironSource, Unity Ads and others):
  tell the user. Do not remove or change them without asking.
- **A consent tool** (CMP) already in the app: keep it if it follows the IAB TCF standard, and do
  not add a second consent dialog.

## 3. Ask for what only the user has

Ask for everything that is missing in a single message:

- **App key**: Wortise dashboard, in the details of the app.
- **Google App ID**: Wortise dashboard, in the details of the app, under `Google App ID`
  (format `ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY`).
- **Ad unit ID** of each placement the user wants.
- For apps on Android and iOS, **each of these values for each platform**: a Wortise app belongs to
  one platform, and the API rejects requests from the other one.
- **Whether the app's audience includes children**, even partially. Apps for children need a
  different setup, and choosing wrongly breaks store policies. Never decide this for the user.

Never invent these values. During development you may use the test ad units, which always return
an ad: `test-app-open`, `test-banner`, `test-interstitial`, `test-rewarded` and `test-native`.
The app key and the Google App ID have no test values: if the user does not have them yet, use a
clearly marked placeholder only if the user agrees, and say that the ads will not work until it is
replaced (on Android, a missing or invalid Google App ID makes the app crash at launch).

## 4. Choose the version

- Use the latest **stable** version stated on the platform's integration page. Package registries
  may report a pre-release as the latest one: ignore versions with `-alpha`, `-beta` or `-rc`
  unless the user asks for them.
- When updating, read the changelog first: <https://docs.wortise.com/changelog.md>.
- Write code only against the APIs documented for that version. Wortise's classes and methods
  differ from those of AdMob, AppLovin MAX and other SDKs: never guess a name by analogy. When you
  are not sure that something exists, check the documentation or the installed package.

## 5. Set it up

Follow the platform reference: dependency, repositories, Google App ID, initialization, consent
and test mode.

## 6. Finish

1. Add the ad placements with the `wortise-sdk-ad-formats` skill.
2. Run the `wortise-sdk-validate` skill on what you changed and fix what it finds.
3. Tell the user what is still theirs to do: replace placeholders and test ad units with their
   own values before release, and complete the steps the Wortise dashboard shows for the app
   (such as `app-ads.txt`).

Two rules apply at every step: test mode and test ad units never reach a release build, and
nobody should tap real ads during development, as those clicks count as invalid traffic.
