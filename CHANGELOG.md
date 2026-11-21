# Changelog

All notable changes to this project are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Project versioning follows the
constitution's foundational version `0.0.1` until the first usable native release.

## [Unreleased]

### Changed
- _Nothing yet._

### Fixed
- _Nothing yet._

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
