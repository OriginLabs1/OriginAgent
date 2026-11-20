# Changelog

All notable changes to this project are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Project versioning follows the
constitution's foundational version `0.0.1` until the first usable native release.

## [Unreleased]

### Changed
- _Nothing yet._

### Fixed
- _Nothing yet._

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
