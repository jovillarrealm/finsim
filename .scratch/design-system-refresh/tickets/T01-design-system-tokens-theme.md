# T01 — Tokens de Color y Tema Base («Warm Botanical Precision»)

- Status: completed
- Depends On: None
- Blocks: T02, T03, T04, T05

## Descripción
Actualizar la paleta fundacional en Compose Multiplatform (`Theme.kt`) y en CSS web (`src/styles.css`):
- `BrandForest` / `--green`: `#0F382A` (Botanical Pine).
- `BrandChartreuse` / `--lime`: `#C4F278` (Radiant Chartreuse).
- `BrandMint` / `--mint`: `#84E296` (Financial Mint para foco y acentos de ahorro).
- `TextMain` / `text-primary`: `#0F2922`.
- `TextForestMuted` / `text-muted` / `--muted`: `#52665D`.
- `BackgroundWarm` / `background`: `#F8F9F5` (Warm Porcelain Paper).
- `SurfaceCard` / `SurfaceWarm`: `#FFFFFF`.
- `BorderSage` / `--line`: `#E2E8DF`.
- `BorderInput`: `#CBD5C6`.
- `SurfaceHighlight`: `#F0FDF4`.

## Criterios de Aceptación
- [x] `Theme.kt` exporta los nuevos colores y configura `LightColorScheme` con `primary = BrandForest (#0F382A)`, `secondary = BrandChartreuse (#C4F278)`, `tertiary = BrandMint (#84E296)`.
- [x] `src/styles.css` actualiza las variables `:root` (`--green`, `--line`, `--muted`, `--lime`, `--mint`, `background: #f8f9f5`).
- [x] Ningún cálculo financiero ni contrato sufre modificaciones.
