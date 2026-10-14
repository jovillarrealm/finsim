# Comparador visual de ofertas de financiación

Status: ready-for-agent
Triage Label: ready-for-agent
Fecha: 2026-10-13
Alcance: confirmado por el usuario; implementación no iniciada.
Tracker: Markdown local, conforme al patrón existente del proyecto.
Cobertura de pruebas: flujo completo del espacio de trabajo compartido, más móvil y teclado; confirmada por el usuario.
Esta es la fuente canónica de la especificación. La reserva y ejecución de tareas siguen el protocolo del proyecto.

## Problem Statement

Una persona con ingresos relativamente estables, deudas y pocos ahorros recibe ofertas que destacan cifras diferentes: tasa, cuota, número de pagos, descuento de contado o una promoción de compra. No puede ver fácilmente cuánto dinero necesita hoy, cuánto pagará cada mes y cuánto termina pagando. Una cuota menor puede extender la obligación; un precio financiado puede perder un descuento; un cargo puede estar descontado del desembolso, incluido en la cuota o cobrado aparte.

Finsim ya explora un préstamo y sus eventos, pero su comparación original/modificado no representa dos ofertas independientes. Tampoco distingue el capital contractual del efectivo neto recibido, ni una cotización incompleta de un resultado calculable. La persona necesita usar sus propios números, empezar con la información que tiene y añadir detalle sin perder su trabajo.

## Solution

Un comparador guiado para Colombia y Argentina que muestra dos ofertas de financiación nueva en una misma moneda. Admite préstamos personales de tasa fija y pagos mensuales, y una compra aislada en cuotas con tarjeta existente o nueva.

La persona elige una necesidad de financiación: recibir un mismo efectivo neto o realizar una misma compra. En compras se admiten precios de contado/financiados y aportes iniciales distintos. Introduce una cotización de pagos y cargos, o simula a partir de capital, tasa y plazo. Los gráficos mensuales comparten escala y calendario; el aporte inicial se identifica por separado. Los resúmenes distinguen pagos, duración, total desembolsado y costo de financiación.

El contexto personal es opcional y añade el margen mensual después de gastos y obligaciones vigentes. Se pueden modificar condiciones para explorar escenarios, identificando cada modificación como hipotética. La app explica diferencias; la acción la elige la persona.

La entrada es progresiva: permite datos conocidos, estimados y desconocidos, resultados parciales y borradores locales recuperables. Un faltante nunca significa cero ni una cotización transcrita acredita por sí sola fidelidad contractual.

## User Stories

1. Como persona no especialista, quiero comparar con mis propios importes, para comprender las consecuencias de las ofertas que recibí.
2. Como persona en Colombia o Argentina, quiero seleccionar explícitamente el país de la operación, para conocer qué ámbito tienen las reglas presentadas.
3. Como persona que compara, quiero seleccionar una moneda común, para no sumar ni contrastar importes de divisas diferentes.
4. Como persona que necesita efectivo, quiero fijar cuánto debe quedarme disponible del crédito, para comparar el mismo desembolso neto.
5. Como persona que quiere comprar un bien, quiero comparar la misma compra con distintos precios, para incorporar descuentos de contado y condiciones de financiación.
6. Como persona que compra, quiero introducir aportes iniciales diferentes, para ver cuánto dinero propio exige cada oferta hoy.
7. Como persona que recibe un préstamo, quiero distinguir capital contractual y efectivo recibido, para reconocer los descuentos iniciales.
8. Como persona que usa una tarjeta, quiero comparar una compra aislada en cuotas, para entender esa operación sin modelar toda la tarjeta.
9. Como persona que ya tiene una tarjeta, quiero separar cargos existentes y nuevos, para no atribuir a la compra costos que seguirían existiendo.
10. Como persona que contrataría una tarjeta nueva, quiero incluir sus cargos conocidos, para comprender el costo adicional de esa alternativa.
11. Como persona que compara, quiero identificar las ofertas A y B, para relacionar cada gráfico con la propuesta correspondiente.
12. Como persona con una cotización, quiero introducir cantidad e importe de cuotas sin conocer la tasa, para comparar los pagos que sí conozco.
13. Como persona con cuotas iniciales o finales distintas, quiero registrar esos importes, para representar el calendario cotizado.
14. Como persona que conoce tasa y plazo, quiero calcular una proyección mensual, para explorar condiciones alternativas.
15. Como persona que introduce una tasa, quiero identificar su convención y frecuencia cuando corresponda, para evitar confundir tasa nominal, efectiva y costo integral.
16. Como persona con financiación sin intereses, quiero introducir tasa cero expresamente, para distinguirla de una tasa desconocida.
17. Como persona que empieza, quiero completar solo la información necesaria para la vista disponible, para obtener utilidad sin llenar todo el formulario.
18. Como persona que estima un dato, quiero marcarlo como estimación, para recordar qué parte del resultado depende de una aproximación.
19. Como persona que desconoce un cargo, quiero conservarlo como desconocido, para no recibir un costo completo ficticio.
20. Como persona que completa información, quiero añadir detalles sin perder lo ingresado, para avanzar conforme consigo documentos o respuestas.
21. Como persona que compara, quiero ver los pagos mensuales alineados, para entender cuánto sale del bolsillo y durante cuánto tiempo.
22. Como persona que compara plazos distintos, quiero compartir ejes y horizonte, para que una escala distinta no oculte diferencias.
23. Como persona que debe pagar al inicio, quiero ver aporte y cargos iniciales, para no confundirlos con una cuota recurrente.
24. Como persona con una cotización sin desglose, quiero ver el pago conocido sin componentes inventados, para entender el límite de la información.
25. Como persona con desglose verificable, quiero identificar capital, interés, seguros y otros cargos, para comprender la composición del pago.
26. Como persona que compara, quiero distinguir total pagado y costo de financiación, para no llamar interés al capital que devuelvo.
27. Como persona que compara, quiero ver diferencias con signo y su significado, para reconocer aumentos y reducciones sin recibir un ganador automático.
28. Como persona que analiza una oferta, quiero consultar el detalle mensual en una tabla, para acceder a la misma información sin depender del gráfico.
29. Como persona que explora, quiero modificar monto, plazo, tasa y cargos, para comprender cómo cambian las consecuencias.
30. Como persona que explora una cotización, quiero reconocer qué valores se volvieron hipotéticos o requieren revisión, para no atribuir al banco una oferta modificada.
31. Como persona que modifica condiciones, quiero aplicar o descartar mis ediciones, para mantener gráficos y totales coherentes con los datos aplicados.
32. Como persona con una edición inválida, quiero conservar el trabajo y ver el error junto al dato, para corregirlo sin perder la comparación.
33. Como persona que aporta contexto personal, quiero introducir ingresos, gastos y cuotas vigentes, para relacionar la nueva financiación con mi margen mensual.
34. Como persona que prefiere no aportar contexto, quiero comparar las ofertas igualmente, para no compartir información innecesaria.
35. Como persona cuyo margen resulta negativo, quiero ver el valor negativo, para reconocer la presión mensual que produciría esa alternativa.
36. Como persona con gastos ya incluidos en mi contexto, quiero evitar contarlos otra vez como cargos de la oferta, para obtener un margen coherente.
37. Como persona que guarda un borrador, quiero retomarlo incompleto otro día, para reunir la información a mi ritmo.
38. Como persona que guarda cambios pendientes, quiero distinguirlos del resultado aplicado, para no interpretar una edición sin aplicar como cálculo vigente.
39. Como persona que conserva su información, quiero exportar e importar una copia, para recuperarla o trasladarla manualmente a otro dispositivo.
40. Como persona que importa un archivo inválido, quiero conservar mis datos anteriores, para evitar pérdidas por un archivo defectuoso.
41. Como persona con archivos de préstamos anteriores, quiero seguir usándolos en la herramienta existente, para no perder trabajo al incorporar el comparador.
42. Como persona cuyo almacenamiento falla, quiero recibir un error y mantener mis entradas, para poder intentar otra vez o exportar una copia.
43. Como persona que usa el teléfono, quiero leer y editar sin recortes ni controles amontonados, para comparar ofertas en mi dispositivo habitual.
44. Como persona que usa teclado o lector de pantalla, quiero acceder a controles, errores y detalle mensual, para completar el mismo flujo.
45. Como persona que consulta una tasa integral publicada, quiero distinguirla de la tasa de interés del modelo, para no sumar cargos dos veces.
46. Como persona que revisa el resultado, quiero reconocer fuente, fecha y alcance de una condición, para distinguir una cotización, una simulación y un perfil contrastado.
47. Como persona que cambia de país o producto, quiero que se revisen las reglas dependientes, para no arrastrar supuestos incompatibles.
48. Como persona que aprende mediante escenarios, quiero explicar qué cambió en aporte, cuota, plazo y costo, para decidir con una comprensión propia.

## Implementation Decisions

### 1. Arquitectura y superficie pública

- Se respeta ADR 0001: Kotlin Multiplatform shared es el módulo canónico de cálculo para Android y Web; el motor TypeScript permanece congelado.
- La superficie pública principal propuesta es un espacio de trabajo de comparación, denominado conceptualmente ComparisonWorkspace. Reutiliza el patrón de WorkspaceStore: comandos de edición/aplicación, un estado observable y operaciones locales de guardado/importación/exportación.
- Se necesita un estado de comparación propio: WorkspaceState actual exige un préstamo completo y sus resultados original/modificado, mientras esta función contiene dos ofertas independientes y datos incompletos. No se reinterpretan esas invariantes ni se reutiliza original/modificado para nombrar entidades distintas.
- Se reutilizan Decimal, conversiones de tasas, simulateLoan para los casos compatibles, adaptadores de ScenarioStorage, formato monetario y presentación Compose. No se construye otro motor de amortización ni un framework genérico de escenarios.
- El resultado mensual de una oferta cotizada debe permitir pago conocido con desglose desconocido. No se fabrican LoanResult o MonthRow con intereses/capital en cero para adaptar la cotización al gráfico existente.
- La presentación consume el mismo estado aplicado para gráfico, tabla, resúmenes y margen. Solo las coordenadas de dibujo se convierten a punto flotante; el dinero y las diferencias conservan aritmética decimal.
- La vista principal reutiliza la disposición mensual alineada y la identidad visual existentes. El componente actual acopla sus entradas a dos LoanResult; se adapta la presentación al contrato de oferta, preservando el uso de la herramienta de préstamos.

### 2. Contrato de comparación y procedencia de datos

La comparación contiene una necesidad de financiación, un país de operación seleccionado, una moneda común, un mes de referencia, dos posiciones de oferta y contexto personal opcional. Durante el borrador puede faltar cualquiera de esos datos; la ausencia determina qué resultados están disponibles.

| Elemento | Información requerida cuando corresponda |
| --- | --- |
| Necesidad en efectivo | Importe neto objetivo; las ofertas se contrastan contra ese objetivo. |
| Necesidad de compra | Identificación común de la compra; precio de cada alternativa, descuento si se conoce y aporte propio inicial. |
| Oferta | Identidad A/B, nombre opcional, tipo de producto y modo cotización/simulación. |
| Tarjeta | Existente o nueva; la operación analizada es una compra aislada, sin saldo rotativo dentro del modelo. |
| Cotización | Mes de primera cuota, número y pagos mensuales; cuotas uniformes pueden introducirse como cantidad e importe, y las excepciones como importes por mes. |
| Simulación | Capital contractual, tasa fija con unidad/frecuencia explícita, plazo y mes de primera cuota; cuota base calculada o conocida y compatible con el modelo. |
| Cargos | Identidad, concepto, importe, mes o intervalo, tratamiento de caja, inclusión en otro pago y carácter nuevo o preexistente. |
| Procedencia | Registrado por la persona, estimado por ella o desconocido; fuente/fecha opcionales al comenzar. |
| Contexto personal | Ingresos netos mensuales, gastos habituales y cuotas vigentes; cobertura de cargos preexistentes para evitar duplicación. |

- Cada dato conocido conserva si es registrado o estimado. Desconocido es ausencia explícita, no cadena cero, tasa cero ni importe cero.
- Un cero confirmado es válido para tasa, cargos, aportes, gastos o ingresos cuando el campo lo permita. Capital financiado y necesidad en efectivo deben ser positivos; la comparación inicial no añade una modalidad de compra íntegramente de contado sin financiación.
- Cotización registrada y escenario de oferta editado se distinguen. Basta conservar la cotización de referencia y las entradas de trabajo; no se exige historial ilimitado ni una biblioteca de ofertas.
- Se conserva por separado el borrador y las entradas aplicadas. Los resultados calculados son derivados y no se serializan como fuente de verdad.
- Los campos personales no bloquean la comparación de ofertas; fuente, proveedor y documento se completan progresivamente. Su ausencia impide afirmar que el perfil fue contrastado, no realizar una operación aritmética identificada como simulación.
- El contexto usa entradas monetarias no negativas; sus márgenes y las diferencias entre ofertas admiten signos. No se fuerza un resultado negativo al tipo Money no negativo ni se recorta a cero.
- Las entradas respetan los límites monetarios vigentes del dominio; los agregados se calculan con Decimal y nunca se envuelven por desbordamiento ni se truncan silenciosamente.

### 3. Disponibilidad y validación progresivas

- Con una sola oferta o datos parciales se permite guardar y consultar la información conocida. No se afirma que dos ofertas sean equivalentes hasta comprobar necesidad, moneda y datos requeridos.
- Se distinguen, por resultado, datos insuficientes, resultado parcial y resultado completo bajo las entradas declaradas. Una estimación mantiene esa marca aunque todos los campos necesarios tengan valor.
- Un cargo desconocido impide declarar completo el costo afectado; puede mantenerse visible la suma de pagos conocidos con su alcance explícito. No se muestra ahorro definitivo a partir de costos parciales.
- La moneda o el objetivo diferentes permiten conservar ambas entradas, pero bloquean diferencias monetarias presentadas como comparación equivalente. No se hacen conversiones de divisa.
- Un dato presente debe ser sintáctica y semánticamente válido. Los borradores admiten campos ausentes, no importes negativos en entradas no negativas, valores no finitos, versiones desconocidas o estructuras arbitrarias. Una edición temporal inválida permanece en pantalla con error y no reemplaza entradas aplicadas ni guardadas.
- Dinero de entrada con máximo dos decimales; tasas expresadas como fracciones decimales internamente y porcentajes explícitos en la interfaz. Dinero serializado como cadenas.
- Se mantienen precisión decimal de 40 dígitos y redondeo de mitades hacia arriba a dos decimales en los puntos establecidos por las convenciones. Las tasas convertidas no se redondean a la etiqueta visual.
- Meses y cantidades de cuotas son enteros; plazo y último mes calculable se limitan a 1.200. No se recorta un calendario excedido para presentarlo como completo.
- Un cambio de país, moneda, producto, capital o tasa revalida las condiciones dependientes. No se interpretan CFT, VTUA o una tasa de convención desconocida como tasa de interés.

### 4. Contabilidad de caja y equivalencia

Se distinguen capital contractual, importe aplicado a la necesidad, efectivo neto, aporte propio, pagos y cargos. Cada cargo tiene una única identidad y no produce dos salidas por figurar también en un pago compuesto.

| Tratamiento | Regla |
| --- | --- |
| Cargo pagado con dinero propio al inicio | Se suma a la salida inicial; no incrementa el capital automáticamente. |
| Cargo descontado del crédito | Reduce el efectivo neto recibido; ya forma parte del capital que se devuelve. No se vuelve a sumar como salida independiente. |
| Cargo financiado | Integra el capital contractual y se separa del importe aplicado a la necesidad. Se devuelve mediante las cuotas; no se cobra otra vez como pago independiente. |
| Cargo cobrado aparte después | Se incorpora una vez en su mes o intervalo. |
| Cargo incluido en una cuota cotizada | Puede desglosarse si se conoce, pero no se suma otra vez al pago. |
| Cargo preexistente | Se muestra como obligación que ya existía y se excluye del costo adicional de la operación; su efecto en contexto personal se cuenta una vez. |

Para efectivo, el neto recibido del crédito es capital menos cargos descontados y financiados que no se entregan para la necesidad; esas modalidades son excluyentes para un mismo cargo. Los gastos pagados con recursos propios se muestran como necesidad inicial adicional. Si un cargo se cubre con el propio desembolso, se clasifica como descuento para comprobar el efectivo neto utilizable.

Para compras, el capital cubre el precio de esa oferta menos aporte propio, más cargos financiados o descontados que el préstamo deba cubrir. Si se introduce un capital que no satisface esa identidad, se conserva el borrador y se explica la discrepancia; no se ajusta silenciosamente el capital ni el precio.

- Total pagado por la alternativa: aporte propio, pagos de financiación y cargos nuevos pagados aparte, contando cada salida una vez.
- Costo nominal de financiación en efectivo: total pagado por la alternativa menos efectivo neto aplicado a la necesidad.
- Costo nominal de financiación en compra: total pagado por la alternativa menos precio de esa misma oferta. La diferencia de precios se presenta separada del costo de financiación.
- El cargo financiado/descontado puede aparecer como componente explicativo del costo y estar ya devuelto dentro del pago; mostrarlo no crea una segunda salida de caja.
- Una cotización cuyo pago no permite reconciliar el capital y sus cargos se marca para revisión; no se inventa una subvención, un saldo perdonado ni una liquidación.
- Se identifican tanto el plazo de cuotas como el último mes de cargos nuevos. Pagar la compra no implica cancelar la tarjeta ni terminar su mantenimiento.
- Los cargos recurrentes tienen intervalo o alcance conocido. Si su final es desconocido, el costo sigue siendo parcial; puede mostrarse un subtotal hasta el horizonte declarado, sin denominarlo costo total de vida del producto.
- La comparación usa un horizonte común que cubre cuotas y cargos nuevos con final conocido de ambas ofertas. Un cero posterior a la última cuota solo se muestra cuando se conoce que no quedan pagos/cargos modelados; desconocido no se dibuja como cero.

### 5. Cotización y simulación mensual

**Cotización:** reproduce los pagos introducidos y sus meses, sin exigir tasa. Si no existe desglose, la barra muestra pago cotizado sin separar capital/interés. El total de cuotas conocido puede calcularse sin atribuirlo a componentes desconocidos. Una tasa publicada opcional se conserva con su nombre, unidad, base de impuestos y procedencia; no modifica el calendario transcrito.

**Simulación:** usa el motor de tasa fija mensual compatible, sin eventos de abono, omisión o recuperación en este flujo. Se conservan las reglas actuales de amortización, cuota final, seguros y cálculo incompleto. Los demás cargos se modelan por importes y calendario declarados; no se disfrazan de seguro.

- Los seguros pueden usar los tipos ya soportados cuando la convención coincida. No se añade un lenguaje de fórmulas de cargos ni un cálculo fiscal universal.
- Los cargos generales del primer alcance se introducen como importes monetarios por fecha/intervalo. Si solo se conoce un porcentaje contractual no modelado, el importe queda por completar o se introduce como estimación explícita; no se adivina su base.
- Si una cotización y el modelo no coinciden, se conserva la cotización y se muestran las diferencias. No se ajusta la tasa automáticamente para hacerla coincidir.
- Cambiar monto, plazo o tasa de una cotización no autoriza escalar sus pagos. Se exige completar pagos actualizados o pasar a simulación con datos suficientes. Los valores dependientes quedan por revisar, mientras la cotización original permanece identificable.
- Al cambiar una base que afecte un cargo o impuesto, su valor cotizado no se arrastra como constante confirmada. Se revalida o se introduce un supuesto explícito; sin ello el costo correspondiente es parcial.
- Los pagos cotizados variables son admisibles si el calendario es finito y conocido; no implican saldo rotativo ni nuevas compras. La simulación de una promoción por etapas o interés diario queda fuera del modelo inicial.

### 6. Tiempo, margen y visualización

- El momento inicial se identifica como mes 0 para aporte y cargos iniciales. Las cuotas se ubican por sus meses de vencimiento conocidos; si ocurren en el mismo mes inicial se agregan en ese mes, conservando el desglose de salida inicial y cuota.
- Cuando no hay fechas, se usa una alineación relativa explícita y estimada, con primera cuota en mes 1. No se presenta como calendario contractual confirmado.
- Introducir fechas de caja no añade devengo diario ni prorrateos al préstamo mensual.
- Ambos gráficos usan los mismos ejes, escala monetaria, horizonte y selección de mes. La vista principal es mensual; en móvil las dos ofertas pueden apilarse manteniendo la alineación.
- El resumen muestra aporte inicial, cuota uniforme o rango cuando varíe, número/último mes de pagos, total pagado y costo de financiación cuando estén disponibles. Las diferencias se etiquetan B menos A y con lenguaje de aumento/reducción.
- El detalle de cada mes está disponible como texto/tabla accesible. Un pago sin desglose permanece como pago sin desglose; las partes conocidas y desconocidas no comparten una leyenda que sugiera certeza inexistente.
- Gráfico, tabla, resúmenes y contexto se actualizan desde una misma aplicación del borrador. Los campos pendientes se identifican y pueden aplicarse o descartarse; no aparecen totales nuevos junto a un gráfico anterior sin advertencia.
- Margen mensual: ingresos netos menos gastos habituales, cuotas vigentes y salidas nuevas de la oferta en ese mes. En el mes inicial incluye los aportes/cargos iniciales conocidos.
- Un cargo preexistente común se mantiene en ambos escenarios personales, aunque una alternativa no use esa tarjeta. Se identifica si ya está incluido en gastos o cuotas vigentes; si no, se incorpora una vez. Una atribución desconocida impide afirmar un margen completo.
- Si una cuota cotizada ya incluye ese cargo preexistente, se necesita su importe para separar la salida incremental y evitar duplicación en el margen. Si no se conoce, se muestra el pago cotizado y la limitación.
- Los ingresos/gastos mensuales se usan como referencia constante bajo supuesto visible; no se proyectan aumentos, ahorro inicial ni saldo acumulado. Los márgenes negativos conservan su signo.
- El margen no acredita disponibilidad del aporte inicial, solvencia entre fechas de cobro, aprobación del crédito ni dinero en cuenta. No se crea un puntaje de salud financiera o ganador automático.

### 7. Borradores, persistencia y compatibilidad

- Una comparación incompleta válida puede guardarse/exportarse expresamente como borrador, incluyendo cotizaciones, estimaciones, datos ausentes y entradas pendientes. Guardar borrador no equivale a aplicar cambios.
- Al reabrir se restauran las entradas aplicadas y el borrador pendiente, recalculando resultados únicamente desde las primeras. Si no había entradas aplicadas, se muestra el borrador sin resultados inventados.
- El documento de comparación tiene discriminante propio y versión inicial, separados del documento de préstamo existente. Usa una clave de almacenamiento diferente y los adaptadores de almacenamiento locales existentes.
- No se sobreescribe la versión del préstamo ni su contrato original/modificado. Los archivos anteriores siguen abriéndose en la herramienta de préstamos; el importador reconoce el tipo y dirige al flujo correcto o informa cómo abrirlo. No convierte eventos antiguos en ofertas perdiéndolos.
- No se almacenan gráficos, cronogramas derivados ni supuestos deducidos como si fueran cotizaciones originales. Sí se conserva la procedencia y la versión de reglas/modelo usada para interpretar las entradas.
- La importación valida tamaño real UTF-8, discriminante, versión, tipos, valores presentes, identidades y referencias antes de sustituir estado. Se mantiene el límite de 2 MB; una versión no soportada o archivo inválido no destruye datos previos.
- Lectura, escritura o cuota de almacenamiento fallidas muestran error y conservan el trabajo en memoria y el último dato guardado. Un ejemplo no se guarda encima de datos corruptos automáticamente.
- No se requieren cuenta, sincronización, backend ni envío de datos financieros. El guardado/exportación es explícito; se comunica cuando hay trabajo sin guardar.

### 8. Países, indicadores y fidelidad

Ambos países admiten el flujo de cotización y el modelo mensual educativo dentro de su alcance documentado. Seleccionar país no acredita reproducción de cualquier contrato. Cada perfil contrastado debe identificar producto, moneda, convención, fuente fechada, versión y límites.

- Se distinguen cotización transcrita, simulación bajo supuestos y perfil contrastado con cronograma independiente.
- CFT/VTUA cotizados se conservan como indicadores de la fuente cuando estén disponibles, con su alcance. No se introducen como tasa de interés ni se recalculan a partir de la suma nominal de pagos.
- La primera entrega no calcula automáticamente CFT/VTUA ni impuestos por país. Los cargos/impuestos se introducen como importes conocidos o estimados y con inclusión explícita en las cuotas. Una conversión de TNA a efectiva anual no se etiqueta CFT.
- Antes de presentar un perfil como reproducción contractual se contrasta un calendario completo independiente, además de reglas y documentos oficiales. Tasa, CFT o primera cuota por sí solos no validan el resto del calendario.
- La primera cuota, cargos asociados y momento de facturación pueden diferir del modelo de anualidad. En esos casos se admite cotización; no se publicita la simulación mensual como exacta para esa entidad.
- La implementación debe incluir casos de ambos países y una matriz de cobertura verificable. La liberación de la comparación educativa no autoriza afirmar fidelidad a perfiles todavía no contrastados.
- Las discrepancias de precancelación argentinas ya registradas siguen pendientes en la herramienta existente. No se heredan como autoridad normativa ni se exige activar/corregir prepago dentro de este comparador que no lo utiliza.

## Testing Decisions

### Superficie principal confirmada

Probar el comportamiento observable del espacio de trabajo de comparación: crear/completar dos ofertas, editar y aplicar, consultar una instantánea coherente de resultados, guardar un borrador, exportar/importar y retomar. Es la superficie nueva más alta que abarca reglas, cálculo y persistencia sin depender de funciones privadas.

Reutilizar la estrategia de WorkspaceStoreTest y el almacenamiento en memoria; ScenarioPersistenceTest aporta precedentes de ida y vuelta y rechazo de versiones; LoanTest, AmortizationAccountingTest y RatesTest aportan referencias literales e invariantes. La prueba existente de marcas del gráfico no acredita legibilidad ni interacción móvil: se añaden pocas pruebas de pantalla y revisión de teclado/tabla.

Un buen test usa entradas y resultados esperados independientes, comprueba lo que recibe la persona y evita aserciones sobre nombres internos, número de llamadas o estructura del árbol de componentes. Solo añadir pruebas inferiores para un algoritmo no trivial que no quede bien discriminado por los casos superiores.

### Referencias numéricas sintéticas

Todos los importes están expresados en una misma moneda, sin pretender reproducir un banco. La tasa cero es un dato explícito en los casos que la usan. Los cargos no nombrados se confirman como cero; si no se confirma esa ausencia, los resultados tienen que permanecer parciales.

| Caso | Entradas | Resultado esperado |
| --- | --- | --- |
| F01: capital, neto y tres tratamientos de cargo | Efectivo objetivo 1.000,00. Capital 1.060,00, cargo financiado 40,00, cargo descontado 20,00 y cargo inicial de recursos propios 30,00; tasa cero, 2 cuotas. | Neto 1.000,00; salida inicial 30,00; cuotas 530,00 y 530,00; total pagado 1.090,00; costo de financiación 90,00. Los cargos de 40,00 y 20,00 no vuelven a sumarse como salidas independientes. |
| F02: dos precios, dos aportes y dos plazos | Mismo bien. A: precio 900,00, aporte 200,00, cargo inicial propio 10,00, capital 700,00, tasa cero, 2 cuotas. B: precio 1.000,00, aporte cero, 4 cuotas cotizadas de 250,00, sin tasa/desglose de interés. | A: inicial 210,00, cuotas 350,00, total 910,00 y costo de financiación 10,00. B: total 1.000,00 y costo adicional de financiación 0,00; pagos sin desglose. B menos A: total +90,00, aporte inicial -210,00 y última cuota +2 meses si ambas empiezan en mes 1. No hay ganador automático. |
| F03: cotización incompleta | Efectivo neto 1.000,00; 8 cuotas cotizadas de 150,00; sin tasa ni desglose; posible cargo adicional desconocido. | Suma de cuotas conocidas 1.200,00. Costo completo no disponible; no se inventa desglose ni se afirma que todos los cargos valen cero. |
| F04: cargo preexistente frente a nuevo | Compra 600,00, 5 cuotas de 120,00. La persona ya paga un cargo de tarjeta 20,00 incluido en gastos. Alternativa con tarjeta nueva añade otro cargo de 20,00 en meses 1–5, con final explícito. | Compra con tarjeta existente: pagos nuevos 600,00 y costo adicional 0,00. Nueva: pagos nuevos 700,00 y costo adicional 100,00. El cargo preexistente permanece en el contexto de ambas alternativas. |
| F05: margen mensual y signo | Ingreso 1.000,00; gastos 400,00 que ya incluyen el cargo preexistente de F04; cuotas vigentes 100,00. Pago nuevo 120,00 o 140,00. Variante: ingreso 600,00. | Márgenes 380,00 y 360,00. Con ingreso 600,00: -20,00 y -40,00. No se suma otra vez el cargo incluido ni se recorta el margen a cero. |
| F06: amortización y redondeo existentes | Capital 1.000,00, tasa mensual 1%, 2 cuotas, sin cargos ni seguros. Variante: capital 100,00, tasa cero, 3 cuotas. | Primer caso: cuotas 507,51; intereses 10,00 y 5,02; saldo tras primer pago 502,49; total 1.015,02. Variante: 33,33; 33,33; 33,34, sin mes espurio. |

### Casos de comportamiento adicionales

1. Guardar una oferta A y oferta B incompleta; reiniciar; restaurar datos, marcas de estimación y pendientes sin aplicar silenciosamente.
2. Borrar un cargo conocido y dejarlo desconocido: al aplicar, el costo cambia a parcial; no conserva una cifra completa obsoleta.
3. Editar capital/plazo de una cotización sin tasa: solicitar revisión de pagos o simulación suficiente; conservar cotización y resultado aplicado hasta la acción correspondiente.
4. Rechazar dinero con más de dos decimales, números no finitos, meses fuera de rango, referencias duplicadas, moneda incompatible y versión/tipo desconocidos; permitir cero explícito donde corresponde.
5. Comprobar todos los tratamientos de cargo e inclusión en cuota: ninguna suma duplica cargo, seguro, impuesto o aporte.
6. Un cargo preexistente sin importe/inclusión conocidos deja incompleto el margen afectado; uno nuevo con fin desconocido impide costo definitivo y cero posterior ficticio.
7. Una edición inválida, importación fallida o fallo de almacenamiento conserva datos previos; comprobar el límite UTF-8 con contenido no ASCII y la compatibilidad del documento de préstamo existente.
8. Comparar calendarios de distinto comienzo, cuotas y cargos posteriores: ejes compartidos, mes inicial visible y totales obtenidos de las mismas filas aplicadas.
9. En Android y Web, ejecutar un flujo representativo de cotización y otro de simulación, incluyendo contexto opcional y recuperación de borrador. Reutilizar las comprobaciones vigentes de shared; no ampliar el motor TypeScript.
10. Revisar teléfono compacto, teclado, lectura de errores, selección de mes y tabla alternativa. Una persona de la audiencia debe explicar aporte, cuota, duración, costo nominal y datos faltantes sin ayuda. Registrar observación real; una prueba técnica no sustituye esa aceptación.

Estas son decisiones y casos de prueba para la implementación. En la sesión de especificación solo se verifican el documento y las referencias aritméticas; no se afirma que el producto ya pase estas pruebas. Las referencias F01–F06 se comprobaron en esta sesión mediante 22 aserciones independientes con centavos enteros, sin usar el motor bajo prueba.

## Out of Scope

- Saldo rotativo, pago mínimo, avances, nuevas compras dentro del cálculo de una tarjeta y varias operaciones simultáneas con reglas independientes.
- Refinanciación, compra de cartera, reemplazo de deudas y liquidación de la obligación anterior.
- Abonos, omisiones, recuperación o mora contractual dentro del comparador nuevo. La herramienta existente conserva sus funciones.
- Créditos indexados UVR/UVA, tasas variables, intereses diarios, prorrateos y simulación de promociones por etapas. Un calendario finito de pagos cotizados puede transcribirse sin afirmar que se simula su regla.
- Ajuste por inflación, valor presente, rentabilidad de inversión, trayectoria de ahorro, evaluación probabilística y aprobación crediticia.
- Planificación conjunta del hogar, biblioteca ilimitada de ofertas, más de dos posiciones comparadas y sincronización entre cuentas/dispositivos.
- Catálogo de bancos o tasas en vivo, recomendación/contratación automática, conexión bancaria, cálculo tributario integral y cálculo automático de CFT/VTUA.
- Ampliar extracción PDF/VLM para reconocer todos los campos del comparador. La entrada manual y JSON cubre esta entrega; cualquier reutilización de ingesta debe conservar revisión humana y no inventar datos faltantes.
- Rediseñar el motor legado, sustituir las convenciones del préstamo existente o corregir todos los perfiles regulatorios como parte de esta entrega.

## Further Notes

### Autoridad y continuidad

- El alcance procede de la [investigación y entrevista confirmada](../../docs/research/financial-decision-product-discovery.md); el vocabulario está en [CONTEXT](../../CONTEXT.md), con [GLOSSARY](../../GLOSSARY.md) como referencia del préstamo existente.
- Se respeta [ADR 0001](../../docs/adr/0001-canonical-kmp-shared-engine.md) y las [convenciones financieras](../../docs/financial-conventions.md). Las decisiones específicas de comparación de esta spec amplían la representación de ofertas; no alteran implícitamente el préstamo anterior.
- La confirmación de cobertura de pruebas corresponde al flujo completo compartido más móvil/teclado. No se reabre la entrevista.
- La etiqueta ready-for-agent identifica la especificación publicada en el tracker local; no reserva tarea, no acredita integración ni equivale a permiso de despliegue. La coordinación asigna ID, rama y límites antes de implementar según [tracking](../../docs/tracking.md).
- Publicación local de esta especificación; sin issue remoto de GitHub ni implementación en esta sesión.

### Fuentes de contraste y límites

Fuentes consultadas el 2026-10-13. Sirven para fijar límites y preparar referencias verificables; no constituyen una certificación completa de vigencia o reproducción contractual:

- Colombia: [Decreto 2555, artículo 2.35.4.3.1](https://cancilleria.gov.co/sites/default/files/Normograma/docs/decreto_2555_2010_pr034.htm). El VTUA tiene metodología propia; la operación tipo de tarjeta no equivale a esta compra aislada. No se traslada ese indicador al interés del modelo.
- Colombia: [Bancolombia, facturación de intereses de compras](https://www.bancolombia.com/centro-de-ayuda/preguntas-frecuentes/intereses-primer-mes-compras-tarjeta-credito). Ilustra diferencias de momento de facturación y producto; exige calendario cotizado o contraste antes de afirmar coincidencia.
- Argentina: [BCRA, Tasas de interés en las operaciones de crédito](https://www.bcra.gob.ar/Pdfs/Texord/t-tasint.pdf), secciones 3.2 y 3.4. El texto recuperado indica ordenamiento al 01/07/2024, A8057; su ubicación oficial no demuestra por sí sola ausencia de cambios posteriores. Verificar aplicabilidad antes de liberar un perfil.
- Argentina: [BNA, cuadro de primera cuota de préstamo fijo](https://www.bna.com.ar/Downloads/Personas_NacionDestinoLibre_566%20CC.pdf). Distingue cuota base, IVA y costos asociados; la primera cuota aproximada no es una referencia suficiente para validar un cronograma completo.

Antes de activar una etiqueta de perfil contrastado, conservar una referencia fechada del calendario completo, sus cargos, la comprobación independiente y las discrepancias admitidas. Las entradas del usuario y las simulaciones educativas pueden seguir funcionando con su procedencia y límites visibles.
