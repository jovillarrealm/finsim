# State of the Art (SOTA) 2026: On-Device Document Analysis on Edge Devices (Mobile PDF Intelligence)

Research date: 2026-10-12. Scope: Autonomous, local document analysis (PDF/image ingestion, layout understanding, and structured extraction) executed entirely on edge devices (Android, iOS, mobile Web). This report evaluates modern Small Vision-Language Models (VLMs), on-device runtimes, hardware acceleration backends (NPU/GPU/TPU), end-to-end ingestion pipelines, and systems-level engineering trade-offs (memory pressure, thermal limits, constrained decoding, and distribution).

---

## 1. Executive Summary & Architectural Taxonomy

Processing complex PDF documents on mobile devices has undergone a fundamental architectural transition between 2024 and 2026. The historical standard—a **two-stage pipeline** coupling optical character recognition (OCR) with a quantized text-only Small Language Model (SLM)—is rapidly being superseded by **"Vision-First" Vision-Language Models (VLMs)** and **hybrid cascaded pipelines**. 

While classic OCR flattens two-dimensional layout geometry, misorders multi-column text, and fails on forms, checkboxes, tables, and signatures, direct VLM ingestion interprets documents natively as visual artifacts. However, running multi-billion-parameter multimodal transformers on commodity mobile hardware exposes severe systems bottlenecks: memory starvation under the Android Low Memory Killer (LMKD), memory bandwidth constraints during prefill, thermal throttling under sustained compute, and model distribution thresholds in app stores.

```
+---------------------------------------------------------------------------------------------------+
|                                  ON-DEVICE DOCUMENT ANALYSIS STACK                                |
+---------------------------------------------------------------------------------------------------+
|  [Ingestion & Rendering]   Android PdfRenderer (API 35+) / PDFium C++ / pdf.js WebAssembly       |
|                            Dynamic DPI scaling (150-200 DPI) -> ARGB_8888 Page Bitmaps            |
+---------------------------------------------------------------------------------------------------+
|  [Triage & Router]         Heuristic Classifier: Native Text Density vs Visual Complexity         |
|                            (Simple text -> Fast OCR/Regex | Complex/Tables/Forms -> VLM Pipeline) |
+---------------------------------------------------------------------------------------------------+
|  [Edge VLM Backbone]       Google Gemma 4 (E2B/E4B) | Qwen2.5-VL-3B / Qwen3-VL | SmolVLM2 2.2B     |
|                            Phi-4-multimodal | EmbeddingGemma 2 (On-device Multimodal RAG)         |
+---------------------------------------------------------------------------------------------------+
|  [Quantization & Weights]  INT4-AWQ / GGUF Q4_K_M / INT4-W4A16 (Per-channel weight quantization)  |
+---------------------------------------------------------------------------------------------------+
|  [Inference Engine]        Google LiteRT (LiteRT-LM) | ExecuTorch (QNN/CoreML) | llama.cpp (GGUF) |
+---------------------------------------------------------------------------------------------------+
|  [Hardware Acceleration]   Qualcomm Hexagon NPU | Apple Neural Engine (ANE) | Google Tensor TPU   |
+---------------------------------------------------------------------------------------------------+
|  [Constrained Decoding]    XGrammar / Outlines DFA / GBNF Grammar Engine (Strict JSON Schema)     |
+---------------------------------------------------------------------------------------------------+
|  [Operating System]        Android 17 Per-App Limits / LMKD / iOS Jetsam Memory Supervision       |
+---------------------------------------------------------------------------------------------------+
```

---

## 2. Modern On-Device Vision-Language Models (VLMs) and SLMs

### 2.1 Google Gemma Family

Google DeepMind's open model family represents the bleeding edge of modular, hardware-optimized edge computing.

*   **Gemma 4 (E2B / E4B Modular-Native Edge Models):** Released in April 2026 under the Apache 2.0 license, Gemma 4 established the "Effective Parameter" (E) architecture. The **E2B** and **E4B** variants are purpose-built for local mobile and edge deployment. Rather than bolting a separate vision encoder onto an autoregressive decoder, Gemma 4 incorporates a unified, encoder-free multimodal design where vision and text tokens feed into a shared representation backbone. Gemma 4 edge variants introduce native **Multi-Token Prediction (MTP)** drafters, achieving a 2.2x inference speedup on mobile GPUs and 1.5x on mobile CPUs without output degradation. It natively supports a 256K context window.
    *   *Primary Sources:* [Google DeepMind Gemma 4 Release](https://blog.google/technology/developers/gemma-4-open-models/), [Gemma 4 Architecture Guide](https://ai.google.dev/gemma/docs/gemma-4).
*   **Gemma 3 & Gemma 3n:** Gemma 3 (early 2025) introduced 128K context and multilingual processing across 140+ languages. Its mobile-tailored spin-off, **Gemma 3n**, was co-engineered with mobile silicon vendors (Qualcomm, MediaTek, Samsung). Gemma 3n introduced **Per-Layer Embeddings (PLE)**, which decouple embedding storage from working RAM, enabling an effective 8B-capacity model (E4B) to operate within a physical 4B RAM footprint (~2.5 GB at INT4).
    *   *Primary Sources:* [Gemma 3 Technical Report](https://blog.google/technology/developers/gemma-3/), [Google AI Edge Gemma 3n Documentation](https://ai.google.dev/edge/gemma3n).
*   **EmbeddingGemma 2:** Released in October 2026, EmbeddingGemma 2 is a 740M parameter modular multimodal embedding model derived from the Gemma 4 architecture. It features a 270M parameter text/code backbone with an optional 170M vision encoder and 300M audio encoder. It maps document images, page crops, and text queries into a unified 768-dimensional vector space using **Matryoshka Representation Learning (MRL)**, permitting dynamic vector truncation to 128d, 256d, or 512d to conserve mobile storage and vector index RAM. This provides the primary foundation for on-device visual RAG over multi-page PDF documents.
    *   *Primary Sources:* [EmbeddingGemma 2 Launch](https://blog.google/technology/developers/embedding-gemma-2/), [EmbeddingGemma 2 Model Card](https://ai.google.dev/gemma/docs/embeddinggemma2).

### 2.2 Microsoft Phi Family

*   **Phi-4-Multimodal:** Released by Microsoft Research in early 2025 (arXiv:2503.01743), Phi-4-Multimodal scales compact multimodality to 5.6 billion parameters. It unifies text, vision, and speech processing in a single checkpoint using a **Mixture-of-LoRAs (MoLoRA)** architecture. In this design, modality-specific Low-Rank Adaptation (LoRA) experts are dynamically routed while sharing a frozen dense core, minimizing activation memory during inference.
    *   *Primary Sources:* [Phi-4-Mini & Multimodal Technical Report (arXiv:2503.01743)](https://arxiv.org/abs/2503.01743), [Microsoft Phi Cookbook](https://aka.ms/Phicookbook).
*   **Phi-4-mini:** A 3.8B parameter dense language model trained extensively on synthetic textbook-quality data. It features a 200K token vocabulary and state-of-the-art multi-step reasoning capabilities. When deployed in a two-stage edge document pipeline (OCR text piped into SLM), Phi-4-mini-instruct delivers near-frontier reasoning on complex financial and legal extraction tasks.
    *   *Primary Sources:* [Phi-4-Mini Model Card (Hugging Face)](https://huggingface.co/microsoft/Phi-4-mini-instruct).

### 2.3 Alibaba Qwen Family

Alibaba Cloud's Qwen vision models set the empirical benchmark for document and layout extraction due to dynamic resolution handling.

*   **Qwen2.5-VL (arXiv:2502.13923):** Built with a native dynamic-resolution Vision Transformer (ViT) that replaces fixed square image resizing with aspect-ratio-preserving patches (grouped in multiples of 28). To eliminate the quadratic cost ($O(N^2)$) of high-resolution document processing, Qwen2.5-VL implements **Window Attention** (8x8 window), bounding attention computation linearly to patch count. It integrates **Multimodal Rotary Position Embeddings (mRoPE)** to encode 2D spatial document coordinates alongside 1D sequence IDs. The **Qwen2.5-VL-3B-Instruct** variant is the primary open baseline for on-device document extraction, natively producing structured **QwenVL HTML/JSON** formats for complex tables, checkmarks, and forms.
    *   *Primary Sources:* [Qwen2.5-VL Technical Report (arXiv:2502.13923)](https://arxiv.org/abs/2502.13923), [Qwen2.5-VL GitHub Repository](https://github.com/QwenLM/Qwen2.5-VL).
*   **Qwen3-VL (September 2025):** Evolves the dynamic-resolution ViT to native multiples of 32 (16x16 patch alignment) and introduces **DeepStack**, which fuses multi-level intermediate ViT layer features directly into the language model. This resolves fine-grained character recognition issues in small-font footnotes and complex financial balance sheets. Qwen3-VL natively supports 256K context tokens (extensible to 1M).
    *   *Primary Sources:* [Qwen3-VL Announcement & Specs](https://qwen.ai/blog/qwen3-vl), [Qwen Official Models](https://github.com/QwenLM/Qwen).

### 2.4 Specialized Edge Architectures

*   **SmolVLM & SmolVLM2 (Hugging Face):** SmolVLM (256M and 500M) and SmolVLM2 (2.2B) are specifically engineered for sub-2GB VRAM constraints. SmolVLM2 uses an ultra-compact visual token compression strategy, feeding downsampled patches into a lightweight text backbone. It runs offline on entry-level mobile hardware via WebGPU or ONNX Runtime Mobile.
    *   *Primary Sources:* [SmolVLM GitHub & Model Collection](https://github.com/huggingface/smollm), [Hugging Face SmolVLM Model Card](https://huggingface.co/HuggingFaceTB/SmolVLM2-2.2B-Instruct).
*   **Docling & Granite-Docling (IBM Research):** IBM's Docling ecosystem provides targeted document layout decomposition. Rather than running a general-purpose conversational VLM, Docling couples specialized modular models: **DocLayNet** (document layout analysis), **TableFormer** (table structural recovery), and the compact **Granite-Docling-258M** model. It produces structured `DocLang` or Markdown directly from PDF binary streams, cutting edge compute requirements by an order of magnitude compared to 3B+ general VLMs.
    *   *Primary Sources:* [Docling Open Source Project (DS4SD)](https://github.com/DS4SD/docling), [IBM Granite-Docling Models](https://huggingface.co/ibm-granite/granite-docling-258m).
*   **MobileLLM & MobileLLM-R1 (Meta Research):** Focuses on sub-billion parameter scaling (125M, 350M, 1B). MobileLLM employs "deep and thin" network architectures, block-wise parameter sharing between non-consecutive layers, and Grouped-Query Attention (GQA). MobileLLM-R1 introduces reasoning tokens for compact logic verification at the edge.
    *   *Primary Sources:* [MobileLLM Technical Paper (arXiv:2402.14905)](https://arxiv.org/abs/2402.14905), [Meta MobileLLM GitHub](https://github.com/facebookresearch/MobileLLM).

---

### 2.5 Comparative Model Evaluation for Mobile Document Extraction

| Model | Parameter Count | Vision Architecture | Resolution Handling | RAM Footprint (INT4 Weights + KV) | Document Parsing Strengths | Primary Limitation on Mobile |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Gemma 4 E2B** | 2.1B (Effective) | Encoder-Free Unified | Dynamic Patch Pooling | ~1.6 GB (8K Context) | Native JSON formatting, MTP 2.2x speedup | Requires modern GPU/NPU for unified prefill |
| **Gemma 3n E4B** | ~8B raw / 4B active | Modular ViT + PLE | Native Dynamic | ~2.5 GB (PLE enabled) | Preserves high-parameter reasoning in low RAM | Co-processor dependency (Qualcomm/MediaTek) |
| **EmbeddingGemma 2**| 740M (Modular) | SigLIP-derived (170M) | Fixed/Tile Multiscale | ~650 MB (Vision + Text) | On-device visual document search & RAG | Does not generate text; embedding-only |
| **Qwen2.5-VL-3B** | 3.1B | Dynamic ViT + mRoPE | Dynamic (multiples of 28) | ~2.2 GB (4K Context) | Best-in-class table & complex form parsing | Heavy prefill latency on mobile CPUs |
| **Phi-4-Multimodal** | 5.6B | MoLoRA Vision Adapter | Multiscale Patch Tiling | ~3.8 GB (8K Context) | Deep visual reasoning & multi-modal routing | High memory pressure; triggers LMK on 6GB RAM |
| **SmolVLM2-2.2B** | 2.2B | Compact SigLIP | Tiled Patch Compression | ~1.4 GB (4K Context) | Low memory usage; fast prefill on mid-tier SoCs | Lower accuracy on dense multi-page tables |
| **Granite-Docling** | 258M | Specialized TableFormer | Bounding-box layout grid | ~350 MB | Exact layout & table structure to Markdown | Narrow general visual reasoning capabilities |

---

## 3. Mobile and Edge Inference Engines & Hardware Acceleration

### 3.1 Google LiteRT and MediaPipe GenAI

*   **LiteRT (formerly TensorFlow Lite):** In 2024–2025, Google rebranded and re-architected TensorFlow Lite as **LiteRT** under the Google AI Edge umbrella. LiteRT serves as Google's production runtime for on-device ML across Android, iOS, and embedded platforms.
    *   **CompiledModel API:** The modern entry point in LiteRT. It automates hardware delegation, routing tensor graphs across CPU, GPU (OpenCL/Vulkan), and vendor NPUs without requiring manual delegate configuration.
    *   **LiteRT-LM (Android Kotlin API):** The specialized runtime layer for generative language and multimodal models. It manages the KV cache, handles prompt formatting, and supports chunked prefill to maintain UI responsiveness.
    *   **MediaPipe Status:** The legacy MediaPipe LLM Inference API was placed into **maintenance-only mode** in late 2026. Google officially directs all new Android generative AI implementations to **LiteRT-LM**. MediaPipe lacked direct, flexible vendor NPU selection, whereas LiteRT provides native co-design with hardware vendors.
    *   *Primary Sources:* [Google AI Edge LiteRT Overview](https://ai.google.dev/edge/litert), [LiteRT-LM GitHub & Architecture](https://github.com/google-ai-edge/litert-lm), [MediaPipe LLM Migration Notice](https://developers.google.com/mediapipe/solutions/genai/llm_inference).

### 3.2 ExecuTorch (PyTorch On-Device)

Developed by Meta and the PyTorch Foundation, **ExecuTorch** provides a unified software stack for running PyTorch programs on edge devices with zero Python runtime dependency.
*   **Multimodal Splitting Architecture:** To run VLMs on heterogeneous mobile compute, ExecuTorch splits the model at `torch.export` time into three decoupled modules: (1) the Vision Encoder, (2) the Text Embedding Projector, and (3) the Autoregressive Text Decoder.
*   **Hardware Delegation:** 
    *   *Qualcomm QNN Delegate:* Uses Ahead-Of-Time (AOT) graph compilation to push the heavy Vision Encoder and Prefill stages directly to the Hexagon NPU.
    *   *Apple Core ML Delegate:* Integrates via `CoreMLPartitioner`, lowering transformer blocks to Apple Neural Engine (ANE) subgraphs.
    *   *Backends:* CPU execution is backed by XNNPACK; GPU execution leverages Vulkan and Metal.
*   *Primary Sources:* [PyTorch ExecuTorch Documentation](https://pytorch.org/executorch/main/), [ExecuTorch Multimodal Export Pattern](https://pytorch.org/executorch/main/model-export-overview.html).

### 3.3 llama.cpp (Android NDK / GGUF)

`llama.cpp` remains the dominant open-source C/C++ engine for cross-platform quantized inference.
*   **Multimodal Projector Architecture (`mmproj`):** `llama.cpp` handles VLMs (such as Qwen2.5-VL and LLaVA) via paired files: the base language model GGUF and a companion `.mmproj` GGUF file containing the vision encoder and projection weights.
*   **Android NDK Integration:** Shipped as precompiled shared libraries (`libllama.so`) accessed via Java Native Interface (JNI).
*   **Execution Flags for Edge Devices:**
    *   `--no-mmproj-offload`: Forces the vision projector to execute on system CPU/RAM rather than mobile GPU VRAM. This is a critical workaround on devices with strict GPU memory allocations to prevent instant graphics-driver crashes.
    *   Backends on Android: OpenCL (Adreno/Mali), Vulkan, and experimental Hexagon Direct delegates.
*   *Primary Sources:* [llama.cpp GitHub Repository](https://github.com/ggerganov/llama.cpp), [GGUF Specification](https://github.com/ggerganov/ggml/blob/master/docs/gguf.md).

### 3.4 WebLLM and ONNX Runtime Web (In-Browser Edge Ingestion)

*   **WebLLM (MLC-LLM / Apache TVM):** Compiles models directly to WebGPU compute shaders, exposing an OpenAI-compatible TypeScript API. WebLLM incorporates **XGrammar** for zero-latency JSON schema enforcement directly inside browser worker threads.
    *   *Primary Sources:* [WebLLM Documentation](https://webllm.mlc.ai/), [MLC-LLM Architecture](https://github.com/mlc-ai/mlc-llm).
*   **ONNX Runtime Web:** Executes models in-browser via the WebGPU Execution Provider or WebAssembly (Wasm). Serving as the backend for **Transformers.js**, it is standard for running sub-1B document vision models (e.g., SmolVLM, Donut, LayoutLM) directly inside mobile web browsers.
    *   *Primary Sources:* [ONNX Runtime WebGPU Guide](https://onnxruntime.ai/docs/tutorials/web/webgpu-execution-provider.html), [Transformers.js Repository](https://github.com/huggingface/transformers.js).

---

### 3.5 Hardware Accelerator Architectures (NPU, ANE, TPU)

```
+-------------------------------------------------------------------------------------------------------------+
|                                  HARDWARE ACCELERATION COMPARISON MATRIX                                    |
+----------------------+-----------------------------+-----------------------------+--------------------------+
| Specification        | Qualcomm Hexagon NPU (QNN)  | Apple Neural Engine (ANE)   | Google Tensor TPU        |
+----------------------+-----------------------------+-----------------------------+--------------------------+
| Target Platforms     | Snapdragon 8 Gen 3/4/Elite  | iPhone 15 Pro / 16 / A17/18 | Pixel 8 / 9 / 10         |
| Primary Framework    | QNN SDK / ExecuTorch / LiteRT| Core ML / MLX               | LiteRT / Android AICore  |
| Memory Architecture  | Dedicated Shared NPU SRAM   | Unified System Memory (UMA) | Shared System DRAM       |
| Hardware Transformer | Element Accelerator         | ANE Matrix Engine Units     | TPU Tensor Cores         |
| Acceleration         | (Native Attention Op Fusion)| (Static Graph Compilation)  | (LiteRT Edge TPU ops)    |
| Stateful KV Cache    | Direct HTP Buffer Residency | Core ML MLState API (iOS 18)| In-memory LiteRT buffer  |
| Supported Precisions | INT2, INT4, INT8, FP16      | INT4, INT8, FP16            | INT4, INT8, FP16         |
| Major Constraint     | Strict AOT shape validation | Static shapes (mod 32)      | Restricted system API    |
|                      |                             | Dynamic shapes fall to GPU  | access outside AICore    |
+----------------------+-----------------------------+-----------------------------+--------------------------+
```

*   **Qualcomm Snapdragon (Hexagon NPU & QNN SDK):** Modern Snapdragon platforms (8 Gen 3, Gen 4, Elite) feature a dedicated **Element Accelerator** designed specifically for transformer attention primitives. It is paired with a 50% larger dedicated on-chip shared memory buffer, avoiding costly DRAM transfers for intermediate ViT attention maps. The Qualcomm AI Engine Direct (QNN) SDK compiles quantized INT4 models ahead-of-time to ensure optimal hardware utilization.
    *   *Primary Sources:* [Qualcomm AI Engine Direct SDK](https://developer.qualcomm.com/software/qualcomm-ai-engine-direct-sdk), [Qualcomm AI Hub](https://aihub.qualcomm.com/).
*   **Apple Neural Engine (ANE & Core ML 18+):** Starting in iOS 18/macOS 15, Apple introduced the **`MLState` API** in Core ML. Previously, autoregressive decoding on the ANE required re-feeding the full KV cache into the input tensor at every step, creating an $O(N^2)$ memory bandwidth bottleneck. `MLState` keeps the KV cache resident within the ANE's hardware buffers, enabling in-place state updates. However, the ANE enforces rigid **static tensor shapes** (typically requiring tensor dimensions to be exact multiples of 32). Production implementations must build "multifunction" models: dynamic/batched graphs for visual prefill, and static stateful graphs for token decode.
    *   *Primary Sources:* [Apple Core ML Stateful Models Documentation](https://apple.github.io/coremltools/docs-guides/source/stateful-models.html), [Apple Machine Learning Research](https://machinelearning.apple.com/).
*   **Google Tensor TPU & Android AICore:** On Google Pixel hardware, on-device AI is bifurcated:
    *   *Android AICore:* A background OS system daemon managing system-level instances of **Gemini Nano**. AICore handles model updates, hardware scheduling, and thermal throttling at the system level.
    *   *LiteRT:* The developer-facing framework for custom models (Gemma, Qwen, Docling) utilizing the Tensor TPU via the LiteRT Edge TPU delegate.
    *   *Primary Sources:* [Android AICore Documentation](https://developer.android.com/ai/aicore), [Google AI Edge on Tensor](https://ai.google.dev/edge).

---

## 4. End-to-End Architectural Pipeline for Mobile PDFs

### 4.1 PDF Page Rendering & Image Ingestion

On-device document analysis begins by converting binary PDF streams into rasterized bitmaps. Because VLMs process raw pixels rather than PostScript vectors, rendering quality directly determines extraction accuracy.

```
+--------------------+      +--------------------+      +-----------------------+
|  Binary PDF Stream | ---> | Engine Rasterizer  | ---> | Hardware Bitmap       |
|  (Storage / Stream)|      | (DPI Scale Formula)|      | (ARGB_8888 in Memory) |
+--------------------+      +--------------------+      +-----------------------+
                                                                    |
                                                                    v
+--------------------+      +--------------------+      +-----------------------+
| Constrained JSON   | <--- | On-Device VLM      | <--- | ViT Patch Tiling      |
| Extraction Output  |      | Autoregressive Gen |      | (e.g. 14x14 Patches)  |
+--------------------+      +--------------------+      +-----------------------+
```

#### Android Framework Engines: `PdfRenderer` vs. `PDFium` vs. `pdf.js`

1.  **Android Native `PdfRenderer` (`android.graphics.pdf.PdfRenderer`):**
    *   Available since API 21, built directly into the Android framework. It delegates internally to an Android-hardened build of the Google PDFium C++ library.
    *   *API 35+ (Android 15) Evolution:* Transformed into an updatable Android Mainline module (`com.google.android.documentsui`). API 35 introduced native programmatic **text searching, linear text extraction, and PDF form-field introspection** directly within `PdfRenderer`, closing the historical feature gap with external libraries.
    *   *Advantage:* Zero APK overhead; direct hardware-accelerated rendering into native `android.graphics.Bitmap`.
2.  **PDFium C++ (via NDK bindings, e.g., `PdfiumAndroid`):**
    *   Self-contained native C++ library compiled into the APK.
    *   *Advantage:* Consistent rendering across all Android versions (API 21 through 34); supports password-protected documents, embedded annotations, non-standard font substitution, and direct raw page extraction without OS version variance.
    *   *Disadvantage:* Increases base APK binary size by 8 MB to 15 MB across ABIs (`arm64-v8a`, `armeabi-v7a`).
3.  **Mozilla `pdf.js` (WebAssembly / JavaScript):**
    *   Used in hybrid Web / PWA environments. Renders pages to an HTML5 `<canvas>`.
    *   *Disadvantage:* High memory overhead in mobile WebViews; significantly slower rasterization speeds compared to native NDK PDFium.

#### The DPI Resolution Formula and Memory Calculations

PDFs define page geometries in **PostScript points** (where 1 point = 1/72 inch). Standard A4 dimensions are $595 \times 842$ points; US Letter is $612 \times 792$ points. Rendering at default scale yields a blurry 72 DPI image where small 8pt table text becomes illegible to a Vision Transformer.

To achieve the optimal balance between character legibility and memory footprint, applications must scale the rendered bitmap:

$$\text{Bitmap Width} = \left( \frac{\text{Target DPI}}{72} \right) \times \text{Page Width in Points}$$

$$\text{Bitmap Height} = \left( \frac{\text{Target DPI}}{72} \right) \times \text{Page Height in Points}$$

```kotlin
// Android Native Implementation for 150 DPI Ingestion
fun renderPageToBitmap(pdfRenderer: PdfRenderer, pageIndex: Int, targetDpi: Float = 150f): Bitmap {
    val page = pdfRenderer.openPage(pageIndex)
    val scale = targetDpi / 72f
    val bitmapWidth = (page.width * scale).toInt()
    val bitmapHeight = (page.height * scale).toInt()
    
    // Allocate native hardware bitmap (ARGB_8888 = 4 bytes/pixel)
    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
    page.close()
    return bitmap
}
```

*   **Memory Cost:** An uncompressed A4 page rendered at **300 DPI** produces a $2480 \times 3508$ bitmap, requiring **34.8 MB of uncompressed RAM**. Rendering at **150 DPI** produces a $1240 \times 1754$ bitmap, consuming only **8.7 MB of RAM**, which retains sufficient character contrast for models with dynamic resolution (like Qwen2.5-VL) while preventing graphics memory allocation faults.

---

### 4.2 "Vision-First" Direct VLM vs. Classic Two-Stage Pipeline

A major architectural debate in mobile document processing centers on whether to use a direct end-to-end VLM or a classic two-stage pipeline.

```
PARADIGM 1: CLASSIC TWO-STAGE PIPELINE
+-------------+      +-------------------+      +------------------+      +------------------+
| Document    | ---> | On-Device OCR     | ---> | Extracted Text   | ---> | Quantized Text   | ---> Structured
| Image       |      | (Google ML Kit)   |      | (Flattened String|      | SLM (Gemma-2B)   |      JSON
+-------------+      +-------------------+      +------------------+      +------------------+

PARADIGM 2: VISION-FIRST END-TO-END VLM
+-------------+      +---------------------------------------------+      +------------------+
| Document    | ---> | Mobile VLM (Qwen2.5-VL / Gemma 4 E2B)       | ---> | Structured JSON  |
| Image       |      | (ViT Patches + Autoregressive Decoding)     |      | Output           |
+-------------+      +---------------------------------------------+      +------------------+

PARADIGM 3: PRODUCTION HYBRID CASCADED PIPELINE (RECOMMENDED)
                     +---------------------------------------+
                     | Document Ingestion & Page Triage      |
                     +---------------------------------------+
                                         |
                     [Native Digital Text Layer Present?]
                                    /         \
                             (YES) /           \ (NO / Scanned / Form)
                                  v             v
            +-----------------------+         +-------------------------------+
            | Fast Regex / Parser   |         | Visual Complexity Evaluator   |
            +-----------------------+         +-------------------------------+
                        |                                /                 \
             [Extraction Complete?]          (Low Complexity)      (High Complexity)
                 /              \                   |                      |
          (PASS)/         (FAIL) \                  v                      v
               v                  +-------> +---------------+    +--------------------+
          Structured JSON                   | ML Kit OCR +  |    | Vision-First VLM   |
          Output                            | Text SLM      |    | (Qwen2.5-VL-3B /   |
                                            +---------------+    | Gemma 4 E2B)       |
                                                    |            +--------------------+
                                                    v                      |
                                            Structured JSON <--------------+
                                            Output
```

#### Detailed Trade-Off Comparison

| Evaluation Metric | Classic Two-Stage (OCR $\rightarrow$ Text SLM) | Vision-First VLM (End-to-End Image Ingestion) |
| :--- | :--- | :--- |
| **Complex Table Extraction** | **Poor.** Table cell borders are lost; column text interleaves unpredictably. | **Superior.** 2D spatial coordinates and grid lines are directly encoded in ViT tokens. |
| **Checkboxes, Signatures, Forms** | **Fails completely.** Standard OCR drops non-text glyphs or hallucinates arbitrary characters. | **Robust.** Visual elements are identified as explicit semantic concepts. |
| **Time-to-First-Token (TTFT)** | **Fast (100–300 ms).** OCR finishes in 30–50 ms; text prefill is small (300–800 tokens). | **Slow (1500–5000 ms).** ViT patch projection and visual prefill dominate processing. |
| **Memory Bandwidth & KV Cache** | **Minimal.** Context is limited to actual character token count. | **Heavy.** 512–2048 visual patch tokens occupy KV cache throughout the decoding phase. |
| **Hallucination & Inflation Risk** | Low hallucination rate for extracted text, but high risk of dropped fields. | Autoregressive generation can hallucinate numbers or loop endlessly without grammar constraints. |
| **Compute / Battery Footprint** | Extremely low energy consumption; OCR runs on DSP/NPU in milliseconds. | Sustained NPU/GPU utilization drives battery drain and raises SoC temperatures. |

*   **Benchmark Grounding (OmniDocBench, VDR-Bench):** Real-world evaluations on financial benchmarks demonstrate that while pure OCR achieves lower Character Error Rates (CER) on dense, single-column paragraphs, it fails on complex financial schedules. VLMs eliminate reading-order errors but exhibit an **"inflation ratio"** (over-generating plausible text) unless governed by constrained decoding.
*   **The Recommended Production Solution:** A **Hybrid Cascaded Architecture**. If a document contains a valid embedded digital text layer and a simple single-column layout, use direct programmatic text extraction or fast OCR into a text SLM. If the page contains multi-column structures, tables, form fields, or scanned receipts, route the page image to the Vision-First VLM.

---

### 4.3 Structured JSON Output & Constrained Decoding on Edge

Extracting data from documents requires deterministic schema compliance (e.g., extracting invoice amounts, loan interest rates, or payment dates into strongly typed JSON). Pure autoregressive decoding often introduces syntax errors, mismatched brackets, or missing required fields.

**Constrained decoding** intercepts token generation at each decoding step. Before the model executes the final `argmax` or multinomial sampling, a constrained decoding mask zeroes out the logits of all tokens that would violate a specified formal grammar (typically represented as a Context-Free Grammar or Regular Expression DFA).

```
   Raw Model Logits (Vocab Size ~150k)
                  |
                  v
   +------------------------------+
   |   Logit Masking Interceptor  | <--- Grammar / Schema State Engine
   +------------------------------+      (DFA / Pushdown Automaton)
                  |
   Filtered Logits (Only Valid Next JSON Tokens)
                  |
                  v
   Sampled Token (e.g., ':', '"', '[0-9]')
```

#### Framework Comparison: XGrammar vs. Outlines vs. GBNF

1.  **XGrammar (State of the Art for Edge):**
    *   Developed by the MLC team and integrated into WebLLM and modern serving backends.
    *   *Mechanism:* Rather than performing costly runtime grammar validation, XGrammar pre-computes an **adaptive token mask cache** across the tokenizer's vocabulary. During token generation, computing the valid token mask requires only a lightweight bitmask lookup.
    *   *Edge Impact:* Adds negligible computational overhead (<5% latency penalty), making it ideal for mobile CPU/NPU deployments.
    *   *Primary Sources:* [XGrammar Technical Paper & GitHub](https://github.com/mlc-ai/xgrammar).
2.  **GBNF (GGML BNF Grammars - `llama.cpp`):**
    *   The native grammar engine in `llama.cpp`.
    *   *Mechanism:* Uses a backtracking stack parser based on Backus-Naur Form rules.
    *   *Edge Impact:* Extremely memory-efficient, implemented in raw C/C++. However, on highly complex, deeply nested JSON schemas with hundreds of fields, the stack evaluation loop can introduce a measurable delay (10–20 ms per token) on slower mobile CPUs.
    *   *Primary Sources:* [llama.cpp GBNF Guide](https://github.com/ggerganov/llama.cpp/blob/master/grammars/README.md).
3.  **Outlines (dottxt):**
    *   Converts JSON Schemas and Regular Expressions into Deterministic Finite Automata (DFAs).
    *   *Edge Impact:* Industry-standard in Python and server-side environments, but requires transpilation or C++ runtime bindings to execute inside mobile applications.
    *   *Primary Sources:* [Outlines Documentation](https://outlines-dev.github.io/outlines/).

> [!IMPORTANT]
> **Structural Validity vs. Semantic Correctness:**
> Constrained decoding provides a mathematical guarantee of **structural validity** (the output is guaranteed to parse via `JSON.parse()`). It does **not** guarantee **semantic truth**. If a 2B or 3B parameter model struggles to locate a specific field within an image, grammar masking may force it to emit hallucinated numbers simply to satisfy the schema's required `number` type. For complex extraction tasks, use a two-step prompt: allow the model to output a short unconstrained "scratchpad" reasoning trace before emitting the constrained JSON object.

---

## 5. Practical Mobile Engineering Trade-Offs & Production Hardening

Deploying generative vision models to mobile hardware requires navigating the strict physical and operating-system constraints of client devices.

### 5.1 Memory Management & The Android Low Memory Killer (LMKD)

Mobile operating systems enforce strict limits on resident process memory. Unlike desktop operating systems, mobile platforms do not use traditional swap partitions because writing continuously to flash storage degrades NAND flash lifecycles and drains battery reserves.

```
+---------------------------------------------------------------------------------------------------+
| PHYSICAL DEVICE RAM TIER: 4 GB (e.g. Budget Android)                                              |
| [ OS Core & System Services: ~1.8 GB ] [ System zRAM: ~800 MB ] [ Available App RAM: ~1.2 GB max ]|
|   CRITICAL: A 3B INT4 Model (~2.2 GB) WILL TRIGGER AN INSTANT LMKD FOREGROUND KILL (SIGKILL)      |
+---------------------------------------------------------------------------------------------------+
| PHYSICAL DEVICE RAM TIER: 8 GB - 12 GB (e.g. Mid-to-High Tier Android)                             |
| [ OS Core & System Services: ~2.5 GB ] [ Foreground App Budget: ~3.5 GB - 5.0 GB ]                |
|   STABLE: 2B - 3B INT4 Model fits safely with 8K context and UI overhead                          |
+---------------------------------------------------------------------------------------------------+
```

#### Dual Enforcement: System LMKD vs. Android 17 Per-App Limits

1.  **System-Wide LMKD (Low Memory Killer Daemon):** Monitors kernel memory pressure via `memcg` (memory cgroups). When total available RAM drops below system thresholds, `lmkd` terminates processes in order of their `oom_adj_score`, beginning with cached background apps and eventually killing foreground activities if memory pressure reaches critical levels.
2.  **Android 17 Per-App Memory Limits:** Modern Android implementations introduce strict per-process memory limits. Even if the device has unallocated physical RAM available, an application exceeding its designated memory tier is aggressively compressed into **zRAM** (in-memory compressed RAM). Continued expansion triggers an immediate process termination (`REASON_OTHER` in `ApplicationExitInfo`).
3.  **Native Memory vs. Java Heap:** While Android's JVM heap is bounded by `largeHeap` (typically 256 MB to 512 MB), native memory allocations (`malloc` / `mmap`) via C++ runtimes (`libllama.so`, `liblitert.so`) bypass Java heap ceilings. However, the Linux kernel aggregates both allocations under the process's total Resident Set Size (RSS), subjecting the native runtime directly to `lmkd` enforcement.

#### Safe Operating Ceilings

*   **4 GB RAM Devices:** Maximum safe native memory allocation is **1.0 GB to 1.2 GB**. 3B models cannot safely run here. Target sub-billion parameter models (e.g., MobileLLM, Granite-Docling-258M) or route execution to two-stage OCR pipelines.
*   **8 GB RAM Devices:** Safe allocation ceiling is **2.5 GB to 3.2 GB**. Can comfortably host 2B to 3B models (Gemma 4 E2B, Qwen2.5-VL-3B) at INT4 quantization with up to 4K–8K context tokens.
*   **12 GB+ RAM Devices:** Safe allocation ceiling is **5.0 GB+**. Can run 4B–5B models (Gemma 3n E4B, Phi-4-Multimodal) with extensive multimodal context.

---

### 5.2 4-Bit Quantization Landscape (INT4, AWQ, GGUF)

Unquantized FP16 weights require 2 bytes per parameter (a 3B model consumes ~6 GB of storage and RAM, making edge execution impossible). Quantization is mandatory.

*   **Weight-Only Quantization (INT4-W4A16):** Model weights are compressed to 4-bit integers (~0.5 bytes per parameter), while activations remain in FP16/BF16 during matrix multiplications. A 3B parameter model shrinks to **~1.7 GB** of weight memory.
*   **Activation-Aware Weight Quantization (AWQ):** Identifies the top 1% of salient weight channels that preserve model accuracy and retains them at higher precision while aggressively compressing non-salient weights. AWQ is critical for Vision Transformers, where visual patch projections contain high outlier activations that degrade severely under naive uniform quantization.
*   **GGUF Quantization Formats (`Q4_K_M`, `Q4_K_S`):** `llama.cpp`'s "k-quant" strategy uses mixed block quantization, keeping critical attention layers at 5-bit or 6-bit while quantizing feed-forward layers to 4-bit.
*   **Impact on Document Extraction:** Aggressive uniform 4-bit quantization can degrade fine-grained character recognition (e.g., misreading small decimal points in invoices, confusing `$` with `8`, or misinterpreting tiny table grid lines). Per-channel AWQ or mixed-precision quantization (retaining the ViT vision patch embedder in FP16/INT8 and quantizing the text decoder to INT4) is the recommended production configuration.

---

### 5.3 Thermal Throttling & Battery Impact

*   **Burst vs. Sustained Compute:** Autoregressive token decoding is memory-bandwidth bound, but **visual prefill** (processing 1000+ visual patches through the Vision Transformer) is heavily compute-bound.
*   **Thermal Throttling Drop-off:** Running high-intensity visual prefill draws **5 W to 10 W** across the mobile SoC. Because smartphones lack active cooling, skin temperature limits force the kernel's thermal governor to downclock CPU/GPU/NPU frequencies after **45 to 60 seconds** of sustained execution.
*   **Performance Collapse:** A model generating 20 tokens per second at peak performance can drop to **3 to 5 tokens per second** under thermal throttling.
*   **Battery Drain:** A single multi-page PDF analysis run consuming 60 seconds of unconstrained NPU/GPU execution can drain **1% to 2% of total battery capacity**.
*   **Mitigation:** 
    *   Process multi-page documents lazily (page-by-page on user scroll or explicit demand) rather than batch-processing entire 50-page PDFs.
    *   Insert a 500 ms thermal cooldown pause between consecutive page extractions.
    *   Monitor thermal status using Android's `PowerManager.addThermalStatusListener`. If the thermal status reaches `THERMAL_STATUS_SEVERE`, throttle prefill chunk sizes or drop resolution.

---

### 5.4 Model Download Size vs. User Experience

Google Play and the Apple App Store enforce strict packaging constraints for mobile applications:
*   **Base APK Limit:** Google Play caps initial base APK download sizes at **200 MB**; the App Store sets the cellular download warning threshold at **200 MB**. A 1.5 GB to 2.5 GB model file cannot be bundled directly inside the base app download.

```
+---------------------------------------------------------------------------------------------------+
| GOOGLE PLAY DISTRIBUTION STRATEGIES FOR ON-DEVICE AI                                              |
+------------------------------------+--------------------------------------------------------------+
| Delivery Model                     | Operational Characteristics                                  |
+------------------------------------+--------------------------------------------------------------+
| Play Asset Delivery (PAD):         | Downloaded automatically in the background immediately       |
| Fast-Follow (Up to 1.5 GB/pack)    | after app install. Ideal for apps where AI is core.          |
+------------------------------------+--------------------------------------------------------------+
| Play Asset Delivery (PAD):         | Downloaded only when the user first opens the document       |
| On-Demand (Up to 1.5 GB/pack)      | scanner feature. Best for optional AI tooling.               |
+------------------------------------+--------------------------------------------------------------+
| Play for On-Device AI (Beta):      | Dynamic targeting: Google Play delivers specific model packs |
| Hardware/RAM Targeted Delivery     | based on device hardware (delivering 1B to 4GB devices,      |
|                                    | 3B to 8GB devices, and skipping download on low-end phones). |
+------------------------------------+--------------------------------------------------------------+
```

*   **User Churn Metrics:** Industry telemetry shows that requiring an upfront, blocking download of >1 GB during initial app onboarding results in user drop-off rates exceeding **40%**.
*   **Production UX Pattern:** Deliver the app with standard, fast OCR (via Google ML Kit, ~25 MB binary footprint) immediately accessible out-of-the-box. Prompt the user to download the high-accuracy "Enhanced Neural Document Engine" (1.5 GB asset pack) as a background task over unmetered Wi-Fi.

---

## 6. Recommended Reference Architecture for Edge Document AI

Based on the 2026 state of the art, the following reference blueprint provides an optimal architecture for client-side document processing in mobile applications (such as financial simulators, expense managers, and legal assistants).

```
                      +---------------------------------------+
                      |       Input Document (PDF / Scan)     |
                      +---------------------------------------+
                                          |
                      +---------------------------------------+
                      |         Device Capability Gate        |
                      |   Check RAM, SoC, Battery & Thermal   |
                      +---------------------------------------+
                                          |
                   +----------------------+----------------------+
                   |                                             |
                   v                                             v
        [Device RAM < 6 GB]                           [Device RAM >= 6 GB]
                   |                                             |
+------------------------------------+        +------------------------------------+
| TIER 1: EFFICIENT DUAL PIPELINE    |        | TIER 2: VISION-FIRST VLM PIPELINE  |
| 1. Android PdfRenderer (API 35+)   |        | 1. Render Page to Bitmap @ 150 DPI |
| 2. Google ML Kit On-Device OCR     |        | 2. Google LiteRT-LM / ExecuTorch   |
| 3. Layout Bounding-Box Heuristics  |        | 3. Model: Gemma 4 E2B (2.1B) or    |
| 4. Text SLM: MobileLLM (1B) or     |        |    Qwen2.5-VL-3B (INT4-AWQ)        |
|    Granite-Docling-258M            |        | 4. Acceleration: Qualcomm QNN NPU  |
| 5. Output: JSON Extraction         |        |    or Apple Neural Engine (ANE)    |
+------------------------------------+        | 5. Constrained Decoding: XGrammar  |
                   |                          | 6. Output: Strictly Validated JSON |
                   |                          +------------------------------------+
                   |                                             |
                   +----------------------+----------------------+
                                          |
                                          v
                      +---------------------------------------+
                      |      Zod / Strict Domain Schema       |
                      |         Validation & UI Sync          |
                      +---------------------------------------+
```

### Architectural Decisions Summary

1.  **Rendering:** Use native Android `PdfRenderer` on Android 15+ (API 35+), scaling to **150 DPI**. For Android 14 and earlier, use a standalone NDK `PDFium` wrapper.
2.  **Model Selection:**
    *   *High-Tier / Flagship Devices ($\ge$8 GB RAM):* **Gemma 4 E2B** or **Qwen2.5-VL-3B-Instruct** quantized to INT4-AWQ, running on the NPU/GPU.
    *   *Mid-to-Budget Devices (4 GB – 6 GB RAM):* **Granite-Docling-258M** or **SmolVLM2-2.2B**, or fallback to **ML Kit OCR + MobileLLM 1B**.
    *   *Document Retrieval / Multimodal RAG:* **EmbeddingGemma 2 (740M)** truncated to 256 dimensions via Matryoshka learning.
3.  **Inference Engine:**
    *   *Android Native:* **LiteRT (LiteRT-LM)** for native Google/Android ecosystem alignment and direct NPU delegation.
    *   *Cross-Platform (iOS + Android):* **ExecuTorch** with Qualcomm QNN and Apple Core ML delegates.
4.  **Structured Output Enforcement:** Standardize on **XGrammar** for logit masking to guarantee valid JSON extraction with negligible latency penalty.
5.  **Distribution:** Use **Play Asset Delivery (PAD) Fast-Follow/On-Demand** with Google Play hardware-targeted asset delivery to ensure low-RAM devices never download incompatible 3B+ model checkpoints.

---

## 7. Primary Sources & References

### Foundational Models & Technical Reports
*   **Google DeepMind Gemma 4:** [Gemma 4 Open Models](https://blog.google/technology/developers/gemma-4-open-models/) | [Architecture & Guide](https://ai.google.dev/gemma/docs/gemma-4)
*   **Google DeepMind Gemma 3 / 3n:** [Gemma 3 Release](https://blog.google/technology/developers/gemma-3/) | [Gemma 3n Mobile Co-Design](https://ai.google.dev/edge/gemma3n)
*   **Google DeepMind EmbeddingGemma 2:** [EmbeddingGemma 2 Announcement](https://blog.google/technology/developers/embedding-gemma-2/) | [Model Documentation](https://ai.google.dev/gemma/docs/embeddinggemma2)
*   **Microsoft Phi-4 Family:** [Phi-4-Mini Technical Report (arXiv:2503.01743)](https://arxiv.org/abs/2503.01743) | [Microsoft Phi Cookbook](https://aka.ms/Phicookbook)
*   **Alibaba Qwen2.5-VL:** [Qwen2.5-VL Technical Report (arXiv:2502.13923)](https://arxiv.org/abs/2502.13923) | [Qwen2.5-VL Repository](https://github.com/QwenLM/Qwen2.5-VL)
*   **Alibaba Qwen3-VL:** [Qwen3-VL Model Specifications](https://qwen.ai/blog/qwen3-vl) | [Qwen Architecture](https://github.com/QwenLM/Qwen)
*   **Meta MobileLLM:** [MobileLLM Technical Paper (arXiv:2402.14905)](https://arxiv.org/abs/2402.14905) | [MobileLLM GitHub](https://github.com/facebookresearch/MobileLLM)
*   **Hugging Face SmolVLM:** [SmolVLM Collection](https://github.com/huggingface/smollm) | [SmolVLM2 Model Card](https://huggingface.co/HuggingFaceTB/SmolVLM2-2.2B-Instruct)
*   **IBM Research Docling:** [Docling Project (DS4SD)](https://github.com/DS4SD/docling) | [Granite-Docling Models](https://huggingface.co/ibm-granite/granite-docling-258m)

### On-Device Inference Engines & Frameworks
*   **Google AI Edge LiteRT:** [LiteRT Official Documentation](https://ai.google.dev/edge/litert) | [LiteRT-LM Repository](https://github.com/google-ai-edge/litert-lm)
*   **PyTorch ExecuTorch:** [ExecuTorch Documentation](https://pytorch.org/executorch/main/) | [Core ML & QNN Backends](https://pytorch.org/executorch/main/build-run-qnn.html)
*   **llama.cpp:** [llama.cpp Repository](https://github.com/ggerganov/llama.cpp) | [GGUF Specification](https://github.com/ggerganov/ggml/blob/master/docs/gguf.md)
*   **WebLLM & XGrammar:** [WebLLM Official Portal](https://webllm.mlc.ai/) | [XGrammar Repository](https://github.com/mlc-ai/xgrammar)
*   **ONNX Runtime Web:** [ONNX Runtime WebGPU Documentation](https://onnxruntime.ai/docs/tutorials/web/webgpu-execution-provider.html)

### Mobile Hardware & Platform Engineering
*   **Qualcomm Snapdragon Neural Processing:** [Qualcomm AI Engine Direct (QNN) SDK](https://developer.qualcomm.com/software/qualcomm-ai-engine-direct-sdk) | [Qualcomm AI Hub](https://aihub.qualcomm.com/)
*   **Apple Neural Engine:** [Core ML Stateful Models Guide](https://apple.github.io/coremltools/docs-guides/source/stateful-models.html) | [Apple ML Research](https://machinelearning.apple.com/)
*   **Android AICore & OS Documentation:** [Android AICore Documentation](https://developer.android.com/ai/aicore) | [Android PdfRenderer API](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer)
*   **Google Play Asset Delivery:** [Play Asset Delivery Documentation](https://developer.android.com/guide/playcore/asset-delivery) | [Play Feature Delivery](https://developer.android.com/guide/playcore/feature-delivery)
