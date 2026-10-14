# Finsim: decisiones financieras para público general

Fecha de investigación: 2026-10-13. Responsable: /root, con investigación documental de /root/financial_decisions_research. Público general; Colombia y Argentina como alcance inicial confirmado por el usuario al cerrar la entrevista de diseño. Esta orientación sustituye la primera preferencia de no fijar país.

Estado: investigación documental entregada y alcance de producto confirmado por el usuario el 2026-10-13. La validación de uso y de los modelos sigue pendiente. Este documento conserva propuestas iniciales como antecedentes y registra las decisiones de diseño. No es una especificación de cálculo ni una reserva de implementación FS. No modifica el brief, las convenciones, el tablero ni el código.

## Síntesis de diseño: confirmada

La [especificación derivada](../../.scratch/financing-offer-comparison/spec.md) se publicó mediante to-spec el 2026-10-13, con etiqueta ready-for-agent en el tracker Markdown local. La cobertura de pruebas desde el espacio de trabajo compartido más móvil/teclado fue confirmada por el usuario; no se reabrió la entrevista.

Esta sección consolida las respuestas de la entrevista, confirmadas expresamente por el usuario en Q22 el 2026-10-13, y tiene precedencia sobre las prioridades iniciales propuestas en la investigación. Es alcance de producto, no especificación de cálculo ni autorización para implementar.

**Propuesta de valor:** ver y comparar qué pasa con la plata propia al explorar escenarios de financiación, comprendiendo sus diferencias sin que Finsim prescriba una acción.

| Eje | Alcance acordado en las rondas |
| --- | --- |
| Usuario y países | Persona no especialista, ingresos relativamente estables, deudas y pocos ahorros; Colombia y Argentina con un conjunto pequeño y verificado de productos. |
| Primera decisión | Comparar dos ofertas de financiación nueva: préstamos personales de tasa fija y pagos mensuales, o una compra aislada en cuotas con tarjeta existente o nueva. |
| Equivalencia | Mismo efectivo neto para préstamos en efectivo; misma compra para adquisición, admitiendo descuentos, capital diferente y distintos aportes propios iniciales. |
| Datos | Cotizaciones con pagos/cargos conocidos o simulación desde tasa/plazo. Estimaciones visibles; datos desconocidos separados de cero. Incorporación progresiva y borradores locales que se pueden retomar. |
| Vista principal | Pagos mensuales alineados, aporte inicial identificado y resumen de cuotas, plazo, total pagado y costos que puedan determinarse. Moneda común e importes nominales. |
| Contexto personal | Opcional: ingresos, gastos y cuotas vigentes para mostrar margen mensual. Sin inferir ahorro acumulado, solvencia entre fechas ni aprobación crediticia a partir de ese margen. |
| Exploración | Cambiar monto, plazo, tasa y cargos; identificar variaciones hipotéticas frente a cotizaciones. No presentar una condición modificada como una oferta confirmada. |
| Costos de tarjetas | Distinguir cargos ya existentes de los nuevos que origina la operación, evitando doble conteo y atribución incorrecta del costo. |
| Continuidad | Mantener datos locales, exportación/importación y revisión de ediciones. La herramienta de préstamos existente conserva su alcance; estas restricciones delimitan el comparador nuevo. |

### Fuera de esta primera ampliación

Saldo rotativo y nuevas compras dentro de una misma tarjeta; refinanciación/compra de cartera; abonos extraordinarios dentro del comparador; créditos indexados UVR/UVA; resultados ajustados por inflación; trayectoria de ahorros; inversiones y planificación conjunta del hogar. Son candidatas futuras, no funcionalidades eliminadas del producto existente.

### Criterios de aceptación derivados para revisar en la especificación

1. Comparar dos ofertas con igual efectivo neto y distintos plazos/cargos, mostrando sus pagos mensuales y diferencias sin confundir menor cuota con menor costo nominal.
2. Comparar préstamo y compra en cuotas para un mismo bien con descuento y aportes iniciales distintos; contar cada salida de dinero una sola vez. Distinguir total pagado de intereses/cargos de financiación.
3. Una cotización sin desglose no muestra intereses/capital inventados. Los cargos desconocidos impiden presentar el costo como completo; una estimación continúa identificada al calcular, guardar y recargar.
4. Completar información progresivamente, guardar un borrador incompleto y retomarlo sin perder entradas. La importación inválida conserva los datos anteriores y se comprueba la compatibilidad de archivos existentes.
5. Comparar sin contexto personal; al aportarlo, mostrar margen mensual coherente y distinguir cargos preexistentes de nuevos sin contarlos dos veces. No convertir un saldo inicial desconocido en dinero disponible.
6. En móvil y con teclado, una persona de la audiencia puede explicar diferencias de cuota, duración, aporte inicial y total, reconocer qué datos son estimados o faltantes y modificar un supuesto sin ayuda. Este criterio requiere observación real; no se ha probado en esta sesión.

### Verificación pendiente antes de una implementación

Precisar contratos soportados y reglas por país con fuentes y fechas; corregir las discrepancias argentinas registradas; definir cargos descontados/financiados y recurrentes, redondeos, fechas, campos mínimos por modo y tratamiento del CFT cotizado o calculado. Verificar cada modelo frente a casos independientes y contratos reales antes de afirmar fidelidad a una entidad. El motor actual no proporciona por sí solo todas esas capacidades.

## Recomendación

**Prioridad acordada durante grill-with-docs:** empezar por comparar ofertas de crédito con datos personales y estimaciones visibles. El orden de etapas sugerido en la investigación inicial se conserva como antecedente; no obliga a construir primero el presupuesto doméstico. El alcance concreto quedó confirmado en la síntesis de diseño.

Finsim debería ayudar a responder: **«Si tomo esta decisión, ¿me alcanza, qué gano, a qué renuncio y qué pasa si las cosas salen peor?»**

La principal ampliación es conectar préstamos, dinero disponible y metas del hogar. Calcular intereses ahorrados no permite determinar por sí solo si una decisión es viable. La app debe comparar costo, liquidez, deuda y avance hacia metas, con supuestos visibles. Esta es una propuesta de producto derivada de las fuentes, no una demanda de usuarios ya demostrada.

Para un público general de Colombia y Argentina conviene empezar por decisiones comunes y cantidades introducidas por la persona, incorporando las reglas locales que cambian el resultado. Los impuestos, reglas de prepago, productos y beneficios dependen de país, fecha y contrato; no se debe asumir una jurisdicción por la moneda o el idioma. La ampliación normativa se documenta más abajo.

## Punto de partida comprobado

Se revisaron el [brief](../product-brief.md), el [plan](../implementation-plan.md), el [tracking](../tracking.md), las [convenciones](../financial-conventions.md) y el [fundamento del stack, incluido su addendum KMP](stack-maintainability.md). El brief conserva partes históricas anteriores a la migración; para orientar futuras implementaciones se consultó también el código Kotlin actual.

| Capacidad | Evidencia del proyecto | Implicación para esta ampliación |
| --- | --- | --- |
| Un préstamo, tasa fija, seguros y eventos mensuales | `kmp/shared/src/commonMain/kotlin/com/finsim/domain/Scenario.kt`, `Scenario`, `LoanEvent` y `LoanResult` | Reutilizar el cálculo de deuda; faltan ingresos, gastos domésticos, activos y metas. |
| Original y modificado del mismo préstamo | `kmp/shared/src/commonMain/kotlin/com/finsim/workspace/WorkspaceStore.kt`, `WorkspaceState` y cálculo de ambos resultados | La comparación actual no representa distintas decisiones domésticas ni una bolsa común para varias deudas. |
| Préstamo, conversor y conciliación | `kmp/composeApp/src/commonMain/kotlin/com/finsim/app/App.kt`, lista de pestañas | Añadir entradas por pregunta cotidiana y mostrar detalles financieros progresivamente. |
| Guardado local, exportación/importación; ingesta documental en desarrollo/revisión | Tracking, `WorkspaceStore.kt` y `ui/ingestion/` | Conservar entrada manual y revisión de datos; la extracción facilita completar entradas, pero no determina si una decisión conviene. |
| CDT/ETF previsto y diferido | FS-12 y brief | Generalizar la pregunta a ahorro líquido, depósito a plazo e inversión. Comparar prepago con inversión amplía expresamente el alcance histórico. |
| Abonos para reducir plazo y mora ilustrativa | Convenciones y brief | Reducir cuota, tasas variables, crédito rotativo y atrasos iniciales requieren nuevas convenciones; no se obtienen cambiando etiquetas. |

La existencia de código no demuestra que esté integrado, aceptado por usuarios o que reproduzca contratos reales. Esta investigación no repitió esas verificaciones.

## Evidencia externa y límites

1. **El bienestar financiero tiene varias dimensiones.** CFPB distingue control cotidiano, absorción de shocks, avance hacia metas y libertad de elección. Es un marco útil para seleccionar resultados de Finsim; no valida un puntaje propio ni preferencias universales. [CFPB: financial well-being findings](https://www.consumerfinance.gov/consumer-tools/educator-tools/financial-well-being-resources/explore-findings/).

2. **Las fechas importan además de los totales.** La guía de emergencias de CFPB explica que un desfase entre entradas y salidas puede causar falta de efectivo. También señala que la reserva necesaria depende de la situación. Propuesta: calendario de caja y shocks editables; evitar imponer un número universal de meses de ahorro. [CFPB: emergency fund](https://www.consumerfinance.gov/an-essential-guide-to-building-an-emergency-fund/).

3. **Varias deudas introducen objetivos distintos.** El plan de deuda de CFPB compara atender primero el menor saldo o la mayor tasa manteniendo pagos mínimos. Finsim puede mostrar costo y liberación de cuotas con el mismo presupuesto. La herramienta no cubre todos los contratos ni políticas de mora. [CFPB: debt action plan](https://files.consumerfinance.gov/f/documents/cfpb_your-money-your-goals_debt-action-plan_tool_2018-11.pdf).

4. **Una cuota menor puede elevar el costo total.** CFPB advierte que consolidar deudas puede alargar el plazo y sumar cargos; tampoco corrige un déficit recurrente del hogar. Esto justifica comparar flujo de caja y costo por separado. Sus reglas legales estadounidenses no se trasladan a otros países. [CFPB: debt consolidation](https://www.consumerfinance.gov/ask-cfpb/what-do-i-need-to-know-if-im-thinking-about-consolidating-my-credit-card-debt-en-1861/).

5. **Ahorro, deuda e inversión compiten por recursos.** Investor.gov relaciona presupuesto, deuda costosa, emergencias e inversión a largo plazo. Sus orientaciones permiten formular comparaciones, pero no garantizan rendimientos ni justifican recomendar automáticamente invertir o prepagar. No se adoptan sus cuentas tributarias ni porcentajes como reglas del producto. [SEC: saving and investing](https://www.investor.gov/build-wealth-over-time-through-saving-and-investing).

6. **Una compra grande genera costos posteriores.** CFPB enumera entrada, impuestos, seguros, mantenimiento y reparaciones de vivienda, además del horizonte de permanencia. Son categorías para investigar y completar localmente, no una fórmula universal de comprar frente a arrendar. [CFPB: buying a home](https://www.consumerfinance.gov/ask-cfpb/what-are-some-of-the-financial-considerations-of-buying-a-home-en-119/).

7. **La comprensión financiera no puede darse por supuesta.** La encuesta OECD/INFE 2023 cubre 39 países y economías y encuentra relación entre alfabetización, bienestar y resiliencia. Orienta a explicar interés, inflación y riesgos dentro de la decisión. Una asociación no prueba que una función de Finsim produzca una mejora. [OECD/INFE: encuesta 2023](https://www.oecd.org/en/publications/2023/12/oecd-infe-2023-international-survey-of-adult-financial-literacy_8ce94e2c.html).

La evidencia anterior identifica problemas financieros reconocidos. No determina su frecuencia dentro de la audiencia futura de Finsim, su disposición a usar una app ni el orden definitivo del roadmap.

### Referencias de producto

| Producto y fuente oficial | Capacidad documentada | Aprendizaje propuesto |
| --- | --- | --- |
| [YNAB](https://www.ynab.com/features) | Metas, planificación de préstamos y reportes de gasto/patrimonio | Conectar abonos con el presupuesto que los hace posibles. |
| [Monarch](https://www.monarch.com/) | Presupuesto, flujo de caja, metas y cargos recurrentes | Reutilizar ingresos y obligaciones para distintas preguntas. |
| [ProjectionLab](https://projectionlab.com/) | Proyecciones, escenarios de vida y análisis de caja; no exige conectar cuentas | Se puede explorar el futuro con entradas manuales. |

Se revisó documentación comercial oficial, no se probaron las apps. No se ha demostrado una ventaja competitiva de Finsim, ni adecuación local, accesibilidad o precisión de esos productos. La hipótesis de diferenciación es resolver decisiones concretas en español, con pocos datos y explicaciones verificables.

## Funciones que faltan, en orden propuesto

P0 significa fundamento para las primeras decisiones; P1, siguiente ampliación; P2, decisiones más complejas. Son prioridades de investigación, no tareas reservadas.

| Prioridad | Pregunta de la persona | Función mínima | Resultado útil |
| --- | --- | --- | --- |
| P0 | ¿Me alcanza si hago esto? | Foto simple del hogar: efectivo/ahorros disponibles, ingresos netos y fechas, gastos esenciales y flexibles, cuotas y gastos periódicos | Saldo mínimo, primer faltante y margen después de la decisión. |
| P0 | ¿Cuánto aguanto si pasa algo? | Reserva elegida y escenarios de pérdida/retraso de ingreso o gasto inesperado | Cuándo se agota el dinero accesible, faltante y recuperación de la reserva. |
| P0 | ¿Cuánto puedo separar para una meta? | Monto, fecha, ahorro inicial y aportes; varias metas con prioridades explícitas | Aporte necesario o fecha alcanzable y efecto sobre otras obligaciones. |
| P1 | ¿A cuál deuda abono? | Varias deudas con mínimos y una única bolsa de dinero extra | Intereses, cuotas liberadas, fecha de salida y reserva restante. |
| P1 | ¿Me sirve esta oferta? | Mantener crédito frente a refinanciar/comprar cartera; costos de cambio y plazo; reducir cuota frente a reducir plazo cuando aplique | Alivio mensual, costo adicional o ahorro, deuda pendiente al horizonte común. |
| P1 | ¿Compro ahora, ahorro o pago a cuotas? | Precio de contado, descuento, cuota inicial, cargos, financiación y espera | Costo integral y liquidez restante; fecha en que la compra resulta viable bajo los supuestos. |
| P2 | ¿Ahorro, prepago o invierto? | Reparto de un excedente entre reserva, deuda, metas, depósito e inversión | Patrimonio y dinero accesible, escenarios de pérdida, comisiones e inflación. |
| P2 | ¿Arriendo o compro? ¿Vehículo o transporte? | Plantillas de costo total, financiación, mantenimiento, horizonte y valor de salida | Costos, patrimonio final, caja mínima y sensibilidad a supuestos. |
| P2 | ¿Qué cambia si cambia mi vida? | Cambio de empleo, independencia, mudanza, dependientes y jubilación | Ingresos netos y gastos nuevos, transición, metas desplazadas y autonomía financiera. |

Seguros de protección merecen una investigación posterior: prima, cobertura, deducible, exclusiones y demora de pago. El seguro añadido al préstamo no basta para comparar la protección de un hogar. Jubilación requiere también prestaciones esperadas y reglas locales; no debe prometer una pensión a partir de una tasa supuesta.

## Escenarios nuevos que podrían cambiar una decisión

Las cifras de los shocks las elige la persona. Estos escenarios no tienen probabilidades asignadas ni representan predicciones.

| Situación | Alternativas o cambio que explorar | Qué debe revelar la app |
| --- | --- | --- |
| El mes cierra positivo, pero una factura vence antes del salario | Cambiar fecha, esperar una compra o mantener un colchón | Faltante entre fechas aunque el total mensual alcance. |
| Ingreso variable, estacional o pago de cliente retrasado | Ingreso habitual, mes bajo, cobro tardío | Diferencia entre ganar dinero y tenerlo disponible. |
| Pérdida temporal de un ingreso del hogar | Mantener gastos, reducir los ajustables o posponer una meta | Duración de la reserva y obligaciones sin cubrir. |
| Pérdida de ingreso junto con una emergencia | Shock combinado | Fragilidad que no aparece al probar cada problema por separado. |
| Prima, bono, regalo o ingreso extraordinario | Reservar, abonar, gastar, invertir o repartir | Consecuencias de cada uso sin duplicar el mismo dinero. |
| Varias deudas simultáneas | Mayor tasa, menor saldo o prioridad elegida | Diferencia entre ahorro de intereses y liberación de cuotas; si ni siquiera alcanzan los mínimos, mostrar déficit. |
| Refinanciación con menor cuota | Mantener crédito, cambiarlo o mantener el pago anterior al cambiar | Plazo, cargos, costo total y caja doméstica. |
| Abono que deja poca reserva | Abonar todo, parte o esperar | Ahorro de deuda frente a dinero accesible para imprevistos. |
| Compra con financiación promocional | Contado, cuotas, ahorro previo y promoción vencida | Cargos, tasa posterior, pago final elevado si existe y descuento perdido. |
| Gastos anuales o semestrales previsibles | Provisionarlos o pagarlos cuando llegan | Meses de presión sobre la caja; no confundirlos con emergencias. |
| Inversión que se necesita antes del horizonte | Esperar vencimiento, retirar o vender antes | Disponibilidad contractual, costos y pérdidas posibles; un valor de cartera no es efectivo disponible. |
| Inflación, tipo de cambio o mercado adverso | Trayectorias editables, incluida caída temprana con retiro posterior | Poder adquisitivo, volatilidad y efecto del orden de rendimientos. |
| Vivienda o vehículo | Comprar, arrendar/usar alternativas o esperar | Costos de mantenerlo, entrada inmovilizada, deuda restante y valor de venta supuesto. |
| Cambio de empleo o mudanza | Oferta actual y nueva; transición sin ingreso | Salario neto, beneficios, transporte, cuidado y gastos de traslado. |
| Nuevo dependiente o apoyo familiar | Más gastos y posibles cambios de ingresos | Efecto conjunto sobre reserva y metas compartidas. |
| Atrasos ya existentes o crédito rotativo | Regularizar, seguir pagando mínimos o dejar de comprar | Necesidad de modelos contractuales nuevos; el préstamo mensual al día no representa estos casos. |

Para público general, efectivo, ingresos de varias personas y registros aproximados deben ser posibles sin conexión bancaria. Como referencia contextual, la encuesta colombiana de demanda de 2022 reportó uso importante del efectivo; no demuestra una preferencia global ni actual. Su ficha describe 5.513 adultos, entrevistas presenciales en abril-mayo de 2022 y margen nacional declarado de 1,8% al 95% de confianza (PDF p.3; efectivo p.17). [Banca de las Oportunidades: presentación 2022](https://www.bancadelasoportunidades.gov.co/sites/default/files/2022-10/Presentaci%C3%B3n%20de%20la%20Encuesta%20de%20Demanda%202022.pdf).

## Cómo debería explicar una decisión

La entrada principal puede ofrecer preguntas como «Quiero comprar algo», «Tengo dinero extra», «Quiero salir de deudas», «Quiero alcanzar una meta» y «Quiero prepararme para un imprevisto». Cada flujo pide solo los datos necesarios y permite ampliar el detalle.

Cada comparación debería mostrar:

- **Viabilidad:** saldo mínimo proyectado, primer déficit y compromisos sin cubrir. El saldo mínimo se calcula con las fechas conocidas; si solo hay meses, se declara ese límite.
- **Costo y deuda:** cargos, pagos y deuda restante; distinguir costo financiero de devolución del capital.
- **Liquidez y protección:** dinero accesible, reserva asignada y necesidades que quedarían descubiertas bajo el shock elegido.
- **Metas y patrimonio:** fecha de la meta y activos menos pasivos al mismo horizonte; identificar valores de venta y rendimientos supuestos.
- **Condiciones del resultado:** qué datos son confirmados, estimados o faltantes; qué supuesto cambia la conclusión.

Ejemplo de explicación, con cifras ilustrativas en la moneda elegida: «El abono reduce tu deuda, pero tus ahorros disponibles pasan de 5.000 a 1.000. Un imprevisto de 2.000 dejaría un faltante de 1.000». El ahorro exacto de intereses debe venir del motor con las condiciones del crédito, no inventarse en el texto.

La comparación usa los mismos recursos iniciales, ingresos externos y horizonte. La alternativa decide dónde poner ese dinero. Si libera una cuota, debe indicar si se guarda, se gasta o se reinvierte; no asumir un destino silencioso. El crédito nuevo aumenta efectivo y deuda, no riqueza neta por sí solo. Una transferencia entre cuentas o metas propias tampoco es un gasto de consumo ni dinero nuevo.

No convertir objetivos distintos en un ganador automático: una opción puede mejorar liquidez y empeorar costo. La persona elige qué quiere proteger o mejorar. Evitar un puntaje universal de salud financiera y umbrales rígidos de capacidad de pago.

## Primera ampliación recomendable

**Etapa A: comprobar viabilidad.** Foto del hogar, calendario de caja, gastos periódicos, reserva y una meta. Integrar el préstamo existente para contestar «¿cuánto extra puedo pagar sin romper el plan que elegí?». Probar un shock de ingreso y uno de gasto. Esto aporta utilidad también a personas sin deudas.

**Etapa B: decidir entre obligaciones.** Varias deudas compatibles con el motor, una bolsa común de abonos y comparación de una oferta de refinanciación. Añadir compras al contado frente a financiación soportada. Tarjetas rotativas, tasas variables y mora contractual se habilitan únicamente con convenciones y casos de referencia propios.

**Etapa C: distribuir excedentes.** Ahorro líquido, depósitos a plazo e inversión; después, comparar con prepago. Mantener rendimientos prescritos y escenarios adversos. Incluir costos, retiros, inflación y disponibilidad del dinero. FS-12 es un antecedente, pero esta etapa necesita alcance nuevo.

**Etapa D: decisiones grandes.** Vivienda, movilidad y transiciones familiares/laborales; seguros y jubilación después de investigar los supuestos y reglas necesarios. Las plantillas reutilizan la foto del hogar y la comparación, sin crear un framework genérico por anticipado.

La primera entrega no requiere sincronización bancaria, cotizaciones en vivo, recomendación automática de productos, chatbot, ejecución de pagos ni Monte Carlo. Incorporarlos solo si la investigación de uso demuestra que resuelven una dificultad concreta.

## Implicaciones técnicas para una especificación futura

- Mantener `kmp/shared` como motor canónico según el addendum del stack; no extender el motor TypeScript congelado.
- `Scenario` representa hoy un préstamo. La foto del hogar y las alternativas necesitan un contrato propio que use esos resultados, con persistencia versionada y conservación de los archivos anteriores.
- Los importes `Money` actuales no admiten negativos y `RateValue` no admite tasas negativas (`domain/types/DomainTypes.kt`). Una proyección doméstica debe poder mostrar déficits y una inversión debe poder perder valor. No truncar pérdidas a cero ni debilitar los invariantes del préstamo para reutilizar esos tipos.
- El calendario de entradas/salidas es distinto del devengo de intereses. Introducir fechas de caja no convierte el préstamo mensual en un modelo diario; conservar y explicar ese límite.
- Una moneda de comparación por decisión al principio. Si hay activos en otra divisa, exigir un supuesto de conversión y sus costos; no sumar monedas distintas. País, moneda y régimen contractual son datos diferentes.
- Reutilizar edición por lotes, validación, importación revisada y exportación. Los datos desconocidos no deben entrar silenciosamente como cero. Separar escenarios futuros de movimientos ya realizados.

Al implementar habrá que seguir la reserva del tracking, definir archivos/contratos afectados y probar conservación de caja, transferencias sin doble conteo, déficits, horizonte común y supuestos adversos. Este documento no inicia esa implementación.

## Validación de producto pendiente

Propuesta de investigación: entrevistas y pruebas en Colombia y Argentina con personas de ingresos fijos, variables, con/sin deudas y con/sin dependientes. Una muestra exploratoria de 8–12 participantes puede descubrir problemas de comprensión; no sería representativa del público general de ninguno de los dos países.

Pedir una decisión reciente y reconstruir qué alternativas consideró, qué datos tenía, qué le faltó y qué consecuencia quería evitar. Evitar empezar por «¿usarías esta función?». Después observar si puede:

1. Completar una comparación con datos que tiene a mano, identificando estimaciones.
2. Explicar qué mejora, qué empeora y qué dinero queda disponible.
3. Entender por qué una cuota más baja puede costar más.
4. Reconocer que rendimiento supuesto y dinero accesible son cosas distintas.
5. Cambiar un supuesto adverso y explicar cómo afecta su decisión sin ayuda.

Registrar dificultad, abandono, tiempo hasta una respuesta útil y comprensión observada. Priorizar con esa evidencia antes de fijar el roadmap completo. La comprensión sin ayuda continúa el criterio de aceptación del brief; no se ha observado en esta sesión.

## Ampliación: Colombia y Argentina

El país debe afectar las opciones y los cálculos. Seleccionar COP o ARS solo cambia la moneda; no determina la normativa aplicable. Para estas comparaciones se necesita identificar el producto, el acreedor, las condiciones contractuales y las fechas pertinentes. Residencia fiscal y país del producto pueden ser diferentes.

### Diferencias verificadas que afectan decisiones

| Tema | Evidencia oficial consultada el 2026-10-13 | Consecuencia propuesta para Finsim |
| --- | --- | --- |
| Prepago colombiano, régimen general | El artículo 1 de la Ley 1555 permite prepago total/parcial en moneda nacional sin penalización dentro de su ámbito; excluye saldos superiores a 880 SMMLV, contempla varios créditos por entidad y permite elegir reducción de cuota o plazo. Excluye créditos hipotecarios de ese artículo. [Ley 1555](https://normograma.dian.gov.co/dian/compilacion/docs/ley_1555_2012.htm). | Determinar si corresponde ese régimen antes de asumir comisión cero. Recoger saldo, otros créditos con la entidad, tipo de acreedor y contrato cuando proceda. Comparar cuota frente a plazo. |
| Vivienda colombiana | El artículo 17.8 de la Ley 546 prevé prepagos sin penalidad y elección cuota/plazo para vivienda individual a largo plazo. Es un régimen distinto al límite anterior. El artículo 24 trata la cesión de crédito hipotecario de vivienda. [Ley 546](https://normograma.supersalud.gov.co/compilacion/docs/ley_0546_1999.htm). | Distinguir vivienda, otras finalidades con garantía hipotecaria y leasing; distinguir cesión de cartera de cancelación y nueva financiación. No asignar los mismos costos a todas. |
| Precancelación argentina | El punto 2.3.2.1 del texto BCRA permite que total/parcial dé lugar a comisiones. Para precancelación total no admite comisión después del mayor entre un cuarto del plazo original y 180 días corridos desde el otorgamiento. [BCRA: protección de usuarios](https://www.bcra.gob.ar/Pdfs/Texord/t-pusf.pdf), texto recuperado al 06/05/2026, A8433. | Comparar cancelar hoy, cancelar parcialmente o esperar. Usar fecha original y fecha de pago; verificar la comisión contractual. La exención total no se extiende automáticamente a los abonos parciales. |
| Costo de crédito argentino | BCRA distingue TNA y CFT; este último incorpora intereses, comisiones, seguros, IVA y otros cargos. [BCRA: préstamos personales](https://www.bcra.gob.ar/prestamos-personales-comparar-costos-y-condiciones/). | Comparar desembolso neto, flujo completo y CFT correctamente definido. No convertir una TNA a TEA y presentarla como costo integral. Los impuestos aplicables se verifican, no se presuponen para todos los créditos. |
| Indexación de vivienda | Banrep describe UVR basada en IPC. BCRA regula UVA actualizada por CER y advierte que los ingresos pueden no seguir su evolución. [Banrep: precios e inflación](https://www.banrep.gov.co/es/estadisticas-economicas/series-historicas/precios-inflacion), [BCRA: política de crédito, sección 6.1](https://www.bcra.gob.ar/Pdfs/Texord/t-polcre.pdf), texto al 11/09/2026, A8479. | Comparar pesos frente a unidad indexada con trayectorias separadas de índice e ingresos. Mostrar saldo y cuota en unidades y pesos. Una tasa fija sobre unidades indexadas no implica cuota fija en pesos; UVR y UVA necesitan convenciones propias. |
| Ahorro y acceso al dinero | BCRA diferencia depósitos e inversiones con opción de cancelación anticipada y exige condiciones contractuales para ejercerla. [BCRA: depósitos, secciones 1.13 y 2.3](https://www.bcra.gob.ar/Pdfs/Texord/t-depinv.pdf), texto al 11/09/2026, A8479. | Distinguir vencimiento, disponibilidad anticipada y rendimiento al salir antes. Un depósito a plazo no se cuenta automáticamente como reserva inmediata. |
| Protección de depósitos | Fogafín agrega cobertura por depositante y entidad para productos cubiertos. SEDESA también describe alcance y exclusiones de su sistema. [Fogafín](https://www.fogafin.gov.co/preguntas-frecuentes), [SEDESA](https://www.sedesa.com.ar/sistema-de-garantia-de-los-depositos/). | Comparar concentración por entidad y cobertura identificada, separadas de liquidez y riesgo de inversión. Guardar topes con fecha; no equiparar un depósito con cualquier producto vendido por una entidad o billetera. |

La tabla es una revisión documental parcial. No certifica que una operación concreta esté cubierta por una norma ni sustituye la clasificación del contrato. No se adoptan porcentajes de comisión, IVA o límites monetarios como constantes universales.

### Escenarios que ganan prioridad

1. **Abonar en Colombia reduciendo cuota o plazo:** mismo dinero inicial, impacto en liquidez doméstica e intereses; régimen aplicable explícito.
2. **Precancelar en Argentina hoy o esperar:** ahorro de intereses frente a comisión contractual, distinción total/parcial y fechas efectivas.
3. **Crédito en pesos frente a UVR/UVA:** inflación mayor a la prevista mientras el salario crece menos; efecto sobre cuota/ingreso y saldo en pesos.
4. **Depósito tradicional frente a indexado o ahorro líquido:** rendimiento neto y real, vencimiento y necesidad de retirar antes. CDT y plazo fijo son nombres locales; no implican contratos idénticos.
5. **Refinanciar o trasladar cartera:** sumar costos realmente aplicables de la operación y mostrar el efecto de financiarlos, cuando sea posible, frente a pagarlos al inicio.
6. **Mover ahorros o concentrarlos en una entidad:** costos de movimientos, cobertura identificada y disponibilidad del dinero.

Inflación y rendimientos futuros son supuestos económicos; condiciones del banco son datos contractuales; los límites y derechos son reglas normativas. La app debe mostrar cuál de esas tres cosas cambia cada resultado.

### Impuestos: incorporar sin automatizar conclusiones incompletas

La revisión tributaria es inicial. En Colombia el Concepto DIAN 6727/2026 describe la exención GMF consolidada y una transición operativa: a abril de 2026 continuaba la marcación en la práctica por falta de implementación del sistema. Ese documento no demuestra el estado operativo de octubre. [DIAN: Concepto 6727](https://normograma.dian.gov.co/dian/compilacion/docs/oficio_dian_6727_2026.htm).

En Argentina, ARCA describe exenciones de Bienes Personales para determinados depósitos y requisitos para otros instrumentos. Una exención en ese impuesto no determina por sí sola el tratamiento en Ganancias. [ARCA: bienes exentos](https://arca.gob.ar/gananciasYBienes/bienes-personales/conceptos-basicos/bienes-exentos.asp).

Propuesta: registrar residencia fiscal, periodo, instrumento y cargos/retenciones relevantes cuando se compare rendimiento neto. Si falta verificar el tratamiento, identificar el resultado como previo a esos impuestos o pendiente; no asumir cero. El cálculo tributario integral y las restricciones cambiarias vigentes quedan pendientes de investigación específica.

### Brecha observada en el perfil argentino existente

Lectura de `engine/AmortizationAccounting.kt`, `ArgentinaPrepaymentAdapter`, y su llamada en `engine/Loan.kt`:

- El umbral actual usa solo un cuarto del plazo en meses. No incorpora el mínimo de 180 días ni recibe la fecha original del crédito, aunque `ArgentinaProfile` contiene un campo de origen.
- La interfaz evalúa capital extraordinario sin distinguir precancelación total de parcial; aplica el mismo umbral de exención.
- Los valores por defecto de comisión del 3% e IVA del 21% son supuestos del código; las fuentes consultadas no acreditan que correspondan universalmente a los créditos argentinos.

Es una discrepancia observada entre el modelo simplificado y la regla consultada, no una auditoría exhaustiva ni una corrección implementada. Debe resolverse antes de presentar ese perfil como reproducción normativa. La regla estándar sin comisión tampoco acredita cobertura legal para cualquier crédito colombiano.

### Orden revisado de trabajo

Mantener la base doméstica propuesta en Etapa A, pero investigar y validar la fidelidad de los préstamos soportados antes de ampliar comparaciones basadas en ellos. Preparar después reducción cuota/plazo, refinanciación y ahorro local; UVR/UVA requieren un hito propio porque el motor actual es de tasa fija y capital nominal.

Cada regla implementada debería conservar país, ámbito, fecha de vigencia conocida, fuente, fecha de revisión y versión del cálculo. Cada escenario exportado debe identificar qué versión utilizó. Al actualizar una regla, mostrar su efecto antes de sustituir los resultados guardados. No hace falta construir un sistema genérico de regulación ni un servicio de datos en vivo para empezar.

## Entrevista de diseño: grill-with-docs

Estado: rondas 1–7 resueltas y síntesis confirmada en Q22, 2026-10-13. Entrevista cerrada; no autoriza implementación.

### Ronda 1: decisiones confirmadas por el usuario

- R1-Q1 = A: usuario inicial con ingresos relativamente estables, deudas y pocos ahorros; necesita decidir cuánto abonar, reservar o gastar.
- R1-Q2 = A: comparación guiada. La propuesta de valor es ver y poder comparar mediante visualizaciones con la plata propia, ayudando a entender diferentes escenarios. No se eligieron recomendación de distribución ni gestión continua del plan.
- R1-Q3 = A: Colombia y Argentina desde el alcance inicial, con un conjunto pequeño de decisiones y productos verificados y cobertura explícita. No implica soporte de todos los productos de ambos países.

Vocabulario resuelto: comparación guiada, registrado en el glosario de producto CONTEXT.md. GLOSSARY.md conserva el vocabulario existente de préstamos; escenario financiero, alternativa y foto del hogar siguen por precisar antes de cambiar definiciones.

### Ronda 2: decisiones confirmadas por el usuario

- R2-Q4 = B: la primera ampliación debe comparar ofertas de crédito, sus cuotas, plazos y costos. Esta elección sustituye la recomendación inicial de comenzar por excedente/prepago/reserva; esas comparaciones quedan como candidatas posteriores.
- R2-Q5 = A: situación financiera personal, sin bolsa conjunta del hogar en el alcance inicial.
- R2-Q6 = A: permitir estimaciones identificadas por la persona; mostrar qué resultados dependen de ellas. No convertir datos desconocidos en cero ni presentar estimaciones como condiciones confirmadas.

Vocabulario añadido: oferta de crédito y estimación, en CONTEXT.md. La definición vigente de escenario sigue correspondiendo a un préstamo; no se ha aprobado ampliarla a una economía doméstica.

### Ronda 3: decisiones confirmadas por el usuario

- R3-Q7 = A+C: incluir préstamos personales de tasa fija y pagos mensuales, además de tarjetas/compras financiadas. El alcance se precisó después en R4-Q10: compra aislada en cuotas, sin saldo rotativo.
- R3-Q8 = A: comparar el mismo dinero neto disponible, permitiendo distintas tasas y plazos. La equivalencia para compras se refinó después en R5-Q13; no se equipara una compra financiada con efectivo recibido.
- R3-Q9 = A: contexto personal opcional (ingresos, gastos habituales y cuotas vigentes), para mostrar margen restante cuando la persona lo aporte.

Vocabulario añadido: desembolso neto y contexto personal, en CONTEXT.md.

### Ronda 4: decisiones confirmadas por el usuario

- R4-Q10 = A: compras aisladas en cuotas con condiciones y plazo definidos. El saldo rotativo con nuevas compras y pagos mínimos queda fuera de la primera ampliación.
- R4-Q11 = A: vista principal de pagos mensuales alineados, acompañada de resumen de cuota, plazo y costo total.
- R4-Q12 = A: dos ofertas simultáneas.

Vocabulario añadido: compra en cuotas, en CONTEXT.md.

### Ronda 5: decisiones confirmadas por el usuario

- R5-Q13 = A: para compras, comparar la misma adquisición aunque descuentos y condiciones cambien el capital necesario; para créditos en efectivo, conservar el mismo desembolso neto. Refina R3-Q8 y resuelve la equivalencia compra/efectivo, sin confundir precio, capital y dinero recibido.
- R5-Q14 = A: admitir tanto cotizaciones con pagos/cargos conocidos como simulaciones desde tasa/plazo. Un desglose de interés/capital desconocido no se inventa; se muestran pagos y costos determinables y los datos faltantes.
- R5-Q15 = A: en el comparador inicial se exploran cambios de monto, plazo, tasa y cargos; los abonos extraordinarios no se añaden a este flujo nuevo. La herramienta de préstamos existente conserva su alcance.

Vocabulario añadido: necesidad de financiación, cotización y escenario de oferta, en CONTEXT.md. Escenario de oferta distingue el concepto de producto del Scenario de préstamo existente; no se cambia código ni se declara que ambos contratos sean equivalentes.

### Ronda 6: decisiones confirmadas por el usuario

- R6-Q16 = A: permitir diferentes aportes iniciales para una misma compra; mostrar aporte propio, pagos posteriores y total. Esto no acredita disponibilidad del aporte si falta información personal.
- R6-Q17 = A: incluir tarjetas existentes y nuevas, distinguiendo cargos preexistentes de los que nacen con la operación.
- R6-Q18 = A: contexto personal opcional orientado al margen mensual; la trayectoria de ahorro no forma parte de esta primera ampliación.
- Aclaración adicional del usuario: se debe poder ir añadiendo información según sea necesario. La entrada será progresiva, conservando los datos ya aportados y sin exigir completar contexto o detalle no necesario para la vista disponible. No implica por sí sola que todos los resultados sean calculables desde el primer dato ni que un faltante sea cero.

Vocabulario añadido: información progresiva y dato desconocido, en CONTEXT.md. El guardado de borradores incompletos se confirmó después en R7-Q19.

### Ronda 7: decisiones confirmadas por el usuario

- R7-Q19 = A: guardar borradores incompletos localmente, identificando qué falta y permitiendo retomarlos otro día. Un borrador no se presenta como resultado calculado.
- R7-Q20 = A: nueva financiación en la primera ampliación; refinanciación/compra de cartera queda para otra entrega.
- R7-Q21 = A: importes nominales en la moneda elegida, con fechas/plazos visibles. El ajuste por inflación queda fuera del comparador inicial.

Vocabulario añadido: borrador de comparación, margen mensual y costo nominal, en CONTEXT.md.

### Revisión de la frontera y cierre confirmado

Se han respondido las 21 decisiones de producto abiertas en las rondas 1–7. No queda otra elección de alcance identificada en este árbol; el usuario confirmó la síntesis y el entendimiento compartido en Q22. Los hechos normativos, convenciones numéricas e integración técnica aún requieren investigación/especificación y verificación: no se consideran resueltos por votos del usuario ni por esta síntesis. Si esa verificación exige cambiar el alcance acordado, se reabre la decisión afectada.

- Q22 = sí: confirmación expresa del usuario de que la síntesis refleja lo que quiere construir. Entrevista cerrada el 2026-10-13. No hay autorización de implementación derivada de esta entrevista.

Reglas de presentación derivadas de los acuerdos: datos estimados identificados; desconocido distinto de cero; pagos/costos conocidos visibles sin declarar un costo completo si faltan cargos; contexto personal solo muestra lo que permiten sus entradas; no se declara una oferta ganadora de manera automática. Los detalles y fuentes se completan progresivamente sin hacer pasar una hipótesis por cotización confirmada.

Restricciones heredadas que no se reabren: datos locales y exportación/importación, sin necesidad de cuenta; edición revisable conforme al patrón existente; resultados en una moneda de comparación; el país no se deduce de la moneda. Las condiciones de ofertas registradas y las variaciones hipotéticas deben identificarse para no presentar una exploración como promesa de una entidad. La primera entrega mantiene comparación guiada, sin prescribir una acción.

Lectura técnica completada en paralelo: Scenario.kt:102 y AmortizationAccounting.kt:114 representan una operación aislada con capital, tasa fija y cuota mensual conocida/calculada, siempre que sus convenciones coincidan con el contrato. No hay mínimo rotativo, nuevas compras que aumenten capital, tasas promocionales por etapas, cargos generales ni distinción entre principal y desembolso neto (Scenario.kt:54; Loan.kt:16; AmortizationAccounting.kt:101,175,191). Estos hechos no validan contratos reales de tarjetas y no restringen silenciosamente la elección del usuario; un alcance rotativo requeriría un modelo propio. Las discrepancias del perfil argentino siguen como revisión pendiente.

## Registro de continuidad

- Rama observada: `feat/kmp-migration`; checkout: `C:/Users/Lenovo/Documents/finsim`.
- Trabajo realizado: revisión documental y de contratos/navegación actuales, fuentes primarias y referencias oficiales de producto; ampliación normativa parcial Colombia/Argentina y brecha de precancelación argentina registrada sin editar código.
- Archivos propios sin commit: `docs/research/financial-decision-product-discovery.md`, `CONTEXT.md`, `.scratch/financing-offer-comparison/spec.md` y `docs/specs/FINANCING-OFFER-COMPARISON.md`. No se editaron tareas FS ni el tablero compartido.
- Comprobaciones: lectura de fuentes citadas y verificación de escritura del documento. Sin pruebas ni build: no cambió código. Sin prueba de apps competidoras, entrevistas, reconciliación bancaria ni validación jurídica/fiscal exhaustiva de contratos.
- Decisiones de producto: confirmadas en Q22; entrevista cerrada. Especificación funcional/financiera publicada en el tracker local mediante to-spec. Pendientes: reserva/descomposición de implementación cuando se solicite, contraste contractual y verificación de perfiles; no se presentan como validaciones ya realizadas.
- Siguiente paso: usar la especificación publicada para descomponer/reservar implementación cuando se solicite esa etapa. Cumplir el protocolo de coordinación y los gates de contraste del perfil antes de afirmar fidelidad contractual.
- Cierre de entrevista: 2026-10-13; Q22 confirmado por el usuario. Continuación to-spec: especificación publicada con etiqueta ready-for-agent y cobertura de pruebas confirmada. Se comprobaron 22 referencias aritméticas en centavos; revisión de estructura y enlaces locales del documento. No se ejecutaron pruebas del producto ni build, pues no cambió código. Git status no pudo ejecutarse por fallo del helper de procesos; los archivos propios se verificaron por lectura/escritura del filesystem. Los cuatro archivos propios siguen sin commit.


### Continuación to-tickets — 2026-10-14

- Solicitud activa: descomponer la especificación; no se inició la implementación solicitada en el turno anterior.
- Se preparó [una propuesta de diez entregas verticales](../../.scratch/financing-offer-comparison/breakdown-proposal.md), con bloqueos, criterios y trazabilidad de las 48 historias. No se publicaron archivos individuales ready-for-agent: pendiente de la revisión/aprobación del desglose que exige to-tickets.
- Verificación documental: diez propuestas, dependencias referidas a entregas anteriores, cobertura de las 48 historias y hash SHA-256 de la especificación padre sin cambios. La carpeta de la función contiene spec y propuesta; no se reservaron IDs FS ni se modificó el tablero.
- Siguiente paso: revisar tamaño, bloqueos y posibles uniones/divisiones con el usuario; después de aprobar, publicar un archivo por ticket en el tracker local. Antes de implementar, cumplir reserva de tarea, rama y worktree.
- Sin cambios de código, pruebas de la app, builds ni validación contractual nueva. Git status continúa sin comprobarse por fallo previo del helper de procesos; no se crearon commits.
