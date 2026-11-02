# client-app changelog

All notable changes to the client app are recorded here. Each release states the salon-backend
version it needs.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/). The version is the `version` field in
[`pubspec.yaml`](pubspec.yaml), before the `+`; each release is tagged `client-app/v<version>` on
`main`.

## [Unreleased]

### Added
- The Flutter project, for iOS and Android. The app opens on a placeholder screen that says booking is
  on its way; a debug build also shows the address of the API it was given.
- The API's address is set when the app is built, with `--dart-define=API_BASE_URL=...`. Without it,
  the app uses salon-backend on the computer running the simulator: `http://10.0.2.2:8081` from the
  Android emulator, `http://localhost:8081` from the iOS simulator. An address that isn't http or https
  stops the app at start.
- The app may reach the API over plain http in Android debug builds, and on iOS at local network
  addresses. Anywhere else it needs https.
