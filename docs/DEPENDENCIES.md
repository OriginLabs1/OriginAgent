# Dependency ledger

Principle: minimal necessary dependencies. Every entry explains why it exists.

| Dependency | Version | Why | Optional? |
|---|---|---|---|
| Kotlin stdlib | 2.4.21 | Language runtime | No |
| AndroidX Core KTX | 1.16.0 | Platform compatibility helpers | No |
| KotlinX Coroutines Android | 1.10.2 | Structured concurrency for the harness | No |
| AndroidX Activity Compose | 1.10.1 | Compose host activity | No |
| AndroidX Lifecycle (runtime/viewmodel Compose) | 2.9.4 | Lifecycle-safe state | No |
| AndroidX Navigation Compose | 2.9.8 | Screen navigation | No |
| Compose BOM | 2026.02.01 | Compose artifact alignment for SDK 36 | No |
| Compose UI | 1.10.4 (via BOM) | Declarative UI foundation | No |
| Compose Material3 | 1.4.0 (via BOM) | Material You / Expressive patterns | No |
| Compose Material Icons Extended | 1.7.8 (via BOM) | Accessible iconography | No |
| JUnit 4 | 4.13.2 | Unit tests | Test only |
| AndroidX Test Ext JUnit | 1.3.0 | Instrumentation assertions | Test only |
| Espresso Core | 3.7.0 | UI smoke tests | Test only |
| CMake + NDK | 3.22.1 / 27.3.13750724 | Native provenance library | Build-time, skippable with `-PskipNative=true` |

**Deliberately absent:** ads, analytics, crash reporting, Firebase, mandatory cloud SDKs,
account SDKs, WebView-based UI, model weights, proprietary binaries.

Need to add something? Update this table in the same pull request.
