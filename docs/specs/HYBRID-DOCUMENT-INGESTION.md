# Especificación: Pipeline Híbrido de Ingesta y Análisis de Extractos Bancarios (PDF en Edge)

**Estado:** Aprobado tras entrevista `/grill-me` (2026-10-12)  
**Alcance:** Ingesta y extracción local de condiciones de préstamos y filas de extractos bancarios en Android y Web (Kotlin Multiplatform), sin depender de plantillas fijas de bancos ni servicios en la nube. La visión con Gemma se ofrece en Android; Web conserva el análisis local de texto pegado y declara la ruta PDF visual no disponible hasta su implementación propia.

---

## 1. Resumen Ejecutivo y Objetivos

El módulo de ingesta de documentos permite a los usuarios de **finsim** cargar extractos bancarios en formato PDF para:
1. **Inicializar un Nuevo Escenario:** Extraer capital inicial o saldo actual, plazo, tasa de interés (EA/EM/Nominal) y seguros para crear o reconfigurar un préstamo sin digitación manual.
2. **Auditar un Mes en Conciliación:** Extraer los cobros efectivos de un mes (capital pagado, intereses cobrados, seguros facturados, saldo final) para contrastarlos automáticamente contra la cuota teórica simulada.

### Principios de Diseño
* **Cero plantillas fijas:** No se mantienen expresiones regulares ni coordenadas fijas por banco. El motor debe ser inmune al rediseño visual de los extractos.
* **100% Privado en Edge (Sin BYOK ni nube):** El procesamiento ocurre estrictamente en el dispositivo del usuario. Ningún byte o dato bancario sale de la máquina.
* **Preservación del tamaño base (~25 MB):** La app base no incluye pesos de modelos pesados. Los pesos de IA visual son un módulo opcional descargable bajo demanda.
* **Humano en el bucle (Human-in-the-loop):** Todo dato extraído debe pasar por un modal de confirmación con campos editables antes de mutar el estado financiero ([`WorkspaceStore`](file:///C:/Users/Lenovo/Documents/finsim/kmp/shared/src/commonMain/kotlin/com/finsim/workspace/WorkspaceStore.kt)).

---

## 2. Decisiones Arquitectónicas Acordadas (`/grill-me`)

| Dimensión | Decisión Acordada | Razón / Impacto |
| :--- | :--- | :--- |
| **Propósito de entrada** | **Doble propósito:** Crear nuevo escenario u operar sobre pestaña de conciliación. | Maximiza la utilidad tanto en onboarding inicial como en auditoría mensual recurrente. |
| **Marco regulatorio / País** | **Selector explícito previo en modal de carga.** | Evita adivinanzas heurísticas. Fija moneda por defecto, orden de imputación y reglas de prepago (ej. Ley 546 en Colombia) antes del análisis. |
| **Entradas y niveles** | El texto pegado se procesa con Tier 1. El PDF es una entrada visual separada; en Android se rasteriza y analiza con Gemma después de una acción explícita. | Evita tratar bytes PDF como texto y evita la descarga del modelo para texto manual suficiente. |
| **Control de hardware (RAM)** | Tier 2 requiere Android, al menos 8 GiB de RAM total y ausencia de presión actual (`ActivityManager.MemoryInfo.lowMemory`). | Separa el umbral de RAM total de la presión disponible y conserva el camino manual. |
| **Páginas** | El texto pegado puede segmentarse y Tier 1 elige su página con mayor densidad. El análisis PDF actual procesa explícitamente la página 1 a 150 DPI. | La UI indica la página analizada y no afirma extracción del documento completo. |
| **Revisión de resultados** | **Modal / Bottom Sheet interactivo con campos editables.** | Ningún dato sobreescribe el modelo en silencio. El usuario verifica y ajusta los valores antes de confirmar. |
| **Almacenamiento del modelo** | Persistente en el almacenamiento privado de Android; el artefacto está fijado a una revisión, nombre, tamaño de 2.583.085.056 bytes y SHA-256. | Permite uso offline y verifica descarga e integridad antes de la inferencia; la UI informa el tamaño de 2,58 GB y ofrece liberar espacio. |

---

## 3. Arquitectura del Pipeline en Cascada

```mermaid
flowchart TD
    User["Usuario elige texto pegado o PDF"] --> TextPath{"¿Texto pegado?"}
    TextPath -- "Sí" --> Tier1["Tier 1 local: analizar texto pegado y elegir su página más relevante"]
    Tier1 --> ReviewModal["Revisión editable de una página"]
    TextPath -- "PDF" --> AndroidCheck{"¿Android con soporte Gemma?"}
    AndroidCheck -- "No" --> ManualFallback["PDF visual no disponible; pegar texto para continuar"]
    AndroidCheck -- "Sí" --> HardwareCheck{"RAM total >= 8 GiB y lowMemory = false?"}
    HardwareCheck -- "No" --> ManualFallback
    HardwareCheck -- "Sí" --> ModelCheck{"¿Artefacto fijado y verificado?"}
    ModelCheck -- "No" --> DownloadPrompt["Descarga explícita del modelo (2,58 GB)"]
    DownloadPrompt -- "Cancelar" --> ManualFallback
    DownloadPrompt -- "Descarga íntegra y hash válidos" --> Rasterize
    ModelCheck -- "Sí" --> Rasterize["Rasterizar página 1 a 150 DPI, solo en memoria (PDF <= 25 MiB, <= 12 MP)"]
    Rasterize --> VLMInference["LiteRT-LM Android: imagen real, salida JSON determinista, speculative decoding apagado"]
    VLMInference --> ReviewModal
    ManualFallback --> ReviewModal
    ReviewModal -- "Confirmar e importar" --> Workspace["Actualizar Scenario o StatementRow"]
```

---

## 4. Contratos y Esquema de Extracción (Domain Contracts)

### 4.1 Esquema de Salida JSON Restringido
La inferencia de Tier 2 se ejecuta bajo una gramática JSON rígida (XGrammar / GBNF) que mapea directamente a los modelos de dominio de finsim:

```kotlin
package com.finsim.ingestion

import com.finsim.domain.Currency
import com.finsim.domain.Rate

/**
 * Resultado estructurado emitido por el pipeline de extracción.
 */
data class ExtractedDocumentData(
    val sourceEngine: ExtractionEngine,       // TIER_1_DIGITAL_TEXT o TIER_2_LOCAL_VLM
    val analyzedPage: Int,
    val bankNameCandidate: String?,
    val statementMonth: Int?,
    val closingBalance: String?,              // Cadena con formato Decimal
    val originalPrincipal: String?,
    val termMonths: Int?,
    val rate: ExtractedRate?,
    val monthlyPayment: String?,
    val insurances: List<ExtractedInsurance>,
    val confidence: Float                     // 0.0f a 1.0f
)

enum class ExtractionEngine {
    TIER_1_DIGITAL_TEXT,
    TIER_2_LOCAL_VLM
}

data class ExtractedRate(
    val valuePercent: String,                 // "18.5"
    val kind: String,                         // "annual", "monthly", "nominal"
    val nominalFrequency: Int?
)

data class ExtractedInsurance(
    val name: String,                         // "Seguro de vida", "Seguro deudores"
    val monthlyCost: String                   // "12450.00"
)
```

---

## 5. Módulos y Límites de Responsabilidad (KMP)

```
kmp/
├── shared/src/commonMain/kotlin/com/finsim/ingestion/
│   ├── HybridDocumentOrchestrator.kt         // Texto pegado, gate, página e invocación Tier 2
│   ├── DocumentIngestionRuntime.kt           // Dependencias por plataforma
│   ├── PageRasterizer.kt                     // Contrato y límites de memoria
│   ├── ExtractedDocumentData.kt              // Contratos revisables
│   └── VlmJsonGrammar.kt                     // Salida JSON estricta
├── shared/src/androidMain/kotlin/com/finsim/ingestion/
│   ├── PageRasterizer.android.kt             // PdfRenderer sobre descriptor de bytes en memoria
│   ├── AndroidModelDownloader.kt              // Descarga fijada y streaming
│   ├── AndroidModelStorage.kt                 // Archivo privado persistente y SHA-256
│   └── AndroidGemmaVisionRunner.kt             // LiteRT-LM 0.18 con imagen real y backend CPU
└── composeApp/src/{commonMain,androidMain,wasmJsMain}/.../ingestion/
    ├── DocumentImportDialog.kt               // Texto pegado y PDF como entradas separadas
    └── PdfDocumentPicker.*                    // SAF en Android; indisponibilidad honesta en Web
```

---

## 6. Fases de Implementación

1. **Fase 1: Contratos, Diálogo de Carga y Tier 1 (0 MB)**
   - Creación de contratos [`ExtractedDocumentData`](file:///C:/Users/Lenovo/Documents/finsim/kmp/shared/src/commonMain/kotlin/com/finsim/ingestion/ExtractedDocumentData.kt).
   - Diálogo de carga con selector de país/marco regulatorio ([`DocumentImportDialog`](file:///C:/Users/Lenovo/Documents/finsim/kmp/composeApp/src/commonMain/kotlin/com/finsim/app/ui/ingestion/DocumentImportDialog.kt)).
   - Tier 1 para texto pegado y selección de su página más relevante.
   - Selector SAF Android y rasterizador PDF nativo a 150 DPI sin archivos temporales; Web informa la indisponibilidad visual.
   - Modal de revisión final con campos editables antes de aplicar a [`WorkspaceStore`](file:///C:/Users/Lenovo/Documents/finsim/kmp/shared/src/commonMain/kotlin/com/finsim/workspace/WorkspaceStore.kt).

2. **Fase 2: Puerta de Hardware y Gestor de Descarga de Modelo**
   - Gate Android de RAM total >= 8 GiB y presión de memoria actual separada.
   - Descarga streaming bajo demanda de Gemma 4 E2B fijado (2.583.085.056 bytes) con validación de nombre, tamaño y SHA-256.
   - Opción explícita para eliminar el modelo local y liberar hasta 2,58 GB.

3. **Fase 3: Motor de Inferencia VLM Local (Tier 2)**
   - Integración real de LiteRT-LM en Android con entrada de imagen, backend CPU, sampler determinista y speculative decoding deshabilitado.
   - Decodificación restringida JSON para evitar formatos de salida ambiguos; los valores ausentes se conservan como ausentes.
   - Pruebas con extractos reales (digitales y escaneados) y verificación de temperatura/batería.
