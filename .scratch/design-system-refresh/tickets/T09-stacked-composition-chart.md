# T09 — Gráfico de Composición Mensual con Torres de Abono

- Status: completed
- Depends On: T06, T08
- Blocks: None

## Descripción
Implementar el gráfico de composición de cuota mensual y desembolso por componentes según la pantalla `5b0f011c`:
- Series apiladas según la prelación contractual:
  1. Base: Capital habitual programado (`#2563EB`).
  2. Segunda capa: Intereses corrientes causados (`#D97706`).
  3. Tercera capa: Seguros obligatorios (`#7C3AED`).
  4. Bloque superior: Capital extraordinario de abono (`#0D9488`).
- **Torres de abono:** En los meses con eventos a capital (ej. M06 y M18), se dibuja una columna sobresaliente con bloque verde esmeralda que representa el monto prepagado.
- **Efecto de cuña de intereses:** Visualizar cómo la capa de intereses (`#D97706`) se comprime inmediatamente en los meses subsiguientes al abono.
- **Zona de cuotas extinguidas:** Recuadro delimitado en verde claro para los meses posteriores a la liquidación con la leyenda `$0 CUOTAS RESTANTES`.
- Sincronización bidireccional del mes seleccionado con el Gráfico de Trayectoria (T08).

## Criterios de Aceptación
- [x] La suma de las alturas apiladas en cada mes coincide exactamente con `row.cashPaid`.
- [x] Los meses posteriores a `payoffMonth` se muestran con altura $0 y zona de ahorro resaltada.
- [x] Leyenda interactiva con filtros para alternar la visibilidad de componentes.
