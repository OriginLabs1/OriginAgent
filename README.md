<p align="center">
  <img alt="Origin Agent" src="docs/assets/origin-mark.svg" width="112" height="112">
</p>

<h1 align="center">Origin Agent</h1>

<p align="center"><strong>Universal intelligence. Trustworthy execution. Human empowerment.</strong></p>

<p align="center">
  <a href="https://github.com/OriginLabs1/OriginAgent/actions/workflows/build.yml"><img alt="Build" src="https://github.com/OriginLabs1/OriginAgent/actions/workflows/build.yml/badge.svg"></a>
  <a href="https://github.com/OriginLabs1/OriginAgent/releases"><img alt="Release" src="https://img.shields.io/github/v/release/OriginLabs1/OriginAgent?include_prereleases"></a>
  <a href="LICENSE"><img alt="MIT license" src="https://img.shields.io/badge/license-MIT-blue.svg"></a>
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android%208%2B-green.svg">
  <img alt="Play Store" src="https://img.shields.io/badge/Google%20Play-not%20distributed-lightgrey.svg">
</p>

A **native, mobile-first, model-agnostic intelligent agent environment**. Origin Agent works
offline, keeps your data on your device, and treats trust as an execution property — not a slogan.

> Origin Agent is the product. **Origin AI** is its intelligence foundation:
> interchangeable, evidence-evaluated and never mandatory.

---

## Why Origin Agent?

- **Useful immediately.** Deterministic tools (calculator, date/time, notes, knowledge search) work with zero setup and zero network.
- **Sovereign by default.** Local append-only memory, user-controlled export, no account, no telemetry, no mandatory cloud.
- **Trustworthy execution.** Every consequential action follows `Intent → Plan → Authorization → Execute → Verify → Persist → Provenance → Result`.
- **Model agnostic.** A local deterministic engine is always available; `llama.cpp`, `bitnet.cpp` and future engines are optional adapters.
- **Beautiful native UX.** Material You / Material 3 Expressive, dynamic color, expressive motion and accessible micro-interactions.
- **Small and resilient.** Designed for 4 GB devices such as the Galaxy S9 (Android 10) while scaling upward.

## Get it

| Channel | Status |
|---|---|
| [GitHub Releases](https://github.com/OriginLabs1/OriginAgent/releases) | Primary APK distribution |
| Build from source | `bash ./gradlew assembleDebug` |
| F-Droid | Metadata in [`metadata/`](metadata/) — submission-ready |
| Google Play Store | **Not distributed (by design, for now)** |

> Install an APK from a source you trust. Verify the SHA-256 in the release notes before installing.
> Origin Agent requests no unnecessary permissions and contains no ads, analytics or proprietary SDKs.

## What's inside (v0.0.1)

- **Origin** — conversational home with suggestions, honest engine status and live trust badges.
- **Capabilities** — tool catalog, consent controls and the model registry with availability truth.
- **Memory** — searchable local notes and facts, one-tap JSON export, clear-with-confirmation.
- **Trust** — execution timeline, authorization ledger and offline provenance-chain verification.
- **Settings** — theme, dynamic color, reduced motion and about/legal information.

## Architecture at a glance

```
Intent → Plan → Authorization → Execute → Verify → Persist → Provenance → Result
                  │                │          │
             policy + consent   tool registry  hash-chained ledger
                                      │
                        engine registry (offline-first)
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full map,
[`CONTEXT.md`](CONTEXT.md) for current state and decisions, and
[`SPECS.md`](SPECS.md) for skills, plan, tools and workflows.

## Build from source

Requirements: JDK 21, Android SDK (platform 36, build-tools 36.0.0), CMake 3.22+, Android NDK 27.x.

```bash
git clone https://github.com/OriginLabs1/OriginAgent.git
cd OriginAgent
bash ./gradlew test
bash ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On hosts where the downloaded NDK tools cannot run (for example aarch64 Linux), add
`-PskipNative=true` to each Gradle command. The Kotlin fallback is complete and the app
remains fully functional; CI and releases build the native layer normally.

No model weights are downloaded or bundled by the build. Add engines only through the
documented adapters in [`docs/MODEL_REGISTRY.md`](docs/MODEL_REGISTRY.md).

## Governance

The constitution is the source of truth: [`CONSTITUTION.md`](CONSTITUTION.md).
Contributions are welcome — read [`CONTRIBUTING.md`](CONTRIBUTING.md) and
[`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md). Security issues: [`SECURITY.md`](SECURITY.md).

**We build the best, or we build again—only when justifiably vital or necessary.**

© 2026 OneOrigin · OriginLabs1 · MIT License.
