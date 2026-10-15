# 06 — Distinguir tarjetas existentes, nuevas y cargos preexistentes

**What to build:** Ver el costo adicional de una compra según se use una tarjeta existente o nueva, separando obligaciones que ya existían.

**Blocked by:** 05

**Status:** in-progress

- [ ] Cada oferta de tarjeta declara existente o nueva; sus cargos identifican si nacen con la operación o ya existían, junto con importe, inclusión en pago y calendario.
- [ ] Un cargo preexistente no se atribuye al costo incremental de la compra. Un cargo nuevo se cuenta una vez, aunque aparezca también como componente explicativo de una cuota.
- [ ] La obligación preexistente se conserva como base común de ambas alternativas personales. Se registra si ya está cubierta en gastos/cuotas para que el contexto pueda contarla una sola vez.
- [ ] Cuando un pago cotizado contiene un cargo preexistente, se exige conocer su importe para separar la salida incremental; si falta, se mantiene el pago conocido y se marca la limitación.
- [ ] El mantenimiento nuevo puede continuar después de las cuotas. Un final desconocido conserva costo parcial y no supone cancelación automática de la tarjeta.
- [ ] F04 muestra compra existente con pagos nuevos 600,00/costo 0,00 y tarjeta nueva con pagos nuevos 700,00/costo 100,00 para cinco cargos nuevos de 20,00. Se prueba también cargo preexistente incluido en cuota, sin importe y sin duplicación.
- [ ] El estado, las vistas y el guardado disponible conservan las distinciones; préstamo y compra sin tarjeta mantienen sus resultados.
