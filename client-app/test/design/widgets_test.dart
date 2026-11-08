import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:salonat_client/design/design.dart';

Future<void> pump(WidgetTester tester, Widget child) {
  return tester.pumpWidget(
    MaterialApp(
      theme: salonatTheme(),
      home: Scaffold(
        body: Center(
          child: Padding(padding: const EdgeInsets.all(20), child: child),
        ),
      ),
    ),
  );
}

Color? backgroundOf(WidgetTester tester, Finder button) {
  final style = tester.widget<TextButton>(button).style!;
  final enabled = tester.widget<TextButton>(button).onPressed != null;
  return style.backgroundColor!.resolve({if (!enabled) WidgetState.disabled});
}

void main() {
  test('the theme is Outfit, on the ground colour, with rose for actions', () {
    final theme = salonatTheme();

    expect(theme.textTheme.bodyMedium!.fontFamily, 'Outfit');
    expect(theme.scaffoldBackgroundColor, SalonatColors.ground);
    expect(theme.colorScheme.primary, SalonatColors.rose);
  });

  group('SalonatButton', () {
    testWidgets('is 54 px tall and calls back when tapped', (tester) async {
      var taps = 0;
      await pump(
        tester,
        SalonatButton(label: 'Book appointment', onPressed: () => taps++),
      );

      await tester.tap(find.text('Book appointment'));

      expect(taps, 1);
      expect(tester.getSize(find.byType(TextButton)).height, 54);
      expect(backgroundOf(tester, find.byType(TextButton)), SalonatColors.rose);
    });

    testWidgets('the tint kind is 44 px tall', (tester) async {
      await pump(
        tester,
        SalonatButton(label: 'Book', kind: ButtonKind.tint, onPressed: () {}),
      );

      expect(tester.getSize(find.byType(TextButton)).height, minTouchTarget);
    });

    testWidgets('without a callback it shows as disabled', (tester) async {
      await pump(
        tester,
        const SalonatButton(label: 'Book appointment', onPressed: null),
      );

      expect(
        tester.getSemantics(find.byType(TextButton)),
        matchesSemantics(
          label: 'Book appointment',
          isButton: true,
          hasEnabledState: true,
          isFocusable: false,
        ),
      );
      expect(backgroundOf(tester, find.byType(TextButton)), SalonatColors.line);
    });
  });

  group('SalonatChip', () {
    testWidgets('asks to be switched on or off', (tester) async {
      bool? asked;
      await pump(
        tester,
        SalonatChip(
          label: 'Available today',
          selected: false,
          onSelected: (value) => asked = value,
        ),
      );

      await tester.tap(find.text('Available today'));

      expect(asked, isTrue);
    });

    testWidgets('takes taps on the whole 44 px area, past what it draws', (
      tester,
    ) async {
      var taps = 0;
      await pump(
        tester,
        SalonatChip(
          label: 'Recommended',
          selected: true,
          onSelected: (_) => taps++,
        ),
      );

      final area = tester.getRect(find.byType(SalonatChip));
      expect(area.height, minTouchTarget);
      // As wide as its label, not the 760 px the test screen offers.
      expect(area.width, lessThan(200));
      // 2 px from the top: outside the 38 px chip, inside the 44 px area.
      await tester.tapAt(Offset(area.center.dx, area.top + 2));

      expect(taps, 1);
    });

    testWidgets('tells screen readers whether it is selected', (tester) async {
      await pump(
        tester,
        SalonatChip(label: 'Recommended', selected: true, onSelected: (_) {}),
      );

      expect(
        tester.getSemantics(find.byType(SalonatChip)),
        isSemantics(
          label: 'Recommended',
          isButton: true,
          isSelected: true,
          isEnabled: true,
          hasTapAction: true,
        ),
      );
    });
  });

  testWidgets('a time slot is rose when selected and calls back when tapped', (
    tester,
  ) async {
    var taps = 0;
    await pump(
      tester,
      TimeSlot(label: '10:15', selected: true, onTap: () => taps++),
    );

    await tester.tap(find.text('10:15'));

    expect(taps, 1);
    expect(
      tester
          .widget<Material>(
            find.descendant(
              of: find.byType(TimeSlot),
              matching: find.byType(Material),
            ),
          )
          .color,
      SalonatColors.rose,
    );
    expect(tester.getSize(find.byType(TimeSlot)).height, minTouchTarget);
  });

  testWidgets('a status badge shows its label in its tone', (tester) async {
    await pump(
      tester,
      const StatusBadge(label: 'Confirmed', tone: BadgeTone.success),
    );

    expect(
      tester.widget<Text>(find.text('Confirmed')).style!.color,
      SalonatColors.success,
    );
  });

  group('SalonatTextField', () {
    testWidgets('screen readers hear its label once, with the field', (
      tester,
    ) async {
      final handle = tester.ensureSemantics();
      await pump(
        tester,
        const SalonatTextField(label: 'Email', initialValue: 'olivia@x.com'),
      );

      expect(find.bySemanticsLabel('Email'), findsOneWidget);
      expect(
        tester.getSemantics(find.byType(EditableText)),
        isSemantics(isTextField: true),
      );
      handle.dispose();
    });

    testWidgets('shows what is wrong, with a red border', (tester) async {
      await pump(
        tester,
        const SalonatTextField(label: 'Email', error: 'Enter a full email'),
      );

      expect(find.text('Enter a full email'), findsOneWidget);
      final decorator = tester.widget<InputDecorator>(
        find.byType(InputDecorator),
      );
      expect(decorator.decoration.error, isNotNull);
      expect(
        Theme.of(tester.element(find.byType(InputDecorator)))
            .inputDecorationTheme
            .errorBorder!
            .borderSide
            .color,
        SalonatColors.errorFill,
      );
    });

    testWidgets('passes typing on', (tester) async {
      String? typed;
      await pump(
        tester,
        SalonatTextField(label: 'Name', onChanged: (value) => typed = value),
      );

      await tester.enterText(find.byType(TextField), 'Olivia');

      expect(typed, 'Olivia');
    });
  });

  group('SalonatCard', () {
    testWidgets('is one button when it has onTap', (tester) async {
      var taps = 0;
      await pump(
        tester,
        SalonatCard(onTap: () => taps++, child: const Text('Glow')),
      );

      await tester.tap(find.text('Glow'));

      expect(taps, 1);
    });

    testWidgets('the dark card writes in white', (tester) async {
      await pump(tester, const SalonatCard.dark(child: Text('Next')));

      expect(
        DefaultTextStyle.of(tester.element(find.text('Next'))).style.color,
        Colors.white,
      );
    });
  });

  group('SalonatTabBar', () {
    testWidgets('selects the tab tapped', (tester) async {
      MainTab? picked;
      await pump(
        tester,
        SalonatTabBar(
          selected: MainTab.discover,
          onSelected: (tab) => picked = tab,
        ),
      );

      await tester.tap(find.text('Bookings'));

      expect(picked, MainTab.bookings);
    });

    testWidgets('marks the selected tab for screen readers, in rose', (
      tester,
    ) async {
      await pump(
        tester,
        SalonatTabBar(selected: MainTab.favourites, onSelected: (_) {}),
      );

      expect(
        tester.getSemantics(find.text('Favourites')),
        isSemantics(isSelected: true, isButton: true),
      );
      expect(find.byIcon(Icons.favorite_rounded), findsOneWidget);
      expect(
        tester.widget<Text>(find.text('Favourites')).style!.color,
        SalonatColors.rose,
      );
    });

    testWidgets('shows only the tabs given', (tester) async {
      await pump(
        tester,
        SalonatTabBar(
          selected: MainTab.discover,
          tabs: const [MainTab.discover, MainTab.bookings, MainTab.profile],
          onSelected: (_) {},
        ),
      );

      expect(find.text('Favourites'), findsNothing);
      expect(find.text('Profile'), findsOneWidget);
    });
  });

  group('InitialsAvatar', () {
    test('takes the first letters of the first and last names', () {
      expect(InitialsAvatar.initialsOf('Olivia Weber'), 'OW');
      expect(InitialsAvatar.initialsOf(' ana maria silva '), 'AS');
      expect(InitialsAvatar.initialsOf('Sara'), 'S');
      expect(InitialsAvatar.initialsOf(''), '');
    });

    testWidgets('is hidden from screen readers', (tester) async {
      final handle = tester.ensureSemantics();
      await pump(tester, const InitialsAvatar(name: 'Olivia Weber'));

      expect(find.text('OW'), findsOneWidget);
      expect(find.bySemanticsLabel('OW'), findsNothing);
      handle.dispose();
    });
  });
}
