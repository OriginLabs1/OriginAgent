# Model Registry — Origin AI

> Candidates, **not** a performance ranking. Nothing here is bundled in the APK.
> Promotion requires reproducible evidence on the intended hardware.

## 1. Strategic shortlist

| Priority | Candidate direction | Why we evaluate it | Status |
|---:|---|---|---|
| 1 | BitNet b1.58 2B4T | Ternary inference, exceptional intelligence-per-memory | Candidate; adapter planned |
| 2 | LFM2.5-class | Compact, efficient edge intelligence | Candidate; adapter planned |
| 3 | Optimized sub-1B | Fast, everyday responsiveness on 4 GB devices | Candidate |
| 4 | Qwen-class compact | Strong conventional baseline and broad task capability | Candidate |
| 5 | Emerging architectures / quantization | Discover superior intelligence-efficiency combinations | Watching |

## 2. Selection criteria

1. Actual task quality and reasoning
2. Memory footprint
3. Response speed / time to first token
4. Energy use and thermal sustainability
5. Reliability and instruction adherence
6. Context length and multilingual capability
7. Runtime and hardware compatibility
8. Licensing and distribution rights
9. Portability and long-term maintainability

## 3. Runtime compatibility matrix

| Runtime | Quantization / format | Android 8+ ARM64 | Notes |
|---|---|---|---|
| `bitnet.cpp` | ternary (b1.58) | Target | Preferred for ternary; build separately, load via JNI adapter |
| `llama.cpp` | GGUF (Q4_K_M, Q5, etc.) | Target | Optional default baseline; must not be forced onto ternary weights |
| Deterministic local engine | none | ✅ Always | Baseline for offline usefulness and verification |

## 4. Evidence log

No benchmark claims yet. This section is intentionally empty until results are reproducible.
Each entry must include: date, device, OS, runtime commit, model file hash, exact command,
task suite, quality score, peak RSS, TTFT, tokens/s, energy observation and license link.

## 5. Integration checklist for a new engine

- [ ] License permits redistribution or user-supplied loading.
- [ ] `isAvailable()` checks real native/library/model presence.
- [ ] `generate()` returns typed errors; no silent fallback that misattributes output.
- [ ] Harness fallback to deterministic engine tested.
- [ ] Benchmark entry added with command and hardware.
- [ ] UI capability text describes trade-offs honestly.

## 6. Web adapters (v0.0.2)

The picker is not limited to user-supplied local weights. It offers two legitimate paths:

1. **Built-in web adapter templates** — OpenAI, OpenRouter, Groq, Together, DeepSeek, Ollama
   and LM Studio. Each prefills a documented OpenAI-compatible `/v1` endpoint and model name.
2. **Added adapters** — "Add adapter" accepts any documented OpenAI-compatible endpoint the
   user trusts. Added adapters are stored locally and are removable from the picker.

Web access is **possible and enabled by default** but controlled by the Settings switch
`Web access (for web adapters)`. No network request occurs until a web adapter is selected,
and the harness attributes each answer to the exact configured model name. `Test` calls
`GET {endpoint}/models`; generation calls `POST {endpoint}/chat/completions`. Failures are
reported as typed errors and fall back to the deterministic local engine without misattribution.

Credentials and endpoints stay on the device. JSON exports contain endpoints and model names
but mask API keys. Local adapters (bitnet.cpp / llama.cpp) still require user-supplied weights
and a compiled native bridge; they never claim availability without one.
