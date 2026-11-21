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
- **Real AI chat.** Every conversation ends with a model you download here or a web adapter you configure. Tools may gather verified context first, but a rule-based engine never imitates a chat answer.
- **Sovereign by default.** Local append-only memory, user-controlled export, no account, no telemetry, no mandatory cloud.
- **Trustworthy execution.** Every consequential action follows `Intent → Plan → Authorization → Execute → Verify → Persist → Provenance → Result`.
- **Model agnostic.** Four downloadable on-device models (BitNet b1.58 2B4T, LFM2.5 350M, SmolLM2 360M, Qwen2.5 0.5B) run through a bundled llama.cpp runtime; web adapters use real OpenAI-compatible endpoints.
- **Beautiful native UX.** The Aurora design system — glass surfaces, luminous accents, honest status chips and calm motion — over Material 3, with optional dynamic color.
- **Small and resilient.** Designed for 4 GB devices such as the Galaxy S9 (Android 10) while scaling upward.

## Get it

| Channel | Status |
|---|---|
| [GitHub Releases](https://github.com/OriginLabs1/OriginAgent/releases) | Primary APK distribution (`v0.0.1`, `v0.0.2`, `v0.0.3`) |
| [GitHub Pages website](https://originlabs1.github.io/OriginAgent/) | Live static site from [`site/`](site/) |
| [Cloudflare Pages](https://origin-agent-kit.coreconcept1.net) | Live `origin-agent-kit` project (also on `origin-agent-kit.pages.dev`) |
| Build from source | `bash ./gradlew assembleRelease` |
| F-Droid | Metadata in [`metadata/`](metadata/) — submission-ready |
| Google Play Store | **Not distributed (by design, for now)** |

> Install an APK from a source you trust. Verify the SHA-256 in the release notes before installing.
> Origin Agent requests no unnecessary permissions and contains no ads, analytics or proprietary SDKs.

## What's inside (v0.0.3)

- **Four real, downloadable AI models** — BitNet b1.58 2B4T, LFM2.5 350M, optimized sub-1B (SmolLM2 360M) and Qwen-class compact (Qwen2.5 0.5B). Tap **Get Model**: Origin downloads the GGUF, checks its pinned SHA-256, then runs it on-device through llama.cpp. Nothing is user-supplied; no weights are bundled. See [`docs/MODEL_REGISTRY.md`](docs/MODEL_REGISTRY.md).
- **Aurora UI** — a complete visual reimplementation: obsidian canvas, layered light wells, glass cards, gradient conversation turns, a floating dock and a light companion palette. See [`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md).
- **Origin** — conversational home with starter cards, live trust badges and a model chip showing the model actually answering.
- **Universal model picker** — provider, capabilities, limitations and connection status; switching models keeps the conversation.
- **Add adapter** — built-in web adapter templates (OpenAI, OpenRouter, Groq, Together, DeepSeek, Ollama, LM Studio) and any custom OpenAI-compatible `/v1` endpoint.
- **Web access** — possible and enabled by default, with a Settings switch. No network call is made unless you select a web adapter; local models stay offline.
- **Capabilities** — tool catalog, consent controls and an honest model registry.
- **Memory** — searchable local notes and facts, one-tap JSON export (credentials masked), clear-with-confirmation.
- **Trust** — execution timeline, authorization ledger and offline provenance-chain verification.
- **Settings** — theme, dynamic color, reduced motion, web access and about/legal information.

The v0.0.1 harness, tools, memory and provenance remain intact; v0.0.2 model freedom is preserved; v0.0.3 adds a real local AI runtime with verified downloads and the Aurora interface.

## Architecture at a glance

```
Intent → Plan → Authorization → Execute → Verify → Persist → Provenance → Result
                  │                │          │
             policy + consent   tool registry  hash-chained ledger
                                      │
                        engine registry (offline-first)
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full map,
[`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md) for the Aurora interface,
[`CONTEXT.md`](CONTEXT.md) for current state and decisions, and
[`SPECS.md`](SPECS.md) for skills, plan, tools and workflows.

## Build from source

Requirements: JDK 21, Android SDK (platform 36, build-tools 36.0.0). The llama.cpp runtime is a prebuilt AAR; CMake and the NDK are only needed when building the optional in-repo C/C++ tools natively.

```bash
git clone https://github.com/OriginLabs1/OriginAgent.git
cd OriginAgent
bash ./gradlew test
bash ./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

On hosts where the downloaded NDK tools cannot run (for example aarch64 Linux), add
`-PskipNative=true` to each Gradle command. The optional in-repo C/C++ tools fall back to
Kotlin; the bundled llama.cpp AAR still ships its arm64 native library in the APK.

The build does not fetch or bundle model weights. At runtime, **Get Model** downloads a GGUF
over HTTPS, verifies its published size and SHA-256, and only then makes it selectable; failed
or cancelled downloads never become active. Web adapters use documented OpenAI-compatible
endpoints; an INTERNET permission is present but all network use is gated by an explicit
download, or by the in-app Web access switch and a selected web adapter.

## Governance

The constitution is the source of truth: [`CONSTITUTION.md`](CONSTITUTION.md).
Contributions are welcome — read [`CONTRIBUTING.md`](CONTRIBUTING.md) and
[`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md). Security issues: [`SECURITY.md`](SECURITY.md).

**We build the best, or we build again—only when justifiably vital or necessary.**

© 2026 OneOrigin · OriginLabs1 · MIT License.
