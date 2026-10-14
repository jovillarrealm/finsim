# Issue 01: Mobile Layout & Ergonomics Revamp

**What to build:** Refactor the Compose Multiplatform mobile presentation layer to provide a phone-first experience matching the original web version:
1. Strip broken Unicode/emoji glyphs (`[]`) from navigation tabs and footer notices, restoring clean text labels (`Préstamo`, `Conversor`, `Conciliación`).
2. Adapt the loan parameters form (`LoanForm`) to render in a single full-width column on mobile screens (< 600dp), with appropriate decimal/numeric keyboard types (`KeyboardType.Decimal`).
3. Make the amortization schedule table (`MonthlyTable`) collapsible via an accordion (closed by default) to eliminate vertical scroll trapping on mobile and provide smooth horizontal panning when opened.
4. Adapt the scenario toolbar to stack the active scenario title above touch-friendly action buttons.
5. Ensure fluid KPI font sizing and comfortable paddings in `LoanSummary` cards.

**Blocked by:** None (can start immediately)

**Triage Label:** `ready-for-agent`

**Status:** ready

- [ ] Clean text navigation tabs without emoji glyph fallback boxes.
- [ ] Single-column adaptive layout for `LoanForm` on compact phone screens.
- [ ] Explicit decimal keyboard configuration (`KeyboardType.Decimal`, `KeyboardType.Number`) for loan inputs.
- [ ] Stacked, responsive scenario toolbar in `App.kt` for compact screens.
- [ ] Collapsible accordion for `MonthlyTable` with row count badge and smooth horizontal panning.
- [ ] Fluid typography for `LoanSummary` KPI cards preventing currency overflow.
- [ ] Verification on Android emulator (`medium_phone`) via screenshot/layout and test suite passing.
