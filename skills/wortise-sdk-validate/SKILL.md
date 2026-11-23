---
name: wortise-sdk-validate
description: Audits a Wortise SDK integration - setup, initialization order, consent, test mode, ad lifecycle, apps for children and placement risks - then builds the app and, when a device is available, checks the SDK logs. Use it after integrating or changing Wortise ads, before publishing an app that uses Wortise, or whenever the user asks to review, check or audit their Wortise integration.
---

# Validate a Wortise integration

The goal is a list of real problems, each with its evidence and its fix, not a generic checklist.
Report only what you verified in this project.

## 1. Identify the platform and the version

Identify the platform as the `wortise-sdk-integrate` skill describes, and find the SDK version
the project uses. Read the platform reference:

| Platform | Reference |
|---|---|
| Android | [references/android.md](references/android.md) |
| iOS | [references/ios.md](references/ios.md) |
| Flutter | [references/flutter.md](references/flutter.md) |
| React Native | [references/react-native.md](references/react-native.md) |
| Unity | [references/unity.md](references/unity.md) |

## 2. Review the code

Go through every check of the reference. For each one, search the project as the check says and
read the code you find: a search hit is a lead, not a finding. Skip generated code and
third-party code.

These two checks apply on every platform and are not repeated in the references:

- **The app never waits for an ad event to continue.** Look for anything blocked before a show
  call and released only in a "dismissed" callback: a disabled button, a paused game (for
  example, Unity's `Time.timeScale = 0`), a pending navigation. When no ad is available, or it
  fails to show, that callback never arrives and the app hangs. A fullscreen ad that is not
  loaded again after it is dismissed makes this happen at every later break. Blocker.
- **Each platform uses its own values.** A Wortise app belongs to one platform, and the API
  rejects requests from the other one, so an app on Android and iOS needs the app key, Google
  App ID and ad unit IDs of each platform. One set of values for both makes one platform earn
  nothing. Blocker.

## 3. Build

Build the app with the project's own command (for example, the debug and release variants on
Android). A release build matters: test mode, minification and placeholders show up there.

If the project cannot be built here (it is an excerpt, or a toolchain or a file is missing), say
so, review the code anyway, and mark as not verified each check that needs what is missing (for
example, a storyboard, the Xcode project, a lockfile or the installed package).

## 4. Check it running

Only when a device or emulator is available and the user agrees: install the debug build, open
the screens with ads, and read the SDK logs as the reference explains. With test ad units every
format must load; if one does not, use the `wortise-sdk-troubleshoot` skill. Never tap the ads.

## 5. Report

Group the findings by severity:

- **Blocker**: the app crashes or hangs, earns nothing, or breaks a store or ad network policy. For
  example: no Google App ID, test mode in release, no consent, an app for children with the
  standard SDK.
- **Should fix**: revenue is lost or resources leak. For example: retry loops, ads never
  destroyed, shows without checking availability.
- **Note**: improvements that are not wrong today.

When several checks fail for one cause (for example, no consent flow at all), report one finding
that names them all. For each finding give the file and line, what happens and the fix. Then list
what you could not verify (for example, no device available) instead of presenting it as correct.

Release blockers you notice outside Wortise (for example, a release build signed with the debug
key) may be mentioned briefly, apart from the Wortise findings.

Fix the problems in code you wrote in this session. For existing code, report first and ask
before changing it.
