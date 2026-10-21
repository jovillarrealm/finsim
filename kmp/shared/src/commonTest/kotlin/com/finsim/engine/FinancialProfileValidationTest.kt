package com.finsim.engine

import com.finsim.domain.Rate
import com.finsim.domain.Scenario
import kotlin.test.Test
import kotlin.test.assertFailsWith

class FinancialProfileValidationTest {
    @Test
    fun testNonPositivePrincipalIsRejectedAtPublicSimulationSeam() {
        val scenario = Scenario(
            name = "Capital inválido",
            principal = "0.00",
            startDate = "2026-01",
            termMonths = 12,
            rate = Rate.Monthly("0.01")
        )

        assertFailsWith<IllegalArgumentException> { simulateLoan(scenario) }
    }
}
