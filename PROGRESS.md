# Progress

Android conversion of אור ירח ("One More Moon") into private couple organizer for Shahar &
Topaz. Branch: `feature/android-app`. Plan: see `docs/architecture/`.

**Threat model:** privacy boundary = couple vs outside world, not partner vs partner. Both
users share all data; end-to-end encrypted so Supabase, an attacker, or a lost phone learns
nothing. See `docs/architecture/005-data-privacy.md` for the full decision record.

This file was compacted 2026-08-16 — full phase-by-phase history (every bug found, every
live-test transcript) is in git history (`git log -- PROGRESS.md`) if ever needed. What
follows is state, not narrative.

## Done (Phases 0–13)

- **Phase 0–2**: Gradle/Koin/Compose skeleton, `:core:crypto` (record encryption, key
  wrapping, BIP-39 recovery phrase), Supabase schema + RLS, email/password auth, Keystore
  session, Room+SQLCipher, sync engine (push/pull/conflict detection), in-app updater
  (download, checksum verify, mandatory-update support).
- **Phase 3**: Tasks, Shopping+budget, Important Dates, Home dashboard (moon countdown,
  weekly info, daily message), web-app JSON import (`:core:domain`'s `WebSnapshot`).
- **Phase 4**: Folders (nested, materialized path), documents (SAF import, encrypted upload
  to Supabase Storage, preview for text/image/PDF/JSON/CSV), task/folder/cycle attachments.
- **Phase 5**: `:core:scanner` — ML Kit on-device document scanner (multi-page → PDF),
  wired into Folders and Tasks attachment flows.
- **Phase 6**: Cycle module — predictions (`predictNextCycle`), statistics, day-level entries
  (flow/symptoms/mood/pain/notes), calendar grid (shape/fill distinctions, not color-only).
- **Phase 7**: `:feature:settings` — biometric lock + auto-lock, `FLAG_SECURE` screenshot
  block (default on), local reminder notifications (generic text only, opt-in), device
  management (invite/approve, revocation via `revoke_device_key` RPC), recovery-phrase
  show/enter both directions.
- **Phase 8**: Unified calendar (tasks/dates/cycle merged view), full-text search
  (`:feature:search`), sync conflict detection + resolution UI, accessibility pass
  (headings, content-descriptions).
- **Phase 9**: ADR drift pass — reconciled `docs/architecture/` against what was actually
  built; `README.md` rewritten to reflect Android as the active product.
- **Phase 10**: (folded into 9/11 — see git history if needed.)
- **Phase 11**: First real two-device live testing (Pixel + MIUI). Found and fixed: invite-code
  field input scrambling, Folders FAB accessibility, duplicate device-key labels, `TaskForm`
  sheet not scrolling (Save unreachable), **a real sync-conflict bug where the same sync
  cycle that detected a conflict immediately clobbered it** (`RoomSyncStore.applyRemote`
  needed a second guard checking `state.conflict(record.id)`, not just `hasPending`). Rolled
  out pull-to-refresh to all tabs.
- **Phase 12**: M3 compliance audit — delete confirmations on Tasks/Shopping/Dates/Cycle,
  in-page headings on every screen, Settings toggle accessibility (merged semantics), Search
  empty-state copy, Documents thumbnail corner-radius token fix.
- **Phase 13 — Navigation redesign** (user-requested, `/frontend-design`): bottom
  `NavigationBar` (9 cramped tabs) replaced with a side drawer (`MoonNavigationDrawer` +
  `MoonTopBar`, `:core:ui/nav/`) after a live-screenshotted first attempt (a floating bottom
  pill bar) was rejected for label truncation. Drawer reuses the moon countdown's own
  `NightPalette` — always-dark, independent of light/dark theme, same precedent as the
  countdown card itself. Filled/outlined icon convention, glow-pill behind the active item.
  Fixed live: hamburger icon rendering under the status bar (missing inset), Cycle/Calendar
  icon confusion (Cycle → `WaterDrop`). `headlineMedium` (every screen's page title)
  hand-tuned rather than left at M3 default. Live-verified via actual screenshots (not just
  logcat) on both devices, both RTL and LTR.
- **Sign-out DB-wipe gap fixed and live-verified**: `LocalDataWiper`/`RoomLocalDataWiper`
  (`:core:security`/`:app`) closes Room, deletes `or-yareach.db`, restarts the process.
  Confirmed via `run-as` that the file is actually gone after sign-out, and confirmed
  restore-via-recovery-phrase works end to end.
- **Documents non-text preview (PDF/PNG/JSON/CSV) live-verified**, after finding and fixing
  the actual root cause of an earlier flaky failure: `AuthRepository` mapped Supabase's
  transient `SessionStatus.RefreshFailure` (failed background token refresh — session is
  still valid) to the same `AuthState.Unknown` as genuine cold-start `Initializing`, and
  `SaharApp`'s routing unmounts the whole authenticated UI tree for `Unknown`. A refresh
  blip right as an external Activity (the SAF picker) returned was unmounting `HomeRoute`
  mid-flight, dropping the pending `ActivityResultLauncher` callback. Fixed: `RefreshFailure`
  now maps to `SignedIn` (only `NotAuthenticated` means actually signed out).
- **Dark-mode and motion/microinteraction timing**: reviewed by the user directly on-device,
  no changes needed — closed 2026-08-16.

## Phase 14: per-screen visual pass (reviewed, two real fixes made)

User's ask, following the nav redesign: apply the same design attention to each screen's own
layout (spacing, cards, buttons, hierarchy) now that the nav shell is settled. Read through
`TasksScreen`, `ShoppingScreen`, `DatesScreen`, `FoldersScreen`, `CycleScreen`,
`SettingsScreen` (`HomeScreen` already got attention in Phase 13's typography pass) looking
for real inconsistencies rather than inventing changes with nothing concrete to anchor them
to. Found and fixed two:

- **Tasks' "Add hospital bag checklist" button ran edge-to-edge** while every other
  interactive element on every screen has the same 16dp side margin — a plain
  `Modifier.fillMaxWidth()` with no padding, genuinely inconsistent with the rest of the app,
  not a style preference. Changed to `OutlinedButton` (was a bare `TextButton`, which read as
  lower-emphasis than its role — it's the single most-used quick action on the Tasks screen)
  with the standard 16dp horizontal padding.
- **Shopping's Estimated/Spent/Bought stat row had no card**, just three `Column`s floating
  directly on the screen background — confirmed via live screenshot, inconsistent with every
  other stat/summary block in the app (Home's countdown card, Settings' `SectionCard`s all
  use `Card`). Wrapped in the same `Card`/`CardDefaults.cardColors(containerColor =
  MaterialTheme.colorScheme.surface)` pattern used everywhere else.

Both fixes live-verified on the MIUI device via screenshot (not just launch-and-logcat): the
button now sits inset with visible margin, the stat row now reads as a distinct card matching
the item cards below it, no `FATAL`/`AndroidRuntime` in logcat, no regression in either
screen's data (real synced Tasks/Shopping items rendered correctly in both screenshots).

**Everything else reviewed and left alone, not skipped**: Dates, Folders, Cycle, and
Settings already follow the same `Card`/16dp-margin/`headlineMedium`-title pattern
consistently — Cycle in particular (`OngoingCard`/`PredictionCard`/`StatisticsCard`/
`CalendarCard`, all `Card`-wrapped) was already close to a model example of the pattern the
two fixes above bring Tasks/Shopping in line with. No further per-screen changes made this
pass; `CalendarScreen`/`SearchScreen` not separately reviewed (`Calendar` already got the
`Scaffold` consistency fix in an earlier phase this session, `Search` got its empty-state fix
in Phase 12) — worth a look if the user finds something specific on either, but nothing was
found proactively worth changing blind.

## Known minor gaps (real, low-priority, not blocking)

- Shopping alternatives (per-item price comparison): DB column exists (JSON list), no UI to
  add one.
- Web-import: `Task.dueDate` not carried over from web-app JSON export (dead field since
  Phase 1 — no UI/repository support for it regardless of import).
- Scanner (`:core:scanner`): launch path confirmed live; actual page capture via real camera
  never exercised (needs a physical document + camera, not something `adb` can drive).
- Task/cycle document attachment (as opposed to folder placement): upload path shared with
  folder import, which is live-verified, but the attachment-specific UI wasn't separately
  tap-tested.
- Search/Calendar "open result" only jumps to the owning tab, not the exact row/item
  (documented, deliberate scope limit — no per-screen "scroll to id" support yet).

## Deferred (P2)

OCR (Hebrew script unsupported by ML Kit; cloud OCR ruled out by encryption design), widgets,
pregnancy mode, advanced analytics, foldable/tablet responsive layout (removed from active
backlog per user 2026-08-16 — revisit only if the user gets foldable/tablet hardware).

## Environment notes

- MIUI device: `uiautomator dump` throws a `ThemeCompatibilityLoader` exception every call
  (known Xiaomi quirk) but still writes the dump file correctly — safe to ignore the
  exception text.
- `FLAG_SECURE` (screenshot block, on by default) makes `screencap`/`exec-out screencap`
  return empty output. User can toggle "Block screenshots" off in Settings for a live design
  review session; default should stay on otherwise (the app's whole reason to exist is
  privacy).
- Signing out via `LocalDataWiper` kills and restarts the process — expect `VM exiting with
  result code 0` in logcat, not a crash signature.
