# Benchmarks — Origin Agent

No numeric results are published yet. This file defines the protocol so future numbers are
comparable and honest.

## Protocol

1. **Devices**
   - Reference constrained: Samsung Galaxy S9, Snapdragon 845, 4 GB RAM, Android 10.
   - Reference current: ARM64 phone with 8 GB RAM, Android 14+.
   - Desktop baseline: x86-64 Linux, 16 GB RAM.
2. **Modes**: offline deterministic tools (not chat); the five downloadable, checksum-verified
   GGUF models through llama.cpp (SmolLM2 135M, BitNet 2B4T, LFM2.5 350M, SmolLM2 360M,
   Qwen2.5 0.5B); compatible imported GGUFs identified by SHA-256; opt-in web adapters. Research runtimes stay out of the protocol until a real artifact exists.
3. **Tasks**: 20 fixed prompts covering arithmetic, planning, memory recall, knowledge search,
   consent explanation, multilingual instruction following and refusal of unsafe actions.
4. **Metrics**
   - Task quality (rubric, 0–5, two independent raters)
   - Time to first token (ms)
   - Tokens/s after first token
   - Peak RSS (MB)
   - Energy proxy (battery % over 200 requests, thermal state)
   - Authorization correctness (no unapproved consequential action)
   - Provenance verification success rate
5. **Reproducibility**: record git SHA, Gradle/CMake versions, runtime commit, file SHA-256,
   OS build, command line and full raw output in `docs/benchmarks/<date>-<device>.json`.

## Rules

- Never publish a ranking without raw data.
- Never compare a 2B ternary model to a sub-1B model without normalizing for footprint.
- A model that fails authorization correctness is disqualified regardless of quality score.
