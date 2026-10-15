# FS-18 — Resultados copiables y gráfico unificado del préstamo

Estado: entrevista abierta, 2026-10-14. Primera ronda respondida; acuerdos parciales y decisiones pendientes distinguidos abajo. Referencia: UI KMP de FS-17 en f49a64f; convenciones financieras vigentes. No se modifica código durante el grill.

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

Segunda frontera:

1. Comparación y filtros: cómo representar original/modificado en ambos gráficos y qué se puede ocultar.
2. Alcance del pago: cuota habitual frente a todo lo pagado en el mes, incluyendo abonos y recuperaciones; conservar su distinción.
3. Copia: lugares concretos y formato del importe pegado.

Después de resolver la frontera:

- Copia: formato pegado, moneda, precisión, feedback, teclado y móvil; convivencia con campos editables.
- Composición: seguros, extras, recuperación, pagos omitidos e intereses pendientes; ejes, escala y colores.
- Visibilidad: toggles por escenario/componente, valores iniciales, comportamiento al ocultar todo, duración de preferencias.
- Interacción: selección mensual, detalle, edición y navegación accesible en teléfono/escritorio.
- Sustitución: criterios concretos para retirar doble pista y confirmar entendimiento compartido.

## Decisiones acordadas

- Acción de copia disponible en varias partes; no limitarla a Cuota base. Lugares concretos pendientes.
- Composición de pagos de cada mes, no de acumulados.
- Dos gráficos uno debajo del otro: saldo de capital arriba; composición mensual abajo mediante barras verticales apiladas.
- Cambiar también saldo a barras verticales, conforme a la propuesta del usuario. Mostrar cierres mensuales discretos; no interpolar un saldo diario que el motor no calcula.
- Todavía no está acordado cómo mostrar original/modificado, cuáles capas estarán activas, si los abonos integran la barra ni el formato de copia. La alineación temporal entre gráficos es una recomendación pendiente de concretar.
- Retirada de doble pista sigue condicionada a conservar inspección/edición y representación financiera, con criterios de aceptación por acordar. No requiere ADR por ahora.

## Límites

Diseño de presentación; no cambiar fórmulas ni contratos financieros para dibujar el gráfico. Sin implementación, integración ni publicación. La reserva FS-17 y sus pendientes de aceptación continúan vigentes.
