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
| Phase | 0.0.4 — Satin workspace + resumable verified downloads, GGUF import and model library |
| Version | `0.0.4` |
| Product form | Native Android app (Kotlin + Jetpack Compose, Satin design system over Material You). **Not a PWA.** The website keeps Aurora and is visually unchanged. |
| Reference device | Samsung Galaxy S9 — Snapdragon 845, 4 GB RAM, Android 10 |
| Minimum device | Android 8.0 (API 26), ARM64 or x86-64, 4 GB RAM target |
| Intelligence | Offline-first. Five verified local GGUF models plus compatible imported GGUFs served by bundled llama.cpp, memory-aware suggestions and opt-in OpenAI-compatible web adapters. Deterministic tools never answer chat. |
| Distribution | GitHub source, GitHub Releases APK, GitHub Pages website (`site/`), Cloudflare Pages project `origin-agent-kit` (`origin-agent-kit.coreconcept1.net`), F-Droid-ready metadata. **No Google Play Store for now.** |
| Repository | `OriginLabs1/OriginAgent` (public) |
| License | MIT (see `LICENSE`) |
| Local verification | `bash ./gradlew test assembleRelease -PskipNative=true --offline --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx640m` green: 80 JVM tests, minified debug-signed APK as version `0.0.4` / code `4`; `lint` green; SHA-256 `f0e6497cd0e200eeda867b0d7fe14dbfd2c24ab2b8fb95935c3e3119c578ae14`. Native in-repo tools disabled locally because the downloaded NDK host tools are x86-64; CI builds them by default. |
| Publishing | `v0.0.1` and `v0.0.2` remain live on GitHub Releases (v0.0.2 published SHA-256 `c2d2f1f258fa5936fce07168c35205cb2d3af52a41c4e9bca154f5042bc191b6`). `v0.0.3` is pushed, tagged and released; published APK SHA-256 `ce0238e94c59423e9d45d344353c927da573c10707653d1f59915cd4504f665c` (the local reference build is `c8c2cd3a029725fbfeaca4aa0226a779598ff931a17622f182f3d4a885b3afdd`). GitHub Pages is live at `https://originlabs1.github.io/OriginAgent/`. Cloudflare Pages serves `origin-agent-kit.coreconcept1.net` / `origin-agent-kit.pages.dev`. |

## 2. What is already decided (do not re-litigate without evidence)

1. **Mobile-first native app.** The first lovable release is an Android app, not a web wrapper, not a PWA.
2. **Satin design system over Material You.** Dynamic color remains optional; neutral surfaces, one attention accent, status-only colours and truthful chips define the native identity from v0.0.4. Aurora remains the website identity and `site/` is unchanged.
3. **Model agnosticism.** No model or provider is a hard dependency. Engines are adapters behind one interface; users can keep and switch between downloaded and imported GGUFs.
4. **Offline usefulness.** Tools, memory and the trust ledger work with no model and no network. Chat requires a real AI model: a verified local GGUF downloaded in-app, a compatible GGUF imported from the device or an explicitly configured web adapter. A deterministic engine is present for tools only.
5. **Trustworthy execution.** `Intent → Plan → Authorization → Execute → Verify → Persist → Provenance → Result` is the only allowed consequential-action path.
6. **Sovereignty.** Local-by-default storage, user-controlled memory, portable JSON export, no mandatory account/cloud/subscription.
7. **Small foundation.** C/C++ for sovereignty-critical native primitives; Kotlin for the native Android UI and orchestration; optional Rust/Zig integrations only with demonstrable benefit.
8. **Evidence over claims.** Models, runtimes and architectures enter the registry with benchmarks; none earns permanent preference.
9. **No Play Store for now.** Distribute via GitHub Releases, GitHub Pages, Cloudflare Pages and reproducible source builds.
10. **Web access is opt-in.** Built-in web templates are candidates only until explicitly saved/configured and selected; no network request happens on launch or from defaults. Suggested model weights are downloaded, resumed, verified and activated in-app; a user-owned GGUF may instead be imported and validated locally.
11. **bitnet.cpp is a research target, not the Android default.** It has no official AAR/JNI and uses I2_S/TL1/TL2 formats, not llama.cpp's TQ1_0. The shipped BitNet weights run through the bundled llama.cpp AAR; `BitnetCppEngine` stays research-only until correctness and on-device evidence exist.
12. **Local data beats a server in v0.0.4.** JSONL and SharedPreferences on the device are sufficient; PostgreSQL and a full OpenBot server are not justified yet. See `docs/LOCAL_FIRST_DATA_DECISIONS.md`.

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
  └─ Engine registry .. verified downloaded + imported GGUFs (llama.cpp) + real web adapters
                          tool engine is never chat-capable; no rule-based fallback
        │
        ▼
Satin native UI (Chat, Tasks, Make, Channels, Capabilities, Memory, Trust, Settings) over Material You
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
Details and evidence live in `docs/MODEL_REGISTRY.md`. All four strategic families now have
real public GGUF artifacts, pinned sizes and SHA-256 checksums, and appear in the Satin model
library through **Get Model**: SmolLM2 135M Q4_K_M (starter), BitNet b1.58 2B4T TQ1_0,
LFM2.5 350M Q4_K_M, SmolLM2 360M Q4_K_M and Qwen2.5 0.5B Q4_K_M. Compatible user-owned
GGUFs can also be imported, hashed and kept on-device. `LlamaEngine` runs these through the
bundled llama.cpp AAR; `OriginCompactEngine` and its 4.4 KB JSONL were removed because a
weighted-intent program is not a conversation.

## 5. Non-goals (for v0.0.1)

- No mandatory account, cloud service, subscription or telemetry.
- No Play Store listing, no proprietary SDKs, no ad frameworks.
- No model weights bundled in the APK; Get Model downloads a verified GGUF once, and an optional import stays in app-private storage. Both then run offline.
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
