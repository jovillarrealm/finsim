package com.finsim.ingestion

import com.finsim.core.Decimal
import com.finsim.domain.Currency
import com.finsim.domain.Rate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DocumentTextParserTest {

    @Test
    fun testPageDensityScanner_selectsBestPage() {
        val page1 = """
            Términos y condiciones legales generales.
            Este documento constituye información de privacidad conforme a las leyes vigentes.
            Para mayor información consulte los canales virtuales y la página web institucional.
        """.trimIndent()

        val page2 = """
            Bancolombia - Extracto de Crédito de Consumo
            Detalle del préstamo y condiciones financieras:
            Saldo de capital: $ 25.000.000,00
            Capital inicial desembolsado: $ 30.000.000,00
            Tasa Efectiva Anual (E.A.): 18.50%
            Tasa Mes Vencido (M.V.): 1.42%
            Plazo pactado: 36 meses
            Valor cuota mensual: $ 890.500,00
            Seguro deudores: $ 15.400,00
            Seguro de vida: $ 8.200,00
            Fecha de corte: 15/10/2026
        """.trimIndent()

        val page3 = """
            Canales de atención al cliente y red de cajeros automáticos.
            Horarios de atención de lunes a viernes de 8:00 a 16:00.
        """.trimIndent()

        val bestIndex = PageDensityScanner.findBestPageIndex(listOf(page1, page2, page3))
        assertEquals(2, bestIndex, "La página 2 debe tener la mayor densidad de términos financieros")

        val scores = PageDensityScanner.scorePages(listOf(page1, page2, page3))
        assertEquals(3, scores.size)
        assertTrue(scores[1].score > scores[0].score)
        assertTrue(scores[1].score > scores[2].score)
    }

    @Test
    fun testColombianSfcRateAndBalanceExtraction() {
        val statement = """
            Bancolombia
            Extracto Crédito Libre Destino
            Fecha de corte: 15/10/2026
            Capital inicial: $ 30.000.000,00
            Saldo de capital: $ 24.500.000,00
            Tasa Efectiva Anual (E.A.): 19.50%
            Tasa Mes Vencido (M.V.): 1.50%
            Plazo en meses: 48
            Valor cuota mensual: $ 850.000,00
            Seguro deudores: $ 12.500,00
            Seguro de vida: $ 10.000,00
        """.trimIndent()

        val extracted = DocumentTextParser.parse(statement, RegulatoryFramework.COLOMBIA_SFC, pageIndex = 1)

        assertEquals("Bancolombia", extracted.bankNameCandidate)
        assertEquals("24500000.00", extracted.closingBalance)
        assertEquals("30000000.00", extracted.originalPrincipal)
        assertEquals(48, extracted.termMonths)
        assertEquals("850000.00", extracted.monthlyPayment)
        assertEquals("2026-10", extracted.statementCutDate)

        assertNotNull(extracted.rate)
        assertEquals("19.50", extracted.rate.valuePercent)
        assertEquals("annual", extracted.rate.kind)

        assertEquals(2, extracted.insurances.size)
        assertEquals("Seguro deudores", extracted.insurances[0].name)
        assertEquals("12500.00", extracted.insurances[0].monthlyCost)
        assertEquals("Seguro de vida", extracted.insurances[1].name)
        assertEquals("10000.00", extracted.insurances[1].monthlyCost)

        // Convert to domain Scenario and validate
        val scenario = extracted.toScenario(framework = RegulatoryFramework.COLOMBIA_SFC)
        scenario.validate()

        assertEquals(Currency.COP, scenario.currency)
        assertEquals("24500000.00", scenario.principal)
        assertEquals(48, scenario.termMonths)
        assertEquals("2026-10", scenario.startDate)
        assertTrue(scenario.rate is Rate.Annual)
        val rateRatio = Decimal.parse(scenario.rate.value)
        assertEquals("0.195", rateRatio.toString())
    }

    @Test
    fun testMexicanCondusefCatAndOrdinariaRate() {
        val statement = """
            BBVA México
            Estado de Cuenta Préstamo Personal
            Fecha de corte: 2026-10-01
            Saldo insoluto: $ 120,000.00
            Tasa de interés ordinaria anual fija: 22.50%
            CAT Promedio: 26.8% sin IVA
            Plazo: 24 meses
            Pago mensual: $ 6,250.00
            Seguro de desempleo: $ 450.00
        """.trimIndent()

        val extracted = DocumentTextParser.parse(statement, RegulatoryFramework.MEXICO_CONDUSEF, pageIndex = 1)

        assertEquals("BBVA México", extracted.bankNameCandidate)
        assertEquals("120000.00", extracted.closingBalance)
        assertEquals(24, extracted.termMonths)
        assertEquals("6250.00", extracted.monthlyPayment)
        assertEquals("2026-10", extracted.statementCutDate)

        // Must prioritize contractual Tasa Ordinaria over CAT
        assertNotNull(extracted.rate)
        assertEquals("22.50", extracted.rate.valuePercent)
        assertEquals("annual", extracted.rate.kind)

        assertEquals(1, extracted.insurances.size)
        assertEquals("Seguro de desempleo", extracted.insurances[0].name)
        assertEquals("450.00", extracted.insurances[0].monthlyCost)

        val scenario = extracted.toScenario(framework = RegulatoryFramework.MEXICO_CONDUSEF)
        scenario.validate()
        assertEquals(24, scenario.termMonths)
        assertEquals("120000.00", scenario.principal)
    }

    @Test
    fun testMexicanCondusefFallbackToCatWhenNoOrdinaria() {
        val statement = """
            Santander México
            Estado de cuenta de Crédito
            Saldo insoluto: $ 50,000.00
            CAT Promedio: 28.5%
            Plazo: 12 meses
        """.trimIndent()

        val extracted = DocumentTextParser.parse(statement, RegulatoryFramework.MEXICO_CONDUSEF)
        assertNotNull(extracted.rate)
        assertEquals("28.5", extracted.rate.valuePercent)
        assertEquals("annual", extracted.rate.kind)
        assertEquals("50000.00", extracted.closingBalance)
        assertEquals(12, extracted.termMonths)
    }

    @Test
    fun testArgentinaBcraRatesAndNominalHandling() {
        val statement = """
            Banco Galicia
            Resumen de Cuenta Préstamo
            Saldo deudor: $ 850.000,50
            TNA: 48.00%
            TEA: 60.10%
            CFT TEA: 78.50%
            Plazo: 36 meses
            Cuota mensual: $ 45.200,00
            Seguro de vida: $ 1.850,00
            Fecha de emisión: 10/10/2026
        """.trimIndent()

        val extracted = DocumentTextParser.parse(statement, RegulatoryFramework.ARGENTINA_BCRA)

        assertEquals("Banco Galicia", extracted.bankNameCandidate)
        assertEquals("850000.50", extracted.closingBalance)
        assertEquals(36, extracted.termMonths)

        // BCRA prefers TNA for contractual nominal rate
        assertNotNull(extracted.rate)
        assertEquals("48.00", extracted.rate.valuePercent)
        assertEquals("nominal", extracted.rate.kind)
        assertEquals(12, extracted.rate.nominalFrequency)

        val scenario = extracted.toScenario(framework = RegulatoryFramework.ARGENTINA_BCRA)
        scenario.validate()
        assertEquals(Currency.ARS, scenario.currency)
        assertTrue(scenario.rate is Rate.Nominal)
        assertEquals(12, (scenario.rate as Rate.Nominal).frequency)
    }

    @Test
    fun testInMemoryPdfTextExtractor() {
        val predefined = listOf("Página 1: Introducción", "Página 2: Saldo y tasa")
        val extractor = InMemoryPdfTextExtractor(predefined)
        val result = extractor.extractPages(byteArrayOf())
        assertEquals(2, result.size)
        assertEquals("Página 1: Introducción", result[0])
        assertEquals("Página 2: Saldo y tasa", result[1])
    }

    @Test
    fun testDigitalPdfTextExtractor_plainTextFallback() {
        val rawDoc = "Extracto línea 1\u000CExtracto línea 2"
        val pages = DigitalPdfTextExtractor.extractPages(rawDoc.encodeToByteArray())
        assertEquals(2, pages.size)
        assertEquals("Extracto línea 1", pages[0])
        assertEquals("Extracto línea 2", pages[1])
    }
}
