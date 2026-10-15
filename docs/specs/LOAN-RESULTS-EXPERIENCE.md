# FS-18 — Resultados copiables y gráfico unificado del préstamo

Estado: entrevista abierta, 2026-10-14. Dos rondas respondidas; acuerdos parciales y decisiones pendientes distinguidos abajo. Referencia: UI KMP de FS-17 en f49a64f; convenciones financieras vigentes. No se modifica código durante el grill.

## Solicitud

Hacer copiables los campos, por ejemplo Cuota base del resumen. Mostrar composición intereses/capital en la curva del saldo y permitir limpiar la vista mediante toggles. Retirar la vista de doble pista una vez que la experiencia unificada funcione bien.

## Hechos del producto

- LoanSummary muestra Cuota base y otros resultados como Text; no ofrece selección ni acción de copia.
- HorizontalComparisonChart alterna Curva y Doble pista. La curva muestra saldo de capital restante; doble pista contiene el desglose de pagos mensuales.
- Saldo es una existencia al cierre; capital/intereses pagados son movimientos del periodo. No deben sumarse en una misma pila.
- Intereses generados y pagados pueden diferir con omisiones/recuperaciones. Un extra puede cubrir primero seguros e intereses; solo su porción destinada a capital es abono a capital.
- La interfaz permite seleccionar mes y editar eventos; retirar una vista requiere conservar ese recorrido y la alternativa de tabla.
- El resumen recibe el escenario modificado. La curva empieza en el saldo posterior a M1; sus etiquetas Capital Ahorrado/Deuda evitada nombran una diferencia de saldos, no ahorro financiero. Revisar lenguaje y punto inicial al diseñar.
- Doble pista agrupa capital de extra y recuperación bajo Extra; comisiones/IVA pueden estar en caja sin segmento actual. El reemplazo debe decidir su representación y mantener seguros posteriores a liquidación visibles.

## Árbol de decisiones

Primera frontera (respondida):

1. Copia: el usuario confirma utilidad de acción de copia en varias partes. Ubicaciones y formato pendientes; selección libre no confirmada.
2. Composición: confirma pagos mensuales, no acumulados.
3. Presentación: solicita segundo gráfico debajo del saldo, con barras verticales que desglosen interés/capital por cuota; propone sustituir también la línea de saldo por barras verticales. Visibilidad de escenarios aún pendiente.

Segunda frontera (respondida):

1. El usuario reconsidera barras contiguas: «volvamos a las lineas». Saldo vuelve a dos líneas original/modificado; no se asume aceptación de barras contiguas en el gráfico inferior.
2. Acepta recomendación: representar todo lo efectivamente pagado durante el mes; distinguir capital habitual y adicional, intereses, seguros y otros cargos cuando existan.
3. Acepta recomendación: acciones de copia en resumen, detalle mensual y valores de tabla; importes con dos decimales y coma decimal, sin moneda ni separadores de miles.

Tercera frontera:

1. Gráfico inferior: un escenario seleccionado o dos barras apiladas contiguas por mes.
2. Filtros de componentes: ocultar frente a atenuar, con lectura inequívoca de importe visible frente a total pagado.
3. Horizonte largo en teléfono: meses comprimidos en pantalla frente a tramo mensual navegable.

Después de resolver la frontera:

- Copia: feedback, teclado y móvil; convivencia con campos editables.
- Composición: representación de recuperación, pagos omitidos e intereses pendientes; ejes, escala y colores.
- Visibilidad: toggles por escenario/componente, valores iniciales, comportamiento al ocultar todo, duración de preferencias.
- Interacción: selección mensual, detalle, edición y navegación accesible en teléfono/escritorio.
- Sustitución: criterios concretos para retirar doble pista y confirmar entendimiento compartido.

## Decisiones acordadas

- Acción de copia en resumen, detalle del mes y valores de tabla. Importes pegados con dos decimales y coma decimal, sin moneda ni separadores de miles (ejemplo: 507,51). Selección libre no exigida.
- Composición de pagos de cada mes, no de acumulados.
- Dos gráficos uno debajo del otro: saldo de capital arriba mediante líneas original/modificado; composición mensual abajo mediante barras verticales apiladas. El retorno a líneas sustituye la propuesta anterior de barras para saldo. Las conexiones muestran tendencia entre cierres; no representan cálculo diario.
- El gráfico inferior representa todo lo efectivamente pagado: capital habitual, capital adicional de abonos/recuperación, intereses, seguros y otros cargos cuando existan. Conservar distribución real; un extra no equivale íntegramente a capital. El excedente no aplicado no integra el pago.
- Todavía no están acordados escenarios del gráfico inferior, capas activas ni navegación del horizonte. La alineación temporal y selección común entre gráficos siguen como recomendación por concretar.
- Retirada de doble pista sigue condicionada a conservar inspección/edición y representación financiera, con criterios de aceptación por acordar. No requiere ADR por ahora.

## Límites

Diseño de presentación; no cambiar fórmulas ni contratos financieros para dibujar el gráfico. Sin implementación, integración ni publicación. La reserva FS-17 y sus pendientes de aceptación continúan vigentes.
