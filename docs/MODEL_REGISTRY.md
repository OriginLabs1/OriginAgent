# Model Registry — Origin AI

> Candidates, **not** a performance ranking. No weights are bundled in the APK.
>
> **Product rule (v0.0.4):** Origin Agent suggests only verified models it can fetch and run
> itself, and never substitutes a rule-based answer for a conversation. Every suggested chat
> model has a public URL, a pinned SHA-256, a published size and a compatible runtime. Tap
> **Get Model**: Origin downloads, verifies and activates it. You may also import a GGUF you
> already own; it is validated, copied into app-private storage and becomes a real selectable
> local model. An interrupted transfer keeps its verified partial file and resumes
> automatically; the user sees a friendly pause message, never a raw socket exception.

## 1. Strategic shortlist — shipped as real downloadable models

| # | Family | Downloadable artifact | Status |
|---:|---|---|---|
| 1 | Optimized sub-1B · starter | `SmolLM2-135M-Instruct-Q4_K_M.gguf` | ✅ Get Model · smallest |
| 2 | BitNet b1.58 2B4T | `bitnet-b1.58-2B-4T-tq1_0.gguf` | ✅ Get Model |
| 3 | LFM2.5-class | `LFM2.5-350M-Q4_K_M.gguf` | ✅ Get Model |
| 4 | Optimized sub-1B | `SmolLM2-360M-Instruct-Q4_K_M.gguf` | ✅ Get Model |
| 5 | Qwen-class compact | `qwen2.5-0.5b-instruct-q4_k_m.gguf` | ✅ Get Model |
| 6 | User-owned GGUF | Any compatible `.gguf` on this device | ✅ Import · validated locally |
| 7 | Emerging architectures / quantization | — | Watching; not suggested until downloadable |

## 1a. Verified artifacts

| Model | Artifact | Size | SHA-256 | License |
|---|---|---:|---|---|
| SmolLM2 135M · Pocket | [lmstudio-community/SmolLM2-135M-Instruct-GGUF](https://huggingface.co/lmstudio-community/SmolLM2-135M-Instruct-GGUF) | 105,454,144 B | `bda484992f9655d22504b14e57985257fa6a86937c61f957cf99c10a3bcae169` | [Apache-2.0](https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct/blob/main/LICENSE) |
| BitNet b1.58 2B4T | [ihtesham0345/bitnet-b1.58-2B-4T-llamakind](https://huggingface.co/ihtesham0345/bitnet-b1.58-2B-4T-llamakind) | 1,105,874,048 B | `db8852edfaa08476ab8476052c7777990cf73df16f2618604115a2c954e06f3a` | [MIT upstream weights](https://huggingface.co/microsoft/bitnet-b1.58-2B-4T/blob/main/LICENSE) |
| LFM2.5 350M | [LiquidAI/LFM2.5-350M-GGUF](https://huggingface.co/LiquidAI/LFM2.5-350M-GGUF) | 229,312,224 B | `7e6f72643caafc9a68256686638c4d7916f2cec76d1df478d4c3ddcd95a6aed4` | [LFM 1.0](https://huggingface.co/LiquidAI/LFM2.5-350M/blob/main/LICENSE) |
| Optimized sub-1B (SmolLM2 360M) | [lmstudio-community/SmolLM2-360M-Instruct-GGUF](https://huggingface.co/lmstudio-community/SmolLM2-360M-Instruct-GGUF) | 270,590,592 B | `e55c2a5df7f8f6eed1a843ff709fa8008dbce65d82e57c5c06dec5ee8d060415` | [Apache-2.0](https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct/blob/main/LICENSE) |
| Qwen-class compact (Qwen2.5 0.5B) | [Qwen/Qwen2.5-0.5B-Instruct-GGUF](https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF) | 491,400,032 B | `74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db` | [Apache-2.0](https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct/blob/main/LICENSE) |

The exact URL, size and checksum live in `DownloadableModels.kt` and are asserted by
`DownloadableModelTest`. Tap once in the picker: `ModelDownloadManager` streams the file to
app-private storage, checks the size and SHA-256, and only then promotes it into place.
The model is selectable at that point and works offline afterwards. Remove uses the same
screen. A failed or cancelled download never becomes active.

**BitNet note.** `bitnet.cpp` is an upstream research/CLI framework, not an Android SDK: it
publishes no AAR, APK or JNI binding, and its `I2_S` / `TL1` / `TL2` formats are different
from llama.cpp's `TQ1_0`. Origin therefore does **not** bundle bitnet.cpp in v0.0.4. The
artifact above is the same MIT-licensed 2B4T weights converted to llama.cpp-compatible
`TQ1_0` GGUF (architecture `bitnet`), executed by the bundled llama.cpp AAR. That is a
quantization of the named model, not a different model, but loadable is not the same as
benchmarked: upstream reports a ReLU²/SiLU graph mismatch and ARM TQ kernels that fall back
to scalar paths. Keep the pick's quality claims modest until the evidence log below has
real on-device results; the 220–490 MB Q4_K_M models remain the safer everyday choice on
4 GB phones. `BitnetCppEngine` stays research-only and is never selectable.

## 1b. Browse, import and switch

The model library reads real device memory and marks each suggested artifact as fitting
comfortably, workable or tight. The estimate includes the weight file plus a conservative
context/runtime overhead; it is guidance, not a benchmark. Small Q4_K_M models are placed
first on low-memory phones, while BitNet remains a first-class verified option.

**Browse** opens Hugging Face's GGUF search in the user's browser. The app also keeps curated
links to primary sources — Hugging Face TB, Qwen, Liquid AI and Microsoft BitNet — so users
can inspect files, licenses and upstream notes before downloading. The app makes no network
request just because a source is listed.

**Import .gguf** uses the Android document picker. Origin streams the chosen file into
app-private storage, checks the `GGUF` header, computes SHA-256 while copying, records the
size and keeps a stable SHA-derived id. An unreadable or non-GGUF file is rejected with a
friendly message. Imported models are labelled honestly: their licensing, quality and
architecture compatibility are the user's responsibility, and an unsupported file fails at
load time rather than being imitated by another engine.

Downloaded and imported models coexist. Only one is resident in the native runtime at a time;
switching releases the previous model safely and persists the choice locally. Removing a model
deletes its weights from app-private storage and clears the active selection if needed.

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
| `dev.ffmpegkit-maintained:llama-android` (llama.cpp) | GGUF Q4_K_M, TQ1_0 and compatible | ✅ arm64-v8a, API 24+ | Bundled AAR, MIT; all suggested artifacts and verified compatible imports load through this engine |
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

Web access is possible and controlled by the Settings switch, but built-in templates are
**candidates, not configured adapters**. Saving a template (or adding a custom adapter) and
explicitly selecting it is required before any request is made. A mere app launch makes no
remote call. Each answer is attributed to the exact configured model name. Failures are
reported as typed errors; they never fall back to a deterministic imitation of a chat.

Credentials and endpoints stay on the device. JSON exports contain endpoints and model names
but mask API keys.
