import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../design/design.dart';

/// The tabs shown. Favourites stays hidden until salon-backend keeps them, task B15 of the plan;
/// its branch and screen are there already.
const visibleTabs = [MainTab.discover, MainTab.bookings, MainTab.profile];

/// The main screens, one branch of [navigationShell] per [MainTab] in its order, with the tab bar
/// along the bottom. Each tab keeps its own stack of screens.
class MainShell extends StatelessWidget {
  const MainShell({super.key, required this.navigationShell});

  final StatefulNavigationShell navigationShell;

  @override
  Widget build(BuildContext context) {
    final current = MainTab.values[navigationShell.currentIndex];
    return Scaffold(
      body: navigationShell,
      bottomNavigationBar: SalonatTabBar(
        selected: current,
        tabs: visibleTabs,
        // Tapping the tab already open goes back to its first screen.
        onSelected: (tab) => navigationShell.goBranch(
          tab.index,
          initialLocation: tab == current,
        ),
      ),
    );
  }
}
