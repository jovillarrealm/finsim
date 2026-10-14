# 10 — Verificar el flujo completo en teléfono, Android y Web

**What to build:** Completar y entender la comparación de principio a fin en las plataformas soportadas, con pantalla compacta, teclado y alternativa textual al gráfico.

**Blocked by:** 07, 08, 09

**Status:** in-progress

- [x] En Android y Web se ejecutan flujos representativos de cotización y simulación: dos ofertas, edición/aplicación, gráfico y tabla, contexto opcional, guardado de pendientes y recuperación.
- [x] En teléfono compacto se puede introducir y revisar información sin recortes ni controles amontonados. Las ofertas pueden apilarse conservando escala, meses y selección común.
- [ ] Teclado y lector de pantalla pueden acceder a controles, errores y detalle mensual; se comprueban foco, etiquetas y lectura equivalente sin depender de color o del gráfico.
- [x] Se añaden pocas pruebas de pantalla para lo no cubierto por el espacio de trabajo. Las pruebas financieras y de persistencia se mantienen en cada entrega, sin duplicarlas indiscriminadamente aquí.
- [x] Se comprueba que documentos antiguos y herramienta de préstamos mantienen su flujo, y se ejecutan las comprobaciones vigentes de shared y los builds pertinentes de Android/Web. Se registra evidencia y limitaciones reales.
- [ ] Una persona de la audiencia explica aporte, cuota/rango, duración, costo nominal y datos faltantes después de usar escenarios. Se registra una observación real; si falta participante, ese criterio queda pendiente y no se declara aceptación completa.
- [ ] El cierre distingue verificación técnica, integración y aceptación de uso conforme al protocolo del proyecto. No implica despliegue ni certificación contractual de perfiles sin contraste.


## Verificación retomada · 2026-10-14

122 pruebas pasan (115 shared + 7 UI); APK debug y Web producción actuales correctos en 66a78a0. Chrome DevTools: cotizaciones, edición/validación/aplicación, pendientes y recuperación, tabla/selección y fixtures F02/F06 comprobados. Emulación 360x800 revisada parcialmente. Persiste P1 de accesibilidad (campos sin nombre y árbol de menú cerrado); transferencia de archivos bloqueada por workspace roots de upload_file. Android de dispositivo y audiencia pendientes. Las casillas de cierre permanecen abiertas. Evidencia y límites: [informe](../../../docs/reviews/FS-17-browser-verification.md).


## Continuación — f8551b4

Corregidos nombres y árbol tras selección. Pasan cinco pruebas de navegador (cuatro de accesibilidad 1280/360 y transferencia real en ventana amplia), siete UI y builds Android/Web. Android: dos cotizaciones ingresadas/aplicadas/guardadas y recuperación A1200/B1120 tras reinicio. Revisión sin hallazgos nuevos. Se marca solo la incorporación de pruebas de pantalla; flujo compacto/lector completo, simulación y transferencia Android, documentos antiguos y audiencia siguen abiertos. Véase informe enlazado arriba.

## Continuación de verificación — 49b13fe

Pasan juntas ocho pruebas de navegador y siete UI; APK y Wasm actuales correctos. El recorrido 360x800 ahora usa controles físicamente visibles y scroll, incluidos la tabla anidada, contexto, validación, aplicación, pendientes y recuperación. Tab/Espacio/Shift+Tab tienen una regresión red/green; queda pendiente todo el recorrido solo con teclado y lector real. Se conectó la transferencia de préstamos KMP reutilizando el selector JSON: fixture histórico Monedas, abono preservado, recálculo1014.02, exportación real y rechazo no destructivo verificados en Web.

Android: exportación/importación/cancelación por selector y rechazo de JSON inválido, versión99 y2MB preservanA1200/B1120. F06 importado se recorre mediante UI/tabla:507.51,497.51/10/502.49 y márgenes -407.51/-400. Guardar ingreso650 pendiente conserva aplicado600 al reiniciar; descartar vuelve a600. Variante33.33/33.33/33.34 con saldo cero en mes3. No se afirma entrada manual completa de simulación ni lector. Detalles, evidencia y límites en el informe enlazado. Reserva/estado in-progress conservados; no integración ni publicación.

Documento antiguo también importado/exportado por selector Android con APK49b13fe: Monedas conserva COP, abono100 y caja1014.02. Evidencia en la última continuación del informe; no se guardó sobre el préstamo anterior. Continúan abiertos lector, recorrido íntegro solo con teclado, audiencia y revisión/integración.

Recorrido representativo solo con teclado añadido: Tab/Espacio desde entrada, dos cotizaciones/contexto, guardado/aplicación, rechazo/corrección, flechas y tabla mensual con márgenes negativos; controles usados dentro del viewport. Nueve pruebas de navegador pasan, sin cambios adicionales de producto. Se marcan los criterios técnicos acreditados; lector, audiencia y revisión/integración conservan el ticket abierto.
