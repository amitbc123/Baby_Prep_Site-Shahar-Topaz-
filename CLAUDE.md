# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

"אור ירח" (One More Moon) — a private couple organizer for Shahar & Topaz, prepping for
their baby's arrival. Hebrew, RTL. Shopping list w/ budget & priorities, tasks (incl.
hospital bag), important dates & wishes, menstrual cycle tracking, folders & documents
(incl. scanning), and a countdown to the due date framed as a moon filling up.

**The active product is the Android app, in `android/`.** It is end-to-end encrypted:
both partners share one workspace and one encryption key; Supabase (the backend) only ever
sees ciphertext. See `docs/architecture/` for the full decision record and `PROGRESS.md`
for what's built vs. planned, phase by phase — read `PROGRESS.md` first when picking up
work here, it is the living plan.

`src/` is the original web PWA (React 19 + Vite, `localStorage`-only, no backend) —
**retired**, kept for reference only. Its export/import JSON format is still the migration
path into the Android app (`:core:domain`'s `WebSnapshot`/`toImportedSnapshot`). Everything
under "Web app (legacy)" below still describes that subtree accurately; skip straight to
"Android app" unless you're specifically touching `src/`.

## Android app

Read `docs/architecture/001-android-architecture.md` first — it's short and covers the
module shape, DI choice (Koin, not Hilt), and UI pattern (Compose, MVVM with a
state/effect split) that everything below assumes.

**Commands** (from `android/`):
```bash
./gradlew :app:assembleDebug   # debug APK
./gradlew test                 # all modules, incl. :core:crypto and :core:domain's pure-logic tests
./gradlew lint                 # Kotlin/Android lint, warnings are errors
```
`assembleRelease` needs the signing secrets (`ANDROID_KEYSTORE_BASE64` etc., see
`docs/architecture/011-release-signing-and-updates.md`); without them it still builds
(R8/shrink/proguard all run) but comes out **unsigned**, by design — not a build failure.

**Module rule that matters most:** `:feature:*` modules must never depend on each other
(enforced by convention, not the compiler — `AndroidFeatureConventionPlugin`'s doc comment
states it, nothing fails the build if it's violated). Anything that needs to be reached
from two different features — device management from Settings, "Check for updates" from
Settings — is wired in `:app`, which is the only module allowed to see every feature.
`:core:*` modules are the shared seam instead: e.g. `SessionController` and
`WorkspaceKeyProvider` in `:core:security`/`:core:sync` let a feature module trigger
lock/read the key without depending on `:app`'s concrete `SessionState`.

**Routing has no `NavHost`.** `SaharApp.kt` derives which screen to show from state
(`AuthState`, whether a workspace key is unlocked, `SessionState.isLocked`) via a plain
`when`, not a back stack — see its doc comment. `HomeRoute` is a bottom-tab switch on a
local enum, not a nav graph. A screen that needs to show a "sub-screen" (e.g. Settings'
device management) does it by flipping a local `rememberSaveable` boolean, not by pushing a
route.

**Persistence.** Room (SQLCipher-encrypted, `:core:database`) is the only thing the UI
reads from — the network is never on the render path. A background `SyncEngine`
(`:core:sync`) keeps it in sync with Supabase; every repository writes locally first, then
enqueues a sync op via `SyncTrigger`. Record content is encrypted client-side
(`:core:crypto`, ChaCha20-Poly1305) before it ever reaches `:core:network` — Supabase RLS is
a second layer, not the only one.

**Domain math stays in `:core:domain`**, pure Kotlin, unit-tested on the JVM (no
emulator): pregnancy progress, budget calculations, cycle predictions/statistics, the
web-import mapper. Don't inline date/domain math into a ViewModel or composable if it
belongs here — see `core/domain/src/main/kotlin/com/oryareach/core/domain/` for the
existing shape (one subpackage per domain area) before adding a new one.

**Adding an entity that syncs** touches, in order: `:core:model` (the data class),
`:core:database` (`Entity`/`Dao`, a `Migrations.kt` bump, `DatabaseConverters` for any new
enum/list field), `Mappers.kt` (entity ↔ domain), `RoomSyncStore` (all four spots — grep
`EntityType.CYCLE_ENTRY` for the most recently added one as a template; the `when` blocks
are exhaustive, so a missed branch is a compile error, not a silent gap), and
`supabase/migrations/` only if the `entity_type` enum doesn't already have a slot for it
(check first — several were declared in `0001_init.sql` a phase ahead of being used).

**Strings are bilingual**, `values/` (English fallback) + `values-iw/` (Hebrew) in every
module with UI. Add both together, never just one.

## Web app (legacy)

### Commands

```bash
npm run dev       # vite dev server
npm run build      # tsc -b && vite build (type-check is part of the build, no separate typecheck script)
npm run lint       # oxlint
npm run preview    # preview production build
npm run test       # vitest run
```

Tests live next to the code they cover (`*.test.ts`), config is `vitest.config.ts` (separate from `vite.config.ts` since the latter isn't built with `vitest/config`'s `defineConfig`). Coverage is limited to the pure-logic files in `src/lib` and `src/features/shopping/budget.ts` — no component/integration tests.

### Architecture

**Persistence — single storage seam.** `src/stores/appStore.ts` is one zustand store (with `persist` middleware) holding all app state: `settings`, `shoppingItems`, `tasks`, `importantDates`. It never touches `localStorage` directly — it goes through `src/lib/storage.ts`'s `createAppStorage()` adapter. If this ever moves to a real backend, only `storage.ts` needs to change. Keep all persisted state in this one store; don't create parallel stores or read/write `localStorage` elsewhere.

**Domain types** live in `src/types/models.ts` — this is the source of truth for shopping/task/date shapes, categories (`SHOPPING_CATEGORIES`, `TASK_CATEGORIES`), and label maps (`PRIORITY_LABEL`, `SHOPPING_STATUS_LABEL`). Categories and enums are Hebrew string literals used directly as data, not just labels — adding a category means editing the `as const` array here.

**Feature-sliced structure**: `src/features/{home,shopping,tasks,dates,settings}` each hold a page plus any form/card components specific to that feature. Cross-feature UI (nav, layout shell) is in `src/components/layout`; the moon countdown is in `src/components/countdown`; generic shadcn/radix primitives are in `src/components/ui` (standard shadcn setup, see `components.json`).

**Pure logic in `src/lib`**: `pregnancy.ts` (due-date math, weekly info, weekly fruit-size comparison, moon fraction), `messages.ts` (daily message picker), `budget.ts` under `features/shopping` (spend calculations), `hospital-bag-preset.ts` (seed data for the hospital-bag task preset). Keep date/domain math here, not inline in components.

**Routing**: `src/app/router.tsx` + `src/app/layout.tsx` (`RootLayout`). Nav items are declared once in `src/components/layout/nav-items.ts` and rendered as both a desktop top pill-nav and a mobile bottom tab bar in `RootLayout`.

**Design tokens**: all color/radius/font tokens are CSS custom properties in `src/index.css` under `:root` / `.dark`, mapped into Tailwind v4 via `@theme inline`. Named tokens beyond shadcn defaults: `moss`, `blush` (plus the standard shadcn set). Headings use `--font-heading` (Assistant Variable), body uses `--font-sans` (Heebo Variable) — both support Hebrew. Add new colors/fonts as CSS vars here, not as one-off Tailwind arbitrary values. The moon-countdown card (`src/components/countdown/moon-countdown.tsx`) hardcodes its own always-dark "night sky" palette independent of light/dark theme — keep those hex values in sync with `.dark`'s tone if the dark palette changes.

**Bottom sheets and the keyboard**: the add/edit forms (`shopping-item-form.tsx`, `task-form.tsx`, `date-form.tsx`) use `Sheet` (`side="bottom"`) with a max-height clamped to a `--visual-vh` CSS var, kept live by `useVisualViewportHeight()` (`src/lib/use-visual-viewport.ts`, mounted once in `RootLayout`). This is the iOS Safari fallback for the mobile-keyboard-covers-the-sheet problem; `index.html`'s `interactive-widget=resizes-content` viewport meta handles it natively on Chromium. If you add another bottom sheet with form inputs, reuse the same `max-h-[min(92dvh,calc(var(--visual-vh,100dvh)*0.92))]` pattern rather than a bare `dvh` value.

**PWA / deploy**: `vite.config.ts` sets `base: '/Baby_Prep_Site-Shahar-Topaz-/'` for GitHub Pages — must match the repo name if the repo is ever renamed. `VitePWA` config (manifest, workbox caching) also lives there. Deploys automatically via `.github/workflows/deploy.yml` on push to `main`; GitHub Pages source must be set to "GitHub Actions" once per repo.

**Compiler**: React Compiler is enabled via `@rolldown/plugin-babel` + `reactCompilerPreset()` in `vite.config.ts` — avoid manual `useMemo`/`useCallback` unless there's a specific reason, the compiler handles most of it.

**Path alias**: `@/*` → `./src/*` (configured in both `tsconfig.app.json` and `vite.config.ts`).
