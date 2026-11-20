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
