# T08 — Gráfico de Trayectoria de Saldo con Zona de Extinción

- Status: completed
- Depends On: T06
- Blocks: None

## Descripción
Implementar en la vista de gráficos la trayectoria de saldo con ilustración de brecha y zona de extinción según las pantallas `5b0f011c` y `268286d1`:
- Curva original pactada punteada (`#717974`, 48 meses).
- Curva modificada con escalones pronunciados en meses con prepagos (`#0D9488`, 3.5px).
- Polígono de brecha de amortización con relleno degradado sutil (`#0D9488` con opacidad del 22% al 4%).
- **Zona de extinción anticipada:**
  - Rectángulo sombreado en `#ECFDF5` entre el mes de liquidación (ej. Mes 36) y el fin de plazo original (Mes 48).
  - Borde discontinuo en `#A7F3D0`.
  - Etiqueta centrada: `LIBRE DE DEUDA · $0 pagos requeridos`.
- Badges de hito flotantes en coordenadas SVG correspondientes a los abonos (ej. `M6: -$5M`, `M18: -$3M`, `Mes 36: $0`).
- Cursor interactivo de referencia (`#111827`) que sincroniza el mes seleccionado con la tarjeta de inspección.

## Criterios de Aceptación
- [x] La curva modificada refleja los saldos exactos al cierre de cada mes (`row.closingBalance`).
- [x] La zona de extinción se activa únicamente cuando el escenario modificado liquida el préstamo antes del plazo original.
- [x] Al pulsar o hacer hover en cualquier punto de la curva, el mes activo se actualiza.
