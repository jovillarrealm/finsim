# T05 — Continuous Scrolling Month-by-Month Schedule with Sticky Headers

Status: closed
Depends On: T01, T02
Blocks: T06, T07

## Description
Refactor `MonthlyTable.kt`:
- Eliminate the rigid 12-month pagination buttons (`Tabla anterior` / `Tabla siguiente`).
- Provide smooth vertical scrolling within a bounded container (max-height 500dp) alongside horizontal scrolling.
- Keep the header row sticky / fixed at the top of the table container so column titles remain visible during vertical scrolling.
- Display all months of the loan result seamlessly.

## Acceptance Criteria
- Table displays all months with vertical and horizontal scrolling.
- Headers remain visible as the user scrolls through months M1 to M36+.
- Discrete copy actions work smoothly within each table cell.
