---
type: llm
weight: 2
---

The answer covers the Android setup the Wortise docs require: the Wortise Maven repositories, the `com.wortise:android-sdk` dependency, the `com.google.android.gms.ads.APPLICATION_ID` meta-data in the manifest, initialization in `Application.onCreate`, and test mode enabled only in debug builds. Fail if it adds the Google Mobile Ads SDK or mediation adapters as separate dependencies, or depends on `com.wortise:android-core`.
