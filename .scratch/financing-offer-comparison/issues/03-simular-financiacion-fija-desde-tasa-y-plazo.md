# 03 — Simular financiación fija desde tasa y plazo

**What to build:** Comparar una cotización con una simulación mensual de tasa fija compatible, viendo cuota y desglose calculado con las convenciones existentes.

**Blocked by:** 01

**Status:** ready-for-agent

- [ ] Cada oferta permite modo cotización o simulación. La simulación pide capital, tasa fija con convención/frecuencia, plazo y comienzo; permite cuota base conocida compatible y tasa cero explícita.
- [ ] Se reutiliza el motor canónico Kotlin Multiplatform y sus conversiones, precisión y redondeos; no se añade otro motor ni se amplía el motor TypeScript congelado.
- [ ] El desglose de capital, interés y seguros solo aparece cuando es calculable. Se admiten seguros existentes compatibles, su mes final y los posteriores al préstamo; no se disfrazan cargos generales como seguros.
- [ ] Una cotización conserva sus pagos y procedencia aunque difiera del modelo. Se muestran diferencias sin ajustar automáticamente la tasa ni exigir una tasa a la cotización.
- [ ] Se identifican cálculo incompleto, residuo final y límites de alcance. No se activan abonos, mora, recuperación, interés diario, indexación o promociones por etapas.
- [ ] F06 se verifica desde el espacio de trabajo con expectativas literales: 507,51 por cuota, intereses 10,00 y 5,02, saldo 502,49 y total 1.015,02; la variante de tasa cero paga 33,33, 33,33 y 33,34 sin mes adicional.
- [ ] Se comprueban tasa cero frente a desconocida, conversiones sin redondear la tasa a su etiqueta y seguro posterior. Si el guardado ya existe, preserva estas nuevas entradas y recalcula al cargar.
