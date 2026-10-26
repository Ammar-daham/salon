# Salonat

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="brand/salonat-lockup-on-dark.svg">
  <img src="brand/salonat-lockup.svg" alt="Salonat" height="48">
</picture>

A platform for running hair and beauty salons: their staff, services, opening hours, clients and
appointments. Salons run on **Salonat Pro**, the admin panel. **Salonat** is also the name for the app
clients will book with, which isn't built yet.

| App | What it is |
|---|---|
| [salon-backend](salon-backend) | The REST API: Spring Boot 4, Java 21, PostgreSQL |
| [admin-panel](admin-panel) | Salonat Pro, the web app salon staff and platform admins use: Next.js |
| [client-app](client-app) | Salonat, the app clients book with: Flutter. Not built yet; its design and the plan to build it are here |

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
- [brand](brand): the logo files, its colours and typefaces
- CI builds both apps, runs their tests, checks that `docker compose` comes up with the demo data
  loaded, and runs the admin panel's end-to-end tests against it, on every pull request
