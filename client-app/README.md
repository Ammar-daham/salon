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

Start the API from the repository root, with sample salons:

```bash
docker compose --profile demo up --build
```

Then, from this folder, with a simulator or emulator open:

```bash
flutter pub get
flutter run
```

Without being told otherwise, the app looks for the API on the computer running the simulator:
`http://10.0.2.2:8081` from the Android emulator, `http://localhost:8081` from the iOS simulator. A debug
build shows the address it uses on its start screen. For anywhere else, such as a phone on the same
network, pass the address when you run or build it:

```bash
flutter run --dart-define=API_BASE_URL=http://192.168.1.20:8081
flutter build apk --dart-define=API_BASE_URL=https://api.example.com
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
| `gallery.dart` | A screen showing all of the above, opened from the start screen's Design gallery button in debug builds |

Outfit is bundled in `assets/fonts/outfit` (under the SIL Open Font License, shown on the app's licences
page), and icons come from Lucide (`lucide_icons_flutter`). The tests load Outfit too, in
`test/flutter_test_config.dart`, so text takes the same room as in the app.

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
