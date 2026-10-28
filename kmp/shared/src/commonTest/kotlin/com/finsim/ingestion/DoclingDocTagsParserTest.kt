package com.finsim.ingestion

import com.finsim.domain.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class DoclingDocTagsParserTest {
    @Test
    fun parsesDocTagsWithoutCombiningBasePaymentAndInsurance() {
        val extracted = DoclingDocTagsParser.parse(
            """
                <doctag>
                  <text>Extracto de préstamo COP</text>
                  <text>Capital original: $ 1.000,00</text>
                  <text>Saldo de capital: $ 502,49</text>
                  <text>Plazo original: 2 meses</text>
                  <text>Plazo restante: 1 mes</text>
                  <text>Tasa efectiva mensual: 1,00%</text>
                  <text>Cuota base: $ 507,51</text>
                  <text>Cuota total con seguros: $ 527,51</text>
                  <text>Seguro de vida mensual: $ 20,00</text>
                </doctag>
            """.trimIndent(),
            RegulatoryFramework.COLOMBIA_SFC
        )

        assertEquals(ImportDocumentKind.LOAN_STATEMENT, extracted.documentKind)
        assertEquals(setOf(Currency.COP), extracted.detectedCurrencies)
        assertEquals("1000.00", extracted.originalPrincipal)
        assertEquals("502.49", extracted.capitalBalance)
        assertEquals(BalanceMeaning.PRINCIPAL, extracted.balanceMeaning)
        assertEquals(2, extracted.originalTermMonths)
        assertEquals(1, extracted.remainingTermMonths)
        assertEquals(ExtractedRate("1.00", "monthly"), extracted.rate)
        assertEquals("507.51", extracted.basePayment)
        assertEquals("527.51", extracted.totalPayment)
        assertEquals(PaymentMeaning.BASE_PAYMENT, extracted.paymentMeaning)
        assertEquals("20.00", extracted.totalInsurance)
        assertEquals(InsuranceAmountMeaning.PERIODIC_CHARGE, extracted.insurances.single().amountMeaning)
        assertEquals("20.00", extracted.insurances.single().monthlyCost)
    }

    @Test
    fun leavesInsuranceAmountMeaningUnknownWithoutPeriodicEvidence() {
        val extracted = DoclingDocTagsParser.parse(
            "<doctag><text>Seguro de vida: $ 20,00</text></doctag>",
            RegulatoryFramework.COLOMBIA_SFC
        )

        assertEquals(InsuranceAmountMeaning.UNKNOWN, extracted.insurances.single().amountMeaning)
        assertNull(extracted.basePayment)
        assertNull(extracted.totalPayment)
    }

    @Test
    fun keepsOtslCellsAndRowsSeparatedWhenReadingFinancialLabels() {
        val extracted = DoclingDocTagsParser.parse(
            """
                <doctag><text>Extracto de préstamo COP</text>
                <otsl><fcel>Saldo de capital: <fcel>$ 502,49<nl>
                <fcel>Cuota base: <fcel>$ 507,51<nl>
                <fcel>Cuota total con seguros: <fcel>$ 527,51<nl>
                <fcel>Seguro de vida mensual: <fcel>$ 20,00</otsl></doctag>
            """.trimIndent(),
            RegulatoryFramework.COLOMBIA_SFC
        )

        assertEquals("502.49", extracted.capitalBalance)
        assertEquals("507.51", extracted.basePayment)
        assertEquals("527.51", extracted.totalPayment)
        assertEquals("20.00", extracted.totalInsurance)
        assertEquals(8, extracted.rawExtractedText!!.split("<fcel>").size - 1)
    }

    @Test
    fun keepsAdjacentOtslCellMarkersFromJoiningTheLabelAndAmount() {
        val extracted = DoclingDocTagsParser.parse(
            "<doctag><otsl><fcel>Saldo de capital<fcel>$ 502,49<nl></otsl></doctag>",
            RegulatoryFramework.COLOMBIA_SFC
        )

        assertEquals("502.49", extracted.capitalBalance)
    }

    @Test
    fun keepsAllOtslCellMarkersAsSeparatorsWithoutClosingTags() {
        val extracted = DoclingDocTagsParser.parse(
            """<doctag><otsl>
                <fcel>Cuota<ecel/>base<fcel>$ 507,51<nl>
                <fcel>Cuota<lcel/>total con seguros<fcel>$ 527,51<nl>
                <fcel>Seguro<ucel/>vida mensual<fcel>$ 20,00<nl>
                <fcel>Capital<xcel/>original<fcel>$ 1.000,00<nl>
                </otsl></doctag>""",
            RegulatoryFramework.COLOMBIA_SFC
        )

        assertEquals("507.51", extracted.basePayment)
        assertEquals("527.51", extracted.totalPayment)
        assertEquals("20.00", extracted.totalInsurance)
        assertEquals("1000.00", extracted.originalPrincipal)
    }

    @Test
    fun withholdsPaymentWhenOneTableRowHasTwoUnlabeledInsuranceColumnAmounts() {
        val extracted = DoclingDocTagsParser.parse(
            """<doctag><otsl>
                <fcel>Concepto<fcel>Sin seguros<fcel>Con seguros<nl>
                <fcel>Cuota mensual<fcel>507,51<fcel>527,51<nl>
                </otsl></doctag>""",
            RegulatoryFramework.COLOMBIA_SFC
        )

        assertNull(extracted.basePayment)
        assertNull(extracted.monthlyPayment)
        assertNull(extracted.totalPayment)
        assertEquals(PaymentMeaning.UNKNOWN, extracted.paymentMeaning)
        assertContains(extracted.reviewWarnings, "Se detectaron varias cuotas en una fila sin una columna inequívoca; revisa cuál corresponde al préstamo.")
    }

    @Test
    fun rejectsTextThatIsNotDocTagsOutput() {
        val error = assertFailsWith<IllegalArgumentException> {
            DoclingDocTagsParser.parse("!!!!!!!!!!!!!!!!", RegulatoryFramework.COLOMBIA_SFC)
        }

        assertContains(error.message.orEmpty(), "DocTags")
    }

    @Test
    fun withholdsConflictingCandidateValuesAndAddsHumanReviewWarning() {
        val extracted = DoclingDocTagsParser.parse(
            """
                <doctag><text>Extracto crédito COP</text>
                <text>Capital original: $ 1.000,00</text>
                <text>Capital original: $ 900,00</text></doctag>
            """.trimIndent(),
            RegulatoryFramework.COLOMBIA_SFC
        )

        assertNull(extracted.originalPrincipal)
        assertContains(extracted.reviewWarnings, "Se detectaron montos originales diferentes; revisa el capital original en el documento.")
    }
}
