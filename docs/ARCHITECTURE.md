# Architecture — Origin Agent v0.0.4

## 1. Shape

Origin Agent is a single native Android application with a deliberately small, layered core.
The UI never talks directly to tools or engines; everything goes through the
**Universal Intelligence Harness (OUIH)**.

```
┌──────────────────────────────────────────────────────────────┐
│ Satin UI over Material You (Compose)                          │
│ Chat · Tasks · Make · Channels · Tools · Memory · Trust       │
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
- `LlamaEngine` — the five downloadable GGUF models and compatible imported GGUFs, executed by
  the bundled MIT llama.cpp AAR (`LlamaRuntime` serialises one resident model at a time).
- `OpenAiCompatibleEngine` — real `/v1` endpoints the user configures (including local
  Ollama/LM Studio servers), gated by the Web access switch.
- Future engines must implement the same interface and pass the same availability checks.

`isAvailable()` performs real feature detection: native library load, verified weight file and
published size. Built-in web templates are candidates only until the user saves a config; an
added adapter is available only once added. Missing libraries or weights return
`GenerationResult.Unavailable` / `Failure`; the harness reports a typed error and there is
**no rule-based fallback**.

### 3.2a Verified model installation
`ModelDownloadManager` streams a GGUF over HTTPS to app-private storage, resumes a valid `.part`
file with a checked `Range` request, retries interrupted transfers with friendly progress, and
checks the published size and SHA-256 before promoting it. Failed or cancelled downloads are
never selectable; cancellation never reports a failure and never discards a valid partial.
No weight file is bundled. A user-owned GGUF may be imported through the document picker:
`ImportedModelStore` validates the header, hashes it while copying into app-private storage
and exposes it as a real local engine. Both kinds coexist and switching is a local preference.

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
| Model weights | GGUF | `filesDir/models`, `filesDir/imported-models` | Downloaded or imported, SHA-256 verified; not exported |
| Duet workspace | JSONL | `filesDir/duet` | Tasks, channels and local draft pages |
| Preferences | SharedPreferences | app-private | Included in export |

No records leave the device unless the user explicitly shares an export.

## 5. UI system

- **Satin** design system (v0.0.4) over Material You: graphite/paper canvases, quiet cards,
  status-only colours, short paragraph replies and a floating four-tab dock.
- Dynamic color on Android 12+ remains user-controlled; Satin status semantics stay stable.
- New primitives live in `ui/components/DuetKit.kt`; workspace screens live in
  `ui/screens/DuetScreens.kt`; the Satin model library lives in
  `ui/screens/ModelLibraryDialog.kt`; legacy secondary dialogs remain in `ui/screens/Screens.kt`;
  navigation and global dialogs live in `MainActivity.kt`.
- Motion: the mic orb's spring pulse only while listening; disabled by Reduce motion.
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
