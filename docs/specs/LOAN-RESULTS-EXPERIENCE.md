# FS-18 — Resultados copiables y gráfico unificado del préstamo

Estado: entrevista abierta, 2026-10-14. Tres rondas respondidas; acuerdos y propuesta de cierre distinguidos abajo. Referencia: UI KMP de FS-17 en f49a64f; convenciones financieras vigentes. No se modifica código durante el grill.

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

Tercera frontera (respondida):

1. Ambos escenarios también abajo, mediante líneas. El usuario especifica «ya no habrá barcharts»: se descarta completamente la propuesta de barras.
2. Acepta recomendación Q8: filtros ocultan componentes; el detalle conserva total real y distingue importe de componentes visibles. Toggles Original/Modificado para saldo.
3. Acepta recomendación Q9: tramo navegable en teléfono, mismos meses en ambos gráficos; selección en cualquiera muestra detalle y permite editar eventos.

Frontera de cierre:

- Confirmar codificación de escenario/componente, capas iniciales y comportamiento de filtros.
- Confirmar propuesta completa documentada abajo, incluidos estados límite, copia y condiciones de sustitución.

## Decisiones acordadas

- Acción de copia en resumen, detalle del mes y valores de tabla. Importes pegados con dos decimales y coma decimal, sin moneda ni separadores de miles (ejemplo: 507,51). Selección libre no exigida.
- Composición de pagos de cada mes, no de acumulados.
- Dos gráficos de líneas uno debajo del otro, ambos con original/modificado: saldo de capital arriba y componentes pagados por mes abajo. No habrá barras. Las conexiones entre puntos mensuales no representan cálculo diario; los componentes inferiores son importes individuales, no acumulados ni apilados.
- El gráfico inferior representa todo lo efectivamente pagado: capital habitual, capital adicional de abonos/recuperación, intereses, seguros y otros cargos cuando existan. Conservar distribución real; un extra no equivale íntegramente a capital. El excedente no aplicado no integra el pago.
- Toggles ocultan componentes en vez de atenuarlos. El filtro no modifica ni aparenta modificar total realmente pagado; detalle distingue componentes visibles y total real. Saldo permite ocultar Original/Modificado.
- Teléfono con tramo navegable; ambos gráficos comparten meses visibles y mes seleccionado. Seleccionar un mes en cualquiera muestra su detalle y permite editar eventos del modificado.
- Retirada de doble pista sigue condicionada a conservar inspección/edición y representación financiera, con criterios de aceptación por acordar. No requiere ADR por ahora.

## Propuesta de cierre pendiente de confirmación

- Una línea inferior por componente y escenario: capital habitual, capital adicional, intereses, seguros y otros cargos. Mismo color por componente; original discontinuo y modificado continuo, con etiquetas además del color. Puntos mensuales y segmentos rectos; sin áreas apiladas.
- Activar ambos escenarios y todos los componentes que tengan algún pago distinto de cero en el horizonte. No mostrar controles de categorías inexistentes. Con préstamo simple: cuatro líneas inferiores (capital/interés de cada escenario). Un componente no desaparece solo porque sea cero en el tramo visible.
- Toggles de escenario compartidos por ambos gráficos y toggles de componente solo abajo. Al ocultar una línea, las restantes mantienen sus importes. Escala monetaria inferior ajustada a series visibles, misma escala para ambos escenarios; ejes siempre etiquetados. Filtrar no recalcula el préstamo. Si no quedan series, estado Sin series visibles y acción Restablecer; nunca fingir pagos/saldo cero.
- Navegación por tramos de hasta 12 meses en teléfono, botones anterior/siguiente y selección de mes accesible, sincronizados. Inicio del saldo incluye el capital antes del primer pago como mes 0, identificado; mes 0 sin pago ni edición de eventos. Horizonte conserva meses de seguros posteriores a liquidación. Sin un control de zoom adicional en esta entrega.
- Detalle común del mes con original/modificado, distribución completa, total real y total de componentes visibles cuando se filtre. Identificar intereses/seguros pendientes por separado del dinero pagado. Usar Diferencia de saldo para diferencias entre saldos, sin llamarlas Capital Ahorrado/Deuda evitada.
- Omitir cuota puede generar cargos con pagos cero. Recuperación y extras mantienen asignación real a cada categoría; comisión/IVA se muestran como otros cargos, sin omitirlos ni duplicarlos. Resultado incompleto identificado; no extender artificialmente una línea a cero ni prometer ahorro definitivo.
- Mantener editor y tabla como alternativa accesible; gráficos y detalle reflejan último escenario aplicado mientras existan borradores de eventos. Ocultar una serie no borra selección, eventos ni resultados. Visibilidad es estado de la sesión y no cambia el documento guardado.
- Copiar con acción accesible por valor (también teclado), feedback breve de éxito y error explícito si falla, sin alterar el dato. Importes: coma decimal y exactamente dos decimales, sin signo monetario ni miles; otros valores se copian como texto mostrado. Sin introducir edición al tocar un resultado.
- Retirar doble pista y selector de tipo de gráfico una vez verificados: selección/edición mensual desde ambos gráficos y tabla, filtros no destructivos, copia real Android/Web, horizonte largo/móvil/teclado, y casos de cuota ordinaria, abono, omisión/recuperación, seguros posteriores y resultado incompleto. Conservar pendientes de aceptación generales de FS-17/KMP; este cambio no los cierra.

## Límites

Diseño de presentación; no cambiar fórmulas ni contratos financieros para dibujar el gráfico. Sin implementación, integración ni publicación. La reserva FS-17 y sus pendientes de aceptación continúan vigentes.
