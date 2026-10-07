# admin-panel changelog

All notable changes to the admin panel are recorded here. Each release states the salon-backend
version it needs.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/). The version is the `version` field in
[`package.json`](package.json); each release is tagged `admin-panel/v<version>` on `main`.

## [Unreleased]

Requires salon-backend with the customers API (DB-03), migration V9 (DB-07, DB-08) and the opening
hours API with migration V10 (DB-09, DB-14).

### Changed
- Customers run on live data from `/businesses/{id}/customers` instead of generated sample data
  (FE-03). The list shows name, email, phone and client-since date; the tags, visit, spend and
  last-visit columns are gone until appointments exist.
- Prices show in the salon's own currency instead of always EUR, in the services table, the salon
  overview and the service form's price label (DB-08).
- Address coordinates are read as numbers, matching the backend (DB-07).

### Added
- An Hours tab on each salon (`/businesses/:id/hours`) with its weekly opening hours, on the salon's
  clock. A day without hours is closed, and a second interval makes a lunch break. The week is saved
  whole, and backwards or overlapping intervals are pointed out before saving. Admins edit it;
  employees see it read-only (FE-04).
- A time-zone picker on the salon create and settings forms, listing the browser's IANA zones
  (DB-09).
- A currency picker on the salon create and settings forms (DB-08).
- Add a client (`/customers/new`) and edit one (`/customers/:id/edit`). Admins can edit notes from
  the Notes tab and delete clients from the list; employees can add and view clients (FE-03).

### Removed
- The mock data generators in `lib/mock/`, now unused.

## [0.1.0] - 2026-09-28

Requires salon-backend ≥ 0.1.0.

### Changed
- DELETE requests are sent without a body; the panel no longer re-fetches a record to echo it
  back.
- ADMIN is granted `user:list`, `user:create`, `user:edit` and `user:delete` now that the backend
  scopes users to the caller's business (FE-01, #21).

### Added
- Vitest unit tests for the permission matrix, route access and the auth, user and business wire
  mappers.
- CI runs lint, type check, unit tests and `next build` on every pull request (#11).

[Unreleased]: https://github.com/Ammar-daham/salon/compare/admin-panel/v0.1.0...HEAD
[0.1.0]: https://github.com/Ammar-daham/salon/releases/tag/admin-panel/v0.1.0
