import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import 'design.dart';

/// Every colour, text style and widget of the design system on one screen, to compare with the
/// design's Foundations screen. Debug builds open it from the start screen.
class DesignGallery extends StatefulWidget {
  const DesignGallery({super.key});

  @override
  State<DesignGallery> createState() => _DesignGalleryState();
}

class _DesignGalleryState extends State<DesignGallery> {
  final _chips = {'Recommended': true, 'Available today': false};
  bool _pickDate = false;
  String _slot = '10:15';
  MainTab _tab = MainTab.discover;

  @override
  Widget build(BuildContext context) {
    void noop() {}

    return Scaffold(
      appBar: AppBar(
        title: const Text('Design gallery'),
        backgroundColor: SalonatColors.ground,
      ),
      bottomNavigationBar: SalonatTabBar(
        selected: _tab,
        onSelected: (tab) => setState(() => _tab = tab),
      ),
      // Not a ListView, so every widget is built at once and a test can find it.
      body: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(screenMargin, 8, screenMargin, 32),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const _Section('Colour', children: [_Swatches()]),
            _Section(
              'Type',
              children: [
                Text('Bookings', style: SalonatText.screenTitle),
                Text('Pick a time', style: SalonatText.stepTitle),
                Text('Free times today', style: SalonatText.section),
                Text('Signature Haircut', style: SalonatText.itemTitle),
                Text('Wash, cut and style.', style: SalonatText.body),
                Text('Hair & nails · 1.1 km', style: SalonatText.meta),
                Text('UPCOMING', style: SalonatText.overline),
              ],
            ),
            _Section(
              'Buttons',
              children: [
                SalonatButton(label: 'Book appointment', onPressed: noop),
                SalonatButton(
                  label: 'Keep appointment',
                  kind: ButtonKind.secondary,
                  onPressed: noop,
                ),
                SalonatButton(
                  label: 'Continue with Apple',
                  kind: ButtonKind.dark,
                  onPressed: noop,
                ),
                SalonatButton(
                  label: 'Add to calendar',
                  kind: ButtonKind.secondary,
                  icon: LucideIcons.calendarPlus,
                  onPressed: noop,
                ),
                Row(
                  spacing: 10,
                  children: [
                    Expanded(
                      child: SalonatButton(
                        label: 'Book',
                        kind: ButtonKind.tint,
                        onPressed: noop,
                      ),
                    ),
                    const Expanded(
                      child: SalonatButton(
                        label: 'Disabled',
                        kind: ButtonKind.tint,
                        onPressed: null,
                      ),
                    ),
                  ],
                ),
                SalonatButton(
                  label: 'Cancel appointment',
                  kind: ButtonKind.danger,
                  onPressed: noop,
                ),
              ],
            ),
            _Section(
              'Chips & time slots',
              children: [
                Wrap(
                  spacing: 8,
                  children: [
                    for (final MapEntry(key: label, value: on)
                        in _chips.entries)
                      SalonatChip(
                        label: label,
                        selected: on,
                        onSelected: (value) =>
                            setState(() => _chips[label] = value),
                      ),
                    SalonatChip(
                      label: 'Pick a date',
                      icon: LucideIcons.calendar,
                      selected: _pickDate,
                      onSelected: (value) => setState(() => _pickDate = value),
                    ),
                  ],
                ),
                Wrap(
                  spacing: 8,
                  children: [
                    for (final time in ['09:00', '10:15', '10:30', '11:45'])
                      TimeSlot(
                        label: time,
                        selected: time == _slot,
                        onTap: () => setState(() => _slot = time),
                      ),
                  ],
                ),
              ],
            ),
            const _Section(
              'Appointment status → API value',
              children: [
                _BadgeRow('BOOKED', 'Awaiting confirmation', BadgeTone.waiting),
                _BadgeRow('CONFIRMED', 'Confirmed', BadgeTone.success),
                _BadgeRow('COMPLETED', 'Completed', BadgeTone.done),
                _BadgeRow('CANCELLED', 'Cancelled', BadgeTone.cancelled),
                _BadgeRow('NO_SHOW', 'Missed', BadgeTone.cancelled),
              ],
            ),
            const _Section(
              'Fields',
              children: [
                SalonatTextField(label: 'First name', initialValue: 'Olivia'),
                SalonatTextField(label: 'Last name', hint: 'Weber'),
                SalonatTextField(
                  label: 'Email',
                  initialValue: 'olivia@example',
                  keyboardType: TextInputType.emailAddress,
                  error: 'Enter a full email address, like name@example.com',
                ),
                SalonatTextField(
                  label: 'Disabled',
                  initialValue: 'Can’t change this',
                  enabled: false,
                ),
              ],
            ),
            _Section(
              'Cards',
              children: [
                SalonatCard.dark(
                  onTap: noop,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    spacing: 4,
                    children: [
                      Text(
                        'NEXT APPOINTMENT',
                        style: SalonatText.overline.copyWith(
                          color: SalonatColors.glow,
                        ),
                      ),
                      Text(
                        'Signature Haircut',
                        style: SalonatText.itemTitle.copyWith(
                          color: Colors.white,
                        ),
                      ),
                      const Text('Thu 6 Nov · 10:15 · Glow Beauty Studio'),
                    ],
                  ),
                ),
                SalonatCard(
                  onTap: noop,
                  child: Row(
                    spacing: 14,
                    children: [
                      const PhotoPlaceholder(width: 64, height: 64, radius: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          spacing: 6,
                          children: [
                            Text(
                              'Urban Cuts Barbershop',
                              style: SalonatText.itemTitle,
                            ),
                            Text(
                              'Beard Trim · 30 min',
                              style: SalonatText.meta,
                            ),
                            const StatusBadge(
                              label: 'Confirmed',
                              tone: BadgeTone.success,
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            _Section(
              'Photo placeholders & avatars',
              children: [
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    for (final tone in PlaceholderTone.values)
                      PhotoPlaceholder(
                        width: 48,
                        height: 48,
                        radius: 12,
                        tone: tone,
                        iconSize: 22,
                      ),
                  ],
                ),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    for (final (i, name) in const [
                      'Olivia Weber',
                      'Lena Hoffmann',
                      'Mehmet Yilmaz',
                      'Sara',
                      'Jonas Becker',
                      'Ana Silva',
                    ].indexed)
                      InitialsAvatar(
                        name: name,
                        size: 48,
                        tone: PlaceholderTone.values[i],
                      ),
                  ],
                ),
              ],
            ),
            _Section(
              'Tab bar',
              children: [
                Text(
                  'Along the bottom of this screen; tap a tab to select it.',
                  style: SalonatText.meta,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _Section extends StatelessWidget {
  const _Section(this.title, {required this.children});

  final String title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        spacing: 12,
        children: [
          Text(title, style: SalonatText.section),
          ...children,
        ],
      ),
    );
  }
}

class _BadgeRow extends StatelessWidget {
  const _BadgeRow(this.apiValue, this.label, this.tone);

  final String apiValue;
  final String label;
  final BadgeTone tone;

  @override
  Widget build(BuildContext context) {
    return Row(
      spacing: 14,
      children: [
        StatusBadge(label: label, tone: tone),
        Flexible(
          child: Text(
            apiValue,
            style: SalonatText.meta.copyWith(fontFamily: 'monospace'),
          ),
        ),
      ],
    );
  }
}

class _Swatches extends StatelessWidget {
  const _Swatches();

  static const _colours = [
    ('Rose', SalonatColors.rose),
    ('Rose deep', SalonatColors.roseDeep),
    ('Plum', SalonatColors.plum),
    ('Glow', SalonatColors.glow),
    ('Rose tint', SalonatColors.roseTint),
    ('Rose wash', SalonatColors.roseWash),
    ('Ground', SalonatColors.ground),
    ('Surface', SalonatColors.surface),
    ('Line', SalonatColors.line),
    ('Field', SalonatColors.field),
    ('Muted', SalonatColors.muted),
    ('Body', SalonatColors.body),
  ];

  @override
  Widget build(BuildContext context) {
    return GridView.count(
      crossAxisCount: 3,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      mainAxisSpacing: 12,
      crossAxisSpacing: 12,
      childAspectRatio: 1.4,
      children: [
        for (final (name, colour) in _colours)
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            spacing: 6,
            children: [
              Expanded(
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: colour,
                    borderRadius: BorderRadius.circular(SalonatRadii.input),
                    border: Border.all(color: SalonatColors.line),
                  ),
                  child: const SizedBox.expand(),
                ),
              ),
              Text(name, style: SalonatText.meta.copyWith(fontSize: 13)),
            ],
          ),
      ],
    );
  }
}
