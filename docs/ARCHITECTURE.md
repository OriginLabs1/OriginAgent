# Architecture — Origin Agent v0.0.1

## 1. Shape

Origin Agent is a single native Android application with a deliberately small, layered core.
The UI never talks directly to tools or engines; everything goes through the
**Universal Intelligence Harness (OUIH)**.

```
┌──────────────────────────────────────────────────────────────┐
│ Material 3 Expressive UI (Compose)                           │
│ Origin · Capabilities · Memory · Trust · Settings            │
└───────────────────────────┬──────────────────────────────────┘
                            │ AgentViewModel (state + intents)
┌───────────────────────────▼──────────────────────────────────┐
│ UniversalIntelligenceHarness                                 │
│  Planner → Authorizer → ToolRegistry → Verifier → Ledger     │
└───────┬──────────────┬──────────────┬──────────────┬──────────┘
        │              │              │              │
   MemoryStore   KnowledgeBase   EngineRegistry   NativeBridge
   (JSONL)        (JSONL)        (adapters)       (C/C++ JNI)
```

## 2. Execution contract

The same eight stages apply to every consequential action:

| # | Stage | Artifact | Failure behavior |
|---|---|---|---|
| 1 | Intent | `Intent` (source, text, timestamp) | Reject malformed input |
| 2 | Plan | `Plan` (ordered `PlanStep`s, risk) | Emit `Rejected` event |
| 3 | Authorization | `AuthorizationDecision` per step | Step is `BLOCKED`; no execution |
| 4 | Execute | `ToolResult` or `GenerationResult` | `FAILED` with typed error |
| 5 | Verify | `Verification` (checks + verdict) | `ATTEMPTED`, never `VERIFIED` |
| 6 | Persist | Memory entries | Keep result, report persistence error |
| 7 | Provenance | Hash-chained `ProvenanceRecord`s | Chain stays verifiable |
| 8 | Result | `HarnessResult` + final answer | User sees exactly what happened |

**Attempted ≠ verified.** The UI renders these states distinctly and never claims success
for a blocked or failed action.

## 3. Intelligence layers

### 3.1 Local deterministic engine (always present)
A rule-based engine that answers capability questions, performs arithmetic routing,
explains trust status and gracefully declines unknown requests. It needs no model,
no network and no native library. This satisfies the "useful without AI or internet" rule.

### 3.2 Optional engines
`InferenceEngine` is the only contract the harness knows. Adapters:
- `BitnetCppEngine` — ternary models via `bitnet.cpp` (recommended direction).
- `LlamaCppEngine` — GGUF models via `llama.cpp` (default optional baseline).
- Future engines implement the same interface; no UI changes required.

`isAvailable()` must perform real feature detection. Missing native libraries produce an
`EngineUnavailable` result and automatic fallback — never a silent fake answer.

### 3.3 Native C/C++ layer
`src/main/cpp` contains a dependency-free library for provenance hashing, canonicalization
and intent normalization, exposed through JNI. Kotlin implementations mirror it as a
fallback, so the APK remains functional even if the native library is absent.

## 4. Data model

| Store | Format | Location | Portability |
|---|---|---|---|
| Memory | JSONL | `filesDir/origin/memory.jsonl` | Full JSON export |
| Knowledge | JSONL | bundled assets + user file | Plain text/JSON |
| Provenance | JSONL hash chain | `filesDir/origin/provenance.jsonl` | Verifiable offline |
| Preferences | SharedPreferences | app-private | Included in export |

No records leave the device unless the user explicitly shares an export.

## 5. UI system

- Material 3 Expressive with dynamic color on Android 12+; curated fallback palette otherwise.
- Adaptive two-pane layout on wide screens, bottom navigation on phones.
- Motion: spring-like state changes, animated status pills, subtle ambient orb.
- Accessibility: semantic labels, reduced-motion support, scalable typography, contrast-aware colors.

## 6. Build and distribution

- Gradle wrapper + pinned plugin versions for reproducible builds.
- CI builds, tests and attaches a debug APK to GitHub Releases on tags.
- F-Droid metadata mirrors the build. **Google Play is deliberately excluded.**

## 7. Invariants (tests enforce these)

1. No tool executes without an authorization decision.
2. Provenance chain verifies after a clean round trip and fails after tampering.
3. Unknown tools and unavailable engines are rejected/fallback, never guessed.
4. Offline tools produce identical results with and without network.
5. Export contains all user-created memory and provenance records.
