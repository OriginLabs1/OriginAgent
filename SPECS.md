# SPECS — Origin Agent

> Implementation specification: skills, plan, tools and workflows.
> Companion to `CONTEXT.md` (state) and `CONSTITUTION.md` (law).

---

## 1. Skills (capability inventory)

A "skill" here is a capability the project must be able to perform, not a marketing label.

### 1.1 Harness skills

| ID | Skill | Minimum evidence |
|---|---|---|
| S-01 | Accept intent from touch, voice-shaped text, tools and system events | `Intent` created with source + timestamp + provenance |
| S-02 | Produce an inspectable plan before acting | `Plan` with ordered steps and risk levels |
| S-03 | Gate consequential steps on authorization | Policy decision + user consent recorded |
| S-04 | Execute through registered tools only | Unknown tool is rejected, never guessed |
| S-05 | Verify outcomes separately from attempts | `ExecutionStatus.ATTEMPTED` ≠ `VERIFIED` |
| S-06 | Persist user-controlled memory locally | Append-only JSONL, exportable, no network |
| S-07 | Record verifiable provenance | Hash chain verifies offline; tamper detected |
| S-08 | Work fully offline | App passes tests with engines disabled and airplane mode |
| S-09 | Swap inference engines without UI changes | Engine registry + capability negotiation |
| S-10 | Export all user data | JSON export includes memory, provenance, preferences |
| S-11 | Present a coherent, accessible Satin UI | Contrast-tested palettes, truthful model chip, reduced-motion support |

### 1.2 Tool skills (v0.0.1)

| Tool | Purpose | Consent |
|---|---|---|
| `calculator` | Deterministic arithmetic, parentheses, decimals | Low — auto |
| `datetime` | Local/UTC time, date arithmetic | Low — auto |
| `knowledge.search` | Search bundled + user knowledge | Low — auto |
| `notes.add` | Store a user note in memory | High — explicit |
| `notes.search` | Retrieve user notes | Low — auto |
| `system.status` | Engine, storage and trust status | Low — auto |
| `model.status` | Model registry and availability | Low — auto |

### 1.3 Experience skills

- Material You dynamic color with a stable, beautiful fallback palette.
- Material 3 Expressive components, motion and spring-like micro-interactions.
- Responsive layouts from 360 dp phones to tablets/desktop windows.
- Reduced-motion and font-scale support; semantic labels on all controls.
- Immediate usefulness: no setup wizard required for offline tools.

---

## 2. Plan (phased, evidence-gated)

### Phase 0 — Constitution and governance ✅
Deliverables: `CONSTITUTION.md`, `CONTEXT.md`, `SPECS.md`, `LICENSE`, `README.md`,
`.github/` templates. Exit: repo created at `OriginLabs1/OriginAgent`.

### Phase 1 — Lightweight harness (current)
Deliverables: domain model, planner, policy, tool registry, verifier, memory store,
provenance ledger, deterministic local engine, unit tests.
Exit: `bash ./gradlew test` green; pipeline works with no model and no network.

### Phase 2 — Native foundation
Deliverables: C/C++ provenance + normalization library via JNI, CMake build,
graceful Kotlin fallback when the library is unavailable.
Exit: `bash ./gradlew assembleRelease` green on ARM64 and x86-64 toolchains; tests pass.

### Phase 3 — Native UI
Deliverables: a four-tab OpenDuet-style workspace (Chat, Tasks, Make, Channels) plus
Capabilities, Memory, Trust and Settings, with Material You color, motion and accessibility.
Exit: manual smoke flow on a 360 dp viewport and a tablet viewport; zero console/runtime errors.

### Phase 4 — Model adapters and evidence
Deliverables: `InferenceEngine` adapters for five verified GGUF models plus user-owned GGUF
imports through bundled `llama.cpp`, memory-aware suggestions, trusted browsing links and
OpenAI-compatible web adapters; `docs/MODEL_REGISTRY.md`; reproducible benchmark harness.
Exit: suggested weights install with published SHA-256 verification; imported files are
validated and copied into app-private storage; chat is impossible until a real model is
available; unavailable engines report typed errors with no rule-based fallback; benchmarks
cite commands.

### Phase 5 — Distribution
Deliverables: minified, debug-signed release APK attached to a GitHub Release, F-Droid metadata,
reproducible build instructions. **Explicitly not Google Play Store.**
Exit: a user can install and use the APK offline.

### Phase 6 — Expansion
Deliverables: desktop/Linux harness shell, distributed execution research,
additional platforms. Evidence-gated; no architectural dependency on any platform.

---

## 3. Tools (toolchain and commands)

### 3.1 Required

| Tool | Version (stable at time of writing) | Purpose |
|---|---|---|
| JDK | 21 | Gradle + Android build |
| Gradle Wrapper | 9.9.2 | Reproducible builds |
| Android Gradle Plugin | 9.5.1 | Android build |
| Kotlin | 2.4.21 | Built into AGP 9.x + Compose compiler plugin 2.4.21 |
| Compose BOM | 2026.02.01 | Material3 1.4.0 Material You / Expressive patterns, SDK 36 compatible |
| Android SDK | platform 36, build-tools 36.0.0, platform-tools | Compile/target |
| CMake | 3.22.1+ | Native harness |
| Android NDK | 27.x | JNI library (optional but configured) |
| Git + GitHub CLI | any recent | Source and release management |

### 3.2 Common commands

```bash
# Local verification
bash ./gradlew test                    # JVM unit tests (fast, offline-safe)
bash ./gradlew lint                    # Android lint
bash ./gradlew assembleRelease         # APK in app/build/outputs/apk/release/

# Device smoke test
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat | grep -i origin

# Hosts whose NDK binaries cannot run (e.g. aarch64 Linux) can skip the optional native layer.
# Kotlin fallbacks are complete; CI/release runners build native by default.
bash ./gradlew test lint assembleRelease -PskipNative=true

# Release (tag-driven via GitHub Actions)
git tag v0.0.1 && git push origin v0.0.1
```

### 3.3 Dependency policy

- Every dependency must justify its existence in `docs/DEPENDENCIES.md`.
- Prefer platform APIs and small libraries. No ads, no analytics, no mandatory cloud SDK.
- Optional engines are discovered at runtime; never packaged as required dependencies.

---

## 4. Workflows

### 4.1 Feature workflow

1. Read `CONTEXT.md` + this file.
2. Write the smallest testable slice; add or update tests first when practical.
3. Implement behind the existing interfaces; do not bypass authorization or provenance.
4. Run `bash ./gradlew test` and `bash ./gradlew assembleDebug`.
5. Exercise the flow in the app (or with a unit test if UI-only changes are not runnable here).
6. Update `CONTEXT.md` "Current state" and docs when behavior changes.
7. Commit with a conventional message: `feat(harness): add verifier`, `fix(ui): ...`.

### 4.2 Trustworthy execution workflow (mandatory for consequential actions)

```
Intent → Plan → Authorization → Execute → Verify → Persist → Provenance → Result
```

- A plan step may not execute before its authorization decision is recorded.
- `BLOCKED`, `ATTEMPTED`, `FAILED` and `VERIFIED` are distinct outcomes.
- Every state transition appends a provenance record with a hash chain.
- User-visible actions show what was proposed, what was authorized and what succeeded.

### 4.3 Model evaluation workflow

1. Register the candidate in `docs/MODEL_REGISTRY.md` with license and source.
2. Verify the bundled runtime support (the llama.cpp AAR supports the pinned TQ1_0 and Q4_K_M GGUFs).
3. Run the fixed benchmark suite on the reference device and on a desktop baseline.
4. Record task quality, footprint, TTFT, energy/thermal notes, reliability, and command used.
5. Promote only on evidence. Never bundle weights in the APK or repository.

### 4.4 Release workflow

1. Update version, changelog and acceptance-criteria checklist.
2. `bash ./gradlew test lint assembleRelease`.
3. Tag `vX.Y.Z` and push; `.github/workflows/release.yml` builds and attaches the APK.
4. Publish notes that state clearly: local-first, no Play Store, no telemetry.
5. Update F-Droid metadata with APK hash and build commands.

### 4.5 Security and privacy workflow

- Report vulnerabilities privately via `SECURITY.md`; do not open public issues for exploits.
- Never log user content in production builds.
- Keep exports local; sharing is an explicit user action.
- Revoke/rotate any credential used for automation after use.

---

## 5. API contracts (v0.0.1)

### 5.1 Harness

```kotlin
interface UniversalHarness {
    suspend fun run(intent: Intent, onEvent: (HarnessEvent) -> Unit): HarnessResult
}
```

Events: `IntentReceived`, `Planned`, `AwaitingAuthorization`, `Executing`, `Verified`,
`Persisted`, `Completed`, `Rejected`.

### 5.2 Engine

```kotlin
interface InferenceEngine {
    val id: String
    val displayName: String
    val capabilities: Set<String>
    fun isAvailable(): Boolean
    suspend fun generate(request: GenerationRequest): GenerationResult
}
```

Availability must be truthful. An unavailable engine returns a typed `EngineUnavailable`
result. Chat never falls back to `LocalDeterministicEngine`; the deterministic engine runs
tools only. Remote adapters require explicit configuration and selection before any request.

### 5.3 Tool

```kotlin
interface Tool {
    val id: String
    val risk: RiskLevel
    val permission: String
    suspend fun execute(input: Map<String, String>): ToolResult
}
```

### 5.4 Data formats

- Memory / provenance / knowledge: UTF-8 JSON Lines (`.jsonl`), one object per line.
- Export: JSON object with `schema`, `version`, `exportedAt`, `memory`, `provenance`, `preferences`.
- Provenance hash: FNV-1a 64-bit over canonical field order, chained with `previousHash`.

---

## 6. Definition of done

A change is done when it is useful, tested, documented, reversible, offline-safe where
claimed, and does not violate the constitution. "Works on my machine" is not enough:
the CI workflow must be green and the APK must be installable.
