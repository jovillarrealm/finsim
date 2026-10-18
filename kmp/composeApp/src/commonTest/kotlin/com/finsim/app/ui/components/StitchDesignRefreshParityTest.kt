package com.finsim.app.ui.components

import com.finsim.core.Decimal
import com.finsim.domain.*
import com.finsim.engine.simulateLoan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StitchDesignRefreshParityTest {

    private val baseScenario = Scenario(
        name = "Base Loan",
        principal = "10000000.00",
        startDate = "2026-01",
        termMonths = 36,
        rate = Rate.Monthly("0.015"),
        currency = Currency.COP
    )

    @Test
    fun testScheduleCsvGenerationIntegrity() {
        val result = simulateLoan(baseScenario)
        val csv = generateScheduleCsv(result, "TestScenario", ';')

        // Must start with UTF-8 BOM
        assertTrue(csv.startsWith('\uFEFF'), "CSV must start with UTF-8 BOM")

        val lines = csv.trimStart('\uFEFF').split("\r\n").filter { it.isNotBlank() }
        assertEquals(37, lines.size, "Must have header + 36 monthly rows")

        // Validate header columns
        val header = lines[0].split(';')
        assertEquals(18, header.size, "Must have all 18 accounting columns")
        assertEquals("Mes", header[0])
        assertEquals("Fecha", header[1])
        assertEquals("Saldo inicial", header[2])
        assertEquals("Capital habitual", header[3])
        assertEquals("Capital adicional", header[4])
        assertEquals("Pago total", header[10])
        assertEquals("Saldo final", header[11])

        // First row assertions
        val firstRow = lines[1].split(';')
        assertEquals("M1", firstRow[0])
        assertEquals("2026-01", firstRow[1])
        assertEquals("10000000,00", firstRow[2])
    }

    @Test
    fun testKeyMilestonesCuratedFilteringWithPrepayments() {
        val modifiedScenario = baseScenario.copy(
            events = listOf(
                LoanEvent.Extra("ev1", 6, "2000000.00"),
                LoanEvent.Extra("ev2", 18, "3000000.00")
            )
        )
        val original = simulateLoan(baseScenario)
        val modified = simulateLoan(modifiedScenario)

        val rows = modified.rows
        val prepaymentMonths = rows.filter {
            val ext = it.extra.principal.toDoubleOrNull() ?: 0.0
            val cat = it.catchup.principal.toDoubleOrNull() ?: 0.0
            ext + cat > 0.0
        }.map { it.month }.toSet()

        assertEquals(setOf(6, 18), prepaymentMonths)

        // Compute milestone set
        val milestoneSet = mutableSetOf<Int>()
        if (rows.isNotEmpty()) {
            milestoneSet.add(1)
            for (m in prepaymentMonths) {
                if (m > 1) milestoneSet.add(m - 1)
                milestoneSet.add(m)
            }
            if (modified.payoffMonth != null) {
                milestoneSet.add(modified.payoffMonth!!)
            } else {
                milestoneSet.add(rows.last().month)
            }
        }

        assertTrue(1 in milestoneSet, "Must include M01 start")
        assertTrue(5 in milestoneSet, "Must include pre-abono M05")
        assertTrue(6 in milestoneSet, "Must include abono M06")
        assertTrue(17 in milestoneSet, "Must include pre-abono M17")
        assertTrue(18 in milestoneSet, "Must include abono M18")
        assertTrue(modified.payoffMonth!! in milestoneSet, "Must include early payoff month")

        // Payoff month must be earlier than original (36)
        assertTrue(modified.payoffMonth!! < original.payoffMonth!!, "Prepayments must accelerate payoff")
    }

    @Test
    fun testKeyMilestonesFilteringWithoutPrepayments() {
        val original = simulateLoan(baseScenario)
        val rows = original.rows
        val prepaymentMonths = rows.filter {
            val ext = it.extra.principal.toDoubleOrNull() ?: 0.0
            val cat = it.catchup.principal.toDoubleOrNull() ?: 0.0
            ext + cat > 0.0
        }.map { it.month }.toSet()

        assertTrue(prepaymentMonths.isEmpty())

        val milestoneSet = mutableSetOf<Int>()
        if (rows.isNotEmpty()) {
            milestoneSet.add(1)
            if (original.payoffMonth != null) {
                milestoneSet.add(original.payoffMonth!!)
            } else {
                milestoneSet.add(rows.last().month)
            }
            if (prepaymentMonths.isEmpty() && rows.size > 2) {
                milestoneSet.add(rows.size / 2)
            }
        }

        assertEquals(3, milestoneSet.size, "Without prepayments, should have M01, middle month, and final payoff")
        assertTrue(1 in milestoneSet)
        assertTrue(18 in milestoneSet)
        assertTrue(36 in milestoneSet)
    }

    @Test
    fun testMathematicalInvariantsAndSavingsDerivations() {
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

        // Interest percentage saved calculation
        val interestPctSaved = if (origInterest > Decimal.ZERO && interestDelta < Decimal.ZERO) {
            val ratio = ((-interestDelta) / origInterest) * Decimal.fromInt(100)
            ratio.toFixed(0).toIntOrNull()
        } else null

        assertNotNull(interestPctSaved)
        assertTrue(interestPctSaved > 0, "Interest percentage saved must be > 0%")

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

    @Test
    fun testZeroPrepaymentsFallbackState() {
        val original = simulateLoan(baseScenario)
        val modified = simulateLoan(baseScenario) // No extra events

        val origInterest = Decimal.parse(original.totals.interest)
        val modInterest = Decimal.parse(modified.totals.interest)
        val interestDelta = modInterest - origInterest
        val cashDelta = Decimal.parse(modified.totals.cashPaid) - Decimal.parse(original.totals.cashPaid)

        val monthsSaved = (original.payoffMonth ?: 36) - (modified.payoffMonth ?: 36)

        assertEquals(Decimal.ZERO, interestDelta)
        assertEquals(Decimal.ZERO, cashDelta)
        assertEquals(0, monthsSaved)

        val interestPctSaved = if (origInterest > Decimal.ZERO && interestDelta < Decimal.ZERO) {
            val ratio = ((-interestDelta) / origInterest) * Decimal.fromInt(100)
            ratio.toFixed(0).toIntOrNull()
        } else null

        assertNull(interestPctSaved, "interestPctSaved must be null when there is no interest reduction")
    }
}
