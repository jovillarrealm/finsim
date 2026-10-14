package com.finsim.reconciliation

import com.finsim.domain.Rate
import com.finsim.domain.Scenario
import com.finsim.engine.simulateLoan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReconciliationTest {

    @Test
    fun testReconciliationMatchingAndDiscrepantRows() {
        val scenario = Scenario(
            name = "Reconciliation Loan",
            principal = "1000.00",
            startDate = "2026-01",
            termMonths = 2,
            rate = Rate.Monthly("0.01")
        )
        val loanResult = simulateLoan(scenario)

        // Month 1 simulated interest is 10.00
        // Statement with exact match 10.00
        val exactStatement = StatementRow(month = 1, interest = "10.00")
        val exactDiff = reconcileRow(exactStatement, loanResult.rows)
        assertEquals(1, exactDiff.size)
        assertEquals("0.00", exactDiff[0].difference)

        // Statement with discrepancy 11.25 -> diff should be +1.25
        val diffStatement = StatementRow(month = 1, interest = "11.25")
        val diffList = reconcileRow(diffStatement, loanResult.rows)
        assertEquals(1, diffList.size)
        assertEquals("1.25", diffList[0].difference)
    }
}
