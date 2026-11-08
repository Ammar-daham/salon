import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../theme.dart';
import '../tokens.dart';

/// The app's main sections, one per tab.
enum MainTab {
  discover('Discover', LucideIcons.compass),
  bookings('Bookings', LucideIcons.calendar),
  // Lucide's icons are outlines only, so the selected heart is Material's filled one.
  favourites('Favourites', LucideIcons.heart, Icons.favorite_rounded),
  profile('Profile', LucideIcons.user);

  const MainTab(this.label, this.icon, [this.selectedIcon]);

  final String label;
  final IconData icon;

  /// The icon while this tab is selected, when it isn't [icon].
  final IconData? selectedIcon;
}

/// The bar of [tabs] along the bottom of the main screens. The [selected] one is rose.
class SalonatTabBar extends StatelessWidget {
  const SalonatTabBar({
    super.key,
    required this.selected,
    required this.onSelected,
    this.tabs = MainTab.values,
  });

  final MainTab selected;
  final ValueChanged<MainTab> onSelected;
  final List<MainTab> tabs;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: const BoxDecoration(
        color: SalonatColors.surface,
        border: Border(top: BorderSide(color: SalonatColors.line)),
      ),
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(8, 6, 8, 6),
          child: Row(
            children: [
              for (final tab in tabs)
                Expanded(
                  child: _TabItem(
                    tab: tab,
                    selected: tab == selected,
                    onTap: () => onSelected(tab),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _TabItem extends StatelessWidget {
  const _TabItem({
    required this.tab,
    required this.selected,
    required this.onTap,
  });

  final MainTab tab;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final color = selected ? SalonatColors.rose : SalonatColors.muted;
    return MergeSemantics(
      child: Semantics(
        button: true,
        selected: selected,
        child: InkResponse(
          onTap: onTap,
          radius: 32,
          // Grows past 54 px with large text.
          child: ConstrainedBox(
            constraints: const BoxConstraints(minHeight: 54),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              mainAxisAlignment: MainAxisAlignment.center,
              spacing: 3,
              children: [
                Icon(
                  selected ? tab.selectedIcon ?? tab.icon : tab.icon,
                  size: 24,
                  color: color,
                ),
                Text(
                  tab.label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: SalonatText.label.copyWith(
                    fontSize: 12,
                    height: 16 / 12,
                    fontWeight: selected ? FontWeight.w600 : FontWeight.w500,
                    color: color,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
