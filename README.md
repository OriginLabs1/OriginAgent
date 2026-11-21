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
| [GitHub Releases](https://github.com/OriginLabs1/OriginAgent/releases) | Primary APK distribution (`v0.0.1`, `v0.0.2`) |
| [GitHub Pages website](https://originlabs1.github.io/OriginAgent/) | Live static site from [`site/`](site/) |
| [Cloudflare Pages](https://origin-agent-kit.coreconcept1.net) | Live `origin-agent-kit` project (also on `origin-agent-kit.pages.dev`) |
| Build from source | `bash ./gradlew assembleRelease` |
| F-Droid | Metadata in [`metadata/`](metadata/) — submission-ready |
| Google Play Store | **Not distributed (by design, for now)** |

> Install an APK from a source you trust. Verify the SHA-256 in the release notes before installing.
> Origin Agent requests no unnecessary permissions and contains no ads, analytics or proprietary SDKs.

## What's inside (v0.0.2)

- **Origin** — conversational home with suggestions, live trust badges and a model chip showing the model actually answering.
- **Universal model picker** — provider, capabilities, limitations and connection status; switching models keeps the conversation.
- **Add adapter** — built-in web adapter templates (OpenAI, OpenRouter, Groq, Together, DeepSeek, Ollama, LM Studio) and any custom OpenAI-compatible `/v1` endpoint.
- **Web access** — possible and enabled by default, with a Settings switch. No network call is made unless you select a web adapter; local models stay offline.
- **Capabilities** — tool catalog, consent controls and an honest model registry.
- **Memory** — searchable local notes and facts, one-tap JSON export (credentials masked), clear-with-confirmation.
- **Trust** — execution timeline, authorization ledger and offline provenance-chain verification.
- **Settings** — theme, dynamic color, reduced motion, web access and about/legal information.

The v0.0.1 foundation (harness, tools, memory, provenance, Material 3 UI) remains intact.

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
bash ./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

On hosts where the downloaded NDK tools cannot run (for example aarch64 Linux), add
`-PskipNative=true` to each Gradle command. The Kotlin fallback is complete and the app
remains fully functional; CI and releases build the native layer normally.

No model weights are downloaded or bundled by the build. Add engines only through the
documented adapters in [`docs/MODEL_REGISTRY.md`](docs/MODEL_REGISTRY.md). Web adapters use
documented OpenAI-compatible endpoints; an INTERNET permission is present but all network use
is gated by the in-app Web access switch and an explicitly selected web adapter.

## Governance

The constitution is the source of truth: [`CONSTITUTION.md`](CONSTITUTION.md).
Contributions are welcome — read [`CONTRIBUTING.md`](CONTRIBUTING.md) and
[`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md). Security issues: [`SECURITY.md`](SECURITY.md).

**We build the best, or we build again—only when justifiably vital or necessary.**

© 2026 OneOrigin · OriginLabs1 · MIT License.
