package com.finsim.ingestion

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ResponseFormat
import com.google.ai.edge.litertlm.SamplerConfig
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class GemmaInferenceDiagnostics(
    val modelRevision: String,
    val modelSha256: String,
    val backend: String,
    val totalRamBytes: Long,
    val availableRamBytesBeforeLoad: Long,
    val peakProcessPssKb: Long,
    val modelInitializationLatencyMs: Long,
    val inferenceLatencyMs: Long
)

class AndroidGemmaVisionRunner(
    context: Context,
    private val modelStorage: ModelStorage,
    private val capabilityGateProvider: () -> DeviceCapabilityGate = { AndroidDeviceCapabilityGate(context) }
) : VlmVisionRunner, AutoCloseable {
    private val appContext = context.applicationContext
    private val inferenceMutex = Mutex()
    private var engine: Engine? = null

    @Volatile
    var lastDiagnostics: GemmaInferenceDiagnostics? = null
        private set

    override val modelName: String = "Gemma 4 E2B"

    override suspend fun isModelReady(): Boolean = withContext(Dispatchers.IO) {
        capabilityGateProvider().isEligibleForVlm && modelStorage.isModelDownloaded() &&
            modelStorage.getModelFilePath()?.let { path ->
                val file = File(path)
                file.isFile && file.name == GemmaModelArtifact.fileName && file.name.endsWith(".litertlm") &&
                    file.length() == GemmaModelArtifact.sizeBytes
            } == true
    }

    override suspend fun analyzePage(
        imageBytes: ByteArray,
        framework: RegulatoryFramework,
        targetMode: ImportTargetMode
    ): ExtractedDocumentData = inferenceMutex.withLock {
        lastDiagnostics = null
        withContext(Dispatchers.IO) {
            require(imageBytes.isNotEmpty()) { "La página rasterizada está vacía." }
            requireEligible()
            check(modelStorage.isModelDownloaded()) { "Gemma 4 E2B no está descargado y verificado." }

            val memoryBefore = readMemorySnapshot()
            val pssSampler = ProcessPssSampler()
            var initializationMs = 0L
            try {
                val (activeEngine, initTime) = getOrInitializeEngine()
                initializationMs = initTime
                requireEligible()
                val start = System.nanoTime() / 1_000_000L
                val response = activeEngine.createConversation(
                    ConversationConfig(
                        samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0, seed = 7),
                        maxOutputToken = MAX_OUTPUT_TOKENS,
                        enableResponseFormat = true,
                        enableSpeculativeDecoding = false
                    )
                ).use { conversation ->
                    conversation.sendMessage(
                        com.google.ai.edge.litertlm.Contents.of(
                            Content.Text(buildStatementPrompt(framework, targetMode)),
                            Content.ImageBytes(imageBytes)
                        ),
                        maxOutputToken = MAX_OUTPUT_TOKENS,
                        responseFormat = ResponseFormat.json(VlmJsonGrammar.JSON_SCHEMA_PROMPT)
                    ).toString()
                }
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                val inferenceMs = System.nanoTime() / 1_000_000L - start
                requireEligible()
                check(response.isNotBlank()) { "LiteRT-LM devolvió una respuesta vacía." }
                val extracted = VlmJsonGrammar.parseVlmResponse(response, framework, targetMode)
                check(extracted.hasRecognizedValues()) { "Gemma no devolvió campos utilizables del documento." }

                lastDiagnostics = GemmaInferenceDiagnostics(
                    modelRevision = GemmaModelArtifact.revision,
                    modelSha256 = GemmaModelArtifact.sha256,
                    backend = "CPU",
                    totalRamBytes = memoryBefore.totalRamBytes,
                    availableRamBytesBeforeLoad = memoryBefore.availableRamBytes,
                    peakProcessPssKb = pssSampler.peakPssKb,
                    modelInitializationLatencyMs = initializationMs,
                    inferenceLatencyMs = inferenceMs
                )
                Log.i(LOG_TAG, "Gemma real image inference complete: $lastDiagnostics")
                extracted
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (oom: OutOfMemoryError) {
                close()
                throw IllegalStateException(
                    "Android se quedó sin memoria al cargar o ejecutar Gemma 4 E2B. Libera memoria y vuelve a intentar; 8 GiB de RAM total no garantizan espacio libre suficiente.",
                    oom
                )
            } catch (failure: Exception) {
                close()
                throw IllegalStateException("Falló la inferencia real de Gemma 4 E2B: ${failure.message ?: failure::class.simpleName}.", failure)
            } finally {
                pssSampler.close()
            }
        }
    }

    private fun requireEligible() {
        val gate = capabilityGateProvider()
        check(gate.isEligibleForVlm) {
            gate.ineligibilityReason ?: "Gemma requiere Android, al menos 8 GiB de RAM total y ausencia de presión de memoria baja."
        }
    }

    private suspend fun getOrInitializeEngine(): Pair<Engine, Long> {
        engine?.let { return it to 0L }
        requireEligible()
        val modelFile = verifyModelArtifact()
        val cacheDir = File(appContext.cacheDir, "litert-lm").apply { check(exists() || mkdirs()) }
        val created = Engine(
            EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = Backend.CPU(),
                visionBackend = Backend.CPU(),
                maxNumTokens = 4096,
                maxNumImages = 1,
                cacheDir = cacheDir.absolutePath
            )
        )
        val start = System.nanoTime() / 1_000_000L
        try {
            created.initialize()
            engine = created
            return created to (System.nanoTime() / 1_000_000L - start)
        } catch (failure: Exception) {
            runCatching { created.close() }
            throw IllegalStateException("No se pudo inicializar el modelo Gemma 4 E2B: ${failure.message ?: failure::class.simpleName}.", failure)
        }
    }

    private fun verifyModelArtifact(): File {
        val path = modelStorage.getModelFilePath()
            ?: error("No se encontró el archivo privado verificado de Gemma 4 E2B.")
        val file = File(path)
        check(file.isFile && file.name == GemmaModelArtifact.fileName && file.name.endsWith(".litertlm")) {
            "El modelo debe llamarse ${GemmaModelArtifact.fileName} y conservar la extensión .litertlm."
        }
        check(file.length() == GemmaModelArtifact.sizeBytes) {
            "El tamaño del modelo no coincide con la revisión fijada; elimina la descarga y vuelve a descargarla."
        }
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(HASH_BUFFER_BYTES)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
        check(actualHash.equals(GemmaModelArtifact.sha256, ignoreCase = true)) {
            "SHA-256 incorrecto para Gemma revisión ${GemmaModelArtifact.revision}; elimina la descarga y vuelve a descargarla."
        }
        return file
    }

    private fun readMemorySnapshot(): MemorySnapshot {
        val gate = capabilityGateProvider()
        val info = ActivityManager.MemoryInfo()
        val manager = appContext.getSystemService(ActivityManager::class.java)
        val availableBytes = runCatching {
            checkNotNull(manager) { "Android no entregó ActivityManager." }
            manager.getMemoryInfo(info)
            info.availMem
        }.getOrElse { throw IllegalStateException("No se pudo consultar la memoria disponible de Android.", it) }
        return MemorySnapshot((gate.totalRamGb * BYTES_PER_GIB).toLong(), availableBytes)
    }

    private fun buildStatementPrompt(framework: RegulatoryFramework, targetMode: ImportTargetMode): String =
        """Extrae solo valores visibles en esta página de estado financiero para ${framework.label} y destino ${targetMode.label}. No infieras, calcules ni completes datos ausentes. Usa null cuando un valor no aparezca o no sea legible. Conserva los dígitos decimales impresos y omite símbolos monetarios y separadores de miles en los campos numéricos. Devuelve únicamente el objeto JSON solicitado, sin explicación ni Markdown."""

    private fun ExtractedDocumentData.hasRecognizedValues(): Boolean = listOf(
        bankNameCandidate, statementMonth?.toString(), statementCutDate, closingBalance, originalPrincipal,
        termMonths?.toString(), rate?.valuePercent, monthlyPayment, principalPaid, interestCharged, totalInsurance
    ).any { !it.isNullOrBlank() } || insurances.isNotEmpty()

    @Synchronized
    override fun close() {
        runCatching { engine?.close() }
        engine = null
    }

    private data class MemorySnapshot(val totalRamBytes: Long, val availableRamBytes: Long)

    private class ProcessPssSampler : AutoCloseable {
        private val stopped = AtomicBoolean(false)
        private val peak = AtomicLong(readPss())
        private val thread = Thread({
            while (!stopped.get()) {
                peak.updateAndGet { maxOf(it, readPss()) }
                try {
                    Thread.sleep(100)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }, "finsim-gemma-pss").apply {
            isDaemon = true
            start()
        }

        val peakPssKb: Long get() = peak.get()

        override fun close() {
            stopped.set(true)
            thread.interrupt()
            runCatching { thread.join(500) }
        }

        private companion object {
            fun readPss(): Long = runCatching { Debug.getPss() }.getOrDefault(0L)
        }
    }

    private companion object {
        const val LOG_TAG = "FinsimGemma"
        const val MAX_OUTPUT_TOKENS = 512
        const val HASH_BUFFER_BYTES = 128 * 1024
        const val BYTES_PER_GIB = 1024.0 * 1024.0 * 1024.0
    }
}
