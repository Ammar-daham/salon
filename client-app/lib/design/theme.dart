import 'package:flutter/material.dart';

import 'tokens.dart';

/// The type scale on the design's Foundations screen: Outfit throughout, in plum unless a style says
/// otherwise.
abstract final class SalonatText {
  static const family = 'Outfit';

  static const _base = TextStyle(
    fontFamily: family,
    color: SalonatColors.plum,
    fontWeight: FontWeight.w400,
  );

  /// A tab's title, such as Bookings: 30/36 SemiBold.
  static final screenTitle = _base.copyWith(
    fontSize: 30,
    height: 36 / 30,
    fontWeight: FontWeight.w600,
    letterSpacing: -0.3,
  );

  /// A booking step's title, such as Pick a time: 26/32 SemiBold.
  static final stepTitle = _base.copyWith(
    fontSize: 26,
    height: 32 / 26,
    fontWeight: FontWeight.w600,
    letterSpacing: -0.26,
  );

  /// A section heading, such as Free times today: 20/26 SemiBold.
  static final section = _base.copyWith(
    fontSize: 20,
    height: 26 / 20,
    fontWeight: FontWeight.w600,
  );

  /// The name of a service, salon or booking in a list: 16/24 SemiBold.
  static final itemTitle = _base.copyWith(
    fontSize: 16,
    height: 24 / 16,
    fontWeight: FontWeight.w600,
  );

  /// Text in fields, which stays at 16 so iOS doesn't zoom in on them.
  static final input = _base.copyWith(fontSize: 16, height: 22 / 16);

  /// Running text: 15/22 Regular.
  static final body = _base.copyWith(fontSize: 15, height: 22 / 15);

  /// Details next to a title, such as a category and distance: 14/20, muted.
  static final meta = _base.copyWith(
    fontSize: 14,
    height: 20 / 14,
    color: SalonatColors.muted,
  );

  /// A field's label: 14 Medium.
  static final label = _base.copyWith(
    fontSize: 14,
    height: 20 / 14,
    fontWeight: FontWeight.w500,
  );

  /// A button's text: 16 SemiBold.
  static final button = _base.copyWith(
    fontSize: 16,
    height: 22 / 16,
    fontWeight: FontWeight.w600,
  );

  /// A small heading above a group, such as UPCOMING: 13 SemiBold, tracked and muted. The text is
  /// given in capitals.
  static final overline = _base.copyWith(
    fontSize: 13,
    height: 18 / 13,
    fontWeight: FontWeight.w600,
    letterSpacing: 0.78,
    color: SalonatColors.muted,
  );
}

/// Salonat's look, from the design's Foundations screen, for Material's own widgets. The app's own
/// widgets in `widgets/` read the tokens directly.
ThemeData salonatTheme() {
  const scheme = ColorScheme(
    brightness: Brightness.light,
    primary: SalonatColors.rose,
    onPrimary: Colors.white,
    primaryContainer: SalonatColors.roseTint,
    onPrimaryContainer: SalonatColors.roseDeep,
    secondary: SalonatColors.plum,
    onSecondary: Colors.white,
    error: SalonatColors.errorFill,
    onError: Colors.white,
    errorContainer: SalonatColors.errorBackground,
    onErrorContainer: SalonatColors.error,
    surface: SalonatColors.surface,
    onSurface: SalonatColors.plum,
    onSurfaceVariant: SalonatColors.muted,
    outline: SalonatColors.field,
    outlineVariant: SalonatColors.line,
    surfaceTint: Colors.transparent,
  );

  OutlineInputBorder border(Color color, double width) => OutlineInputBorder(
    borderRadius: BorderRadius.circular(SalonatRadii.input),
    borderSide: BorderSide(color: color, width: width),
  );

  return ThemeData(
    colorScheme: scheme,
    scaffoldBackgroundColor: SalonatColors.ground,
    fontFamily: SalonatText.family,
    // Material's own buttons and icon buttons get a 48 px tap area, above the 44 px minimum.
    materialTapTargetSize: MaterialTapTargetSize.padded,
    dividerColor: SalonatColors.line,
    textTheme: TextTheme(
      headlineLarge: SalonatText.screenTitle,
      headlineMedium: SalonatText.stepTitle,
      titleLarge: SalonatText.section,
      titleMedium: SalonatText.itemTitle,
      bodyLarge: SalonatText.input,
      bodyMedium: SalonatText.body,
      bodySmall: SalonatText.meta,
      labelLarge: SalonatText.button,
      labelMedium: SalonatText.label,
      labelSmall: SalonatText.overline,
    ),
    textSelectionTheme: const TextSelectionThemeData(
      cursorColor: SalonatColors.rose,
      selectionHandleColor: SalonatColors.rose,
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: SalonatColors.surface,
      // 22 px of text between 15 px above and below makes the design's 52 px field.
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 15),
      hintStyle: SalonatText.input.copyWith(color: SalonatColors.muted),
      errorStyle: SalonatText.meta.copyWith(
        fontSize: 13,
        color: SalonatColors.error,
      ),
      border: border(SalonatColors.field, 1),
      enabledBorder: border(SalonatColors.field, 1),
      disabledBorder: border(SalonatColors.line, 1),
      focusedBorder: border(SalonatColors.rose, 2),
      errorBorder: border(SalonatColors.errorFill, 2),
      focusedErrorBorder: border(SalonatColors.errorFill, 2),
    ),
  );
}
