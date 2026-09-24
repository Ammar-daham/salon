# Admin Panel — Audit Findings

Audit date: 2026-09-23 (supersedes the 2026-09-19 sign-in/sign-up security audit in this file).
Scope: `admin-panel/` (Next.js 16 / React 19 / TanStack Query / Tailwind 4) and the backend auth
behaviour it depends on.

**The phased MVP roadmap, database redesign and future features for the whole platform are in
[`../salon-backend/AUDIT_FINDINGS.md`](../salon-backend/AUDIT_FINDINGS.md) §5–§7.** This file covers
frontend-specific findings only.

A ✅ next to a finding's ID means its fix has landed; the parenthetical *(fixed: …)* note on that
finding says what changed. So far: ✅ FE-09.

## 0. Honest summary

The admin panel is **ahead of the backend**, and it's the healthier of the two codebases:

- Clean layering — typed `endpoints.ts`, wire-shape mappers per resource, a `Repository` interface that
  lets mock and live data swap without touching call sites, a typed query-key factory.
- Capability-based permissions (`can(user, "service:edit")`) shared by the sidebar and the route
  guard, so a hidden nav item and a blocked route can't disagree.
- Honest about gaps: mock-backed pages carry a "Sample data" pill; unbuilt pages are labelled
  placeholders instead of dead links.
- `eslint` and `tsc --noEmit` pass clean. (`next build` couldn't be verified in the audit sandbox —
  it was OOM-killed, which is an environment limit, not a code finding.)

But most of what a salon owner would open it for doesn't work yet:

| Area | State |
|---|---|
| Sign-in / session | Live |
| Dashboard | Live (businesses + users counts only) |
| Businesses, My salon, Services | Live |
| Users (platform) | Live — SUPER_ADMIN only |
| Staff account creation | Live |
| Employees list / profile / schedule | **Mock** (backend staff API is a stub) |
| Customers list / profile / notes | **Mock** (no customers API) |
| Appointments, Calendar | **Placeholder** (no backend) |
| Reports, Settings, Subscriptions, Notifications | **Placeholder** |
| Header notification bell | **Template mock** (TailAdmin demo content) |

## 1. Findings

Every finding has an ID (`FE-xx`) and ends with **→ the branch that fixes it**, linked to that
branch's row in [`VERSION_CONTROL_GUIDE.md` §9](../VERSION_CONTROL_GUIDE.md#9-branch-plan-for-the-mvp-roadmap). Backend
findings it depends on are linked by their `BE-xx` / `DB-xx` IDs.

### Critical — backend holes the UI is currently papering over
- <a id="fe-01"></a>**FE-01 — Hiding a button is the only thing stopping an ADMIN from editing/deleting
  other salons.** `src/lib/auth/permissions.ts` withholds `business:delete` and `user:*` from ADMIN
  *because* the backend doesn't enforce ownership. Verified live: an ADMIN can approve another salon,
  edit another salon's services, list every user, and promote themselves to SUPER_ADMIN with plain API
  calls ([BE-01](../salon-backend/AUDIT_FINDINGS.md#be-01)–[BE-05](../salon-backend/AUDIT_FINDINGS.md#be-05)). Fix is backend-side; once fixed, revisit which grants
  ADMIN should get. **→ [`test/admin-panel-permission-matrix`](../VERSION_CONTROL_GUIDE.md#br-0-12)** (after
  [0.3](../VERSION_CONTROL_GUIDE.md#br-0-3)–[0.6](../VERSION_CONTROL_GUIDE.md#br-0-6) merge)
- <a id="fe-02"></a>**FE-02 — Client-side route guarding only.** `RouteGuard` / `routeAccess.ts` are UX,
  not security (and documented as such). Acceptable *only* once the backend enforces the same rules.
  **→ [`fix/salon-backend-business-tenant-scoping`](../VERSION_CONTROL_GUIDE.md#br-0-4),
  [`fix/salon-backend-service-ownership`](../VERSION_CONTROL_GUIDE.md#br-0-5), [`fix/salon-backend-user-scoping`](../VERSION_CONTROL_GUIDE.md#br-0-6)**

### High
- <a id="fe-03"></a>**FE-03 — Employees and Customers run on generated mock data.** The rosters are
  fabricated per real business (`lib/mock/generators.ts`); the "Add staff" action is live, so a newly
  created employee will not appear in the Employees list. Confusing for any real user.
  **→ [`feature/admin-panel-live-employees`](../VERSION_CONTROL_GUIDE.md#br-1-7), [`feature/admin-panel-live-customers`](../VERSION_CONTROL_GUIDE.md#br-1-9)**
- <a id="fe-04"></a>**FE-04 — Appointments, Calendar and scheduling are placeholders.** The core booking
  product has no UI (and no backend, [DB-14](../salon-backend/AUDIT_FINDINGS.md#db-14)).
  **→ [`feature/admin-panel-hours-and-schedules`](../VERSION_CONTROL_GUIDE.md#br-2-5), [`feature/admin-panel-appointments`](../VERSION_CONTROL_GUIDE.md#br-2-6),
  [`feature/admin-panel-calendar`](../VERSION_CONTROL_GUIDE.md#br-2-7)**
- <a id="fe-05"></a>**FE-05 — No login rate limiting / lockout** (backend, [BE-22](../salon-backend/AUDIT_FINDINGS.md#be-22)) — the
  sign-in form is the entry point. **→ [`feature/salon-backend-login-rate-limiting`](../VERSION_CONTROL_GUIDE.md#br-3-1)**
- <a id="fe-06"></a>**FE-06 — Password policy is client-only.** `UserForm` enforces ≥ 8 characters; the
  backend accepts any non-blank password. A direct API call bypasses it. Email format is likewise
  HTML5-only ([BE-23](../salon-backend/AUDIT_FINDINGS.md#be-23)). **→ [`feature/salon-backend-request-validation`](../VERSION_CONTROL_GUIDE.md#br-1-4)**
- <a id="fe-07"></a>**FE-07 — Session fixation & insecure cookie** (backend, [BE-24](../salon-backend/AUDIT_FINDINGS.md#be-24),
  [BE-25](../salon-backend/AUDIT_FINDINGS.md#be-25)) — session id not rotated on login; `secure` flag not forced; timeout is the
  implicit 30-minute default. **→ [`fix/salon-backend-session-hardening`](../VERSION_CONTROL_GUIDE.md#br-0-11),
  [`chore/salon-backend-production-config`](../VERSION_CONTROL_GUIDE.md#br-3-4)**

### Medium
- <a id="fe-08"></a>**FE-08 — No frontend tests.** No unit tests for `permissions.ts` / `routeAccess.ts` /
  the mappers, and no e2e (Playwright) for sign-in and CRUD flows. The permission matrix and wire
  mappers are exactly where regressions will hide.
  **→ [`test/admin-panel-permission-matrix`](../VERSION_CONTROL_GUIDE.md#br-0-12), [`test/admin-panel-e2e-smoke`](../VERSION_CONTROL_GUIDE.md#br-3-8)**
- <a id="fe-09"></a>✅ **FE-09 — Deletes echo the whole record in the DELETE body.** `businesses.api.ts` and
  `users.api.ts` re-fetch the record and send it back so the backend can cascade children
  ([BE-10](../salon-backend/AUDIT_FINDINGS.md#be-10)). **→ [`refactor/admin-panel-drop-delete-bodies`](../VERSION_CONTROL_GUIDE.md#br-0-8)** (after
  [`fix/salon-backend-server-side-deletes`](../VERSION_CONTROL_GUIDE.md#br-0-7))
  *(fixed: both repositories' `remove(id)` now send a plain body-less `DELETE` — no re-fetch, no echoed
  record. Relies on the backend's [BE-10](../salon-backend/AUDIT_FINDINGS.md#be-10) fix, which deletes
  children from the canonical record, so this must ship after `fix/salon-backend-server-side-deletes`.)*
- <a id="fe-10"></a>**FE-10 — CSRF is CORS-only** (backend, [BE-28](../salon-backend/AUDIT_FINDINGS.md#be-28)) — fine while
  `allowed-origins` stays pinned to the panel's origin. **→ [`chore/salon-backend-production-config`](../VERSION_CONTROL_GUIDE.md#br-3-4)**
- <a id="fe-11"></a>**FE-11 — `GET /businesses` open to every authenticated role** — intended for a future
  customer app; confirm, and hide non-APPROVED salons from non-admins.
  **→ [`fix/salon-backend-business-tenant-scoping`](../VERSION_CONTROL_GUIDE.md#br-0-4)**
- <a id="fe-12"></a>**FE-12 — Stale identity.** The backend session keeps a snapshot of the user, so a role
  change made in the Users page doesn't take effect for that user until they sign in again
  ([BE-14](../salon-backend/AUDIT_FINDINGS.md#be-14)). **→ [`fix/salon-backend-session-hardening`](../VERSION_CONTROL_GUIDE.md#br-0-11)**
- <a id="fe-13"></a>**FE-13 — Staff password can never be changed or reset.** `UserForm` says so
  explicitly; no endpoint exists. **→ [`feature/salon-backend-password-reset`](../VERSION_CONTROL_GUIDE.md#br-3-2), then
  [`feature/admin-panel-password-reset`](../VERSION_CONTROL_GUIDE.md#br-3-3)**

### Low
- <a id="fe-14"></a>**FE-14 — TailAdmin leftovers.** `README.md` is the template's README,
  `banner.png`/`LICENSE` are template files, and `NotificationDropdown.tsx` (384 lines) renders
  hard-coded demo notifications. **→ [`chore/admin-panel-remove-template-leftovers`](../VERSION_CONTROL_GUIDE.md#br-3-7)**
- <a id="fe-15"></a>**FE-15 — Unused dependencies.** `apexcharts`/`react-apexcharts` are imported nowhere;
  `flatpickr` is only used by `components/form/date-picker.tsx`, which nothing renders, yet its CSS is
  loaded globally in `app/layout.tsx`. FullCalendar is also unused today but is legitimately needed for
  Phase 2. **→ [`chore/admin-panel-remove-template-leftovers`](../VERSION_CONTROL_GUIDE.md#br-3-7)**
- <a id="fe-16"></a>**FE-16 — `NEXT_PUBLIC_DEV_BUSINESS_ID` escape hatch** in `lib/auth/scope.ts` — make sure
  it's never set in production (document it, or remove it once every seeded user has a business).
  **→ [`chore/admin-panel-remove-template-leftovers`](../VERSION_CONTROL_GUIDE.md#br-3-7)**
- <a id="fe-17"></a>**FE-17 — No `.env.example`** documenting `NEXT_PUBLIC_API_URL`.
  **→ [`chore/admin-panel-remove-template-leftovers`](../VERSION_CONTROL_GUIDE.md#br-3-7)**

### Fixed since the previous audit
- Dead "Forgot password?" link removed from the sign-in form.
- Dead "Keep me logged in" checkbox removed.
- Old `SignUpForm` replaced by `features/users/UserForm` with password confirmation and an 8-character minimum.
- Wire format unified on snake_case with mappers for users, auth and businesses; business endpoints plural.

### Confirmed solid
Auth is a context (the 401 interceptor can clear identity synchronously); expected 401s on `/auth/me`
and `/auth/login` don't trigger global sign-out; queries never retry auth/validation failures;
`confirmPassword` is never sent to the backend; logout calls the backend before clearing state;
no sensitive data logged.

## 2. Frontend work per roadmap phase

The admin-panel branches, in merge order, with their status, are in
[`VERSION_CONTROL_GUIDE.md` §9](../VERSION_CONTROL_GUIDE.md#9-branch-plan-for-the-mvp-roadmap):

- **Phase 0** — [`refactor/admin-panel-drop-delete-bodies`](../VERSION_CONTROL_GUIDE.md#br-0-8) ([FE-09](#fe-09)),
  [`test/admin-panel-permission-matrix`](../VERSION_CONTROL_GUIDE.md#br-0-12) ([FE-01](#fe-01), [FE-08](#fe-08)).
- **Phase 1** — [`feature/admin-panel-live-employees`](../VERSION_CONTROL_GUIDE.md#br-1-7) and
  [`feature/admin-panel-live-customers`](../VERSION_CONTROL_GUIDE.md#br-1-9) ([FE-03](#fe-03)).
- **Phase 2** — [`feature/admin-panel-hours-and-schedules`](../VERSION_CONTROL_GUIDE.md#br-2-5),
  [`feature/admin-panel-appointments`](../VERSION_CONTROL_GUIDE.md#br-2-6), [`feature/admin-panel-calendar`](../VERSION_CONTROL_GUIDE.md#br-2-7) ([FE-04](#fe-04)).
- **Phase 3** — [`feature/admin-panel-password-reset`](../VERSION_CONTROL_GUIDE.md#br-3-3) ([FE-13](#fe-13)),
  [`chore/admin-panel-remove-template-leftovers`](../VERSION_CONTROL_GUIDE.md#br-3-7) ([FE-14](#fe-14)–[FE-17](#fe-17)),
  [`test/admin-panel-e2e-smoke`](../VERSION_CONTROL_GUIDE.md#br-3-8) ([FE-08](#fe-08)).
