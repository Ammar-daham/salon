# client-app

Salonat, the app clients use to find a salon, see its free times and book. Flutter, for iOS and
Android, talking to [salon-backend](../salon-backend) like the [admin panel](../admin-panel) does.

**It's early:** the app starts and shows a placeholder screen. What comes next, screen by screen, is in
[the plan](docs/plan.md); what changed in each version is in [CHANGELOG.md](CHANGELOG.md).

| Folder | What's in it |
|---|---|
| `lib/` | The app |
| `test/` | Unit and widget tests |
| `android/`, `ios/` | The native projects Flutter builds into |
| [design](design) | Every screen of the app, as the sources of its design canvas |
| [docs/plan.md](docs/plan.md) | The plan to build it: phases, small tasks and a branch name for each |

## Run it

You need [Flutter](https://docs.flutter.dev/get-started/install) 3.47 or later on the stable channel
(CI uses 3.47.8), plus Xcode for the iOS simulator or Android Studio for an Android emulator.
`flutter doctor` says what's missing.

From this folder, with a simulator or emulator open:

```bash
flutter pub get
flutter run
```

That runs on the app's stand-ins for the API, with the demo salons, so nothing else is needed. To use
salon-backend for what it already serves the app, start it from the repository root with sample salons,
and run with `DATA=api`:

```bash
docker compose --profile demo up --build
flutter run --dart-define=DATA=api
```

Without being told otherwise, the app looks for the API on the computer running the simulator:
`http://10.0.2.2:8081` from the Android emulator, `http://localhost:8081` from the iOS simulator. A debug
build shows the address and data it uses on its Welcome screen. For anywhere else, such as a phone on
the same network, pass the address when you run or build it:

```bash
flutter run --dart-define=DATA=api --dart-define=API_BASE_URL=http://192.168.1.20:8081
flutter build apk --dart-define=DATA=api --dart-define=API_BASE_URL=https://api.example.com
```

Plain http only works where you develop: in Android debug builds
(`android/app/src/debug/AndroidManifest.xml`), and on iOS for local network addresses
(`NSAllowsLocalNetworking` in `ios/Runner/Info.plist`). Anywhere else needs https.

## Design system

Screens are built from [lib/design](lib/design), the design's `Foundations` screen in code. Import
`design/design.dart` for all of it:

| File | What's in it |
|---|---|
| `tokens.dart` | `SalonatColors`, `SalonatRadii`, the 44 px `minTouchTarget` and the 20 px `screenMargin` |
| `theme.dart` | `SalonatText`, the type scale, and `salonatTheme()`, which gives Material's own widgets the same look |
| `widgets/` | `SalonatButton`, `SalonatChip`, `TimeSlot`, `StatusBadge`, `SalonatTextField`, `SalonatCard`, `SalonatTabBar`, `PhotoPlaceholder` and `InitialsAvatar` |
| `gallery.dart` | A screen showing all of the above, opened from Welcome in debug builds |

Outfit is bundled in `assets/fonts/outfit` (under the SIL Open Font License, shown on the app's licences
page), and icons come from Lucide (`lucide_icons_flutter`). The tests load Outfit too, in
`test/flutter_test_config.dart`, so text takes the same room as in the app.

## Talking to the API

| Folder | What's in it |
|---|---|
| [lib/api](lib/api) | `ApiClient`, which calls salon-backend with dio; `ApiException`, what every failed call becomes; `Page` and `PageQuery` for paged lists; and `Endpoints`, every path the app calls |
| [lib/data](lib/data) | A folder per resource: its models, read from the API's JSON by hand, and its repository, an interface with an `Api…` and a `Fake…` version |

Screens read data through the repositories only, so each can be built on its fake before its API
exists, then switched over in `Repositories` (`lib/data/repositories.dart`). `Repositories.fake()`
gives every fake, optionally with a delay so loading states show. The fakes use the demo salons from
`salon-backend`'s seed data, and sign in as olivia@example.com with `password123`.

Signing in and the client's own appointments are fakes only for now, as salon-backend can't serve
them to the app yet: it gets token sign-in in task B2 of [the plan](docs/plan.md), and the client's
own bookings in B6 to B8.

Times from the API are on the salon's clock, with no time zone; `lib/data/salon_time.dart` reads and
writes them, and says how to show them.

## Screens and navigation

[lib/app](lib/app) holds what joins the screens up:

| File | What's in it |
|---|---|
| `routes.dart` | `Routes`, every screen's location, such as `Routes.salon(1)` for `/salons/1` |
| `router.dart` | The go_router routes: a location per screen in the design, which ones need signing in, and `designScreens`, which lists them all |
| `main_shell.dart` | The tabs. Discover, Bookings and Profile show; Favourites has its routes but stays hidden until phase 2 |
| `providers.dart` | The Riverpod providers: the config, the repositories, and the session, who is signed in |

Browsing needs no account. Signing in is needed for the Bookings, Favourites and Profile tabs,
notifications, and booking from its last step; a signed-out client is sent to sign in, then on to where
they were going. The sign-in screens, search, filters, the booking flow and the screens that finish a
task cover the tab bar, as in the design; the others sit in a tab, which keeps its own back stack.

Until each screen is built, its location shows a placeholder naming its template in [design](design)
and its task in [the plan](docs/plan.md), with buttons to the screens the design leads to from it. Sign
in on the placeholder with the sample account. In a debug build, Welcome links to a list of every
screen and to the design gallery.

## Check it

| Command | What it does |
|---|---|
| `flutter analyze` | Lints and type checks, with the rules in `analysis_options.yaml` |
| `dart format lib test` | Formats the code; CI fails if this would change anything |
| `flutter test` | Runs the unit and widget tests |

CI runs all three on every pull request.

## Not settled yet

The app ids, `com.salonat.salonat_client` on Android and `com.salonat.salonatClient` on iOS, the icon
and the launch screen are Flutter's placeholders until the release setup task in [the plan](docs/plan.md).
