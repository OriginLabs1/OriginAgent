# Roadmap

Evidence-gated, not date-gated. See `SPECS.md §2` for exit criteria.

| Phase | Theme | Status | Next proof |
|---|---|---|---|
| 0 | Constitution and governance | Done | Repo + docs published |
| 1 | Lightweight harness | Done | 15 JVM tests green, offline pipeline verified |
| 2 | Native C/C++ foundation | In progress | JNI sources complete; Kotlin fallback verified; CI build in progress |
| 3 | Material 3 Expressive UI | In progress | Screens implemented; phone/tablet device smoke still pending |
| 4 | Model adapters + benchmarks | Planned | Evidence log populated; no weights bundled |
| 5 | GitHub Releases distribution | Done | `v0.0.1` APK + checksums published; no Play Store |
| 6 | Linux/desktop + distributed research | Planned | Shared harness runs off-device |

## Long horizon

- Distributed execution with verifiable provenance across devices.
- Additional languages (Zig/Rust) only where evidence shows benefit.
- New accelerators and quantization formats through the technology registry.
- Broader platform support without making any platform an architectural dependency.

**Principles remain stable; technology choices remain replaceable.**
