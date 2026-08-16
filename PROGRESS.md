# Progress

Android conversion of אור ירח into private couple organizer.
Branch: `feature/android-app`. Plan: see `docs/architecture/`.

**Threat model note:** privacy boundary = couple vs outside world, not
partner vs partner. Both users share all data with each other; end-to-end
encrypted so Supabase, attacker, or lost phone learns nothing. This
supersedes private-by-default / opt-in-sharing model described in
`docs/specs/01-android-conversion.md` (corrected from earlier wrong reference to
`task.md` here — `task.md` actually unrelated auto-update spec; see
`docs/architecture/005-data-privacy.md` for full decision record). See also
Phase 9 below.

## Phase 0 — Analysis
- [x] Analyze frontend (React 19 + Vite PWA, Hebrew/RTL, 5 pages)
- [x] Analyze backend — **none**; no API, no auth, no network code at all
- [x] Analyze database — none; zustand + `localStorage`, manual JSON export/import
- [x] Analyze authentication — none
- [x] Inventory domain logic to port (pregnancy math, budget, hospital-bag preset)
- [x] Inventory design tokens to port (`src/index.css` light/dark palette, fonts)
- [x] Architecture plan agreed with user

## Phase 1 — Walking skeleton
- [x] Gradle 9.7 + AGP 9.3.1 project, version catalog, all versions stable
- [x] Convention plugins (application, library, compose, feature, room, jvm)
- [x] Bilingual resources (en default, he) + `localeConfig`, RTL enabled
- [x] `:core:model`, `:core:common` (AppResult / AppError, injectable dispatchers)
- [x] `:core:crypto` — record encryption; 32 crypto tests green
- [x] `:core:crypto` — key wrapping (HPKE) for pairing
- [x] `:core:crypto` — recovery phrase (BIP-39, official vectors)
- [x] Theme ported from `src/index.css` (light + dark palette, radius scale, both
      bundled variable fonts) — `:core:ui`, confirmed matching web app live on-device
- [x] Supabase schema + RLS, first migration (25 access-control tests written; hosted
      project's pgTAP harness currently fails to resolve `plan()` despite extension
      present — role/search_path environment issue, not regression from anything this
      session; needs own investigation)
- [x] Auth (email/password, Keystore-backed session)
- [x] Couple pairing — create-workspace path verified end-to-end on emulator against
      live project. Join-with-code/approve-device path implemented but no UI entry
      point yet (app routes past `PairingScreen`'s `Ready` stage instant key unlocks);
      needs Settings/device-management screen, planned Phase 7.
- [x] Room + SQLCipher (tasks, cycles, sync operations)
- [x] Sync engine core: push/pull, cursor, conflict detection (11 tests)
- [x] Sync engine: Supabase wiring + WorkManager worker, wired to app startup and
      every local write via `SyncTrigger`
- [x] Tasks: list / add / edit / toggle-done / delete, `:feature:tasks`, offline-first
      through `TaskRepository`'s outbox
- [x] Cycle: log period start and end, history, delete — `:feature:cycle`, offline-first
      through `CycleRepository`'s outbox; only user-entered dates stored, length
      computed at read time
- [x] Security acceptance tests, per gate in plan:
      - Ciphertext-only — verified manually on live project for both task write and
        cycle write (title/dates don't appear anywhere in stored row's ciphertext)
      - Outsider access → 0 rows, unauthenticated → denied, invitation expired/revoked
        rejected, third member blocked by workspace cap — all in
        `supabase/tests/001_access_control.sql` (blocked from re-running right now by
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
- [x] Async startup check, non-blocking, offline-tolerant — `UpdateViewModel.init` fires
      check on coroutine; failed/offline check just clears `checking`, no crash, no dialog
      (verified live: repo has no GitHub release yet, checker gets 404, app launches clean)
- [x] Update notification with release notes, Install / View Release / Later — `UpdateDialog`
      in `:feature:update`, hosted at app root (`SaharApp`'s `UpdateHost`) so it can
      interrupt any screen
- [x] Download with progress + SHA-256 verification before install — `UpdateDownloader`
      (Ktor `onDownload` progress callback) + `IntegrityVerifier`; checksum mismatch deletes
      file and surfaces as failure, never installs
- [x] `PackageInstaller` install flow — `UpdateInstaller`, session-based, dynamically
      registered receiver for commit result, handles `STATUS_PENDING_USER_ACTION` by
      handing confirmation `Intent` back through effect
- [x] Mandatory-update support — manifest's `mandatory` flag, or installed version below
      `minSupportedVersion`, both force `UpdateAvailability.Mandatory`; dialog then has no
      dismiss/later/skip
- [x] Persisted update state (last check, last notified, skipped) — `UpdateState`, DataStore
      Preferences, deliberately outside encrypted workspace so it's readable pre-unlock
- [x] Manual "Check for updates" in Settings — done, but later than tracked here: Settings
      screen this depended on didn't land until Phase 7, button itself actually added
      during Phase 9's ADR drift pass (`docs/architecture/011` names this requirement
      explicitly; pass caught Phase 7's Settings screen shipped without it). `SettingsScreen`
      gained `footer` slot specifically because `:feature:settings` can't depend on
      `:feature:update` (feature modules must not depend on each other) — `SaharApp.kt`'s
      `SettingsRoute` supplies button via that slot, wired to already-existing
      `UpdateViewModel.onCheckNow()`.
- [x] Updater tests — `VersionComparatorTest` (5 cases incl. `1.9.0 < 1.10.0` and prerelease
      precedence); download/install/UI verified live on-device rather than instrumented
      tests, since both need real network/PackageInstaller behavior

## Phase 3 — Port the existing app
- [x] Task model — already full from Phase 1 (title/category/priority/dueDate/assignee/note/done)
- [x] Shopping list + budget — `:feature:shopping`, `ShoppingItemRepository`, budget summary
      (estimated/spent/bought) live on list using `:core:domain`'s `calculateBudget`;
      verified end-to-end on-device (create → mark bought → totals update correctly → syncs,
      ciphertext-only confirmed). Alternatives (per-item price comparison) not yet ported —
      model/DB column exists (JSON list) but no UI to add one yet
- [x] Important dates — `:feature:dates`, `ImportantDateRepository`, Material3 `DatePicker`;
      verified end-to-end on-device including sync
- [x] Home dashboard — `:feature:home`, Home tab (now first in bottom nav). Canvas-drawn
      moon countdown matching web SVG (night-sky palette from `NightPalette`, radial glow
      rising with `moonFraction`), week/day progress, weekly fruit+animal, weekly-info card,
      daily message, budget summary, open-tasks count, inline due-date/babyName editor (no
      dueDate/babyName means no Settings screen yet, so this doubles as minimal settings
      surface until Phase 7). Backed by new synced `AppSettings`/`app_settings` entity
      (`EntityType.SETTINGS`, previously declared but unused) — migration 2→3. Weekly
      info/fruit/animal copy and 6 daily messages are bilingual string-array resources,
      ported verbatim from `pregnancy.ts`/`messages.ts`; verified end-to-end on-device
      including sync and ciphertext-only for settings
  - **Post-completion refinement** (per explicit user request): editor now asks for
    first day of last menstrual period instead of due date directly — standard
    obstetric estimate (Naegele's rule: LMP + 280 days) and more accurate starting point
    than due date someone remembers or was told, one hop removed from actual
    measurement. `AppSettings.dueDate` still what's stored and synced (no schema/migration
    change) — only input semantics changed: `:core:domain` gained
    `dueDateFromLastPeriod`/`lastPeriodFromDueDate` (exact inverses, 2 new tests, 11/11 green),
    `HomeViewModel` converts at submit time and re-derives LMP date for display when
    re-opening editor. Compile, full test suite, lint, and `assembleDebug` all green.
    Unverified on-device like rest of session's UI.
- [x] Hospital-bag preset — `TasksViewModel.onSeedHospitalBag()`, additive/re-runnable (skips
      titles already present, same pattern as web version); verified live, including
      second tap doesn't duplicate
- [x] JSON import from web app — `:core:domain`'s `WebSnapshot`/`toImportedSnapshot`
      (Hebrew-literal → enum mapping table plan called "the migration contract",
      with tests), wired to SAF file picker on Home tab. Verified against real
      export-shaped fixture on-device: settings/tasks/shopping items/dates all land with
      correct category/priority/assignee/status mapping, re-import deduped, sync
      lands all of it server-side with no plaintext in ciphertext. Alternatives re-keyed
      correctly (chosenAlternativeId follows its alternative to fresh id). Not
      carried over: task `dueDate` — dead everywhere (no UI, no repository support) since
      Phase 1; pre-existing gap, not introduced here, out of scope to fix now.
- [x] Ported pregnancy and budget tests — `:core:domain` (`PregnancyProgressTest`,
      `BudgetTest`, `DailyMessageTest`, `WebImportMapperTest`), near line-for-line port of
      Vitest suite

  New this phase: `:core:domain` (pure Kotlin — `daysUntil`/`getPregnancyProgress`/
  `isPastDate`, `calculateBudget`/`itemEffectivePrice`, `dailyMessageIndex`, web-import
  mapper), Room migrations 1→2 (`shopping_items`/`important_dates`) and 2→3 (`app_settings`),
  `RoomSyncStore` generalized from hardcoded two-table `when` to check all five tables (was
  latent bug waiting for third entity type).

  **Real bug found and fixed while testing import** (not import-specific — affects
  any burst of rapid local writes, including hospital-bag preset seeding 12 tasks at
  once): `SyncWorker.syncNow()` used `ExistingWorkPolicy.KEEP`, which silently drops
  sync request arriving while one already running, nothing to catch up
  afterward. Record created after in-flight run had already read outbox could be
  permanently stranded — confirmed live (only 2 of 6 imported records reached server).
  Fixed by switching to `APPEND_OR_REPLACE`, guaranteeing at least one more run after
  current one finishes; since each run drains outbox until empty, extra runs a
  burst produces are cheap no-ops. Re-verified: all 6 records now sync.

## Phase 4 — Folders & documents
- [x] Nested folders (arbitrary depth) — `:feature:folders`, `FolderRepository`, materialized
      `path` (e.g. `/<ancestor>/.../<id>/`) derived server-independently at write time so
      subtree is `LIKE 'path%'` query, never recursive. Breadcrumb navigation, rename,
      cascading delete (deletes folder and everything under it as one soft-delete
      transaction). Verified end-to-end on-device: create → nest → rename → delete parent
      confirms child gone and unrelated sibling survives; synced; ciphertext-only
      confirmed. New `EntityType.FOLDER` sync branch in `RoomSyncStore` (already declared,
      unused until now) — migration 3→4 adds `folders`.
- [x] SAF import + document UI — folded into `:feature:folders` rather than new module:
      `FoldersUiState`/`DocumentPreview` scaffolding for this already staged uncommitted
      from earlier in session, so that's architecture being followed. `FoldersScreen`
      now lists documents alongside subfolders in current folder; FAB expands into
      "New folder" / "Import document"; import uses `ActivityResultContracts.OpenDocument()`
      (`*/*`), reads bytes + display name + MIME type off `ContentResolver`, calls
      `DocumentRepository.upload`. Delete has own confirm dialog (mirrors folder-delete
      one). `FoldersViewModel` now combines `FolderRepository.observeChildren` and
      `DocumentRepository.observeInFolder` per current parent id. `:app:compileDebugKotlin`,
      `:feature:folders:compileDebugKotlin`, full `./gradlew test`, and `./gradlew lint` all
      green. Not yet exercised on-device (deferred per this session's instruction to skip live
      testing) — needs same live verification pass as everything else before counts as
      done-done, plus Supabase Storage migration (0005) actually needs run against
      live project, hasn't been yet.
- [x] Encrypted upload pipeline with checksums — `:core:database`'s `DocumentRepository`
      (upload: encrypt then push ciphertext to `DocumentBlobStore` at `{workspaceId}/{id}`,
      SHA-256 of ciphertext stored on metadata row; download: fetch, verify hash
      before decrypt so tampered/corrupted blob caught before ever reaching cipher),
      `SupabaseDocumentBlobStore` (`:core:network`), new `Document` model, `DocumentDao`/
      `DocumentEntity`, Room migration 4→5 (`documents` table — metadata only, bytes live in
      Storage). Supabase side: `supabase/migrations/0005_document_storage.sql` adds private
      `documents` Storage bucket with select/insert RLS keyed off object path's workspace-id
      segment via `is_workspace_member`; no update/delete policy — deletion propagates through
      record's tombstone, not by mutating stored blob. Delete still leaves blob in
      Storage (orphan reclamation background-job concern, not foreground-delete one).
      `DocumentRepository`/`documentDao()` now wired into `AppModule` (was written but not
      registered with Koin — real gap, not stylistic, since nothing could've injected
      it). `:app:compileDebugKotlin` green after wiring; full `./gradlew test` also green (no
      test regressions from wiring). Still not yet run against live
      project or exercised on-device — needs same live verification pass (ciphertext-only,
      tamper detection) other entities got in Phase 1/3 before counts as done-done. No UI
      yet (that's SAF-import and attachments items below).
- [x] Preview (PDF, image, text, JSON, CSV) — `FoldersViewModel.toPreview` dispatches on MIME
      type: `text/*` (covers CSV) and `application/json` decode as UTF-8 text, `image/*` decodes
      via `BitmapFactory`, `application/pdf` renders page 1 to bitmap through `PdfRenderer`
      (bytes written to throwaway cache file, since `PdfRenderer` needs real file
      descriptor — cleaned up after render), anything else shows "unsupported" state rather
      than failing. Same not-yet-live-verified caveat as item above.
- [x] Attachments to tasks and folders — `Document` gained `taskId` field, independent of
      `folderId` (document can be filed in folder, attached to task, both, or neither).
      Room migration 5→6 adds `task_id` column + index and bumps DB to version 6;
      `DocumentDao.observeForTask`/`DocumentRepository.observeForTask` mirror existing
      per-folder query. `TasksViewModel` now observes attachments for whichever task being
      edited (task must already exist to attach to it, so only applies to edit path,
      not create-new-task path) and exposes `onAttachDocument`/`onDeleteAttachment`;
      `TaskForm` grew attachments section (SAF picker, list, remove) shown only while
      editing. `:app:compileDebugKotlin`, full `./gradlew test`, and `./gradlew lint` all
      green after wiring. Same not-yet-live-verified caveat as rest of this phase.

  **Phase 4 now feature-complete** on all five checklist items; what remains before calling
  phase done-done entirely verification: run `supabase/migrations/0005_document_storage.sql`
  against live project, then repeat live on-device pass (ciphertext-only, tamper
  detection, folder placement, task attachment, all four preview types) every other entity
  got in Phases 1/3. Deliberately skipped this session per instruction to continue
  without live tests — one thing separating "compiles and passes unit tests" from
  "actually works," should be first thing done before phase marked complete
  above.

## Phase 5 — Scanner
- [x] ML Kit document scanner, multi-page, PDF output — new `:core:scanner` module (a *core*
      module, not `:feature:scanner`, deliberately: plan's rule that feature modules must
      not depend on each other — enforced by `AndroidFeatureConventionPlugin`'s doc comment —
      meant shared capability like this belongs alongside `:core:sync`/`:core:network`, not as
      third feature both `:feature:folders` and `:feature:tasks` would have to depend on
      sideways). Wraps `com.google.android.gms:play-services-mlkit-document-scanner:16.0.0`
      (confirmed as current GA version against Google's Maven index, not guessed) behind one
      composable, `rememberDocumentScanner`: launches on-device scan UI (crop/enhance,
      multi-page, gallery import disabled so every page is live capture) via
      `GmsDocumentScannerOptions.RESULT_FORMAT_PDF`, so ML Kit does page-merging — no
      client-side PDF assembly needed — returns one `ScannedDocument(name, mimeType, bytes)`.
      Entirely on-device: no cloud vision/OCR call, consistent with why cloud OCR ruled out
      for this app (see Deferred, bottom of file).
- [x] Save to folder, optional task attachment — no new plumbing needed: both call sites reuse
      upload path Phase 4 already built. `FoldersScreen`'s FAB gained third option ("Scan
      document", alongside "New folder"/"Import document") calling same
      `actions.onDocumentPicked` as SAF import, so scan saves into current folder.
      `TasksScreen`'s attachments section gained matching "Scan document" button next to
      "Attach document", calling `actions.onAttachDocument`. `:core:scanner:compileDebugKotlin`,
      `:app:compileDebugKotlin`, full `./gradlew test`, and `./gradlew lint` all green.
      **Phase 5 feature-complete but wholly unverified on real device** — Play services
      module (downloaded on first use, not bundled) never actually exercised; this is
      most likely thing in whole app to have API-shape mistake couldn't catch by
      compiling, since nothing here executes scanner at build time. First thing
      to live-test when live testing resumes, ahead of Phase 4's backlog.

## Phase 6 — Cycle module (full)
- [x] Predictions from history, with insufficient-history handling — `:core:domain`'s
      `predictNextCycle` (new `cycle/CyclePrediction.kt`). Cycle length measured as gap
      between consecutive period *start* dates, never single period's own length — needs
      at least 2 recorded starts (1 gap) to say anything; with fewer returns
      `hasSufficientHistory = false` rather than guessing with textbook 28-day default
      (presenting guess as fact exactly what prediction feature must not do). When enough
      history: next period = last start + average gap, ovulation = next period − 14
      days (textbook luteal phase, explicitly documented as estimate not measurement),
      fertile window = 5 days before ovulation through 1 day after. 6 tests
      (`CyclePredictionTest`), including insufficient-history cases and 3-cycle average.
- [x] Statistics — `:core:domain`'s `calculateCycleStatistics` (`cycle/CycleStatistics.kt`):
      cycle count, average/shortest/longest cycle length (from start-date gaps), average period
      length (from completed periods only — ongoing one has no end yet to measure). 4 tests
      (`CycleStatisticsTest`).
- [x] Symptoms, flow, mood, pain, notes — new `CycleEntry` model/entity, separate from
      `MenstrualCycle`: most logged days aren't period days at all, so day-level detail
      (flow/symptoms/mood/pain/note) tracked independently of period start/end, matching
      `CYCLE_ENTRY` `EntityType` already declared-but-unused since Phase 1 (same pattern
      as `FOLDER`/`DOCUMENT` before their phases — clearly the plan). At most one entry
      per calendar date: logging already-logged date updates it in place (found by date
      lookup in repository, not DB uniqueness constraint — constraint would reject
      legitimate edit). Room migration 6→7 adds `cycle_entries` + `documents.cycle_id`,
      bumps DB to version 7. New `DatabaseConverters` entries for `FlowLevel`/`PainLevel`
      (nullable enum-by-name) and `List<Symptom>`/`List<Mood>` (JSON, same pattern as
      `ShoppingAlternative`). `RoomSyncStore` gained `CYCLE_ENTRY` branch in all four
      places (`markSynced`, `markConflict`, `applyRemote`, `serialize`, `entityTypeOf`) —
      with all 8 `EntityType` values now handled, `when` blocks became exhaustive and their
      `else` branches had to be deleted (compiler treats now-redundant `else` as warning,
      this build treats warnings as errors). No Supabase migration needed: `cycle_entry` was
      already in `entity_type` enum from `0001_init.sql`.
- [x] Cycle calendar (actual / predicted / estimated, distinguished without relying on color) —
      `CycleScreen`'s new `CalendarGrid`: plain Sunday-first month grid (no external calendar
      library). Distinguishing marks are shape/fill, not just color, so screen doesn't
      depend on color perception: actual logged period days filled circle, predicted
      period days tinted circle at low alpha (still visibly different fill, not just
      different hue), fertile window small dot below day number, estimated
      ovulation day small square below day number (square vs dot, not red vs green).
      Tapping any day opens bottom sheet (`DayForm`) to log or edit that date's flow/pain
      (single-select `FilterChip` rows)/mood/symptoms (multi-select)/note, pre-filled from any
      existing entry; days with logged entry render number bold as additional
      non-color cue. Month navigation is chevron buttons, no swipe gesture.
- [x] Cycle document attachments — `Document` gained `cycleId` field alongside existing
      `taskId`/`folderId` (independent of both — ultrasound report can attach to
      period without living in any folder). `DocumentRepository.observeForCycle`/`upload(...,
      cycleId = ...)` mirror task-attachment methods from Phase 4. Each history row expands
      (tap row) into same attach-via-SAF / attach-via-scan / delete UI as tasks and
      folders, reusing `:core:scanner`'s `rememberDocumentScanner`.
- [x] `:feature:cycle` gained `:core:domain` and `:core:scanner` dependencies.
      `:feature:cycle:compileDebugKotlin`, `:app:compileDebugKotlin`, full `./gradlew test`
      (10/10 new domain tests green, no regressions elsewhere), and `./gradlew lint` all
      green. **UI entirely unverified on-device** — same caveat as every other UI-layer phase
      this session: compiles and pure-logic pieces unit-tested, but nobody tapped a
      calendar day on real screen yet. Calendar's Sunday-first assumption in particular
      a guess (app has no existing Settings-driven first-day-of-week preference to
      follow) and should be sanity-checked live, along with RTL layout for day grid and
      day-detail sheet's chip rows.

## Phase 7 — Security hardening & settings
- [x] Settings tree — new `:feature:settings` module, new bottom-nav tab (`HomeTab.Settings` in
      `SaharApp.kt`). Sections: Security, Notifications, Recovery phrase, Devices, Sign out.
      No `NavHost` exists in this app (routing derived `when` state, see `SaharApp.kt`'s doc
      comment), so device management reached by flipping local `showDeviceManagement`
      boolean in `HomeRoute` rather than real navigation stack — consistent with how every
      other screen here already works, not shortcut specific to this phase.
- [x] Biometric lock, auto-lock — found and fixed real gap first: `SessionState.lock()` alone
      was not a lock. Routing on `!session.isUnlocked` fell through to `PairingRoute()`, whose
      `PairingViewModel.init` unconditionally calls `onRefresh()`, which reads device's own
      Keystore-sealed key copy via `DeviceIdentity.workspaceKey()` and silently reopens
      session — no prompt, no gate, "lock" undid itself instant screen recomposed.
      Fixed with `SessionState._locked` flag independent of key presence, checked in
      `SaharApp`'s routing `when` *before* `!isUnlocked` branch, routing to new
      `LockRoute` instead of back through pairing. `LockRoute`
      (`app/.../lock/LockScreen.kt`) drives `androidx.biometric.BiometricPrompt`
      (`BIOMETRIC_STRONG or DEVICE_CREDENTIAL`) and on success calls new
      `SessionState.unlock(key)` — re-arming already-open session from
      `DeviceIdentity.workspaceKey()` directly, never touching network or re-running
      pairing. `AutoLockController` (`DefaultLifecycleObserver` on `ProcessLifecycleOwner`, not
      Activity's own lifecycle — rotation or multi-window change stops/restarts an
      Activity without app leaving foreground) locks after configurable timeout, but
      **only when biometric unlock enabled**: app has no separate password, so lock
      with nothing able to safely re-open it would just strand user — auto-lock
      deliberately inert until user opts into one thing that can unlock it again.
      `SessionController` (new interface in `:core:security`, implemented by `SessionState`)
      is seam letting `:feature:settings` trigger lock/sign-out without depending on
      `:app`, mirroring `WorkspaceKeyProvider`'s existing read-side seam.
- [x] Screenshot policy — `MainActivity` applies `FLAG_SECURE` from `SettingsPreferences` flow,
      defaulting **on**: opt-in default would leave most users unprotected without ever
      knowing option existed, and app's whole reason to exist is keeping content
      private.
- [x] Locally scheduled notifications, generic text by default — `ReminderWorker` (WorkManager,
      1-day period, mirrors `SyncWorker`'s existing pattern) posts one notification via
      `ReminderNotifier` with hardcoded generic copy ("You have updates to check") — never task
      title, due date, anything from workspace. Notification tray not part of
      this app's encryption boundary (any app, or lock screen, can read it), so personalized
      notification would leak exactly what rest of app exists to protect.
      Opt-in (default off) via Settings toggle; turning it on requests `POST_NOTIFICATIONS`
      at runtime on Android 13+ (manifest permission added) before actually scheduling.
      `ReminderScheduler` (interface in `:core:settings`, `WorkManagerReminderScheduler` impl in
      `:app`) is same seam pattern as `SyncTrigger`.
- [x] Device management — Settings' "Manage devices" reuses `PairingViewModel`'s already-live
      `Ready` stage (invite-code generation, pending-device approval) rather than rebuilding it;
      this first actual UI entry point for it since Phase 1 noted join/approve path
      "has no UI entry point yet."
- [x] Recovery-phrase flow — both halves now done. "Show recovery phrase" in Settings
      re-derives words on demand via `RecoveryPhrase.encode(identity.workspaceKey())`.
      Other half, added this pass: new `PairingStage.EnterRecoveryPhrase`, reachable
      from `Choose` (signed in, never joined workspace on this device) and from
      `AwaitingKey` (joined, but recovering directly faster than waiting for partner
      device to approve). Finally exercises `RecoveryPhrase.decode()`, which sat completely
      unused since Phase 1 despite being fully tested. `onSubmitRecoveryPhrase` resolves
      workspace id from wherever device already knows it (`identity.workspaceId` if
      joined, else `workspaces.currentWorkspaceId()` off account's membership — phrase
      alone carries no workspace id of its own), decodes phrase, saves key, and
      registers this device (so it shows up in device management even though never
      part of normal join flow). Bad phrase fails loudly via BIP-39 checksum,
      surfaced as new `pairing_error_invalid_phrase` string rather than falling through to
      generic error message. `:feature:pairing:compileDebugKotlin`,
      `:app:compileDebugKotlin`, `:app:assembleDebug`, full `./gradlew test`, and
      `./gradlew lint` all green. Unverified on-device like rest of session — this
      one particular touches same `identity.workspaceId`/`saveWorkspaceKey` state normal
      join flow depends on, so worth confirming it doesn't leave that state
      inconsistent for device that later also goes through normal pairing.
- [x] Key rotation — **narrower than full rotation, deliberately scoped that way**. Built
      device-key **revocation**, not workspace-symmetric-key rotation:
  - `supabase/migrations/0006_revoke_device_key.sql` adds `SECURITY DEFINER` RPC,
    `revoke_device_key(target_device_key_id)`, checks workspace membership and sets
    `device_keys.revoked_at`. Deliberately not broadened RLS `UPDATE` policy — a
    workspace-member-scoped `UPDATE` on `device_keys` would let either partner overwrite
    *other* partner's device's `public_key` column too, not just `revoked_at`, which is MITM
    vector, not revoke feature. RPC mirrors existing `accept_invitation()` pattern and
    only ever touches `revoked_at`.
  - `WorkspaceRepository`: `PartnerDevice` gained `isRevoked: Boolean`, `DeviceKeyRow` now reads
    `revoked_at`, new `revokeDevice(deviceKeyId)` calls RPC.
  - `PairingViewModel.showReady()` now filters revoked devices out of `pendingDevices`
    entirely (revoked device no longer approvable), separately computes
    `revocableDevices` — active, already-key-holding devices other than this one — for new
    `onRevokeDevice(deviceKeyId)` action.
  - `PairingScreen`'s `Ready` stage (reused by Settings → Manage devices) shows "Paired
    devices" list with Revoke button per non-self device.
  - **What this doesn't do, and why that matters**: revoking `device_keys` row doesn't end
    that device's ongoing Supabase Auth session, doesn't rotate workspace's
    symmetric key. `records` RLS governed by `workspace_members`/`auth.uid()`, not
    `device_keys` — revoked device still signed in keeps reading/writing records
    until its session ends or signed out elsewhere. Revocation only prevents that device's
    public key from being trusted for *future* key-wrap grants (i.e. can no longer be handed
    workspace key by re-pairing). Full workspace-key rotation — re-encrypting every record
    under new key and re-wrapping to every remaining device — out of scope: needs
    pgTAP coverage of re-wrap fan-out this environment's Supabase pgTAP harness cannot
    currently run (see earlier note on that pre-existing environment issue), so building it
    without that safety net judged too risky.
  - `:core:network:compileDebugKotlin`, `:feature:pairing:compileDebugKotlin`,
    `:app:compileDebugKotlin`, `:app:assembleDebug`, full `./gradlew test`, and `./gradlew lint`
    all green. Unverified on-device, same caveat as rest of session — new
    Supabase migration itself hasn't been applied against live project or exercised end to
    end (no pgTAP run either, same reason full rotation skipped).
- Known gap, not on original checklist but surfaced while building sign-out: signing out
  clears `SessionState` and `DeviceIdentity` (device keypair, workspace key, registration) but
  does **not** wipe local Room database. SQLCipher passphrase (`KeystoreDatabasePassphrase`)
  device-Keystore-backed and independent of workspace pairing, so previously-synced decrypted
  content stays readable by this app on this device after sign-out. Real "forget this
  device" flow needs to drop/recreate database too — not attempted this pass.
- `:app:compileDebugKotlin`, full `./gradlew test`, and `./gradlew lint` all green after every
  addition above. As with every UI-layer phase this session: compiles and (where pure
  logic) unit-tested, but nothing here tapped on real device — biometric
  prompt, auto-lock timing, and FLAG_SECURE toggle all specifically need live pass
  before counted as done-done, more so than most UI because security-relevant and
  Compose previews/unit tests cannot exercise `BiometricPrompt` or process-lifecycle timing at
  all.

## Phase 8 — Search, calendar, conflicts, polish
- [x] Search — **FTS4, not FTS5**: Room 2.8.4 (version pinned here) has no `@Fts5`
      annotation at all, only `@Fts3`/`@Fts4` (confirmed directly against jar). Hard
      library limitation, not risk-averse substitution — corrected
      `docs/architecture/007-encryption.md`'s "FTS5" claim to match. Single hand-maintained
      `search_index` FTS4 table (`SearchIndexEntity`) spans all seven searchable entity types
      (tasks, shopping items, important dates, folders, documents, cycles, cycle entries) —
      Room's `contentEntity` mirroring only ever binds one table, so real unified index has
      to be kept in sync by hand. `SearchIndexer` (`:core:database`) is that sync point: every
      repository's `enqueue`/upload and soft-delete now also calls `index()`/`remove()` inside
      same transaction as its normal write, so index can never drift from what's
      on-screen. `RoomSyncStore.applyRemote` got same treatment — record synced in from
      partner's device indexed (or removed, if arrived as tombstone) exactly like local write,
      so search covers whole shared workspace, not just what created on
      this device. Migration 7→8 creates table and backfills it from every already-synced
      row via `INSERT ... SELECT`, so upgrading doesn't leave existing data unsearchable until
      its next edit. New `:feature:search` module + 8th bottom-nav tab; results show which
      entity type matched and snippet, tapping one switches to tab that owns it (no
      per-screen "scroll to this item" support built, so gets you to right area, not
      exact row — documented gap, not oversight). `SearchRepository.toFtsQuery` turns
      free text into safe `term*` prefix-match query per word rather than passing raw user
      input to `MATCH`, which would either mis-parse as boolean operators or throw on stray FTS
      syntax characters. `:app:assembleDebug`, full `./gradlew test`, and `./gradlew lint` all
      green. **Unverified on-device** like everything else this session — FTS4 query behavior
      and migration backfill in particular need real run against actual synced data.
- [x] Unified calendar — real gap found first: `Task.dueDate` existed in model since Phase 1
      but `TaskRepository.create`/`update` never accepted it and no UI ever set it (flagged as
      known gap back in Phase 3's import work). Fixed alongside calendar, since
      due-date calendar with no way to ever set due date would be pointless: `TaskRepository`
      now takes `dueDate: LocalDate?` in both, `TasksScreen` gained due-date field (Material3
      `DatePicker`, same pattern as `:feature:dates`) in add/edit sheet.
      New `:feature:calendar` module + 9th bottom-nav tab (see note below). `CalendarViewModel`
      merges three live flows — `TaskRepository.observeAll` (by `dueDate`),
      `ImportantDateRepository.observeAll`, `CycleRepository.observeAll` (actual period days,
      plus predicted ones via `:core:domain`'s `predictNextCycle`/`calculateCycleStatistics`,
      reused as-is from Phase 6) — via `combine` into one `List<CalendarEvent>`. Deliberately
      not stored/synced entity: read-side projection recomputed live, same reasoning
      as search's index but simpler since nothing here needs to survive process restart faster
      than three source flows can re-emit. Month grid marks days with colored dots per event
      kind (task/date/period — not relying on color alone was Phase 6's cycle-calendar
      precedent; this one smaller feature and uses color-coded dots only, real
      accessibility gap flagged for Phase 8 accessibility pass rather than fixed twice).
      Tapping day opens bottom sheet listing that day's events; tapping event switches to
      tab that owns it (same "right area, not exact row" gap as search's `OpenResult`).
      `:app:compileDebugKotlin`, `:app:assembleDebug`, full `./gradlew test`, and
      `./gradlew lint` all green.
  - **Bottom nav now 9 tabs** (Home/Tasks/Shopping/Dates/Folders/Cycle/Calendar/Search/
    Settings) on phone screen. Already past typical guidance at 7 before this
    session touched it (Settings, Search) and worse now. Flagged here rather than fixed:
    real UX problem (needs consolidation, overflow menu, or icon-only compact tabs) that
    deserves own deliberate pass, not reflexive fix mid-calendar-feature.
- [x] Conflict resolution UI — data layer had this waiting since Phase 1:
      `SyncConflictEntity`/`SyncStateDao.observeConflicts()`/`observeConflictCount()` existed,
      declared but no UI ever reading them (same "built ahead of phase that needs it"
      pattern as `FOLDER`/`DOCUMENT`/`CYCLE_ENTRY` before their phases). New
      `ConflictRepository` (`:core:database`) only place a conflict ever gets resolved,
      by person choosing side — nothing here auto-resolves anything, matching
      `SyncConflictEntity`'s own doc comment ("resolving it silently is how partner's edit
      disappears without either noticing"). `keepLocal` re-queues local row for
      push, rebased onto server's version so retry isn't immediately rejected as stale
      again; `keepServer` decrypts and applies server's copy in place of local one and
      marks it synced, since nothing left to push. Both paths reindex search
      (`SearchIndexer`) and go through `database.withTransaction`, same discipline as every
      other write path this session. Necessarily duplicates `RoomSyncStore`'s per-entity-type
      `when` dispatch (7 branches) rather than reusing it — `RoomSyncStore` methods aren't
      structured to be called from outside sync pipeline. New `:feature:conflicts` module,
      hosted at app root (`ConflictHost` in `SaharApp.kt`, alongside `UpdateHost`) rather
      than behind tab — stuck conflict means partner's edit isn't syncing, which deserves
      surfacing proactively, not burial in 10th nav tab on top of 9 already there. Not
      modal like mandatory update: dismissible, but re-surfaces whenever conflict count
      changes. `:app:compileDebugKotlin`, `:app:assembleDebug`, full `./gradlew test`, and
      `./gradlew lint` all green. **Unverified on-device**, more than most features this
      session actually needs it: this the one path never exercised even
      indirectly (nothing else in app deliberately produces conflict), so actual
      SQLite/Room mechanics of "two devices edit same row, one gets rejected, resolve it"
      unverified beyond compiling.
- [x] Recurring tasks (rule-based) — `Task` gained `recurrence: Recurrence?`
      (`RecurrenceFrequency` DAILY/WEEKLY/MONTHLY + interval — "every 2 weeks" needs
      interval, bare frequency enum can't express it), only meaningful alongside `dueDate`
      since rule needs date to advance from. `:core:domain`'s `nextDueDate` (new
      `task/RecurrenceAdvance.kt`) is pure advance-by-one-occurrence math — monthly
      preserves day-of-month where target month has it and clamps to last day where
      it doesn't (Jan 31 monthly → Feb 28, not March 3); 4 tests, all green. Deliberately kept
      out of `:core:database`: `TaskRepository.completeAndScheduleNext(id, nextDueDate)` takes
      already-computed next date as plain parameter rather than importing
      `:core:domain` itself, so data layer's dependency graph stays one-directional
      (domain math depends on nothing; data layer doesn't reach up into it) — caller
      (`TasksViewModel`, which already needs domain logic) computes it. Completing recurring
      task both marks current occurrence done *and* creates next one in same
      transaction (new task row, `dueDate` advanced, fresh id — not mutation of the
      completed one, so completed occurrence's history stays intact rather than being
      overwritten). Un-completing, or completing non-recurring task, still plain toggle.
      Migration 8→9 adds `tasks.recurrence_frequency`/`recurrence_interval` (both null =
      "does not repeat", default for every existing row). `TasksScreen` gained repeat
      picker (shown only once due date is set) and small repeat icon on recurring rows.
      Interval fixed at 1 in UI for now (model and domain math both support
      arbitrary intervals — "every 2 weeks" — but picker for it wasn't built this pass,
      scoped down deliberately rather than left half-wired).
      `:app:compileDebugKotlin`, `:app:assembleDebug`, full `./gradlew test` (4/4 new domain
      tests green), and `./gradlew lint` all green. Unverified on-device like rest of this
      session's UI work.
- [x] Tags — scoped to `Task` only, not every entity (original spec's schema sketch, per
      `docs/specs/01-android-conversion.md` §54, proposes normalized `tags`/`task_tags`
      tables for cross-entity reuse and autocomplete; skipped that for `List<String>` column
      on `Task` — same JSON-column pattern already used for `CycleEntry`'s `symptoms`/`mood` —
      because this app is one couple's task list, not multi-tenant catalog that needs
      reuse/autocomplete infrastructure to stay fast). Spec's "cycle tags are private,
      never shown to partner" clause dropped as already superseded by this app's
      actual threat model (couple-vs-outsiders, not partner-vs-partner — see note at
      top of file); nothing here treats any tag as hidden from either user.
      Migration 9→10 adds `tasks.tags` (`'[]'` default). Tags freely user-defined text,
      normalized on entry (`#Medical`, `medical`, `Medical` all become `medical` —
      case/`#`-sensitive match would quietly stop matching first time tag typed
      differently) via small `normalizeTag` helper in `TasksViewModel`. `TasksScreen` gained
      tag-chip input in add/edit form and horizontally-scrolling filter row above task list
      (derived from whatever tags currently exist across all tasks — no separate
      "manage tags" screen, since set of tags in use already *is* that list). Tags feed
      into search too — `TaskRepository`'s `searchBody()` now folds tags into indexed text
      alongside note, so `#medical` finds task even if word never appears in its
      title.
      `:app:compileDebugKotlin`, `:app:assembleDebug`, full `./gradlew test`, and
      `./gradlew lint` all green. Unverified on-device like rest of this session's UI.
- [x] Accessibility pass — used `android-skills:android-ux`'s M3 compliance audit (category 9
      + quick grep checks for categories 1/3/5) rather than freehand review. Findings:
      - **Real bug, fixed**: device-management back button (`SaharApp.kt`'s
        `DeviceManagementRoute`) was standalone `IconButton` with
        `contentDescription = null` — TalkBack would have announced it as unlabeled
        button with no indication it navigates back. Every other `IconButton` in app
        already had real description; grepped for pattern to confirm this was only
        one. Added `R.string.app_back` (new, `:app` had no strings.xml entries for this kind
        of generic chrome yet).
      - **Real bug, fixed**: Cycle's and Calendar's day-grid cells (`DayCell` in both
        `CycleScreen.kt` and `CalendarScreen.kt`, both new this session) were 36dp tap
        targets — below M3's 48dp minimum. Fixed with
        `Modifier.minimumInteractiveComponentSize()` (actual M3 mechanism for this,
        not hand-rolled padding math) ahead of `.size(36.dp)`, so visual circle stays
        compact for 7-column month grid while tappable area expands to 48dp, centered.
        Applied to blank leading-day placeholders too, so grid columns stay aligned.
        Trade-off accepted, not hidden: 7 × 48dp = 336dp minimum row width, tight
        (not necessarily broken, but untested) on smallest screens `minSdk 26` nominally
        allows; real devices that narrow vanishingly rare in 2026, so not chased
        further.
      - **Fixed**: every screen's page-title `Text` (Calendar, Cycle, Settings, Auth) and
        Settings' section-card titles now carry `Modifier.semantics { heading() }`, so
        TalkBack users can jump between sections instead of swiping through every element
        linearly.
      - **Checked, no finding**: categories 1/3/5's grep checks (hardcoded colors, hardcoded
        corner radii, Material 2 imports) — only hardcoded-color hits are `Color.kt`
        itself (token *definitions*, where they belong) and moon-countdown's
        intentionally-hardcoded night palette (documented in `CLAUDE.md` as deliberate
        exception); no M2 imports found anywhere; all `FloatingActionButton`/other
        icon-only actions already had real content descriptions.
      - **Not fixed, flagged**: Tasks, Shopping, Dates, Folders, and Home have no in-page
        heading `Text` at all — rely entirely on bottom-nav tab label for context,
        which screen-reader user landing directly on content won't hear. Adding page
        titles to five screens is new UI surface, not semantics-only change, so
        flagged rather than added reflexively under "accessibility pass" label.
      `:app:compileDebugKotlin`, `:app:assembleDebug`, full `./gradlew test`, and
      `./gradlew lint` all green. Touch-target and heading fixes structural
      (semantics/sizing), so about as verifiable from source as accessibility work
      gets without TalkBack pass on real device — still worth actual screen-reader
      run before calling this done-done.
- [x] Performance (paging, indexes, thumbnails) — scoped down deliberately on first of
      three, with reasoning written down rather than silently dropped:
      - **Paging — skipped, not built.** App has exactly two users' worth of data —
        tasks, shopping items, documents in folder realistically dozens, not thousands,
        forever. Paging 3 (`PagingSource`, `RemoteMediator`, `LazyPagingItems`) solves
        problem this app structurally doesn't have; adding it would be complexity for its own
        sake, not performance fix. Revisit only if real usage pattern ever produces
        genuinely large lists (won't, for two-person household).
      - **Indexes — audited, no gap found.** Read every entity's `indices` list: all nine
        syncable tables already carry `sync_status` (push query), `workspace_id +
        updated_at` (pull cursor and list ordering), and query-shaped composite index where
        screen actually filters on something else (`category` on tasks/shopping,
        `due_date` on tasks, `path`/`parent_id` on folders, `folder_id`/`task_id`/`cycle_id`
        on documents, `start_date` on cycles). Kept current incrementally as each
        entity added this session, rather than needing catch-up pass now.
      - **Thumbnails — built, and design is actual point of this item.** Documents
        had no preview at all before this — just generic file icon, even for photos. The
        obvious approach (fetch thumbnail from Storage) doesn't exist for E2EE content:
        no cheap way to fetch "just a preview" of encrypted blob, smallest
        fetchable unit is whole file. So `DocumentRepository.upload` generates small
        (160px, JPEG quality 70) downsampled preview **from plaintext bytes already in
        memory for encrypt call right after it** — no extra fetch, no extra decrypt —
        using `inSampleSize`-based decoding so peak memory stays bounded even for large
        photo. Base64 preview travels in `Document`'s existing small metadata record
        (already syncs as encrypted JSON), so partner's device gets thumbnail
        for free without downloading and decrypting original either. Migration 10→11
        adds `documents.thumbnail_base64`; null for every existing document and every
        non-image one — deliberately not backfilled, since generating one would mean
        downloading and decrypting every existing image during schema migration, which
        must never be network-dependent or unbounded. `FoldersScreen`'s document rows now
        show decoded thumbnail (cached via `remember` so scrolling doesn't redecode
        every frame) instead of generic icon when one exists.
      `:app:compileDebugKotlin`, `:app:assembleDebug`, full `./gradlew test`, and
      `./gradlew lint` all green. Unverified on-device like rest of session — the
      thumbnail generation path in particular (real image bytes through `BitmapFactory`)
      never run outside JVM/emulator-free compile step.

## Phase 9 — Docs
- [x] Rewrote `README.md` — Android (`android/`) now documented as active product:
      what app is, module layout (`:core:*`/`:feature:*`), build/run commands,
      workspace/pairing/E2EE model at glance, pointers out to `docs/architecture/` and
      `PROGRESS.md` rather than duplicating their depth. Web app's fate, per user
      (asked directly rather than guessed): **retired**. `src/` section trimmed to "legacy,
      kept for reference; still source of one-time JSON import path."
      `.github/workflows/deploy.yml` (GitHub Pages auto-deploy) deliberately left alone —
      disabling/removing CI bigger, more reversible-cost decision than docs pass
      should make unilaterally; flagged here as candidate for follow-up cleanup, not done.
- [x] `docs/architecture/` drift pass — read all four ADRs (001 architecture, 005 privacy,
      007 encryption, 011 release/updates) against what Phases 1–7 actually built. **No
      drift found** — all four hold up exactly as written, including 007's prediction that
      "reminders are scheduled locally" (Phase 7's `ReminderNotifier`, built independently
      this session, matches it precisely) and 011's mention of manual "Check for updates"
      in Settings, which Phase 7's Settings screen had actually missed until this pass caught
      it (see fix noted under Phase 7... actually added here: `SettingsScreen` gained
      `UpdateViewModel.onCheckNow()` button, wired via `footer` slot since `:feature:settings`
      can't depend on `:feature:update` — same cross-feature seam pattern as device management).
- [x] `CLAUDE.md` — added full "Android app" section (commands, no-cross-feature-deps
      rule, no-`NavHost` routing shape, persistence/sync flow, where domain math goes, the
      "adding synced entity touches these five places" checklist, bilingual strings) ahead
      of original web-app content, now explicitly marked legacy/retired and
      demoted under "Web app (legacy)". `AGENTS.md` unrelated boilerplate (caveman-mode
      activation file, not project documentation) — nothing to update there.
- [x] `task.md` — turned out not to need anything. Investigating "threat-model note"
      pointer at top of file found it was **wrong**: blamed `task.md` for the
      superseded private-by-default/partner-sharing model, but that content actually lives in
      `docs/specs/01-android-conversion.md` — `task.md` is unrelated auto-update spec,
      already faithfully captured in `docs/architecture/011-release-signing-and-updates.md`,
      contains no privacy/sharing content at all (checked). Fixed note at top of
      this file to point at right spec, corrected same wrong reference in this
      session's saved memory (`couple-app-threat-model.md`), which had copied same error.
- [x] Debug build sanity check (pulled forward from Phase 10 since docs-adjacent
      "does everything actually still fit together" check): `./gradlew :app:assembleDebug`
      succeeds end-to-end, produces real APK (`app-debug.apk`, ~39 MB). Also ran
      `:app:assembleRelease` (unsigned, no keystore secrets in this environment) to confirm
      R8/shrink/proguard path itself works, not just per-module `compileDebugKotlin` —
      succeeded, producing `app-release-unsigned.apk`.

## Phase 10 — Release
- [x] Debug build — `:app:assembleDebug` succeeds, produces `app-debug.apk`. Done under
      Phase 9 above (pulled forward as "does it all still fit together" check).
- [~] Release build — build path works (`:app:assembleRelease` succeeds: R8 minify,
      resource shrinking, proguard all run clean, produces `app-release-unsigned.apk`), but
      it is genuinely **unsigned** — no `ANDROID_KEYSTORE_*` secrets exist in this
      environment, and per `docs/architecture/011-release-signing-and-updates.md` that's
      correct behavior (never silently fall back to debug signing) rather than bug to fix.
      Actual signed, installable release needs real keystore secrets, which only exist
      as GitHub Actions secrets + user's offline backup — not obtainable from inside this
      session. Tagging `v*` and letting `.github/workflows/android-release.yml` run is
      real release path; nothing to do here beyond confirming local build itself sound.
- [x] Final report — `docs/FINAL_REPORT.md`, following exact template in
      `docs/specs/01-android-conversion.md` §85 (Implementation Summary sections plus
      explicit "how X works" list at end). Written now rather than waiting on live
      verification, per explicit instruction to finish what doesn't depend on it — but its
      "Known Limitations" section says so plainly and up front, per that same spec section's
      own instruction not to claim app is secure or working without having verified it.
      Live-verification backlog (Phases 4–8, all built and unit-tested but never run on
      device) is load-bearing caveat across this entire report, not footnote.

## Phase 11 — Live two-device testing (in progress)

First real on-device pass, two physical phones (Pixel 9, Xiaomi/MIUI device) over adb,
two separate real Supabase accounts. Session state as of 2026-08-16:

**Confirmed working live:**
- App launches and renders on both devices, Hebrew (RTL) and English, no crashes across
  full pass of all 9 tabs (Home, Tasks, Shopping, Dates, Documents, Cycle, Calendar, Search,
  Settings).
- Naegele's-rule last-period math (Phase-session change earlier today) computes correctly
  on-device ("Week 34, day 4 of 40" etc.), on both phones independently.
- Full real pairing flow end-to-end: create workspace → recovery phrase shown → generate
  invite code → enter code on second device → approve from first device → both devices
  share one `workspace_members`-backed workspace. This is **first time real
  invite/accept/approve flow has been exercised live**, not just recovery-phrase-based
  re-entry.
- Recovery-phrase re-entry verified live on both devices independently (BIP-39 checksum
  decode, correct key recovery).
- Device revocation (`revoke_device_key` RPC, migration `0006_revoke_device_key.sql`) —
  **pushed to live linked Supabase project this session** (`npx supabase db push
  --linked`) and confirmed working end-to-end after push; failed with generic error
  beforehand because migration file existed locally but had never been applied remotely.
  Cleaned up several duplicate `device_keys` rows this way.
- Cross-device sync confirmed working: last-period date and cycle entry saved on one
  device appeared on other after either periodic 6-hour worker or explicit
  refresh.
- Pull-to-refresh added to Home this session specifically because live testing showed
  **no way to trigger sync from UI** — only 6-hour periodic WorkManager job and
  push-after-local-write path, so partner's change could sit unseen for hours with
  app open. `HomeViewModel` now calls `SyncEngine.sync()` directly (not through
  `SyncTrigger`/WorkManager, since UI needs to await completion to stop spinner) via
  new `refreshing: Boolean` state and `onRefresh()` action; `HomeScreen` wraps its content
  in `PullToRefreshBox` (`androidx.compose.material3.pulltorefresh` — not top-level
  `material3` package, easy to get wrong). **Only wired into Home so far** — Tasks, Shopping,
  Dates, Documents, Cycle, Calendar have no pull-to-refresh yet and share same "can sit
  stale for hours" gap. Compiles, full `./gradlew test`, `./gradlew lint` all green;
  confirmed working live on-device by explicit user test.
- Shopping (add item), Cycle (log period start) confirmed working live end-to-end including
  real save.
- Search confirmed working live: shopping item created during this session was found via
  full-text search.

**Bugs found live, fixed this pass (2026-08-16, no live device access this session — fixed
from root-cause analysis below, all three compile clean, full `./gradlew test` and
`./gradlew lint` green after each; still need re-verification on actual two devices
before called done-done):**
- **Invite-code entry field scrambled input when typed programmatically.** Root cause found:
  `EnterCodeStage`'s `OutlinedTextField` fed *dashed display string* back out through
  `onValueChange` on every keystroke — `value = InvitationToken.forDisplay(uiState.enteredCode)`,
  `onValueChange = actions::onCodeChange`. Each keystroke's dash-insertion changes string
  length by more than one character relative to raw typed text, which breaks Compose's own
  cursor-position diffing between old and new `String` value — real bug, not artifact
  of `adb input text`'s lack of real per-key IME timing (that just made it easy to trigger
  reliably; paste and fast IME autocomplete could hit same path). Fixed by keeping field's
  actual value as *raw* undashed code and moving dash formatting into
  `VisualTransformation` (`inviteCodeDashTransformation` in `PairingScreen.kt`) with explicit
  `OffsetMapping`, so underlying text/cursor state Compose tracks always matches what was
  actually typed — display-only dashes never touch it. `onCodeChange` (still normalizes
  through `InvitationToken.normalize`) unchanged. Where:
  `feature/pairing/PairingScreen.kt`'s `EnterCodeStage`.
- **Folders screen's expanded FAB actions exposed no accessible text.** Each
  `ExtendedFloatingActionButton`'s icon had `contentDescription = null` (correct, since
  adjacent `text` slot supposed to cover it) but nothing forced that visible text into
  button's own merged semantics node, so TalkBack found nothing (`NAF="true"` on all three).
  Fixed by adding `Modifier.semantics { contentDescription = <same string as the visible label> }`
  to each of three buttons — belt-and-suspenders over relying on text slot's own
  semantics merging. Where: `feature/folders/FoldersScreen.kt`'s `floatingActionButton` block.
- **Duplicate `device_keys` registrations were indistinguishable.** Root cause: sign-out wipes
  `DeviceIdentity` (fresh keypair every time, per known Phase 7 gap) but
  `registerDevice()`'s label was just `"${Build.MANUFACTURER} ${Build.MODEL}"` — identical
  across every re-registration from same physical device, since ANDROID_ID would've stayed
  constant anyway and doesn't vary per-keypair. Fixed by appending 4-hex-char suffix derived
  from SHA-256 of *that registration's own public key* (`keySuffix()` in
  `PairingViewModel.registerDevice`) — stable for lifetime of one registration, distinct
  across registrations, needs no new state to track since keypair itself already changes
  each cycle. Where: `feature/pairing/PairingViewModel.kt`'s `registerDevice`.
- One UI navigation got Android device itself stuck with `NotificationShade` holding
  window focus above app (no crash, no exception, unclear trigger — happened once, not
  reproduced deliberately) — recovered via normal device lock/unlock cycle, not app fix.
  Noting in case it recurs; not clearly app bug versus OS/vendor (MIUI or Pixel) quirk.

**Explicitly not yet tested live:**
- Folders: actually creating folder end-to-end (blocked on FAB accessibility/testing
  friction above, not re-attempted after stuck-notification-shade detour).
- Documents: upload, scan (ML Kit document scanner), nested folder navigation, cascading
  delete.
- Dates: adding item through form (screen loads and shows correct empty state;
  add flow itself wasn't exercised).
- Tasks: manual add (only hospital-bag preset exercised), recurring tasks, tags.
- Calendar: task/date markers on specific days, conflict indicators.
- Settings: biometric-unlock toggle, screenshot-block toggle (its effect inferred
  from `screencap` returning empty/black earlier in this session, not from toggling it
  on-device), auto-lock timing, and "Lock now" → biometric re-entry flow.
- Sign-out's known gap (Room DB not wiped on sign-out, documented under Phase 7) — not
  re-verified this session.
- True multi-partner conflict (both devices editing same record while offline, then
  syncing) — `SyncConflictEntity`/conflict UI exist from Phase 8 but weren't exercised with
  two real devices this session.

**Continued this session on MIUI phone only** (single-device pass, via `adb`/
`uiautomator`, after committing three bug fixes above and reinstalling):
- All three bug fixes confirmed live: FAB actions now expose real `content-desc`
  ("Scan document"/"Import document"/"New folder") in accessibility tree, no more
  `NAF="true"`. Folder create/subfolder create/document import via SAF all typed correctly
  through `adb shell input text` with no scrambling (didn't retest invite-code field
  itself this pass — that needs two-device pairing flow, not exercised this session).
- Folders/Documents fully exercised end-to-end for first time: created folder, nested
  subfolder inside it, imported real `.txt` file via SAF into parent folder, opened
  it (text preview renders actual file content correctly), then deleted parent
  folder and confirmed cascading delete removed subfolder and document with it —
  back to empty state. All of Phase 4's "explicitly not yet tested" folders item now
  covered except upload of non-text MIME types (image/PDF/CSV/JSON preview) and cycle/task
  attachment, still unverified.
- ML Kit document scanner (Phase 5) launches cleanly from FAB — `GmsDocumentScanningDelegateActivity`
  starts, hands off to real Play Services scanning UI, no crash on launch or on
  cancel-back. Play Services module already downloaded from prior session. Actual
  page capture/crop wasn't exercised (needs real camera pointed at real document, not
  something `adb` can drive meaningfully) — launch-path only.
- Calendar tab renders correctly (month grid, "Task due"/"Date"/"Period" legend) — first
  live confirmation of this screen.
- Dates add-form exercised through Material3 `DatePicker` (title entry, date selection
  all worked correctly) but flow wasn't taken all way to saved row this pass (sheet
  dismissed via back rather than tapping Save) — still needs one more pass to
  confirm actual save.
- Dates add-form retested to completion: title, date picker, Save all worked, saved row
  ("Ultrasound" / 2026-08-16) confirmed on list. Dates item closed out.
- **Real bug found + fixed live: `TaskForm`'s bottom sheet had no scroll, Save unreachable
  once Repeat + Tags (Phase 8 additions) pushed content past sheet height.** `TaskForm`'s
  root `Column` (`feature/tasks/TasksScreen.kt`) had `.fillMaxWidth().imePadding()
  .padding(24.dp)`, no scroll modifier — fine when form shorter, but due date set (reveals
  Repeat row) + tag chip added clipped Save's bounds to sliver at sheet edge, no swipe could
  reach it. Reproduced live via full add-task flow (title → due date → Weekly repeat →
  `#medical` tag), confirmed via source (no `verticalScroll`), fixed with
  `.verticalScroll(rememberScrollState())` on Column. Rebuilt, reinstalled, redid flow live:
  Save reachable after one scroll, saves correctly, reopening for edit confirms due date
  (2026-08-23), Weekly recurrence, `#medical` tag all persisted. `./gradlew
  :app:assembleDebug test lint` green after fix. Tasks add/recurring/tags item closed.
  Where: `feature/tasks/TasksScreen.kt`'s `TaskForm`.
- **False alarm, resolved:** tag-entry field's earlier `NAF="true"` (flagged as accessibility
  gap) retested after scroll fix, gone — same clipped-sheet symptom as Save-button bug, not
  separate defect. Field fully in view (188px vs earlier 48px sliver) carries no `NAF`. No
  fix needed.
- Settings toggles verified live (MIUI): biometric-unlock turns on "Auto-lock after 5
  minutes" row (only shown once biometric enabled, matches Phase 7 gating); "Lock now"
  drives real `BiometricPrompt` ("Touch the fingerprint sensor"/"USE PIN"), completing it
  re-unlocks session. "Block screenshots" confirmed via `adb exec-out screencap`: empty
  output while on, real 179KB PNG the moment toggled off, empty again once back on —
  `FLAG_SECURE` applies live, no restart needed. All three toggles read `NAF="true"` in
  accessibility tree (no label reaches TalkBack) — flagged for Phase 12, not fixed this pass.
- **Real bug found live with two devices (Pixel + MIUI), fixed: a detected sync conflict was
  destroyed by the very sync cycle that detected it, before user ever saw it.** Reproduced:
  edited same task differently on both devices while both offline (`svc wifi disable` +
  `svc data disable`, since `AIRPLANE_MODE` broadcast needs root, denied), brought both back
  online. Expected: conflict banner both sides. Actual (pre-fix): no banner, both devices
  silently converged on whichever push landed first — other device's edit vanished with no
  trace, exactly what `SyncEngine`'s own doc comment says must never happen ("silently
  picking last-write-wins is how shared edits get lost"). Root cause: `RoomSyncStore.markConflict`
  calls `operations.removeByRecord(recordId)` after saving `SyncConflictEntity` (so replay
  of same stale write doesn't just conflict again) — but `applyRemote`'s only guard against
  overwriting a record with unsent local edits was `operations.hasPending`. Since `push()`
  + `pull()` run back-to-back inside same `sync()` call, pull right after the
  conflict-producing push had its guard already removed, immediately upserted server's copy
  over local row — clobbering "mine" side conflict UI needs before resolution ever ran, and
  (upsert also resets sync status to SYNCED) cleared conflict count back to zero same
  transaction cycle, so `ConflictHost` never rendered. First surfaced live: an actual stale
  `SyncConflictEntity` from pre-fix run showed "This device" and "Partner's device" both
  displaying *identical* (already-clobbered) text — visible proof, not theoretical. Fixed:
  second guard on `applyRemote`, skip record if `state.conflict(record.id) != null`, added
  to existing `hasPending` check (`SyncStateDao.conflict(recordId)` already existed for
  this). Rebuilt, reinstalled both devices, redid identical two-device offline-edit
  scenario: conflict banner now shows correctly distinct sides ("This device: Pack hospital
  bag -MIUI 3" vs "Partner's device: Pack hospital bag -MIUI 2"), resolving via "Keep this
  device's version" converges both devices to kept text on next sync — verified by
  relaunching both apps, confirming title matches on each. Was the one Phase 8 conflicts
  item flagged "never exercised even indirectly" — now exercised, found broken, fixed.
  `./gradlew :app:assembleDebug test lint` green after fix. Where:
  `core/database/sync/RoomSyncStore.kt`'s `applyRemote`.
- **Pull-to-refresh rollout started for other tabs** — Home had it since earlier this
  session; Tasks is first of rest to get it (`TasksUiState.refreshing`,
  `TasksActions.onRefresh`, `TasksViewModel` now takes `SyncEngine`, calls `.sync()` same
  way `HomeViewModel` does, `TasksScreen` wraps content in `PullToRefreshBox`, `AppModule`
  passes `syncEngine = get()`). Compiles, full `./gradlew test`/`lint` green, installed +
  launched no crash. **Shopping, Dates, Documents, Cycle, Calendar still don't have it** —
  stopped here on explicit instruction to wrap up session rather than leave five more
  modules half-wired; same copy-pasteable pattern as Tasks/Home, next session repeats it
  across remaining five.
- **Pull-to-refresh rollout finished — Shopping, Dates, Documents (Folders), Cycle,
  Calendar all now have it, same pattern as Tasks/Home**: each `UiState` gained
  `refreshing: Boolean = false`, each `Actions` interface gained `onRefresh()`, each
  `ViewModel` now takes `SyncEngine` and calls `.sync()` guarded by the same
  `if (refreshing) return` re-entrancy check, each `Screen` wraps its content in
  `PullToRefreshBox`, `AppModule` passes `syncEngine = get()` at all five call sites.
  Two screens (Cycle, Calendar) don't use `Scaffold`/`LazyColumn` at top level like the
  rest — `CycleScreen` wraps its `LazyColumn` inside `Surface`, `CalendarScreen` wraps a
  plain non-scrolling `Column` inside `Surface` — `PullToRefreshBox` inserted around each
  screen's existing root content regardless of that shape difference, only the previews'
  `Noop*Actions` objects needed the same one-line `onRefresh() = Unit` addition all six
  screens share. `./gradlew :app:compileDebugKotlin` and full `./gradlew test lint` both
  green. Installed on connected MIUI device via `adb install -r`, launched via `monkey`,
  confirmed resumed and awake with no `FATAL`/`AndroidRuntime` exceptions in `logcat` —
  **launch-only smoke check**; actually pulling down on each of the five newly-wired
  screens to confirm the spinner appears and a real sync round-trips wasn't done this
  pass (no second device paired in this session to produce anything worth pulling for),
  so still belongs on Phase 11's live-testing backlog, narrower than before: five taps
  to confirm, not five features to wire.

## Phase 12 — UI/UX polish pass (queued, not started)

`UIUX.md` (repo root) is generic senior-Android-designer audit brief — not project-specific
findings, a prompt to follow: audit every screen for spacing/typography/hierarchy/nav/
accessibility/dark-mode/states against Material Design conventions, prioritize P0–P3, fix
P0/P1 first, then report before/after per change. Scope explicitly says *review and improve*,
not redesign — "do not blindly redesign everything," reuse existing components/tokens, don't
change functionality unless usability clearly demands it. Full checklist lives in `UIUX.md`
itself; not duplicated here. Not started this session — flagged as next major phase after
Phase 11's live-testing backlog closes out (Documents non-text preview, Tasks manual
add/recurring/tags, Settings toggles, sign-out DB-wipe gap, two-device conflict resolution).

**Test-environment notes, not app issues:** MIUI device's `uiautomator dump` throws
`ThemeCompatibilityLoader` `FileNotFoundException` on every invocation (known Xiaomi/MIUI
quirk unrelated to this app) but still writes dump file correctly afterward: safe to
ignore exception text and read output file. `screencap`/`exec-out screencap` return
empty output on both devices whenever in-app "block screenshots" setting active
(`FLAG_SECURE`) — expected, not bug, but means visual screenshots aren't available for
verification while that setting on; `uiautomator dump`'s text-only tree still works.

## Deferred (P2)
OCR (Hebrew script unsupported by ML Kit; cloud OCR ruled out by encryption
design), widgets, pregnancy mode, advanced analytics.