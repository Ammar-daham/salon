# Client app: plan

How to build Salonat, the client app, from [its design](../design) to the stores. The work is split into
phases, and each phase into small tasks: one branch and one pull request each, named the way this repo
names them (`<type>/<app>-<what>`). Task IDs such as B6 or C4 only exist in this file, to say which task
needs which; commits and changelogs don't use them.

## Decisions

These are the starting points; the first task in each area confirms them or records a change here.

- **Flutter**, one app for iOS and Android, in `client-app/` (Dart package `salonat_client`). Suggested
  packages: go_router, Riverpod, dio, freezed and json_serializable, flutter_secure_storage, intl,
  url_launcher, add_2_calendar. Outfit is bundled as a font asset (it's under the OFL); icons are
  Lucide, the closest match to the design's line icons.
- **Signing in from the app uses tokens.** The API only has cookie sessions, which end after 30 minutes
  without a request, and its CSRF reasoning assumes a browser. The app signs in for an opaque token, kept
  in the API only as a SHA-256 hash (like password reset tokens) and sent as `Authorization: Bearer`. The
  admin panel keeps its cookie session.
- **A client's account meets a salon's client record on their first booking there.** Each salon keeps
  its own `customers` row per client; the app finds or creates that row with `user_id` set, which the
  unique `(business_id, user_id)` index already allows for.
- **Bookings from the app must fit a time the salon offers.** Staff can still book anyone anywhere, as
  today.
- **Screens first, API second.** Each screen is built against a fake repository shaped like the API, then
  switched to the real one when its backend task is merged, so app and API work can run side by side.
- **English first**, prices in the salon's currency, times on the salon's clock. German, then Arabic with
  right-to-left layout, come later.

## What the API has today

A `CUSTOMER` can sign in (cookie session) and browse approved salons with their services, staff and
opening hours. A client **can't** register themselves, use the API without a session, see free times
(availability is for salon staff only), book, see their own appointments across salons, cancel, or get
any email or notification about a booking. There's nothing yet for reviews, favourites, service
categories, distance search, more than one photo per salon, push notifications or payments. Phase 1 is
mostly about closing the first list.

## Phase 0: groundwork

| ID | Branch | What | Done when |
|---|---|---|---|
| P1 | `docs/client-app-design-and-plan` | The design sources and this plan | Merged |
| P2 | `feat/client-app-flutter-scaffold` | The Flutter project in this folder. The API address comes from `--dart-define` (the Android emulator reaches the host as `10.0.2.2:8081`). Analysis options, one widget test, a README section on running it, `CHANGELOG.md` at 0.1.0, and a "client-app (Flutter)" job in `.github/workflows/ci.yml` running `flutter analyze` and `flutter test` | The app starts on both simulators and CI runs it |
| P3 | `feat/client-app-design-system` | The theme from `Foundations`: colours, type scale, corner sizes, 44 px minimum touch targets. Widgets: buttons, chip, time slot, status badge, text field, cards, tab bar, photo placeholder, initials avatar | A gallery screen shows every widget; widget tests pass |
| P4 | `feat/client-app-api-client` | dio client; the API's error body `{errorCode, message, status, timestamp, path}` and page body `{items, page, size, total_items, total_pages}`; models mirroring `admin-panel/src/lib/resources/*/*.mappers.ts`; one repository interface per resource, each with a fake | Unit tests cover parsing, errors and paging |
| P5 | `feat/client-app-navigation` | go_router with the Discover, Bookings, Favourites and Profile tabs (Favourites hidden until phase 2), a route per screen, and signing in required for booking and account screens | Every screen in the design is reachable |

## Phase 1: sign up, find a salon, book, manage the booking

### salon-backend

| ID | Branch | What |
|---|---|---|
| B1 | `feat/salon-backend-customer-sign-up` | `POST /api/v1/auth/sign-up`, open without a session: creates a `CUSTOMER` with no salon. Same password rules and throttle as salon registration; 409 when the email is taken |
| B2 | `feat/salon-backend-mobile-sign-in-tokens` | A migration for an `auth_tokens` table (hash, user, created, last used, expiry that slides with use); `POST /api/v1/auth/token` to sign in for a token, `DELETE` to sign out; `SecurityConfig` accepts a bearer token next to the session cookie |
| B3 | `feat/salon-backend-public-salon-browsing` | Salons, their opening hours, staff and services readable without a session; clients and anonymous callers see approved salons only, and only active staff and services |
| B4 | `feat/salon-backend-customer-availability` | Availability readable by clients and anonymous callers (slots only). Takes "now" from the `Clock` bean like the rest of the API, instead of `Instant.now()` |
| B5 | `feat/salon-backend-customer-profile` | `GET` and `PUT /api/v1/me`: name, mobile number and offers opt-in. A client can add their own phone number, which today only an admin can |
| B6 | `feat/salon-backend-customer-booking` | `POST /api/v1/me/appointments` with `business_id`, `service_id`, optional `staff_id` (none means anyone free), `starts_at` and `notes`. Finds or creates the client's record at that salon, refuses a time that availability doesn't offer, books it as `BOOKED`; 409 when someone took the slot first |
| B7 | `feat/salon-backend-my-appointments` | `GET /api/v1/me/appointments?when=upcoming\|past` across every salon, and `GET /api/v1/me/appointments/{id}`; each carries the salon's id, name, address and phone, and the service's length |
| B8 | `feat/salon-backend-customer-cancel` | A migration adding the salon's cancellation notice in hours (24 by default) and who cancelled an appointment and why; `PUT /api/v1/me/appointments/{id}/cancel`, refused once the notice has passed |
| B9 | `feat/salon-backend-booking-emails` | Emails the client when they book, when the salon confirms, and when the salon cancels; sent after the transaction commits, the way `SalonDecisionMailer` does |
| B10 | `feat/salon-backend-booking-alerts` | New notification types for a booking or cancellation from the app, with the appointment's id (a migration widens the type check), so the admin panel's bell shows them |
| B11 | `feat/salon-backend-customer-password-reset` | A client's reset email links to the client app's own reset page, set by its own setting, and says Salonat rather than Salonat Pro |

### admin-panel

| ID | Branch | What | Needs |
|---|---|---|---|
| A1 | `feat/admin-panel-online-bookings` | Marks appointments booked in the app and ones the client cancelled; shows the new bell notifications | B8, B10 |
| A2 | `feat/admin-panel-cancellation-notice` | The salon's cancellation notice in its settings | B8 |

### client-app

| ID | Branch | Screens (templates in [design](../design)) | Needs |
|---|---|---|---|
| C1 | `feat/client-app-sign-in` | Welcome, sign in, create account, forgot password (`Main`, `SignIn`, `SignUp`) | B1, B2, B11 |
| C2 | `feat/client-app-discover` | Home without categories and free times, search, results list (`Home`, `Search`, `Results`) | B3 |
| C3 | `feat/client-app-salon-page` | Services, and About & team (`Salon`, `SalonAbout`) | B3 |
| C4 | `feat/client-app-booking-flow` | Professional, time, review, booked (`BookStaff`, `BookTime`, `BookReview`, `BookDone`) | B4, B6 |
| C5 | `feat/client-app-bookings` | Bookings and appointment detail, add to calendar, directions (`Bookings`, `BookingDetail`) | B7 |
| C6 | `feat/client-app-cancel-booking` | Cancel an appointment (`CancelBooking`) | B8 |
| C7 | `feat/client-app-profile` | Profile, account details, sign out, delete account, offers opt-in (`Profile`, `EditProfile`). The stores require in-app account deletion | B5 |
| C8 | `chore/client-app-release-setup` | App icon from `brand/salonat-mark-1024.png`, launch screen, bundle ids, signing, a TestFlight build and an internal Play test build | |

When phase 1 ships: tag `client-app/v0.1.0` and the backend and admin panel versions it needs, and add
the milestone to the root [CHANGELOG.md](../../CHANGELOG.md).

## Phase 2: finding salons and coming back

| ID | Branch | What | Needs |
|---|---|---|---|
| B12 | `feat/salon-backend-service-categories` | A fixed list of categories (hair, barber, nails, massage, facials, brows & lashes) on services; salons filtered by category | |
| A3 | `feat/admin-panel-service-categories` | Pick a category on the service form | B12 |
| B13 | `feat/salon-backend-nearby-salons` | `?near=<lat>,<lng>&radius_km=` on the salon list, with each salon's distance and sorting by it, from the coordinates addresses already store | |
| B14 | `feat/salon-backend-free-times` | The next free times per salon for a service or category on a day, for Home and the results list | B4 |
| B15 | `feat/salon-backend-favourites` | `GET /api/v1/me/favourites`, and `PUT` and `DELETE /api/v1/me/favourites/{business_id}` | |
| B16 | `feat/salon-backend-customer-reschedule` | Move an appointment to another offered time, within the cancellation notice | B8 |
| C9 | `feat/client-app-categories-and-free-times` | Home's categories and "Free times today"; free times on results | B12, B14 |
| C10 | `feat/client-app-map` | The map (`Map`), location permission and distances | B13 |
| C11 | `feat/client-app-filters` | Filters (`Filters`): sort, day, time of day, maximum price | B13, B14 |
| C12 | `feat/client-app-favourites` | The Favourites tab and the heart on salons (`Favourites`) | B15 |
| C13 | `feat/client-app-change-time-and-book-again` | Change an appointment's time; book a past visit again | B16 |

## Phase 3: trust and staying in touch

| ID | Branch | What | Needs |
|---|---|---|---|
| B17 | `feat/salon-backend-reviews` | One review per completed appointment, the salon's reply, and each salon's rating and count | |
| A4 | `feat/admin-panel-review-replies` | Read and reply to reviews | B17 |
| C14 | `feat/client-app-reviews` | Rate a visit (`Review`), the salon's reviews (`SalonReviews`), ratings on salon cards | B17 |
| B18 | `feat/salon-backend-customer-notifications` | In-app notifications for clients, each linked to its appointment | B10 |
| C15 | `feat/client-app-notifications` | The inbox and the bell's dot (`Notifications`) | B18 |
| B19 | `feat/salon-backend-push-notifications` | Device tokens, sending through Firebase Cloud Messaging, a reminder the day before | B18 |
| C16 | `feat/client-app-push-notifications` | Asking permission, registering the device, opening the right screen from a notification | B19 |
| B20 | `feat/salon-backend-salon-photos` | Several photos per salon, uploaded and stored | |
| A5 | `feat/admin-panel-salon-photos` | Add, order and remove a salon's photos | B20 |
| C17 | `feat/client-app-salon-photos` | The salon page's photo gallery | B20 |
| B21 | `feat/salon-backend-social-sign-in` | Sign in with Apple and with Google | B2 |
| C18 | `feat/client-app-social-sign-in` | The Apple and Google buttons. Apple requires its own option wherever Google is offered | B21 |

## Later

- `feat/client-app-german`: German, for salons in Germany.
- `feat/client-app-arabic-rtl`: Arabic, laid out right to left.
- Ideas without tasks yet: paying online, gift cards, loyalty.

## Done means

- **salon-backend:** integration tests on `support/IntegrationTest` with the fixtures in
  `src/test/resources/fixtures`; the endpoint's Javadoc and `controller/package-info.java` say who may
  call it; an entry under `[Unreleased]` in its CHANGELOG, breaking API changes first; `./gradlew build`
  passes.
- **admin-panel:** lint, typecheck, tests and build pass, and the end-to-end tests where a page changed.
- **client-app:** `flutter analyze` and `flutter test` pass; the screen matches its template in
  [design](../design); touch targets are at least 44 px and every control has a label for screen
  readers; an entry under `[Unreleased]` in its CHANGELOG.
