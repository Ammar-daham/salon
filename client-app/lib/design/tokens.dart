import 'package:flutter/widgets.dart';

/// The colours on the design's Foundations screen.
abstract final class SalonatColors {
  // Brand.

  /// Primary buttons and selection.
  static const rose = Color(0xFF9C5A65);

  /// Links, and text on rose tints.
  static const roseDeep = Color(0xFF7D4751);

  /// Text, dark cards and selected chips.
  static const plum = Color(0xFF2C191D);

  /// An accent used on plum only.
  static const glow = Color(0xFFE2B3A9);

  /// Secondary actions and icon wells.
  static const roseTint = Color(0xFFF9E8E4);

  /// Selected rows and unread items.
  static const roseWash = Color(0xFFFDF6F4);

  // Surfaces and text.

  /// The screen background.
  static const ground = Color(0xFFFAF8F6);

  /// Cards, inputs and sheets.
  static const surface = Color(0xFFFFFFFF);

  /// Card and chip borders.
  static const line = Color(0xFFE4DED8);

  /// Input borders.
  static const field = Color(0xFFCEC6BD);

  /// Secondary text, 6.6:1 on the ground.
  static const muted = Color(0xFF5F5852);

  /// Long-form text.
  static const body = Color(0xFF4A443F);

  // Status, always shown with a label: text colour on its background.

  static const waiting = Color(0xFF7C5113);
  static const waitingBackground = Color(0xFFFBF4E7);
  static const success = Color(0xFF2E6345);
  static const successBackground = Color(0xFFEAF5EE);
  static const error = Color(0xFF862720);
  static const errorBackground = Color(0xFFFBEEEC);

  /// Error borders and the danger button.
  static const errorFill = Color(0xFFA33229);
  static const info = Color(0xFF314A64);
  static const infoBackground = Color(0xFFEEF2F7);

  /// Finished things, such as a completed appointment.
  static const done = body;
  static const doneBackground = Color(0xFFF2EEEA);
}

/// Corner sizes, by what they round.
abstract final class SalonatRadii {
  static const slot = 10.0;
  static const input = 12.0;
  static const button = 14.0;
  static const card = 20.0;
  static const sheet = 24.0;
}

/// The smallest area a finger can tap, in each direction.
const minTouchTarget = 44.0;

/// Space between a screen's edges and its content.
const screenMargin = 20.0;
