# Progress

Android conversion of אור ירח into a private couple organizer.
Branch: `feature/android-app`. Plan: see `docs/architecture/`.

**Threat model note:** the privacy boundary is the couple vs. the outside world, not
partner vs. partner. Both users share all data with each other; it is end-to-end
encrypted so that Supabase, an attacker, or a lost phone learns nothing. This
supersedes the private-by-default / opt-in-sharing model described in `task.md`.

## Phase 0 — Analysis
- [x] Analyze frontend (React 19 + Vite PWA, Hebrew/RTL, 5 pages)
- [x] Analyze backend — **there is none**; no API, no auth, no network code at all
- [x] Analyze database — none; zustand + `localStorage`, manual JSON export/import
- [x] Analyze authentication — none
- [x] Inventory domain logic to port (pregnancy math, budget, hospital-bag preset)
- [x] Inventory design tokens to port (`src/index.css` light/dark palette, fonts)
- [x] Architecture plan agreed with the user

## Phase 1 — Walking skeleton
- [x] Gradle 9.7 + AGP 9.3.1 project, version catalog, all versions stable
- [x] Convention plugins (application, library, compose, feature, room, jvm)
- [x] Bilingual resources (en default, he) + `localeConfig`, RTL enabled
- [x] `:core:model`, `:core:common` (AppResult / AppError, injectable dispatchers)
- [x] `:core:crypto` — record encryption; 32 crypto tests green
- [x] `:core:crypto` — key wrapping (HPKE) for pairing
- [x] `:core:crypto` — recovery phrase (BIP-39, official vectors)
- [ ] Theme ported from `src/index.css` (light + dark)
- [x] Supabase schema + RLS, first migration (25 access-control tests green)
- [ ] Auth (email/password, Keystore-backed session)
- [ ] Couple pairing end-to-end across two emulators
- [ ] Room + SQLCipher (tasks, cycles, sync operations)
- [ ] Sync engine (push, pull, cursor, offline queue)
- [ ] Tasks: list / add / edit, offline
- [ ] Cycle: log period start and end, history
- [ ] Security acceptance tests (ciphertext-only, outsider access, tamper)
- [x] Version derived from git tag (single source of truth)
- [x] CI workflow: test + lint
- [x] Release workflow: signed APK + SHA-256 + manifest on `v*` tags

## Phase 2 — In-app updater
- [ ] `:core:update` — VersionManager, ReleaseChecker, VersionComparator
- [ ] Semantic version comparison (incl. `1.9.0 < 1.10.0`, prereleases)
- [ ] Async startup check, non-blocking, offline-tolerant
- [ ] Update notification with release notes, Install / View Release / Later
- [ ] Download with progress + SHA-256 verification before install
- [ ] `PackageInstaller` install flow
- [ ] Mandatory-update support
- [ ] Persisted update state (last check, last notified, skipped)
- [ ] Manual "Check for updates" in Settings
- [ ] Updater tests (comparison, checker, download, checksum mismatch, UI)

## Phase 3 — Port the existing app
- [ ] Task model (full)
- [ ] Shopping list + budget + alternatives
- [ ] Important dates
- [ ] Home dashboard: moon countdown, weekly fruit/animal/info
- [ ] Hospital-bag preset
- [ ] JSON import from the web app
- [ ] Ported pregnancy and budget tests

## Phase 4 — Folders & documents
- [ ] Nested folders (5+ levels)
- [ ] SAF import
- [ ] Encrypted upload pipeline with checksums
- [ ] Preview (PDF, image, text, JSON, CSV)
- [ ] Attachments to tasks and folders

## Phase 5 — Scanner
- [ ] ML Kit document scanner, multi-page, PDF output
- [ ] Save to folder, optional task attachment

## Phase 6 — Cycle module (full)
- [ ] Cycle calendar (actual / predicted / estimated, distinguished without relying on color)
- [ ] Predictions from history, with insufficient-history handling
- [ ] Statistics
- [ ] Symptoms, flow, mood, pain, notes
- [ ] Cycle document attachments

## Phase 7 — Security hardening & settings
- [ ] Biometric lock, auto-lock, screenshot policy
- [ ] Locally scheduled notifications, generic text by default
- [ ] Device management and key rotation
- [ ] Recovery-phrase flow
- [ ] Settings tree

## Phase 8 — Search, calendar, conflicts, polish
- [ ] Room FTS5 search
- [ ] Unified calendar
- [ ] Conflict resolution UI
- [ ] Recurring tasks (rule-based)
- [ ] Tags
- [ ] Accessibility pass
- [ ] Performance (paging, indexes, thumbnails)

## Phase 9 — Release
- [ ] Debug build
- [ ] Release build
- [ ] Documentation
- [ ] Final report

## Deferred (P2)
OCR (Hebrew script is unsupported by ML Kit; cloud OCR is ruled out by the encryption
design), widgets, pregnancy mode, advanced analytics.
