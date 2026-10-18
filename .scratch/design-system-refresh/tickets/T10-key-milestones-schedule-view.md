# T10 — Cronograma de Hitos Decisivos y Vista Reducida Móvil

- Status: completed
- Depends On: T06
- Blocks: None

## Descripción
Implementar una vista curada de *Hitos Clave* para el cronograma de amortización, pensada para dispositivos móviles y consultas rápidas:
- **Filtrado Curado de Filas:**
  - Mes 01: Inicio del préstamo y primera cuota.
  - Mes anterior a cada abono extraordinario: Muestra el saldo y estado inmediatamente previo al impacto.
  - Meses de abono extraordinario: Tarjeta destacada con icono de rayo (`bolt`), badge `+X Extra`, desglose del saldo posterior y desembolso total.
  - Mes de liquidación final anticipada: Tarjeta en `#ECFDF5` con icono de verificación (`verified`), badge `Fin de Crédito`, y saldo `$0,00 (100% Amortizado)`.
- Selector de modo: `Hitos Clave` vs. `Tabla Completa`.
- Botón accesible para exportar el cronograma íntegro en formato CSV.

## Criterios de Aceptación
- [x] La vista de hitos identifica automáticamente los meses de inicio, pre-abono, abono y liquidación sin lógica quemada fija.
- [x] Si no hay abonos extraordinarios, muestra inicio, mitad de plazo y finalización.
- [x] Se mantiene el acceso a la tabla mensual exhaustiva en 2D con scroll sincronizado.
