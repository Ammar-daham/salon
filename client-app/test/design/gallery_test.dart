import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:salonat_client/design/design.dart';
import 'package:salonat_client/design/gallery.dart';

void main() {
  Future<void> pumpGallery(WidgetTester tester) async {
    // As wide as the phone the design is drawn on, 390 px, and tall enough to show the whole
    // gallery without scrolling.
    tester.view.physicalSize = const Size(390, 3200) * 3;
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      MaterialApp(theme: salonatTheme(), home: const DesignGallery()),
    );
  }

  final scrollable = find.byType(Scrollable).first;

  testWidgets('shows every widget of the design system', (tester) async {
    await pumpGallery(tester);

    for (final type in [
      SalonatButton,
      SalonatChip,
      TimeSlot,
      StatusBadge,
      SalonatTextField,
      SalonatCard,
      PhotoPlaceholder,
      InitialsAvatar,
      SalonatTabBar,
    ]) {
      expect(find.byType(type), findsWidgets, reason: '$type');
    }
  });

  testWidgets('everything tappable is at least 44 × 44 px', (tester) async {
    final handle = tester.ensureSemantics();
    await pumpGallery(tester);

    expect(
      tester.state<ScrollableState>(scrollable).position.maxScrollExtent,
      0,
      reason: 'the whole gallery is on screen',
    );
    await expectLater(tester, meetsGuideline(iOSTapTargetGuideline));
    handle.dispose();
  });

  testWidgets('chips, time slots and tabs can be picked', (tester) async {
    await pumpGallery(tester);

    await tester.tap(
      find.descendant(
        of: find.byType(SalonatTabBar),
        matching: find.text('Bookings'),
      ),
    );
    await tester.tap(find.text('09:00'));
    await tester.tap(find.text('Available today'));
    await tester.pump();

    expect(
      tester.widget<SalonatTabBar>(find.byType(SalonatTabBar)).selected,
      MainTab.bookings,
    );
    expect(
      tester.widget<TimeSlot>(find.widgetWithText(TimeSlot, '09:00')).selected,
      isTrue,
    );
    expect(
      tester
          .widget<SalonatChip>(
            find.widgetWithText(SalonatChip, 'Available today'),
          )
          .selected,
      isTrue,
    );
  });
}
