# admin-panel changelog

All notable changes to the admin panel are recorded here. Each release states the salon-backend
version it needs.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/). The version is the `version` field in
[`package.json`](package.json); each release is tagged `admin-panel/v<version>` on `main`.

## [Unreleased]

Requires salon-backend with the customers API (DB-03), migration V9 (DB-07, DB-08), the opening
hours API with migration V10 (DB-09, DB-14), and the appointments and availability APIs with
migration V12 (DB-13, DB-14).

### Changed
- Customers run on live data from `/businesses/{id}/customers` instead of generated sample data
  (FE-03). The list shows name, email, phone and client-since date; the tags, visit, spend and
  last-visit columns are gone until appointments exist.
- Prices show in the salon's own currency instead of always EUR, in the services table, the salon
  overview and the service form's price label (DB-08).
- Address coordinates are read as numbers, matching the backend (DB-07).

### Added
- An Appointments page (`/appointments`) with the salon's bookings by date, on the salon's clock,
  filterable by status and staff member and searchable by client, service or staff. A SUPER_ADMIN
  sees every salon's. The same list fills each salon's and each employee's Appointments tab
  (FE-04).
- An appointment page (`/appointments/:id`) with the client, staff member, service, notes and the
  price it was booked at. It can be confirmed, marked completed or a no-show once it has started,
  or cancelled; the last three are final, so the panel asks first (FE-04).
- Booking (`/appointments/new`) and changing (`/appointments/:id/edit`) appointments. Pick a client
  and a service, then one of the salon's open times for a day, from anyone who performs it or one
  staff member. A time outside the open times can be typed in for one staff member, and changing an
  appointment can keep its current time. A SUPER_ADMIN picks the salon first. "Book appointment" on
  a salon's or employee's Appointments tab starts the form with them; a booked or confirmed
  appointment's page has an Edit button (FE-04).
- An Hours tab on each salon (`/businesses/:id/hours`) with its weekly opening hours, on the salon's
  clock. A day without hours is closed, and a second interval makes a lunch break. The week is saved
  whole, and backwards or overlapping intervals are pointed out before saving. Admins edit it;
  employees see it read-only (FE-04).
- A Schedule tab on each employee (`/employees/:id/schedule`) with their weekly shifts, on the
  salon's clock. A day without shifts is a day off, and the week is saved whole with the same
  editor as the salon's opening hours. Admins edit it; employees see it read-only (FE-04).
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
