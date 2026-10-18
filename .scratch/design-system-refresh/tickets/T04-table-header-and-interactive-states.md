# T04 — Tabla de Amortización y Estados Interactivos

- Status: completed
- Depends On: T01
- Blocks: T05

## Descripción
Ajustar la tabla de amortización mensual (`MonthlyTable.tsx` / `styles.css` / Compose `MonthlyTable.kt`):
- Encabezado `thead th`: fondo `#E6ECE2` (o `#E6F4EA`), texto `11px` mayúscula, color `#4D5E4C`.
- Fila cebra alternada: `#FFFFFF` y `#F9FBFA`.
- Fila seleccionada / Resalte: `#F0FDF4` (o tinte menta suave `#D1FAE5`).
- Fila hover: `#F4F6F1`.
- Columna inicial (mes y fecha) fija (*sticky*).

## Criterios de Aceptación
- [x] La tabla en web y Compose adopta los fondos de cabecera y hover definidos.
- [x] La alineación numérica permanece estricta con `tabular-nums`.
