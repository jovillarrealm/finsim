# 01: Ingesta de Escenario vía Texto Digital y Selector Regulatorio (Tier 1)

**What to build:** En la barra de herramientas principal de la aplicación, el usuario puede presionar `[Importar extracto]` para abrir un diálogo donde selecciona su país/regulación (`Colombia · SFC`, `México · CONDUSEF`, etc.) y carga un archivo PDF bancario. El motor lee el texto plano nativo sin costo de modelo (Tier 1: 0 MB), detecta la página del resumen por densidad de términos clave y extrae los campos iniciales del préstamo (capital, tasa con tipo EA/EM/Nominal, plazo en meses y desglose de seguros). Presenta una Hoja de Revisión interactiva con los valores detectados en campos editables. Al pulsar `[Confirmar e importar]`, se inicializa o actualiza el escenario en el WorkspaceStore con recálculo completo de cronograma y gráficos.

**Blocked by:** None (can start immediately)

**Status:** completed

- [x] Contratos de dominio en `shared`: `ExtractedDocumentData`, `ExtractedRate`, `ExtractedInsurance`, y `ExtractionEngine.TIER_1_DIGITAL_TEXT`.
- [x] Selector de marco regulatorio previo (`Colombia · SFC`, `México · CONDUSEF`, `Argentina · BCRA`, `Chile · CMF`, `Genérico`).
- [x] Lector y extractor de texto plano de PDF por página (`PdfRenderer` / extractor nativo en Android y canvas/pdf en Web).
- [x] Escáner de densidad de palabras clave (`PageDensityScanner`) que selecciona la página con mayor relevancia de términos clave financieros.
- [x] Mapeo de términos de texto plano a `Scenario` según la convención regulatoria seleccionada.
- [x] Hoja de revisión (`ExtractionReviewSheet`) con campos de texto editables, etiqueta de procedencia del dato y botón `[Confirmar e importar]`.
- [x] Integración con el botón `[Importar extracto]` en la barra de herramientas del escenario de `App.kt`.
- [x] Pruebas unitarias de parsing y flujo de extracción de texto plano pasando con `./gradlew :shared:jvmTest`.
