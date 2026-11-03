import 'package:flutter/material.dart';

import '../theme.dart';
import '../tokens.dart';

/// A rounded box that groups one thing, such as a booking or a salon: white with a border, or plum
/// for the one thing a screen puts first, such as the next appointment.
///
/// With [onTap] the whole card is one button.
class SalonatCard extends StatelessWidget {
  /// A white card.
  const SalonatCard({
    super.key,
    required this.child,
    this.onTap,
    this.padding = const EdgeInsets.all(16),
  }) : dark = false;

  /// A plum card, with white text and icons.
  const SalonatCard.dark({
    super.key,
    required this.child,
    this.onTap,
    this.padding = const EdgeInsets.all(18),
  }) : dark = true;

  final Widget child;
  final VoidCallback? onTap;
  final EdgeInsetsGeometry padding;
  final bool dark;

  @override
  Widget build(BuildContext context) {
    final foreground = dark ? Colors.white : SalonatColors.plum;
    final onTap = this.onTap;
    Widget content = Padding(
      padding: padding,
      child: DefaultTextStyle.merge(
        style: SalonatText.body.copyWith(color: foreground),
        child: IconTheme.merge(
          data: IconThemeData(color: foreground),
          child: child,
        ),
      ),
    );
    if (onTap != null) {
      content = InkWell(onTap: onTap, child: content);
    }
    return Material(
      color: dark ? SalonatColors.plum : SalonatColors.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(SalonatRadii.card),
        side: dark
            ? BorderSide.none
            : const BorderSide(color: SalonatColors.line),
      ),
      clipBehavior: Clip.antiAlias,
      child: content,
    );
  }
}
