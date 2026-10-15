# 01 — Comparar dos cotizaciones de préstamo en efectivo

**What to build:** Introducir progresivamente dos cotizaciones para la misma necesidad de efectivo y consultar pagos mensuales, diferencias y límites de la información, sin conocer una tasa.

**Blocked by:** None — can start immediately

**Status:** in-progress

- [ ] Se accede al comparador desde la app y se conserva el flujo de préstamos existente. Se eligen país, moneda, necesidad neta y ofertas A/B; una oferta incompleta puede consultarse sin exigir contexto personal.
- [ ] Se introducen cantidad e importe de cuotas, excepciones por mes y comienzo del calendario. Una fecha ausente produce alineación relativa estimada desde mes 1; el mes inicial y las cuotas en ese mismo mes conservan sus conceptos separados.
- [ ] Las entradas distinguen registrado, estimado, desconocido y cero confirmado. Se conservan campos ausentes y errores de edición sin sustituir el resultado aplicado; completar y aplicar actualiza conjuntamente gráfico, tabla y resumen.
- [ ] Ambas ofertas comparten escala, horizonte y selección de mes. El detalle accesible muestra pagos sin inventar capital/interés; el resumen muestra cuota/rango, duración y pagos conocidos. Las diferencias tienen signo y etiqueta B menos A, sin ganador automático.
- [ ] Solo se declaran totales y costo completos cuando sus datos lo permiten. Moneda o necesidad incompatible bloquean diferencias equivalentes; no se convierten divisas ni se deduce el país de la moneda.
- [ ] Se rechazan dinero con más de dos decimales, entradas negativas donde no corresponden, valores no finitos y meses/cuotas no enteros o superiores a 1.200. Los agregados usan aritmética decimal y las diferencias conservan el signo.
- [ ] Una prueba del espacio de trabajo reproduce F03: ocho pagos de 150,00 suman 1.200,00, pero el cargo desconocido impide costo completo. Otra verifica calendarios de distinto inicio/plazo y coherencia de las vistas. Controles y errores tienen etiquetas y acceso por teclado.
