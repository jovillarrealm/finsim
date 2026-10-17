# T01 — Color Palette & Theme Tokens (Esmeralda y Menta Financiera)

Status: ready
Depends On: None
Blocks: T02, T03, T04, T05

## Description
Revitalize the visual presentation of Finsim by implementing the "Esmeralda y Menta Financiera" palette:
- Replace dull beige/alabaster background with clean, luminous `#F8FAF8`.
- Set panel surfaces to crisp white (`#FFFFFF`) with subtle borders (`#E2E8F0` / `#D1DCD0`).
- Update `BrandForest` (`#144436`) for structural titles and hero cards.
- Add `BrandEmerald` (`#059669`) and `BrandMint` (`#10B981`) for active elements, positive indicators, and interactive highlights.
- Update `LightColorScheme` in `Theme.kt` and base surfaces in `App.kt` and `LoanSummary.kt`.

## Acceptance Criteria
- `Theme.kt` defines the agreed palette tokens.
- Cards, background, and navigation bars display the refreshed colors without breaking accessibility contrast.
- Visual inspection via browser reflects crisp white cards on clean background with forest/mint highlights.
