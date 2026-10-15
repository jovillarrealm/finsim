# 03: Puerta de Hardware y Gestor de Descarga del Modelo Local (Tier 2 Prep)

**What to build:** En dispositivos con menos de 6 GB de RAM (o sin soporte de WebGPU en la web), la aplicación muestra una advertencia preventiva y restringe la activación del VLM de visión profunda para evitar cierres forzosos por el Low Memory Killer (LMKD). En dispositivos elegibles (>=6 GB de RAM), cuando Tier 1 no encuentra texto plano suficiente, ofrece un diálogo para descargar bajo demanda el modelo local Gemma 4 E2B INT4 (~1.1 GB) con barra de progreso en tiempo real y almacenamiento en el disco del dispositivo. Adicionalmente, añade en la interfaz de ajustes un botón de `[Liberar espacio: Eliminar modelo de IA local]` que purga los archivos y recupera el almacenamiento al instante.

**Blocked by:** 01: Ingesta de Escenario vía Texto Digital y Selector Regulatorio (Tier 1)

**Status:** completed

- [x] Contrato expect/actual `DeviceCapabilityGate` para inspección de memoria del sistema (`ActivityManager.MemoryInfo` en Android, `navigator.deviceMemory` / `navigator.gpu` en Web).
- [x] Diálogo de aviso de capacidad de hardware restrictivo cuando RAM < 6 GB.
- [x] Gestor de descarga bajo demanda con reporte de porcentaje y bytes descargados en `ModelDownloadProgressCard`.
- [x] Almacenamiento local persistente del checkpoint del modelo en el directorio de archivos de la aplicación.
- [x] Botón de gestión de almacenamiento `[Liberar espacio: Eliminar modelo local (~1.1 GB)]` con modal de confirmación y eliminación física del archivo.
- [x] Pruebas unitarias de la lógica de evaluación de hardware y ciclo de vida de almacenamiento.
