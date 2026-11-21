# Roadmap

Evidence-gated, not date-gated. See `SPECS.md §2` for exit criteria.

| Phase | Theme | Status | Next proof |
|---|---|---|---|
| 0 | Constitution and governance | Done | Repo + docs published |
| 1 | Lightweight harness | Done | 15 JVM tests green, offline pipeline verified |
| 2 | Native C/C++ foundation | In progress | JNI sources complete; Kotlin fallback verified; CI build in progress |
| 3 | Aurora native UI | Shipped in 0.0.3 | Five-screen redesign, contrast-tested palettes, reduced-motion support; device smoke still pending |
| 4 | Model adapters + benchmarks | In progress | Web adapters + picker shipped in 0.0.2; native bridges and evidence log still pending |
| 5 | GitHub Releases distribution | Done | `v0.0.1` and `v0.0.2` published; `v0.0.3` Aurora release in progress; no Play Store |
| 6 | GitHub Pages + Cloudflare Pages | Done | Both sites live; custom domain `origin-agent-kit.coreconcept1.net` active |
| 7 | Linux/desktop + distributed research | Planned | Shared harness runs off-device |

## Long horizon

- Distributed execution with verifiable provenance across devices.
- Additional languages (Zig/Rust) only where evidence shows benefit.
- New accelerators and quantization formats through the technology registry.
- Broader platform support without making any platform an architectural dependency.

**Principles remain stable; technology choices remain replaceable.**
