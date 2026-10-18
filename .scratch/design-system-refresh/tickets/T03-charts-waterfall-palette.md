# T03 — Cascada Semántica de Gráficos (Web y Compose)

- Status: completed
- Depends On: T01
- Blocks: T05

## Descripción
Actualizar los colores de series de gráficos en `src/features/loan/ScenarioComparison.tsx`, `Comparison.css` y `kmp/composeApp/.../HorizontalComparisonChart.kt` para reflejar con precisión la paleta semántica de Stitch:
- Capital Habitual (`scheduled`): `#2563EB` (Dark: `#1D4ED8`).
- Extra y Recuperación (`extra`): `#0D9488` (Dark: `#059669`).
- Intereses Pagados (`interest`): `#D97706` (Dark: `#EA580C`).
- Seguros Pagados (`insurance`): `#7C3AED` (Dark: `#8B5CF6`).
- Cursor de seguimiento / Reference line: `#111827`.

## Criterios de Aceptación
- [x] `ScenarioComparison.tsx` utiliza los códigos `#2563EB`, `#0D9488`, `#D97706`, `#7C3AED` en sus barras Recharts.
- [x] `HorizontalComparisonChart.kt` en Compose Multiplatform utiliza los mismos tonos semánticos para sus trazos y áreas.
- [x] Línea de cursor vertical se mantiene en `#111827`.
