# T03 — Interactive Dual Charts (Bottom Legends, Y-Axis Scale, Hover Tooltip)

Status: blocked
Depends On: T01
Blocks: T06, T07

## Description
Revamp the two line charts in `HorizontalComparisonChart.kt`:
- Relocate scenario toggles ("Original: visible" / "Modificado: visible") directly below Chart 1 (Capital Balance).
- Relocate component toggles ("Capital habitual", "Capital adicional", "Intereses pagados", "Seguros pagados", "Otros cargos") directly below Chart 2 (Monthly Payments by Component), styled with their corresponding series color indicators.
- Replace the plain text scale comment (`Escala: COP 0 a COP...`) with 3 to 4 compact formatted Y-axis labels aligned on the left edge along the horizontal grid lines.
- Implement an interactive vertical tracking line and floating tooltip card that displays the hovered month index and the exact amounts for all currently visible series.
- Increase chart height to 220dp for better spacing and clarity.

## Acceptance Criteria
- Toggles appear directly below their respective chart and function as interactive legends.
- Y-axis scale numbers are clearly visible on the left.
- Pointer hover over charts displays vertical tracking line and exact series values.
- Canvas height is 220dp.
