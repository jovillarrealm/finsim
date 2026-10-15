# Spec: Mobile Layout & Ergonomics Revamp

**Triage Label:** `ready-for-agent`  
**Status:** Ready for implementation  

---

## Problem Statement

When using Finsim on mobile devices (smartphones with screen widths between 320dp and 430dp), users encounter significant visual, navigational, and ergonomic defects compared to the web experience:

1. **Broken Font Glyphs ("Tofu" Boxes):** Top navigation tabs display missing-character placeholder boxes (e.g., `[] Préstamo`, `[] Conversor`, `[] Conciliación`) and footer disclaimers render broken symbols instead of clear typography.
2. **Cramped Multi-Column Form Inputs:** Loan parameter input fields are forced into two tight side-by-side columns, causing label clipping, dropdown truncation, and uncomfortable touch targets.
3. **Frustrating Keyboard Ergonomics:** Tapping numeric and financial fields triggers the standard alphanumeric QWERTY keyboard instead of a dedicated numeric/decimal keypad.
4. **Overcrowded Scenario Toolbar:** The active scenario title and primary actions (`[Guardar]`, `[Exportar]`, `[Ejemplo]`, `[Importar]`) share an unyielding horizontal row, causing buttons to squeeze, wrap awkwardly, or push the title into illegible truncation.
5. **Scroll Trap from Persistent Schedule Table:** The 36+ month amortization schedule table is expanded by default as a massive block with nested scrolling, trapping the user's thumb and horizontally truncating essential monthly financial columns (`Extra / Recup.`, `Seguros`, `Saldo final`).
6. **Constrained Summary Cards:** Key financial performance indicators (KPIs) in the loan summary cram multi-million currency amounts against tight card borders without graceful scaling or breathing room.

Users want the fluid, legible, and focused experience of the web version: single-column inputs, collapsible tables, clean typography, responsive action toolbars, and phone-friendly numeric keypads.

---

## Solution

Revamp the mobile presentation layer in Compose Multiplatform into an adaptive, ergonomics-first layout tailored for handheld touchscreens:

1. **Clean Glyphless Navigation:** Strip incompatible emoji codepoints from navigation pills and footnotes, adopting clean, high-contrast text labels and robust typography.
2. **Adaptive Single-Column Form Grid:** Refactor loan condition inputs to render as full-width, stacked fields on compact viewports (< 600dp), while reserving dual-column arrangements strictly for tablets and desktops.
3. **Native Decimal Keyboards:** Equip all monetary, interest rate, and month index fields with decimal and numeric input configurations to immediately surface the numeric keypad on Android.
4. **Responsive Stacked Scenario Toolbar:** Separate the scenario identity from action controls on mobile screens, displaying the scenario name prominently with a comfortable horizontal action row below it.
5. **Collapsible Amortization Accordion:** Transform the amortization schedule into an elegant, closed-by-default accordion (*"Ver cronograma de N meses ▾"*). When expanded, provide smooth horizontal panning with sticky headers and a compact card alternative for individual month inspection.
6. **Fluid KPI Readouts:** Adjust summary card typography and paddings to gracefully accommodate large formatted currency amounts without clipping.

---

## User Stories

1. As a mobile borrower, I want to see clean tab titles without missing-glyph boxes, so that I can confidently navigate between the loan simulator, rate converter, and reconciliation tools.
2. As a mobile borrower, I want the loan parameters form to display each input at full width on my phone, so that labels, helper text, and values are entirely readable without horizontal compression.
3. As a mobile user entering a principal amount, I want the Android system to automatically display a numeric keyboard with decimal support, so that I can type large numbers quickly without switching keyboard modes.
4. As a mobile user entering an interest rate percentage, I want the numeric keyboard to pop up immediately, so that I can input percentages with decimals effortlessly.
5. As a mobile user entering a loan term in months, I want a pure numeric keypad, so that I do not accidentally enter letters or invalid characters.
6. As a mobile user, I want the active scenario name to be displayed clearly across the width of the screen, so that long scenario descriptions do not get cut off into an unreadable string.
7. As a mobile user, I want scenario action buttons (`Guardar`, `Exportar`, `Ejemplo`, `Importar`) to be arranged in an accessible, touch-friendly row, so that I can tap them without misclicks.
8. As a mobile user reviewing my simulation, I want the loan summary KPI cards to display large monetary figures with clear spacing and readable font sizes, so that totals like cash paid and interest paid do not collide with card borders.
9. As a mobile user scrolling through my loan overview, I want the 36-month amortization schedule to be collapsed by default, so that it does not trap my vertical scroll gesture or push all subsequent sections off-screen.
10. As a mobile user interested in month-by-month details, I want to tap an explicit toggle to expand the amortization schedule, so that I only see granular numbers when I intentionally request them.
11. As a mobile user inspecting the expanded amortization schedule, I want smooth horizontal panning with sticky column headers, so that I can see scheduled capital, extra payments, interest, insurance, and closing balance without losing track of the month index.
12. As a mobile user selecting a specific month in the schedule, I want the selected month to highlight distinctly and update the event batch editor, so that I can adjust prepayment or catchup events for that exact period.
13. As a mobile user editing monthly events, I want the event editor cards and buttons (`Añadir mes`, `Aplicar cambios`, `Descartar`) to be properly sized for thumb interaction, so that editing multiple events is frictionless.
14. As a mobile user, I want the interactive balance comparison chart to include readable month labels along the bottom and currency markers on the vertical axis, so that I can interpret the trajectory of my debt at a glance.
15. As a mobile user, I want to switch between the loan simulation, rate converter, and statement reconciliation tabs smoothly, so that each tool presents an equally polished mobile-adapted layout.
16. As a mobile user using the rate converter, I want conversion cards to stack vertically on mobile, so that nominal, effective, and monthly rate cards have sufficient width for their dropdowns and results.
17. As a mobile user on the reconciliation tab, I want statement auditing fields to display in a legible vertical sequence, so that discrepancies and difference badges are prominently visible.
18. As a mobile user with a device camera cutout or navigation bar, I want safe inset paddings at the top and bottom of the screen, so that critical controls are never obscured by system UI.
19. As a tablet or desktop user, I want the layout to automatically expand into spacious multi-column grids and side-by-side comparisons, so that larger viewports maintain high information density.

---

## Implementation Decisions

### Decision 1: Viewport-Adaptive Layout Architecture
- Introduce a viewport width categorization at the root Compose presentation seam:
  - `Compact` (< 600dp): Phone portrait. Forces single-column forms, stacked scenario headers, collapsed amortization accordion, and fluid KPI cards.
  - `Medium / Expanded` (>= 600dp): Tablets and desktop displays. Preserves dual-column forms, side-by-side controls, and persistent table panes.
- Compute the viewport class via root composition local or window size metrics without coupling individual presentational composables to platform-specific code.

### Decision 2: Elimination of Tofu Boxes and Unicode Glyph Normalization
- Remove all decorative emoji icons from top-level navigation tabs. Replace with concise text labels: `"Préstamo"`, `"Conversor"`, `"Conciliación"`.
- Sanitize footer text and disclaimer strings, replacing unsupported Unicode separators (such as unmapped bullet points or special arrow glyphs) with standard typography (`·`, `-`, `/`).

### Decision 3: Single-Column Form Adaptation and Touch Ergonomics
- Refactor the loan condition drafting interface so that each field group (Principal, Term, Rate Type, Rate Value, Scheduled Payment, Payment Origin) stacks vertically in a single column on compact screens.
- Equip all monetary and numeric text input fields with explicit `KeyboardType.Decimal` or `KeyboardType.Number` configurations and `ImeAction.Next` / `ImeAction.Done` transitions.

### Decision 4: Collapsible Amortization Schedule Accordion
- Rebuild the schedule presentation module to be collapsed by default on compact viewports:
  - Header displays a clean toggle button with the row count badge: `"Cronograma mes a mes · N meses"` with an expand/collapse chevron.
  - When expanded, replace the nested `LazyColumn` inside vertical scroll with a non-virtualized, horizontally scrollable layout or a bounded container that prevents touch scroll-jacking.
  - Individual rows provide a compact summary with a tap-to-inspect detail sheet for deep monthly allocations (insurance, interest, overdue capital, catchup).

### Decision 5: Responsive Scenario Toolbar and Status Bar Insets
- On compact screens, split the scenario toolbar into:
  1. Upper row: Active scenario title and currency badge.
  2. Lower row: Horizontally scrollable or wrapped action chips (`Guardar`, `Exportar`, `Ejemplo`, `Importar`).
- Preserve status bar and navigation bar insets across all screens so that notches and gesture bars do not overlap interactive surfaces.

---

## Testing Decisions

### What Makes a Good Test
- **External Behavior Over Implementation Details:** Tests must verify what the user sees and interacts with (e.g., verifying that a single-column layout renders when width is 400dp, verifying that the schedule is collapsed initially and expands on click, verifying that inputs request decimal keyboards, and confirming that tab labels contain valid text without missing-glyph symbols).
- **Cross-Platform Seam:** Test at the Compose Multiplatform UI seam using Compose UI test rules, ensuring identical ergonomic behavior on Android and Web targets.

### Modules to Be Tested
1. **Interactive Presentation Seam (`AppTest` / `ComposeUiTest`):**
   - Verification of compact viewport behavior: tabs render valid text, scenario toolbar wraps cleanly, and amortization table initializes collapsed.
   - Verification of expansion interaction: tapping the schedule toggle expands the rows and exposes month selection.
2. **Form Presentation Seam (`LoanFormTest`):**
   - Verification that monetary and rate input fields provide appropriate keyboard options.
   - Parity check with the underlying `LoanConditionsDraft` module to ensure validation and formatting remain uncorrupted by layout changes.
3. **Visual Verification Seam:**
   - Automated screenshot capture and layout hierarchy inspection on mobile dimensions (412x915) via Android emulator and Skiko Web viewport emulation.

### Prior Art
- Existing unit test suite in `kmp/shared/src/commonTest/` validating `LoanConditionsDraft`, `EventDraftBatch`, and `WorkspaceStore`.
- Visual regression captures in the project artifact directory (`screenshot-mobile-top.png`, `screenshot-mobile-scrolled.png`).

---

## Out of Scope

- Changes to financial calculation formulas, payment allocation priority, or decimal precision logic (governed canonically by `:shared` under ADR 0001).
- Offline document OCR or neural VLM weight downloads (covered separately by the Document Ingestion spec).
- Introduction of third-party charting dependencies; charting improvements remain within native Skiko/Compose canvas primitives.

---

## Further Notes

- All color tokens, typography scales, and surface elevation tokens must strictly adhere to the established Finsim visual identity (`BrandForest` `#173F32`, `BrandLime` `#D7E8A3`, `BrandLimeDark` `#839954`, `SurfaceWarm` `#FFFFFF`, `BorderSage` `#DCE3D8`, `TextForestMuted` `#617168`).
