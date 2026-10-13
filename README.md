# Salon

A platform for running hair and beauty salons: their staff, services, opening hours, clients and
appointments.

| App | What it is |
|---|---|
| [salon-backend](salon-backend) | The REST API: Spring Boot 4, Java 21, PostgreSQL |
| [admin-panel](admin-panel) | The web app salon staff and platform admins use: Next.js |

## Try it

With Docker installed, from this folder:

```bash
docker compose --profile demo up --build
```

Then open http://localhost:3000 and sign in as `superadmin@salon.example.com` with `Password123!`.
`--profile demo` adds sample salons, staff and clients to an empty database; leave it out to start
with no data at all.

| Address | What's there |
|---|---|
| http://localhost:3000 | The admin panel |
| http://localhost:8081 | The API |
| http://localhost:8025 | Mailpit: every email the app sends, such as password reset links |
| `localhost:5433` | PostgreSQL (database, user and password `salondb` / `salon` / `salon`) |

`docker compose down` stops everything; `docker compose down --volumes` also deletes the data.

To work on one app without Docker, see its own README. The
[salon-backend README](salon-backend/README.md) also covers production.

## Working on it

- [CHANGELOG.md](CHANGELOG.md): platform milestones; each app has its own changelog
- CI builds both apps, runs their tests, checks that `docker compose` comes up with the demo data
  loaded, and runs the admin panel's end-to-end tests against it, on every pull request
