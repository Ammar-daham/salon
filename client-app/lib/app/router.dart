import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../data/sample_data.dart';
import '../design/design.dart';
import '../design/gallery.dart';
import '../screens/placeholder_screen.dart';
import 'main_shell.dart';
import 'providers.dart';
import 'routes.dart';

// Until the screens are built, their links lead to the sample salon, service, staff member and
// appointments the fakes start with.
final _salon = SampleData.glow.id;
final _service = SampleData.glow.services.first.id;
final _staff = SampleData.staff[SampleData.glow.id]!.first.id;
const _upcoming = 1;
const _past = 2;

/// The app's routes: a location per screen in the design (see [Routes]). The main screens sit in
/// tabs; the sign-in screens, search, filters, the booking flow and the screens that finish a task
/// cover the tab bar, as in the design.
///
/// Signing in is needed for booking from its last step, and for the Bookings, Favourites and
/// Profile tabs and notifications. A signed-out client is sent to sign in, then back.
final routerProvider = Provider<GoRouter>((ref) {
  final root = GlobalKey<NavigatorState>(debugLabel: 'root');

  // Routes are checked again whenever someone signs in or out.
  final sessionChanged = ValueNotifier(0);
  ref.listen(sessionProvider, (_, _) => sessionChanged.value++);
  ref.onDispose(sessionChanged.dispose);

  bool signedIn() => ref.read(sessionProvider).value != null;

  String? requireSignIn(BuildContext context, GoRouterState state) {
    final session = ref.read(sessionProvider);
    // Still finding out at start: the redirect runs again once it's known.
    if (session.isLoading && !session.hasValue) {
      return null;
    }
    return signedIn() ? null : Routes.signInFrom(state.uri.toString());
  }

  GoRoute screen(
    String path,
    Widget Function(GoRouterState state) build, {
    bool covers = false,
    bool private = false,
    List<RouteBase> routes = const [],
  }) => GoRoute(
    path: path,
    parentNavigatorKey: covers ? root : null,
    redirect: private ? requireSignIn : null,
    builder: (context, state) => build(state),
    routes: routes,
  );

  final router = GoRouter(
    navigatorKey: root,
    initialLocation: Routes.welcome,
    refreshListenable: sessionChanged,
    // Signed in, the sign-in screens have nothing to offer: on to where the client was going.
    redirect: (context, state) {
      const signInScreens = [Routes.welcome, Routes.signIn, Routes.signUp];
      if (!signedIn() || !signInScreens.contains(state.uri.path)) {
        return null;
      }
      return state.uri.queryParameters['from'] ?? Routes.home;
    },
    routes: [
      screen(
        Routes.welcome,
        (_) => const PlaceholderScreen(
          title: 'Welcome',
          template: 'Main',
          task: 'C1',
          links: [
            ScreenLink('Create an account', Routes.signUp),
            ScreenLink('Sign in', Routes.signIn),
            ScreenLink.go('Browse without an account', Routes.home),
          ],
          children: [if (kDebugMode) _DeveloperLinks()],
        ),
      ),
      screen(
        Routes.signIn,
        (state) => PlaceholderScreen(
          title: 'Sign in',
          template: 'SignIn',
          task: 'C1',
          links: const [
            ScreenLink.go('Create an account instead', Routes.signUp),
          ],
          children: [_SignInButton(from: state.uri.queryParameters['from'])],
        ),
      ),
      screen(
        Routes.signUp,
        (_) => const PlaceholderScreen(
          title: 'Create an account',
          template: 'SignUp',
          task: 'C1',
          links: [
            ScreenLink.go('Sign in instead', Routes.signIn),
            ScreenLink.go('Browse without an account', Routes.home),
          ],
        ),
      ),
      screen(
        Routes.search,
        (_) => PlaceholderScreen(
          title: 'Search',
          template: 'Search',
          task: 'C2',
          links: [
            ScreenLink('Salons for “Haircut”', Routes.resultsFor('Haircut')),
            ScreenLink(SampleData.glow.name, Routes.salon(_salon)),
          ],
        ),
      ),
      screen(
        '/booked/:appointmentId',
        private: true,
        covers: true,
        (_) => PlaceholderScreen(
          title: 'Booked',
          template: 'BookDone',
          task: 'C4',
          links: [
            ScreenLink.go('See the booking', Routes.booking(_upcoming)),
            const ScreenLink.go('Back to Discover', Routes.home),
          ],
        ),
      ),
      if (kDebugMode) ...[
        screen(Routes.screens, (_) => const _ScreenIndex()),
        screen(Routes.gallery, (_) => const DesignGallery()),
      ],
      StatefulShellRoute.indexedStack(
        builder: (context, state, shell) => MainShell(navigationShell: shell),
        // One branch per MainTab, in its order.
        branches: [
          StatefulShellBranch(routes: [_discover(screen)]),
          StatefulShellBranch(routes: [_bookings(screen)]),
          StatefulShellBranch(
            routes: [
              screen(
                Routes.favourites,
                private: true,
                (_) => PlaceholderScreen(
                  title: 'Favourites',
                  template: 'Favourites',
                  task: 'C12',
                  links: [
                    ScreenLink.go(SampleData.glow.name, Routes.salon(_salon)),
                    const ScreenLink.go('Find salons', Routes.home),
                  ],
                ),
              ),
            ],
          ),
          StatefulShellBranch(routes: [_profile(screen)]),
        ],
      ),
    ],
  );
  ref.onDispose(router.dispose);
  return router;
});

typedef _Screen = GoRoute Function(
  String path,
  Widget Function(GoRouterState state) build, {
  bool covers,
  bool private,
  List<RouteBase> routes,
});

/// The Discover tab: Home, results, the salon page and booking.
GoRoute _discover(_Screen screen) => screen(
  Routes.home,
  (_) => PlaceholderScreen(
    title: 'Discover',
    template: 'Home',
    task: 'C2',
    links: [
      const ScreenLink('Search', Routes.search),
      ScreenLink('Salons for “Hair”', Routes.resultsFor('Hair')),
      ScreenLink(SampleData.glow.name, Routes.salon(_salon)),
      ScreenLink.go('Your next appointment', Routes.booking(_upcoming)),
      ScreenLink(
        'A free time today',
        Routes.bookTime(_salon, _service, staffId: _staff),
      ),
      const ScreenLink('Notifications', Routes.notifications),
    ],
  ),
  routes: [
    screen(
      'results',
      (state) => PlaceholderScreen(
        title: 'Results for “${state.uri.queryParameters['q'] ?? ''}”',
        template: 'Results',
        task: 'C2',
        links: [
          const ScreenLink('Map', Routes.map),
          const ScreenLink('Filters', Routes.filters),
          ScreenLink(SampleData.glow.name, Routes.salon(_salon)),
          const ScreenLink('Search again', Routes.search),
        ],
      ),
      routes: [
        screen(
          'map',
          (_) => PlaceholderScreen(
            title: 'Map',
            template: 'Map',
            task: 'C10',
            links: [
              const ScreenLink('Filters', Routes.filters),
              ScreenLink(SampleData.glow.name, Routes.salon(_salon)),
            ],
          ),
        ),
        screen(
          'filters',
          covers: true,
          (_) => const PlaceholderScreen(
            title: 'Filters',
            template: 'Filters',
            task: 'C11',
            links: [ScreenLink.go('Show salons', Routes.results)],
          ),
        ),
      ],
    ),
    screen(
      'notifications',
      private: true,
      (_) => PlaceholderScreen(
        title: 'Notifications',
        template: 'Notifications',
        task: 'C15',
        links: [
          ScreenLink.go('A confirmed booking', Routes.booking(_upcoming)),
          ScreenLink('Rate your last visit', Routes.reviewVisit(_past)),
        ],
      ),
    ),
    screen(
      'salons/:salonId',
      (state) => PlaceholderScreen(
        title: 'Salon ${state.pathParameters['salonId']}',
        template: 'Salon',
        task: 'C3',
        links: [
          ScreenLink('About & team', Routes.salonAbout(_salon)),
          ScreenLink('Reviews', Routes.salonReviews(_salon)),
          ScreenLink(
            'Book ${SampleData.glow.services.first.name}',
            Routes.bookStaff(_salon, _service),
          ),
        ],
      ),
      routes: [
        screen(
          'about',
          (_) => PlaceholderScreen(
            title: 'About & team',
            template: 'SalonAbout',
            task: 'C3',
            links: [ScreenLink('Reviews', Routes.salonReviews(_salon))],
          ),
        ),
        screen(
          'reviews',
          (_) => PlaceholderScreen(
            title: 'Reviews',
            template: 'SalonReviews',
            task: 'C14',
            links: [ScreenLink('About & team', Routes.salonAbout(_salon))],
          ),
        ),
        _booking(screen),
      ],
    ),
  ],
);

/// Booking a service: who with, when, then a last look, which needs the client signed in.
GoRoute _booking(_Screen screen) {
  int? staffOf(GoRouterState state) =>
      int.tryParse(state.uri.queryParameters['staff'] ?? '');
  int idOf(GoRouterState state, String name) =>
      int.parse(state.pathParameters[name]!);

  return screen(
    'book/:serviceId',
    covers: true,
    (state) => PlaceholderScreen(
      title: 'Choose a professional',
      template: 'BookStaff',
      task: 'C4',
      links: [
        ScreenLink(
          'Anyone available',
          Routes.bookTime(idOf(state, 'salonId'), idOf(state, 'serviceId')),
        ),
        ScreenLink(
          'Mia Stylist',
          Routes.bookTime(
            idOf(state, 'salonId'),
            idOf(state, 'serviceId'),
            staffId: _staff,
          ),
        ),
      ],
    ),
    routes: [
      screen(
        'time',
        covers: true,
        (state) => PlaceholderScreen(
          title: 'Pick a time',
          template: 'BookTime',
          task: 'C4',
          links: [
            ScreenLink(
              'Continue',
              Routes.bookReview(
                idOf(state, 'salonId'),
                idOf(state, 'serviceId'),
                staffId: staffOf(state),
              ),
            ),
          ],
        ),
      ),
      screen(
        'review',
        covers: true,
        private: true,
        (_) => PlaceholderScreen(
          title: 'Review and book',
          template: 'BookReview',
          task: 'C4',
          links: [
            const ScreenLink('Change your details', Routes.editProfile),
            ScreenLink.go('Book', Routes.booked(_upcoming)),
          ],
        ),
      ),
    ],
  );
}

/// The Bookings tab: the client's appointments, one of them, cancelling and rating.
GoRoute _bookings(_Screen screen) => screen(
  Routes.bookings,
  private: true,
  (_) => PlaceholderScreen(
    title: 'Bookings',
    template: 'Bookings',
    task: 'C5',
    links: [
      ScreenLink('Your upcoming appointment', Routes.booking(_upcoming)),
      ScreenLink('Rate your last visit', Routes.reviewVisit(_past)),
      ScreenLink(
        'Book it again',
        Routes.bookTime(
          SampleData.urban.id,
          SampleData.urban.services[1].id,
          staffId: SampleData.staff[SampleData.urban.id]!.first.id,
        ),
      ),
    ],
  ),
  routes: [
    screen(
      ':appointmentId',
      (state) => PlaceholderScreen(
        title: 'Appointment ${state.pathParameters['appointmentId']}',
        template: 'BookingDetail',
        task: 'C5',
        links: [
          ScreenLink(
            'Change time',
            Routes.bookTime(_salon, _service, staffId: _staff),
          ),
          ScreenLink('Cancel appointment', Routes.cancelBooking(_upcoming)),
          ScreenLink.go(SampleData.glow.name, Routes.salon(_salon)),
        ],
      ),
      routes: [
        screen(
          'cancel',
          covers: true,
          (_) => PlaceholderScreen(
            title: 'Cancel appointment',
            template: 'CancelBooking',
            task: 'C6',
            links: [
              ScreenLink.go('Keep appointment', Routes.booking(_upcoming)),
              const ScreenLink.go('Cancel appointment', Routes.bookings),
            ],
          ),
        ),
        screen(
          'review',
          covers: true,
          (_) => const PlaceholderScreen(
            title: 'Rate your visit',
            template: 'Review',
            task: 'C14',
            links: [ScreenLink.go('Send review', Routes.bookings)],
          ),
        ),
      ],
    ),
  ],
);

/// The Profile tab.
GoRoute _profile(_Screen screen) => screen(
  Routes.profile,
  private: true,
  (_) => const PlaceholderScreen(
    title: 'Profile',
    template: 'Profile',
    task: 'C7',
    links: [ScreenLink('Account details', Routes.editProfile)],
    children: [_SignOutButton()],
  ),
  routes: [
    screen(
      'edit',
      covers: true,
      (_) => const PlaceholderScreen(
        title: 'Account details',
        template: 'EditProfile',
        task: 'C7',
        links: [ScreenLink.go('Save', Routes.profile)],
      ),
    ),
  ],
);

/// Signs in as the sample client, until the sign-in form is built.
class _SignInButton extends ConsumerWidget {
  const _SignInButton({this.from});

  /// Where to go once signed in.
  final String? from;

  @override
  Widget build(BuildContext context, WidgetRef ref) => SalonatButton(
    label: 'Sign in as ${SampleData.client.name}',
    onPressed: () async {
      await ref
          .read(sessionProvider.notifier)
          .signIn(
            email: SampleData.client.email,
            password: SampleData.clientPassword,
          );
      if (context.mounted) {
        context.go(from ?? Routes.home);
      }
    },
  );
}

class _SignOutButton extends ConsumerWidget {
  const _SignOutButton();

  @override
  Widget build(BuildContext context, WidgetRef ref) => SalonatButton(
    label: 'Sign out',
    kind: ButtonKind.secondary,
    onPressed: () async {
      await ref.read(sessionProvider.notifier).signOut();
      if (context.mounted) {
        context.go(Routes.welcome);
      }
    },
  );
}

/// The API's address and the way to the developer screens, on Welcome in debug builds.
class _DeveloperLinks extends ConsumerWidget {
  const _DeveloperLinks();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final config = ref.watch(configProvider);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      spacing: 10,
      children: [
        Text(
          'API: ${config.apiBaseUrl} · data: ${config.dataSource.name}',
          style: SalonatText.meta,
        ),
        SalonatButton(
          label: 'Every screen',
          kind: ButtonKind.tint,
          onPressed: () => context.push(Routes.screens),
        ),
        SalonatButton(
          label: 'Design gallery',
          kind: ButtonKind.tint,
          onPressed: () => context.push(Routes.gallery),
        ),
      ],
    );
  }
}

/// A link to every screen in the design, for checking them as they're built.
class _ScreenIndex extends StatelessWidget {
  const _ScreenIndex();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Every screen'),
        backgroundColor: SalonatColors.ground,
      ),
      body: ListView(
        padding: const EdgeInsets.all(screenMargin),
        children: [
          for (final (template, location) in designScreens) ...[
            SalonatButton(
              label: template,
              kind: ButtonKind.secondary,
              onPressed: () => context.go(location),
            ),
            const SizedBox(height: 10),
          ],
        ],
      ),
    );
  }
}

/// Every screen in the design, by its template in `design/templates`, with a location that shows it.
final designScreens = <(String, String)>[
  ('Main', Routes.welcome),
  ('SignIn', Routes.signIn),
  ('SignUp', Routes.signUp),
  ('Home', Routes.home),
  ('Search', Routes.search),
  ('Results', Routes.resultsFor('Hair')),
  ('Map', Routes.map),
  ('Filters', Routes.filters),
  ('Salon', Routes.salon(_salon)),
  ('SalonReviews', Routes.salonReviews(_salon)),
  ('SalonAbout', Routes.salonAbout(_salon)),
  ('BookStaff', Routes.bookStaff(_salon, _service)),
  ('BookTime', Routes.bookTime(_salon, _service, staffId: _staff)),
  ('BookReview', Routes.bookReview(_salon, _service, staffId: _staff)),
  ('BookDone', Routes.booked(_upcoming)),
  ('Bookings', Routes.bookings),
  ('BookingDetail', Routes.booking(_upcoming)),
  ('CancelBooking', Routes.cancelBooking(_upcoming)),
  ('Review', Routes.reviewVisit(_past)),
  ('Favourites', Routes.favourites),
  ('Notifications', Routes.notifications),
  ('Profile', Routes.profile),
  ('EditProfile', Routes.editProfile),
];
