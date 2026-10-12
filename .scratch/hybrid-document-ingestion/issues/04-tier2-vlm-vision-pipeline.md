# 04: Pipeline de Visión Local con Decodificación Restringida (Tier 2 VLM)

**What to build:** Cuando un documento PDF bancario carece de capa de texto digital legible o contiene tablas complejas/escaneadas, el router en cascada activa el motor de visión local (Tier 2). Rasteriza la página con mayor relevancia visual a 150 DPI y la procesa en el dispositivo mediante LiteRT-LM (Android) o WebLLM (Web). Se aplica decodificación restringida mediante gramática JSON rígida (XGrammar) para asegurar que los valores numéricos y tasas salgan con la estructura exacta de `ExtractedDocumentData` sin alucinaciones de formato. Los resultados se canalizan a la Hoja de Revisión etiquetados como "Visión local Gemma 4", listos para aprobación del usuario.

**Blocked by:** 01: Ingesta de Escenario vía Texto Digital y Selector Regulatorio (Tier 1), 03: Puerta de Hardware y Gestor de Descarga del Modelo Local (Tier 2 Prep)

**Status:** completed

- [x] Integración del ejecutor VLM local (`LiteRtVlmRunner` en Android / `WebLlmVlmRunner` en Web).
- [x] Rasterizado de la página seleccionada a 150 DPI nativo (ARGB_8888, ~8.7 MB de memoria por página A4).
- [x] Definición de esquema JSON y gramática de logit masking (XGrammar / JSON Schema mode).
- [x] Orquestación en cascada: Tier 1 evalúa -> si faltan campos y el modelo está listo -> Tier 2 procesa la imagen.
- [x] Alimentación de los resultados de Tier 2 a la `ExtractionReviewSheet` con badge `"Visión local Gemma 4"`.
- [x] Verificación de estabilidad y pausas de enfriamiento térmico (500 ms) en documentos de múltiples páginas.
- [x] Pruebas unitarias de orquestación del pipeline y parsing de respuestas estructuradas pasando con `./gradlew :shared:jvmTest`.
