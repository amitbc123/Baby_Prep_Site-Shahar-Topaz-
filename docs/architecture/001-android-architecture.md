# 001 — Android architecture

**Status:** Accepted (2026-08-15)

## Context

Existing product: Vite + React 19 PWA, everything in `localStorage`. No backend, no API, no auth, no network code. Conversion spec wants real native Android app — explicitly not WebView wrapper — extended with folders, documents, scanning, menstrual cycle tracking, offline-first sync for two users.

## Decision

**Multi-module, feature-vertical.** `:core:*` holds shared infrastructure, `:feature:*` holds screens. Feature modules never depend on each other; navigation between them wired in `:app`. `:core:model`, `:core:common`, `:core:domain`, `:core:crypto` — pure Kotlin JVM modules, zero Android deps. Keeps domain/crypto logic testable without emulator.

**UI layer.** Compose + Material 3, MVVM with MVI-style state/effect split: one immutable `UiState` per screen exposed as `StateFlow`, plus `Channel`-backed effects stream for fire-once imperatives (navigation, snackbars) — effect emitted while backgrounded buffers instead of drops. Business logic stays out of composables.

**Data layer.** Room (SQLCipher-encrypted) = single source of truth for UI. Network never on render path — UI reads local data, background sync engine updates it. Errors mapped to `AppError` at repository boundary; platform exceptions never leak past it.

**DI: Koin**, not Hilt. Conversion spec names Koin explicitly (§5). Hilt would otherwise be default for pure-Android project, but pure-JVM modules integrate more simply with Koin — suits module layout above.

**Build.** Gradle convention plugins in `build-logic` (application, library, compose, feature, room, jvm) so ~15 modules don't each restate same config. Versions live in single catalog, all pinned to stable releases; JDK 21 (LTS) = toolchain anchor.

## Consequences

- AGP 9 has built-in Kotlin support, so `kotlin-android` plugin doesn't exist and library modules no longer accept `targetSdk`. Conventions written against real AGP 9 API.
- `allWarningsAsErrors` on for Kotlin; lint aborts on error. Dependency-freshness checks disabled — upgrades reviewed decision, not build failure.
- Pure-JVM core modules mean crypto/domain tests run in seconds on JVM — makes security acceptance tests practical to run on every change.