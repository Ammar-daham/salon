import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../design/design.dart';

/// A way on from a [PlaceholderScreen] to another screen.
class ScreenLink {
  /// Opens [location] on top of this screen, so Back returns here.
  const ScreenLink(this.label, this.location) : replace = false;

  /// Goes to [location] instead of this screen, such as back to a tab.
  const ScreenLink.go(this.label, this.location) : replace = true;

  final String label;
  final String location;
  final bool replace;
}

/// A screen still to be built: its name, the design template and plan task it comes from, and a
/// button to each screen the design leads to from it.
class PlaceholderScreen extends StatelessWidget {
  const PlaceholderScreen({
    super.key,
    required this.title,
    required this.template,
    required this.task,
    this.links = const [],
    this.children = const [],
  });

  final String title;

  /// The screen's template in `design/templates`, such as `Home`.
  final String template;

  /// The plan's task that builds it, such as C2.
  final String task;

  final List<ScreenLink> links;

  /// Shown above the links, for a screen that already does something.
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: SalonatColors.ground,
        scrolledUnderElevation: 0,
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(screenMargin, 0, screenMargin, 32),
        children: [
          Semantics(
            header: true,
            child: Text(title, style: SalonatText.screenTitle),
          ),
          const SizedBox(height: 4),
          Text(
            'To be built in $task, from the design’s $template screen.',
            style: SalonatText.meta,
          ),
          const SizedBox(height: 24),
          for (final child in children) ...[child, const SizedBox(height: 10)],
          for (final link in links) ...[
            SalonatButton(
              label: link.label,
              kind: ButtonKind.secondary,
              onPressed: () => link.replace
                  ? context.go(link.location)
                  : context.push(link.location),
            ),
            const SizedBox(height: 10),
          ],
        ],
      ),
    );
  }
}
