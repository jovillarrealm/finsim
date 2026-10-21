package com.finsim.ingestion

import com.finsim.core.Decimal
import com.finsim.domain.Currency
import com.finsim.domain.Insurance
import com.finsim.domain.Rate
import com.finsim.domain.Scenario
import com.finsim.engine.simulateLoan
import com.finsim.reconciliation.reconcileRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
            Cargo mensual de seguro deudores: $ 12.500,00
            Prima mensual de seguro de vida: $ 10.000,00
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
        val reviewed = extracted.copy(
            remainingTermMonths = extracted.termMonths,
            termMeaning = TermMeaning.REMAINING,
            basePayment = extracted.monthlyPayment,
            paymentMeaning = PaymentMeaning.BASE_PAYMENT
        )
        val scenario = reviewed.toScenario(
            framework = RegulatoryFramework.COLOMBIA_SFC,
            startDate = "2026-11",
            confirmCurrencyAssumption = true
        )
        scenario.validate()

        assertEquals(Currency.COP, scenario.currency)
        assertEquals("24500000.00", scenario.principal)
        assertEquals(48, scenario.termMonths)
        assertEquals("2026-11", scenario.startDate)
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
            Prima mensual de seguro de desempleo: $ 450.00
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

        val reviewed = extracted.copy(
            remainingTermMonths = extracted.termMonths,
            termMeaning = TermMeaning.REMAINING,
            basePayment = extracted.monthlyPayment,
            paymentMeaning = PaymentMeaning.BASE_PAYMENT
        )
        val scenario = reviewed.toScenario(
            framework = RegulatoryFramework.MEXICO_CONDUSEF,
            startDate = "2026-11",
            confirmCurrencyAssumption = true
        )
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
            Extracto de Préstamo
            Saldo de capital: $ 850.000,50
            TNA: 48.00%
            TEA: 60.10%
            CFT TEA: 78.50%
            Plazo restante: 36 meses
            Cuota base: $ 45.200,00
            Prima mensual de seguro de vida: $ 1.850,00
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
        assertNull(extracted.rate.nominalFrequency)
        assertFailsWith<IllegalArgumentException> {
            extracted.toScenario(
                framework = RegulatoryFramework.ARGENTINA_BCRA,
                startDate = "2026-11",
                confirmCurrencyAssumption = true
            )
        }

        val reviewed = extracted.copy(rate = extracted.rate.copy(nominalFrequency = 12))
        val scenario = reviewed.toScenario(
            framework = RegulatoryFramework.ARGENTINA_BCRA,
            startDate = "2026-11",
            confirmCurrencyAssumption = true
        )
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

    @Test
    fun testMonthlyStatementExtractionToStatementRowAndReconciliation() {
        val statement = """
            Bancolombia S.A. - Extracto Mensual de Préstamo
            Fecha de corte: 15/10/2026
            Cuota 12 de 48
            Saldo a capital: $ 24.150.000,00
            Abono a capital: $ 350.000,00
            Interés corriente: $ 477.500,00
            Cargo mensual de seguro deudores: $ 12.500,00
            Prima mensual de seguro de vida grupo: $ 10.000,00
            Total seguros: $ 22.500,00
        """.trimIndent()

        val extracted = DocumentTextParser.parse(statement, RegulatoryFramework.COLOMBIA_SFC)

        assertEquals(12, extracted.statementMonth)
        assertEquals("350000.00", extracted.principalPaid)
        assertEquals("477500.00", extracted.interestCharged)
        assertEquals("22500.00", extracted.totalInsurance)
        assertEquals("24150000.00", extracted.closingBalance)

        val stmtRow = extracted.toStatementRow()
        assertEquals(12, stmtRow.month)
        assertEquals("350000.00", stmtRow.principal)
        assertEquals("477500.00", stmtRow.interest)
        assertEquals("22500.00", stmtRow.insurance)
        assertEquals("24150000.00", stmtRow.balance)

        // Simulate loan scenario and calculate reconciliation differences
        val scenario = Scenario(
            name = "Test Loan",
            principal = "25000000.00",
            startDate = "2025-10",
            termMonths = 48,
            rate = Rate.Monthly("0.015"),
            insurance = listOf(
                Insurance(
                    id = "ins1",
                    name = "Seguros",
                    kind = "fixed",
                    value = "22500.00",
                    endsAtPayoff = true
                )
            )
        )
        val loanResult = simulateLoan(scenario)

        val diffs = reconcileRow(stmtRow, loanResult.rows)
        assertTrue(diffs.isNotEmpty())
        assertEquals(4, diffs.size) // interest, insurance, principal, balance
        val insuranceDiff = diffs.first { it.field == "insurance" }
        assertEquals("Seguro cobrado", insuranceDiff.label)
        assertEquals("22500.00", insuranceDiff.actual)
        assertEquals("22500.00", insuranceDiff.simulated)
        assertEquals("0.00", insuranceDiff.difference)
    }

    @Test
    fun testMexicanStatementRowExtractionAndDifferences() {
        val statement = """
            BBVA México - Estado de Cuenta de Préstamo
            Fecha de corte: 2026-10-01
            Cuota 5 de 24
            Amortización a capital: $ 4,120.00
            Interés ordinario: $ 1,360.00
            Cargo mensual de seguro de desempleo: $ 450.00
            Saldo a capital: $ 115,880.00
        """.trimIndent()

        val extracted = DocumentTextParser.parse(statement, RegulatoryFramework.MEXICO_CONDUSEF)

        val stmtRow = extracted.toStatementRow()
        assertEquals(5, stmtRow.month)
        assertEquals("4120.00", stmtRow.principal)
        assertEquals("1360.00", stmtRow.interest)
        assertEquals("450.00", stmtRow.insurance)
        assertEquals("115880.00", stmtRow.balance)
    }

    @Test
    fun testFallbackMonthHandlingInStatementRow() {
        val statementWithDateOnly = """
            Banco de Bogotá
            Fecha de corte: 2026-08-15
            Saldo final: $ 10.000.000,00
            Abono a capital: $ 200.000,00
            Interés del periodo: $ 150.000,00
            Total seguros: $ 25.000,00
        """.trimIndent()

        val extractedWithDate = DocumentTextParser.parse(statementWithDateOnly, RegulatoryFramework.COLOMBIA_SFC)
        assertNull(extractedWithDate.statementMonth)
        assertFailsWith<IllegalArgumentException> { extractedWithDate.toStatementRow() }
        assertEquals(1, extractedWithDate.toStatementRow(fallbackMonth = 1).month)

        val statementWithoutDateOrCuota = """
            Pago a capital: $ 100.000,00
            Interés corriente: $ 80.000,00
        """.trimIndent()

        val extractedWithoutDate = DocumentTextParser.parse(statementWithoutDateOrCuota, RegulatoryFramework.COLOMBIA_SFC)
        assertEquals(null, extractedWithoutDate.statementMonth)
        // Falls back to fallbackMonth
        assertEquals(3, extractedWithoutDate.toStatementRow(fallbackMonth = 3).month)
        assertEquals("100000.00", extractedWithoutDate.toStatementRow(fallbackMonth = 3).principal)
        assertEquals("80000.00", extractedWithoutDate.toStatementRow(fallbackMonth = 3).interest)
    }

    @Test
    fun importReviewKeepsFinancialMeaningsSeparateAndDoesNotTurnMissingValuesIntoZero() {
        val statement = """
            Extracto de préstamo
            Capital inicial: $ 1,000.00
            Saldo a capital: $ 800.00
            Saldo compuesto: $ 850.00
            Plazo original: 12 meses
            Plazo restante: 8 meses
            Cuota base: $ 100.00
            Cuota total: $ 120.00
            Seguro de vida - valor asegurado: $ 5,000.00
            Prima mensual de seguro: $ 0.00
            Fecha de corte: 2026-10-01
            Tasa efectiva anual: 12.00%
        """.trimIndent()

        val extracted = DocumentTextParser.parse(statement, RegulatoryFramework.COLOMBIA_SFC)

        assertEquals(ImportDocumentKind.LOAN_STATEMENT, extracted.documentKind)
        assertEquals("1000.00", extracted.originalPrincipal)
        assertEquals("800.00", extracted.capitalBalance)
        assertEquals("850.00", extracted.compoundBalance)
        assertEquals(12, extracted.originalTermMonths)
        assertEquals(8, extracted.remainingTermMonths)
        assertEquals("100.00", extracted.basePayment)
        assertEquals("120.00", extracted.totalPayment)
        assertEquals(InsuranceAmountMeaning.INSURED_VALUE, extracted.insurances[0].amountMeaning)
        assertEquals(InsuranceAmountMeaning.PERIODIC_CHARGE, extracted.insurances[1].amountMeaning)
        assertEquals("0.00", extracted.insurances[1].monthlyCost)
        assertNull(extracted.interestCharged)

        val scenario = extracted.toScenario(
            framework = RegulatoryFramework.COLOMBIA_SFC,
            startDate = "2026-11",
            confirmNoInsurance = false,
            confirmCurrencyAssumption = true
        )
        assertEquals("800.00", scenario.principal)
        assertEquals(8, scenario.termMonths)
        assertEquals("100.00", scenario.payment)
        assertEquals(1, scenario.insurance.size)
        assertEquals("0.00", scenario.insurance.single().value)
    }

    @Test
    fun missingInsuranceAmountRemainsMissingInsteadOfBecomingZero() {
        val extracted = DocumentTextParser.parse(
            "Extracto de préstamo\nPrima mensual de seguro de vida",
            RegulatoryFramework.COLOMBIA_SFC
        )

        assertEquals(1, extracted.insurances.size)
        assertNull(extracted.insurances.single().monthlyCost)
        assertEquals(InsuranceAmountMeaning.PERIODIC_CHARGE, extracted.insurances.single().amountMeaning)
        assertNull(extracted.totalInsurance)
    }

    @Test
    fun mixedCurrencyAccountSummaryCannotBecomeLoanOrReconciliationRow() {
        val summary = """
            Resumen de cuenta
            Sección pesos argentinos ARS
            Saldo a capital: $ 800.00
            Plazo restante: 8 meses
            Tasa efectiva anual: 12.00%
            Cuota base: $ 100.00
            Sección dólares USD
            Saldo: US$ 800.00
            Cuota: US$ 100.00
            Cuota 1 de 8
        """.trimIndent()

        val extracted = DocumentTextParser.parse(summary, RegulatoryFramework.ARGENTINA_BCRA)

        assertEquals(ImportDocumentKind.ACCOUNT_SUMMARY, extracted.documentKind)
        assertEquals(setOf(Currency.ARS, Currency.USD), extracted.detectedCurrencies)
        assertFailsWith<IllegalArgumentException> {
            extracted.toScenario(startDate = "2026-11")
        }
        assertFailsWith<IllegalArgumentException> {
            extracted.toStatementRow()
        }
    }

    @Test
    fun singleCurrencyAccountSummaryCannotBecomeLoanOrReconciliationRowEvenWithLoanLikeValues() {
        val summary = """
            Resumen de cuenta
            Sección pesos argentinos ARS
            Saldo de capital: $ 800.00
            Plazo restante: 8 meses
            Tasa efectiva anual: 12.00%
            Cuota base: $ 100.00
            Cuota 1 de 8
        """.trimIndent()

        val extracted = DocumentTextParser.parse(summary, RegulatoryFramework.ARGENTINA_BCRA)

        assertEquals(ImportDocumentKind.ACCOUNT_SUMMARY, extracted.documentKind)
        assertTrue(extracted.importBlockReason()?.contains("resumen de cuenta") == true)
        assertFailsWith<IllegalArgumentException> {
            extracted.toScenario(startDate = "2026-11")
        }
        assertFailsWith<IllegalArgumentException> {
            extracted.toStatementRow()
        }
    }

    @Test
    fun compoundDebtAndUnclassifiedPrincipalCannotSupplyScenarioCapital() {
        val commonReviewedFacts = ExtractedDocumentData(
            documentKind = ImportDocumentKind.LOAN_STATEMENT,
            detectedCurrencies = setOf(Currency.ARS),
            rate = ExtractedRate("12.00", "annual"),
            remainingTermMonths = 8,
            termMeaning = TermMeaning.REMAINING,
            basePayment = "100.00",
            paymentMeaning = PaymentMeaning.BASE_PAYMENT,
            insurances = listOf(ExtractedInsurance("Prima", "0.00", InsuranceAmountMeaning.PERIODIC_CHARGE))
        )

        val compoundOnly = commonReviewedFacts.copy(
            closingBalance = "850.00",
            compoundBalance = "850.00",
            balanceMeaning = BalanceMeaning.COMPOUND_DEBT
        )
        val unclassifiedOnly = commonReviewedFacts.copy(unclassifiedPrincipal = "800.00")

        assertFailsWith<IllegalArgumentException> {
            compoundOnly.toScenario(startDate = "2026-11")
        }
        assertFailsWith<IllegalArgumentException> {
            unclassifiedOnly.toScenario(startDate = "2026-11")
        }
    }

    @Test
    fun missingScenarioFactsRequireExplicitReviewInsteadOfUsingExampleLoan() {
        val extracted = DocumentTextParser.parse(
            "Extracto de préstamo sin condiciones identificables",
            RegulatoryFramework.COLOMBIA_SFC
        ).copy(documentKind = ImportDocumentKind.LOAN_STATEMENT)

        assertFailsWith<IllegalArgumentException> {
            extracted.toScenario()
        }
        assertFailsWith<IllegalArgumentException> {
            extracted.toStatementRow()
        }
    }

    @Test
    fun emptyExtractionCannotCreateLoanThroughExistingMapperSeam() {
        assertFailsWith<IllegalArgumentException> {
            ExtractedDocumentData().toScenario()
        }
    }
}
