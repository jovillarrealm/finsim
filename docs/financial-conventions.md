# Convenciones financieras del simulador

Estado: reglas seleccionadas siguiendo las recomendaciones aceptadas por el usuario. Motor implementado y referencias automatizadas verificadas; estado de integración y aceptación en [tracking](tracking.md).

Este es un modelo educativo mensual. No representa las condiciones de un banco ni una política legal de cobro. Para afirmar coincidencia con una entidad, será necesario reconciliar un ejemplo real.

## 1. Periodos, cuota y redondeo

- Préstamos en COP, ARS, USD o EUR (una moneda por escenario, sin conversión de importes), con tasa fija y periodos mensuales completos. Los eventos pertenecen a un mes; no se calculan intereses diarios ni prorrateos.
- El saldo inicial de un préstamo ingresado a mitad de su vida representa capital al inicio del próximo periodo y supone que está al día. Importar atrasos anteriores queda fuera de esta versión.
- La cuota base contiene capital e intereses; el seguro se añade por separado. Puede ingresarse una cuota conocida o calcularse a partir del saldo, tasa y plazo restantes.
- Para capital `P`, tasa mensual `r` y `n` meses, la cuota calculada es `P*r/(1-(1+r)^(-n))`. Si `r=0`, es `P/n`.
- Usar aritmética decimal con 40 dígitos significativos. No redondear la tasa convertida a la precisión de su etiqueta visual.
- Registrar importes monetarios con dos decimales y redondeo de mitades hacia arriba: `1,005 → 1,01`. Redondear la cuota calculada, el interés de cada mes y cada cargo de seguro por separado. Derivar capital y saldos de esos importes registrados; no redondear independientemente componentes que deben sumar la cuota.
- Los importes ingresados o importados deben tener como máximo dos decimales; rechazar mayor precisión con una explicación. Las tasas pueden tener más decimales.
- Sumar los cargos y pagos ya registrados para obtener totales. No recalcular un total a partir de cargos sin redondear. No acumular fracciones inferiores a un centavo como deuda oculta.
- La última cuota se ajusta al saldo e intereses pendientes. Incorporar cualquier residuo debido exclusivamente al redondeo a la última cuota prevista, sin crear un mes adicional por ese residuo. Los atrasos reales sí pueden extender el plazo. Nunca perdonar capital ni producir saldos negativos.

## 2. Secuencia de cada mes

1. Tomar el capital inicial del mes.
2. Calcular y registrar interés ordinario sobre ese capital y los seguros activos. Los pagos de este mes no reducen esos cargos.
3. Registrar el capital exigible de la cuota del mes: el mayor entre cero y `cuota base - interés corriente`, limitado al capital que todavía no está vencido. El capital vencido es parte del saldo, no una deuda adicional.
4. Aplicar el pago habitual, salvo que el mes esté marcado como omitido.
5. Aplicar el pago de recuperación explícito, adicional al habitual.
6. Aplicar los abonos extraordinarios, incluidos los recurrentes.
7. Registrar saldo, cargos pendientes y capital vencido al cierre.

Cada pago sigue este orden: seguros pendientes, intereses pendientes y capital. Dentro de cada categoría se atiende lo más antiguo primero; al pagar capital se cubre primero el capital vencido. Por tanto, un seguro corriente precede a un interés antiguo: la categoría tiene prioridad sobre la antigüedad.

El pago habitual disponible es la cuota base más el seguro corriente. No aumenta automáticamente para cubrir atrasos. En el último mes se ajusta al importe necesario para liquidar las obligaciones del préstamo. Los seguros que continúan después de liquidarlo conservan su propio calendario.

Si un abono extraordinario cubre seguros o intereses, mostrar esa distribución: solo su parte aplicada a capital se denomina «abono a capital». El excedente ofrecido por encima de las obligaciones exigibles y el capital restante se muestra como no aplicado y no cuenta como dinero pagado. No se anticipan seguros de meses futuros.

## 3. Seguros

- Cada seguro tiene importe fijo mensual o tasa porcentual mensual explícita. Un `0,1 %` significa multiplicar el capital inicial por `0,001`.
- Calcular los porcentuales sobre el capital inicial del mes, antes de cualquier pago. Redondear cada seguro y después sumar.
- Cobrar el mes completo de liquidación; no devolver ni prorratear ese cargo.
- Si termina con el préstamo, no generar cargos desde el mes siguiente a su liquidación. «Liquidación» exige capital, intereses y seguros vencidos en cero.
- Si continúa después, exigir un mes final inclusivo, medido desde el inicio de la simulación. El seguro fijo continúa hasta ese mes; el porcentual vale cero cuando el capital inicial es cero. El mes final también limita su cobro si el préstamo se extiende.
- En un mes omitido el seguro activo se genera y queda pendiente. No se capitaliza ni genera intereses.
- Distinguir fecha de liquidación del préstamo y último mes de seguro. Incluir los seguros posteriores en el total de caja y en la comparación temporal.

## 4. Omisiones y recuperación

- «Omitir cuota» elimina el pago habitual completo, incluido el seguro corriente; no elimina los cargos. No elimina pagos de recuperación o extraordinarios programados explícitamente para ese mismo mes.
- El interés ordinario sigue calculándose sobre el capital pendiente. No hay penalidades, tasa de mora, gastos de cobranza ni intereses sobre intereses o seguros pendientes.
- El capital impagado permanece en el saldo. Registrar qué parte está vencida sin sumarla nuevamente al total adeudado.
- Al cierre, los atrasos son seguros pendientes + intereses pendientes + capital vencido. La deuda total es capital restante + intereses pendientes + seguros pendientes. Nunca sumar los atrasos completos a la deuda total.
- Después de una omisión se reanuda la cuota base habitual; la recuperación es un importe adicional elegido por el usuario. Si no alcanza, los atrasos continúan visibles.
- Los abonos reducen plazo, no recalculan a la baja la cuota base. Los atrasos pueden extenderlo. No inventar un pago final de recuperación para forzar una fecha de liquidación.
- Si el dinero previsto no permite amortizar, mostrar que no se alcanza la liquidación. Como límite operativo, calcular hasta 1.200 meses y conservar todos los saldos pendientes; un resultado truncado no puede presentarse como costo total ni como ahorro definitivo.

## 5. Ejemplos de referencia

Los importes siguientes son valores esperados definidos con operaciones elementales, independientes del futuro motor. En esta tabla se usa punto decimal para facilitar su uso posterior en pruebas.

| Caso | Entradas y operación | Resultado esperado |
| --- | --- | --- |
| Mitad de centavo | Capital `100.50`, tasa mensual `1 %` | Interés `1.005 → 1.01` |
| Cuota y conservación | Capital `100.00`, tasa cero, 3 meses | Pagos `33.33`, `33.33`, `33.34`; capital total `100.00`; saldo final cero |
| Cuota con interés | Capital `1000.00`, tasa mensual `1 %`, 2 meses, sin seguro | Cuota calculada `507.51`; mes 1: interés `10.00`, capital `497.51`, saldo `502.49`; mes 2: interés `5.02`, pago final `507.51` |
| Base del seguro y liquidación | Capital `1000.00`, tasa cero, seguro mensual `0.1 %`, pago base `100.00` y extra ofrecido `1000.00` | Seguro `1.00`; pago habitual `101.00`; extra aplicado `900.00`, no aplicado `100.00`; caja `1001.00`; saldo cero; sin seguro el mes siguiente si termina al liquidar |
| Seguro fijo posterior | Capital `100.00`, tasa cero, cuota `100.00`, seguro fijo `10.00` que continúa hasta el mes 3 | Préstamo liquidado en mes 1; caja mensual `110.00`, `10.00`, `10.00`; caja total `130.00` |
| Dos seguros redondeados | Capital `100.50`, dos seguros de `1 %` cada uno | Cada cargo `1.01`; seguro total `2.02`, no `2.01` |

### Omisión con recuperación completa

Capital inicial `1000.00`; tasa mensual `1 %`; cuota base `100.00`; seguro fijo mensual `5.00` que termina al liquidar. Se omite el mes 1 y se añade una recuperación de `105.00` en el mes 2.

| Concepto | Mes 1 | Mes 2 |
| --- | ---: | ---: |
| Capital inicial | 1000.00 | 1000.00 |
| Interés generado | 10.00 | 10.00 |
| Seguro generado | 5.00 | 5.00 |
| Pago habitual realizado | 0.00 | 105.00 |
| Recuperación realizada | 0.00 | 105.00 |
| Total aplicado a seguros | 0.00 | 10.00 |
| Total aplicado a intereses | 0.00 | 20.00 |
| Total aplicado a capital | 0.00 | 180.00 |
| Capital final | 1000.00 | 820.00 |
| Capital vencido al cierre | 90.00 | 0.00 |
| Interés pendiente al cierre | 10.00 | 0.00 |
| Seguro pendiente al cierre | 5.00 | 0.00 |
| Atrasos al cierre | 105.00 | 0.00 |
| Deuda total al cierre | 1015.00 | 820.00 |

Control: en el mes 2, `210.00 = 10.00 + 20.00 + 180.00`; el interés es `10.00`, no `10.15`, porque los cargos pendientes no se capitalizan.

Variantes independientes del mismo mes 2:

- Sin recuperación: caja `105.00`, seguros `10.00`, intereses `20.00`, capital pagado `75.00`; saldo `925.00`, capital vencido y atrasos `105.00`.
- Con recuperación de `20.00`: caja `125.00`, capital pagado `95.00`; saldo `905.00`, capital vencido y atrasos `85.00`.
- Otra omisión y solo recuperación de `7.00`: se aplica únicamente a seguros; quedan seguro `3.00`, intereses `20.00`, capital vencido `180.00` y saldo de capital `1000.00`. Atrasos `203.00`; deuda total `1023.00`.

## 6. Verificación requerida antes de publicar

- Convertir los ejemplos anteriores en pruebas con resultados esperados literales, sin generarlos con el motor bajo prueba.
- Verificar conservación: capital inicial = capital pagado + capital final; cargos generados = cargos pagados + cargos pendientes; caja aplicada = seguros pagados + intereses pagados + capital pagado.
- Comprobar que el capital vencido nunca supera el capital restante y que todos los importes pendientes son no negativos.
- Comprobar identidad exacta entre escenario original y modificado sin eventos, y el ajuste final por redondeo en plazos largos.
- Verificar omisiones consecutivas, recuperación insuficiente, coincidencia de omisión y abono, abonos superiores a la deuda y eventos posteriores a la liquidación. Estos últimos no generan pagos del préstamo y se señalan como no aplicados.
- Verificar seguros fijos y porcentuales, mes de liquidación, continuación y mes final inclusivo; no confundir cargos generados con caja pagada.
- Para tasas, comprobar ida y vuelta con error absoluto máximo `1e-12` en tasas expresadas como fracción. Ejemplo exacto: mensual `0.01` → efectiva anual `0.126825030131969720661201`; nominal anual `0.12` con 12 capitalizaciones → mensual `0.01`. Las fórmulas son `EA=(1+EM)^12-1` y `EM=(1+j/m)^(m/12)-1` para nominal anual `j` y frecuencia `m`.
- Validar entradas e importaciones: capital y cuota positivos, tasas y cargos no negativos, meses y frecuencias enteros positivos, importes finitos y eventos dentro del horizonte admitido. Permitir tasa cero. Rechazar referencias a meses inexistentes o posteriores al límite de 1.200 meses.
- Verificar el estado de simulación incompleta y evitar presentar saldos truncados como liquidación.
- Reconciliar un ejemplo real antes de afirmar equivalencia con un prestamista. Las diferencias deben mostrarse, no corregirse silenciosamente.

Verificación actual: ejemplos literales e invariantes comprobados en `src/engine/loan.test.ts` y conversiones en `src/engine/rates.test.ts`. Las pruebas de navegador están en `tests/acceptance.spec.ts`. No se ha reconciliado un extracto real ni afirmado equivalencia con un prestamista.
