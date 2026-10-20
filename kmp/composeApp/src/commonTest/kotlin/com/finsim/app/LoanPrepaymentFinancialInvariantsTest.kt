package com.finsim.app

import com.finsim.core.Decimal
import com.finsim.domain.*
import com.finsim.engine.simulateLoan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoanPrepaymentFinancialInvariantsTest {

    private val baseScenario = Scenario(
        name = "Base Loan",
        principal = "10000000.00",
        startDate = "2026-01",
        termMonths = 36,
        rate = Rate.Monthly("0.015"),
        currency = Currency.COP
    )

    @Test
    fun testLoanInvariantsAndPrepaymentFinancialEffects() {
        val modifiedScenario = baseScenario.copy(
            events = listOf(
                LoanEvent.Extra("ev1", 6, "2000000.00"),
                LoanEvent.Extra("ev2", 12, "2000000.00")
            )
        )
        val original = simulateLoan(baseScenario)
        val modified = simulateLoan(modifiedScenario)

        val origInterest = Decimal.parse(original.totals.interest)
        val modInterest = Decimal.parse(modified.totals.interest)
        val interestDelta = modInterest - origInterest

        val origCash = Decimal.parse(original.totals.cashPaid)
        val modCash = Decimal.parse(modified.totals.cashPaid)
        val cashDelta = modCash - origCash

        val monthsSaved = (original.payoffMonth ?: 36) - (modified.payoffMonth ?: 36)

        assertTrue(interestDelta < Decimal.ZERO, "Interest delta must be negative with prepayments")
        assertTrue(cashDelta < Decimal.ZERO, "Net cash paid must decrease despite early extra payments")
        assertTrue(monthsSaved > 0, "Months saved must be positive")

        // Cash conservation invariant: principal + interest + insurance == cashPaid
        val totalPrincipal = Decimal.parse(modified.totals.principal)
        val totalInterest = Decimal.parse(modified.totals.interest)
        val totalInsurance = Decimal.parse(modified.totals.insurance)
        val totalCashPaid = Decimal.parse(modified.totals.cashPaid)
        assertEquals((totalPrincipal + totalInterest + totalInsurance).toFixed(2), totalCashPaid.toFixed(2))

        // Balance monotonic decrease
        for (i in 0 until modified.rows.size - 1) {
            val currentBal = Decimal.parse(modified.rows[i].closingBalance)
            val nextBal = Decimal.parse(modified.rows[i + 1].closingBalance)
            assertTrue(currentBal >= nextBal, "Closing balance must decrease monotonically at month ${i + 1}")
        }
    }
}
