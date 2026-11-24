# Changelog

All notable changes to this project are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Project versioning follows the
constitution's foundational version `0.0.1` until the first usable native release.

## [Unreleased]

### Added
- **Satin native workspace (v0.0.4).** A distinct OpenDuet-inspired mobile UI with a quiet
  neutral palette, one violet accent, four-tab Chat / Tasks / Make / Channels dock, truthful
  status chips and restrained micro-interactions. The product website (`site/`) is unchanged.
- Local Duet workspace: tasks with explicit states, channels and editable local draft pages.
  Defaults are labelled local drafts; no output is attributed to an AI model.
- Opt-in voice dictation through the platform speech recogniser, with honest unavailable and
  permission states instead of simulated transcription.
- Resumable, retrying GGUF downloads driven by a pure, unit-tested `DownloadPlan`.
- A Satin model library: a 135M SmolLM2 starter model, memory-aware fit labels and ordering,
  trusted GGUF browsing links, and one-tap access to downloads and imports.
- Device GGUF import: choose a `.gguf` already on the phone, copy it into app-private storage,
  validate the GGUF header, hash it, and keep it as a selectable local model. Multiple
  downloaded and imported models persist and can be switched without losing the conversation.
- `docs/LOCAL_FIRST_DATA_DECISIONS.md`: why PostgreSQL and a full OpenDuet/OpenBot server are
  deliberately not part of this offline mobile release.

### Changed
- Web adapters are genuinely opt-in: built-in templates are candidates until saved/configured;
  launch and model selection no longer make a remote request.
- Chat inference, connection checks and local workspace writes run off the main thread.
- BitNet is documented precisely: bitnet.cpp is upstream research, not an Android SDK; the
  shipped TQ1_0 weights run through the bundled llama.cpp AAR with no quality claims until the
  evidence log has real measurements.
- `versionCode` 4 / `versionName` 0.0.4.

### Fixed
- Premature EOF and socket failures no longer surface as raw “unexpected end of stream”;
  partial `.part` files are preserved and resumed, and cancellation is not reported as failure.
- SHA-256 verification never runs on the UI thread during status refresh.
- Native model release is serialized with generation so it cannot free a live model.
- Provenance verification fails closed on malformed or missing ledger lines.
- Documentation no longer claims a deterministic chat fallback.

## [0.0.3] - 2026-11-21

### Added
- **Four real, downloadable AI models.** BitNet b1.58 2B4T, LFM2.5 350M, optimized sub-1B
  (SmolLM2 360M) and Qwen-class compact (Qwen2.5 0.5B) are now actual GGUF artifacts with
  public URLs, pinned sizes and SHA-256 checksums — not research notes. `Get Model` downloads
  and verifies the weights, `LlamaEngine` runs them through a bundled MIT llama.cpp AAR, and
  the picker exposes progress, cancel, remove and Use controls. Nothing is user-supplied.
- **Chat requires a real AI model.** The deterministic engine is now tool/status only and can
  never generate a conversation. Every user conversation ends with a real model step; tools may
  gather verified context first but never become the assistant's answer. Until a verified local
  model or an explicitly configured web adapter is available, the composer is gated behind a
  **Get Model** action; there is no rule-based fallback that imitates a model.
- **Aurora design system** — a complete visual reimplementation of the Origin Agent UI:
  obsidian canvas with layered light wells, glass surfaces, luminous status pills,
  gradient conversation turns, a lighter companion palette and a floating navigation dock.
- `ui/components/Aurora.kt` with reusable background, mark, glass card, model chip,
  gradient action and section primitives. The Origin mark is drawn from primitives;
  no new assets or dependencies are introduced.
- `docs/DESIGN_SYSTEM.md` documenting tokens, motion, accessibility and the rationale
  for the redesign.
- JVM contrast tests for both Aurora palettes, enforcing WCAG AA on primary text surfaces.

### Changed
- Chat now opens on a focused conversational hero with starter cards; the active model is
  always visible in a dedicated chip and every assistant turn carries attribution and
  verified/attempted status.
- Capabilities, Memory, Trust and Settings were rebuilt on the same glass-card language;
  consent and model dialogs share the new rounded, tonal treatment.
- Typography moved to tighter display sizes with a monospace micro-label voice; reduced
  motion now disables ambient Aurora drift globally.
- Product website (`site/`) adopted the Aurora palette and glass language.
- `versionCode` 3 / `versionName` 0.0.3; F-Droid metadata updated for the new tag.

### Fixed
- Deprecated send icon usage replaced with the auto-mirrored variant.
- The toy `OriginCompactEngine` and its 4.4 KB JSONL weights were removed; a weighted-intent
  program can no longer answer a conversation.

## [0.0.2] - 2026-11-21

### Added
- **Universal model picker.** Models are grouped by provider with real capabilities,
  limitations, kind and connection status. The active model is shown in chat and on every
  assistant answer; switching never clears the conversation.
- **Add adapter.** Built-in web adapter templates (OpenAI, OpenRouter, Groq, Together,
  DeepSeek, Ollama, LM Studio, custom) plus fully user-added OpenAI-compatible endpoints.
  Adapters are managed in the picker, not hard-coded.
- **Web access.** Network use is possible and enabled by default, gated by a Settings switch.
  No request is made unless the user selects a web adapter; local models remain fully offline.
- Real OpenAI-compatible adapter (`/chat/completions` and `/models`) with explicit connection
  testing, timeouts and typed failures. No simulated model identity or silent misattribution.
- Credentials and endpoints stored locally; API keys masked in JSON exports.
- Static product website in `site/` for GitHub Pages and Cloudflare Pages
  (`origin-agent-kit`), modeled on the OpenDuet landing page with Origin branding.
- JVM tests for model catalog integrity, built-in adapter templates and OpenAI-compatible
  request/response parsing.

### Changed
- `versionCode` 2 / `versionName` 0.0.2; Settings and Capabilities updated for model control.
- Manifest declares INTERNET with a network security config that permits cleartext only for
  local model servers (localhost, 127.0.0.1, emulator host).
- Local release builds with `-PskipNative=true` no longer invoke host NDK symbol tools.

### Fixed
- Release builds no longer attempt native symbol extraction when the native layer is skipped.

## [0.0.1] - 2026-11-20

### Added
- Constitutional foundation (`CONSTITUTION.md`), context and implementation specs.
- Native Android app skeleton with Material 3 Expressive theme.
- Universal Intelligence Harness pipeline and offline deterministic engine.
- Local memory, knowledge retrieval and hash-chained provenance.
- Tool registry (calculator, date/time, notes, knowledge, status).
- GitHub Actions build/test/release workflows and F-Droid metadata.
- JVM tests for authorization gating, provenance tamper detection, planner routing,
  calculator safety and JSONL export.
- Gradle wrapper and reproducible pinned build configuration.
- Optional native C/C++ provenance layer with complete Kotlin fallback; local builds can
  use `-PskipNative=true` on hosts without runnable NDK binaries.
- First installable, minified release APK published on GitHub Releases with `SHA256SUMS.txt`.
- Minified and resource-shrunk `release` build type with debug signing for sideloading.

### Changed
- CI workflows use the runner's preinstalled Android SDK tools, invoke Gradle through `bash`, and build/attach the release APK.

### Fixed
- Material 3 icon imports, launcher resource qualification and build configuration for SDK 36.
