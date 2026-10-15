# Comparador de financiación — cobertura y verificación

Fecha: 2026-10-14. Reglas: monthly-v1. Especificación canónica: [spec](../../.scratch/financing-offer-comparison/spec.md). Reserva y evidencia de ejecución: [FS-17](../tasks/FS-17.md).

| País seleccionado | Necesidad/producto | Monedas independientes | Cotización | Simulación educativa |
| --- | --- | --- | --- | --- |
| Colombia | Efectivo / préstamo personal | COP, ARS, USD, EUR | Pagos mensuales transcritos | Tasa fija EM, EA o nominal anual con frecuencia |
| Argentina | Efectivo / préstamo personal | COP, ARS, USD, EUR | Pagos mensuales transcritos | Tasa fija EM, EA o nominal anual con frecuencia |
| Colombia | Misma compra / préstamo personal, tarjeta existente o nueva | COP, ARS, USD, EUR | Cuotas uniformes y excepciones | Modelo mensual fijo compatible |
| Argentina | Misma compra / préstamo personal, tarjeta existente o nueva | COP, ARS, USD, EUR | Cuotas uniformes y excepciones | Modelo mensual fijo compatible |

La matriz indica capacidades educativas, no disponibilidad comercial o legal de cada combinación. Elegir moneda no selecciona país. No hay conversión monetaria. El calendario usa meses relativos al mes inicial declarado; sin fechas, la alineación desde mes 1 se identifica como estimada.

No se habilita ningún perfil contractual contrastado. La interfaz y el importador no aceptan una etiqueta de fidelidad contractual. Se necesita un cronograma completo independiente, fuentes fechadas, reglas, cargos, versión y discrepancias comprobadas antes de añadir uno. Los documentos oficiales enlazados en la especificación no se presentan como prueba de vigencia o reproducción de un contrato.

CFT y VTUA son indicadores publicados con valor, unidad, base de impuestos, fuente y fecha opcionales. No modifican el interés ni las cuotas, y no se calculan sumando pagos. No hay impuestos automáticos ni porcentajes predeterminados por país. Las reglas de precancelación de la herramienta anterior no se activan en este comparador.

Las simulaciones reutilizan el motor KMP: cuota base de capital e interés, seguros compatibles y ajuste final por redondeo. Un cargo marcado incluido se incorpora una vez al pago simulado; en cotización ya forma parte del pago ingresado. Cargos financiados/descontados forman parte del capital y no generan otra salida aparte. El contexto es mensual constante, opcional, y conserva márgenes negativos.

La prueba pública ComparisonWorkspaceTest cubre F01–F06, originales/aplicados/pendientes, cambios hipotéticos, validación no destructiva, fallos de almacenamiento, UTF-8, calendario compartido, cargos abiertos y compatibilidad por clave con préstamos anteriores. La matriz de países/productos/monedas prueba cotización y simulación separadas. Las pruebas de pantalla y resultados de builds se registran en FS-17; las pruebas técnicas no sustituyen aceptación de uso.

Pendiente de aceptación: una persona de la audiencia debe explicar aporte, cuota/rango, duración, costo nominal y datos faltantes sin ayuda. No se simula esa observación ni se despliega automáticamente.
