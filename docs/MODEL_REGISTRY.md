# Model Registry — Origin AI

> Candidates, **not** a performance ranking. No weights are bundled in the APK.
>
> **Product rule (v0.0.3):** Origin Agent never suggests a model whose weights the user must
> supply, and never substitutes a rule-based answer for a conversation. Every chat model in
> the picker has a public URL, a pinned SHA-256, a published size and a runtime that can
> actually execute it. Tap **Get Model**: Origin downloads, verifies and activates it.

## 1. Strategic shortlist — shipped as real downloadable models

| # | Family | Downloadable artifact | Status |
|---:|---|---|---|
| 1 | BitNet b1.58 2B4T | `bitnet-b1.58-2B-4T-tq1_0.gguf` | ✅ Get Model |
| 2 | LFM2.5-class | `LFM2.5-350M-Q4_K_M.gguf` | ✅ Get Model |
| 3 | Optimized sub-1B | `SmolLM2-360M-Instruct-Q4_K_M.gguf` | ✅ Get Model |
| 4 | Qwen-class compact | `qwen2.5-0.5b-instruct-q4_k_m.gguf` | ✅ Get Model |
| 5 | Emerging architectures / quantization | — | Watching; not suggested until downloadable |

## 1a. Verified artifacts

| Model | Artifact | Size | SHA-256 | License |
|---|---|---:|---|---|
| BitNet b1.58 2B4T | [ihtesham0345/bitnet-b1.58-2B-4T-llamakind](https://huggingface.co/ihtesham0345/bitnet-b1.58-2B-4T-llamakind) | 1,105,874,048 B | `db8852edfaa08476ab8476052c7777990cf73df16f2618604115a2c954e06f3a` | [MIT upstream weights](https://huggingface.co/microsoft/bitnet-b1.58-2B-4T/blob/main/LICENSE) |
| LFM2.5 350M | [LiquidAI/LFM2.5-350M-GGUF](https://huggingface.co/LiquidAI/LFM2.5-350M-GGUF) | 229,312,224 B | `7e6f72643caafc9a68256686638c4d7916f2cec76d1df478d4c3ddcd95a6aed4` | [LFM 1.0](https://huggingface.co/LiquidAI/LFM2.5-350M/blob/main/LICENSE) |
| Optimized sub-1B (SmolLM2 360M) | [lmstudio-community/SmolLM2-360M-Instruct-GGUF](https://huggingface.co/lmstudio-community/SmolLM2-360M-Instruct-GGUF) | 270,590,592 B | `e55c2a5df7f8f6eed1a843ff709fa8008dbce65d82e57c5c06dec5ee8d060415` | [Apache-2.0](https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct/blob/main/LICENSE) |
| Qwen-class compact (Qwen2.5 0.5B) | [Qwen/Qwen2.5-0.5B-Instruct-GGUF](https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF) | 491,400,032 B | `74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db` | [Apache-2.0](https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct/blob/main/LICENSE) |

The exact URL, size and checksum live in `DownloadableModels.kt` and are asserted by
`DownloadableModelTest`. Tap once in the picker: `ModelDownloadManager` streams the file to
app-private storage, checks the size and SHA-256, and only then promotes it into place.
The model is selectable at that point and works offline afterwards. Remove uses the same
screen. A failed or cancelled download never becomes active.

**BitNet note.** Microsoft's published `i2_s` GGUF uses a tensor type the Android llama.cpp
runtime cannot load. The artifact above is the same MIT-licensed 2B4T weights converted to
llama.cpp-compatible `TQ1_0` GGUF (architecture `bitnet`, which the runtime supports). This
is a quantization of the named model, not a different model.

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
| `dev.ffmpegkit-maintained:llama-android` (llama.cpp) | GGUF Q4_K_M, TQ1_0 and compatible | ✅ arm64-v8a, API 24+ | Bundled AAR, MIT; all four artifacts above load through this engine |
| Deterministic local engine | none | ✅ Always | Tools, status and knowledge only — never chat |

Local GGUF inference needs an arm64-v8a device. On other ABIs the picker says the local
runtime is unavailable and only web adapters can be used; it never pretends a model loaded.

## 4. Evidence log

No numeric benchmark claims yet. This section stays empty until results are reproducible.
Each entry must include: date, device, OS, runtime commit, model file hash, exact command,
task suite, quality score, peak RSS, TTFT, tokens/s, energy observation and license link.

## 5. Integration checklist for a new engine

- [ ] License permits redistribution and in-app download.
- [ ] A public HTTPS URL, size and SHA-256 are published in `DownloadableModels.kt`.
- [ ] The GGUF architecture and quantization are supported by the bundled llama.cpp.
- [ ] A regression test fails if the catalog loses a URL, checksum or size.
- [ ] `isAvailable()` checks real native/library/model presence.
- [ ] `generate()` returns typed errors; there is no rule-based fallback.
- [ ] Benchmark entry added with command and hardware.
- [ ] UI capability text describes trade-offs honestly.

## 6. Web adapters

The picker also offers real remote AI through OpenAI-compatible endpoints:

1. **Built-in templates** — OpenAI, OpenRouter, Groq, Together, DeepSeek, Ollama and LM Studio.
2. **Added adapters** — any documented OpenAI-compatible endpoint the user trusts.

Web access is **possible and enabled by default** but controlled by the Settings switch. No
network request occurs until a web adapter is selected, and each answer is attributed to the
exact configured model name. Failures are reported as typed errors; they never fall back to
a deterministic imitation of a chat.

Credentials and endpoints stay on the device. JSON exports contain endpoints and model names
but mask API keys.
