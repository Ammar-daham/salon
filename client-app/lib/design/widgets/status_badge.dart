import 'package:flutter/widgets.dart';

import '../theme.dart';
import '../tokens.dart';

/// The colours a [StatusBadge] can take.
enum BadgeTone {
  /// Waiting on someone, such as Awaiting confirmation.
  waiting(SalonatColors.waitingBackground, SalonatColors.waiting),

  /// Went well, such as Confirmed.
  success(SalonatColors.successBackground, SalonatColors.success),

  /// Over, such as Completed.
  done(SalonatColors.doneBackground, SalonatColors.done),

  /// Didn't happen, such as Cancelled or Missed.
  cancelled(SalonatColors.errorBackground, SalonatColors.error);

  const BadgeTone(this.background, this.foreground);

  final Color background;
  final Color foreground;
}

/// A short state with a dot before it, such as an appointment's Confirmed. The colour only backs up
/// the label, which always says what it means.
class StatusBadge extends StatelessWidget {
  const StatusBadge({super.key, required this.label, required this.tone});

  final String label;
  final BadgeTone tone;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 26,
      padding: const EdgeInsets.symmetric(horizontal: 10),
      decoration: ShapeDecoration(
        color: tone.background,
        shape: const StadiumBorder(),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        spacing: 6,
        children: [
          Container(
            width: 6,
            height: 6,
            decoration: BoxDecoration(
              color: tone.foreground,
              shape: BoxShape.circle,
            ),
          ),
          Text(
            label,
            style: SalonatText.label.copyWith(
              fontSize: 13,
              color: tone.foreground,
            ),
          ),
        ],
      ),
    );
  }
}
