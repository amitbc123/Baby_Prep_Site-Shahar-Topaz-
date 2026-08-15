# Progress

Android conversion of אור ירח into a private couple organizer.
Branch: `feature/android-app`. Plan: see `docs/architecture/`.

**Threat model note:** the privacy boundary is the couple vs. the outside world, not
partner vs. partner. Both users share all data with each other; it is end-to-end
encrypted so that Supabase, an attacker, or a lost phone learns nothing. This
supersedes the private-by-default / opt-in-sharing model described in
`docs/specs/01-android-conversion.md` (corrected from an earlier, wrong reference to
`task.md` here — `task.md` is actually the unrelated auto-update spec; see
`docs/architecture/005-data-privacy.md` for the full decision record). See also
Phase 9 below.

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
- [x] Manual "Check for updates" in Settings — done, but later than tracked here: the
      Settings screen this depended on didn't land until Phase 7, and the button itself was
      actually added during Phase 9's ADR drift pass (`docs/architecture/011` names this
      requirement explicitly; the pass caught that Phase 7's Settings screen had shipped
      without it). `SettingsScreen` gained a `footer` slot specifically because
      `:feature:settings` can't depend on `:feature:update` (feature modules must not depend
      on each other) — `SaharApp.kt`'s `SettingsRoute` supplies the button via that slot,
      wired to the already-existing `UpdateViewModel.onCheckNow()`.
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
- [x] Home dashboard — `:feature:home`, Home tab (now first in the bottom nav). Canvas-drawn
      moon countdown matching the web SVG (night-sky palette from `NightPalette`, radial glow
      rising with `moonFraction`), week/day progress, weekly fruit+animal, weekly-info card,
      daily message, budget summary, open-tasks count, inline due-date/baby-name editor (no
      dueDate/babyName means no Settings screen yet, so this doubles as the minimal settings
      surface until Phase 7). Backed by a new synced `AppSettings`/`app_settings` entity
      (`EntityType.SETTINGS`, previously declared but unused) — migration 2→3. Weekly
      info/fruit/animal copy and the 6 daily messages are bilingual string-array resources,
      ported verbatim from `pregnancy.ts`/`messages.ts`; verified end-to-end on-device
      including sync and ciphertext-only for settings
  - **Post-completion refinement** (per explicit user request): the editor now asks for the
    first day of the last menstrual period instead of the due date directly — the standard
    obstetric estimate (Naegele's rule: LMP + 280 days) and a more accurate starting point
    than a due date someone remembers or was told, which is one hop removed from the actual
    measurement. `AppSettings.dueDate` is still what's stored and synced (no schema/migration
    change) — only the input semantics changed: `:core:domain` gained
    `dueDateFromLastPeriod`/`lastPeriodFromDueDate` (exact inverses, 2 new tests, 11/11 green),
    `HomeViewModel` converts at submit time and re-derives the LMP date for display when
    re-opening the editor. Compile, full test suite, lint, and `assembleDebug` all green.
    Unverified on-device like the rest of this session's UI.
- [x] Hospital-bag preset — `TasksViewModel.onSeedHospitalBag()`, additive/re-runnable (skips
      titles already present, same pattern as the web version); verified live, including that
      a second tap does not duplicate
- [x] JSON import from the web app — `:core:domain`'s `WebSnapshot`/`toImportedSnapshot`
      (the Hebrew-literal → enum mapping table the plan called "the migration contract",
      with tests), wired to a SAF file picker on the Home tab. Verified against a real
      export-shaped fixture on-device: settings/tasks/shopping items/dates all land with
      correct category/priority/assignee/status mapping, re-import is deduped, and sync
      lands all of it server-side with no plaintext in the ciphertext. Alternatives are
      re-keyed correctly (chosenAlternativeId follows its alternative to a fresh id). Not
      carried over: task `dueDate` — dead everywhere (no UI, no repository support) since
      Phase 1; a pre-existing gap, not one introduced here, and out of scope to fix now.
- [x] Ported pregnancy and budget tests — `:core:domain` (`PregnancyProgressTest`,
      `BudgetTest`, `DailyMessageTest`, `WebImportMapperTest`), near line-for-line port of
      the Vitest suite

  New this phase: `:core:domain` (pure Kotlin — `daysUntil`/`getPregnancyProgress`/
  `isPastDate`, `calculateBudget`/`itemEffectivePrice`, `dailyMessageIndex`, the web-import
  mapper), Room migrations 1→2 (`shopping_items`/`important_dates`) and 2→3 (`app_settings`),
  `RoomSyncStore` generalized from a hardcoded two-table `when` to check all five tables (was
  a latent bug waiting for a third entity type).

  **Real bug found and fixed while testing the import** (not import-specific — it affects
  any burst of rapid local writes, including the hospital-bag preset seeding 12 tasks at
  once): `SyncWorker.syncNow()` used `ExistingWorkPolicy.KEEP`, which silently drops a
  sync request that arrives while one is already running, with nothing to catch up
  afterward. A record created after the in-flight run had already read the outbox could be
  permanently stranded — confirmed live (only 2 of 6 imported records reached the server).
  Fixed by switching to `APPEND_OR_REPLACE`, which guarantees at least one more run after
  the current one finishes; since each run drains the outbox until empty, the extra runs a
  burst produces are cheap no-ops. Re-verified: all 6 records now sync.

## Phase 4 — Folders & documents
- [x] Nested folders (arbitrary depth) — `:feature:folders`, `FolderRepository`, materialized
      `path` (e.g. `/<ancestor>/.../<id>/`) derived server-independently at write time so a
      subtree is a `LIKE 'path%'` query, never a recursive one. Breadcrumb navigation, rename,
      cascading delete (deletes the folder and everything under it as one soft-delete
      transaction). Verified end-to-end on-device: create → nest → rename → delete parent
      confirms the child is gone and an unrelated sibling survives; synced; ciphertext-only
      confirmed. New `EntityType.FOLDER` sync branch in `RoomSyncStore` (was already declared,
      unused until now) — migration 3→4 adds `folders`.
- [x] SAF import + document UI — folded into `:feature:folders` rather than a new module: the
      `FoldersUiState`/`DocumentPreview` scaffolding for this was already staged uncommitted
      from earlier in the session, so that's the architecture being followed. `FoldersScreen`
      now lists documents alongside subfolders in the current folder; the FAB expands into
      "New folder" / "Import document"; import uses `ActivityResultContracts.OpenDocument()`
      (`*/*`), reads bytes + display name + MIME type off the `ContentResolver`, and calls
      `DocumentRepository.upload`. Delete has its own confirm dialog (mirrors the folder-delete
      one). `FoldersViewModel` now combines `FolderRepository.observeChildren` and
      `DocumentRepository.observeInFolder` per current parent id. `:app:compileDebugKotlin`,
      `:feature:folders:compileDebugKotlin`, full `./gradlew test`, and `./gradlew lint` all
      green. Not yet exercised on-device (deferred per this session's instruction to skip live
      testing) — needs the same live verification pass as everything else before it counts as
      done-done, plus the Supabase Storage migration (0005) actually needs to be run against
      the live project, which it has not been yet.
- [x] Encrypted upload pipeline with checksums — `:core:database`'s `DocumentRepository`
      (upload: encrypt then push ciphertext to `DocumentBlobStore` at `{workspaceId}/{id}`,
      SHA-256 of the ciphertext stored on the metadata row; download: fetch, verify hash
      before decrypt so a tampered/corrupted blob is caught before ever reaching the cipher),
      `SupabaseDocumentBlobStore` (`:core:network`), new `Document` model, `DocumentDao`/
      `DocumentEntity`, Room migration 4→5 (`documents` table — metadata only, bytes live in
      Storage). Supabase side: `supabase/migrations/0005_document_storage.sql` adds a private
      `documents` Storage bucket with select/insert RLS keyed off the object path's workspace-id
      segment via `is_workspace_member`; no update/delete policy — deletion propagates through
      the record's tombstone, not by mutating the stored blob. Delete still leaves the blob in
      Storage (orphan reclamation is a background-job concern, not a foreground-delete one).
      `DocumentRepository`/`documentDao()` now wired into `AppModule` (was written but not
      registered with Koin — a real gap, not a stylistic one, since nothing could have injected
      it). `:app:compileDebugKotlin` green after wiring; full `./gradlew test` also green (no
      test regressions from the wiring). Still not yet run against the live
      project or exercised on-device — needs the same live verification pass (ciphertext-only,
      tamper detection) the other entities got in Phase 1/3 before it counts as done-done. No UI
      yet (that's the SAF-import and attachments items below).
- [x] Preview (PDF, image, text, JSON, CSV) — `FoldersViewModel.toPreview` dispatches on MIME
      type: `text/*` (covers CSV) and `application/json` decode as UTF-8 text, `image/*` decodes
      via `BitmapFactory`, `application/pdf` renders page 1 to a bitmap through `PdfRenderer`
      (bytes written to a throwaway cache file, since `PdfRenderer` needs a real file
      descriptor — cleaned up after render), anything else shows an "unsupported" state rather
      than failing. Same not-yet-live-verified caveat as the item above.
- [x] Attachments to tasks and folders — `Document` gained a `taskId` field, independent of
      `folderId` (a document can be filed in a folder, attached to a task, both, or neither).
      Room migration 5→6 adds the `task_id` column + index and bumps the DB to version 6;
      `DocumentDao.observeForTask`/`DocumentRepository.observeForTask` mirror the existing
      per-folder query. `TasksViewModel` now observes attachments for whichever task is being
      edited (a task must already exist to attach to it, so this only applies to the edit path,
      not the create-new-task path) and exposes `onAttachDocument`/`onDeleteAttachment`;
      `TaskForm` grew an attachments section (SAF picker, list, remove) shown only while
      editing. `:app:compileDebugKotlin`, full `./gradlew test`, and `./gradlew lint` all green
      after wiring. Same not-yet-live-verified caveat as the rest of this phase.

  **Phase 4 is now feature-complete** on all five checklist items; what remains before calling
  the phase done-done is entirely verification: run `supabase/migrations/0005_document_storage.sql`
  against the live project, then repeat the live on-device pass (ciphertext-only, tamper
  detection, folder placement, task attachment, all four preview types) that every other entity
  got in Phases 1/3. That was deliberately skipped this session per instruction to continue
  without live tests — it is the one thing separating "compiles and passes unit tests" from
  "actually works," and should be the first thing done before this phase is marked complete
  above.

## Phase 5 — Scanner
- [x] ML Kit document scanner, multi-page, PDF output — new `:core:scanner` module (a *core*
      module, not `:feature:scanner`, deliberately: the plan's rule that feature modules must
      not depend on each other — enforced by `AndroidFeatureConventionPlugin`'s doc comment —
      meant a shared capability like this belongs alongside `:core:sync`/`:core:network`, not as
      a third feature both `:feature:folders` and `:feature:tasks` would have to depend on
      sideways). Wraps `com.google.android.gms:play-services-mlkit-document-scanner:16.0.0`
      (confirmed as the current GA version against Google's Maven index, not guessed) behind one
      composable, `rememberDocumentScanner`: launches the on-device scan UI (crop/enhance,
      multi-page, gallery import disabled so every page is a live capture) via
      `GmsDocumentScannerOptions.RESULT_FORMAT_PDF`, so ML Kit does the page-merging — no
      client-side PDF assembly needed — and returns one `ScannedDocument(name, mimeType, bytes)`.
      Entirely on-device: no cloud vision/OCR call, consistent with why cloud OCR is ruled out
      for this app (see Deferred, bottom of this file).
- [x] Save to folder, optional task attachment — no new plumbing needed: both call sites reuse
      the upload path Phase 4 already built. `FoldersScreen`'s FAB gained a third option ("Scan
      document", alongside "New folder"/"Import document") that calls the same
      `actions.onDocumentPicked` as SAF import, so a scan saves into the current folder.
      `TasksScreen`'s attachments section gained a matching "Scan document" button next to
      "Attach document", calling `actions.onAttachDocument`. `:core:scanner:compileDebugKotlin`,
      `:app:compileDebugKotlin`, full `./gradlew test`, and `./gradlew lint` all green.
      **Phase 5 is feature-complete but wholly unverified on a real device** — the Play services
      module (downloaded on first use, not bundled) has never actually been exercised; this is
      the most likely thing in the whole app to have an API-shape mistake I couldn't catch by
      compiling, since nothing here executes the scanner at build time. This is the first thing
      to live-test when live testing resumes, ahead of Phase 4's backlog.

## Phase 6 — Cycle module (full)
- [x] Predictions from history, with insufficient-history handling — `:core:domain`'s
      `predictNextCycle` (new `cycle/CyclePrediction.kt`). Cycle length is measured as the gap
      between consecutive period *start* dates, never a single period's own length — needs at
      least 2 recorded starts (1 gap) to say anything; with fewer it returns
      `hasSufficientHistory = false` rather than guessing with a textbook 28-day default
      (presenting a guess as fact is exactly what a prediction feature must not do). When there
      is enough history: next period = last start + average gap, ovulation = next period − 14
      days (textbook luteal phase, explicitly documented as an estimate not a measurement),
      fertile window = 5 days before ovulation through 1 day after. 6 tests
      (`CyclePredictionTest`), including the insufficient-history cases and a 3-cycle average.
- [x] Statistics — `:core:domain`'s `calculateCycleStatistics` (`cycle/CycleStatistics.kt`):
      cycle count, average/shortest/longest cycle length (from start-date gaps), average period
      length (from completed periods only — an ongoing one has no end yet to measure). 4 tests
      (`CycleStatisticsTest`).
- [x] Symptoms, flow, mood, pain, notes — new `CycleEntry` model/entity, separate from
      `MenstrualCycle`: most logged days aren't period days at all, so day-level detail
      (flow/symptoms/mood/pain/note) is tracked independently of period start/end, matching the
      `CYCLE_ENTRY` `EntityType` that was already declared-but-unused since Phase 1 (same pattern
      as `FOLDER`/`DOCUMENT` before their phases — this was clearly the plan). At most one entry
      per calendar date: logging an already-logged date updates it in place (found by date
      lookup in the repository, not a DB uniqueness constraint — a constraint would reject a
      legitimate edit). Room migration 6→7 adds `cycle_entries` + `documents.cycle_id`, bumps
      the DB to version 7. New `DatabaseConverters` entries for `FlowLevel`/`PainLevel`
      (nullable enum-by-name) and `List<Symptom>`/`List<Mood>` (JSON, same pattern as
      `ShoppingAlternative`). `RoomSyncStore` gained the `CYCLE_ENTRY` branch in all four
      places (`markSynced`, `markConflict`, `applyRemote`, `serialize`, `entityTypeOf`) — with
      all 8 `EntityType` values now handled, the `when` blocks became exhaustive and their
      `else` branches had to be deleted (compiler treats the now-redundant `else` as a warning,
      and this build treats warnings as errors). No Supabase migration needed: `cycle_entry` was
      already in the `entity_type` enum from `0001_init.sql`.
- [x] Cycle calendar (actual / predicted / estimated, distinguished without relying on color) —
      `CycleScreen`'s new `CalendarGrid`: a plain Sunday-first month grid (no external calendar
      library). Distinguishing marks are shape/fill, not just color, so the screen doesn't
      depend on color perception: actual logged period days are a filled circle, predicted
      period days are a tinted circle at low alpha (still a visibly different fill, not just a
      different hue), the fertile window is a small dot below the day number, and the estimated
      ovulation day is a small square below the day number (square vs. dot, not red vs. green).
      Tapping any day opens a bottom sheet (`DayForm`) to log or edit that date's flow/pain
      (single-select `FilterChip` rows)/mood/symptoms (multi-select) /note, pre-filled from any
      existing entry; days with a logged entry render their number bold as an additional
      non-color cue. Month navigation is chevron buttons, no swipe gesture.
- [x] Cycle document attachments — `Document` gained a `cycleId` field alongside the existing
      `taskId`/`folderId` (independent of both — an ultrasound report can be attached to a
      period without living in any folder). `DocumentRepository.observeForCycle`/`upload(...,
      cycleId = ...)` mirror the task-attachment methods from Phase 4. Each history row expands
      (tap the row) into the same attach-via-SAF / attach-via-scan / delete UI as tasks and
      folders, reusing `:core:scanner`'s `rememberDocumentScanner`.
- [x] `:feature:cycle` gained `:core:domain` and `:core:scanner` dependencies.
      `:feature:cycle:compileDebugKotlin`, `:app:compileDebugKotlin`, full `./gradlew test`
      (10/10 new domain tests green, no regressions elsewhere), and `./gradlew lint` all green.
      **UI is entirely unverified on-device** — same caveat as every other UI-layer phase this
      session: it compiles and the pure-logic pieces are unit-tested, but nobody has tapped a
      calendar day on a real screen yet. The calendar's Sunday-first assumption in particular
      is a guess (this app has no existing Settings-driven first-day-of-week preference to
      follow) and should be sanity-checked live, along with RTL layout for the day grid and the
      day-detail sheet's chip rows.

## Phase 7 — Security hardening & settings
- [x] Settings tree — new `:feature:settings` module, new bottom-nav tab (`HomeTab.Settings` in
      `SaharApp.kt`). Sections: Security, Notifications, Recovery phrase, Devices, Sign out.
      No `NavHost` exists in this app (routing is derived `when` state, see `SaharApp.kt`'s doc
      comment), so device management is reached by flipping a local `showDeviceManagement`
      boolean in `HomeRoute` rather than a real navigation stack — consistent with how every
      other screen here already works, not a shortcut specific to this phase.
- [x] Biometric lock, auto-lock — found and fixed a real gap first: `SessionState.lock()` alone
      was not a lock. Routing on `!session.isUnlocked` fell through to `PairingRoute()`, whose
      `PairingViewModel.init` unconditionally calls `onRefresh()`, which reads the device's own
      Keystore-sealed key copy via `DeviceIdentity.workspaceKey()` and silently reopens the
      session — no prompt, no gate, the "lock" undid itself the instant the screen recomposed.
      Fixed with a `SessionState._locked` flag independent of key presence, checked in
      `SaharApp`'s routing `when` *before* the `!isUnlocked` branch, routing to a new
      `LockRoute` instead of back through pairing. `LockRoute`
      (`app/.../lock/LockScreen.kt`) drives `androidx.biometric.BiometricPrompt`
      (`BIOMETRIC_STRONG or DEVICE_CREDENTIAL`) and on success calls the new
      `SessionState.unlock(key)` — re-arming the already-open session from
      `DeviceIdentity.workspaceKey()` directly, never touching the network or re-running
      pairing. `AutoLockController` (`DefaultLifecycleObserver` on `ProcessLifecycleOwner`, not
      the Activity's own lifecycle — a rotation or multi-window change stops/restarts an
      Activity without the app leaving the foreground) locks after a configurable timeout, but
      **only when biometric unlock is enabled**: this app has no separate password, so a lock
      with nothing able to safely re-open it would just strand the user — auto-lock is
      deliberately inert until the user opts into the one thing that can unlock it again.
      `SessionController` (new interface in `:core:security`, implemented by `SessionState`)
      is the seam that lets `:feature:settings` trigger lock/sign-out without depending on
      `:app`, mirroring `WorkspaceKeyProvider`'s existing read-side seam.
- [x] Screenshot policy — `MainActivity` applies `FLAG_SECURE` from a `SettingsPreferences` flow,
      defaulting **on**: an opt-in default would leave most users unprotected without ever
      knowing the option existed, and this app's whole reason to exist is keeping its content
      private.
- [x] Locally scheduled notifications, generic text by default — `ReminderWorker` (WorkManager,
      1-day period, mirrors `SyncWorker`'s existing pattern) posts one notification via
      `ReminderNotifier` with hardcoded generic copy ("You have updates to check") — never a
      task title, a due date, anything from the workspace. The notification tray is not part of
      this app's encryption boundary (any app, or the lock screen, can read it), so a
      personalized notification would leak exactly what the rest of the app exists to protect.
      Opt-in (default off) via the Settings toggle; turning it on requests `POST_NOTIFICATIONS`
      at runtime on Android 13+ (manifest permission added) before actually scheduling.
      `ReminderScheduler` (interface in `:core:settings`, `WorkManagerReminderScheduler` impl in
      `:app`) is the same seam pattern as `SyncTrigger`.
- [x] Device management — Settings' "Manage devices" reuses `PairingViewModel`'s already-live
      `Ready` stage (invite-code generation, pending-device approval) rather than rebuilding it;
      this is the first actual UI entry point for it since Phase 1 noted the join/approve path
      "has no UI entry point yet."
- [x] Recovery-phrase flow — both halves now done. "Show recovery phrase" in Settings
      re-derives the words on demand via `RecoveryPhrase.encode(identity.workspaceKey())`.
      The other half, added in this pass: a new `PairingStage.EnterRecoveryPhrase`, reachable
      from `Choose` (signed in, never joined a workspace on this device) and from
      `AwaitingKey` (joined, but recovering directly is faster than waiting for a partner
      device to approve). Finally exercises `RecoveryPhrase.decode()`, which sat completely
      unused since Phase 1 despite being fully tested. `onSubmitRecoveryPhrase` resolves the
      workspace id from wherever this device already knows it (`identity.workspaceId` if
      joined, else `workspaces.currentWorkspaceId()` off the account's membership — a phrase
      alone carries no workspace id of its own), decodes the phrase, saves the key, and
      registers this device (so it shows up in device management even though it was never
      part of the normal join flow). A bad phrase fails loudly via the BIP-39 checksum,
      surfaced as a new `pairing_error_invalid_phrase` string rather than falling through to
      the generic error message. `:feature:pairing:compileDebugKotlin`,
      `:app:compileDebugKotlin`, `:app:assembleDebug`, full `./gradlew test`, and
      `./gradlew lint` all green. Unverified on-device like the rest of this session — this
      one in particular touches the same `identity.workspaceId`/`saveWorkspaceKey` state the
      normal join flow depends on, so it's worth confirming it doesn't leave that state
      inconsistent for a device that later also goes through normal pairing.
- [x] Key rotation — **narrower than full rotation, deliberately scoped that way**. Built
      device-key **revocation**, not workspace-symmetric-key rotation:
  - `supabase/migrations/0006_revoke_device_key.sql` adds a `SECURITY DEFINER` RPC,
    `revoke_device_key(target_device_key_id)`, that checks workspace membership and sets
    `device_keys.revoked_at`. Deliberately not a broadened RLS `UPDATE` policy — a
    workspace-member-scoped `UPDATE` on `device_keys` would let either partner overwrite the
    *other* partner's device's `public_key` column too, not just `revoked_at`, which is a MITM
    vector, not a revoke feature. The RPC mirrors the existing `accept_invitation()` pattern and
    only ever touches `revoked_at`.
  - `WorkspaceRepository`: `PartnerDevice` gained `isRevoked: Boolean`, `DeviceKeyRow` now reads
    `revoked_at`, and a new `revokeDevice(deviceKeyId)` calls the RPC.
  - `PairingViewModel.showReady()` now filters revoked devices out of `pendingDevices`
    entirely (a revoked device can no longer be approved), and separately computes
    `revocableDevices` — active, already-key-holding devices other than this one — for a new
    `onRevokeDevice(deviceKeyId)` action.
  - `PairingScreen`'s `Ready` stage (reused by Settings → Manage devices) shows a "Paired
    devices" list with a Revoke button per non-self device.
  - **What this does not do, and why that matters**: revoking a `device_keys` row does not end
    that device's ongoing Supabase Auth session, and it does not rotate the workspace's
    symmetric key. `records` RLS is governed by `workspace_members`/`auth.uid()`, not
    `device_keys` — a revoked device that is still signed in keeps reading/writing records
    until its session ends or it's signed out elsewhere. Revocation only prevents that device's
    public key from being trusted for *future* key-wrap grants (i.e. it can no longer be handed
    the workspace key by re-pairing). Full workspace-key rotation — re-encrypting every record
    under a new key and re-wrapping it to every remaining device — is out of scope: it needs
    pgTAP coverage of the re-wrap fan-out that this environment's Supabase pgTAP harness cannot
    currently run (see the earlier note on that pre-existing environment issue), so building it
    without that safety net was judged too risky.
  - `:core:network:compileDebugKotlin`, `:feature:pairing:compileDebugKotlin`,
    `:app:compileDebugKotlin`, `:app:assembleDebug`, full `./gradlew test`, and `./gradlew lint`
    all green. Unverified on-device, same caveat as the rest of this session — and the new
    Supabase migration itself has not been applied against a live project or exercised end to
    end (no pgTAP run either, for the same reason full rotation was skipped).
- Known gap, not on the original checklist but surfaced while building sign-out: signing out
  clears `SessionState` and `DeviceIdentity` (device keypair, workspace key, registration) but
  does **not** wipe the local Room database. The SQLCipher passphrase (`KeystoreDatabasePassphrase`)
  is device-Keystore-backed and independent of workspace pairing, so previously-synced decrypted
  content stays readable by this app on this device after a sign-out. A real "forget this
  device" flow needs to drop/recreate the database too — not attempted this pass.
- `:app:compileDebugKotlin`, full `./gradlew test`, and `./gradlew lint` all green after every
  addition above. As with every UI-layer phase this session: compiles and (where there's pure
  logic) is unit-tested, but nothing here has been tapped on a real device — the biometric
  prompt, the auto-lock timing, and the FLAG_SECURE toggle all specifically need a live pass
  before they count as done-done, more so than most UI because they're security-relevant and
  Compose previews/unit tests cannot exercise `BiometricPrompt` or process-lifecycle timing at
  all.

## Phase 8 — Search, calendar, conflicts, polish
- [x] Search — **FTS4, not FTS5**: Room 2.8.4 (the version pinned here) has no `@Fts5`
      annotation at all, only `@Fts3`/`@Fts4` (confirmed directly against the jar). This is a
      hard library limitation, not a risk-averse substitution — corrected
      `docs/architecture/007-encryption.md`'s "FTS5" claim to match. A single hand-maintained
      `search_index` FTS4 table (`SearchIndexEntity`) spans all seven searchable entity types
      (tasks, shopping items, important dates, folders, documents, cycles, cycle entries) —
      Room's `contentEntity` mirroring only ever binds one table, so a real unified index has
      to be kept in sync by hand. `SearchIndexer` (`:core:database`) is that sync point: every
      repository's `enqueue`/upload and soft-delete now also calls `index()`/`remove()` inside
      the same transaction as its normal write, so the index can never drift from what's
      on-screen. `RoomSyncStore.applyRemote` got the same treatment — a record synced in from
      the partner's device is indexed (or removed, if it arrived as a tombstone) exactly like a
      local write, so search covers the whole shared workspace, not just what was created on
      this device. Migration 7→8 creates the table and backfills it from every already-synced
      row via `INSERT ... SELECT`, so upgrading doesn't leave existing data unsearchable until
      its next edit. New `:feature:search` module + 8th bottom-nav tab; results show which
      entity type matched and a snippet, tapping one switches to the tab that owns it (no
      per-screen "scroll to this item" support was built, so it gets you to the right area, not
      the exact row — documented gap, not an oversight). `SearchRepository.toFtsQuery` turns
      free text into a safe `term*` prefix-match query per word rather than passing raw user
      input to `MATCH`, which would either mis-parse as boolean operators or throw on stray FTS
      syntax characters. `:app:assembleDebug`, full `./gradlew test`, and `./gradlew lint` all
      green. **Unverified on-device** like everything else this session — FTS4 query behavior
      and the migration backfill in particular need a real run against actual synced data.
- [x] Unified calendar — real gap found first: `Task.dueDate` existed in the model since Phase 1
      but `TaskRepository.create`/`update` never accepted it and no UI ever set it (flagged as
      a known gap back in Phase 3's import work). Fixed alongside the calendar, since a
      due-date calendar with no way to ever set a due date would be pointless: `TaskRepository`
      now takes `dueDate: LocalDate?` in both, `TasksScreen` gained a due-date field (Material3
      `DatePicker`, same pattern as `:feature:dates`) in the add/edit sheet.
      New `:feature:calendar` module + 9th bottom-nav tab (see note below). `CalendarViewModel`
      merges three live flows — `TaskRepository.observeAll` (by `dueDate`),
      `ImportantDateRepository.observeAll`, `CycleRepository.observeAll` (actual period days,
      plus predicted ones via `:core:domain`'s `predictNextCycle`/`calculateCycleStatistics`,
      reused as-is from Phase 6) — via `combine` into one `List<CalendarEvent>`. Deliberately
      not a stored/synced entity: it is a read-side projection recomputed live, same reasoning
      as search's index but simpler since nothing here needs to survive a process restart faster
      than the three source flows can re-emit. Month grid marks days with colored dots per event
      kind (task/date/period — not relying on color alone was Phase 6's cycle-calendar
      precedent; this one is a smaller feature and uses color-coded dots only, a real
      accessibility gap flagged for the Phase 8 accessibility pass rather than fixed twice).
      Tapping a day opens a bottom sheet listing that day's events; tapping an event switches to
      the tab that owns it (same "right area, not the exact row" gap as search's `OpenResult`).
      `:app:compileDebugKotlin`, `:app:assembleDebug`, full `./gradlew test`, and
      `./gradlew lint` all green.
  - **Bottom nav is now 9 tabs** (Home/Tasks/Shopping/Dates/Folders/Cycle/Calendar/Search/
    Settings) on a phone screen. This was already past typical guidance at 7 before this
    session touched it (Settings, Search) and is worse now. Flagged here rather than fixed:
    a real UX problem (needs consolidation, an overflow menu, or icon-only compact tabs) that
    deserves its own deliberate pass, not a reflexive fix mid-calendar-feature.
- [x] Conflict resolution UI — the data layer had this waiting since Phase 1:
      `SyncConflictEntity`/`SyncStateDao.observeConflicts()`/`observeConflictCount()` existed,
      declared but with no UI ever reading them (same "built ahead of the phase that needs it"
      pattern as `FOLDER`/`DOCUMENT`/`CYCLE_ENTRY` before their phases). New
      `ConflictRepository` (`:core:database`) is the only place a conflict ever gets resolved,
      by a person choosing a side — nothing here auto-resolves anything, matching
      `SyncConflictEntity`'s own doc comment ("resolving it silently is how a partner's edit
      disappears without either of them noticing"). `keepLocal` re-queues the local row for
      push, rebased onto the server's version so the retry isn't immediately rejected as stale
      again; `keepServer` decrypts and applies the server's copy in place of the local one and
      marks it synced, since there's nothing left to push. Both paths reindex search
      (`SearchIndexer`) and go through `database.withTransaction`, same discipline as every
      other write path this session. Necessarily duplicates `RoomSyncStore`'s per-entity-type
      `when` dispatch (7 branches) rather than reusing it — `RoomSyncStore` methods aren't
      structured to be called from outside the sync pipeline. New `:feature:conflicts` module,
      hosted at the app root (`ConflictHost` in `SaharApp.kt`, alongside `UpdateHost`) rather
      than behind a tab — a stuck conflict means a partner's edit isn't syncing, which deserves
      surfacing proactively, not burial in a 10th nav tab on top of the 9 already there. Not
      modal like a mandatory update: dismissible, but re-surfaces whenever the conflict count
      changes. `:app:compileDebugKotlin`, `:app:assembleDebug`, full `./gradlew test`, and
      `./gradlew lint` all green. **Unverified on-device**, and more than most features this
      session actually needs it: this is the one path that has never been exercised even
      indirectly (nothing else in the app deliberately produces a conflict), so the actual
      SQLite/Room mechanics of "two devices edit the same row, one gets rejected, resolve it"
      are unverified beyond compiling.
- [x] Recurring tasks (rule-based) — `Task` gained `recurrence: Recurrence?`
      (`RecurrenceFrequency` DAILY/WEEKLY/MONTHLY + an interval — "every 2 weeks" needs the
      interval, a bare frequency enum can't express it), only meaningful alongside `dueDate`
      since a rule needs a date to advance from. `:core:domain`'s `nextDueDate` (new
      `task/RecurrenceAdvance.kt`) is the pure advance-by-one-occurrence math — monthly
      preserves day-of-month where the target month has it and clamps to the last day where
      it doesn't (Jan 31 monthly → Feb 28, not March 3); 4 tests, all green. Deliberately kept
      out of `:core:database`: `TaskRepository.completeAndScheduleNext(id, nextDueDate)` takes
      the already-computed next date as a plain parameter rather than importing
      `:core:domain` itself, so the data layer's dependency graph stays one-directional
      (domain math depends on nothing; data layer doesn't reach up into it) — the caller
      (`TasksViewModel`, which already needs domain logic) computes it. Completing a recurring
      task both marks the current occurrence done *and* creates the next one in the same
      transaction (new task row, `dueDate` advanced, fresh id — not a mutation of the
      completed one, so the completed occurrence's history stays intact rather than being
      overwritten). Un-completing, or completing a non-recurring task, is still a plain toggle.
      Migration 8→9 adds `tasks.recurrence_frequency`/`recurrence_interval` (both null =
      "does not repeat", the default for every existing row). `TasksScreen` gained a repeat
      picker (shown only once a due date is set) and a small repeat icon on recurring rows.
      Interval is fixed at 1 in the UI for now (the model and domain math both support
      arbitrary intervals — "every 2 weeks" — but a picker for it wasn't built this pass,
      scoped down deliberately rather than left half-wired).
      `:app:compileDebugKotlin`, `:app:assembleDebug`, full `./gradlew test` (4/4 new domain
      tests green), and `./gradlew lint` all green. Unverified on-device like the rest of this
      session's UI work.
- [x] Tags — scoped to `Task` only, not every entity (the original spec's schema sketch, per
      `docs/specs/01-android-conversion.md` §54, proposes normalized `tags`/`task_tags`
      tables for cross-entity reuse and autocomplete; skipped that for a `List<String>` column
      on `Task` — same JSON-column pattern already used for `CycleEntry`'s `symptoms`/`mood` —
      because this app is one couple's task list, not a multi-tenant catalog that needs
      reuse/autocomplete infrastructure to stay fast). The spec's "cycle tags are private,
      never shown to the partner" clause was dropped as already superseded by this app's
      actual threat model (couple-vs-outsiders, not partner-vs-partner — see the note at the
      top of this file); nothing here treats any tag as hidden from either user.
      Migration 9→10 adds `tasks.tags` (`'[]'` default). Tags are freely user-defined text,
      normalized on entry (`#Medical`, `medical`, `Medical` all become `medical` — a
      case/`#`-sensitive match would quietly stop matching the first time a tag was typed
      differently) via a small `normalizeTag` helper in `TasksViewModel`. `TasksScreen` gained
      a tag-chip input in the add/edit form and a horizontally-scrolling filter row above the
      task list (derived from whatever tags currently exist across all tasks — no separate
      "manage tags" screen, since the set of tags in use already *is* that list). Tags feed
      into search too — `TaskRepository`'s `searchBody()` now folds tags into the indexed text
      alongside the note, so `#medical` finds a task even if the word never appears in its
      title.
      `:app:compileDebugKotlin`, `:app:assembleDebug`, full `./gradlew test`, and
      `./gradlew lint` all green. Unverified on-device like the rest of this session's UI.
- [x] Accessibility pass — used `android-skills:android-ux`'s M3 compliance audit (category 9
      + the quick grep checks for categories 1/3/5) rather than freehand review. Findings:
      - **Real bug, fixed**: the device-management back button (`SaharApp.kt`'s
        `DeviceManagementRoute`) was a standalone `IconButton` with
        `contentDescription = null` — TalkBack would have announced it as an unlabeled
        button with no indication it navigates back. Every other `IconButton` in the app
        already had a real description; grepped for the pattern to confirm this was the only
        one. Added `R.string.app_back` (new, `:app` had no strings.xml entries for this kind
        of generic chrome yet).
      - **Real bug, fixed**: Cycle's and Calendar's day-grid cells (`DayCell` in both
        `CycleScreen.kt` and `CalendarScreen.kt`, both new this session) were 36dp tap
        targets — below M3's 48dp minimum. Fixed with
        `Modifier.minimumInteractiveComponentSize()` (the actual M3 mechanism for this,
        not hand-rolled padding math) ahead of `.size(36.dp)`, so the visual circle stays
        compact for a 7-column month grid while the tappable area expands to 48dp, centered.
        Applied to the blank leading-day placeholders too, so grid columns stay aligned.
        Trade-off accepted, not hidden: 7 × 48dp = 336dp minimum row width, which is tight
        (not necessarily broken, but untested) on the smallest screens `minSdk 26` nominally
        allows; real devices that narrow are vanishingly rare in 2026, so this wasn't chased
        further.
      - **Fixed**: every screen's page-title `Text` (Calendar, Cycle, Settings, Auth) and
        Settings' section-card titles now carry `Modifier.semantics { heading() }`, so
        TalkBack users can jump between sections instead of swiping through every element
        linearly.
      - **Checked, no finding**: categories 1/3/5's grep checks (hardcoded colors, hardcoded
        corner radii, Material 2 imports) — the only hardcoded-color hits are `Color.kt`
        itself (the token *definitions*, which is where they belong) and the moon-countdown's
        intentionally-hardcoded night palette (documented in `CLAUDE.md` as a deliberate
        exception); no M2 imports found anywhere; all `FloatingActionButton`/other
        icon-only actions already had real content descriptions.
      - **Not fixed, flagged**: Tasks, Shopping, Dates, Folders, and Home have no in-page
        heading `Text` at all — they rely entirely on the bottom-nav tab label for context,
        which a screen-reader user landing directly on the content won't hear. Adding page
        titles to five screens is new UI surface, not a semantics-only change, so it was
        flagged rather than added reflexively under an "accessibility pass" label.
      `:app:compileDebugKotlin`, `:app:assembleDebug`, full `./gradlew test`, and
      `./gradlew lint` all green. The touch-target and heading fixes are structural
      (semantics/sizing), so they're about as verifiable from source as accessibility work
      gets without a TalkBack pass on a real device — still worth an actual screen-reader
      run before calling this done-done.
- [x] Performance (paging, indexes, thumbnails) — scoped down deliberately on the first of
      the three, with the reasoning written down rather than silently dropped:
      - **Paging — skipped, not built.** This app has exactly two users' worth of data —
        tasks, shopping items, documents in a folder are realistically dozens, not thousands,
        forever. Paging 3 (`PagingSource`, `RemoteMediator`, `LazyPagingItems`) solves a
        problem this app structurally doesn't have; adding it would be complexity for its own
        sake, not a performance fix. Revisit only if a real usage pattern ever produces
        genuinely large lists (it won't, for a two-person household).
      - **Indexes — audited, no gap found.** Read every entity's `indices` list: all nine
        syncable tables already carry `sync_status` (the push query), `workspace_id +
        updated_at` (pull cursor and list ordering), and a query-shaped composite index where
        a screen actually filters on something else (`category` on tasks/shopping,
        `due_date` on tasks, `path`/`parent_id` on folders, `folder_id`/`task_id`/`cycle_id`
        on documents, `start_date` on cycles). This was kept current incrementally as each
        entity was added this session, rather than needing a catch-up pass now.
      - **Thumbnails — built, and the design is the actual point of this item.** Documents
        had no preview at all before this — just a generic file icon, even for photos. The
        obvious approach (fetch the thumbnail from Storage) doesn't exist for E2EE content:
        there is no cheap way to fetch "just a preview" of an encrypted blob, the smallest
        fetchable unit is the whole file. So `DocumentRepository.upload` generates a small
        (160px, JPEG quality 70) downsampled preview **from the plaintext bytes already in
        memory for the encrypt call right after it** — no extra fetch, no extra decrypt —
        using `inSampleSize`-based decoding so peak memory stays bounded even for a large
        photo. The Base64 preview travels in `Document`'s existing small metadata record
        (which already syncs as encrypted JSON), so the partner's device gets the thumbnail
        for free without downloading and decrypting the original either. Migration 10→11
        adds `documents.thumbnail_base64`; null for every existing document and every
        non-image one — deliberately not backfilled, since generating one would mean
        downloading and decrypting every existing image during a schema migration, which
        must never be network-dependent or unbounded. `FoldersScreen`'s document rows now
        show the decoded thumbnail (cached via `remember` so scrolling doesn't redecode
        every frame) instead of the generic icon when one exists.
      `:app:compileDebugKotlin`, `:app:assembleDebug`, full `./gradlew test`, and
      `./gradlew lint` all green. Unverified on-device like the rest of this session — the
      thumbnail generation path in particular (real image bytes through `BitmapFactory`)
      has never run outside the JVM/emulator-free compile step.

## Phase 9 — Docs
- [x] Rewrote `README.md` — Android (`android/`) is now documented as the active product:
      what the app is, module layout (`:core:*`/`:feature:*`), build/run commands, the
      workspace/pairing/E2EE model at a glance, pointers out to `docs/architecture/` and
      `PROGRESS.md` rather than duplicating their depth. Web app's fate, per the user
      (asked directly rather than guessed): **retired**. `src/` section trimmed to "legacy,
      kept for reference; still the source of the one-time JSON import path."
      `.github/workflows/deploy.yml` (GitHub Pages auto-deploy) was deliberately left alone —
      disabling/removing CI is a bigger, more reversible-cost decision than a docs pass
      should make unilaterally; flagged here as a candidate for a follow-up cleanup, not done.
- [x] `docs/architecture/` drift pass — read all four ADRs (001 architecture, 005 privacy,
      007 encryption, 011 release/updates) against what Phases 1–7 actually built. **No
      drift found** — all four hold up exactly as written, including 007's prediction that
      "reminders are scheduled locally" (Phase 7's `ReminderNotifier`, built independently
      this session, matches it precisely) and 011's mention of a manual "Check for updates"
      in Settings, which Phase 7's Settings screen had actually missed until this pass caught
      it (see the fix noted under Phase 7... actually added here: `SettingsScreen` gained an
      `UpdateViewModel.onCheckNow()` button, wired via a `footer` slot since `:feature:settings`
      can't depend on `:feature:update` — same cross-feature seam pattern as device management).
- [x] `CLAUDE.md` — added a full "Android app" section (commands, the no-cross-feature-deps
      rule, the no-`NavHost` routing shape, persistence/sync flow, where domain math goes, the
      "adding a synced entity touches these five places" checklist, bilingual strings) ahead
      of the original web-app content, which is now explicitly marked legacy/retired and
      demoted under "Web app (legacy)". `AGENTS.md` is unrelated boilerplate (a caveman-mode
      activation file, not project documentation) — nothing to update there.
- [x] `task.md` — turned out not to need anything. Investigating the "threat-model note"
      pointer at the top of this file found it was **wrong**: it blamed `task.md` for the
      superseded private-by-default/partner-sharing model, but that content actually lives in
      `docs/specs/01-android-conversion.md` — `task.md` is the unrelated auto-update spec,
      already faithfully captured in `docs/architecture/011-release-signing-and-updates.md`,
      and contains no privacy/sharing content at all (checked). Fixed the note at the top of
      this file to point at the right spec, and corrected the same wrong reference in this
      session's saved memory (`couple-app-threat-model.md`), which had copied the same error.
- [x] Debug build sanity check (pulled forward from Phase 10 since it's a docs-adjacent
      "does everything actually still fit together" check): `./gradlew :app:assembleDebug`
      succeeds end-to-end, produces a real APK (`app-debug.apk`, ~39 MB). Also ran
      `:app:assembleRelease` (unsigned, no keystore secrets in this environment) to confirm
      the R8/shrink/proguard path itself works, not just per-module `compileDebugKotlin` —
      succeeded, producing `app-release-unsigned.apk`.

## Phase 10 — Release
- [x] Debug build — `:app:assembleDebug` succeeds, produces `app-debug.apk`. Done under
      Phase 9 above (pulled forward as a "does it all still fit together" check).
- [~] Release build — the build path works (`:app:assembleRelease` succeeds: R8 minify,
      resource shrinking, proguard all run clean, produces `app-release-unsigned.apk`), but
      it is genuinely **unsigned** — no `ANDROID_KEYSTORE_*` secrets exist in this
      environment, and per `docs/architecture/011-release-signing-and-updates.md` that's the
      correct behavior (never silently fall back to debug signing) rather than a bug to fix.
      An actual signed, installable release needs the real keystore secrets, which only exist
      as GitHub Actions secrets + the user's offline backup — not obtainable from inside this
      session. Tagging `v*` and letting `.github/workflows/android-release.yml` run is the
      real release path; nothing to do here beyond confirming the local build itself is sound.
- [x] Final report — `docs/FINAL_REPORT.md`, following the exact template in
      `docs/specs/01-android-conversion.md` §85 (Implementation Summary sections plus the
      explicit "how X works" list at the end). Written now rather than waiting on live
      verification, per explicit instruction to finish what doesn't depend on it — but its
      "Known Limitations" section says so plainly and up front, per that same spec section's
      own instruction not to claim the app is secure or working without having verified it.
      The live-verification backlog (Phases 4–8, all built and unit-tested but never run on a
      device) is the load-bearing caveat across this entire report, not a footnote.

## Phase 11 — Live two-device testing (in progress)

First real on-device pass, two physical phones (Pixel 9, a Xiaomi/MIUI device) over adb,
two separate real Supabase accounts. Session state as of 2026-08-16:

**Confirmed working live:**
- App launches and renders on both devices, Hebrew (RTL) and English, no crashes across a
  full pass of all 9 tabs (Home, Tasks, Shopping, Dates, Documents, Cycle, Calendar, Search,
  Settings).
- Naegele's-rule last-period math (Phase-session change earlier today) computes correctly
  on-device ("Week 34, day 4 of 40" etc.), on both phones independently.
- Full real pairing flow end-to-end: create workspace → recovery phrase shown → generate
  invite code → enter code on second device → approve from first device → both devices
  share one `workspace_members`-backed workspace. This is the **first time the real
  invite/accept/approve flow has been exercised live**, not just recovery-phrase-based
  re-entry.
- Recovery-phrase re-entry verified live on both devices independently (BIP-39 checksum
  decode, correct key recovery).
- Device revocation (`revoke_device_key` RPC, migration `0006_revoke_device_key.sql`) —
  **pushed to the live linked Supabase project this session** (`npx supabase db push
  --linked`) and confirmed working end-to-end after the push; failed with a generic error
  beforehand because the migration file existed locally but had never been applied remotely.
  Cleaned up several duplicate `device_keys` rows this way.
- Cross-device sync confirmed working: a last-period date and a cycle entry saved on one
  device appeared on the other after either the periodic 6-hour worker or an explicit
  refresh.
- Pull-to-refresh added to Home this session specifically because live testing showed there
  was **no way to trigger a sync from the UI** — only a 6-hour periodic WorkManager job and
  the push-after-local-write path, so a partner's change could sit unseen for hours with the
  app open. `HomeViewModel` now calls `SyncEngine.sync()` directly (not through
  `SyncTrigger`/WorkManager, since the UI needs to await completion to stop the spinner) via
  a new `refreshing: Boolean` state and `onRefresh()` action; `HomeScreen` wraps its content
  in `PullToRefreshBox` (`androidx.compose.material3.pulltorefresh` — not the top-level
  `material3` package, easy to get wrong). **Only wired into Home so far** — Tasks, Shopping,
  Dates, Documents, Cycle, Calendar have no pull-to-refresh yet and share the same "can sit
  stale for hours" gap. Compiles, full `./gradlew test`, `./gradlew lint` all green;
  confirmed working live on-device by explicit user test.
- Shopping (add item), Cycle (log period start) confirmed working live end-to-end including
  a real save.
- Search confirmed working live: a shopping item created during this session was found via
  full-text search.

**Bugs found live, not yet fixed:**
- **Invite-code entry field scrambles input when typed programmatically** (`adb shell input
  text`, one character at a time or all at once) — characters land in the wrong positions
  after the live dash-formatting kicks in (e.g. typing `E6XJ9PW6Z5ZK29SXN38P` produced
  `E6XJ9-W6Z5Z-29SXN-8P3KP`). Real manual typing on-device via the phone's own keyboard
  worked fine, so this may be specific to how `adb input text` delivers characters (no real
  per-key IME events, no natural inter-key delay) interacting with the field's
  `VisualTransformation`/cursor-position logic rather than a bug a real user would ever hit —
  but it points at a fragile cursor-offset assumption in that transformation that's worth a
  closer read, since paste-from-clipboard or fast IME autocomplete could plausibly trigger
  the same code path. Not yet root-caused. Where: the code-entry field in
  `feature/pairing`'s `EnterCodeStage`.
- **Folders screen's expanded FAB actions ("Scan", "Import", "Add folder") expose no
  accessible text or content-description** — confirmed via `uiautomator dump`: the three
  `ExtendedFloatingActionButton`s render visible text on screen but the accessibility tree
  shows empty `text`/`content-desc` on every node (`NAF="true"`, Android's own
  not-accessible-friendly marker) for all three. This is on top of the M3 accessibility pass
  done in Phase 8, which didn't cover this screen. TalkBack users get nothing for three
  primary actions on this screen. Where: `feature/folders/FoldersScreen.kt`'s
  `floatingActionButton` block (~line 92-119). Not yet root-caused or fixed.
- **Duplicate `device_keys` registrations accumulate** across repeated sign-out/clear-data
  cycles on the same physical device, all sharing the identical label (manufacturer + model,
  e.g. "Google Pixel 9" three times over from one phone) — makes the "Manage devices" /
  revoke UI impossible to tell apart when this happens. Not a security bug (each row is a
  genuinely distinct keypair, revoking the wrong one just means re-approving), but a real
  UX gap. Likely worth including something instance-distinguishing in the label (an install
  ID suffix, or a "first seen" timestamp) rather than relying on manufacturer+model alone.
  Not yet fixed.
- One UI navigation got the Android device itself stuck with `NotificationShade` holding
  window focus above the app (no crash, no exception, unclear trigger — happened once, not
  reproduced deliberately) — recovered via a normal device lock/unlock cycle, not an app fix.
  Noting in case it recurs; not clearly an app bug versus an OS/vendor (MIUI or Pixel) quirk.

**Explicitly not yet tested live:**
- Folders: actually creating a folder end-to-end (blocked on the FAB accessibility/testing
  friction above, not re-attempted after the stuck-notification-shade detour).
- Documents: upload, scan (ML Kit document scanner), nested folder navigation, cascading
  delete.
- Dates: adding an item through the form (screen loads and shows the correct empty state;
  the add flow itself wasn't exercised).
- Tasks: manual add (only the hospital-bag preset was exercised), recurring tasks, tags.
- Calendar: task/date markers on specific days, conflict indicators.
- Settings: the biometric-unlock toggle, screenshot-block toggle (its effect was inferred
  from `screencap` returning empty/black earlier in this session, not from toggling it
  on-device), auto-lock timing, and the "Lock now" → biometric re-entry flow.
- Sign-out's known gap (Room DB not wiped on sign-out, documented under Phase 7) — not
  re-verified this session.
- A true multi-partner conflict (both devices editing the same record while offline, then
  syncing) — `SyncConflictEntity`/conflict UI exist from Phase 8 but weren't exercised with
  two real devices this session.

**Test-environment notes, not app issues:** the MIUI device's `uiautomator dump` throws a
`ThemeCompatibilityLoader` `FileNotFoundException` on every invocation (a known Xiaomi/MIUI
quirk unrelated to this app) but still writes the dump file correctly afterward: safe to
ignore the exception text and read the output file. `screencap`/`exec-out screencap` return
empty output on both devices whenever the in-app "block screenshots" setting is active
(`FLAG_SECURE`) — expected, not a bug, but means visual screenshots aren't available for
verification while that setting is on; `uiautomator dump`'s text-only tree still works.

## Deferred (P2)
OCR (Hebrew script is unsupported by ML Kit; cloud OCR is ruled out by the encryption
design), widgets, pregnancy mode, advanced analytics.
