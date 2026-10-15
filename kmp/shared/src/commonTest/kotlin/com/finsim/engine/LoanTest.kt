package com.finsim.engine

import com.finsim.core.Decimal
import com.finsim.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoanTest {

    @Test
    fun testExampleScenarioSimulation() {
        val result = simulateLoan(exampleScenario)
        assertEquals(36, result.rows.size)
        assertEquals(36, result.payoffMonth)
        assertEquals("2029-09", result.payoffDate)
        assertTrue(result.complete)
        assertEquals("0.00", result.remainingDebt)
    }

    @Test
    fun testHalfCentRounding() {
        val s = Scenario(
            name = "Half Cent",
            principal = "100.50",
            startDate = "2026-01",
            termMonths = 1,
            rate = Rate.Monthly("0.01")
        )
        val res = simulateLoan(s)
        assertEquals("1.01", res.rows[0].interestCharged)
    }

    @Test
    fun testZeroRateConservation() {
        val s = Scenario(
            name = "Zero Rate",
            principal = "100.00",
            startDate = "2026-01",
            termMonths = 3,
            rate = Rate.Monthly("0")
        )
        val res = simulateLoan(s)
        assertEquals(3, res.rows.size)
        assertEquals("33.33", res.rows[0].scheduled.principal)
        assertEquals("33.33", res.rows[1].scheduled.principal)
        assertEquals("33.34", res.rows[2].scheduled.principal)
        assertEquals("100.00", res.totals.principal)
        assertEquals("0.00", res.rows[2].closingBalance)
    }

    @Test
    fun testTwoMonthsLoanWithInterest() {
        val s = Scenario(
            name = "Two Months",
            principal = "1000.00",
            startDate = "2026-01",
            termMonths = 2,
            rate = Rate.Monthly("0.01")
        )
        val res = simulateLoan(s)
        assertEquals("507.51", res.basePayment)
        assertEquals("10.00", res.rows[0].interestCharged)
        assertEquals("497.51", res.rows[0].scheduled.principal)
        assertEquals("502.49", res.rows[0].closingBalance)
        assertEquals("5.02", res.rows[1].interestCharged)
        assertEquals("502.49", res.rows[1].scheduled.principal)
        assertEquals("0.00", res.rows[1].closingBalance)
        assertTrue(res.complete)
    }

    @Test
    fun testInsuranceContinuingAfterPayoff() {
        val s = Scenario(
            name = "Continuing Insurance",
            principal = "100.00",
            startDate = "2026-01",
            termMonths = 1,
            rate = Rate.Monthly("0"),
            payment = "100.00",
            insurance = listOf(
                Insurance(id = "fix", name = "Fijo", kind = "fixed", value = "10.00", endsAtPayoff = false, endMonth = 3)
            )
        )
        val res = simulateLoan(s)
        assertEquals(1, res.payoffMonth)
        assertEquals(3, res.lastInsuranceMonth)
        assertEquals(3, res.rows.size)
        assertEquals("110.00", res.rows[0].cashPaid)
        assertEquals("10.00", res.rows[1].cashPaid)
        assertEquals("10.00", res.rows[2].cashPaid)
        assertEquals("130.00", res.totals.cashPaid)
    }

    @Test
    fun testMissedPaymentAndFullCatchup() {
        // Table 6.1 from financial conventions
        val s = Scenario(
            name = "Recovery",
            source = "current",
            principal = "1000.00",
            startDate = "2026-01",
            termMonths = 12,
            payment = "100.00",
            rate = Rate.Monthly("0.01"),
            insurance = listOf(
                Insurance(id = "life", name = "Vida", kind = "fixed", value = "5.00", endsAtPayoff = true)
            ),
            events = listOf(
                LoanEvent.Missed(id = "m1", month = 1),
                LoanEvent.Catchup(id = "c2", month = 2, amount = "105.00")
            )
        )
        val res = simulateLoan(s)
        val m1 = res.rows[0]
        assertEquals("1000.00", m1.openingBalance)
        assertEquals("10.00", m1.interestCharged)
        assertEquals("5.00", m1.insuranceCharged)
        assertEquals("0.00", m1.cashPaid)
        assertEquals("90.00", m1.overduePrincipal)
        assertEquals("10.00", m1.pendingInterest)
        assertEquals("5.00", m1.pendingInsurance)
        assertEquals("105.00", m1.arrears)
        assertEquals("1015.00", m1.debt)

        val m2 = res.rows[1]
        assertEquals("1000.00", m2.openingBalance)
        assertEquals("10.00", m2.interestCharged)
        assertEquals("5.00", m2.insuranceCharged)
        assertEquals("210.00", m2.cashPaid)
        assertEquals("820.00", m2.closingBalance)
        assertEquals("0.00", m2.overduePrincipal)
        assertEquals("0.00", m2.pendingInterest)
        assertEquals("0.00", m2.pendingInsurance)
        assertEquals("0.00", m2.arrears)
        assertEquals("820.00", m2.debt)
    }

    @Test
    fun testArgentinaPrepaymentProfile() {
        val s = Scenario(
            name = "Argentina Test",
            currency = Currency.ARS,
            principal = "10000.00",
            startDate = "2026-01",
            termMonths = 12,
            rate = Rate.Monthly("0.01"),
            argentinaProfile = ArgentinaProfile(
                originationDate = "2026-01",
                originalTermMonths = 12,
                commissionRate = "0.03",
                vatRate = "0.21"
            ),
            events = listOf(
                LoanEvent.Extra(id = "e1", month = 2, amount = "1000.00") // Before 1/4 of 12 (month 3)
            )
        )
        val res = simulateLoan(s)
        val m2 = res.rows[1]
        // In month 2, prepaid capital is 1000.00
        // Commission = 3% of 1000 = 30.00
        // VAT = 21% of 30.00 = 6.30
        assertEquals("30.00", m2.commissionPaid)
        assertEquals("6.30", m2.vatPaid)
    }

    @Test
    fun testIncompleteSimulationAt1200Months() {
        // Loan with payment smaller than interest
        val s = Scenario(
            name = "Non amortizing",
            principal = "10000.00",
            startDate = "2026-01",
            termMonths = 12,
            payment = "50.00", // Monthly interest is 100.00 at 1%
            rate = Rate.Monthly("0.01")
        )
        val res = simulateLoan(s)
        assertEquals(MAX_MONTHS, res.rows.size)
        assertFalse(res.complete)
        assertTrue(Decimal.parse(res.remainingDebt) > Decimal.ZERO)
    }
}
