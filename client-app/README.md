# client-app

Salonat, the app clients use to find a salon nearby and book an appointment. Next.js 16 (App
Router) with React 19, TanStack Query and Tailwind CSS 4 — the same stack as
[admin-panel](../admin-panel), with its own, consumer-facing design system.

**This version runs entirely on mock data. There is no backend dependency yet** — `salon-backend`
doesn't let a customer browse salons or book anonymously today (see
[the root CHANGELOG](../CHANGELOG.md) and "What this can't do yet" below). Every screen reads from
an in-repo mock data layer shaped like the real future API, so wiring it up later only touches the
`*.api.ts` file per resource, not types, hooks or components.

What changed in each version is in [CHANGELOG.md](CHANGELOG.md).

## Run it

You need Node 20 or later.

```bash
npm install
npm run dev
```

Then open http://localhost:3001. Port 3001, not 3000 — that's admin-panel's.

## Scripts

| Command | What it does |
|---|---|
| `npm run dev` | Starts the dev server on port 3001 |
| `npm run build` | Production build |
| `npm start` | Runs the production build, port 3001 |
| `npm run lint` | ESLint |
| `npm run typecheck` | Generates route types, then `tsc --noEmit` |
| `npm test` | Runs the Vitest unit tests once |
| `npm run test:watch` | Vitest in watch mode |

No Playwright/e2e suite yet — there's no live backend to drive one against, and no Docker
available in every environment this runs in. Verification for now is lint + typecheck + unit tests
for pure logic (mappers, mock data, distance math), plus manually clicking through `npm run dev`.

`npm test`/`npm run test:watch` need **Node 20.19+ or 22.13+** — vitest 4's config loader requires
it to `require()` an ESM-only dependency, and fails with `ERR_REQUIRE_ESM` on older Node (this
matters here since admin-panel pins the identical vitest/vite versions). `nvm install 20 && nvm use 20`
if your default Node predates that.

## What this can't do yet

This app is a design-and-structure first pass, not a production booking surface:

- **No real salons, no real booking.** Everything — salons, services, staff, availability,
  bookings — is mock data generated in `src/lib/mock/`, reset on every page load unless noted
  otherwise.
- **No backend wiring**, because the backend doesn't support an anonymous customer yet:
  `GET /api/v1/**` requires authentication, `CUSTOMER` accounts can't practically sign in
  (`AppUserDetailsService` rejects a null password and nothing sets one), and the
  availability/customer/appointment endpoints explicitly exclude the `CUSTOMER` role. None of that
  is this app's bug to fix — it's tracked as backend work for later.
- **No Arabic/RTL.** The Salonat brand is Arabic-rooted (see [`../brand`](../brand)), but this
  first pass is English/LTR only, matching admin-panel's own precedent. Localization is a
  deliberately separate, later phase.
