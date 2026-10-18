# 01: KMP Compose Mobile Shell & Floating Navigation Bar

**What to build:** In Compose Multiplatform (Android & Desktop), introduce the mobile-first floating bottom navigation bar with 4 destinations (Simulador, Cronograma, Comparar, Ajustes) respecting device system insets, and the brand header with the Radiant Chartreuse dot and active currency badge.

**Blocked by:** None (can start immediately)

**Status:** closed

- [x] In Compose mobile views, a floating bottom navigation bar is anchored to the bottom with blurred background and safe insets (`navigationBarsPadding()`).
- [x] 4 primary destinations are selectable: Simulador, Cronograma, Comparar, Ajustes.
- [x] Brand header displays `finsim.` with Radiant Chartreuse dot (`#C4F278`) and active currency chip.
- [x] Minimum touch target sizes of 48x48dp are respected for accessibility.
