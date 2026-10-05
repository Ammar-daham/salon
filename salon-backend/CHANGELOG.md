# salon-backend changelog

All notable changes to the salon-backend API are recorded here. Every client (admin panel,
landing page, customer app) depends on this API, so changes that break clients go under
**Breaking API changes** and list each field or path that changed.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/). Before 1.0.0, a breaking API change bumps the minor
version. The version lives in [`gradle.properties`](gradle.properties); each release is tagged
`salon-backend/v<version>` on `main`.

## [Unreleased]

### Breaking API changes
- `DELETE /api/v1/businesses/{id}` returns 409 `CONFLICT` while the salon still has staff; remove or
  move them first (BE-17).

### Added
- Customers API at `/api/v1/businesses/{businessId}/customers` (list, get, create, replace, delete).
  ADMIN and EMPLOYEE of the salon can list, view and add customers; only ADMIN can edit or delete
  them; SUPER_ADMIN can do all of it for any salon (DB-03).
- Migration V8: a `customers` table replaces `business_customers`; existing links are copied over
  with each customer's name, email and first phone number (DB-03).

### Fixed
- `users.updated_at` is set on every update, and user responses now include it (BE-12).
- Emails are unique regardless of case, and sign-in accepts any casing of the email (DB-10).
- Two owners can share a contact value, e.g. a salon's phone that is also its owner's; one owner
  still can't list the same value twice (DB-05).

### Changed
- Migration V4: `users.business_id` is a foreign key to `businesses`, and a `set_updated_at()`
  trigger maintains `updated_at` on every table (DB-01, DB-11).
- Migration V5: every address and contact must have exactly one owner and is deleted with it; contact
  values are unique per owner; the email index is on `lower(email)`; every foreign-key column is
  indexed (DB-05, DB-06, DB-10, DB-12). V5 removes ownerless addresses/contacts, and stops with an
  error if two users' emails differ only by case or a row has two owners.
- Dropped the unused `spring-boot-starter-data-jpa` for `spring-boot-starter-jdbc`; Hibernate no
  longer starts. `@Transactional` is Spring's (BE-36).
- Flyway is managed by the Spring Boot BOM: `flyway-core` goes from 10.0.0 to 11.14.1, matching
  `flyway-database-postgresql` (BE-37).

## [0.1.0] - 2026-09-28

The first release that is safe to run with more than one salon. Every finding the integration
suite could reproduce is fixed and guarded by a test.

### Breaking API changes
- `POST /api/v1/businesses` is SUPER_ADMIN-only; an ADMIN now gets 403 (#23).
- `GET /api/v1/businesses` and `GET /api/v1/businesses/{id}` show non-admins only APPROVED salons
  plus the one they work at; any other salon is a 404 (FE-11, #23).
- A non-SUPER_ADMIN changing a business's `status` gets 403. Re-sending the unchanged status is
  still accepted (BE-02, #13).
- `GET /api/v1/users` returns only the caller's business unless the caller is SUPER_ADMIN (BE-01, #15).
- Nested `addresses`, `contacts` and `services` in business and user PUT bodies are ignored (BE-06,
  BE-07, BE-21, #18).
- Duplicate keys return 409 where some endpoints used to return a false 200/201; error `code`
  values are stable and correctly spelled (BE-09, BE-18, #19).

### Security
- An ADMIN can no longer promote themselves or grant a role at or above their own (BE-04, #12).
- Business writes are scoped to the caller's own salon; only SUPER_ADMIN can approve, reject or
  suspend a salon (BE-02, #13).
- Service GET/PUT/DELETE check that the service belongs to the salon in the path (BE-03, #14).
- User read, update and delete are scoped to the caller's business and run in one transaction
  (BE-01, BE-05, BE-33, #15).
- Nested child ids in updates can no longer edit another salon's records (BE-06, #18).
- The session id is rotated on login (BE-24, #20).
- The signed-in user is reloaded on every request, so role changes and deletions take effect
  immediately (BE-14, #20).
- Only SUPER_ADMIN can create a business (#23).

### Fixed
- Deletes cascade from the stored record, and deleting a user removes both their contacts and
  their addresses (BE-10, BE-11, #16).
- Adding a new address or contact through a parent update no longer crashes (BE-07, #18).
- Unhandled errors are logged 500s instead of masked 400s, and logging goes through SLF4J (BE-27,
  BE-34, #19).
- Non-admins can read their own address and contact records again (BE-41, #22).

### Added
- Integration test suite: Testcontainers Postgres, MockMvc, per-role login helpers and a fixed
  fixture reloaded before every test (BE-32, #10).
- CI builds and tests the backend on every pull request (BE-31, #11).

[Unreleased]: https://github.com/Ammar-daham/salon/compare/salon-backend/v0.1.0...HEAD
[0.1.0]: https://github.com/Ammar-daham/salon/releases/tag/salon-backend/v0.1.0
