# admin-panel changelog

All notable changes to the admin panel are recorded here. Each release states the salon-backend
version it needs.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/). The version is the `version` field in
[`package.json`](package.json); each release is tagged `admin-panel/v<version>` on `main`.

## [Unreleased]

## [0.1.0] - 2026-09-28

Requires salon-backend ≥ 0.1.0.

### Changed
- DELETE requests are sent without a body; the panel no longer re-fetches a record to echo it
  back (FE-09, #17).
- ADMIN is granted `user:list`, `user:create`, `user:edit` and `user:delete` now that the backend
  scopes users to the caller's business (FE-01, #21).

### Added
- Vitest unit tests for the permission matrix, route access and the auth, user and business wire
  mappers (FE-08, #21).
- CI runs lint, type check, unit tests and `next build` on every pull request (#11).

[Unreleased]: https://github.com/Ammar-daham/salon/compare/admin-panel/v0.1.0...HEAD
[0.1.0]: https://github.com/Ammar-daham/salon/releases/tag/admin-panel/v0.1.0
