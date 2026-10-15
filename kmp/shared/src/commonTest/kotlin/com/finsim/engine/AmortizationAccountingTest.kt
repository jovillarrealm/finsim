package com.finsim.engine

import com.finsim.core.Decimal
import com.finsim.domain.Insurance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AmortizationAccountingTest {

    @Test
    fun testWaterfallPriorityInsuranceThenInterestThenPrincipal() {
        val account = AmortizationAccount.open(
            AmortizationTerms(
                principal = Decimal.parse("1000.00"),
                monthlyRate = Decimal.parse("0.01"),
                basePayment = Decimal.parse("100.00"),
                termMonths = 12,
                isPaymentExplicit = true
            )
        )

        // Month 1: missed payment -> accumulates insurance 5.00, interest 10.00, overdue principal 90.00
        val m1 = account.processMonth(
            MonthPaymentInput(
                month = 1,
                isMissed = true,
                activeInsurance = listOf(
                    Insurance(id = "ins", name = "Vida", kind = "fixed", value = "5.00", endsAtPayoff = true)
                )
            )
        )
        assertEquals("105.00", m1.arrears)
        assertEquals("1015.00", m1.debt)
        assertEquals("5.00", m1.pendingInsurance)
        assertEquals("10.00", m1.pendingInterest)
        assertEquals("90.00", m1.overduePrincipal)

        // Month 2: missed again, with partial catchup offer of 7.00
        // Priority waterfall must allocate 7.00 to insurance (5.00 from m1 + 2.00 from m2)
        val m2 = account.processMonth(
            MonthPaymentInput(
                month = 2,
                isMissed = true,
                catchupOffer = Decimal.parse("7.00"),
                activeInsurance = listOf(
                    Insurance(id = "ins", name = "Vida", kind = "fixed", value = "5.00", endsAtPayoff = true)
                )
            )
        )
        assertEquals("7.00", m2.catchup.applied)
        assertEquals("7.00", m2.catchup.insurance)
        assertEquals("0.00", m2.catchup.interest)
        assertEquals("0.00", m2.catchup.principal)
        assertEquals("3.00", m2.pendingInsurance) // 10.00 accrued - 7.00 paid
        assertEquals("20.00", m2.pendingInterest) // 10.00 + 10.00
        assertEquals("180.00", m2.overduePrincipal) // 90.00 + 90.00
    }

    @Test
    fun testArrearsRecoveryAndOverdueCapitalReduction() {
        val account = AmortizationAccount.open(
            AmortizationTerms(
                principal = Decimal.parse("1000.00"),
                monthlyRate = Decimal.parse("0.01"),
                basePayment = Decimal.parse("100.00"),
                termMonths = 12,
                isPaymentExplicit = true
            )
        )

        // Month 1: missed
        account.processMonth(
            MonthPaymentInput(
                month = 1,
                isMissed = true,
                activeInsurance = listOf(
                    Insurance(id = "ins", name = "Vida", kind = "fixed", value = "5.00", endsAtPayoff = true)
                )
            )
        )

        // Month 2: regular payment (105.00) + full recovery (105.00) = 210.00
        val m2 = account.processMonth(
            MonthPaymentInput(
                month = 2,
                isMissed = false,
                catchupOffer = Decimal.parse("105.00"),
                activeInsurance = listOf(
                    Insurance(id = "ins", name = "Vida", kind = "fixed", value = "5.00", endsAtPayoff = true)
                )
            )
        )
        assertEquals("210.00", m2.cashPaid)
        assertEquals("820.00", m2.closingBalance)
        assertEquals("0.00", m2.overduePrincipal)
        assertEquals("0.00", m2.pendingInterest)
        assertEquals("0.00", m2.pendingInsurance)
        assertEquals("0.00", m2.arrears)
        assertEquals("820.00", m2.debt)
    }

    @Test
    fun testArgentinaPrepaymentProfileAdapterAtSeam() {
        val argAdapter = ArgentinaPrepaymentAdapter(
            originalTermMonths = 12,
            commissionRate = Decimal.parse("0.03"),
            vatRate = Decimal.parse("0.21")
        )
        val account = AmortizationAccount.open(
            AmortizationTerms(
                principal = Decimal.parse("10000.00"),
                monthlyRate = Decimal.parse("0.01"),
                basePayment = Decimal.parse("1000.00"),
                termMonths = 12,
                prepaymentRule = argAdapter
            )
        )

        // Free threshold: (12 + 3) / 4 = 3
        // Month 1 (< 3): extra prepayment of 1000.00 incurs 3% commission + 21% VAT
        val m1 = account.processMonth(
            MonthPaymentInput(
                month = 1,
                extraOffer = Decimal.parse("1000.00")
            )
        )
        assertEquals("1000.00", m1.extra.principal)
        assertEquals("30.00", m1.commissionPaid) // 3% of 1000
        assertEquals("6.30", m1.vatPaid) // 21% of 30

        // Month 2 (< 3): extra prepayment of 500.00
        val m2 = account.processMonth(
            MonthPaymentInput(
                month = 2,
                extraOffer = Decimal.parse("500.00")
            )
        )
        assertEquals("15.00", m2.commissionPaid)
        assertEquals("3.15", m2.vatPaid)

        // Month 3 (>= 3): free threshold reached
        val m3 = account.processMonth(
            MonthPaymentInput(
                month = 3,
                extraOffer = Decimal.parse("2000.00")
            )
        )
        assertEquals("0.00", m3.commissionPaid)
        assertEquals("0.00", m3.vatPaid)
    }

    @Test
    fun testMonthOrderingConstraintEnforcement() {
        val account = AmortizationAccount.open(
            AmortizationTerms(
                principal = Decimal.parse("1000.00"),
                monthlyRate = Decimal.parse("0.01"),
                basePayment = Decimal.parse("100.00"),
                termMonths = 12
            )
        )

        account.processMonth(MonthPaymentInput(month = 1))
        // Calling month 3 skipping month 2 must throw IllegalArgumentException
        assertFailsWith<IllegalArgumentException> {
            account.processMonth(MonthPaymentInput(month = 3))
        }
    }

    @Test
    fun testPrincipalConservationWithoutResidualLeakage() {
        val account = AmortizationAccount.open(
            AmortizationTerms(
                principal = Decimal.parse("300.00"),
                monthlyRate = Decimal.ZERO,
                termMonths = 3
            )
        )

        val m1 = account.processMonth(MonthPaymentInput(month = 1))
        val m2 = account.processMonth(MonthPaymentInput(month = 2))
        val m3 = account.processMonth(MonthPaymentInput(month = 3))

        assertEquals("100.00", m1.scheduled.principal)
        assertEquals("100.00", m2.scheduled.principal)
        assertEquals("100.00", m3.scheduled.principal)
        assertEquals("0.00", m3.closingBalance)
        assertTrue(account.isPaidOff())
    }
}
