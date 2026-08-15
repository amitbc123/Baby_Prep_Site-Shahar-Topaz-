# 001 — Android architecture

**Status:** Accepted (2026-08-15)

## Context

The existing product is a Vite + React 19 PWA storing everything in `localStorage`. It has
no backend, no API, no authentication and no network code. The conversion spec asks for a
real native Android app — explicitly not a WebView wrapper — extended with folders,
documents, scanning, menstrual cycle tracking and offline-first sync for two users.

## Decision

**Multi-module, feature-vertical.** `:core:*` holds shared infrastructure, `:feature:*` holds
screens. Feature modules never depend on each other; navigation between them is wired in
`:app`. `:core:model`, `:core:common`, `:core:domain` and `:core:crypto` are pure Kotlin JVM
modules with zero Android dependencies, which keeps the domain and crypto logic testable
without an emulator.

**UI layer.** Compose + Material 3, MVVM with an MVI-style state/effect split: one immutable
`UiState` per screen exposed as `StateFlow`, and a `Channel`-backed effects stream for
fire-once imperatives (navigation, snackbars) so an effect emitted while backgrounded
buffers instead of being dropped. Business logic stays out of composables.

**Data layer.** Room (SQLCipher-encrypted) is the single source of truth for the UI. The
network is never on the render path — the UI reads local data and a background sync engine
updates it. Errors are mapped to `AppError` at the repository boundary; platform exceptions
never leak past it.

**DI: Koin**, not Hilt. The conversion spec names Koin explicitly (§5). Hilt would otherwise
be the default for a pure-Android project, but pure-JVM modules integrate more simply with
Koin, which suits the module layout above.

**Build.** Gradle convention plugins in `build-logic` (application, library, compose,
feature, room, jvm) so ~15 modules do not each restate the same configuration. Versions live
in a single catalog, all pinned to stable releases; JDK 21 (LTS) is the toolchain anchor.

## Consequences

- AGP 9 has built-in Kotlin support, so the `kotlin-android` plugin does not exist and
  library modules no longer accept `targetSdk`. The conventions are written against the real
  AGP 9 API.
- `allWarningsAsErrors` is on for Kotlin; lint aborts on error. Dependency-freshness checks
  are disabled because upgrades are a reviewed decision, not a build failure.
- Pure-JVM core modules mean crypto and domain tests run in seconds on the JVM, which is what
  makes the security acceptance tests practical to run on every change.
