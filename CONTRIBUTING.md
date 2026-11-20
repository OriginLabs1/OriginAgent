# Contributing to Origin Agent

Thank you for helping build universal, trustworthy intelligence.

## Before you start

Read, in order: `CONSTITUTION.md`, `CONTEXT.md`, `SPECS.md`, `docs/ARCHITECTURE.md`.

## Ground rules

- No change may bypass `Intent → Plan → Authorization → Execute → Verify → Persist → Provenance → Result`.
- No mandatory account, cloud, telemetry, ads or proprietary SDK.
- No model weights, large binaries or generated build output in commits.
- No Google Play Store changes (out of scope for now).
- Keep dependencies minimal; document additions in `docs/DEPENDENCIES.md`.
- Be kind and assume good intent. See `CODE_OF_CONDUCT.md`.

## Pull requests

1. Fork, branch from `main`, keep the change focused.
2. Add or update tests for behavior changes.
3. Run `./gradlew test lint assembleDebug`.
4. Update docs (`CONTEXT.md` current state, `SPECS.md` if workflows change).
5. Describe what changed, why, how verified and what remains.

Commit style: `feat(scope): summary`, `fix(scope): summary`, `docs: summary`, `test: summary`.

## Reporting bugs

Use the issue template. Include device, Android version, app version, steps, expected vs actual,
and whether the device was offline. Never include private keys or personal data.

## Security

See `SECURITY.md` for private disclosure. Do not publish exploit details in public issues.
