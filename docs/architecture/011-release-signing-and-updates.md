# 011 — Release signing and in-app updates

**Status:** Accepted (2026-08-15)

## Context

`docs/specs/02-auto-update.md` asks for git-tag-driven releases with an in-app update
prompt. The spec is written platform-generically — its manifest example lists Windows,
Linux and macOS assets — so it needs translating to Android, where self-updating has
constraints the spec does not mention.

The app is distributed privately to two phones. It is not going through Google Play.

## Decision

**Version = the newest `v*` git tag.** `versionName` and `versionCode` are derived at build
time by the `oryareach.android.application` convention plugin; no build file contains a
hand-written version. `versionCode = major*10000 + minor*100 + patch` (1.4.0 → 10400),
which stays ordered as long as minor and patch remain below 100 — enforced by a
`require` that fails the build rather than silently producing a lower code.

Git is read through a Gradle `ValueSource`, not by shelling out at configuration time, so
the configuration cache stays valid and git output is tracked as a build input.

Untagged builds report `0.0.0-dev` / versionCode 1, which sorts below every real release.

**Release pipeline.** `.github/workflows/android-release.yml` triggers on `v*` tags: run
tests and lint, build a signed release APK, compute SHA-256, generate `manifest.json` with
release notes taken from commit subjects since the previous tag, and publish a GitHub
Release with the APK, manifest and checksum file attached. Tests and lint run *before* the
build, so a failing tag produces no release.

**Signing key is permanent and secret.** Android refuses to install an update signed by a
different key than the installed app. Recovering from a lost signing key means uninstall and
reinstall, which **wipes all local data on that device** — for an end-to-end encrypted app
that also means losing whatever had not yet synced.

Therefore:

- The keystore is generated once and stored only as GitHub secrets
  (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`,
  `ANDROID_KEY_PASSWORD`), plus an offline backup the user keeps.
- It is never committed. `android/.gitignore` excludes `*.jks` and `*.keystore`.
- The release build reads it from the environment. When the secrets are absent, the release
  build stays **unsigned** rather than falling back to the debug key — a debug-signed APK
  would install fine on a clean device and then be permanently un-updatable.
- The release workflow fails loudly if `ANDROID_KEYSTORE_BASE64` is missing.

**Install flow.** Android cannot silently self-install. The updater uses the modern
`PackageInstaller` session API and requires `REQUEST_INSTALL_PACKAGES` plus a one-time user
grant of "Install unknown apps". Every install shows a system confirmation. "Restart the
application" in the spec means: session commits → system replaces the APK → app relaunches.

**Update checking.** Once per app start, cached, with a multi-hour floor before rechecking,
plus a manual "Check for updates" in Settings. The GitHub API allows 60 unauthenticated
requests per hour per IP, which this sits far below, so **no token is embedded in the app**.
A failed check is logged and otherwise ignored — it never blocks startup or shows an alarming
error.

**Update state** (`lastUpdateCheck`, `lastNotifiedVersion`, `skippedVersion`) lives in plain
DataStore, deliberately outside the encrypted workspace: it must be readable before the user
unlocks anything, and it contains nothing sensitive.

## Consequences

- Releasing is `git tag v1.3.0 && git push origin v1.3.0`. Nothing else is manual.
- Self-updating APKs violate Google Play policy. Irrelevant for private distribution, but
  recorded so a future decision to publish is not made in ignorance.
- The web PWA already has an equivalent mechanism (`vite-plugin-pwa` with
  `registerType: 'prompt'`, surfaced by `src/app/pwa-update-prompt.tsx`). This ADR covers
  Android only; no work is needed on the web side.
