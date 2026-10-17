# FS-18 — Resultados copiables y gráficos del préstamo

Status: ready-for-agent
Triage Label: ready-for-agent
Fecha: 2026-10-14
Tracker: Markdown local, conforme al patrón existente del proyecto.
Alcance: cierre de diseño confirmado por el usuario; especificación solicitada mediante mattpocock-skills:to-spec.
Implementación: no iniciada; requiere resolver incorporación coordinada de KMP/FS-17.
Esta es la fuente canónica. La etiqueta no sustituye reserva, integración ni aceptación de producto.

## Problem Statement

Una persona que explora su préstamo no puede reutilizar fácilmente valores como la cuota base: los resultados se muestran como texto sin acción de copia. Necesita llevar importes a otro campo, una hoja de cálculo o una conversación sin transcribirlos ni eliminar manualmente símbolos monetarios.

Para entender qué cambian sus abonos debe alternar entre una curva de saldo y una vista de doble pista. La curva permite comparar capital pendiente, pero no explica qué parte de los pagos corresponde a capital, intereses, seguros o cargos. Doble pista contiene parte de ese desglose y ocupa otra vista. La persona necesita comparar original y modificado, limpiar la información visible y consultar meses concretos desde el teléfono, conservando edición y tabla.

## Solution

Una sola experiencia de préstamo con dos gráficos de líneas verticalmente alineados: saldo de capital arriba y componentes efectivamente pagados cada mes abajo. Ambos muestran original y modificado. No habrá barras ni alternancia entre tipos de gráfico.

Cada componente tiene un color consistente. Original utiliza trazo discontinuo y modificado continuo; etiquetas y detalle numérico complementan el color. Los filtros permiten ocultar escenarios o componentes sin alterar el préstamo. Los gráficos comparten mes seleccionado y, en teléfono, tramo temporal navegable. El detalle permite comparar valores exactos, copiar y acceder a edición del escenario modificado. La tabla sigue siendo una alternativa accesible.

Resumen, detalle mensual y tabla incorporan acciones de copia por valor. Los importes se pegan con dos decimales y coma decimal, sin moneda ni separadores de miles: 507,51. Se confirma éxito y se informa un fallo sin alterar el resultado.

## User Stories

1. Como persona que explora un préstamo, quiero copiar la cuota base desde el resumen, para reutilizarla sin transcribirla.
2. Como persona que consulta resultados, quiero copiar otros valores del resumen, para compartir importes, fechas y duración con precisión.
3. Como persona que inspecciona un mes, quiero copiar sus importes desde el detalle, para compararlos con mis registros.
4. Como persona que usa la tabla, quiero copiar sus valores individualmente, para reutilizar el dato que estoy leyendo.
5. Como persona que pega un importe en otro campo, quiero recibir un número sin moneda ni miles, para evitar limpieza manual.
6. Como persona que copia dinero, quiero conservar exactamente dos decimales, para no perder centavos ni introducir redondeos adicionales.
7. Como persona que usa teclado o teléfono, quiero activar Copiar mediante un control accesible, para no depender de seleccionar texto con precisión.
8. Como persona que copia, quiero saber si la operación tuvo éxito o falló, para no pegar inadvertidamente un contenido anterior.
9. Como persona que revisa el préstamo, quiero ver saldo y pagos en dos gráficos próximos, para relacionar la deuda pendiente con el dinero que aporto.
10. Como persona que compara, quiero ver original y modificado en ambos gráficos, para seguir el efecto de mis eventos sin cambiar de escenario.
11. Como persona que interpreta el gráfico, quiero distinguir escenarios mediante trazo y etiquetas además del color, para reconocer series coincidentes o similares.
12. Como persona que consulta el saldo, quiero ver el capital inicial antes del primer pago, para comprender desde dónde empieza la reducción.
13. Como persona que consulta un cierre mensual, quiero ver el capital que queda al final del mes, para no confundirlo con la deuda total ni con el pago del periodo.
14. Como persona que analiza pagos, quiero ver los componentes individuales de cada mes, para distinguir capital habitual, capital adicional, intereses, seguros y otros cargos.
15. Como persona que aporta un abono o recuperación, quiero ver su distribución real, para reconocer cuánto cubrió capital y cuánto atendió otros conceptos.
16. Como persona que ofrece más dinero del necesario, quiero que el excedente no aplicado se mantenga separado, para no atribuirle una salida de caja inexistente.
17. Como persona que omite una cuota, quiero distinguir cargos generados y dinero pagado, para entender por qué puede haber intereses pendientes con pago cero.
18. Como persona con cargos de precancelación calculados, quiero ver comisión e IVA dentro de otros cargos, para comprender toda la caja pagada.
19. Como persona con seguros posteriores a liquidación, quiero seguir viendo esos meses, para no creer que todos los pagos terminan al liquidar capital.
20. Como persona con una simulación incompleta, quiero reconocer su límite y saldo pendiente, para no interpretar una liquidación o ahorro que no se alcanzó.
21. Como persona que quiere concentrarse en una comparación, quiero ocultar cualquiera de los escenarios en ambos gráficos, para reducir información visible.
22. Como persona que analiza intereses, quiero ocultar los demás componentes inferiores, para apreciar su evolución con una escala legible.
23. Como persona que filtra, quiero conservar los importes de las series restantes y el total real del mes, para no confundir visibilidad con ahorro.
24. Como persona que vuelve a un tramo anterior, quiero conservar los mismos componentes disponibles, para no perder una categoría que allí tiene valor cero.
25. Como persona que oculta todas las series, quiero un estado explícito y una acción Restablecer, para recuperar la vista sin interpretar ceros ficticios.
26. Como persona que consulta un préstamo largo en teléfono, quiero navegar por tramos mensuales legibles, para seleccionar meses sin comprimir todo el plazo.
27. Como persona que navega, quiero que ambos gráficos muestren los mismos meses, para comparar saldo y pagos del mismo periodo.
28. Como persona que selecciona un punto en cualquiera de los gráficos, quiero consultar el mismo mes en el detalle, para entender sus valores exactos.
29. Como persona que quiere modificar el préstamo, quiero editar eventos del mes seleccionado, para explorar cambios conservando el original.
30. Como persona que prepara varios eventos, quiero que los gráficos mantengan el último resultado aplicado hasta aplicar el borrador, para distinguir edición pendiente de simulación vigente.
31. Como persona que usa teclado o lectura accesible, quiero disponer de controles etiquetados y tabla mensual, para acceder a información y acciones sin interpretar el dibujo.
32. Como persona que guarda un escenario, quiero que filtrar y navegar no cambie sus entradas ni eventos, para conservar mis datos financieros.
33. Como persona que compara saldos, quiero una diferencia nombrada con precisión, para no confundir menor capital pendiente con ahorro de intereses.
34. Como persona que usa la app, quiero una única experiencia de gráficos que conserve los recorridos útiles, para dejar de alternar con doble pista.

## Implementation Decisions

### Alcance y módulos

- La implementación modifica presentación y coordinación de la experiencia existente de préstamos: resumen, gráficos, detalle mensual, tabla y acciones de copia. Reutiliza WorkspaceStore, el editor de eventos y los resultados LoanResult/MonthRow.
- Respeta ADR 0001: el motor KMP compartido es canónico. No cambia fórmulas, prelación, precisión, reglas regionales ni el motor TypeScript congelado. No introduce otro motor, otra capa de persistencia ni nuevas dependencias para dibujar o copiar.
- No requiere cambios al documento de préstamo, su versión, exportación/importación ni claves de almacenamiento. Los resultados siguen recalculándose desde entradas.
- La UI actual de referencia es KMP en FS-17, entrega f49a64f. La rama reservada para FS-18 contiene documentación; coordinar la incorporación de dependencias antes de implementar. No hacer merges entre ramas hermanas.

### Contrato de representación financiera

- Gráfico superior: saldo de capital al cierre del mes; mes 0 representa el capital inicial antes de cualquier pago. Saldo de capital no incluye intereses ni seguros pendientes.
- Gráfico inferior: cinco categorías individuales, sin apilar ni acumular: capital habitual, capital adicional, intereses pagados, seguros pagados y otros cargos pagados.
- Capital habitual procede de la asignación a capital del pago habitual. Capital adicional suma capital de recuperación y extra. Intereses y seguros pagados suman sus asignaciones habituales, de recuperación y extra. Otros cargos suman comisión e IVA efectivamente pagados; el detalle conserva ambos identificables.
- La suma de categorías debe coincidir exactamente con caja pagada del mes. Un extra ofrecido no se representa íntegramente como capital. Excedentes no aplicados se muestran aparte y no entran en caja.
- Agregados y subtotales se obtienen de importes monetarios registrados mediante aritmética decimal compartida. Las coordenadas del dibujo pueden usar representación gráfica aproximada; el detalle y la copia conservan el importe exacto de dos decimales.
- Distinguir pagos de cargos generados y pendientes. Una omisión puede dar pagos cero con intereses/seguros generados; una recuperación puede pagar cargos de meses anteriores. La tabla conserva sus conceptos explícitos y corrige cualquier etiqueta engañosa del dato de capital adicional al conectar esta experiencia.
- Original y modificado utilizan el horizonte común que incluye seguros posteriores. Tras una liquidación confirmada se puede representar saldo cero y pagos cero donde realmente no existan obligaciones; no rellenar resultados incompletos con liquidación ficticia ni extrapolar más allá del límite calculado.
- Las diferencias entre saldos se llaman Diferencia de saldo. No usar Capital Ahorrado ni Deuda evitada para ese dato. Conservar las diferencias de intereses, seguros, caja y plazo existentes con su signo y límite de completitud.

### Líneas y visibilidad

- Ambos gráficos son de líneas con puntos mensuales y conexiones rectas. Las conexiones expresan evolución entre observaciones mensuales; no simulan importes diarios. No habrá barras, áreas apiladas ni selector de tipo de gráfico.
- Mantener un color por componente inferior entre escenarios; original discontinuo y modificado continuo en ambos gráficos. Las etiquetas identifican componente y escenario; el detalle permite distinguir series superpuestas.
- Al abrir la sesión se activan ambos escenarios y todas las categorías con algún pago no cero en cualquiera de los escenarios a lo largo del horizonte calculado. Las categorías totalmente inexistentes no añaden controles. Disponibilidad no depende del tramo visible ni de un escenario oculto.
- Toggles Original/Modificado compartidos por ambos gráficos. Toggles de componente afectan solo al gráfico inferior. Conservar las elecciones de la sesión al navegar y aplicar eventos; si aparece una categoría nueva, comienza activa. No guardar filtros dentro del documento financiero.
- Ocultar una serie no cambia valores, selección, eventos ni resultados. Las líneas restantes mantienen sus importes. Cada gráfico tiene eje monetario propio, etiquetado y con base cero; los dos escenarios comparten escala dentro de ese gráfico. La escala inferior se ajusta a series visibles para permitir inspección de importes pequeños.
- El detalle mantiene ambos escenarios y sus totales reales aunque estén ocultos. Con filtros activos distingue Importe visible de Total pagado, por escenario. Si no hay componentes visibles, indicar ausencia de series en vez de presentar ese subtotal como un pago real cero.
- Si un gráfico queda sin series, mostrar Sin series visibles y Restablecer. Restablecer recupera escenarios y categorías disponibles inicialmente; no aplica eventos ni escribe almacenamiento. Un mes de pago genuinamente cero se distingue de un gráfico oculto.

### Tiempo, selección y edición

- Ambos gráficos comparten índices de mes, rango visible y mes seleccionado. Saldo es existencia al cierre; componentes son movimientos de ese mismo mes. Un gráfico no usa la escala monetaria del otro.
- En teléfono se navega por tramos de hasta 12 meses de pagos mediante Anterior/Siguiente, con límites y rango identificados. El primer tramo añade el punto inicial M0; no cuenta como cuota. Los dos gráficos siguen el mismo rango. En escritorio la presentación puede aprovechar el ancho conservando selección y alineación temporal; no se añade zoom en esta entrega.
- El mes 0 muestra solo estado inicial, sin pago ni edición de eventos. Los meses desde 1 permiten seleccionar desde cualquiera de los gráficos, desde un control mensual accesible y desde la tabla. Seleccionar desde tabla/control lleva el tramo a ese mes.
- Seleccionar muestra un detalle común con original/modificado, distribución completa, total real, componentes visibles cuando corresponda y cargos pendientes identificados aparte. La interacción toca/selecciona primero; editar eventos corresponde al modificado mediante el editor existente.
- La navegación no descarta borradores. Durante edición pendiente, gráficos, resumen y detalle siguen mostrando el último escenario aplicado. Aplicar o descartar conserva las reglas existentes; filtrar no activa una aplicación implícita. Al reducirse el horizonte tras aplicar, selección/rango quedan dentro de los meses disponibles.
- Controles etiquetados, estado de toggles expuesto, foco visible y operación con teclado. La tabla ofrece la misma selección/detalle y acceso a edición del modificado. El dibujo no es la única vía de acceso a importes o acciones.

### Copia

- Añadir acción Copiar por valor de resultado en resumen, detalle mensual y tabla. Su etiqueta accesible identifica dato y, cuando corresponda, escenario/mes. No añadir copia a cada campo editable ni convertir resultados en entradas al tocarlos.
- Importes copiados: coma decimal, exactamente dos decimales, sin moneda, espacios ni separadores de miles; conservar signo cuando exista. Por ejemplo, COP 1.014,02 se copia como 1014,02; COP 507,51 como 507,51. Otros valores se copian como texto mostrado.
- Usar la capacidad de portapapeles de la plataforma mediante la integración Compose existente o el adaptador mínimo necesario. Confirmación breve de éxito y mensaje explícito de fallo; no anunciar éxito antes de completar la operación. Un fallo no altera datos ni selección.
- Copia accesible con teclado en Web y acción táctil en Android. No depender de mantener pulsado un Canvas ni de conceder permisos permanentes para una acción que la plataforma resuelva por gesto del usuario.

### Sustitución y aceptación

- Retirar doble pista y su selector al entregar el reemplazo, después de demostrar sus recorridos preservados. No mantener una vista adicional como opción indefinida.
- Se conservan editor, tabla, resumen, diferencias y acciones de persistencia existentes. Cierre de FS-18 requiere integración y comprobaciones según protocolo; no cierra automáticamente deuda KMP, FS-17 ni observación de audiencia anterior.

## Testing Decisions

### Superficie principal

- Verificar el comportamiento visible de la aplicación de préstamo, desde entradas o importación pública de un escenario hasta gráficos, selección, detalle, edición y copia real. Esta superficie alta reutiliza el flujo de WorkspaceStore y las adaptaciones de plataforma; no crear una API de pruebas ni acoplar tests al árbol interno de dibujo.
- El cierre de diseño confirmado incluía selección/edición desde ambos gráficos y tabla, filtros no destructivos y copia real Android/Web. Se mantienen esas expectativas para las pruebas; no se abre otra entrevista sobre mecanismos internos.
- Referentes existentes: recorridos Playwright de UI compacta y teclado de la app KMP, compatibilidad JSON histórica y verificación Android con selector nativo y evidencia de UI. Para este alcance los controles deben estar realmente visibles y alcanzables; clic forzado no acredita ergonomía móvil.
- Las pruebas comunes del gráfico actual cubren ticks, no composición ni portapapeles. Reutilizarlas donde sigan siendo pertinentes. Solo añadir una prueba de transformación pura si la representación requiere lógica decimal no trivial; no repartir un caso de usuario entre muchos mocks o capas artificiales.
- Probar gráficos, resumen, detalle y tabla como vistas coherentes del mismo resultado. Evidencia visual confirma líneas/trazos/leyendas; verificaciones numéricas leen importes accesibles, sin inferir centavos a partir de píxeles.

### Casos de aceptación

| ID | Recorrido | Evidencia requerida |
| --- | --- | --- |
| A01 | Abrir préstamo sin eventos | Dos gráficos de líneas con ambos escenarios, leyendas inequívocas y sin barras ni selector doble pista. Escenarios iguales pueden superponerse sin perder identificación en detalle. |
| A02 | Ver préstamo con interés | F01 coincide en saldo, capital, interés y caja de gráficos/detalle/tabla; copiar cuota devuelve exactamente 507,51. |
| A03 | Aplicar un abono | Capital adicional y sus efectos aparecen en ambos gráficos/detalle al aplicar; sin recalcular mientras sea borrador. La suma pagada incluye todo lo aplicado, sin excedente. |
| A04 | Omitir y recuperar | F02 distingue cargos y pagos; recuperación conserva desglose exacto y saldo. Ninguna vista trata interés generado como pagado. |
| A05 | Filtrar componentes/escenarios | Importes restantes y documento guardado no cambian; escala/leyendas responden, detalle conserva ambos totales y diferencia importe visible de total real. |
| A06 | Ocultar todo y restablecer | Estado explícito, recuperación de series y ninguna falsa liquidación/pago cero. |
| A07 | Navegar plazo largo en 360 px | Tramos legibles de hasta 12 meses, límites correctos hasta 1.200, ambos gráficos sincronizados; controles visibles y accesibles. Selección externa lleva al tramo correcto. |
| A08 | Seleccionar/editar mes | Desde ambos gráficos, tabla y teclado se obtiene el mismo detalle y editor del modificado. M0 informa saldo inicial y no admite eventos. |
| A09 | Copiar en tres superficies | Resumen, detalle y tabla escriben el portapapeles real; formato, signo, éxito y error correctos, también Android. Un pegado/lectura real acredita contenido, no solo el aviso. |
| A10 | Redondeo/seguro posterior | F03–F05 conservan centavos, excedentes y meses de seguros sin inventar cuota ni truncar horizonte. |
| A11 | Cargos y resultado incompleto | F06 conserva comisión/IVA en caja. Resultado incompleto conserva saldo/pendientes y su advertencia; no dibuja liquidación inexistente ni ahorro definitivo. |
| A12 | Preservar estado/compatibilidad | Filtros, navegación y copia no cambian entradas/eventos ni JSON; guardar/importar/exportar/recargar siguen funcionando. Prueba representativa del documento histórico con abono. |
| A13 | Android/Web y accesibilidad | Compilan ambas plataformas; recorrido táctil y de teclado comprobado, controles/valores etiquetados y tabla operable. Registrar lector real y límites de dispositivo sin simular evidencia. |

### Referencias numéricas independientes

Los importes siguientes proceden de convenciones acordadas o de una fila sintética explícita; las expectativas no se generan mediante el motor bajo prueba. Notación visual con coma decimal.

| Caso | Entradas o dato suministrado | Valores esperados |
| --- | --- | --- |
| F01 | Capital 1000,00; EM 1 %; 2 meses; sin seguro/eventos | M0 saldo 1000,00. M1 capital habitual 497,51, interés 10,00, caja 507,51, saldo 502,49. M2 capital 502,49, interés 5,02, caja 507,51, saldo 0,00. Interés total 15,02; caja 1015,02. |
| F02 | Capital 1000,00; EM 1 %; cuota base 100,00; seguro fijo 5,00; omitir M1 y recuperación 105,00 en M2 | M1 pago 0,00, interés generado 10,00 y seguro generado 5,00, saldo 1000,00. M2 capital habitual 75,00, capital adicional 105,00, intereses pagados 20,00, seguros pagados 10,00, caja 210,00, saldo 820,00. |
| F03 | Capital 1000,00; tasa cero; seguro mensual 0,1 %; cuota base 100,00; extra ofrecido 1000,00 en M1 | Capital habitual 100,00, adicional 900,00, seguro 1,00, interés 0,00, caja 1001,00, saldo 0,00. Extra no aplicado 100,00, fuera de la suma pagada. |
| F04 | Capital 100,00; tasa cero; 3 meses | Pagos/capital 33,33; 33,33; 33,34. Suma 100,00, saldo final cero, sin M4 espurio. |
| F05 | Capital 100,00; tasa cero; cuota 100,00; seguro fijo 10,00 que continúa hasta M3 | Capital liquidado M1; caja 110,00; 10,00; 10,00. Saldo cero M1–M3, seguro visible hasta M3, caja total 130,00. |
| F06 | Fila sintética de representación: capital habitual 100,00; adicional 100,00; interés pagado 10,00; seguro pagado 5,00; comisión 3,00; IVA 0,63 | Otros cargos 3,63; cinco categorías suman caja 218,63. Es fixture de representación, no certificación de condiciones contractuales. |

- Con F01 M1 y filtro que oculta intereses: Importe visible 497,51 y Total pagado 507,51 por escenario correspondiente. La cuota real no se modifica.
- Añadir caso de resultado incompleto del motor existente y de horizonte largo; comprobar advertencia y ausencia de relleno falso sin inventar una nueva regla financiera.
- Verificar portapapeles con 0,00, 507,51, 1014,02 y una diferencia negativa existente. Copiar conserva dos decimales, signo y dato de origen. Un fallo de plataforma debe mostrarse como fallo.
- Ejecutar comprobaciones pertinentes de UI, regresiones de préstamos/persistencia y builds Android/Web. Solo ampliar pruebas de motor si cambia código financiero, algo fuera del alcance previsto.
- En esta sesión se valida la especificación y su aritmética de referencia. No se afirma que estos criterios ya estén implementados o pasen en producto.
- Verificación documental ejecutada: siete secciones requeridas, 34 historias, criterios A01–A13, referencias F01–F06, enlaces locales y 17 igualdades independientes en centavos correctos; sin rutas concretas en Implementation Decisions. No se ejecutaron suites de producto por esta entrega documental.

## Out of Scope

- Barras, áreas apiladas, doble pista como alternativa permanente, más de dos escenarios o gráficos de pagos acumulados.
- Cálculos diarios, tasas nuevas, cambios de prelación, reglas regionales o fórmulas para adaptar el dibujo.
- Rediseño del comparador de ofertas independiente, conciliación, ingesta de extractos, conversor o motor TypeScript.
- Nuevos contratos JSON, sincronización, cuentas, almacenamiento de preferencias visuales o migraciones.
- Exportación masiva de tabla, copiar filas completas/CSV, controles Copiar en cada input o exigencia nueva de selección libre de texto.
- Control adicional de zoom, nueva librería de gráficos, telemetría o backend.
- Publicar la aplicación, certificar fidelidad bancaria/legal o dar por cerrados pendientes anteriores de KMP/FS-17/audiencia.

## Further Notes

- El usuario confirmó Q10 y solicitó to-spec. Las alternativas anteriores de barras quedan descartadas; esta especificación sustituye el borrador de entrevista.
- La fuente de reglas monetarias es [financial-conventions](../../docs/financial-conventions.md); vocabulario acordado en [CONTEXT](../../CONTEXT.md). ADR 0001 y glosario de préstamos se leyeron en la rama FS-17 de referencia; su incorporación al conjunto está pendiente de coordinación.
- Este acuerdo sustituye, para la experiencia de préstamos FS-18, la presentación histórica mediante barras del brief/FS-07. Conserva sus requisitos financieros, edición y accesibilidad. Actualizar esos documentos al integrar, evitando presentar como desplegado un cambio aún no implementado.
- Publicada como especificación ready-for-agent en tracker local. No se crea issue remoto ni se realiza despliegue. No descompone en tickets de implementación: esa acción requiere su flujo correspondiente.
- La [ficha FS-18](../../docs/tasks/FS-18.md) conserva responsable, rama, dependencias y siguiente paso; el [tablero](../../docs/tracking.md) se modifica solo por la coordinadora en el checkout de integración.
