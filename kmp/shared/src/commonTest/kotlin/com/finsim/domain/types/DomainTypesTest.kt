package com.finsim.domain.types

import com.finsim.core.Decimal
import com.finsim.domain.*
import com.finsim.engine.simulateLoan
import kotlin.test.*

class DomainTypesTest {

    @Test
    fun testMoneyParsingAndInvariants() {
        val m1 = Money.parse("100.50", Currency.COP)
        assertEquals(10050L, m1.cents)
        assertEquals("100.50", m1.toFormattedString())
        assertEquals(Currency.COP, m1.currency)

        val m2 = Money.parse("50", Currency.COP)
        assertEquals(5000L, m2.cents)
        assertEquals("50.00", m2.toFormattedString())

        // Rejection of > 2 decimal places
        assertFailsWith<IllegalArgumentException> {
            Money.parse("100.005", Currency.COP)
        }

        // Rejection of negative money
        assertFailsWith<IllegalArgumentException> {
            Money.parse("-10.00", Currency.COP)
        }
    }

    @Test
    fun testMoneyArithmeticAndCurrencySeam() {
        val m1 = Money.parse("100.50", Currency.COP)
        val m2 = Money.parse("50.25", Currency.COP)

        val sum = m1 + m2
        assertEquals("150.75", sum.toFormattedString())

        val diff = m1 - m2
        assertEquals("50.25", diff.toFormattedString())

        // Underflow guard: cannot produce negative balance
        assertFailsWith<IllegalArgumentException> {
            m2 - m1
        }

        // minusOrZero floor
        assertEquals("0.00", m2.minusOrZero(m1).toFormattedString())

        // Currency mismatch rejected at seam
        val usd = Money.parse("10.00", Currency.USD)
        assertFailsWith<IllegalArgumentException> {
            m1 + usd
        }
    }

    @Test
    fun testMoneyHalfCentRoundingWithRate() {
        // Financial convention: Capital 100.50, tasa mensual 1% -> Interés 1.005 -> 1.01
        val principal = Money.parse("100.50", Currency.COP)
        val monthlyRate = Decimal.parse("0.01")
        val interest = principal.timesFraction(monthlyRate)
        assertEquals("1.01", interest.toFormattedString())
    }

    @Test
    fun testMonthIndexInvariantsAndProjection() {
        val m1 = MonthIndex(1)
        assertEquals(1, m1.value)
        assertEquals("2026-10", m1.toDate(2026, 10))

        val m3 = MonthIndex(3)
        assertEquals("2026-12", m3.toDate(2026, 10))

        val m4 = MonthIndex(4)
        assertEquals("2027-01", m4.toDate(2026, 10))

        // Out of bounds checks
        assertFailsWith<IllegalArgumentException> {
            MonthIndex(0)
        }
        assertFailsWith<IllegalArgumentException> {
            MonthIndex(1201)
        }

        assertEquals(MonthIndex(2), m1.next())
        assertEquals(null, MonthIndex(MAX_MONTHS).next())
    }

    @Test
    fun testRateValueCompoundingEquivalence() {
        val monthly = RateValue.EffectiveMonthly.of("0.01")
        val annual = monthly.toEffectiveAnnual()
        // (1 + 0.01)^12 - 1 = 0.1268250301...
        assertTrue(annual.fractionDecimal > Decimal.parse("0.1268"))

        val backToMonthly = annual.toEffectiveMonthly()
        assertEquals("0.01", backToMonthly.fractionDecimal.toFixed(2))

        // Negative rate rejected
        assertFailsWith<IllegalArgumentException> {
            RateValue.EffectiveMonthly.of("-0.05")
        }

        // Invalid nominal frequency rejected
        assertFailsWith<IllegalArgumentException> {
            RateValue.NominalAnnual.of("0.12", 0)
        }
    }

    @Test
    fun testEventIdInvariants() {
        val id = EventId("pay-1")
        assertEquals("pay-1", id.value)

        assertFailsWith<IllegalArgumentException> {
            EventId("")
        }
        assertFailsWith<IllegalArgumentException> {
            EventId("   ")
        }
    }

    private fun assertEngineParity(scenario: Scenario) {
        val originalResult = simulateLoan(scenario)
        val typedScenario = scenario.toTyped()
        val typedResult = TypedEngine.simulate(typedScenario)
        val seamResult = simulateLoan(typedScenario)

        // Verify TypedEngine and Loan.kt typed seam are 100% equivalent
        assertEquals(typedResult, seamResult, "TypedEngine and simulateLoan(TypedScenario) mismatch in ${scenario.name}")

        val adaptedResult = typedResult.toUntyped()

        assertEquals(originalResult.rows.size, adaptedResult.rows.size, "Row count mismatch in ${scenario.name}")
        assertEquals(originalResult.payoffMonth, adaptedResult.payoffMonth, "Payoff month mismatch in ${scenario.name}")
        assertEquals(originalResult.payoffDate, adaptedResult.payoffDate, "Payoff date mismatch in ${scenario.name}")
        assertEquals(originalResult.lastInsuranceMonth, adaptedResult.lastInsuranceMonth, "Last insurance month mismatch in ${scenario.name}")
        assertEquals(originalResult.complete, adaptedResult.complete, "Completion mismatch in ${scenario.name}")
        assertEquals(originalResult.basePayment, adaptedResult.basePayment, "Base payment mismatch in ${scenario.name}")
        assertEquals(originalResult.remainingDebt, adaptedResult.remainingDebt, "Remaining debt mismatch in ${scenario.name}")
        assertEquals(originalResult.totals, adaptedResult.totals, "Totals mismatch in ${scenario.name}")

        for (i in originalResult.rows.indices) {
            val origRow = originalResult.rows[i]
            val adaptedRow = adaptedResult.rows[i]
            assertEquals(origRow, adaptedRow, "Row $i mismatch in ${scenario.name}")
        }
    }

    @Test
    fun testFullParityBetweenUntypedAndTypedEngineAcrossCorpus() {
        val corpus = listOf(
            exampleScenario,
            exampleScenario.copy(currency = Currency.USD, principal = "5000.00", termMonths = 12),
            exampleScenario.copy(currency = Currency.EUR, principal = "8000.00", termMonths = 24),
            exampleScenario.copy(
                currency = Currency.ARS,
                principal = "100000.00",
                argentinaProfile = ArgentinaProfile(commissionRate = "0.03", vatRate = "0.21"),
                events = listOf(LoanEvent.Extra(id = "e1", month = 2, amount = "10000.00"))
            ),
            exampleScenario.copy(
                events = listOf(
                    LoanEvent.Recurring(id = "r1", month = 3, endMonth = 12, every = 3, amount = "500000.00"),
                    LoanEvent.Missed(id = "m1", month = 5),
                    LoanEvent.Catchup(id = "c1", month = 6, amount = "1000000.00")
                )
            ),
            exampleScenario.copy(
                source = "current",
                payment = "1500000.00",
                insurance = listOf(
                    Insurance(id = "fix", name = "Seguro Fijo", kind = "fixed", value = "50000.00", endsAtPayoff = false, endMonth = 40)
                )
            )
        )

        for (scenario in corpus) {
            assertEngineParity(scenario)
        }
    }

    @Test
    fun testTypedEngineParityPrepayments() {
        val scenario = exampleScenario.copy(
            name = "Prepayments Edge Case",
            principal = "50000000.00",
            termMonths = 36,
            events = listOf(
                LoanEvent.Extra(id = "extra-1", month = 3, amount = "5000000.00"),
                LoanEvent.Recurring(id = "rec-1", month = 6, endMonth = 18, every = 3, amount = "2000000.00")
            )
        )
        assertEngineParity(scenario)
    }

    @Test
    fun testTypedEngineParityArrearsAndCatchup() {
        val scenario = exampleScenario.copy(
            name = "Arrears and Catchup Edge Case",
            principal = "30000000.00",
            termMonths = 24,
            events = listOf(
                LoanEvent.Missed(id = "missed-1", month = 2),
                LoanEvent.Missed(id = "missed-2", month = 3),
                LoanEvent.Catchup(id = "catchup-1", month = 4, amount = "2500000.00"),
                LoanEvent.Catchup(id = "catchup-2", month = 5, amount = "1000000.00")
            )
        )
        assertEngineParity(scenario)
    }

    @Test
    fun testTypedEngineParityArgentinaProfile() {
        val scenario = exampleScenario.copy(
            name = "Argentina Profile Edge Case",
            currency = Currency.ARS,
            principal = "1000000.00",
            termMonths = 20,
            argentinaProfile = ArgentinaProfile(
                originalTermMonths = 20,
                commissionRate = "0.03",
                vatRate = "0.21"
            ),
            events = listOf(
                // Prepayment before 25% threshold (month 2 < (20+3)/4 = 5 -> commission + vat)
                LoanEvent.Extra(id = "pre-early", month = 2, amount = "100000.00"),
                // Prepayment at/after threshold (month 6 >= 5 -> commission waived)
                LoanEvent.Extra(id = "pre-late", month = 6, amount = "200000.00")
            )
        )
        assertEngineParity(scenario)
    }

    @Test
    fun testTypedSeamInLoanKtAndBidirectionalAdaptation() {
        val typedScenario = exampleScenario.toTyped()

        // Verify TypedEngine.simulate produces identical result to simulateLoan(TypedScenario)
        val engineResult = TypedEngine.simulate(typedScenario)
        val seamResult = simulateLoan(typedScenario)
        assertEquals(engineResult, seamResult)

        // Verify bidirectional adaptation
        val roundtripScenario = typedScenario.toUntyped()
        assertEquals(exampleScenario.name, roundtripScenario.name)
        assertEquals(exampleScenario.principal, roundtripScenario.principal)
        assertEquals(exampleScenario.termMonths, roundtripScenario.termMonths)
        assertEquals(exampleScenario.currency, roundtripScenario.currency)
        assertEquals(exampleScenario.startDate, roundtripScenario.startDate)
    }
}
