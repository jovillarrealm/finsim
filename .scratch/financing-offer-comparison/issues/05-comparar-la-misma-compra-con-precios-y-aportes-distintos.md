# 05 — Comparar la misma compra con precios y aportes distintos

**What to build:** Comparar dos formas de financiar el mismo bien, mediante préstamo o compra aislada en cuotas, incorporando descuentos y aportes propios.

**Blocked by:** 04

**Status:** in-progress

- [ ] La necesidad de compra identifica una misma adquisición y permite precio, descuento conocido y aporte propio por oferta. Se admiten préstamo personal y compra aislada con tarjeta; no saldo rotativo, mínimos ni nuevas compras.
- [ ] Se comprueba capital igual a precio menos aporte más cargos financiados/descontados que deba cubrir. Una discrepancia conserva el borrador y explica qué revisar; no corrige silenciosamente el precio.
- [ ] El mes inicial separa aporte y cargos propios. Total pagado incluye cada salida una vez; costo nominal se calcula contra el precio de esa oferta y la diferencia de precios se muestra por separado.
- [ ] Se admiten cotizaciones sin tasa y simulaciones únicamente cuando sus condiciones coinciden con el modelo fijo mensual. Un calendario variable conocido no se presenta como simulación de una promoción.
- [ ] F02 reproduce A: inicial 210,00, cuotas 350,00, total 910,00 y costo 10,00; B: cuatro pagos de 250,00, total 1.000,00 y costo 0,00 sin desglose inventado.
- [ ] Para F02, B menos A muestra total +90,00, inicial -210,00 y última cuota +2 meses con inicio común en mes 1. No se declara ganador ni disponibilidad del aporte.
- [ ] Se prueba necesidad distinta, aporte incompatible y capital no positivo; las comparaciones en efectivo siguen funcionando y el guardado disponible incluye las nuevas entradas.
