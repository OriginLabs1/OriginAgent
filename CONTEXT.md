# CONTEXT — Origin Agent

> Living context for contributors and AI collaborators. Read this before changing code.
> If this file disagrees with `CONSTITUTION.md`, the constitution wins.

## 0. Identity in one paragraph

**Origin Agent** is a universal, extensible, trustworthy intelligent agent environment.
It is the **product**. **Origin AI** is its model-agnostic, runtime-independent intelligence
foundation. The project is a **OneOrigin initiative**, hosted as
[`OriginLabs1/OriginAgent`](https://github.com/OriginLabs1/OriginAgent).

**Slogan:** Universal intelligence. Trustworthy execution. Human empowerment.
**North Star:** Preserve meaning. Transform intelligently. Adapt continuously. Verify universally.

## 1. Current state (update at every milestone)

| Field | Value |
|---|---|
| Phase | 0.0.2 — model choice and interaction maturity |
| Version | `0.0.2` |
| Product form | Native Android app (Kotlin + Jetpack Compose, Material 3 Expressive). **Not a PWA.** |
| Reference device | Samsung Galaxy S9 — Snapdragon 845, 4 GB RAM, Android 10 |
| Minimum device | Android 8.0 (API 26), ARM64 or x86-64, 4 GB RAM target |
| Intelligence | Offline-first. Deterministic native tools + optional interchangeable inference engines. |
| Distribution | GitHub source, GitHub Releases APK, GitHub Pages website (`site/`), Cloudflare Pages project `origin-agent-kit` (`origin-agent-kit.coreconcept1.net`), F-Droid-ready metadata. **No Google Play Store for now.** |
| Repository | `OriginLabs1/OriginAgent` (public) |
| License | MIT (see `LICENSE`) |
| Local verification | `bash ./gradlew test -PskipNative=true` (32 tests green), `bash ./gradlew assembleRelease -PskipNative=true --max-workers=1 -Dorg.gradle.jvmargs=-Xmx640m` produced v0.0.2 minified, debug-signed APK (SHA-256 `9137109826965c29d0954c7d0f26a027e920ce6d13f901073e5daf4badc029db`), verified with `apkanalyzer` (`0.0.2`). Native layer disabled locally only because the downloaded NDK host tools are x86-64; CI builds it by default. |
| Publishing | **Live.** `v0.0.1` and `v0.0.2` are on GitHub Releases (v0.0.2 published APK SHA-256 `c2d2f1f258fa5936fce07168c35205cb2d3af52a41c4e9bca154f5042bc191b6`). GitHub Pages is enabled and serving at `https://originlabs1.github.io/OriginAgent/`. Cloudflare Pages project `origin-agent-kit` is deployed and serving `https://origin-agent-kit.coreconcept1.net` (custom domain active) and `https://origin-agent-kit.pages.dev`. Release, build and Pages workflows all succeeded. |

## 2. What is already decided (do not re-litigate without evidence)

1. **Mobile-first native app.** The first lovable release is an Android app, not a web wrapper, not a PWA.
2. **Material You / Material 3 Expressive.** Dynamic color where available, expressive motion, tactile micro-interactions, accessible defaults.
3. **Model agnosticism.** No model or provider is a hard dependency. Engines are adapters behind one interface.
4. **Offline usefulness.** The Universal Intelligence Harness must work with no model and no network. A deterministic local engine is always present.
5. **Trustworthy execution.** `Intent → Plan → Authorization → Execute → Verify → Persist → Provenance → Result` is the only allowed consequential-action path.
6. **Sovereignty.** Local-by-default storage, user-controlled memory, portable JSON export, no mandatory account/cloud/subscription.
7. **Small foundation.** C/C++ for sovereignty-critical native primitives; Kotlin for the native Android UI and orchestration; optional Rust/Zig integrations only with demonstrable benefit.
8. **Evidence over claims.** Models, runtimes and architectures enter the registry with benchmarks; none earns permanent preference.
9. **No Play Store for now.** Distribute via GitHub Releases, GitHub Pages, Cloudflare Pages and reproducible source builds.
10. **Web access is optional and enabled by default.** Adapters are not limited to user-supplied endpoints: built-in web templates and added adapters are first-class, but no network request happens without a selected web adapter and the Settings switch.

## 3. Architectural map

```
User intent (touch / voice / tool / system)
        │
        ▼
Universal Intelligence Harness (OUIH)
  ├─ Planner .......... deterministic rules, optional model assistance
  ├─ Authorization .... policy + explicit consent for consequential steps
  ├─ Tool registry .... calculator, date/time, notes, knowledge, model status
  ├─ Verifier ......... checks outcomes; distinguishes attempt from verified success
  ├─ Memory store ..... append-only JSONL, user controlled, exportable
  ├─ Provenance ledger  hash-chained records, verifiable offline
  └─ Engine registry .. local deterministic engine + optional llama.cpp / bitnet.cpp adapters
        │
        ▼
Material 3 Expressive native UI (Origin, Capabilities, Memory, Trust, Settings)
        │
        ▼
GitHub Releases APK · source build · F-Droid metadata
```

### Module boundaries

| Layer | Package / path | Depends on |
|---|---|---|
| Domain | `domain/` | Kotlin stdlib only |
| Harness | `harness/` | domain, tools, memory, models |
| Tools | `tools/` | domain, memory |
| Memory | `memory/` | domain, `filesDir` |
| Models | `models/` | domain, optional `NativeBridge` |
| Native | `src/main/cpp/` | Android NDK/CMake, no third-party code vendored |
| UI | `ui/` | Compose + domain/harness via ViewModel |
| Docs | `CONTEXT.md`, `SPECS.md`, `docs/` | — |

## 4. Strategic model shortlist (candidates, not a ranking)

1. BitNet b1.58 2B4T — ternary, exceptional intelligence-per-memory.
2. LFM2.5-class — efficient compact edge intelligence.
3. Optimized sub-1B models — responsiveness and everyday usability.
4. Qwen-class compact models — broad-capability baseline.
5. Emerging architectures / quantization — continuous discovery.

Selection criteria: task quality, footprint, time-to-first-token, energy, reliability,
context/multilingual, runtime compatibility, licensing, portability.
Details and evidence live in `docs/MODEL_REGISTRY.md`.

## 5. Non-goals (for v0.0.1)

- No mandatory account, cloud service, subscription or telemetry.
- No Play Store listing, no proprietary SDKs, no ad frameworks.
- No model weights bundled in the repository or APK.
- No claiming benchmark superiority without reproducible evidence.
- No forcing an incompatible model into an unsuitable runtime.
- No decorative UI that reduces usefulness on 4 GB devices.

## 6. Quality bar

Release only when the Acceptance Criteria in `CONSTITUTION.md §12` are demonstrable:
Amazing · Reliable · Universal · Robust · Standard · Safe · Structured · Cohesive ·
Mature · Self-sufficient · Independent · Lovable · Sustainable · Scalable ·
Reproducible · Cutting-edge · Elegant · Concise · Comprehensive · Intuitive.

## 7. Working rules for agents

- Read `SPECS.md` for tools, workflows and commands before implementing.
- Prefer one-pass working systems; verify with `./gradlew test` and a device build.
- Keep diffs small, explained and reversible. Never overwrite unfamiliar user work.
- Add provenance for consequential actions, including automated ones.
- Update this file's "Current state" when a phase completes.
- If a dependency is optional, make it genuinely optional (feature detection, graceful fallback).
