# FS-18 — Resultados copiables y gráfico unificado del préstamo

Estado: entrevista abierta, 2026-10-14. Ninguna propuesta de esta nota está aprobada todavía. Referencia: UI KMP de FS-17 en f49a64f; convenciones financieras vigentes. No se modifica código durante el grill.

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

Primera frontera (sin respuestas todavía):

1. Copia: selección de texto, acción de copiar el valor completo o ambas; alcance de los resultados copiables.
2. Composición: pagos mensuales o acumulados; su relación visual con saldo de capital.
3. Comparación: escenario modificado como foco con original opcional, o ambos siempre visibles.

Después de resolver la frontera:

- Copia: formato pegado, moneda, precisión, feedback, teclado y móvil; convivencia con campos editables.
- Composición: seguros, extras, recuperación, pagos omitidos e intereses pendientes; ejes, escala y colores.
- Visibilidad: toggles por escenario/componente, valores iniciales, comportamiento al ocultar todo, duración de preferencias.
- Interacción: selección mensual, detalle, edición y navegación accesible en teléfono/escritorio.
- Sustitución: criterios concretos para retirar doble pista y confirmar entendimiento compartido.

## Decisiones acordadas

Ninguna aún. Incorporar respuestas aquí conforme se resuelvan. Actualizar glosario solo al fijar términos; no requiere ADR por ahora.

## Límites

Diseño de presentación; no cambiar fórmulas ni contratos financieros para dibujar el gráfico. Sin implementación, integración ni publicación. La reserva FS-17 y sus pendientes de aceptación continúan vigentes.
