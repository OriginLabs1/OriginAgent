# Architecture — Origin Agent v0.0.3

## 1. Shape

Origin Agent is a single native Android application with a deliberately small, layered core.
The UI never talks directly to tools or engines; everything goes through the
**Universal Intelligence Harness (OUIH)**.

```
┌──────────────────────────────────────────────────────────────┐
│ Aurora UI over Material 3 (Compose)                          │
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

### 3.1 Local deterministic engine (tools only, always present)
A rule-based engine handles capability questions, arithmetic routing, trust status and
unknown-request declines for tools and status. It needs no model, network or native library.
It is deliberately **not chat-capable**: `chatCapable = false`, and the harness never calls it
for `engine.generate`. This keeps the app useful without AI while making the chat boundary honest.

### 3.2 Real AI engines
`InferenceEngine` is the only contract the harness knows. Chat-capable adapters:
- `LlamaEngine` — the four downloadable GGUF models, executed by the bundled MIT llama.cpp
  AAR (`LlamaRuntime` serialises one resident model at a time).
- `OpenAiCompatibleEngine` — real `/v1` endpoints the user configures (including local
  Ollama/LM Studio servers), gated by the Web access switch.
- Future engines must implement the same interface and pass the same availability checks.

`isAvailable()` performs real feature detection: native library load, verified weight file and
published size. Missing libraries or weights return `GenerationResult.Unavailable` /
`Failure`; the harness reports a typed error and there is **no rule-based fallback**.

### 3.2a Verified model installation
`ModelDownloadManager` streams a GGUF over HTTPS to app-private storage, checks the published
size and SHA-256, then promotes it and records verification. Failed or cancelled downloads are
never selectable. No weight file is bundled or accepted from the user.

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
| Model weights | GGUF | `filesDir/models` | Downloaded, SHA-256 verified; not exported |
| Preferences | SharedPreferences | app-private | Included in export |

No records leave the device unless the user explicitly shares an export.

## 5. UI system

- **Aurora** design system (v0.0.3) over Material 3: obsidian/day canvases, glass cards,
  gradient conversation turns, monospace micro-labels and a floating navigation dock.
- Dynamic color on Android 12+ remains user-controlled; Aurora accents keep identity stable.
- Shared primitives live in `ui/components/Aurora.kt`; screen composition lives in
  `ui/screens/Screens.kt`; navigation and global dialogs live in `MainActivity.kt`.
- Motion: ambient light-well drift and mark breathing, both disabled by Reduce motion.
- Accessibility: semantic labels, verified/attempted distinction, AA-tested contrast for both palettes.
- Full token and rationale reference: [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md).

## 6. Build and distribution

- Gradle wrapper + pinned plugin versions for reproducible builds.
- CI builds, tests and attaches a minified release APK to GitHub Releases on tags.
- F-Droid metadata mirrors the build. **Google Play is deliberately excluded.**

## 7. Invariants (tests enforce these)

1. No tool executes without an authorization decision.
2. Provenance chain verifies after a clean round trip and fails after tampering.
3. Unknown tools are rejected; unavailable AI engines report typed errors with no deterministic chat fallback.
4. Offline tools produce identical results with and without network.
5. Export contains all user-created memory and provenance records.
