---
name: wortise-sdk-troubleshoot
description: Diagnoses Wortise SDK problems - ads that do not load or do not show, empty banners, no fill, crashes at launch, an app or game that freezes at an ad break, rewards granted wrongly, build errors, missing callbacks, consent dialogs that do not appear - from the SDK logs, error codes and code. Use it whenever the user reports that Wortise ads, or the app around them, do not work as expected, or shares Wortise logs, error codes or build errors.
---

# Troubleshoot the Wortise SDK

Diagnose from evidence. Most problems show up in the SDK logs, so get them before changing code.

## 1. Collect the facts

- Platform and SDK version.
- What fails: build, launch, one format, every format, one device or every device.
- Ad unit IDs in use: the app's own or the test ones (`test-banner`, `test-interstitial`,
  `test-rewarded`, `test-app-open`, `test-native`).
- The error code from the failure callback, and the SDK logs (the platform reference explains how
  to get them).

## 2. Split the problem with the test ad units

Test ad units always return an ad:

- **They fail too**: the integration is wrong (initialization, Google App ID, repositories,
  consent flow, lifecycle). Use the platform reference and the `wortise-sdk-validate` skill.
- **They work, the app's own ad units do not**: the integration is right. Check that each ad
  unit ID is copied exactly from the dashboard, belongs to the same app as the app key and is used
  with the format it was created for. `NO_FILL` with valid ad units is a normal result that
  depends on the country, the user and the demand: it is not a bug to fix in code.

## 3. Common causes on every platform

| Symptom | Likely cause |
|---|---|
| The app or game freezes, or a button stays disabled, at an ad break | The app continues only in the "dismissed" callback. No callback arrives when no ad is available or it fails to show; and an ad that is not loaded again after being dismissed leaves every later break without one |
| Rewards granted without finishing the ad, or several times | The reward is granted on "dismissed" instead of "completed"; or the listener is registered again on every render or screen load |
| A banner is loaded (the log says so) but not visible | Zero or wrong size, missing layout constraints, hidden or covered, or under the system bars |
| Ads work on one platform only | The app uses the other platform's app key, Google App ID or ad unit IDs: each Wortise app belongs to one platform |

When there are no logs (for example, the user only describes the symptom), start from the code
with the checks of the `wortise-sdk-validate` skill.

## 4. Find the cause

Read the platform reference:

| Platform | Reference |
|---|---|
| Android | [references/android.md](references/android.md) |
| iOS | [references/ios.md](references/ios.md) |
| Flutter | [references/flutter.md](references/flutter.md) |
| React Native | [references/react-native.md](references/react-native.md) |
| Unity | [references/unity.md](references/unity.md) |

## What not to suggest

- Retrying loads in a loop, or forcing test mode, to "fix" missing ads in production.
- Tapping real ads to check that they work: it counts as invalid traffic.
- Adding Google Mobile Ads or mediation adapters by hand: the SDK brings them.

## When it is not in the app

If the logs show the integration is right and the problem continues, tell the user to contact
Wortise with: platform and SDK version, app key, ad unit ID, the `Device identifier` that the SDK
logs at startup, the error code and the logs.
