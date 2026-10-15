# 02 — Guardar y recuperar comparaciones incompletas

**What to build:** Guardar, exportar, importar y retomar un borrador con datos incompletos sin perder trabajo ni afectar archivos de préstamos anteriores.

**Blocked by:** 01

**Status:** in-progress

- [ ] Guardado y exportación explícitos conservan entradas aplicadas, borrador pendiente, ausencias, estimaciones y procedencia disponibles; la interfaz identifica trabajo sin guardar.
- [ ] Al retomar se recalculan resultados solo desde las entradas aplicadas. Los pendientes siguen pendientes; un borrador sin aplicación previa no produce resultados inventados. Una entrada temporal inválida no reemplaza datos válidos guardados.
- [ ] El documento tiene tipo, versión inicial y clave de almacenamiento propios. Los documentos anteriores siguen abriéndose en la herramienta de préstamos, con orientación clara desde el importador, sin convertir ni perder eventos.
- [ ] Antes de sustituir estado se validan discriminante, versión, tipos, valores, identidades y referencias, además del límite de 2 MB medido en bytes UTF-8 reales. El dinero se serializa como cadenas y no se persisten resultados derivados.
- [ ] Importación inválida, versión no soportada y fallos de lectura/escritura/cuota muestran error y conservan memoria y último guardado. No se sobrescribe automáticamente información corrupta con ejemplos.
- [ ] Pruebas del espacio de trabajo verifican ida y vuelta con una oferta incompleta, pendientes sin aplicar, rechazo no destructivo, contenido no ASCII en el límite de tamaño y compatibilidad con documentos anteriores. El flujo funciona localmente sin cuenta ni envío de datos.
