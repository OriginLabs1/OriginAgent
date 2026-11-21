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

| Version | Release | Published APK SHA-256 |
|---|---|---|
| v0.0.3 | **Ready to publish** (Aurora UI + four verified GGUF models) | local `-PskipNative=true` build: `c8c2cd3a029725fbfeaca4aa0226a779598ff931a17622f182f3d4a885b3afdd`. CI publishes a native-layer APK with a different hash; use the release's `SHA256SUMS.txt` as authoritative. |
| v0.0.2 | <https://github.com/OriginLabs1/OriginAgent/releases/tag/v0.0.2> | `c2d2f1f258fa5936fce07168c35205cb2d3af52a41c4e9bca154f5042bc191b6` |
| v0.0.1 | <https://github.com/OriginLabs1/OriginAgent/releases/tag/v0.0.1> | `6e09a39aef75e156115ce58af15aa29a69aab8b2fccc896d890ced1818a58930` |

From v0.0.3 onward, the release workflow checksums only the APK built for the release tag, so archived local builds cannot be mistaken for published artifacts. The `SHA256SUMS.txt` attached to a GitHub Release is authoritative.
The local reference APK in `dist/` was built with `-PskipNative=true` (host NDK tools are
incompatible here), so its hash differs from the CI build, which includes the native layer.
Always verify downloaded APKs against the checksum file from the release:

```bash
sha256sum -c SHA256SUMS.txt
```

> **Publishing status:** `v0.0.1` and `v0.0.2` are live. `v0.0.3` is built, tested and staged
> in `dist/` but has not been pushed from this environment because no GitHub push credential
> is available. The GitHub Pages site is at <https://originlabs1.github.io/OriginAgent/>;
> the Cloudflare Pages project
> `origin-agent-kit` serves <https://origin-agent-kit.coreconcept1.net> and
> <https://origin-agent-kit.pages.dev>. GitHub Actions runs for release, build and Pages deploy
> all completed successfully.

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

## Publishing the staged v0.0.3 release

```bash
# from a machine with push credentials for OriginLabs1/OriginAgent
git add . && git commit -m "Origin Agent v0.0.3 — Aurora interface reimplementation"
git tag v0.0.3 && git push origin main --tags
# .github/workflows/release.yml builds the native APK and attaches it with SHA256SUMS.txt

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
