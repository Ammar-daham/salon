# Salon Platform — Audit Findings & MVP Roadmap

Audit date: 2026-09-23 (supersedes the 2026-09-17 version of this file).
Scope: `salon-backend` (Java 21 / Spring Boot 4 / JdbcTemplate / PostgreSQL / Flyway) and the
database schema. Frontend findings live in [`../admin-panel/AUDIT_FINDINGS.md`](../admin-panel/AUDIT_FINDINGS.md);
the phased MVP plan, DB redesign and future features below cover **both** apps.

Severity legend: **Critical** (cross-tenant access / privilege escalation / data loss), **High** (bug
likely hit in normal use), **Medium** (real bug, narrower trigger), **Low** (hygiene).

**How to read this file.** Every finding has an ID (`BE-xx` backend, `DB-xx` database) and ends with
**→ the branch that fixes it**, linked to that branch's row in
[`VERSION_CONTROL_GUIDE.md` §9](../VERSION_CONTROL_GUIDE.md#9-branch-plan-for-the-mvp-roadmap).
Each row there links back here. Frontend findings (`FE-xx`) use the same scheme.
A ✅ next to a finding's ID means its fix has landed; the parenthetical *(fixed: …)* / *(done: …)* note
on that finding describes what changed and which test guards it.
So far: ✅ BE-01, ✅ BE-02, ✅ BE-03, ✅ BE-04, ✅ BE-05, ✅ BE-31, ✅ BE-33.

## 0. Honest summary

- **Auth works; authorization doesn't.** Login, sessions and BCrypt are sound. But every write is gated
  only on `hasAnyRole("ADMIN","SUPER_ADMIN")` — there is no business scoping anywhere, so one salon's
  admin can modify every other salon, and can promote themselves to platform super admin.
- **The product doesn't exist yet.** There are no appointments, staff schedules, availability or
  customer records ([DB-14](#db-14)). Today this is a salon directory with price lists. Booking *is* the MVP.
- **Zero meaningful tests** ([BE-32](#be-32)). The only test is the empty `contextLoads()`. The git
  history is a record of shipped bugs (broken SQL, swapped fields, null crashes) that tests would have caught.

## 1. Verified live exploits (Critical)

Reproduced on 2026-09-23 against a running backend loaded with `db/seed_data.sql`, logged in as
**Anna — ADMIN of Glow Beauty Studio (business 1)**:

| ID | Request | Result | Fixed by |
|---|---|---|---|
| <a id="be-01"></a>✅ BE-01 | `GET /api/v1/users` | **200** — all 16 users platform-wide, including other salons' staff emails | [`fix/salon-backend-user-scoping`](../VERSION_CONTROL_GUIDE.md#br-0-6) |
| <a id="be-02"></a>✅ BE-02 | `PUT /api/v1/businesses/3` `{"status":"APPROVED", ...}` on another salon | **200** — foreign salon approved and its description overwritten | [`fix/salon-backend-business-tenant-scoping`](../VERSION_CONTROL_GUIDE.md#br-0-4) |
| <a id="be-03"></a>✅ BE-03 | `PUT /api/v1/businesses/1/services/4` (service 4 belongs to business 2) | **200** — foreign service renamed, price set to 0.01 | [`fix/salon-backend-service-ownership`](../VERSION_CONTROL_GUIDE.md#br-0-5) |
| <a id="be-04"></a>✅ BE-04 | `PUT /api/v1/users/{self}` `{"role":"SUPER_ADMIN", ...}` | **200** — **Anna is now a platform super admin** | [`fix/salon-backend-role-escalation`](../VERSION_CONTROL_GUIDE.md#br-0-3) |

Root causes:
- ✅ **BE-01** — `UserService.getAllUsers` has no business filter; admin-only is enforced, but *which* admin isn't.
  *(fixed: `getAllUsers` now takes the caller — a SUPER_ADMIN still sees the whole platform, any other admin
  gets only their own business's users (new `UserDao.getUsersByBusinessId`). Guarded by
  `adminOnlySeesUsersOfTheirOwnBusiness` and `superAdminSeesUsersAcrossAllBusinesses` in `AuthorizationRulesTest`.)*
- ✅ **BE-02** — [`SecurityConfig.java:88-90`](src/main/java/com/example/salon/security/SecurityConfig.java) gates
  POST/PUT/DELETE on `/api/v1/**` only by role. `BusinessController` lets any ADMIN create, update
  (including `status`) and delete **any** business. Approving/suspending a salon should be SUPER_ADMIN-only.
  *(fixed: `BusinessService.updateBusinessById`/`deleteBusiness` now take the caller. A non-SUPER_ADMIN
  may only touch their own business (`business_id` must match the path id) and may never change `status` —
  approving/rejecting/suspending is reserved for SUPER_ADMIN; re-sending the unchanged status is still
  allowed so the panel's full-object PUT keeps working. Guarded by `adminCannotApproveAnotherSalon`,
  `adminCannotChangeEvenTheirOwnBusinessStatus`, `adminCannotDeleteAnotherSalon`, `superAdminCanApproveASalon`
  in `AuthorizationRulesTest`. Still open, deliberately out of scope here: `POST /businesses` creation
  (BE-40/registration flow) and hiding non-APPROVED salons from non-admins on `GET /businesses` (FE-11).)*
- ✅ **BE-03** — [`BusinessServiceController`](src/main/java/com/example/salon/controller/BusinessServiceController.java)
  PUT and DELETE ignore the `businessId` path variable and act on the bare service id.
  *(fixed: PUT/DELETE now bind `businessId` and pass the caller. `BusinessSalonServiceService` requires the
  caller to own the path business (`AccessControl.requireBusinessAccess`, shared with BE-02) and verifies the
  service actually belongs to that business (via `getServiceById`, which 404s otherwise) before mutating it.
  `POST /{businessId}/services` is scoped the same way so an admin can't inject a service into another salon.
  Guarded by `adminCannotEditAnotherSalonsServiceThroughTheirOwnBusinessPath`, `adminCannotDeleteAnotherSalonsService`,
  `adminCannotCreateAServiceUnderAnotherSalon` and `adminCanEditTheirOwnSalonsService` in `AuthorizationRulesTest`.)*
- ✅ **BE-04** — [`UserService.updateUserById`](src/main/java/com/example/salon/service/UserService.java) only locks
  the role for non-admins. `addUser` blocks minting a SUPER_ADMIN; `updateUserById` does not.
  *(fixed: `updateUserById` now rejects an update that sets `role` to `SUPER_ADMIN` unless the caller is
  a SUPER_ADMIN, mirroring `addUser`'s guard. Guarded by `AuthorizationRulesTest.adminCannotPromoteThemselvesToSuperAdmin`.
  BE-05 — the missing business scoping on the same method — is still open and tracked separately.)*
- <a id="be-05"></a>✅ **BE-05 — User update/delete aren't business-scoped either.** An ADMIN can edit or
  delete users (including super admins) of every salon via `PUT/DELETE /users/{id}`.
  **→ [`fix/salon-backend-user-scoping`](../VERSION_CONTROL_GUIDE.md#br-0-6)**
  *(fixed: `getUserById`/`updateUserById`/`deleteUserById` now enforce `AccessControl.requireUserAccess`
  against the loaded record — self, or an admin acting within their own business, or a super admin —
  so cross-salon reads/edits/deletes return 403. Guarded by `adminCannotEditUsersOfAnotherSalon`,
  `adminCannotDeleteUsersOfAnotherSalon` and `adminCanEditUsersOfTheirOwnSalon` in `AuthorizationRulesTest`.)*

## 2. Correctness bugs

### Critical
- <a id="be-06"></a>**BE-06 — IDOR via nested payloads.** `BusinessDataAccessService.updateBusinessById/deleteBusiness`
  and `UserDataAccessService.updateUserById/deleteUserById` act on client-supplied child ids (addresses,
  contacts, services) with no check that the child belongs to the parent in the path.
  `PUT /businesses/1` with `{"contacts":[{"id":999,...}]}` edits contact 999 whoever owns it.
  **→ [`fix/salon-backend-nested-child-writes`](../VERSION_CONTROL_GUIDE.md#br-0-9)**
- <a id="be-07"></a>**BE-07 — Adding a new child to an existing business/user crashes.** Nested
  `address.getId()` / `contact.getId()` / `service.getId()` (a `null` `Long` for new rows) is auto-unboxed
  into `updateXById(long id, ...)` → NPE → masked as `400 Null argument`. There is no working way to add
  an address/contact to an existing business or user.
  **→ [`fix/salon-backend-nested-child-writes`](../VERSION_CONTROL_GUIDE.md#br-0-9)**
- <a id="be-41"></a>**BE-41 — Non-admins can't read *any* address or contact, not even their own.**
  Found by the first test run on 2026-09-23. `AddressDataAccessService.getAddressById` and
  `ContactDataAccessService.getContactById` never select `business_id`/`user_id`, so the ownership check
  in `AddressService`/`ContactService` always sees `null` owners: a salon's public address and a user's
  own personal contact both return 403 to everyone but admins. The "owner or admin" rule from commit
  79b3e2e has never worked. Covered by two `@Disabled` tests in `KnownIssuesTest`.
  **→ [`fix/salon-backend-address-contact-ownership`](../VERSION_CONTROL_GUIDE.md#br-0-13)**
- <a id="be-08"></a>**BE-08 — `Staff` constructor drops fields.** [`Staff.java:14-16`](src/main/java/com/example/salon/model/Staff.java)
  only assigns `id`. Unreachable today (no staff API), but a live defect the moment it's wired up.
  **→ [`feature/salon-backend-staff-api`](../VERSION_CONTROL_GUIDE.md#br-1-6)**

### High
- <a id="be-09"></a>**BE-09 — Duplicate-key swallowed → false success.** `BusinessSalonServiceService.createServiceForBusiness`
  has an empty `catch (DuplicateKeyException)` and returns the service with no id as if it succeeded.
  `BusinessService.addBusiness` only rethrows for two message substrings and silently succeeds on any other.
  **→ [`fix/salon-backend-error-handling`](../VERSION_CONTROL_GUIDE.md#br-0-10)**
- <a id="be-10"></a>**BE-10 — DELETE trusts the request body.** `deleteBusiness` / `deleteUserById` delete
  children from the client-sent object instead of the canonical record. An empty body orphans every
  child, and because `contacts.value` is globally UNIQUE an orphaned phone number becomes unusable
  platform-wide. **→ [`fix/salon-backend-server-side-deletes`](../VERSION_CONTROL_GUIDE.md#br-0-7)**,
  then [`refactor/admin-panel-drop-delete-bodies`](../VERSION_CONTROL_GUIDE.md#br-0-8)
- <a id="be-11"></a>**BE-11 — User delete cleanup is `if / else if`.** [`UserDataAccessService.deleteUserById`](src/main/java/com/example/salon/dao/UserDataAccessService.java):
  a user with both contacts and addresses only has contacts removed; with neither it returns `-1`
  even though the user row was already deleted.
  **→ [`fix/salon-backend-server-side-deletes`](../VERSION_CONTROL_GUIDE.md#br-0-7)**
- <a id="be-12"></a>**BE-12 — `users.updated_at` is never set.** The UPDATE omits `updated_at = now()`,
  unlike every other DAO. **→ [`fix/salon-backend-schema-integrity`](../VERSION_CONTROL_GUIDE.md#br-1-2)**
  (the `updated_at` trigger, [DB-11](#db-11))
- <a id="be-13"></a>**BE-13 — `201 Created` / `Location` never sent.** Controllers build
  `ResponseEntity.created(...)` then call `.getBody()`, discarding status and header; every create returns 200.
  **→ [`fix/salon-backend-created-responses`](../VERSION_CONTROL_GUIDE.md#br-1-5)**
- <a id="be-14"></a>**BE-14 — Stale sessions.** The session stores a snapshot of the `User`
  (`AuthenticatedUser`). A demoted, re-scoped or deleted user keeps their old role and access until the
  session expires. **→ [`fix/salon-backend-session-hardening`](../VERSION_CONTROL_GUIDE.md#br-0-11)**

### Medium
- <a id="be-15"></a>**BE-15 — N+1 queries, no pagination.** `getBusinesses()` runs 1 + 3N queries;
  `getAllUsers()` 1 + 2N. No list endpoint pages or filters.
  **→ [`feature/repo-pagination`](../VERSION_CONTROL_GUIDE.md#br-3-5)**
- <a id="be-16"></a>**BE-16 — Deleting a business hard-deletes rows in the shared `services` table**
  instead of unlinking them. **→ [`refactor/repo-services-owned-by-business`](../VERSION_CONTROL_GUIDE.md#br-1-3)**
  ([DB-02](#db-02))
- <a id="be-17"></a>**BE-17 — Deleting a business leaves its staff dangling**, because `users.business_id`
  has no FK. **→ [`fix/salon-backend-schema-integrity`](../VERSION_CONTROL_GUIDE.md#br-1-2)** ([DB-01](#db-01))
- <a id="be-18"></a>**BE-18 — Typo'd and discarded error codes.** `ContactService` returns `"NOT_FOUNT"`;
  `ErrorCode.NOT_FOUND`'s message is `"NOT-FOUNT_404"`; `ErrorCode`'s constructor discards its
  message/code args, so the frontend gets ad-hoc codes like `"400"`.
  **→ [`fix/salon-backend-error-handling`](../VERSION_CONTROL_GUIDE.md#br-0-10)**

### Low
- <a id="be-19"></a>**BE-19 — Naming inconsistencies.** Mixed `int`/`long` id parameters across DAO
  interfaces; `delectContactById` typo. **→ No dedicated branch** — fix whenever a branch touches that file.
- <a id="be-20"></a>**BE-20 — Style nits.** `jdbcTemplate` non-`final` in `SalonServiceDataAccessService` /
  `BusinessServiceDataAccessService`; stray `;` in `SalonServiceDataAccessService`; `SELECT *` only in
  `UserDataAccessService`; row-count variables named `userId`.
  **→ No dedicated branch** — fix whenever a branch touches that file.
- <a id="be-21"></a>**BE-21 — Copy-pasted nested-collection sync** between `BusinessDataAccessService` and
  `UserDataAccessService`. **→ [`fix/salon-backend-nested-child-writes`](../VERSION_CONTROL_GUIDE.md#br-0-9)**
  (removing nested writes removes the duplication)

### Fixed since the previous audit
- `users.business_id` is now read and written (`userRowMapper`, `addUser`) and exposed via `AuthUserResponse`.
- Wire format is consistently snake_case; business endpoints are plural (`/api/v1/businesses`).
- Bulk `GET /users|/addresses|/contacts` restricted to admins. Single-record user access is
  self-or-admin (still not *business*-scoped — see §1). Single-record address/contact access was
  *meant* to be owner-or-admin but is broken — see [BE-41](#be-41).

## 3. Security

### High
- **Tenant isolation / privilege escalation** — [BE-01](#be-01) to [BE-05](#be-05). The single most serious problem.
- <a id="be-22"></a>**BE-22 — No rate limiting or account lockout** on `POST /api/v1/auth/login`.
  **→ [`feature/salon-backend-login-rate-limiting`](../VERSION_CONTROL_GUIDE.md#br-3-1)**
- <a id="be-23"></a>**BE-23 — No server-side validation.** `spring-boot-starter-validation` is a dependency
  but there are zero `@Valid`/`@NotBlank`/`@Email`/`@Size` annotations. Password strength and email
  format are unchecked server-side; request bodies bind straight onto domain models (mass assignment
  of `role`, `status`, ids). **→ [`feature/salon-backend-request-validation`](../VERSION_CONTROL_GUIDE.md#br-1-4)**
- <a id="be-24"></a>**BE-24 — Session fixation.** `AuthController.login` saves the security context
  manually and never rotates the session id. Call `request.changeSessionId()` before `saveContext`.
  **→ [`fix/salon-backend-session-hardening`](../VERSION_CONTROL_GUIDE.md#br-0-11)**
- <a id="be-25"></a>**BE-25 — Secure cookie not enforced.** No `server.servlet.session.cookie.secure: true`,
  no HTTPS requirement for production; session timeout is the implicit 30-minute default.
  **→ [`chore/salon-backend-production-config`](../VERSION_CONTROL_GUIDE.md#br-3-4)**

### Medium
- <a id="be-26"></a>**BE-26 — SQL built via `%s` column interpolation.** `getAddressesByColumn` /
  `getContactsByColumn` are public and only fed literals today; one careless caller away from SQL
  injection. **→ [`feature/repo-pagination`](../VERSION_CONTROL_GUIDE.md#br-3-5)** (fixing the N+1
  rewrites exactly these methods)
- <a id="be-27"></a>**BE-27 — NPEs silently converted to 400** with no logging — hides real server bugs
  as client errors. **→ [`fix/salon-backend-error-handling`](../VERSION_CONTROL_GUIDE.md#br-0-10)**
- <a id="be-28"></a>**BE-28 — CSRF is CORS-only** (disabled, relying on a pinned origin). Defensible, but
  any loosening of `app.cors.allowed-origins` reopens CSRF on every write. Document it or add a
  double-submit token. **→ [`chore/salon-backend-production-config`](../VERSION_CONTROL_GUIDE.md#br-3-4)**
- **Case-sensitive email uniqueness** — see [DB-10](#db-10).

### Low
- <a id="be-29"></a>**BE-29 — CORS `allowedHeaders("*")` with `allowCredentials(true)`.**
  **→ [`chore/salon-backend-production-config`](../VERSION_CONTROL_GUIDE.md#br-3-4)**
- <a id="be-30"></a>**BE-30 — DB credentials only via the gitignored `application.yml`**; no documented
  env-var override for production. **→ [`chore/salon-backend-production-config`](../VERSION_CONTROL_GUIDE.md#br-3-4)**

### Confirmed solid
BCrypt hashing; `password`/`passwordHash` never serialized; generic login error (no user enumeration);
logout invalidates the session; `addUser` blocks non-super-admins from minting SUPER_ADMIN and scopes
new users to the caller's business; `application.yml` is not tracked in git.

## 4. Architecture, testing, ops

- <a id="be-31"></a>✅ **BE-31 — No CI.** Nothing runs tests or builds on a PR.
  **→ [`chore/repo-ci-pipeline`](../VERSION_CONTROL_GUIDE.md#br-0-2)**
  *(done: [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) runs on every PR and on pushes to
  `main` — `salon-backend` validates the Gradle wrapper and runs `./gradlew build` (compile, the
  Testcontainers suite, war), `admin-panel` runs `npm ci`, lint, typecheck and `next build`. Both
  jobs run on every PR; neither is path-filtered, so they work as required status checks. Making
  them required in the branch-protection rules for `main` is a repo-settings step and still has to be
  done by hand.)*
- <a id="be-32"></a>**BE-32 — Testing: effectively zero.** Start with MockMvc + Testcontainers tests for
  the four exploits in §1. **→ [`test/salon-backend-integration-test-setup`](../VERSION_CONTROL_GUIDE.md#br-0-1)**
  *(in progress: integration test base, passing tests, and 2 `@Disabled` tests in `KnownIssuesTest`
  for BE-41; run them with `./gradlew test -PrunKnownIssues`)*
- <a id="be-33"></a>✅ **BE-33 — Missing `@Transactional`** on `UserService.updateUserById/deleteUserById`
  (multi-step writes). **→ [`fix/salon-backend-user-scoping`](../VERSION_CONTROL_GUIDE.md#br-0-6)**
  (it rewrites these methods)
  *(fixed: both methods are now `@Transactional`, so their access check + multi-row write commit atomically.)*
- <a id="be-34"></a>**BE-34 — No logging.** `System.out.println` in `BusinessService` and nowhere else;
  no SLF4J usage; errors are hidden in production (`include-message: never`) with nothing logged
  server-side. **→ [`fix/salon-backend-error-handling`](../VERSION_CONTROL_GUIDE.md#br-0-10)**
- <a id="be-35"></a>**BE-35 — Stub feature.** `StaffDataAccessService` returns `0`/`List.of()`/`null`,
  isn't a Spring bean, has no controller. **→ [`feature/salon-backend-staff-api`](../VERSION_CONTROL_GUIDE.md#br-1-6)**
- <a id="be-36"></a>**BE-36 — Unused dependency** `spring-boot-starter-data-jpa` (no entities;
  everything is JdbcTemplate). **→ [`chore/salon-backend-dependency-cleanup`](../VERSION_CONTROL_GUIDE.md#br-1-1)**
- <a id="be-37"></a>**BE-37 — Flyway version mismatch.** `flyway-core:10.0.0` vs
  `flyway-database-postgresql:11.0.0`; let the BOM manage both.
  **→ [`chore/salon-backend-dependency-cleanup`](../VERSION_CONTROL_GUIDE.md#br-1-1)**
- <a id="be-38"></a>**BE-38 — Flyway `baselineOnMigrate(true)`** can mask missing migrations on an
  existing database. **→ [`chore/salon-backend-production-config`](../VERSION_CONTROL_GUIDE.md#br-3-4)**
- <a id="be-39"></a>**BE-39 — No actuator/health endpoint.**
  **→ [`chore/salon-backend-production-config`](../VERSION_CONTROL_GUIDE.md#br-3-4)**
- <a id="be-40"></a>**BE-40 — No Dockerfile/compose, no README** (only `RUNNING_AND_API_GUIDE.md`).
  **→ [`chore/repo-docker-compose`](../VERSION_CONTROL_GUIDE.md#br-3-6)**

## 5. Database design review

| ID | Problem | Recommendation | Fixed by |
|---|---|---|---|
| <a id="db-01"></a>DB-01 | `users.business_id` has **no FK** | Add FK to `businesses(id)`; deleting a salon currently strands its staff. | [`fix/salon-backend-schema-integrity`](../VERSION_CONTROL_GUIDE.md#br-1-2) |
| <a id="db-02"></a>DB-02 | `services` ↔ `businesses` is many-to-many, but a service belongs to one salon | Add `services.business_id NOT NULL` FK and drop `business_service`. Fixes shared-service deletion and simplifies ownership checks. | [`refactor/repo-services-owned-by-business`](../VERSION_CONTROL_GUIDE.md#br-1-3) |
| <a id="db-03"></a>DB-03 | `business_customers` has no PK, no FKs, no code | Replace with a `customers` table (business_id, name, phone, email, notes, marketing consent). Customers needn't be `users` rows until they can log in. | [`feature/salon-backend-customers-api`](../VERSION_CONTROL_GUIDE.md#br-1-8) |
| <a id="db-04"></a>DB-04 | `staff` duplicates `users.role/business_id`, stubbed in code | Keep as the employment record (title, active, hired_at, calendar colour) with real FKs, or fold into `users`. | [`feature/salon-backend-staff-api`](../VERSION_CONTROL_GUIDE.md#br-1-6) |
| <a id="db-05"></a>DB-05 | `contacts.value` **globally UNIQUE** | Two customers can't share a family phone; a salon can't share its owner's email. Make it unique per owner or drop it. | [`fix/salon-backend-schema-integrity`](../VERSION_CONTROL_GUIDE.md#br-1-2) |
| <a id="db-06"></a>DB-06 | Polymorphic `business_id OR user_id` on `addresses`/`contacts`, unchecked | Add `CHECK (num_nonnulls(business_id, user_id) = 1)` — or put phone/email columns directly on businesses and customers and keep one address per business. | [`fix/salon-backend-schema-integrity`](../VERSION_CONTROL_GUIDE.md#br-1-2) |
| <a id="db-07"></a>DB-07 | `latitude`/`longitude` are `VARCHAR` | `NUMERIC(9,6)` (PostGIS later for "salons near me"). | [`refactor/salon-backend-column-types`](../VERSION_CONTROL_GUIDE.md#br-1-10) |
| <a id="db-08"></a>DB-08 | Java `double` for money | `BigDecimal` / `NUMERIC(10,2)` + a `currency` column (or integer cents). | [`refactor/salon-backend-column-types`](../VERSION_CONTROL_GUIDE.md#br-1-10) |
| <a id="db-09"></a>DB-09 | `timestamp` without time zone | `timestamptz` everywhere, plus `businesses.timezone` — mandatory before appointments. | [`refactor/salon-backend-column-types`](../VERSION_CONTROL_GUIDE.md#br-1-10) (column type), [`feature/salon-backend-business-hours`](../VERSION_CONTROL_GUIDE.md#br-2-1) (`timezone`) |
| <a id="db-10"></a>DB-10 | Case-sensitive email index — `Anna@x.com` and `anna@x.com` are two accounts | `UNIQUE INDEX ON users (lower(email))`. | [`fix/salon-backend-schema-integrity`](../VERSION_CONTROL_GUIDE.md#br-1-2) |
| <a id="db-11"></a>DB-11 | `updated_at` maintained by hand (and forgotten, [BE-12](#be-12)) | A single `set_updated_at()` trigger on every table. | [`fix/salon-backend-schema-integrity`](../VERSION_CONTROL_GUIDE.md#br-1-2) |
| <a id="db-12"></a>DB-12 | No indexes on FK columns | Index every `business_id`, `user_id`, `service_id`. | [`fix/salon-backend-schema-integrity`](../VERSION_CONTROL_GUIDE.md#br-1-2) |
| <a id="db-13"></a>DB-13 | Hard deletes everywhere | Soft delete (`deleted_at`) for businesses, services, staff, customers — past appointments must keep pointing at them. | [`feature/salon-backend-appointments`](../VERSION_CONTROL_GUIDE.md#br-2-3) |

<a id="db-14"></a>**DB-14 — Booking tables don't exist.** Phase 2 needs `business_hours`,
`staff_schedules`, `staff_time_off`, `staff_services`, `appointments` (customer, staff, service,
`start_at`/`end_at`, status, price snapshot, notes) with an exclusion constraint on
`tstzrange(start_at, end_at)` per staff member so the database itself prevents double-booking,
`appointment_services` (if one visit can include several services), and `audit_log`.
**→ [`feature/salon-backend-business-hours`](../VERSION_CONTROL_GUIDE.md#br-2-1),
[`feature/salon-backend-staff-schedules`](../VERSION_CONTROL_GUIDE.md#br-2-2),
[`feature/salon-backend-appointments`](../VERSION_CONTROL_GUIDE.md#br-2-3),
[`feature/salon-backend-availability`](../VERSION_CONTROL_GUIDE.md#br-2-4)**

## 6. MVP roadmap (both apps)

The branch-by-branch version of this plan, with merge order, dependencies and status, is
[`VERSION_CONTROL_GUIDE.md` §9](../VERSION_CONTROL_GUIDE.md#9-branch-plan-for-the-mvp-roadmap).

### Phase 0 — Stop the bleeding (~1 week, blocks everything)
Tenant scoping and role rules ([BE-01](#be-01)–[BE-05](#be-05)), address/contact ownership
([BE-41](#be-41)), server-side deletes ([BE-10](#be-10),
[BE-11](#be-11)), nested-child writes ([BE-06](#be-06), [BE-07](#be-07)), error handling and logging
([BE-09](#be-09), [BE-18](#be-18), [BE-27](#be-27), [BE-34](#be-34)), session hardening ([BE-14](#be-14),
[BE-24](#be-24)), and the test suite + CI that prove them ([BE-31](#be-31), [BE-32](#be-32)).

### Phase 1 — Make the core model real (1–2 weeks)
Real staff and customers APIs so the admin panel drops mock data ([BE-08](#be-08), [BE-35](#be-35),
[DB-03](#db-03), [DB-04](#db-04)); request DTOs with validation ([BE-23](#be-23)); schema integrity and
column types ([DB-01](#db-01)–[DB-12](#db-12)); 201 responses ([BE-13](#be-13)); dependency cleanup
([BE-36](#be-36), [BE-37](#be-37)).

### Phase 2 — The actual product: booking (2–4 weeks)
Opening hours, staff schedules and time off, staff ↔ service assignment, appointments with a
`BOOKED → CONFIRMED → COMPLETED / CANCELLED / NO_SHOW` status flow, a server-side availability API,
and the admin-panel Appointments, Calendar and customer history ([DB-13](#db-13), [DB-14](#db-14)).

### Phase 3 — Pre-launch hardening (~1 week)
Login rate limiting ([BE-22](#be-22)), password reset, production config ([BE-25](#be-25), [BE-28](#be-28)–[BE-30](#be-30),
[BE-38](#be-38), [BE-39](#be-39)), pagination ([BE-15](#be-15), [BE-26](#be-26)), Docker ([BE-40](#be-40)),
and admin-panel cleanup and e2e tests.

Realistic estimate for a focused solo developer: **5–8 weeks** to a sellable MVP (Phases 0–3).

## 7. Future features (post-MVP)

1. **Customer-facing booking site/widget** — the real growth engine; customer login already fits the role model.
2. **Notifications** — email/SMS confirmations and reminders (biggest lever on no-shows).
3. **Payments** — booking deposits, then POS checkout (e.g. Stripe).
4. **Subscriptions / platform billing** — charge salons; nav slots already exist.
5. **Reports** — revenue, staff utilisation, service mix, no-show rate.
6. **Per-staff permissions** — `admin-panel/src/lib/auth/permissions.ts` is already built for it.
7. Later: multi-location businesses, retail/inventory, loyalty and gift cards, reviews, waitlist,
   Google/Apple calendar sync, i18n.
