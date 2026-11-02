import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../theme.dart';
import '../tokens.dart';

/// A text field with its label above it, and what's wrong below it when there's an [error].
///
/// The field's look comes from the theme's input decoration, so a plain [TextField] matches it.
class SalonatTextField extends StatelessWidget {
  const SalonatTextField({
    super.key,
    required this.label,
    this.controller,
    this.initialValue,
    this.hint,
    this.error,
    this.keyboardType,
    this.textInputAction,
    this.autofillHints,
    this.obscureText = false,
    this.enabled = true,
    this.onChanged,
    this.onSubmitted,
  }) : assert(
         controller == null || initialValue == null,
         'Give the text in the controller, or as initialValue, not both',
       );

  final String label;
  final TextEditingController? controller;

  /// The text the field starts with, when there's no [controller].
  final String? initialValue;

  /// Shown in the empty field.
  final String? hint;

  /// What's wrong with the text, such as "Enter a full email address". Turns the border red.
  final String? error;

  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final Iterable<String>? autofillHints;
  final bool obscureText;
  final bool enabled;
  final ValueChanged<String>? onChanged;
  final ValueChanged<String>? onSubmitted;

  @override
  Widget build(BuildContext context) {
    final error = this.error;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      spacing: 8,
      children: [
        // The field carries the label for screen readers.
        ExcludeSemantics(child: Text(label, style: SalonatText.label)),
        Semantics(
          label: label,
          child: TextFormField(
            controller: controller,
            initialValue: initialValue,
            style: SalonatText.input,
            keyboardType: keyboardType,
            textInputAction: textInputAction,
            autofillHints: autofillHints,
            obscureText: obscureText,
            enabled: enabled,
            onChanged: onChanged,
            onFieldSubmitted: onSubmitted,
            decoration: InputDecoration(
              hintText: hint,
              error: error == null
                  ? null
                  : Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      spacing: 6,
                      children: [
                        const Padding(
                          padding: EdgeInsets.only(top: 1),
                          child: Icon(
                            LucideIcons.info,
                            size: 16,
                            color: SalonatColors.error,
                          ),
                        ),
                        Expanded(
                          child: Text(
                            error,
                            style: SalonatText.meta.copyWith(
                              fontSize: 13,
                              height: 18 / 13,
                              color: SalonatColors.error,
                            ),
                          ),
                        ),
                      ],
                    ),
            ),
          ),
        ),
      ],
    );
  }
}
