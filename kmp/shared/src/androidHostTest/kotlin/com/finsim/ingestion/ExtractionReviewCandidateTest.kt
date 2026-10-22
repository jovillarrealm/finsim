package com.finsim.ingestion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExtractionReviewCandidateTest {
    @Test
    fun unclassifiedPrincipalAloneCanOpenReview() {
        val extracted = VlmJsonGrammar.parseVlmResponse(
            rawOutput = """{"principal":"30000000.00","balanceMeaning":"UNKNOWN"}""",
            framework = RegulatoryFramework.COLOMBIA_SFC
        )

        assertEquals("30000000.00", extracted.unclassifiedPrincipal)
        assertEquals(BalanceMeaning.UNKNOWN, extracted.balanceMeaning)
        assertNull(extracted.bankNameCandidate)
        assertNull(extracted.statementMonth)
        assertNull(extracted.statementCutDate)
        assertNull(extracted.closingBalance)
        assertNull(extracted.originalPrincipal)
        assertNull(extracted.termMonths)
        assertNull(extracted.rate)
        assertNull(extracted.monthlyPayment)
        assertNull(extracted.principalPaid)
        assertNull(extracted.interestCharged)
        assertNull(extracted.totalInsurance)
        assertTrue(extracted.insurances.isEmpty())
        assertTrue(extracted.hasCandidatesForReview())
    }

    @Test
    fun legibleGenericTermPaymentAndInsuranceValuesRemainUnclassified() {
        val extracted = VlmJsonGrammar.parseVlmResponse(
            rawOutput = """{"termMonths":60,"termMeaning":"UNKNOWN","monthlyPayment":"850.000,00","paymentMeaning":"UNKNOWN","insurances":[{"name":"Seguro de vida","amount":"12.500,00","amountMeaning":"UNKNOWN"}]}""",
            framework = RegulatoryFramework.COLOMBIA_SFC
        )

        assertEquals(60, extracted.termMonths)
        assertEquals(TermMeaning.UNKNOWN, extracted.termMeaning)
        assertEquals("850000.00", extracted.monthlyPayment)
        assertEquals(PaymentMeaning.UNKNOWN, extracted.paymentMeaning)
        assertEquals("12500.00", extracted.insurances.single().monthlyCost)
        assertEquals(InsuranceAmountMeaning.UNKNOWN, extracted.insurances.single().amountMeaning)
        assertTrue(extracted.hasCandidatesForReview())
    }

    @Test
    fun meaningSpecificTermAndPaymentCandidatesAlsoReachReview() {
        val originalTerm = VlmJsonGrammar.parseVlmResponse(
            rawOutput = """{"originalTermMonths":60}""",
            framework = RegulatoryFramework.COLOMBIA_SFC
        )
        val remainingTerm = VlmJsonGrammar.parseVlmResponse(
            rawOutput = """{"remainingTermMonths":48}""",
            framework = RegulatoryFramework.COLOMBIA_SFC
        )
        val basePayment = VlmJsonGrammar.parseVlmResponse(
            rawOutput = """{"basePayment":"850000.00"}""",
            framework = RegulatoryFramework.COLOMBIA_SFC
        )
        val totalPayment = VlmJsonGrammar.parseVlmResponse(
            rawOutput = """{"totalPayment":"875000.00"}""",
            framework = RegulatoryFramework.COLOMBIA_SFC
        )

        assertEquals(60, originalTerm.originalTermMonths)
        assertEquals(48, remainingTerm.remainingTermMonths)
        assertEquals("850000.00", basePayment.basePayment)
        assertEquals("875000.00", totalPayment.totalPayment)
        listOf(originalTerm, remainingTerm, basePayment, totalPayment).forEach { candidate ->
            assertTrue(candidate.hasCandidatesForReview(), candidate.rawExtractedText)
        }
        assertFalse(ExtractedDocumentData().hasCandidatesForReview())
    }
}
