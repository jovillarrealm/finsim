# 08 — Explorar cambios sin perder la cotización original

**What to build:** Editar monto, plazo, tasa y cargos, aplicar o descartar escenarios hipotéticos y retomarlos conservando la cotización de referencia.

**Blocked by:** 02, 04

**Status:** ready-for-agent

- [ ] La cotización original sigue identificable y separada de las entradas hipotéticas. Se pueden modificar monto, plazo, tasa y cargos en ambos modos y en los productos disponibles.
- [ ] Aplicar actualiza conjuntamente todas las vistas desde una instantánea; descartar recupera las entradas aplicadas. Una edición inválida conserva el trabajo y muestra error junto al campo.
- [ ] Modificar una cotización no escala pagos ni infiere una tasa. Los pagos dependientes requieren actualización/revisión o pasar expresamente a una simulación con datos suficientes.
- [ ] Modificar país, moneda, producto, capital, plazo o tasa revalida lo dependiente. Un cargo/impuesto ligado a una base cambiada no conserva certeza confirmada sin revisión o supuesto explícito.
- [ ] Al convertir un cargo a desconocido se retira el costo completo afectado; ningún gráfico o total mezcla resultados anteriores con entradas nuevas aplicadas.
- [ ] Guardar y recuperar preserva original, aplicado y pendiente por separado. La carga recalcula solo lo aplicado, sin aplicar cambios silenciosamente ni almacenar cronogramas como fuente de verdad.
- [ ] Se prueba el flujo completo editar–error–corregir–aplicar–descartar–guardar–retomar desde el espacio de trabajo. Las entregas posteriores de compra, tarjeta o contexto mantienen este comportamiento al incorporar sus campos.
