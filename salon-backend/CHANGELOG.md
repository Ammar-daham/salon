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
- `GET /api/v1/businesses`, `GET /api/v1/users` and `GET /api/v1/businesses/{id}/customers` answer
  one page instead of an array: `{"items": [...], "page": 1, "size": 20, "total_items": 57,
  "total_pages": 3}`. `page` counts from 1 and `size` is 1 to 100, 20 if left out; anything else is a
  400. A page past the end has no items but the same totals (BE-15).
- `DELETE /api/v1/businesses/{id}` returns 409 `CONFLICT` while the salon still has staff; remove or
  move them first (BE-17).
- Address `latitude`/`longitude` are JSON numbers instead of strings, in range, and given together
  or not at all; input is rounded to 6 decimals (DB-07).
- A service `price` is required (it used to default to 0 when omitted) and accepts at most 2
  decimals (DB-08).
- `GET /api/v1/addresses` and `GET /api/v1/contacts` give an ADMIN only their own salon's and its
  users' records; they used to list every salon's. A SUPER_ADMIN still gets them all.
- `GET /api/v1/businesses/{id}/staff` answers 404 for a salon that doesn't exist, instead of an empty
  list.
- A user, or a user's address or contact, that the caller may not see is a 404 instead of a 403, on
  `GET`, `PUT` and `DELETE` of `/api/v1/users/{id}`, `/api/v1/addresses/{id}` and
  `/api/v1/contacts/{id}`. A salon's address or contact, which anyone signed in may read, is still a
  403 to someone who may not change it.
- `PUT /api/v1/businesses/{id}` with a name another salon already has answers 409 `CONFLICT`, as
  `POST` does, instead of a 400.
- `PUT /api/v1/contacts/{id}` with a value the owner already has on another contact answers 409
  `CONFLICT` with "Contact already exists.", as `POST /api/v1/users` and `POST /api/v1/businesses`
  do, instead of a 400.
- `GET /api/v1/customers?business_id=<id>` answers 404 for a salon that doesn't exist, as
  `GET /api/v1/businesses/{id}/customers` does, instead of 200 with an empty page.
- `POST /api/v1/businesses/{id}/services` answers 404 for a salon that doesn't exist, instead of a 400
  from the foreign key.
- `GET /api/v1/businesses/{id}/appointments` answers one page, like the other lists, instead of an
  array of every match: without `from` and `to` that was the salon's whole history. It takes `page`
  and `size`, `sort=starts_at` (the default) or `-starts_at` for latest first, and a new `status`
  filter.
- `PUT /api/v1/businesses/{id}` replaces every detail, as the other `PUT`s do: `image`, `currency` and
  `timezone` are required, and `image` can't be blank, as on create. Left out, they used to keep their
  stored value. Leaving `description` out still clears it. The status is no longer changed here: a
  SUPER_ADMIN changes it with the new `PUT /api/v1/businesses/{id}/status`, `{"status": "APPROVED"}`,
  which answers with the salon, and anyone else gets 403 there. A `status` in the details is ignored,
  so it no longer earns an ADMIN a 403.
- `PUT /api/v1/users/{id}` changes only the name and no longer takes a `role`. It used to require
  one from every caller, then ignore it unless an ADMIN or SUPER_ADMIN sent it. An ADMIN or
  SUPER_ADMIN changes a role with the new `PUT /api/v1/users/{id}/role`, `{"role": "EMPLOYEE"}`, which
  answers with the user, and anyone else gets 403 there. A `role` sent to `PUT /api/v1/users/{id}` is
  ignored.
- Every `DELETE` answers 204 instead of 200, still with no body: `/api/v1/businesses/{id}`, and its
  `services/{serviceId}`, `staff/{staffId}`, `staff/{staffId}/time-off/{timeOffId}` and
  `customers/{customerId}`, and `/api/v1/users/{id}`, `/api/v1/addresses/{id}` and
  `/api/v1/contacts/{id}`.
- `GET /api/v1/businesses/{id}/customers` answers 400 to `sort=business_name` or `-business_name`,
  as to any other order it doesn't take. Every customer on it belongs to the one salon, so it was the
  same as `sort=name`. `GET /api/v1/customers` still sorts by `business_name`.

### Security
- An ADMIN can't change the role of a SUPER_ADMIN who belongs to their salon: only a SUPER_ADMIN may
  grant SUPER_ADMIN or take it away. An ADMIN used to be stopped only from granting it.
- Addresses and contacts are scoped to the caller's salon. A salon's may be changed or deleted only
  by an ADMIN of that salon or a SUPER_ADMIN, and a user's read, changed or deleted only by the
  people who may see that user on `/users`: the user, an ADMIN of their salon or a SUPER_ADMIN. Any
  other salon's ADMIN now gets 403; it used to be let through. A salon's address and contacts are
  still readable by anyone signed in.
- `GET /api/v1/businesses/{id}/staff`, `GET .../staff/{staffId}` and `GET .../services/{serviceId}`
  answer only those who may see the salon, as `GET /api/v1/businesses/{id}` does: a salon that isn't
  APPROVED is a 404 to anyone but admins and the salon's own users. Anyone signed in, customers
  included, used to be able to read an unapproved salon's staff names and services.
- User, address and contact ids can't be probed: one the caller may not see gets the same 404 as one
  that doesn't exist. A 403 used to tell them it was there.
- Under the `prod` profile the session cookie is `Secure`, `HttpOnly` and `SameSite=Lax`, a request
  that didn't come over HTTPS is refused with 403 (all but `/actuator/health`), and HTTPS responses
  carry `Strict-Transport-Security`. A session ends after an explicit 30 minutes without a request,
  `SALON_SESSION_TIMEOUT` (BE-25).
- Under the `prod` profile the `X-Forwarded-For` and `X-Forwarded-Proto` of a proxy on a private
  network address are believed, so HTTPS is recognised and the sign-in limits count each client's
  address instead of the proxy's (BE-22, BE-25).
- A CORS preflight may ask only for `Content-Type`, `Accept` and `X-Request-Id`, the headers the
  admin panel sends; it used to allow any header (BE-29).
- There's still no CSRF token, and `SecurityConfig` now says why that holds and when it stops holding:
  any request whose `Origin` isn't in `app.cors.allowed-origins` is refused with 403 before it runs,
  including the POSTs a form or `fetch()` on another site can send without a preflight. A `*` in
  `allowed-origins` stops startup (BE-28).
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
- A `Dockerfile`: the API built with the Gradle wrapper on JDK 21 and run on a Java 21 runtime, as
  a non-root user, on port 8081, with a health check on `/actuator/health`. It's configured from the
  environment; a developer's `application.yml` is never copied into the image (BE-40).
- The paged lists take `q`, a search that ignores case, and `sort`, a key or `-key` for the other
  way round. Salons: `q` matches the name, description or a city; `status` narrows to one status;
  `sort` is `name` (the default), `status` or `created_at`. Users: `q` matches the name or email;
  `role` narrows to one role; `sort` is `name` (the default), `email`, `role` or `created_at`.
  Customers: `q` matches the name, email or phone; `sort` is `name` (the default, last name first),
  `created_at` or `business_name`. An unknown `sort`, `status` or `role` is a 400 (BE-15).
- `GET /api/v1/customers`, paged the same way, lists customers across salons for a super admin, or
  one salon's with `business_id`. Salon staff get only their own salon's, and naming another is a
  403. `GET /api/v1/customers/{id}` reads one without its salon in the path; another salon's is a
  404 (BE-15).
- Customers read back with `business_id` and `business_name` (BE-15).
- A staff record says who it is: `first_name` and `last_name` for everyone who can see the roster,
  and `email` for the salon's admins and super admins only (null for anyone else, as `/users` hides
  it from them). `POST .../staff` answers with the names too (BE-15).
- A `prod` profile, `application-prod.yml`, configured from the environment. `SALON_DB_URL`,
  `SALON_DB_USERNAME`, `SALON_DB_PASSWORD`, `SALON_ALLOWED_ORIGINS`, `SALON_PASSWORD_RESET_LINK`,
  `SALON_MAIL_FROM`, `SALON_SMTP_HOST`, `SALON_SMTP_USERNAME` and `SALON_SMTP_PASSWORD` are required,
  and a missing one stops startup naming it; `SALON_SMTP_PORT` (587), `SALON_DB_POOL_SIZE` (10) and
  `SALON_SESSION_TIMEOUT` (30m) have defaults. It requires an SMTP server with STARTTLS, gives mail 10
  seconds to answer, listens on 8081, and adds `/actuator/health/liveness` and `/readiness` (BE-30).
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
- Every `PUT` answers 200 with the record as stored, as `PUT` on an appointment already did, instead
  of with no body: `/api/v1/businesses/{id}`, and its `services/{serviceId}`, `staff/{staffId}`,
  `staff/{staffId}/time-off/{timeOffId}` and `customers/{customerId}`, and `/api/v1/users/{id}`,
  `/api/v1/addresses/{id}` and `/api/v1/contacts/{id}`. It's the same body as a `GET` of the record
  answers, so a client needn't read it again.
- `POST /api/v1/businesses/{id}/addresses` and `POST /api/v1/businesses/{id}/contacts` add an
  address or contact to a salon that already exists, for an ADMIN of the salon or a SUPER_ADMIN. They
  take the same body as `PUT /api/v1/addresses/{id}` and `PUT /api/v1/contacts/{id}` and answer 201
  with the record as stored, and `/api/v1/addresses/{id}` or `/api/v1/contacts/{id}` in `Location`.
  A salon that doesn't exist is a 404, and a contact value the salon already has is a 409. A salon's
  addresses and contacts used to be addable only in `POST /api/v1/businesses`.
- `GET /api/v1/users?business_id=` lists one salon's users for a SUPER_ADMIN, as
  `GET /api/v1/customers?business_id=` does for customers: a salon that doesn't exist is a 404. An
  ADMIN still gets only their own salon's, and naming another is a 403.

### Fixed
- `app.datasource.pool-size` in the template never did anything, so the pool always had Hikari's
  default of 10 connections. The setting is `maximum-pool-size` (BE-30).
- A path or query parameter of the wrong type, e.g. `/businesses/glow`, is a 400 naming the parameter
  instead of a 500.
- Leaving `marketing_consent` out of a customer request means no consent; it used to fail as
  "Malformed JSON request body".
- `users.updated_at` is set on every update, and user responses now include it (BE-12).
- Emails are unique regardless of case, and sign-in accepts any casing of the email (DB-10).
- Two owners can share a contact value, e.g. a salon's phone that is also its owner's; one owner
  still can't list the same value twice (DB-05).
- A taken salon name is reported as "Business with name … already exists." on create and update.
  Creating one used to answer the generic "Business could not be created due to a conflict."
- `POST /api/v1/users`, `POST /api/v1/businesses` and `POST /api/v1/businesses/{id}/services` answer
  with the record as stored, the same body a `GET` of it answers, as the other creates already did. A
  new user's `created_at` and a new service's are no longer null, a new salon's is the database's
  time instead of the server's, a new user's `addresses` and `contacts` are `[]` rather than null when
  none were sent, and a new salon's `services` is `[]` rather than null.
- A user's address names the user in `user_id` wherever it's read: in `GET /api/v1/addresses` and in
  the `addresses` of a user. It used to be filled only on `GET /api/v1/addresses/{id}` and in the
  answer to `PUT` on it, and was null everywhere else. A salon's address still has a null `user_id`.
- A customer's `first_name` and `last_name` are trimmed, as `email`, `phone` and `notes` already were,
  and every field is trimmed before it's checked. A padded email such as `" ella@example.test "` used
  to be refused as not an email address, and a padded name or note could fail its length limit.
- Validation messages name each field as the body does, such as `first_name`,
  `addresses[0].postal_code` and `hours[0].closes_at`, instead of by its Java name (`firstName`,
  `addresses[0].postalCode`, `hours[0].closesAt`). A rule over several fields is named by where it
  is, as in `addresses[0] latitude and longitude must be given together`.
- `GET` and `DELETE /api/v1/users/{id}` take any user id, as `PUT` does: one past 2147483647 is a
  404 if there's no such user, instead of a 400 "id has an invalid value". `PUT /api/v1/users/{id}`
  and `PUT /api/v1/users/{id}/role` no longer cut such an id down to an `int`, which looked up a
  different user: `PUT /api/v1/users/4294967300/role` answered 200 with user 4, unchanged.
- A new password over 72 bytes in UTF-8 is a 400, "must be at most 72 bytes in UTF-8, where a letter
  outside English takes 2 or more", on `POST /api/v1/users`, `POST /api/v1/auth/change-password` and
  `POST /api/v1/auth/reset-password`, instead of a 500. The limit counted characters, but BCrypt takes
  at most 72 bytes, so 40 × `ä`, 40 characters and 80 bytes, passed the check and then failed.
- A salon or service id past 2147483647 is a 404 if there's no such salon or service, wherever it's
  given: in a path, in `?business_id=` or in a user's `business_id`. The services cut it down to an
  `int`, which found a different one: `GET /api/v1/businesses/4294967297/hours` answered 200 with
  salon 1's timezone, and `GET /api/v1/businesses/1/services/4294967297/availability` with the slots
  of service 1. `/api/v1/businesses/{id}` and `.../services/{serviceId}` took an `int`, so there the
  same id was a 400 "id has an invalid value".
- `POST /api/v1/businesses/{id}/addresses` and `.../contacts` answer with the record they created once
  ids pass 2147483647. They read it back by its id cut down to an `int`, so with the next id 4294967297
  they answered with address or contact 1. `/api/v1/addresses/{id}` and `/api/v1/contacts/{id}` take
  any id: one past 2147483647 is a 404 if there's no such record, instead of a 400.
- Whether an appointment has started, which `PUT .../appointments/{id}/status` checks before it may
  become `COMPLETED` or `NO_SHOW`, goes by instants. It compared times on the salon's clock, so in
  the hour that happens twice when daylight saving time ends, one at 02:30 the first time round was
  refused at 02:10 the second time round, 40 minutes after it started, and one at 02:20 the second
  time round was allowed at 02:40 the first time round, 40 minutes before it started.
- A SUPER_ADMIN can create another SUPER_ADMIN who belongs to no salon, as the first one doesn't:
  `POST /api/v1/users` takes a new SUPER_ADMIN without `business_id`. It was a 400 for every new
  user, and still is for any other role, now saying "business_id is required for role EMPLOYEE"
  instead of "businessId is required when a super admin creates a user". A `business_id` given for a
  SUPER_ADMIN must still be a salon.
- `DELETE /api/v1/businesses/{id}/staff/{staffId}` clears the staff member's shifts, services and
  time off. They stayed in the database, where nothing could reach them again: a staff member taken
  on again is a new staff record. Their appointments still stay, as the salon's history. Migration
  V14 clears what earlier removals left behind.
- `DELETE /api/v1/businesses/{id}/services/{serviceId}` clears who performs the service, which
  likewise stayed in the database out of reach. Its appointments stay. Migration V15 clears what
  earlier removals left behind.

### Changed
- Which salons a non-admin sees, the approved ones and their own, is decided in the query, so the
  totals count only those (BE-15).
- Listing salons or users reads their addresses, contacts and services in one query each instead of
  one per salon or user, so ten salons take 11 queries rather than 32. Each one's addresses, contacts
  and services come in id order (BE-15).
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
