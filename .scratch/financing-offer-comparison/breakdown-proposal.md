# Propuesta de tickets — Comparador de ofertas de financiación

Estado: aprobado para implementación por el usuario el 2026-10-14; tickets publicados en issues/.
Fecha: 2026-10-14.
Fuente: [especificación canónica](spec.md), que permanece sin cambios.
Tracker previsto: Markdown local, un archivo por ticket después de aprobar el desglose.

La numeración es local a esta propuesta; no reserva IDs FS ni sustituye el tablero compartido. La implementación deberá reservar responsable, rama y worktree según el protocolo del proyecto. Los bloqueos indican entregas integradas que habilitan trabajo, no autorización para escribir simultáneamente en el mismo checkout.

Cada ticket entrega comportamiento visible, validación y pruebas por la superficie del espacio de trabajo compartido. Desde que exista guardado, cada ampliación incluye sus nuevas entradas en la recuperación; no se deja la persistencia para una reparación final. Móvil y accesibilidad básica forman parte de cada pantalla desde su primera entrega; el ticket 10 verifica el conjunto.

No se propone prefactor amplio: se reutiliza el motor canónico KMP y la presentación existente, adaptando solo lo necesario dentro de la primera entrega. El estado de comparación distingue dos ofertas e información incompleta del préstamo original/modificado.

## Resumen y dependencias

| Ticket | Entrega completa | Bloqueado por |
| --- | --- | --- |
| 01 | Comparar dos cotizaciones de préstamo en efectivo | Ninguno |
| 02 | Guardar y recuperar comparaciones incompletas | 01 |
| 03 | Simular financiación fija desde tasa y plazo | 01 |
| 04 | Incorporar cargos y explicar capital, efectivo y costo | 03 |
| 05 | Comparar la misma compra con precios y aportes distintos | 04 |
| 06 | Distinguir tarjetas existentes, nuevas y cargos preexistentes | 05 |
| 07 | Relacionar ofertas con el margen mensual personal | 06 |
| 08 | Explorar cambios sin perder la cotización original | 02, 04 |
| 09 | Mostrar alcance por país y procedencia de indicadores | 06 |
| 10 | Verificar el flujo completo en teléfono, Android y Web | 07, 08, 09 |

El 01 abre el flujo. El 02 añade recuperación y el 03 añade simulación; no se bloquean entre sí. El 04 necesita la simulación para comprobar cargos en ambos modos. El 05 añade la identidad de compra; el 06 distingue cargos de tarjetas. El 07 usa esa base común en el margen. El 08 requiere guardado y contabilidad de cargos, pero no necesita esperar al contexto personal ni a tarjetas: las ampliaciones posteriores conservan sus reglas de edición. El 09 comprueba el alcance de los productos admitidos en ambos países. El 10 requiere las tres ramas finales para verificar el flujo integrado.

## Propuestas detalladas

### 01 — Comparar dos cotizaciones de préstamo en efectivo

**What to build:** Introducir progresivamente dos cotizaciones para la misma necesidad de efectivo y consultar pagos mensuales, diferencias y límites de la información, sin conocer una tasa.

**Blocked by:** None — can start immediately

**Estado:** ready-for-agent.

- [ ] Se accede al comparador desde la app y se conserva el flujo de préstamos existente. Se eligen país, moneda, necesidad neta y ofertas A/B; una oferta incompleta puede consultarse sin exigir contexto personal.
- [ ] Se introducen cantidad e importe de cuotas, excepciones por mes y comienzo del calendario. Una fecha ausente produce alineación relativa estimada desde mes 1; el mes inicial y las cuotas en ese mismo mes conservan sus conceptos separados.
- [ ] Las entradas distinguen registrado, estimado, desconocido y cero confirmado. Se conservan campos ausentes y errores de edición sin sustituir el resultado aplicado; completar y aplicar actualiza conjuntamente gráfico, tabla y resumen.
- [ ] Ambas ofertas comparten escala, horizonte y selección de mes. El detalle accesible muestra pagos sin inventar capital/interés; el resumen muestra cuota/rango, duración y pagos conocidos. Las diferencias tienen signo y etiqueta B menos A, sin ganador automático.
- [ ] Solo se declaran totales y costo completos cuando sus datos lo permiten. Moneda o necesidad incompatible bloquean diferencias equivalentes; no se convierten divisas ni se deduce el país de la moneda.
- [ ] Se rechazan dinero con más de dos decimales, entradas negativas donde no corresponden, valores no finitos y meses/cuotas no enteros o superiores a 1.200. Los agregados usan aritmética decimal y las diferencias conservan el signo.
- [ ] Una prueba del espacio de trabajo reproduce F03: ocho pagos de 150,00 suman 1.200,00, pero el cargo desconocido impide costo completo. Otra verifica calendarios de distinto inicio/plazo y coherencia de las vistas. Controles y errores tienen etiquetas y acceso por teclado.

### 02 — Guardar y recuperar comparaciones incompletas

**What to build:** Guardar, exportar, importar y retomar un borrador con datos incompletos sin perder trabajo ni afectar archivos de préstamos anteriores.

**Blocked by:** 01 — Comparar dos cotizaciones de préstamo en efectivo

**Estado:** ready-for-agent.

- [ ] Guardado y exportación explícitos conservan entradas aplicadas, borrador pendiente, ausencias, estimaciones y procedencia disponibles; la interfaz identifica trabajo sin guardar.
- [ ] Al retomar se recalculan resultados solo desde las entradas aplicadas. Los pendientes siguen pendientes; un borrador sin aplicación previa no produce resultados inventados. Una entrada temporal inválida no reemplaza datos válidos guardados.
- [ ] El documento tiene tipo, versión inicial y clave de almacenamiento propios. Los documentos anteriores siguen abriéndose en la herramienta de préstamos, con orientación clara desde el importador, sin convertir ni perder eventos.
- [ ] Antes de sustituir estado se validan discriminante, versión, tipos, valores, identidades y referencias, además del límite de 2 MB medido en bytes UTF-8 reales. El dinero se serializa como cadenas y no se persisten resultados derivados.
- [ ] Importación inválida, versión no soportada y fallos de lectura/escritura/cuota muestran error y conservan memoria y último guardado. No se sobrescribe automáticamente información corrupta con ejemplos.
- [ ] Pruebas del espacio de trabajo verifican ida y vuelta con una oferta incompleta, pendientes sin aplicar, rechazo no destructivo, contenido no ASCII en el límite de tamaño y compatibilidad con documentos anteriores. El flujo funciona localmente sin cuenta ni envío de datos.

### 03 — Simular financiación fija desde tasa y plazo

**What to build:** Comparar una cotización con una simulación mensual de tasa fija compatible, viendo cuota y desglose calculado con las convenciones existentes.

**Blocked by:** 01 — Comparar dos cotizaciones de préstamo en efectivo

**Estado:** ready-for-agent.

- [ ] Cada oferta permite modo cotización o simulación. La simulación pide capital, tasa fija con convención/frecuencia, plazo y comienzo; permite cuota base conocida compatible y tasa cero explícita.
- [ ] Se reutiliza el motor canónico Kotlin Multiplatform y sus conversiones, precisión y redondeos; no se añade otro motor ni se amplía el motor TypeScript congelado.
- [ ] El desglose de capital, interés y seguros solo aparece cuando es calculable. Se admiten seguros existentes compatibles, su mes final y los posteriores al préstamo; no se disfrazan cargos generales como seguros.
- [ ] Una cotización conserva sus pagos y procedencia aunque difiera del modelo. Se muestran diferencias sin ajustar automáticamente la tasa ni exigir una tasa a la cotización.
- [ ] Se identifican cálculo incompleto, residuo final y límites de alcance. No se activan abonos, mora, recuperación, interés diario, indexación o promociones por etapas.
- [ ] F06 se verifica desde el espacio de trabajo con expectativas literales: 507,51 por cuota, intereses 10,00 y 5,02, saldo 502,49 y total 1.015,02; la variante de tasa cero paga 33,33, 33,33 y 33,34 sin mes adicional.
- [ ] Se comprueban tasa cero frente a desconocida, conversiones sin redondear la tasa a su etiqueta y seguro posterior. Si el guardado ya existe, preserva estas nuevas entradas y recalcula al cargar.

### 04 — Incorporar cargos y explicar capital, efectivo y costo

**What to build:** Añadir cargos a cotizaciones y simulaciones y ver una contabilidad de caja que distingue dinero recibido, pagos iniciales y costo de financiación.

**Blocked by:** 03 — Simular financiación fija desde tasa y plazo

**Estado:** ready-for-agent.

- [ ] Cada cargo tiene identidad y tratamiento único: propio inicial, descontado, financiado, posterior separado o incluido en cuota. La interfaz permite importe conocido, estimado o desconocido y calendario cuando corresponda.
- [ ] El neto utilizable descuenta del capital los cargos financiados/descontados que no se aplican a la necesidad. Un cargo financiado o incluido en cuota no se vuelve a cobrar como salida independiente.
- [ ] El resumen distingue capital, neto, salida inicial, total pagado y costo nominal de financiación. Se conserva y explica una discrepancia contable sin ajustar silenciosamente capital, pagos o necesidad.
- [ ] Los cargos recurrentes amplían el horizonte común hasta su final conocido. Final desconocido significa costo parcial y subtotal con alcance visible; nunca costo de vida completo ni cero futuro ficticio.
- [ ] Un porcentaje cuya base/regla no esté modelada exige importe conocido o estimación explícita. No se calculan impuestos universales ni se introducen tasas predeterminadas por país.
- [ ] F01 se comprueba de extremo a extremo: capital 1.060,00, cargos financiado 40,00/descontado 20,00/propio 30,00, neto 1.000,00, dos pagos de 530,00, total 1.090,00 y costo 90,00.
- [ ] Se prueban cargo incluido, varios cargos y seguros sin duplicación, borrado a desconocido que vuelve parcial el costo y recurrencia posterior a las cuotas. Gráfico, tabla, resumen y guardado disponible conservan el mismo tratamiento.

### 05 — Comparar la misma compra con precios y aportes distintos

**What to build:** Comparar dos formas de financiar el mismo bien, mediante préstamo o compra aislada en cuotas, incorporando descuentos y aportes propios.

**Blocked by:** 04 — Incorporar cargos y explicar capital, efectivo y costo

**Estado:** ready-for-agent.

- [ ] La necesidad de compra identifica una misma adquisición y permite precio, descuento conocido y aporte propio por oferta. Se admiten préstamo personal y compra aislada con tarjeta; no saldo rotativo, mínimos ni nuevas compras.
- [ ] Se comprueba capital igual a precio menos aporte más cargos financiados/descontados que deba cubrir. Una discrepancia conserva el borrador y explica qué revisar; no corrige silenciosamente el precio.
- [ ] El mes inicial separa aporte y cargos propios. Total pagado incluye cada salida una vez; costo nominal se calcula contra el precio de esa oferta y la diferencia de precios se muestra por separado.
- [ ] Se admiten cotizaciones sin tasa y simulaciones únicamente cuando sus condiciones coinciden con el modelo fijo mensual. Un calendario variable conocido no se presenta como simulación de una promoción.
- [ ] F02 reproduce A: inicial 210,00, cuotas 350,00, total 910,00 y costo 10,00; B: cuatro pagos de 250,00, total 1.000,00 y costo 0,00 sin desglose inventado.
- [ ] Para F02, B menos A muestra total +90,00, inicial -210,00 y última cuota +2 meses con inicio común en mes 1. No se declara ganador ni disponibilidad del aporte.
- [ ] Se prueba necesidad distinta, aporte incompatible y capital no positivo; las comparaciones en efectivo siguen funcionando y el guardado disponible incluye las nuevas entradas.

### 06 — Distinguir tarjetas existentes, nuevas y cargos preexistentes

**What to build:** Ver el costo adicional de una compra según se use una tarjeta existente o nueva, separando obligaciones que ya existían.

**Blocked by:** 05 — Comparar la misma compra con precios y aportes distintos

**Estado:** ready-for-agent.

- [ ] Cada oferta de tarjeta declara existente o nueva; sus cargos identifican si nacen con la operación o ya existían, junto con importe, inclusión en pago y calendario.
- [ ] Un cargo preexistente no se atribuye al costo incremental de la compra. Un cargo nuevo se cuenta una vez, aunque aparezca también como componente explicativo de una cuota.
- [ ] La obligación preexistente se conserva como base común de ambas alternativas personales. Se registra si ya está cubierta en gastos/cuotas para que el contexto pueda contarla una sola vez.
- [ ] Cuando un pago cotizado contiene un cargo preexistente, se exige conocer su importe para separar la salida incremental; si falta, se mantiene el pago conocido y se marca la limitación.
- [ ] El mantenimiento nuevo puede continuar después de las cuotas. Un final desconocido conserva costo parcial y no supone cancelación automática de la tarjeta.
- [ ] F04 muestra compra existente con pagos nuevos 600,00/costo 0,00 y tarjeta nueva con pagos nuevos 700,00/costo 100,00 para cinco cargos nuevos de 20,00. Se prueba también cargo preexistente incluido en cuota, sin importe y sin duplicación.
- [ ] El estado, las vistas y el guardado disponible conservan las distinciones; préstamo y compra sin tarjeta mantienen sus resultados.

### 07 — Relacionar ofertas con el margen mensual personal

**What to build:** Añadir ingresos, gastos y cuotas vigentes de forma opcional y comparar el margen mensual que dejaría cada oferta.

**Blocked by:** 06 — Distinguir tarjetas existentes, nuevas y cargos preexistentes

**Estado:** ready-for-agent.

- [ ] El contexto personal puede añadirse progresivamente u omitirse sin bloquear las ofertas. Ingresos, gastos y cuotas vigentes se identifican como referencia mensual constante bajo un supuesto visible.
- [ ] El margen resta gastos, cuotas vigentes y salidas nuevas por mes; en mes 0 incorpora aporte y cargos iniciales conocidos. Usa las mismas entradas aplicadas que gráficos y totales.
- [ ] Los cargos preexistentes se mantienen en ambas alternativas y se incorporan una vez según su cobertura declarada. Cobertura o importe desconocido impide afirmar margen completo.
- [ ] Las entradas monetarias son no negativas donde corresponde, pero el margen admite signo y nunca se recorta a cero ni se fuerza al tipo de dinero no negativo.
- [ ] F05 produce 380,00 y 360,00 con ingreso 1.000,00, gastos 400,00 y cuotas vigentes 100,00; con ingreso 600,00 produce -20,00 y -40,00.
- [ ] Se prueba contexto omitido, incompleto, cero explícito, cargos preexistentes cubiertos/no cubiertos y salida inicial. Guardar/retomar conserva contexto y su cobertura.
- [ ] La presentación no convierte el margen en saldo acumulado, ahorro, disponibilidad del aporte, aprobación de crédito, puntaje ni recomendación automática.

### 08 — Explorar cambios sin perder la cotización original

**What to build:** Editar monto, plazo, tasa y cargos, aplicar o descartar escenarios hipotéticos y retomarlos conservando la cotización de referencia.

**Blocked by:** 02 — Guardar y recuperar comparaciones incompletas; 04 — Incorporar cargos y explicar capital, efectivo y costo

**Estado:** ready-for-agent.

- [ ] La cotización original sigue identificable y separada de las entradas hipotéticas. Se pueden modificar monto, plazo, tasa y cargos en ambos modos y en los productos disponibles.
- [ ] Aplicar actualiza conjuntamente todas las vistas desde una instantánea; descartar recupera las entradas aplicadas. Una edición inválida conserva el trabajo y muestra error junto al campo.
- [ ] Modificar una cotización no escala pagos ni infiere una tasa. Los pagos dependientes requieren actualización/revisión o pasar expresamente a una simulación con datos suficientes.
- [ ] Modificar país, moneda, producto, capital, plazo o tasa revalida lo dependiente. Un cargo/impuesto ligado a una base cambiada no conserva certeza confirmada sin revisión o supuesto explícito.
- [ ] Al convertir un cargo a desconocido se retira el costo completo afectado; ningún gráfico o total mezcla resultados anteriores con entradas nuevas aplicadas.
- [ ] Guardar y recuperar preserva original, aplicado y pendiente por separado. La carga recalcula solo lo aplicado, sin aplicar cambios silenciosamente ni almacenar cronogramas como fuente de verdad.
- [ ] Se prueba el flujo completo editar–error–corregir–aplicar–descartar–guardar–retomar desde el espacio de trabajo. Las entregas posteriores de compra, tarjeta o contexto mantienen este comportamiento al incorporar sus campos.

### 09 — Mostrar alcance por país y procedencia de indicadores

**What to build:** Entender si se observa una cotización, una simulación educativa o un perfil contrastado, con alcance explícito para Colombia y Argentina.

**Blocked by:** 06 — Distinguir tarjetas existentes, nuevas y cargos preexistentes

**Estado:** ready-for-agent.

- [ ] La interfaz distingue cotización transcrita, simulación bajo supuestos y perfil contrastado; fuente y fecha pueden completarse progresivamente, sin bloquear la aritmética disponible.
- [ ] CFT/VTUA publicados se guardan con nombre, unidad, base de impuestos y procedencia cuando se conocen. No se usan como interés, no alteran cuotas transcritas ni se recalculan sumando pagos.
- [ ] Cambiar país/producto revisa convenciones, cargos y perfil dependientes. Moneda no selecciona país y seleccionar país no acredita fidelidad contractual.
- [ ] La matriz de cobertura identifica país, producto, moneda, convención, versión y límites. Hay casos verificables de cotización y simulación educativa para Colombia y Argentina, incluidos productos dentro del alcance.
- [ ] Solo se etiqueta un perfil como contrastado si existe cronograma completo independiente, referencia fechada, comprobación de cargos/reglas y discrepancias documentadas. Primera cuota, tasa o indicador aislado no habilitan esa etiqueta.
- [ ] Se prueban rechazo de CFT/VTUA como tasa, falta de procedencia, cambio de país/producto y bloqueo de una etiqueta contractual sin evidencia suficiente. La ausencia de perfiles contrastados permanece explícita.
- [ ] No se automatizan impuestos, no se introducen porcentajes universales argentinos y no se activa prepago. Las fuentes requieren comprobar aplicabilidad/vigencia antes de liberar cualquier perfil contractual; el pendiente de precancelación de la herramienta anterior conserva su alcance separado.
- [ ] Guardar/retomar conserva procedencia, indicadores y versión de reglas, sin promover estimaciones a cotizaciones confirmadas.

### 10 — Verificar el flujo completo en teléfono, Android y Web

**What to build:** Completar y entender la comparación de principio a fin en las plataformas soportadas, con pantalla compacta, teclado y alternativa textual al gráfico.

**Blocked by:** 07 — Relacionar ofertas con el margen mensual personal; 08 — Explorar cambios sin perder la cotización original; 09 — Mostrar alcance por país y procedencia de indicadores

**Estado:** ready-for-agent.

- [ ] En Android y Web se ejecutan flujos representativos de cotización y simulación: dos ofertas, edición/aplicación, gráfico y tabla, contexto opcional, guardado de pendientes y recuperación.
- [ ] En teléfono compacto se puede introducir y revisar información sin recortes ni controles amontonados. Las ofertas pueden apilarse conservando escala, meses y selección común.
- [ ] Teclado y lector de pantalla pueden acceder a controles, errores y detalle mensual; se comprueban foco, etiquetas y lectura equivalente sin depender de color o del gráfico.
- [ ] Se añaden pocas pruebas de pantalla para lo no cubierto por el espacio de trabajo. Las pruebas financieras y de persistencia se mantienen en cada entrega, sin duplicarlas indiscriminadamente aquí.
- [ ] Se comprueba que documentos antiguos y herramienta de préstamos mantienen su flujo, y se ejecutan las comprobaciones vigentes de shared y los builds pertinentes de Android/Web. Se registra evidencia y limitaciones reales.
- [ ] Una persona de la audiencia explica aporte, cuota/rango, duración, costo nominal y datos faltantes después de usar escenarios. Se registra una observación real; si falta participante, ese criterio queda pendiente y no se declara aceptación completa.
- [ ] El cierre distingue verificación técnica, integración y aceptación de uso conforme al protocolo del proyecto. No implica despliegue ni certificación contractual de perfiles sin contraste.

## Trazabilidad y revisión

| Ticket | Historias de la especificación cubiertas |
| --- | --- |
| 01 | 1, 2, 3, 4, 11, 12, 13, 17, 18, 19, 20, 21, 22, 24, 26, 27, 28, 31, 32, 34 |
| 02 | 37, 38, 39, 40, 41, 42 |
| 03 | 7, 14, 15, 16, 25 |
| 04 | 7, 19, 23, 25, 26 |
| 05 | 5, 6, 8, 23, 26 |
| 06 | 9, 10, 36 |
| 07 | 33, 34, 35, 36 |
| 08 | 20, 29, 30, 31, 32, 38, 47 |
| 09 | 2, 15, 45, 46, 47 |
| 10 | 43, 44, 48 |

Las 48 historias tienen al menos una entrega responsable. Las referencias F01–F06 conservan sus expectativas literales; no se generan con el motor bajo prueba. Los casos de validación, tiempo, desconocidos y conservación de estado se distribuyen en las entregas que introducen ese comportamiento.

Antes de publicar, revisar con el usuario:
- ¿El tamaño de las entregas es adecuado o hay alguna demasiado grande/pequeña?
- ¿Los bloqueos son necesarios y correctos?
- ¿Conviene unir o dividir alguna entrega?

Después de aprobar, publicar cada ticket individual en orden de dependencias con el estado ready-for-agent, sin modificar la especificación padre. La aceptación con una persona y el contraste contractual se registran como evidencia real, nunca como pruebas ya realizadas por esta propuesta.
