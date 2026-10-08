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
- Address `latitude`/`longitude` are JSON numbers instead of strings, in range, and given together
  or not at all; input is rounded to 6 decimals (DB-07).
- A service `price` is required (it used to default to 0 when omitted) and accepts at most 2
  decimals (DB-08).

### Security
- Failed sign-ins are limited to five per email and twenty per client address in any 15 minutes.
  Past that, `POST /api/v1/auth/login` answers 429 `TOO_MANY_REQUESTS` with `Retry-After` in seconds,
  even for the right password, until the oldest failure is 15 minutes old. A successful sign-in
  clears the email's failures and doesn't count against the address. Emails without an account lock
  the same way. The limits are `app.login-throttle.max-failures-per-account`,
  `max-failures-per-address` and `window` (BE-22).
- Changing a password signs out every session that signed in with the old one, however it was
  changed (FE-13).
- A password reset link carries a random 256-bit token of which only a SHA-256 is stored. It works
  once, for an hour, and asking for a new one retires the earlier links. A user is sent at most three
  an hour (FE-13).

### Added
- `GET /actuator/health` for a load balancer or orchestrator, without signing in: 200
  `{"status":"UP"}`, or 503 `{"status":"DOWN"}` while the database doesn't answer, and never which
  part failed. Nothing else from Spring Boot Actuator is exposed. A passing check isn't written to the
  request log (BE-39).
- `POST /api/v1/auth/change-password` with `current_password` and `new_password` (8 to 72
  characters) changes the signed-in user's own password and answers 204. The session that changed it
  stays signed in, under a new id. A wrong current password is a 400 and counts as a failed sign-in
  towards the lockout (BE-22, FE-13).
- `POST /api/v1/auth/forgot-password` with an `email` answers 202 straight away and, if the email
  has an account that can sign in, emails it a link to `app.password-reset.link` with `?token=`.
  The answer is the same, and as quick, whether or not the account exists (FE-13).
- `POST /api/v1/auth/reset-password` with that `token` and a new `password` (8 to 72 characters)
  sets the password, answers 204, signs out every session of the user and lifts any sign-in lockout
  on the email. A used, replaced, expired or made-up token is a 400 (FE-13).
- Email through any SMTP server set in `spring.mail.*`, from `app.mail.from`. Without
  `spring.mail.host` an email is written to the log instead, except under the `prod` profile, which
  only logs that it wasn't sent (FE-13).
- Migration V13: a `password_reset_tokens` table (FE-13).
- Availability at `GET /api/v1/businesses/{businessId}/services/{serviceId}/availability`: the
  salon's `timezone`, the service's `duration_minutes`, and for each active staff member who performs
  the service (`id`, `first_name`, `last_name`) their open `slots`, each a `starts_at` and `ends_at`
  on the salon's clock. A slot lies inside the opening hours and the staff member's shift, is clear
  of their time off and of appointments that aren't cancelled or missed, hasn't started yet, and
  starts on a quarter hour (DB-14).
- `from` and `to` (`"yyyy-MM-dd"` on the salon's clock, both included, at most 31 days) default to
  today, and `staff_id` narrows availability to one staff member. Only the salon's ADMINs and
  EMPLOYEEs, or a SUPER_ADMIN, can see it. Booking doesn't require a slot: availability is advice for
  the booking form, so staff can still fit someone in after hours (DB-14).
- Appointments at `/api/v1/businesses/{businessId}/appointments` (book, list, get, replace). A booking
  names a `customer_id`, `staff_id` and `service_id`, a `starts_at` as `"yyyy-MM-ddTHH:mm"` on the
  salon's clock, and optional `notes`. The service sets `ends_at` and the `price`; changing the
  service's price or length later leaves booked appointments alone, and so does moving one, unless
  its service changes. `POST` and `PUT` return the appointment as stored (DB-14).
- An appointment reads back with `customer` and `staff` (`id`, `first_name`, `last_name`), `service`
  (`id`, `name`), `starts_at`, `ends_at`, `status`, `price` (in the salon's `currency`), `notes`,
  `created_at` and `updated_at`. The names stay readable after the customer,
  staff member or service is removed (DB-13, DB-14).
- `PUT .../appointments/{appointmentId}/status` with `{"status": "..."}` moves an appointment along
  `BOOKED` → `CONFIRMED` → `COMPLETED` / `CANCELLED` / `NO_SHOW` and returns it. Confirming is
  optional, the last three are final, and an appointment can only be `COMPLETED` or a `NO_SHOW` once
  it has started. Asking for the current status changes nothing; any other move is a 409. Only a
  `BOOKED` or `CONFIRMED` appointment can be changed with `PUT`. There is no `DELETE`: cancelling
  keeps the history (DB-14).
- `GET .../appointments` lists them by start time. `from` and `to` (`"yyyy-MM-dd"` on the salon's
  clock, both included), `staff_id` and `customer_id` each narrow the list, and are all optional
  (DB-14).
- A booking needs one of the salon's own current customers, an active staff member who performs the
  service, and an active service; otherwise 400. A staff member can't be booked twice at once (409).
  ADMIN and EMPLOYEE of the salon can see, book and change any of its appointments; SUPER_ADMIN can
  anywhere (DB-14).
- Migration V12: `deleted_at` on services, staff and customers. A user can have one current staff
  record, and an account one current customer record per salon; removed ones don't count (DB-13).
  An `appointments` table whose foreign keys only accept the salon's own customer, staff member and
  service, and whose exclusion constraint refuses to double-book a staff member; cancelled and missed
  appointments free their time (DB-14).
- The services each staff member performs, at `/api/v1/businesses/{businessId}/staff/{staffId}/services`.
  `GET` lists them for anyone who can see the salon; `PUT {"service_ids": [...]}` replaces them and
  returns the list. Only the salon's own services can be assigned. Only the salon's ADMIN or a
  SUPER_ADMIN can change them (DB-14).
- Weekly working hours at `.../staff/{staffId}/schedule`, shaped and checked like opening hours but
  with `starts_at`/`ends_at`. The salon's staff can read them; only its ADMIN or a SUPER_ADMIN can
  replace them (DB-14).
- Time off at `.../staff/{staffId}/time-off` (list, get, create, replace, delete): `starts_at` and
  `ends_at` as `"yyyy-MM-ddTHH:mm"` on the salon's clock, and an optional `note`. A whole day off runs
  from 00:00 to 00:00 the next day. One person's absences can't overlap (409). Only the salon's
  admins and the staff member themselves can see it; only an ADMIN or a SUPER_ADMIN can change it
  (DB-14).
- Migration V11: `staff_services`, `staff_schedules` and `staff_time_off`. The database itself refuses
  to link a staff member to another salon's service, and rejects overlapping shifts or absences (DB-14).
- Opening hours at `/api/v1/businesses/{businessId}/hours`. `GET` returns the salon's `timezone` and
  its weekly `hours`, each with a `day_of_week` (`MONDAY` to `SUNDAY`) and `opens_at`/`closes_at` as
  `"HH:mm"` on the salon's clock; anyone who can see the salon can read them. `PUT {"hours": [...]}`
  replaces the whole week and returns it in order: a day left out is closed, and a day can have
  several intervals (e.g. a lunch break). An interval must end after it starts, can't run past
  midnight, and can't overlap another on the same day. Only the salon's ADMIN or a SUPER_ADMIN can
  change them (DB-14).
- `timezone` on businesses (an IANA ID, default `Europe/Berlin`): the clock a salon's hours are on.
  Optional on create and update; omitting it on update keeps the stored one. Fixed offsets such as
  `+01:00` are rejected, since they ignore daylight saving time (DB-09).
- Migration V10: `businesses.timezone` (existing salons get `Europe/Berlin`) and a `business_hours`
  table that rejects backwards or overlapping intervals itself. V10 enables the `btree_gist`
  extension, so the database user needs the CREATE privilege on the database (the owner has it)
  (DB-09, DB-14).
- Request logging: one line per request with the caller, status, duration and, for a failure, the
  error code and message sent back. Every log line carries the request id, which is also returned in
  the `X-Request-Id` header (exposed to CORS clients). Bodies, query values and full emails are never
  logged (BE-42).
- Event logs for every create, update and delete in the services, for sign-in and sign-out, and a
  masked email for each failed sign-in (BE-42).
- Under the `prod` profile (`SPRING_PROFILES_ACTIVE=prod`), logs also go to
  `${LOG_PATH:-logs}/salon-backend.log`, rolled daily and at 50 MB and kept for 30 days, 2 GB at most
  (BE-42).
- `currency` on businesses (ISO 4217, default `EUR`): every price at a salon is in its currency.
  Optional on create and update; omitting it on update keeps the stored one (DB-08).
- Customers API at `/api/v1/businesses/{businessId}/customers` (list, get, create, replace, delete).
  ADMIN and EMPLOYEE of the salon can list, view and add customers; only ADMIN can edit or delete
  them; SUPER_ADMIN can do all of it for any salon (DB-03).
- Migration V8: a `customers` table replaces `business_customers`; existing links are copied over
  with each customer's name, email and first phone number (DB-03).
- Migration V9: every timestamp column is `timestamptz` (DB-09); coordinates are `NUMERIC(9,6)`
  with range checks, and stored values that aren't a valid pair are cleared (DB-07); prices can't
  be negative and salons get a `currency` column (DB-08). V9 stops with an error if a service
  already has a negative price.

### Fixed
- A path or query parameter of the wrong type, e.g. `/businesses/glow`, is a 400 naming the parameter
  instead of a 500.
- Leaving `marketing_consent` out of a customer request means no consent; it used to fail as
  "Malformed JSON request body".
- `users.updated_at` is set on every update, and user responses now include it (BE-12).
- Emails are unique regardless of case, and sign-in accepts any casing of the email (DB-10).
- Two owners can share a contact value, e.g. a salon's phone that is also its owner's; one owner
  still can't list the same value twice (DB-05).

### Changed
- Startup stops with an error, instead of migrating, when the database has tables but no Flyway
  history: it's the wrong database, or a restore that lost `flyway_schema_history`. It used to be
  marked as already at V1 and migrated from V2. `app.flyway.baseline-on-migrate: true` adopts a
  database that predates Flyway, once. A migration file whose name Flyway can't parse, such as
  `V14_name.sql`, also stops startup instead of being skipped (BE-38).
- Deleting a service, staff member or customer keeps the row, stamped with `deleted_at`, so
  appointments can keep pointing at it. It is gone from every read as before, and reading, editing or
  deleting it again is a 404. Someone taken off the staff can be added again, as a new staff record.
  Deleting a salon still deletes everything that belongs to it (DB-13).
- Removing a staff member, service or customer that still has upcoming appointments is refused with
  409 until those are cancelled or moved. Deleting a user whose staff record is on any appointment is
  refused with 409 (DB-13).
- Migration V4: `users.business_id` is a foreign key to `businesses`, and a `set_updated_at()`
  trigger maintains `updated_at` on every table (DB-01, DB-11).
- Migration V5: every address and contact must have exactly one owner and is deleted with it; contact
  values are unique per owner; the email index is on `lower(email)`; every foreign-key column is
  indexed (DB-05, DB-06, DB-10, DB-12). V5 removes ownerless addresses/contacts, and stops with an
  error if two users' emails differ only by case or a row has two owners.
- Dropped the unused `spring-boot-starter-data-jpa` for `spring-boot-starter-jdbc`; Hibernate no
  longer starts. `@Transactional` is Spring's (BE-36).
- Prices are `BigDecimal` end to end instead of `double`, so they are exact and keep their two
  decimals in JSON (DB-08).
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
