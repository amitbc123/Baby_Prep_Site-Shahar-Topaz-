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
- [x] Theme ported from `src/index.css` (light + dark palette, radius scale, both
      bundled variable fonts) — `:core:ui`, confirmed matching the web app live on-device
- [x] Supabase schema + RLS, first migration (25 access-control tests written; the
      hosted project's pgTAP harness currently fails to resolve `plan()` despite the
      extension being present — a role/search_path environment issue, not a regression
      from anything in this session; needs its own investigation)
- [x] Auth (email/password, Keystore-backed session)
- [x] Couple pairing — create-workspace path verified end-to-end on the emulator against
      the live project. Join-with-code/approve-device path is implemented but has no UI
      entry point yet (the app routes past `PairingScreen`'s `Ready` stage the instant the
      key unlocks); needs a Settings/device-management screen, planned for Phase 7.
- [x] Room + SQLCipher (tasks, cycles, sync operations)
- [x] Sync engine core: push/pull, cursor, conflict detection (11 tests)
- [x] Sync engine: Supabase wiring + WorkManager worker, wired to app startup and to
      every local write via `SyncTrigger`
- [x] Tasks: list / add / edit / toggle-done / delete, `:feature:tasks`, offline-first
      through `TaskRepository`'s outbox
- [x] Cycle: log period start and end, history, delete — `:feature:cycle`, offline-first
      through `CycleRepository`'s outbox; only user-entered dates are stored, length is
      computed at read time
- [x] Security acceptance tests, per the gate in the plan:
      - Ciphertext-only — verified manually on the live project for both a task write and
        a cycle write (title/dates do not appear anywhere in the stored row's ciphertext)
      - Outsider access → 0 rows, unauthenticated → denied, invitation expired/revoked
        rejected, third member blocked by the workspace cap — all in
        `supabase/tests/001_access_control.sql` (blocked from re-running right now by the
        pgTAP harness issue above, not un-written)
      - Tamper → fails AEAD, not silent corruption — `RecordCipherTest`,
        `KeyWrapTest` (`:core:crypto`)
      - Sync convergence / conflict-never-silently-overwrites — `SyncEngineTest`
        (`:core:sync`)
- [x] Version derived from git tag (single source of truth)
- [x] CI workflow: test + lint
- [x] Release workflow: signed APK + SHA-256 + manifest on `v*` tags

## Phase 2 — In-app updater
- [x] `:core:update` — VersionManager (reads installed versionName via PackageManager, no
      dependency on `:app`), ReleaseChecker, VersionComparator
- [x] Semantic version comparison (incl. `1.9.0 < 1.10.0`, prereleases; 5 tests green)
- [x] Async startup check, non-blocking, offline-tolerant — `UpdateViewModel.init` fires the
      check on a coroutine; a failed/offline check just clears `checking`, no crash, no dialog
      (verified live: repo has no GitHub release yet, checker gets 404, app launches clean)
- [x] Update notification with release notes, Install / View Release / Later — `UpdateDialog`
      in `:feature:update`, hosted at the app root (`SaharApp`'s `UpdateHost`) so it can
      interrupt any screen
- [x] Download with progress + SHA-256 verification before install — `UpdateDownloader`
      (Ktor `onDownload` progress callback) + `IntegrityVerifier`; a checksum mismatch deletes
      the file and surfaces as a failure, never installs
- [x] `PackageInstaller` install flow — `UpdateInstaller`, session-based, dynamically
      registered receiver for the commit result, handles `STATUS_PENDING_USER_ACTION` by
      handing the confirmation `Intent` back through an effect
- [x] Mandatory-update support — manifest's `mandatory` flag, or installed version below
      `minSupportedVersion`, both force `UpdateAvailability.Mandatory`; the dialog then has no
      dismiss/later/skip
- [x] Persisted update state (last check, last notified, skipped) — `UpdateState`, DataStore
      Preferences, deliberately outside the encrypted workspace so it's readable pre-unlock
- [ ] Manual "Check for updates" in Settings — `UpdateViewModel.onCheckNow()` exists and is
      wired for reuse, but there is no Settings screen yet to put the button on (Settings
      itself is Phase 7); tracked there, not a gap in the updater
- [x] Updater tests — `VersionComparatorTest` (5 cases incl. `1.9.0 < 1.10.0` and prerelease
      precedence); download/install/UI verified live on-device rather than with instrumented
      tests, since both need real network/PackageInstaller behavior

## Phase 3 — Port the existing app
- [x] Task model — already full from Phase 1 (title/category/priority/dueDate/assignee/note/done)
- [x] Shopping list + budget — `:feature:shopping`, `ShoppingItemRepository`, budget summary
      (estimated/spent/bought) live on the list using `:core:domain`'s `calculateBudget`;
      verified end-to-end on-device (create → mark bought → totals update correctly → syncs,
      ciphertext-only confirmed). Alternatives (per-item price comparison) not yet ported —
      the model/DB column exists (JSON list) but there's no UI to add one yet
- [x] Important dates — `:feature:dates`, `ImportantDateRepository`, Material3 `DatePicker`;
      verified end-to-end on-device including sync
- [ ] Home dashboard: moon countdown, weekly fruit/animal/info — not started
- [ ] Hospital-bag preset — not started (seed-12-tasks action)
- [ ] JSON import from the web app — not started
- [x] Ported pregnancy and budget tests — `:core:domain` (`PregnancyProgressTest`,
      `BudgetTest`, `DailyMessageTest`), near line-for-line port of the Vitest suite

  New this phase: `:core:domain` (pure Kotlin — `daysUntil`/`getPregnancyProgress`/
  `isPastDate`, `calculateBudget`/`itemEffectivePrice`, `dailyMessageIndex`), Room migration
  1→2 adding `shopping_items`/`important_dates` tables, `RoomSyncStore` generalized from a
  hardcoded two-table `when` to check all four tables (was a latent bug waiting for a third
  entity type). Weekly pregnancy-info/fruit/animal copy and the daily-message text are left
  as bilingual string resources for the home-dashboard work, not ported as data here.

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
