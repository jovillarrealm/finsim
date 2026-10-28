# ADR 0003: Modelos visual-locales por plataforma

## Estado

Aceptada para la arquitectura de la ingesta visual. La verificación de inferencia real en Web sigue pendiente; consulta [`ISSUE-33`](../tasks/ISSUE-33.md).

## Fecha

2026-10-27

## Contexto

La ruta Android usa Gemma con LiteRT-LM. El objetivo Web oficial es Compose WasmJs y necesita aceptar páginas PDF e imágenes PNG/JPEG sin remitir documentos a un servicio externo. Los runtimes y límites de recursos disponibles en Android y Web son distintos, pero la extracción debe alimentar los mismos contratos de dominio, la misma revisión editable y las mismas reglas financieras.

Issue #33 requiere inferencia real local con Granite-Docling en Web, instalación explícita, artefactos fijados e íntegros, origen visible y revisión antes de aplicar. Issue #37 exige preservar el significado financiero de cada importe: una cuota total que incluye seguros no equivale a la cuota base del préstamo.

## Decisión

- Android conserva Gemma y su runtime LiteRT-LM.
- Web ejecuta Granite-Docling ONNX desde un Web Worker local incluido en la distribución WasmJs.
- Ambos adaptadores producen `ExtractedDocumentData` y pasan por la revisión humana común. La salida DocTags/OTSL de Granite-Docling se convierte mediante `DoclingDocTagsParser`; no se presenta como JSON estructurado restringido.
- La Web instala explícitamente una revisión fijada del modelo, valida el tamaño y SHA-256 de todos los artefactos y mantiene los pesos en Cache Storage. La inferencia no puede descargar artefactos ni documentos de forma implícita. Las peticiones del Worker se limitan a recursos locales y a las URL del manifiesto para GET/HEAD sin cuerpo.
- Los bytes del documento permanecen en memoria. El PDF se procesa en su primera página a 150 DPI; PNG y JPEG se decodifican directamente, con límites de tamaño y píxeles.
- Cuota base, pago total con seguros y significado desconocido permanecen separados. Los conflictos y cantidades ambiguas se conservan como advertencias y requieren revisión.

## Consecuencias

- La app base no incluye los pesos de Granite-Docling; la descarga local es una decisión visible del usuario.
- Actualizar el modelo requiere cambiar revisión, manifiesto de tamaños/SHA-256 y tamaño informado en la interfaz.
- Los dos modelos pueden diferir en rendimiento y extracción. La capa común mantiene iguales la semántica financiera, los bloqueos y el flujo de revisión.
- Una compilación, instalación íntegra o prueba sintética no demuestra calidad de extracción. Se requiere evidencia de inferencia real y validación semántica antes de cerrar #33.

## Alternativas descartadas

- Usar un servicio de inferencia remoto: contradice el límite de privacidad para documentos bancarios.
- Forzar el mismo modelo en ambas plataformas: ignora las diferencias de runtimes y recursos y no es necesario para compartir los contratos de dominio.
- Tratar DocTags como una salida JSON restringida o asignar automáticamente cada cantidad a una cuota base: la salida real no garantiza esos significados.
