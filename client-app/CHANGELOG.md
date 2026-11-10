# client-app changelog

All notable changes to the client app are recorded here. Each release states the salon-backend
version it needs.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/). The version is the `version` field in
[`pubspec.yaml`](pubspec.yaml), before the `+`; each release is tagged `client-app/v<version>` on
`main`.

## [Unreleased]

### Added
- Every screen in the design has its place, with a placeholder until it's built: the Discover, Bookings
  and Profile tabs, each keeping its own place, and the screens on top of them. Favourites waits for a
  later release.
- Browsing needs no account; the Bookings and Profile tabs, notifications and finishing a booking ask
  the client to sign in, then carry on where they were going.
- The app runs on its stand-ins unless built with `--dart-define=DATA=api`.
- The API client: salons, their opening hours, staff and free times read from salon-backend, with every
  failed call turned into a message fit to show, and paged lists. Nothing on screen uses it yet.
- Stand-ins for every resource the app uses, with the demo salons, for building screens before their
  API exists. Signing in and the client's own appointments are stand-ins only for now.
- The design system from the design's Foundations screen: its colours, type scale in Outfit and corner
  sizes, and the widgets screens are built from: buttons, chips, time slots, status badges, text fields,
  cards, the tab bar, photo placeholders and initials avatars. Everything tappable takes up at least
  44 × 44 px.
- A design gallery showing every widget, opened from Welcome in debug builds.
- The Flutter project, for iOS and Android. A debug build shows the address of the API it was given on
  its Welcome screen.
- The API's address is set when the app is built, with `--dart-define=API_BASE_URL=...`. Without it,
  the app uses salon-backend on the computer running the simulator: `http://10.0.2.2:8081` from the
  Android emulator, `http://localhost:8081` from the iOS simulator. An address that isn't http or https
  stops the app at start.
- The app may reach the API over plain http in Android debug builds, and on iOS at local network
  addresses. Anywhere else it needs https.
