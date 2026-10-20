package com.finsim.ingestion

import kotlinx.coroutines.test.runTest
import kotlin.test.*

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
        val storage = InMemoryModelStorage(downloaded = true, sizeBytes = 1100000000L)
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
    fun testEmptyOrScannedDocumentTriggersTier2WhenModelDownloadedAndHardwareEligible() = runTest {
        val gate = FakeDeviceCapabilityGate(totalRamGb = 8.0)
        val storage = InMemoryModelStorage(downloaded = true, sizeBytes = 1100000000L)
        val rasterizer = InMemoryPageRasterizer()
        val vlmRunner = FakeVlmVisionRunner()

        val orchestrator = HybridDocumentOrchestrator(
            rasterizer = rasterizer,
            vlmRunner = vlmRunner,
            capabilityGate = gate,
            modelStorage = storage
        )

        // Empty raw text simulates a scanned PDF where digital text layer is absent
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
        assertEquals(1, vlmRunner.callCount, "VLM runner should be invoked once")
    }

    @Test
    fun testHardwareGateBlocksTier2WhenRamLessThan8GiB() = runTest {
        val gate = FakeDeviceCapabilityGate(totalRamGb = 4.0) // Ineligible device (< 8 GiB)
        val storage = InMemoryModelStorage(downloaded = true, sizeBytes = 1100000000L)
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
        assertEquals("30000000.00", extracted.originalPrincipal)
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
    fun testForceTier2BypassesTier1EvenWithHighConfidenceDigitalText() = runTest {
        val gate = FakeDeviceCapabilityGate(totalRamGb = 8.0)
        val storage = InMemoryModelStorage(downloaded = true, sizeBytes = 1100000000L)
        val rasterizer = InMemoryPageRasterizer()
        val vlmRunner = FakeVlmVisionRunner()

        val orchestrator = HybridDocumentOrchestrator(
            rasterizer = rasterizer,
            vlmRunner = vlmRunner,
            capabilityGate = gate,
            modelStorage = storage
        )

        val result = orchestrator.processDocument(
            pdfBytes = ByteArray(50),
            rawText = sampleColombiaDigitalText,
            framework = RegulatoryFramework.COLOMBIA_SFC,
            targetMode = ImportTargetMode.SCENARIO,
            forceTier2 = true
        )

        assertTrue(result is OrchestrationResult.Success)
        assertEquals(ExtractionEngine.TIER_2_LOCAL_VLM, result.tierUsed)
        assertEquals(1, rasterizer.callCount)
        assertEquals(1, vlmRunner.callCount)
    }
}
