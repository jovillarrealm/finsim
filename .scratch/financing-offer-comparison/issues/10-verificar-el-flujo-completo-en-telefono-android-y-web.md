# 10 — Verificar el flujo completo en teléfono, Android y Web

**What to build:** Completar y entender la comparación de principio a fin en las plataformas soportadas, con pantalla compacta, teclado y alternativa textual al gráfico.

**Blocked by:** 07, 08, 09

**Status:** in-progress

- [ ] En Android y Web se ejecutan flujos representativos de cotización y simulación: dos ofertas, edición/aplicación, gráfico y tabla, contexto opcional, guardado de pendientes y recuperación.
- [ ] En teléfono compacto se puede introducir y revisar información sin recortes ni controles amontonados. Las ofertas pueden apilarse conservando escala, meses y selección común.
- [ ] Teclado y lector de pantalla pueden acceder a controles, errores y detalle mensual; se comprueban foco, etiquetas y lectura equivalente sin depender de color o del gráfico.
- [x] Se añaden pocas pruebas de pantalla para lo no cubierto por el espacio de trabajo. Las pruebas financieras y de persistencia se mantienen en cada entrega, sin duplicarlas indiscriminadamente aquí.
- [ ] Se comprueba que documentos antiguos y herramienta de préstamos mantienen su flujo, y se ejecutan las comprobaciones vigentes de shared y los builds pertinentes de Android/Web. Se registra evidencia y limitaciones reales.
- [ ] Una persona de la audiencia explica aporte, cuota/rango, duración, costo nominal y datos faltantes después de usar escenarios. Se registra una observación real; si falta participante, ese criterio queda pendiente y no se declara aceptación completa.
- [ ] El cierre distingue verificación técnica, integración y aceptación de uso conforme al protocolo del proyecto. No implica despliegue ni certificación contractual de perfiles sin contraste.


## Verificación retomada · 2026-10-14

122 pruebas pasan (115 shared + 7 UI); APK debug y Web producción actuales correctos en 66a78a0. Chrome DevTools: cotizaciones, edición/validación/aplicación, pendientes y recuperación, tabla/selección y fixtures F02/F06 comprobados. Emulación 360x800 revisada parcialmente. Persiste P1 de accesibilidad (campos sin nombre y árbol de menú cerrado); transferencia de archivos bloqueada por workspace roots de upload_file. Android de dispositivo y audiencia pendientes. Las casillas de cierre permanecen abiertas. Evidencia y límites: [informe](../../../docs/reviews/FS-17-browser-verification.md).


## Continuación — f8551b4

Corregidos nombres y árbol tras selección. Pasan cinco pruebas de navegador (cuatro de accesibilidad 1280/360 y transferencia real en ventana amplia), siete UI y builds Android/Web. Android: dos cotizaciones ingresadas/aplicadas/guardadas y recuperación A1200/B1120 tras reinicio. Revisión sin hallazgos nuevos. Se marca solo la incorporación de pruebas de pantalla; flujo compacto/lector completo, simulación y transferencia Android, documentos antiguos y audiencia siguen abiertos. Véase informe enlazado arriba.
