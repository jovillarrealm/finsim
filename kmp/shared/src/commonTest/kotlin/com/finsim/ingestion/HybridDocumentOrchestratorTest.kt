package com.finsim.ingestion

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CancellationException
import kotlin.test.*

/** Explicit orchestration stub; its literal output never represents model inference. */
private class ControlledOrchestrationTestVlmRunner(
    private val output: ExtractedDocumentData,
    override val extractionEngine: ExtractionEngine = ExtractionEngine.TIER_2_LOCAL_VLM
) : VlmVisionRunner {
    var callCount = 0
        private set
    var lastAnalyzedImageBytes: ByteArray? = null
        private set

    override suspend fun isModelReady(): Boolean = true

    override suspend fun analyzePage(
        imageBytes: ByteArray,
        framework: RegulatoryFramework,
        targetMode: ImportTargetMode
    ): ExtractedDocumentData {
        callCount++
        lastAnalyzedImageBytes = imageBytes
        return output
    }
}

class HybridDocumentOrchestratorTest {

    private val sampleColombiaDigitalText = """
        Bancolombia S.A. - Extracto Crédito de Consumo
        Fecha de corte: 15/10/2026
        Cuota 12 de 48
        Saldo de capital: $ 24.500.000,00
        Capital inicial: $ 30.000.000,00
        Tasa Efectiva Anual (E.A.): 19.50%
        Plazo pactado: 48 meses
        Valor cuota mensual: $ 850.000,00
        Abono a capital: $ 350.000,00
        Interés corriente: $ 477.500,00
        Total otros cobros: $ 22.500,00
        Saldo final: $ 24.150.000,00
    """.trimIndent()

    @Test
    fun testHighConfidenceDigitalTextTakesTier1WithoutInvokingRasterizerOrVlm() = runTest {
        val gate = FakeDeviceCapabilityGate(totalRamGb = 8.0)
        val storage = InMemoryModelStorage(downloaded = true, sizeBytes = GemmaModelArtifact.sizeBytes)
        val rasterizer = InMemoryPageRasterizer()
        val vlmRunner = FakeVlmVisionRunner()

        val orchestrator = HybridDocumentOrchestrator(
            rasterizer = rasterizer,
            vlmRunner = vlmRunner,
            capabilityGate = gate,
            modelStorage = storage
        )

        val result = orchestrator.processDocument(
            pdfBytes = ByteArray(0),
            rawText = sampleColombiaDigitalText,
            framework = RegulatoryFramework.COLOMBIA_SFC,
            targetMode = ImportTargetMode.SCENARIO
        )

        assertTrue(result is OrchestrationResult.Success, "Expected Success but got $result")
        assertEquals(ExtractionEngine.TIER_1_DIGITAL_TEXT, result.tierUsed)
        assertEquals(ExtractionEngine.TIER_1_DIGITAL_TEXT, result.data.sourceEngine)
        assertEquals(ExtractionEngine.TIER_1_DIGITAL_TEXT, result.data.engine)
        assertEquals("24500000.00", result.data.closingBalance)
        assertEquals("19.50", result.data.rate?.valuePercent)
        assertEquals("annual", result.data.rate?.kind)

        // Neither rasterizer nor VLM should be invoked for clear digital text
        assertEquals(0, rasterizer.callCount, "Rasterizer should not be called for Tier 1 text")
        assertEquals(0, vlmRunner.callCount, "VLM runner should not be called for Tier 1 text")
    }

    @Test
    fun scannedPdfRoutesRasterThroughControlledOrchestrationStub() = runTest {
        val gate = FakeDeviceCapabilityGate(totalRamGb = 8.0)
        val storage = InMemoryModelStorage(downloaded = true, sizeBytes = GemmaModelArtifact.sizeBytes)
        val rasterizer = InMemoryPageRasterizer()
        // This literal is a test seam for orchestration routing, never evidence of Gemma inference.
        val vlmRunner = ControlledOrchestrationTestVlmRunner(
            ExtractedDocumentData(
                sourceEngine = ExtractionEngine.TIER_2_LOCAL_VLM,
                confidence = 0.95f,
                documentKind = ImportDocumentKind.LOAN_STATEMENT,
                detectedCurrencies = setOf(com.finsim.domain.Currency.COP),
                originalPrincipal = "30000000.00",
                originalTermMonths = 48,
                termMeaning = TermMeaning.ORIGINAL,
                rate = ExtractedRate(valuePercent = "19.50", kind = "annual")
            )
        )

        val orchestrator = HybridDocumentOrchestrator(
            rasterizer = rasterizer,
            vlmRunner = vlmRunner,
            capabilityGate = gate,
            modelStorage = storage
        )

        // Test bytes route the orchestration contract; the in-memory rasterizer and stub do no inference.
        val simulatedScannedPdf = "%PDF-1.4 scanned image".encodeToByteArray()
        val result = orchestrator.processDocument(
            pdfBytes = simulatedScannedPdf,
            rawText = "",
            framework = RegulatoryFramework.COLOMBIA_SFC,
            targetMode = ImportTargetMode.SCENARIO
        )

        assertTrue(result is OrchestrationResult.Success, "Expected Success but got $result")
        assertEquals(ExtractionEngine.TIER_2_LOCAL_VLM, result.tierUsed)
        assertEquals(ExtractionEngine.TIER_2_LOCAL_VLM, result.data.sourceEngine)
        assertEquals(ExtractionEngine.TIER_2_LOCAL_VLM, result.data.engine)
        assertTrue(result.data.confidence >= 0.9f)

        // Rasterizer and VLM must be invoked
        assertEquals(1, rasterizer.callCount, "Rasterizer should be invoked once")
        assertEquals(150, rasterizer.lastDpi, "Page must be rasterized at 150 DPI")
        assertEquals(1, vlmRunner.callCount, "The controlled orchestration stub should be invoked once")
        assertContentEquals(ByteArray(1024) { (it % 256).toByte() }, vlmRunner.lastAnalyzedImageBytes)
    }

    @Test
    fun pdfBytesAreNeverParsedAsTier1Text() = runTest {
        val gate = FakeDeviceCapabilityGate(totalRamGb = 8.0)
        val storage = InMemoryModelStorage(downloaded = true, sizeBytes = GemmaModelArtifact.sizeBytes)
        val rasterizer = InMemoryPageRasterizer()
        val vlmRunner = ControlledOrchestrationTestVlmRunner(
            ExtractedDocumentData(
                sourceEngine = ExtractionEngine.TIER_2_LOCAL_VLM,
                documentKind = ImportDocumentKind.LOAN_STATEMENT,
                detectedCurrencies = setOf(com.finsim.domain.Currency.COP),
                originalPrincipal = "30000000.00",
                rate = ExtractedRate(valuePercent = "19.50", kind = "annual")
            )
        )
        val orchestrator = HybridDocumentOrchestrator(
            rasterizer = rasterizer,
            vlmRunner = vlmRunner,
            capabilityGate = gate,
            modelStorage = storage
        )
        val escapedText = sampleColombiaDigitalText
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
        val pdfBytes = "%PDF-1.4\n1 0 obj\n<< /Type /Page >>\nBT\n($escapedText) Tj\nET\nendobj\n%%EOF".encodeToByteArray()

        // The synthetic PDF contains Tier-1-readable text operators; only the separately pasted
        // rawText input may reach the text parser.
        val result = orchestrator.processDocument(
            pdfBytes = pdfBytes,
            rawText = "",
            framework = RegulatoryFramework.COLOMBIA_SFC,
            targetMode = ImportTargetMode.SCENARIO
        )

        assertTrue(result is OrchestrationResult.Success, "Expected controlled orchestration success but got $result")
        assertEquals(ExtractionEngine.TIER_2_LOCAL_VLM, result.tierUsed)
        assertEquals(1, rasterizer.callCount)
        assertEquals(1, vlmRunner.callCount)
        assertEquals(1, result.data.analyzedPage)
    }

    @Test
    fun directImageUsesTheDoclingEngineWithoutPassingThroughPdfRasterizer() = runTest {
        val imageBytes = pngHeader(width = 32, height = 24)
        val rasterizer = InMemoryPageRasterizer()
        val runner = ControlledOrchestrationTestVlmRunner(
            ExtractedDocumentData(documentKind = ImportDocumentKind.UNKNOWN),
            extractionEngine = ExtractionEngine.TIER_2_LOCAL_DOCLING
        )
        val orchestrator = HybridDocumentOrchestrator(
            rasterizer = rasterizer,
            vlmRunner = runner,
            capabilityGate = FakeDeviceCapabilityGate(totalRamGb = 4.0),
            modelStorage = InMemoryModelStorage(downloaded = true),
            enforceHardwareGate = false,
            supportsDirectImageInputs = true
        )

        val result = orchestrator.processImageDocument(
            imageBytes = imageBytes,
            format = VisualDocumentFormat.PNG,
            framework = RegulatoryFramework.GENERIC
        )

        assertTrue(result is OrchestrationResult.Success)
        assertEquals(ExtractionEngine.TIER_2_LOCAL_DOCLING, result.tierUsed)
        assertEquals(ExtractionEngine.TIER_2_LOCAL_DOCLING, result.data.sourceEngine)
        assertEquals(ImportDocumentKind.UNKNOWN, result.data.documentKind)
        assertEquals(0, rasterizer.callCount)
        assertContentEquals(imageBytes, runner.lastAnalyzedImageBytes)
        assertEquals(1, runner.callCount)
    }

    @Test
    fun directImageRejectsAnInvalidFormatBeforeCallingTheRunner() = runTest {
        val runner = ControlledOrchestrationTestVlmRunner(ExtractedDocumentData())
        val orchestrator = HybridDocumentOrchestrator(
            vlmRunner = runner,
            capabilityGate = FakeDeviceCapabilityGate(totalRamGb = 4.0),
            modelStorage = InMemoryModelStorage(downloaded = true),
            enforceHardwareGate = false,
            supportsDirectImageInputs = true
        )

        val result = orchestrator.processImageDocument(
            imageBytes = "%PDF-1.4".encodeToByteArray(),
            format = VisualDocumentFormat.PNG,
            framework = RegulatoryFramework.GENERIC
        )

        assertTrue(result is OrchestrationResult.Error)
        assertEquals(0, runner.callCount)
    }

    private fun pngHeader(width: Int, height: Int): ByteArray = ByteArray(24).apply {
        byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a).copyInto(this)
        this[11] = 13
        "IHDR".encodeToByteArray().copyInto(this, 12)
        for (index in 0..3) {
            this[16 + index] = (width ushr ((3 - index) * 8)).toByte()
            this[20 + index] = (height ushr ((3 - index) * 8)).toByte()
        }
    }

    @Test
    fun oversizedPdfIsRejectedBeforeRasterization() = runTest {
        val rasterizer = InMemoryPageRasterizer()
        val vlmRunner = FakeVlmVisionRunner()
        val orchestrator = HybridDocumentOrchestrator(
            rasterizer = rasterizer,
            vlmRunner = vlmRunner,
            capabilityGate = FakeDeviceCapabilityGate(totalRamGb = 8.0),
            modelStorage = InMemoryModelStorage(downloaded = true, sizeBytes = GemmaModelArtifact.sizeBytes)
        )

        val result = orchestrator.processDocument(
            pdfBytes = ByteArray(MAX_INGESTION_PDF_BYTES + 1),
            rawText = "",
            framework = RegulatoryFramework.COLOMBIA_SFC,
            forceTier2 = true
        )

        val failure = result as? OrchestrationResult.Error
            ?: fail("Expected oversized PDF error before rasterization, got $result")
        assertContains(failure.message, "25 MiB")
        assertEquals(0, rasterizer.callCount)
        assertEquals(0, vlmRunner.callCount)
    }

    @Test
    fun invalidPdfHeaderIsRejectedBeforeRasterizationOrVision() = runTest {
        val rasterizer = InMemoryPageRasterizer()
        val vlmRunner = ControlledOrchestrationTestVlmRunner(ExtractedDocumentData())
        val orchestrator = HybridDocumentOrchestrator(
            rasterizer = rasterizer,
            vlmRunner = vlmRunner,
            capabilityGate = FakeDeviceCapabilityGate(totalRamGb = 8.0),
            modelStorage = InMemoryModelStorage(downloaded = true, sizeBytes = GemmaModelArtifact.sizeBytes)
        )

        val result = orchestrator.processDocument(
            pdfBytes = "%PDF invalid header".encodeToByteArray(),
            rawText = "",
            framework = RegulatoryFramework.COLOMBIA_SFC,
            forceTier2 = true
        )

        assertIs<OrchestrationResult.Error>(result)
        assertEquals(0, rasterizer.callCount)
        assertEquals(0, vlmRunner.callCount)
    }

    @Test
    fun testHardwareGateBlocksTier2WhenRamLessThan8GiB() = runTest {
        val gate = FakeDeviceCapabilityGate(totalRamGb = 4.0) // Ineligible device (< 8 GiB)
        val storage = InMemoryModelStorage(downloaded = true, sizeBytes = GemmaModelArtifact.sizeBytes)
        val rasterizer = InMemoryPageRasterizer()
        val vlmRunner = FakeVlmVisionRunner()

        val orchestrator = HybridDocumentOrchestrator(
            rasterizer = rasterizer,
            vlmRunner = vlmRunner,
            capabilityGate = gate,
            modelStorage = storage
        )

        val result = orchestrator.processDocument(
            pdfBytes = ByteArray(100),
            rawText = "", // Scanned / empty document needing Tier 2
            framework = RegulatoryFramework.COLOMBIA_SFC,
            targetMode = ImportTargetMode.SCENARIO
        )

        assertTrue(result is OrchestrationResult.HardwareRestricted, "Expected HardwareRestricted but got $result")
        assertTrue(
            result.reason.contains("LMKD") || result.reason.contains("8"),
            "HardwareRestricted message should reference 8.0 GB or LMKD risk: '${result.reason}'"
        )

        // Must not invoke heavy vision pipeline on ineligible hardware
        assertEquals(0, rasterizer.callCount)
        assertEquals(0, vlmRunner.callCount)
    }

    @Test
    fun testMissingModelTriggersRequiresModelDownload() = runTest {
        val gate = FakeDeviceCapabilityGate(totalRamGb = 8.0) // Eligible
        val storage = InMemoryModelStorage(downloaded = false, sizeBytes = 0L) // Model NOT downloaded
        val rasterizer = InMemoryPageRasterizer()
        val vlmRunner = FakeVlmVisionRunner()

        val orchestrator = HybridDocumentOrchestrator(
            rasterizer = rasterizer,
            vlmRunner = vlmRunner,
            capabilityGate = gate,
            modelStorage = storage
        )

        val result = orchestrator.processDocument(
            pdfBytes = ByteArray(100),
            rawText = "",
            framework = RegulatoryFramework.COLOMBIA_SFC,
            targetMode = ImportTargetMode.SCENARIO
        )

        assertTrue(result is OrchestrationResult.RequiresModelDownload, "Expected RequiresModelDownload but got $result")
        assertTrue(
            result.message.contains("Gemma") || result.message.contains("descargado"),
            "Message should reference missing model download: '${result.message}'"
        )

        assertEquals(0, rasterizer.callCount)
        assertEquals(0, vlmRunner.callCount)
    }

    @Test
    fun testJsonGrammarParserCleansMarkdownAndExtractsValidExtractedDocumentData() {
        val rawLlmResponse = """
            ```json
            {
              "bankName": "Bancolombia S.A.",
              "principal": "30,000,000.00",
              "ratePercentage": "19.50",
              "rateKind": "annual",
              "termMonths": 48,
              "statementMonth": 12,
              "statementCutDate": "2026-10",
              "monthlyPayment": "850,000.00",
              "principalPaid": "350,000.00",
              "interestPaid": "477,500.00",
              "insurancePaid": "22,500.00",
              "finalBalance": "24,150,000.00",
              "insurances": [
                {
                  "name": "Seguro deudores",
                  "amount": "12,500.00"
                },
                {
                  "name": "Seguro de vida grupo",
                  "amount": "10,000.00"
                }
              ]
            }
            ```
        """.trimIndent()

        val extracted = VlmJsonGrammar.parseVlmResponse(
            rawOutput = rawLlmResponse,
            framework = RegulatoryFramework.COLOMBIA_SFC,
            targetMode = ImportTargetMode.SCENARIO,
            pageIndex = 2
        )

        assertEquals(ExtractionEngine.TIER_2_LOCAL_VLM, extracted.sourceEngine)
        assertEquals(ExtractionEngine.TIER_2_LOCAL_VLM, extracted.engine)
        assertEquals(2, extracted.analyzedPage)
        assertEquals("Bancolombia S.A.", extracted.bankNameCandidate)
        assertNull(extracted.originalPrincipal)
        assertEquals("30000000.00", extracted.unclassifiedPrincipal)
        assertEquals("24150000.00", extracted.closingBalance)
        assertNotNull(extracted.rate)
        assertEquals("19.50", extracted.rate?.valuePercent)
        assertEquals("annual", extracted.rate?.kind)
        assertEquals(48, extracted.termMonths)
        assertEquals(12, extracted.statementMonth)
        assertEquals("2026-10", extracted.statementCutDate)
        assertEquals("850000.00", extracted.monthlyPayment)
        assertEquals("350000.00", extracted.principalPaid)
        assertEquals("477500.00", extracted.interestCharged)
        assertEquals("22500.00", extracted.totalInsurance)
        assertEquals(2, extracted.insurances.size)
        assertEquals("Seguro deudores", extracted.insurances[0].name)
        assertEquals("12500.00", extracted.insurances[0].monthlyCost)
        assertEquals("Seguro de vida grupo", extracted.insurances[1].name)
        assertEquals("10000.00", extracted.insurances[1].monthlyCost)
        assertTrue(extracted.confidence >= 0.9f)
    }

    @Test
    fun forceTier2RoutesHighConfidenceTextThroughControlledStub() = runTest {
        val gate = FakeDeviceCapabilityGate(totalRamGb = 8.0)
        val storage = InMemoryModelStorage(downloaded = true, sizeBytes = GemmaModelArtifact.sizeBytes)
        val rasterizer = InMemoryPageRasterizer()
        val vlmRunner = ControlledOrchestrationTestVlmRunner(
            ExtractedDocumentData(
                sourceEngine = ExtractionEngine.TIER_2_LOCAL_VLM,
                documentKind = ImportDocumentKind.LOAN_STATEMENT,
                detectedCurrencies = setOf(com.finsim.domain.Currency.COP),
                originalPrincipal = "30000000.00",
                rate = ExtractedRate(valuePercent = "19.50", kind = "annual")
            )
        )

        val orchestrator = HybridDocumentOrchestrator(
            rasterizer = rasterizer,
            vlmRunner = vlmRunner,
            capabilityGate = gate,
            modelStorage = storage
        )

        val result = orchestrator.processDocument(
            pdfBytes = "%PDF-1.4\n".encodeToByteArray(),
            rawText = sampleColombiaDigitalText,
            framework = RegulatoryFramework.COLOMBIA_SFC,
            targetMode = ImportTargetMode.SCENARIO,
            forceTier2 = true
        )

        assertTrue(result is OrchestrationResult.Success)
        assertEquals(ExtractionEngine.TIER_2_LOCAL_VLM, result.tierUsed)
        assertEquals(1, rasterizer.callCount)
        assertEquals(1, vlmRunner.callCount)
        assertContentEquals(ByteArray(1024) { (it % 256).toByte() }, vlmRunner.lastAnalyzedImageBytes)
    }

    @Test
    fun cancellationFromVisionRunnerPropagatesToCaller() = runTest {
        val orchestrator = HybridDocumentOrchestrator(
            rasterizer = InMemoryPageRasterizer(),
            vlmRunner = object : VlmVisionRunner {
                override suspend fun isModelReady(): Boolean = true

                override suspend fun analyzePage(
                    imageBytes: ByteArray,
                    framework: RegulatoryFramework,
                    targetMode: ImportTargetMode
                ): ExtractedDocumentData = throw CancellationException("user closed import")
            },
            capabilityGate = FakeDeviceCapabilityGate(totalRamGb = 8.0),
            modelStorage = InMemoryModelStorage(downloaded = true, sizeBytes = GemmaModelArtifact.sizeBytes)
        )

        var cancellationPropagated = false
        try {
            orchestrator.processDocument(
                pdfBytes = "%PDF-1.4\nsynthetic fixture".encodeToByteArray(),
                rawText = "",
                framework = RegulatoryFramework.COLOMBIA_SFC,
                forceTier2 = true
            )
        } catch (_: CancellationException) {
            cancellationPropagated = true
        }

        assertTrue(cancellationPropagated, "Cancellation must remain cancellation, not an extraction error")
    }

    @Test
    fun missingVlmInsuranceAmountRemainsAbsentAndDoesNotAuthorizeImport() {
        val extracted = VlmJsonGrammar.parseVlmResponse(
            rawOutput = """{"insurances":[{"name":"Seguro de vida","amount":null}],"currencies":["ARS","USD"],"documentKind":"ACCOUNT_SUMMARY"}""",
            framework = RegulatoryFramework.ARGENTINA_BCRA
        )

        assertEquals(ImportDocumentKind.ACCOUNT_SUMMARY, extracted.documentKind)
        assertEquals(setOf(com.finsim.domain.Currency.ARS, com.finsim.domain.Currency.USD), extracted.detectedCurrencies)
        assertEquals(1, extracted.insurances.size)
        assertNull(extracted.insurances.single().monthlyCost)
        assertEquals(InsuranceAmountMeaning.UNKNOWN, extracted.insurances.single().amountMeaning)
        assertNull(extracted.totalInsurance)
        assertFailsWith<IllegalArgumentException> { extracted.toScenario() }
        assertFailsWith<IllegalArgumentException> { extracted.toStatementRow() }
    }

    @Test
    fun vlmOrchestrationRejectsJsonMoneyWithExcessPrecisionBeforeNormalization() = runTest {
        val payload = """{"principal":12.34,"ratePercentage":"18.5","interestPaid":1.005}"""
        val rasterizer = InMemoryPageRasterizer()
        val vlmRunner = object : VlmVisionRunner {
            override suspend fun isModelReady(): Boolean = true

            override suspend fun analyzePage(
                imageBytes: ByteArray,
                framework: RegulatoryFramework,
                targetMode: ImportTargetMode
            ): ExtractedDocumentData = VlmJsonGrammar.parseVlmResponse(
                rawOutput = payload,
                framework = framework,
                targetMode = targetMode
            )
        }
        val orchestrator = HybridDocumentOrchestrator(
            rasterizer = rasterizer,
            vlmRunner = vlmRunner,
            capabilityGate = FakeDeviceCapabilityGate(totalRamGb = 8.0),
            modelStorage = InMemoryModelStorage(downloaded = true, sizeBytes = GemmaModelArtifact.sizeBytes)
        )

        val result = orchestrator.processDocument(
            pdfBytes = "%PDF-1.4\nsynthetic fixture".encodeToByteArray(),
            rawText = "",
            framework = RegulatoryFramework.COLOMBIA_SFC,
            targetMode = ImportTargetMode.SCENARIO,
            forceTier2 = true
        )

        assertTrue(result is OrchestrationResult.Error, "Excess-precision VLM amount must be rejected, got $result")
        assertContains(result.message, "dos decimales")
        assertEquals(1, rasterizer.callCount)
    }

    @Test
    fun vlmParserRejectsExcessPrecisionAcrossEveryMonetaryJsonField() {
        val invalidPayloads = listOf(
            """{"principal":1.005}""",
            """{"originalPrincipal":1.005}""",
            """{"capitalBalance":1.005}""",
            """{"compoundBalance":1.005}""",
            """{"finalBalance":1.005}""",
            """{"monthlyPayment":1.005}""",
            """{"basePayment":1.005}""",
            """{"totalPayment":1.005}""",
            """{"principalPaid":1.005}""",
            """{"interestPaid":1.005}""",
            """{"insurancePaid":1.005}""",
            """{"insurances":[{"name":"Seguro","amount":1.005}]}"""
        )

        invalidPayloads.forEach { payload ->
            val error = assertFailsWith<IllegalArgumentException>(payload) {
                VlmJsonGrammar.parseVlmResponse(
                    rawOutput = payload,
                    framework = RegulatoryFramework.COLOMBIA_SFC
                )
            }
            assertTrue(error.message.orEmpty().contains("dos decimales"), payload)
        }
    }

    @Test
    fun vlmParserPreservesCanonicalAmountsAndRejectsAmbiguousLocalizedStrings() {
        val extracted = VlmJsonGrammar.parseVlmResponse(
            rawOutput = """{"principal":12.34,"originalPrincipal":-12.34,"monthlyPayment":"30.000.000,00","principalPaid":-12.34,"interestPaid":12.34,"insurances":[{"name":"Seguro","amount":12.34},{"name":"Sin importe","amount":null}]}""",
            framework = RegulatoryFramework.COLOMBIA_SFC
        )

        assertEquals("-12.34", extracted.originalPrincipal)
        assertEquals("30000000.00", extracted.monthlyPayment)
        assertEquals("-12.34", extracted.principalPaid)
        assertEquals("12.34", extracted.interestCharged)
        assertEquals("12.34", extracted.insurances[0].monthlyCost)
        assertNull(extracted.totalInsurance)
        assertNull(extracted.closingBalance)
        assertNull(extracted.insurances[1].monthlyCost)

        listOf("1.005", "1,005").forEach { ambiguous ->
            val error = assertFailsWith<IllegalArgumentException> {
                VlmJsonGrammar.parseVlmResponse(
                    rawOutput = """{"principal":"$ambiguous"}""",
                    framework = RegulatoryFramework.COLOMBIA_SFC
                )
            }
            assertTrue(error.message.orEmpty().contains("ambiguo"), ambiguous)
        }
    }

    @Test
    fun nominalRateFrequencyMustBeExplicitInVlmPayload() {
        val absent = VlmJsonGrammar.parseVlmResponse(
            rawOutput = """{"ratePercentage":"48","rateKind":"nominal"}""",
            framework = RegulatoryFramework.ARGENTINA_BCRA
        )
        val supplied = VlmJsonGrammar.parseVlmResponse(
            rawOutput = """{"ratePercentage":"48","rateKind":"nominal","nominalFrequency":4}""",
            framework = RegulatoryFramework.ARGENTINA_BCRA
        )

        assertEquals("nominal", absent.rate?.kind)
        assertNull(absent.rate?.nominalFrequency)
        assertEquals(4, supplied.rate?.nominalFrequency)
    }
}
