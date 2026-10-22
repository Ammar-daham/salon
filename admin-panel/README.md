# admin-panel

Salonat Pro, the web app salon staff and platform admins use: salons, staff and their schedules, services,
opening hours, clients, appointments and a calendar. Next.js 16 (App Router) with React 19,
TanStack Query and Tailwind CSS 4, on top of the [salon-backend](../salon-backend) REST API.

What changed in each version is in [CHANGELOG.md](CHANGELOG.md); each release says which backend
version it needs.

## Run it with Docker

From the repository root:

```bash
docker compose --profile demo up --build
```

This starts the panel on http://localhost:3000 with the API, a database and sample data. Sign in as
`superadmin@salon.example.com` with `Password123!`. The [root README](../README.md) has the details.

## Run it without Docker

You need Node 20 or later, and the API running on http://localhost:8081 (see the
[salon-backend README](../salon-backend/README.md)).

```bash
npm install
npm run dev
```

Then open http://localhost:3000. Keep it on port 3000: in development the API only accepts requests
from http://localhost:3000. If the API isn't on http://localhost:8081, copy
[`.env.example`](.env.example) to `.env.local` and change the address there.

Sign in with an account the backend already has: one from its demo data, or the first super admin
that the backend README shows how to create. A salon owner can register their salon at `/register`;
they can sign in once a super admin approves it.

## Scripts

| Command | What it does |
|---|---|
| `npm run dev` | The development server on http://localhost:3000 |
| `npm run build`, `npm start` | A production build, and serving it |
| `npm run lint` | ESLint |
| `npm run typecheck` | Generates Next's route types, then runs `tsc --noEmit` |
| `npm test` | The Vitest unit tests; `npm run test:watch` reruns them on every change |
| `npm run test:e2e` | The Playwright end-to-end tests, against a running panel and API (below) |

CI runs lint, the type check, the unit tests and the build on every pull request, then the
end-to-end tests against the Docker Compose stack.

## End-to-end tests

`e2e/` holds Playwright smoke tests: signing in and out, and creating, changing and removing a salon
as the super admin, and a service and an appointment as Glow Beauty Studio's admin. They drive
Chromium against a running panel, API and database with the demo data, and start none of it
themselves. From the repository root, then this folder:

```bash
docker compose --profile demo up --build --detach
npx playwright install chromium    # once
npm run test:e2e
```

`npm run dev` against a backend with the demo data works too. `E2E_BASE_URL` points the tests at a
panel somewhere other than http://localhost:3000. Each run creates its own uniquely named records
and removes them again, apart from the appointment, which it cancels: appointments are never
deleted. A failed test leaves a trace in `test-results/` that `npx playwright show-trace` opens.

## Configuration

| Variable | What it's for |
|---|---|
| `NEXT_PUBLIC_API_URL` | Where the browser reaches the API. Defaults to `http://localhost:8081`. |
| `NEXT_OUTPUT` | `standalone` builds Next's self-contained server. Only the Dockerfile sets it. |

Next copies `NEXT_PUBLIC_API_URL` into the browser code when it builds the panel, so it's read by
`npm run dev` and `npm run build`, not when the server starts. Restart `npm run dev` after changing
it, and give each environment its own build. The API has to allow the panel's address too: under
the backend's prod profile that's `SALON_ALLOWED_ORIGINS`
([backend configuration](../salon-backend/README.md#configuration)).

The Docker image is built from [`Dockerfile`](Dockerfile), with the API's address as a build
argument:

```bash
docker build --build-arg NEXT_PUBLIC_API_URL=https://api.example.com -t salon-admin-panel .
docker run -p 3000:3000 salon-admin-panel
```

## How it works

- Signing in starts a session on the backend. The browser keeps it in the `JSESSIONID` cookie and
  sends it with every request. A 401 on any request, apart from signing in and the check for an
  existing session, signs the panel out.
- What each role may see and do is in `src/lib/auth/permissions.ts`, which the sidebar and the route
  guard both read. The backend enforces the same rules: the panel only hides what it would refuse.
- An ADMIN or EMPLOYEE works in their own salon (`src/lib/auth/scope.ts`). A SUPER_ADMIN sees every
  salon, and picks one first where a page works on a single salon.
- The API speaks `snake_case`. Each resource's mappers turn it into the `camelCase` types the rest of
  the panel uses, and back.

## Code layout

| Folder | What's in it |
|---|---|
| `src/app` | The routes: `(admin)` for the signed-in pages, `(full-width-pages)` for sign-in and password reset |
| `src/features` | The screens of each area: salons, staff, clients, appointments, the calendar and so on |
| `src/lib/resources` | One folder per API resource: requests (`*.api.ts`), mappers, TanStack Query hooks and types |
| `src/lib/api`, `src/lib/auth` | The HTTP client, endpoints and paging; permissions, route access and salon scope |
| `src/components`, `src/layout`, `src/context` | Shared UI; the sidebar and header; sign-in, theme and sidebar state |
| `src/test` | The unit tests, in the same layout as `src/` |

## Credits

The layout and many of the UI components started from [TailAdmin](https://tailadmin.com)'s free
Next.js template, used under the MIT license in [TAILADMIN-LICENSE](TAILADMIN-LICENSE).
