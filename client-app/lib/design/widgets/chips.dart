import 'package:flutter/material.dart';

import '../theme.dart';
import '../tokens.dart';

/// A filter or choice that's on or off, such as Available today. Plum when selected.
///
/// It's drawn 38 px tall and takes up 44 px, so it's easy to tap.
class SalonatChip extends StatelessWidget {
  const SalonatChip({
    super.key,
    required this.label,
    required this.selected,
    required this.onSelected,
    this.icon,
  });

  final String label;
  final bool selected;

  /// Called with the value the chip should change to.
  final ValueChanged<bool>? onSelected;

  /// Shown before the label.
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final foreground = selected ? Colors.white : SalonatColors.plum;
    final icon = this.icon;
    final onSelected = this.onSelected;
    return _Tappable(
      height: 38,
      shape: StadiumBorder(
        side: BorderSide(
          color: selected ? SalonatColors.plum : SalonatColors.line,
        ),
      ),
      color: selected ? SalonatColors.plum : SalonatColors.surface,
      selected: selected,
      onTap: onSelected == null ? null : () => onSelected(!selected),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        spacing: 6,
        children: [
          if (icon != null) Icon(icon, size: 18, color: foreground),
          Text(label, style: SalonatText.label.copyWith(color: foreground)),
        ],
      ),
    );
  }
}

/// A free time to book, such as 10:15. Rose when selected.
///
/// It's drawn 40 px tall and takes up 44 px, so it's easy to tap.
class TimeSlot extends StatelessWidget {
  const TimeSlot({
    super.key,
    required this.label,
    required this.onTap,
    this.selected = false,
  });

  final String label;
  final VoidCallback? onTap;
  final bool selected;

  @override
  Widget build(BuildContext context) {
    return _Tappable(
      height: 40,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(SalonatRadii.slot),
        side: selected
            ? BorderSide.none
            : const BorderSide(color: SalonatColors.line),
      ),
      color: selected ? SalonatColors.rose : SalonatColors.surface,
      selected: selected,
      onTap: onTap,
      child: Text(
        label,
        style: SalonatText.body.copyWith(
          fontWeight: FontWeight.w500,
          color: selected ? Colors.white : SalonatColors.plum,
        ),
      ),
    );
  }
}

/// A control drawn [height] tall, inside a [minTouchTarget] tall area that takes the taps.
class _Tappable extends StatelessWidget {
  const _Tappable({
    required this.height,
    required this.shape,
    required this.color,
    required this.selected,
    required this.onTap,
    required this.child,
  });

  final double height;
  final ShapeBorder shape;
  final Color color;
  final bool selected;
  final VoidCallback? onTap;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return MergeSemantics(
      child: Semantics(
        button: true,
        selected: selected,
        enabled: onTap != null,
        onTap: onTap,
        child: GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTap: onTap,
          excludeFromSemantics: true,
          child: ConstrainedBox(
            constraints: const BoxConstraints(
              minWidth: minTouchTarget,
              minHeight: minTouchTarget,
            ),
            child: Center(
              widthFactor: 1,
              heightFactor: 1,
              child: Material(
                color: color,
                shape: shape,
                clipBehavior: Clip.antiAlias,
                child: InkWell(
                  onTap: onTap,
                  excludeFromSemantics: true,
                  child: SizedBox(
                    height: height,
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 14),
                      // As wide as the label, rather than the space given.
                      child: Center(widthFactor: 1, child: child),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
