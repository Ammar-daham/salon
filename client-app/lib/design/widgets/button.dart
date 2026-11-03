import 'package:flutter/material.dart';

import '../theme.dart';
import '../tokens.dart';

/// What a [SalonatButton] is for, which sets its colours.
enum ButtonKind {
  /// The screen's main action, such as Book appointment: rose.
  primary,

  /// An action next to the main one, such as Keep appointment: white with a border.
  secondary,

  /// Plum, such as Continue with Apple.
  dark,

  /// An action that can't be undone, such as Cancel appointment: red.
  danger,

  /// A small action inside a card or list, such as Book: rose tint.
  tint,
}

/// A button as wide as the space it's given: 54 px tall, or 44 px for [ButtonKind.tint].
///
/// Without [onPressed] it shows as disabled.
class SalonatButton extends StatelessWidget {
  const SalonatButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.kind = ButtonKind.primary,
    this.icon,
  });

  final String label;
  final VoidCallback? onPressed;
  final ButtonKind kind;

  /// Shown before the label.
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final small = kind == ButtonKind.tint;
    final (background, foreground, border) = switch (kind) {
      ButtonKind.primary => (SalonatColors.rose, Colors.white, null),
      ButtonKind.secondary => (
        SalonatColors.surface,
        SalonatColors.plum,
        const BorderSide(color: SalonatColors.field),
      ),
      ButtonKind.dark => (SalonatColors.plum, Colors.white, null),
      ButtonKind.danger => (SalonatColors.errorFill, Colors.white, null),
      ButtonKind.tint => (SalonatColors.roseTint, SalonatColors.roseDeep, null),
    };

    final style = ButtonStyle(
      minimumSize: WidgetStatePropertyAll(
        Size.fromHeight(small ? minTouchTarget : 54),
      ),
      padding: const WidgetStatePropertyAll(
        EdgeInsets.symmetric(horizontal: 16),
      ),
      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
      shape: WidgetStatePropertyAll(
        RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(
            small ? SalonatRadii.input : SalonatRadii.button,
          ),
        ),
      ),
      side: WidgetStateProperty.resolveWith(
        (states) => states.contains(WidgetState.disabled) ? null : border,
      ),
      backgroundColor: WidgetStateProperty.resolveWith(
        (states) => states.contains(WidgetState.disabled)
            ? SalonatColors.line
            : background,
      ),
      foregroundColor: WidgetStateProperty.resolveWith(
        (states) => states.contains(WidgetState.disabled)
            ? SalonatColors.muted
            : foreground,
      ),
      overlayColor: WidgetStatePropertyAll(foreground.withValues(alpha: 0.12)),
      textStyle: WidgetStatePropertyAll(
        small ? SalonatText.button.copyWith(fontSize: 15) : SalonatText.button,
      ),
      iconSize: const WidgetStatePropertyAll(20),
    );

    final icon = this.icon;
    return icon == null
        ? TextButton(style: style, onPressed: onPressed, child: Text(label))
        : TextButton.icon(
            style: style,
            onPressed: onPressed,
            icon: Icon(icon),
            label: Text(label),
          );
  }
}
