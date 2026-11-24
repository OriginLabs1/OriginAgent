# Roadmap

Evidence-gated, not date-gated. See `SPECS.md §2` for exit criteria.

| Phase | Theme | Status | Next proof |
|---|---|---|---|
| 0 | Constitution and governance | Done | Repo + docs published |
| 1 | Lightweight harness | Done | 15 JVM tests green, offline pipeline verified |
| 2 | Native C/C++ foundation | In progress | JNI sources complete; Kotlin fallback verified; CI build in progress |
| 3 | Satin native UI | Ready in 0.0.4 | Four-tab local workspace, contrast-tested palettes, reduced-motion support; device smoke still pending |
| 4 | Model adapters + benchmarks | In progress | Five verified GGUFs, memory-aware library, trusted browsing and GGUF import shipped; resumable downloads and honest registry updated. On-device benchmark log still pending |
| 5 | GitHub Releases distribution | Done through 0.0.3 | `v0.0.1`–`v0.0.3` published; `v0.0.4` awaiting verification and approval; no Play Store |
| 6 | GitHub Pages + Cloudflare Pages | Done | Both sites live; custom domain `origin-agent-kit.coreconcept1.net` active |
| 7 | Linux/desktop + distributed research | Planned | Shared harness runs off-device |

## Long horizon

- Distributed execution with verifiable provenance across devices.
- Additional languages (Zig/Rust) only where evidence shows benefit.
- New accelerators and quantization formats through the technology registry.
- Broader platform support without making any platform an architectural dependency.

**Principles remain stable; technology choices remain replaceable.**
