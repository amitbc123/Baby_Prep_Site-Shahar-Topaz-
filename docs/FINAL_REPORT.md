# Final Report — Android conversion of אור ירח

Status: 2026-08-15. Follows template `docs/specs/01-android-conversion.md` §85. Per spec's own instruction: **report not claim app secure or working end-to-end without on-device verification** — see "Known Limitations" below. Everything compiles, pass unit tests, lint clean; almost none run on real device or emulator this session. Distinction most important thing in report.

## Existing Website

Vite + React 19 PWA (`src/`), Hebrew/RTL, store everything in `localStorage`, no backend, no auth, no network code. **Retired** this session — kept in repo for reference, source format for one-time JSON import into Android app. See `README.md`.

## Android Architecture

Multi-module, feature-vertical: `:core:*` (shared infrastructure, zero UI) and `:feature:*` (one screen each), wired together only in `:app` — feature modules never depend each other. Compose + Material 3, MVVM with `StateFlow` UiState + `Channel`-backed effects split. Room (SQLCipher-encrypted) only thing UI reads from; background `SyncEngine` keeps sync with Supabase. Koin for DI. Full rationale: `docs/architecture/001-android-architecture.md`.

26 Gradle modules, 186 Kotlin source files (excluding build output), 103 passing JVM unit tests across `:core:crypto`, `:core:domain`, `:core:security`. Room schema version 11, migration every version step, backfill data where existing rows affected (search, most notably).

## New Features

Everything past original web app's shopping list / tasks / dates / countdown: folders and documents (scanning, image previews), menstrual cycle tracking (calendar, predictions, statistics, symptom/flow/mood logging), search, unified calendar, sync conflict resolution, recurring tasks, tags, biometric lock and auto-lock, local reminder notifications, in-app update mechanism independent of Google Play.

## Task System

Category, priority, assignee, due date, recurrence (daily/weekly/monthly, arbitrary interval — though UI currently only offers interval 1), free-form tags, document attachments. Completing recurring task schedules next occurrence in same transaction as completing current one. Hospital-bag preset seeds fixed checklist, additively and idempotently.

## Document System

Folders nested arbitrary depth via materialized path — subtree query `LIKE` on one indexed column, not recursive. Documents filed in folder, attached to task, attached to cycle, or any combination, independently. Upload encrypts client-side before network sees bytes; small on-device-generated JPEG thumbnail for image documents rides along in same small metadata record already synced as encrypted JSON — neither device fetch/decrypt full original just to render folder listing — no cheaper way to get "just thumbnail" of end-to-end encrypted file. Scanning uses Google Play services' on-device document scanner (crop, enhance, multi-page merged to one PDF) — never cloud OCR/vision API. Preview supports text, JSON, CSV, images, PDF (first page, rendered via `PdfRenderer`).

## Menstrual Cycle System

Logged periods (start/end) separate from daily detail (flow, symptoms, mood, pain, note) — most logged days not period days at all. Predictions and statistics (`:core:domain`, pure Kotlin, unit-tested) computed from history at read time, never stored — can't go stale independent of source data. Calendar month grid marks actual, predicted, fertile-window days by shape as well as color (filled circle / tinted circle / dot / square), for users who can't rely on color alone. Needs at least two logged periods before predicting anything — no default 28-day guess presented as fact.

## Couple / Sharing Architecture

One shared workspace, one shared encryption key, per couple. No per-item visibility flag, no partner-vs-partner privacy boundary — see "Privacy & Security" below for why, and `docs/architecture/005-data-privacy.md` for full decision record (supersedes original spec's partner-vs-partner privacy sections, per explicit correction from user during project). New device joins via one-time invitation code; inviting device seals workspace key to joining device's public key, server relays opaque blob it cannot open. Device management (viewing pending join requests, approving them) reachable from Settings.

## Privacy & Security

**Threat model: couple versus outside world, not one partner versus other.** Both users see everything in workspace; `owner_id` attribution only, never authorization input. Deliberate, user-directed correction of original spec, which proposed per-item visibility and cycle-sharing permissions — mechanism solving problem ("hide things from my partner") app never actually asked to solve, and would've ruled out genuine end-to-end encryption (server evaluating per-item sharing rules must be able to read items).

Content end-to-end encrypted: ChaCha20-Poly1305 with fresh HKDF-derived key per write (Bouncy Castle, chosen over libsodium specifically so `:core:crypto` stays plain JVM module with fast, non-Android-dependent unit tests). Workspace key 32-byte secret only ever exists on-device, wrapped to device's X25519 public key via HPKE when handed to new device, never sent to server unwrapped. Recovery: 24-word BIP-39 mnemonic — phrase *is* key, encoded, not passphrase unlocking stored copy — checksummed so mistyped phrase fails loudly. What *can* be honestly claimed: content end-to-end encrypted between devices. What cannot: account metadata, timestamps, record counts visible to server, not claimed hidden.

Additional device-local protections added this session: biometric/device-credential lock with configurable auto-lock timeout (deliberately inert unless biometric unlock enabled — app has no separate password, lock nothing can reopen would just strand user), opt-in `FLAG_SECURE` screenshot block, defaulting on.

**Known, explicitly unaddressed:** key rotation (regenerating workspace key) not implemented. `WorkspaceRepository` has no revoke/deregister-a-device-key endpoint at all — currently no way to cryptographically cut off lost/compromised device beyond sign-out abandoning it locally — rotation properly needs backend support that doesn't exist yet, building without it would produce something looking like security feature without being one. See Phase 7 in `PROGRESS.md` for detailed reasoning. Sign-out also doesn't wipe local Room database (SQLCipher passphrase device-Keystore-bound, independent of workspace membership) — previously-synced content stays readable by app on that device after sign-out.

## Offline / Sync Architecture

Every write lands in Room and outbox table in one transaction, then kicks WorkManager worker — UI never talks to network directly, write survives being offline. Sync push-then-pull with per-workspace cursor. Push server rejects because record changed underneath it (someone else's write landed first) becomes conflict row, resolved only by person choosing side (`ConflictRepository` — see below) — nothing here silently overwritten.

## Database

SQLCipher-encrypted Room, version 11. Nine syncable entity types, each indexed on `sync_status` (outbox push query), `workspace_id + updated_at` (pull cursor / list ordering), query-shaped composite index wherever screen filters on something else. Search hand-maintained FTS4 index (`search_index`) spanning all seven searchable entity types, kept in sync by every repository's write path and sync engine's pull path alike — not FTS5, despite `docs/architecture/007-encryption.md` originally naming it: Room 2.8.4 (pinned version) has no `@Fts5` annotation at all, confirmed directly against library jar, ADR corrected to match rather than left describing something Room cannot do.

## Files Added

Not enumerated file-by-file here — `git diff --stat` against branch point authoritative list, won't drift out of sync like static list here would. Broad strokes: 12 new Gradle modules this session alone (`:core:scanner`, `:core:settings`, `:feature:settings`, `:feature:search`, `:feature:calendar`, `:feature:conflicts`, and Phase 1–6 modules before them), five new Supabase migrations, eleven Room schema versions.

## Tests

103 passing JVM unit tests (`:core:crypto` — record encryption, key wrapping, recovery phrase against official BIP-39 vectors; `:core:domain` — pregnancy math, budget, daily message, web-import mapping, cycle prediction/statistics, recurrence date advance; `:core:security` — invitation tokens). No instrumented/on-device tests exist — deliberately: every UI-layer feature this session verified by compiling, running full JVM test suite, running lint, assembling debug APK, but not by actually running on device or emulator (none available in this environment). See "Known Limitations."

## Build Result

`./gradlew test`, `./gradlew lint`, `./gradlew :app:assembleDebug` all succeed as of report. `./gradlew :app:assembleRelease` also succeeds — R8 minification, resource shrinking, proguard rules all run cleanly — but produces **unsigned** APK: no `ANDROID_KEYSTORE_*` secrets exist in this environment, per `docs/architecture/011-release-signing-and-updates.md` that's intended behavior (never silently fall back to debug signing) not defect. Real signed release requires tagging `v*`, letting `.github/workflows/android-release.yml` run with real secrets.

## Known Limitations

- **Nothing this session run on real device or emulator.** Single largest gap between "compiles and unit-tested" and "works." Every feature built from Phase 4 onward (folders, documents, scanning, cycle tracking, security hardening, search, calendar, conflict resolution, recurring tasks, tags) needs live pass before called done-done. Sync conflict-resolution path in particular never exercised even indirectly — nothing else in app deliberately produces conflict to test against.
- Key rotation not implemented (needs backend support that doesn't exist yet — see "Privacy & Security" above).
- Sign-out doesn't wipe local decrypted database cache.
- Five screens (Tasks, Shopping, Dates, Folders, Home) have no in-page heading at all, relying solely on bottom-nav tab label for context — real accessibility gap for screen-reader user landing directly on content.
- Bottom navigation grown to 9 tabs across project's phases, past typical M3 guidance; real UX pass (consolidation, overflow menu, compact tabs) warranted but wasn't attempted mid-feature.
- Cycle and Calendar month grids' Sunday-first week assumption a guess — app has no existing first-day-of-week preference to follow — hasn't been checked against RTL layout on real screen.
- Paging (Paging 3) deliberately not built: app's realistic data volumes (one couple's tasks, shopping items, documents) don't need it, adding it would be complexity without problem to solve.

## How to Run

```bash
cd android
./gradlew installDebug   # onto a connected device/emulator
```
or open `android/` in Android Studio (AGP 9, JDK 21), run `app` configuration.

## How to Build APK

```bash
cd android
./gradlew :app:assembleDebug     # unsigned-by-design debug APK, always installable
./gradlew :app:assembleRelease   # needs ANDROID_KEYSTORE_* env vars for a signed, installable APK
```
Real release produced by tagging: `git tag v1.x.y && git push origin v1.x.y`, which `.github/workflows/android-release.yml` picks up.

---

## How authentication and pairing actually work

**How User A authenticates.** Supabase email/password auth (`AuthRepository`), session persisted via encrypted `SessionManager` (Keystore-backed).

**How User B authenticates.** Same way — no asymmetry between two users; "User A" and "User B" not roles, just whoever created workspace versus whoever joined it.

**How couple accounts paired.** Creating device generates workspace key locally, shows recovery phrase once, registers own device public key. Joining device accepts one-time invitation code (proving membership), registers own device public key; waits (`PairingStage.AwaitingKey`) until first device seals workspace key to joiner's public key via HPKE, uploads sealed blob. Neither device ever sends unwrapped key over network.

**How private data stored.** No "private" data in app's model — see "Privacy & Security" above. All workspace content shared between two members by design.

**How shared data synchronized.** Every local write goes to outbox table in same transaction as write itself; WorkManager worker pushes queued operations, pulls new records past per-workspace cursor. UI reads only from Room, never network directly.

**How menstrual cycle data protected.** Same way as everything else in workspace — encrypted client-side, synced as ciphertext, readable by both members. No cycle-specific sharing restriction (deliberate departure from original spec, which proposed one).

**How cycle sharing works.** Doesn't need separate mechanism — it's workspace content like any other entity type.

**How private documents protected.** Same encryption path as records: encrypted client-side before upload, stored in Supabase Storage at path keyed by workspace id, RLS on `storage.objects` restricting access to workspace members, integrity-checked (SHA-256 of ciphertext) before decryption on download.

**How offline changes synchronize.** Don't wait for connectivity to be written locally; Room + outbox make every write durable immediately. Sync catches up whenever network available, via both on-write trigger and periodic 6-hour safety-net worker.

**How conflicts resolved.** Never automatically. Rejected push parked as `SyncConflictEntity` with server's ciphertext attached; `ConflictRepository` decodes both local and server versions for display, offers exactly two actions — keep this device's version (re-queued for push, rebased onto server's version number) or keep partner's version (applied locally, marked synced). Conflict UI hosted at app root, not behind tab, so stuck conflict surfaced proactively.

**How account/device recovery works.** Two paths. If device still registered workspace member but lost local key (e.g., reinstalled app), waits for partner's device to approve it via device management, same as brand-new device joining. If that's not available or not wanted, 24-word recovery phrase decodes directly to workspace key (BIP-39-checksummed, so wrong phrase fails loudly rather than silently producing garbage) — only path back in if no other device left to ask, only way back in at all if every device lost.