# 04 — Incorporar cargos y explicar capital, efectivo y costo

**What to build:** Añadir cargos a cotizaciones y simulaciones y ver una contabilidad de caja que distingue dinero recibido, pagos iniciales y costo de financiación.

**Blocked by:** 03

**Status:** ready-for-agent

- [ ] Cada cargo tiene identidad y tratamiento único: propio inicial, descontado, financiado, posterior separado o incluido en cuota. La interfaz permite importe conocido, estimado o desconocido y calendario cuando corresponda.
- [ ] El neto utilizable descuenta del capital los cargos financiados/descontados que no se aplican a la necesidad. Un cargo financiado o incluido en cuota no se vuelve a cobrar como salida independiente.
- [ ] El resumen distingue capital, neto, salida inicial, total pagado y costo nominal de financiación. Se conserva y explica una discrepancia contable sin ajustar silenciosamente capital, pagos o necesidad.
- [ ] Los cargos recurrentes amplían el horizonte común hasta su final conocido. Final desconocido significa costo parcial y subtotal con alcance visible; nunca costo de vida completo ni cero futuro ficticio.
- [ ] Un porcentaje cuya base/regla no esté modelada exige importe conocido o estimación explícita. No se calculan impuestos universales ni se introducen tasas predeterminadas por país.
- [ ] F01 se comprueba de extremo a extremo: capital 1.060,00, cargos financiado 40,00/descontado 20,00/propio 30,00, neto 1.000,00, dos pagos de 530,00, total 1.090,00 y costo 90,00.
- [ ] Se prueban cargo incluido, varios cargos y seguros sin duplicación, borrado a desconocido que vuelve parcial el costo y recurrencia posterior a las cuotas. Gráfico, tabla, resumen y guardado disponible conservan el mismo tratamiento.
