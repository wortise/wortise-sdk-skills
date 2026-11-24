---
type: llm
weight: 1
---

The answer makes sure test mode cannot reach a release build (for example `AdSettings.setTestEnabled(kDebugMode)`), sets it before loading ads, and does not suggest `setTestEnabled(true)` without a debug-only condition.
