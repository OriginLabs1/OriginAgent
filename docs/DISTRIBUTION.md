# Distribution policy

## Where we distribute

| Channel | Use |
|---|---|
| GitHub source (`OriginLabs1/OriginAgent`) | Canonical source, issues, discussions |
| GitHub Releases | Primary APK distribution, source archives, checksums |
| GitHub Pages | Static product website deployed from `site/` via `.github/workflows/pages.yml` |
| Cloudflare Pages | `origin-agent-kit` project serving the same `site/` at `origin-agent-kit.coreconcept1.net` |
| F-Droid metadata (`metadata/com.oneorigin.originagent.yml`) | Free-software catalog readiness |
| Direct APK from a release | Supported with checksum verification |

## Current releases

| Version | Release | Reference APK SHA-256 (local build) |
|---|---|---|
| v0.0.2 | <https://github.com/OriginLabs1/OriginAgent/releases/tag/v0.0.2> (staged) | `9137109826965c29d0954c7d0f26a027e920ce6d13f901073e5daf4badc029db` |
| v0.0.1 | <https://github.com/OriginLabs1/OriginAgent/releases/tag/v0.0.1> (live) | `6e09a39aef75e156115ce58af15aa29a69aab8b2fccc896d890ced1818a58930` |

The v0.0.2 reference APK in `dist/` was built locally with `-PskipNative=true` (host NDK tools
are incompatible here). `.github/workflows/release.yml` rebuilds from the tag **with the native
layer enabled**, so the published CI APK will have a different hash. **The `SHA256SUMS.txt`
attached to the GitHub Release is authoritative** — always verify against that file, not the
local reference value. Verify with `sha256sum -c SHA256SUMS.txt` from the directory containing
the APKs.

> **Publishing status:** v0.0.1 is live. v0.0.2 artifacts are staged locally in `dist/` and must
> be published by a maintainer with credentials. No GitHub/Cloudflare token exists in this
> environment, so no remote state was changed.

## Where we do **not** distribute (for now)

- **Google Play Store** — explicitly out of scope. No Play App Signing, no Play SDK, no listing.
- Any store requiring mandatory proprietary account/telemetry.

## Release checklist

1. Version bump in `app/build.gradle.kts` (`versionCode`, `versionName`) and `CHANGELOG.md`.
2. `bash ./gradlew test -PskipNative=true` and `bash ./gradlew assembleRelease -PskipNative=true` green.
3. Compute SHA-256 of the APK and include it in the release notes and `dist/SHA256SUMS.txt`.
4. Tag `vX.Y.Z`; `.github/workflows/release.yml` builds and attaches the APK + checksums.
5. Update F-Droid metadata with version, hash and commit.
6. State in notes: local-first, no telemetry, web access opt-in, not on Google Play.

## Publishing the staged v0.0.2 release

```bash
# from a machine with GitHub credentials (gh authenticated as OriginLabs1)
git add . && git commit -m "Origin Agent v0.0.2 — universal model picker and adapters"
git tag v0.0.2 && git push origin main --tags
# .github/workflows/release.yml attaches dist/origin-agent-v0.0.2.apk and SHA256SUMS.txt

# Website — GitHub Pages: enable Settings > Pages > Source: GitHub Actions, then push.
# Website — Cloudflare Pages:
npx wrangler pages deploy site --project-name origin-agent-kit
# Then attach origin-agent-kit.coreconcept1.net in the Cloudflare dashboard.
```

## Verification for users

```bash
sha256sum origin-agent-<version>.apk
```

Compare with the value in the release notes. Install with:

```bash
adb install -r origin-agent-<version>.apk
```
