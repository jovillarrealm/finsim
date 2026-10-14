# 07 — Relacionar ofertas con el margen mensual personal

**What to build:** Añadir ingresos, gastos y cuotas vigentes de forma opcional y comparar el margen mensual que dejaría cada oferta.

**Blocked by:** 06

**Status:** ready-for-agent

- [ ] El contexto personal puede añadirse progresivamente u omitirse sin bloquear las ofertas. Ingresos, gastos y cuotas vigentes se identifican como referencia mensual constante bajo un supuesto visible.
- [ ] El margen resta gastos, cuotas vigentes y salidas nuevas por mes; en mes 0 incorpora aporte y cargos iniciales conocidos. Usa las mismas entradas aplicadas que gráficos y totales.
- [ ] Los cargos preexistentes se mantienen en ambas alternativas y se incorporan una vez según su cobertura declarada. Cobertura o importe desconocido impide afirmar margen completo.
- [ ] Las entradas monetarias son no negativas donde corresponde, pero el margen admite signo y nunca se recorta a cero ni se fuerza al tipo de dinero no negativo.
- [ ] F05 produce 380,00 y 360,00 con ingreso 1.000,00, gastos 400,00 y cuotas vigentes 100,00; con ingreso 600,00 produce -20,00 y -40,00.
- [ ] Se prueba contexto omitido, incompleto, cero explícito, cargos preexistentes cubiertos/no cubiertos y salida inicial. Guardar/retomar conserva contexto y su cobertura.
- [ ] La presentación no convierte el margen en saldo acumulado, ahorro, disponibilidad del aporte, aprobación de crédito, puntaje ni recomendación automática.
