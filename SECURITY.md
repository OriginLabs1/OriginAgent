# Security policy

## Supported versions

| Version | Supported |
|---|---|
| 0.0.x | ✅ |
| < 0.0.1 | ❌ |

## Reporting a vulnerability

Please **do not** open a public issue for a security problem.

Use GitHub's private security advisory flow:
`https://github.com/OriginLabs1/OriginAgent/security/advisories/new`

Include: affected version, device/OS, reproduction steps, impact, and any suggested fix.
You should receive an acknowledgement within 7 days (best effort, volunteer project).

## Security model

- Local-only by default; no telemetry, no mandatory network access.
- Consequential actions require authorization; the verifier records attempts and successes separately.
- Memory and provenance files are app-private. Exports happen only on explicit user action.
- Provenance uses a hash chain so tampering is detectable offline.
- Native code is small, dependency-free and mirrored by a Kotlin fallback.
- No credentials, tokens or signing keys are stored in the repository.

## Scope notes

Origin Agent is a foundational release. It is **not** a sandbox for untrusted arbitrary code,
and user-supplied model files should come from trusted sources. Report gaps accordingly.
