import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../theme.dart';
import '../tokens.dart';

/// The colours for a picture that isn't there yet, so a list of salons or staff isn't all one colour.
enum PlaceholderTone {
  rose(Color(0xFFF0D3CD), Color(0xFF9C5A65), SalonatColors.roseDeep),
  sand(SalonatColors.line, Color(0xFF7A716A), SalonatColors.body),
  blue(Color(0xFFDAE3EE), Color(0xFF4A6D93), SalonatColors.info),
  green(Color(0xFFD3E9DB), Color(0xFF468A63), SalonatColors.success),
  gold(Color(0xFFF5E6C8), Color(0xFFAD7C22), SalonatColors.waiting),
  plum(Color(0xFF4A2B31), Color(0xFFCD8F8E), Color(0xFFF0D3CD));

  const PlaceholderTone(this.background, this.icon, this.text);

  final Color background;

  /// A photo placeholder's icon.
  final Color icon;

  /// An avatar's initials.
  final Color text;
}

/// Where a photo goes until there is one: a tinted box with a picture icon.
class PhotoPlaceholder extends StatelessWidget {
  const PhotoPlaceholder({
    super.key,
    this.width,
    this.height,
    this.radius = SalonatRadii.card,
    this.tone = PlaceholderTone.rose,
    this.iconSize = 28,
  });

  /// Fills the space given without one.
  final double? width;
  final double? height;
  final double radius;
  final PlaceholderTone tone;
  final double iconSize;

  @override
  Widget build(BuildContext context) {
    return ExcludeSemantics(
      child: Container(
        width: width,
        height: height,
        decoration: BoxDecoration(
          color: tone.background,
          borderRadius: BorderRadius.circular(radius),
        ),
        alignment: Alignment.center,
        child: Icon(LucideIcons.image, size: iconSize, color: tone.icon),
      ),
    );
  }
}

/// A circle with someone's initials, for a member of staff or the client.
///
/// Screen readers skip it, so show the name next to it.
class InitialsAvatar extends StatelessWidget {
  const InitialsAvatar({
    super.key,
    required this.name,
    this.size = 44,
    this.tone = PlaceholderTone.rose,
  });

  final String name;
  final double size;
  final PlaceholderTone tone;

  /// The first letters of the first and last words of [name], such as OW for Olivia Weber.
  static String initialsOf(String name) {
    final words = name.trim().split(RegExp(r'\s+')).where((w) => w.isNotEmpty);
    if (words.isEmpty) {
      return '';
    }
    String first(String word) => String.fromCharCode(word.runes.first);
    final initials = words.length == 1
        ? first(words.first)
        : first(words.first) + first(words.last);
    return initials.toUpperCase();
  }

  @override
  Widget build(BuildContext context) {
    return ExcludeSemantics(
      child: Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          color: tone.background,
          shape: BoxShape.circle,
        ),
        alignment: Alignment.center,
        child: Text(
          initialsOf(name),
          style: SalonatText.label.copyWith(
            fontSize: (size * 0.38).roundToDouble(),
            fontWeight: FontWeight.w600,
            color: tone.text,
            height: 1,
          ),
        ),
      ),
    );
  }
}
