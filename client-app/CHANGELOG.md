# client-app changelog

All notable changes to the client app are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/). The version is the `version` field in
[`package.json`](package.json).

## [Unreleased]

Phase 1 of this app: a mock-data-only build, no backend dependency. Real salons/services/
availability/bookings don't exist yet anywhere in the API a customer can reach — see the README's
"What this can't do yet" for the specific backend gaps this is waiting on.

### Added
- Project scaffold: Next.js 16 (App Router), React 19, TanStack Query, Tailwind CSS 4, aligned to
  admin-panel's exact dependency versions so the two apps stay interchangeable in tooling.
- A fresh, mobile-first design system distinct from Salonat Pro's enterprise-dashboard look: an
  iOS-HIG-inspired cool neutral ramp and type scale, the system UI font instead of Outfit (which
  is reserved for the brand wordmark only), a wider radius/shadow scale, and a frosted
  bottom tab bar.
- The navigation shell: a 3-tab bottom bar (Discover, Bookings, Account) inside a phone-width
  `AppShell`, with stub screens for each tab.
- A base UI kit: `Button`, `Card`, `Badge`, `Pill`, `Skeleton`, `EmptyState`, `BottomSheet`,
  `Field`/`TextInput`.
- Dark mode, same flash-free mechanism as admin-panel (`ThemeContext` + an inline pre-paint
  script), with fresh light/dark token values.
