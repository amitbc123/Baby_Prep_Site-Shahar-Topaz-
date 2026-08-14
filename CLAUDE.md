# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

"אור ירח" (One More Moon) — a private baby-prep PWA for Shahar & Topaz. Hebrew, RTL. Shopping list w/ budget & priorities, tasks (incl. hospital bag), important dates & wishes, and a countdown to the due date framed as a moon filling up. No backend: all data lives in `localStorage` on the device; cross-device sync is manual export/import JSON in Settings.

## Commands

```bash
npm run dev       # vite dev server
npm run build      # tsc -b && vite build (type-check is part of the build, no separate typecheck script)
npm run lint       # oxlint
npm run preview    # preview production build
npm run test       # vitest run
```

Tests live next to the code they cover (`*.test.ts`), config is `vitest.config.ts` (separate from `vite.config.ts` since the latter isn't built with `vitest/config`'s `defineConfig`). Coverage is limited to the pure-logic files in `src/lib` and `src/features/shopping/budget.ts` — no component/integration tests.

## Architecture

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
