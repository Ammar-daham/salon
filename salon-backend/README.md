# salon-backend

The REST API behind Salonat, the salon platform: salons, their staff, services, opening hours, clients and
appointments, with accounts for platform admins, salon admins and staff. Spring Boot 4 on Java 21,
PostgreSQL 16, plain JDBC (`JdbcTemplate`) and Flyway migrations.

The [admin panel](../admin-panel), Salonat Pro, is its main client. What changed in each version, field by field,
is in [CHANGELOG.md](CHANGELOG.md).

## Run it with Docker

From the repository root:

```bash
docker compose up --build                # the API on http://localhost:8081
docker compose --profile demo up         # the same, plus sample data (below)
```

This starts PostgreSQL, the API, the admin panel on http://localhost:3000, and
[Mailpit](https://mailpit.axllent.org) on http://localhost:8025, which catches every email the API
sends, such as password reset links. Nothing needs installing apart from Docker. The database is kept
in a volume between runs; `docker compose down --volumes` deletes it.

A fresh database has no accounts. `--profile demo` loads
[`db/seed_data.sql`](src/main/resources/db/seed_data.sql) into it once: three salons with staff,
services and clients, and a super admin, `superadmin@salon.example.com`. Every account it creates
signs in with `Password123!`.

## Run it without Docker

You need Java 21 and PostgreSQL 16.

1. Create a database, e.g. `createdb salondb`.
2. Copy `src/main/resources/application.yml_template` to `application.yml` in the same folder and fill
   in the database user and password. `application.yml` is git-ignored: it never leaves your machine.
3. Start the API, which applies the migrations on its way up:

   ```bash
   ./gradlew bootRun
   ```

It answers on http://localhost:8081. Without a mail server, emails are written to the log instead;
the template shows how to point it at one. For sample data, run the seed once against the empty,
migrated database:

```bash
psql -d salondb -f src/main/resources/db/seed_data.sql
```

## Tests

```bash
./gradlew test
```

The tests run against a real PostgreSQL in a container (Testcontainers), so Docker has to be running.
`./gradlew test -PrunKnownIssues` also runs the tests for open findings that are expected to fail.
CI runs `./gradlew build` on every pull request.

## Configuration

Settings live under `app.*` and Spring Boot's own `spring.*` and `server.*`. In development they come
from `application.yml` (the template lists and explains them); any of them can be overridden by an
environment variable, the way `docker-compose.yml` does.

**Production** runs with `SPRING_PROFILES_ACTIVE=prod`, configured entirely from `SALON_*` environment
variables, which [`application-prod.yml`](src/main/resources/application-prod.yml) lists. A missing
one stops startup. The prod profile expects a reverse proxy in front that terminates TLS and sends
`X-Forwarded-Proto`, refuses plain HTTP apart from the health check, and needs an SMTP server.

The Docker image is built from [`Dockerfile`](Dockerfile):

```bash
docker build -t salon-backend .
docker run -p 8081:8081 -e SPRING_PROFILES_ACTIVE=prod -e SALON_DB_URL=... salon-backend
```

`GET /actuator/health` answers `UP` or `DOWN` without signing in, for a load balancer.

### The first super admin

There's no sign-up: every account is created by another admin, so the first super admin goes straight
into the database. Make a bcrypt hash of their password, for example with Docker:

```bash
docker run --rm httpd:2.4-alpine htpasswd -nbBC 10 "" 'their-password' | tr -d ':\n'
```

then insert them:

```sql
INSERT INTO users (first_name, last_name, role, email, password_hash)
VALUES ('First', 'Last', 'SUPER_ADMIN', 'admin@example.com', '<the hash>');
```

## The API in short

- Everything is under `/api/v1`, in JSON with `snake_case` fields.
- `POST /api/v1/auth/login` with `email` and `password` starts a session; the browser keeps it in the
  `JSESSIONID` cookie and sends it with every request. `GET /api/v1/auth/me` says who's signed in.
- Roles are `SUPER_ADMIN` (the platform), `ADMIN` and `EMPLOYEE` (one salon), and `CUSTOMER`. Each
  endpoint's Javadoc says who may call it.
- The lists that grow, salons, users and clients, come a page at a time:
  `{items, page, size, total_items, total_pages}`, with `?page`, `?size` (up to 100), `?q` and `?sort`.
- An error is `{errorCode, message, status, timestamp, path}`.

Every endpoint is documented in its controller. `./gradlew javadoc` builds that into a reference in
`build/docs/javadoc`: open `index.html` and start at the `com.example.salon.controller` package,
whose page covers signing in, roles, lists and errors.

## Code layout

| Package | What's in it |
|---|---|
| `controller` | The HTTP endpoints |
| `service` | Business rules and who may do what |
| `dao` | The SQL, one class per table |
| `model`, `dto` | What's stored and returned; what requests may send |
| `security` | Sign-in, sessions, CORS and the sign-in limits |
| `paging`, `mail`, `logging`, `datasource` | Lists, email, request logging, the database and Flyway |

Migrations are in `src/main/resources/db/migration`. One that has run anywhere is never edited: a fix
is a new `V<n>__...sql`.
