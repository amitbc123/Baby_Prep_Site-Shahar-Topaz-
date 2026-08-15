# Final Report — Android conversion of אור ירח

Status as of 2026-08-15. This report follows the template in
`docs/specs/01-android-conversion.md` §85. Per that spec's own instruction: **this report
does not claim the application is secure or working end-to-end without on-device
verification** — see "Known Limitations" below. Everything here compiles, passes its unit
tests, and lints clean; almost none of it has been run on a real device or emulator this
session. That distinction is the single most important thing in this report.

## Existing Website

A Vite + React 19 PWA (`src/`), Hebrew/RTL, storing everything in `localStorage` with no
backend, no auth, no network code. **Retired** as of this session — kept in the repo for
reference and as the source format for the one-time JSON import path into the Android app.
See `README.md`.

## Android Architecture

Multi-module, feature-vertical: `:core:*` (shared infrastructure, zero UI) and `:feature:*`
(one screen each), wired together only in `:app` — feature modules must never depend on
each other. Compose + Material 3, MVVM with a `StateFlow` UiState + `Channel`-backed effects
split. Room (SQLCipher-encrypted) is the only thing the UI reads from; a background
`SyncEngine` keeps it in sync with Supabase. Koin for DI. Full rationale:
`docs/architecture/001-android-architecture.md`.

26 Gradle modules, 186 Kotlin source files (excluding build output), 103 passing JVM unit
tests across `:core:crypto`, `:core:domain`, and `:core:security`. Room schema is at version
11, with a migration for every version step, backfilling data where existing rows are
affected (search, most notably).

## New Features

Everything past the original web app's shopping list / tasks / dates / countdown:
folders and documents (with scanning and image previews), menstrual cycle tracking
(calendar, predictions, statistics, symptom/flow/mood logging), search, a unified calendar,
sync conflict resolution, recurring tasks, tags, biometric lock and auto-lock, local
reminder notifications, and an in-app update mechanism independent of Google Play.

## Task System

Category, priority, assignee, due date, recurrence (daily/weekly/monthly, arbitrary
interval — though the UI currently only offers interval 1), free-form tags, document
attachments. Completing a recurring task schedules its next occurrence in the same
transaction as completing the current one. The hospital-bag preset seeds a fixed checklist,
additively and idempotently.

## Document System

Folders are nested to arbitrary depth via a materialized path, so a subtree query is a
`LIKE` on one indexed column rather than a recursive one. Documents can be filed in a
folder, attached to a task, attached to a cycle, or any combination, independently. Upload
encrypts client-side before the network ever sees the bytes; a small on-device-generated
JPEG thumbnail for image documents rides along in the same small metadata record that
already syncs as encrypted JSON, so neither device has to fetch and decrypt a full original
just to render a folder listing — there is no cheaper way to get "just a thumbnail" of an
end-to-end encrypted file. Scanning uses Google Play services' on-device document scanner
(crop, enhance, multi-page merged to one PDF) — never a cloud OCR/vision API. Preview
supports text, JSON, CSV, images, and PDF (first page, rendered via `PdfRenderer`).

## Menstrual Cycle System

Logged periods (start/end) are separate from daily detail (flow, symptoms, mood, pain,
note) — most logged days are not period days at all. Predictions and statistics
(`:core:domain`, pure Kotlin, unit-tested) are computed from history at read time, never
stored, so they can never go stale independently of the data they're derived from. A
calendar month grid marks actual, predicted, and fertile-window days by shape as well as
color (filled circle / tinted circle / dot / square), for users who can't rely on color
alone. Needs at least two logged periods before it will predict anything — no default
28-day guess presented as fact.

## Couple / Sharing Architecture

One shared workspace, one shared encryption key, per couple. There is no per-item
visibility flag, no partner-vs-partner privacy boundary — see "Privacy & Security" below for
why, and `docs/architecture/005-data-privacy.md` for the full decision record (this
supersedes the original spec's partner-vs-partner privacy sections, per an explicit
correction from the user during this project). A new device joins via a one-time invitation
code; the inviting device seals the workspace key to the joining device's public key and
the server relays an opaque blob it cannot open. Device management (viewing pending
join requests, approving them) is reachable from Settings.

## Privacy & Security

**The threat model is the couple versus the outside world, not one partner versus the
other.** Both users see everything in the workspace; `owner_id` is attribution only, never
an authorization input. This was a deliberate, user-directed correction of the original
spec, which proposed per-item visibility and cycle-sharing permissions — a mechanism that
solves a problem ("hide things from my partner") this app was never actually asked to solve,
and that would have ruled out genuine end-to-end encryption (a server that evaluates
per-item sharing rules has to be able to read the items).

Content is end-to-end encrypted: ChaCha20-Poly1305 with a fresh HKDF-derived key per write
(Bouncy Castle, chosen over libsodium specifically so `:core:crypto` stays a plain JVM
module with fast, non-Android-dependent unit tests). The workspace key is a 32-byte secret
that only ever exists on-device, wrapped to a device's X25519 public key via HPKE when
handed to a new device, and never sent to the server unwrapped. Recovery is a 24-word
BIP-39 mnemonic — the phrase *is* the key, encoded, not a passphrase unlocking a stored
copy — checksummed so a mistyped phrase fails loudly. What *can* be honestly claimed:
content is end-to-end encrypted between devices. What cannot: account metadata, timestamps,
and record counts are visible to the server and are not claimed to be hidden.

Additional device-local protections added this session: biometric/device-credential lock
with a configurable auto-lock timeout (deliberately inert unless biometric unlock is
enabled — this app has no separate password, so a lock nothing can reopen would just strand
the user), and an opt-in `FLAG_SECURE` screenshot block, defaulting on.

**Known, explicitly unaddressed:** key rotation (regenerating the workspace key) is not
implemented. `WorkspaceRepository` has no revoke/deregister-a-device-key endpoint at all, so
there is currently no way to cryptographically cut off a lost or compromised device beyond
sign-out abandoning it locally — building rotation properly needs backend support that
doesn't exist yet, and building it without that support would produce something that looks
like a security feature without being one. See Phase 7 in `PROGRESS.md` for the detailed
reasoning. Sign-out also does not wipe the local Room database (its SQLCipher passphrase is
device-Keystore-bound, independent of workspace membership) — previously-synced content
stays readable by the app on that device after signing out.

## Offline / Sync Architecture

Every write lands in Room and an outbox table in one transaction, then kicks a WorkManager
worker — the UI never talks to the network directly, and a write survives being offline.
Sync is push-then-pull with a per-workspace cursor. A push that the server rejects because
the record changed underneath it (someone else's write landed first) becomes a conflict row,
resolved only by a person choosing a side (`ConflictRepository` — see below) — nothing here
is ever silently overwritten.

## Database

SQLCipher-encrypted Room, version 11. Nine syncable entity types, each indexed on
`sync_status` (the outbox push query), `workspace_id + updated_at` (pull cursor / list
ordering), and a query-shaped composite index wherever a screen filters on something else.
Search is a hand-maintained FTS4 index (`search_index`) spanning all seven searchable entity
types, kept in sync by every repository's write path and by the sync engine's pull path
alike — not FTS5, despite `docs/architecture/007-encryption.md` originally naming it: Room
2.8.4 (the pinned version) has no `@Fts5` annotation at all, confirmed directly against the
library jar, and the ADR was corrected to match rather than left describing something Room
cannot do.

## Files Added

Not enumerated file-by-file here — `git diff --stat` against the branch point is the
authoritative list and won't drift out of sync the way a static list in this report would.
In broad strokes: 12 new Gradle modules this session alone (`:core:scanner`,
`:core:settings`, `:feature:settings`, `:feature:search`, `:feature:calendar`,
`:feature:conflicts`, and the Phase 1–6 modules before them), five new Supabase migrations,
eleven Room schema versions.

## Tests

103 passing JVM unit tests (`:core:crypto` — record encryption, key wrapping, recovery
phrase against official BIP-39 vectors; `:core:domain` — pregnancy math, budget, daily
message, web-import mapping, cycle prediction/statistics, recurrence date advance;
`:core:security` — invitation tokens). No instrumented/on-device tests exist — deliberately:
every UI-layer feature this session was verified by compiling, running the full JVM test
suite, running lint, and assembling a debug APK, but not by actually running on a device or
emulator (none was available in this environment). See "Known Limitations."

## Build Result

`./gradlew test`, `./gradlew lint`, and `./gradlew :app:assembleDebug` all succeed as of
this report. `./gradlew :app:assembleRelease` also succeeds — R8 minification, resource
shrinking, and proguard rules all run cleanly — but produces an **unsigned** APK: no
`ANDROID_KEYSTORE_*` secrets exist in this environment, and per
`docs/architecture/011-release-signing-and-updates.md` that is the intended behavior (never
silently fall back to debug signing) rather than a defect. A real signed release requires
tagging `v*` and letting `.github/workflows/android-release.yml` run with the real secrets.

## Known Limitations

- **Nothing in this session has been run on a real device or emulator.** This is the
  single largest gap between "compiles and is unit-tested" and "works." Every feature built
  from Phase 4 onward (folders, documents, scanning, cycle tracking, security hardening,
  search, calendar, conflict resolution, recurring tasks, tags) needs a live pass before it
  can be called done-done. The sync conflict-resolution path in particular has never been
  exercised even indirectly — nothing else in the app deliberately produces a conflict to
  test against.
- Key rotation is not implemented (needs backend support that doesn't exist yet — see
  "Privacy & Security" above).
- Sign-out does not wipe the local decrypted database cache.
- Five screens (Tasks, Shopping, Dates, Folders, Home) have no in-page heading at all,
  relying solely on the bottom-nav tab label for context — a real accessibility gap for a
  screen-reader user landing directly on the content.
- Bottom navigation has grown to 9 tabs across this project's phases, past typical M3
  guidance; a real UX pass (consolidation, overflow menu, or compact tabs) is warranted but
  wasn't attempted mid-feature.
- The Cycle and Calendar month grids' Sunday-first week assumption is a guess — this app has
  no existing first-day-of-week preference to follow — and hasn't been checked against RTL
  layout on a real screen.
- Paging (Paging 3) was deliberately not built: this app's realistic data volumes (one
  couple's tasks, shopping items, documents) don't need it, and adding it would be
  complexity without a problem to solve.

## How to Run

```bash
cd android
./gradlew installDebug   # onto a connected device/emulator
```
or open `android/` in Android Studio (AGP 9, JDK 21) and run the `app` configuration.

## How to Build APK

```bash
cd android
./gradlew :app:assembleDebug     # unsigned-by-design debug APK, always installable
./gradlew :app:assembleRelease   # needs ANDROID_KEYSTORE_* env vars for a signed, installable APK
```
A real release is produced by tagging: `git tag v1.x.y && git push origin v1.x.y`, which
`.github/workflows/android-release.yml` picks up.

---

## How authentication and pairing actually work

**How User A authenticates.** Supabase email/password auth (`AuthRepository`), session
persisted via an encrypted `SessionManager` (Keystore-backed).

**How User B authenticates.** The same way — there is no asymmetry between the two users;
"User A" and "User B" are not roles, just whoever created the workspace versus whoever
joined it.

**How the couple accounts are paired.** The creating device generates the workspace key
locally, shows the recovery phrase once, and registers its own device public key. The
joining device accepts a one-time invitation code (proving membership) and registers its
own device public key; it then waits (`PairingStage.AwaitingKey`) until the first device
seals the workspace key to the joiner's public key via HPKE and uploads that sealed blob.
Neither device ever sends the unwrapped key over the network.

**How private data is stored.** There is no "private" data in this app's model — see
"Privacy & Security" above. All workspace content is shared between the two members by
design.

**How shared data is synchronized.** Every local write goes to an outbox table in the same
transaction as the write itself; a WorkManager worker pushes queued operations and pulls new
records past a per-workspace cursor. The UI reads only from Room, never from the network
directly.

**How menstrual cycle data is protected.** The same way as everything else in the
workspace — encrypted client-side, synced as ciphertext, readable by both members. There is
no cycle-specific sharing restriction (a deliberate departure from the original spec, which
proposed one).

**How cycle sharing works.** It doesn't need a separate mechanism — it's workspace content
like any other entity type.

**How private documents are protected.** Same encryption path as records: encrypted
client-side before upload, stored in Supabase Storage at a path keyed by workspace id, RLS
on `storage.objects` restricting access to workspace members, integrity-checked (SHA-256 of
the ciphertext) before decryption on download.

**How offline changes synchronize.** They don't wait for connectivity to be written locally;
Room + the outbox make every write durable immediately. Sync catches up whenever the network
is available, via both an on-write trigger and a periodic 6-hour safety-net worker.

**How conflicts are resolved.** Never automatically. A rejected push is parked as a
`SyncConflictEntity` with the server's ciphertext attached; `ConflictRepository` decodes both
the local and server versions for display and offers exactly two actions — keep this
device's version (re-queued for push, rebased onto the server's version number) or keep the
partner's version (applied locally, marked synced). The conflict UI is hosted at the app
root, not behind a tab, so a stuck conflict is surfaced proactively.

**How account/device recovery works.** Two paths. If this device is still a registered
workspace member but lost its local key (e.g., reinstalled the app), it waits for the
partner's device to approve it via device management, same as a brand-new device joining.
If that's not available or not wanted, the 24-word recovery phrase decodes directly to the
workspace key (BIP-39-checksummed, so a wrong phrase fails loudly rather than silently
producing garbage) — the only path back in if there is no other device left to ask, and the
only way back in at all if every device is lost.
