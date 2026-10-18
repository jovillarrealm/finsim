# T04 — Compact 3-Column Month Detail Table with Collapsible Accordion

Status: closed
Depends On: T01, T02
Blocks: T07

## Description
Restructure the "Detalle del mes" section in `HorizontalComparisonChart.kt`:
- Replace the 36-line vertical stack with a compact 3-column table:
  - Column 1: Concept / Metric label.
  - Column 2: Original scenario value (with discrete copy button).
  - Column 3: Modified scenario value (with discrete copy button).
- Keep primary rows immediately visible: Capital balance, Total cash paid, and the 5 payment components (Regular capital, Extra capital, Interest, Insurance, Other charges).
- Enclose secondary accounting/regulatory rows in a subtle collapsible accordion button:
  `▼ Ver detalles contables y pendientes` (Commissions, VAT, Interest charged, Insurance charged, Pending interest, Pending insurance, Overdue capital, Arrears, Total debt, Unapplied surplus).

## Acceptance Criteria
- [x] 3-column table compares Original and Modified side-by-side.
- [x] Vertical height of the detail section is reduced by >60%.
- [x] Accordion smoothly expands and collapses secondary accounting metrics.
- [x] All values retain discrete copy capability and correct semantics.
