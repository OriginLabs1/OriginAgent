# Distribution policy

## Where we distribute

| Channel | Use |
|---|---|
| GitHub source (`OriginLabs1/OriginAgent`) | Canonical source, issues, discussions |
| GitHub Releases | Primary APK distribution, source archives, checksums |
| F-Droid metadata (`metadata/com.oneorigin.originagent.yml`) | Free-software catalog readiness |
| Direct APK from a release | Supported with checksum verification |

## Current release

- `v0.0.1` — <https://github.com/OriginLabs1/OriginAgent/releases/tag/v0.0.1>
- APK SHA-256: `6e09a39aef75e156115ce58af15aa29a69aab8b2fccc896d890ced1818a58930`
- Verify with `sha256sum -c SHA256SUMS.txt`.

## Where we do **not** distribute (for now)

- **Google Play Store** — explicitly out of scope. No Play App Signing, no Play SDK, no listing.
- Any store requiring mandatory proprietary account/telemetry.

## Release checklist

1. Version bump in `app/build.gradle.kts` (`versionCode`, `versionName`) and `CHANGELOG.md`.
2. `bash ./gradlew test lint assembleRelease` green.
3. Compute SHA-256 of the APK and include it in the release notes.
4. Tag `vX.Y.Z`; GitHub Actions builds and attaches the APK.
5. Update F-Droid metadata with version, hash and commit.
6. State in notes: local-first, no telemetry, no Play Store.

## Verification for users

```bash
sha256sum origin-agent-<version>.apk
```

Compare with the value in the release notes. Install with:

```bash
adb install -r origin-agent-<version>.apk
```
