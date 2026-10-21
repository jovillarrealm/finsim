package com.finsim.ingestion

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

/** Tests the temporary probe's privacy gate and its local fixture oracle, never model inference. */
class SyntheticProbeComparisonTest {
    @Test
    fun rasterGateRequiresPageAndSourceAndConsumesOnlyTheMatchingRasterOnce() {
        val pdf = "fixture-pdf".encodeToByteArray()
        val otherPdf = "other-pdf".encodeToByteArray()
        val raster = byteArrayOf(1, 2, 3, 4)
        val otherRaster = byteArrayOf(4, 3, 2, 1)
        val gate = SyntheticFixtureRasterGate(pdf.size, sha256(pdf), expectedPageIndex = 1)

        assertNull(gate.beginPdf(pdf, pageIndex = 2))
        assertNull(gate.beginPdf(otherPdf, pageIndex = 1))
        val ticket = assertNotNull(gate.beginPdf(pdf, pageIndex = 1))
        gate.armAfterRaster(ticket, raster, width = 12, height = 18)

        assertNull(gate.consumeForInference(otherRaster))
        assertNull(gate.consumeForInference(raster))

        val nextTicket = assertNotNull(gate.beginPdf(pdf, pageIndex = 1))
        gate.armAfterRaster(nextTicket, raster, width = 12, height = 18)
        val identity = assertNotNull(gate.consumeForInference(raster))
        assertEquals(sha256(pdf), identity.pdfSha256)
        assertEquals(sha256(raster), identity.imageSha256)
        assertEquals(12, identity.width)
        assertEquals(18, identity.height)
        assertNull(gate.consumeForInference(raster))
    }

    @Test
    fun literalControlRequiresTheCorrectAdjacentLabelAmountAndCurrency() {
        assertEquals(
            SyntheticLiteralStatus.MATCH,
            SyntheticFixtureComparison.compareLiteralLine(
                "`Cuota base mensual (capital e\nintereses): 507,51 COP.`"
            )
        )
        assertEquals(
            SyntheticLiteralStatus.MATCH,
            SyntheticFixtureComparison.compareLiteralLine(
                "Cuota base mensual (capital e intereses): 507,51 COP"
            )
        )
        assertEquals(
            SyntheticLiteralStatus.MISMATCH,
            SyntheticFixtureComparison.compareLiteralLine(
                "Cuota base mensual (capital e intereses): 527,51 COP."
            )
        )
        assertEquals(
            SyntheticLiteralStatus.MISMATCH,
            SyntheticFixtureComparison.compareLiteralLine(
                "Cuota total mensual con seguro: 527,51 COP."
            )
        )
        assertEquals(
            SyntheticLiteralStatus.MISMATCH,
            SyntheticFixtureComparison.compareLiteralLine(
                "Cuota base mensual (capital e intereses): 507,50 COP."
            )
        )
        assertEquals(
            SyntheticLiteralStatus.MISMATCH,
            SyntheticFixtureComparison.compareLiteralLine(
                "Cuota base mensual (capital e intereses): 507,51 COPotra."
            )
        )
        assertEquals(
            SyntheticLiteralStatus.MISMATCH,
            SyntheticFixtureComparison.compareLiteralLine(
                "Cuota base mensual (capital e intereses): 507,51 ARS."
            )
        )
        assertEquals(
            SyntheticLiteralStatus.OMITTED,
            SyntheticFixtureComparison.compareLiteralLine("NO_LEGIBLE")
        )
    }

    @Test
    fun structuredComparisonSeparatesPaymentAndInsuranceSemantics() {
        val matched = SyntheticFixtureComparison.compareStructuredFields(expectedExtraction())
        assertEquals(SyntheticFieldStatus.MATCH, matched.getValue("basePayment.direct"))
        assertEquals(SyntheticFieldStatus.MATCH, matched.getValue("totalPayment.direct"))
        assertEquals(SyntheticFieldStatus.MATCH, matched.getValue("capitalBalance.postParse"))
        assertEquals(SyntheticFieldStatus.MATCH, matched.getValue("balanceMeaning"))
        assertEquals(SyntheticFieldStatus.MATCH, matched.getValue("insurance.periodicCharge"))
        assertEquals(SyntheticFieldStatus.MATCH, matched.getValue("insurance.amountMeaning"))

        val wrongSemantics = expectedExtraction().copy(
            basePayment = "527.51",
            totalPayment = "507.51",
            balanceMeaning = BalanceMeaning.COMPOUND_DEBT,
            insurances = listOf(
                ExtractedInsurance(
                    name = "Seguro de vida",
                    monthlyCost = "20.00",
                    amountMeaning = InsuranceAmountMeaning.INSURED_VALUE
                )
            )
        )
        val mismatched = SyntheticFixtureComparison.compareStructuredFields(wrongSemantics)
        assertEquals(SyntheticFieldStatus.MISMATCH, mismatched.getValue("basePayment.direct"))
        assertEquals(SyntheticFieldStatus.MISMATCH, mismatched.getValue("totalPayment.direct"))
        assertEquals(SyntheticFieldStatus.MISMATCH, mismatched.getValue("balanceMeaning"))
        assertEquals(SyntheticFieldStatus.MISMATCH, mismatched.getValue("insurance.periodicCharge"))
        assertEquals(SyntheticFieldStatus.MISMATCH, mismatched.getValue("insurance.amountMeaning"))
    }

    @Test
    fun monthlyPaymentNeedsItsExplicitMeaningAndFailedMappingStaysVisible() {
        val unclassified = expectedExtraction().copy(
            basePayment = null,
            totalPayment = null,
            monthlyPayment = "507.51",
            paymentMeaning = PaymentMeaning.UNKNOWN
        )
        val statuses = SyntheticFixtureComparison.compareStructuredFields(unclassified)
        assertEquals(SyntheticFieldStatus.OMITTED, statuses.getValue("basePayment.direct"))
        assertEquals(SyntheticFieldStatus.AMBIGUOUS, statuses.getValue("basePayment.reviewedMonthly"))
        assertEquals(SyntheticFieldStatus.AMBIGUOUS, statuses.getValue("totalPayment.reviewedMonthly"))
        assertEquals(
            SyntheticFieldStatus.UNAVAILABLE,
            SyntheticFixtureComparison.unavailableStatuses().getValue("basePayment.direct")
        )
    }

    private fun expectedExtraction() = ExtractedDocumentData(
        capitalBalance = "502.49",
        balanceMeaning = BalanceMeaning.PRINCIPAL,
        basePayment = "507.51",
        totalPayment = "527.51",
        paymentMeaning = PaymentMeaning.BASE_PAYMENT,
        principalPaid = "497.51",
        interestCharged = "10.00",
        totalInsurance = "20.00",
        insurances = listOf(
            ExtractedInsurance(
                name = "Seguro de vida",
                monthlyCost = "20.00",
                amountMeaning = InsuranceAmountMeaning.PERIODIC_CHARGE
            )
        )
    )

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
