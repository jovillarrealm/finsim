# T07 — Tarjeta Hero de Ahorro y Cuadrícula KPI 2x2

- Status: completed
- Depends On: T06
- Blocks: None

## Descripción
Adaptar el bloque de resumen ejecutivo visual observado en Stitch móvil y desktop:
- **Tarjeta Hero Principal ("Ahorro Neto Proyectado"):**
  - Fondo `surface-highlight` (`#F0FDF4`) con sutil resplandor difuso (`radial blur`).
  - Badge de porcentaje de ahorro (ej. `-36% intereses` en `#065F46` sobre `#ECFDF5`).
  - Importe de ahorro neto en tipografía `tabular-nums` de 26px (`font-bold`).
  - Indicador de tiempo ganado (`Tiempo ganado: 12 meses (1 año)`) y chip de estado `Libre en Mes 36/48`.
- **Cuadrícula Secundaria 2x2:**
  - KPI 1: Cuota mensual fija (`Capital + Intereses + Seguros`).
  - KPI 2: Intereses acumulados con badge de delta (`-COP 3.42M`) y valor pactado tachado.
  - KPI 3: Seguros totales acumulados con chip de cuotas ahorradas (`-12 cuotas`).
  - KPI 4: Total desembolso de bolsillo con comparativa antes vs. después.

## Criterios de Aceptación
- [x] Todas las cifras se calculan dinámicamente a partir del resultado de `simulateLoan`.
- [x] No existen valores estáticos o ficticios.
- [x] Todos los números usan `font-variant-numeric: tabular-nums`.
