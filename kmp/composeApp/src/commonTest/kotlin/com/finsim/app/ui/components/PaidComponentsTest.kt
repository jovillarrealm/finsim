package com.finsim.app.ui.components

import com.finsim.domain.Allocation
import com.finsim.domain.MonthRow
import com.finsim.domain.*
import com.finsim.engine.simulateLoan
import kotlin.test.Test
import kotlin.test.assertEquals

class PaidComponentsTest {
    private val base = Scenario(name = "FS18", principal = "1000.00", startDate = "2026-01", termMonths = 2, rate = Rate.Monthly("0.01"))

    @Test fun referenceFixturesStayExactAndKeepPostPayoffInsurance() {
        val f01 = simulateLoan(base)
        assertEquals("507.51", f01.basePayment)
        assertEquals(listOf("497.51", "0.00", "10.00", "0.00", "0.00"), paidComponents(f01.rows[0]))
        assertEquals("1000.00", resultBalance(f01, 0))
        assertEquals("502.49", resultBalance(f01, 1))
        assertEquals("0.00", resultBalance(f01, 3))
        assertEquals(null, resultBalance(f01.copy(complete = false), 3))
        val f02 = simulateLoan(base.copy(payment = "100", insurance = listOf(Insurance("i", "Seguro", "fixed", "5")),
            events = listOf(LoanEvent.Missed("m", 1), LoanEvent.Catchup("c", 2, "105"))))
        assertEquals(listOf("0.00", "0.00", "0.00", "0.00", "0.00"), paidComponents(f02.rows[0]))
        assertEquals(listOf("75.00", "105.00", "20.00", "10.00", "0.00"), paidComponents(f02.rows[1]))
        val f03 = simulateLoan(base.copy(rate = Rate.Monthly("0"), payment = "100", insurance = listOf(Insurance("i", "Seguro", "percentage", "0.001")),
            events = listOf(LoanEvent.Extra("e", 1, "1000"))))
        assertEquals(listOf("100.00", "900.00", "0.00", "1.00", "0.00"), paidComponents(f03.rows[0]))
        assertEquals("100.00", f03.rows[0].extra.unapplied)
        val f04 = simulateLoan(base.copy(principal = "100", rate = Rate.Monthly("0"), termMonths = 3))
        assertEquals(listOf("33.33", "33.33", "33.34"), f04.rows.map { paidComponents(it)[0] })
        val f05 = simulateLoan(base.copy(principal = "100", rate = Rate.Monthly("0"), payment = "100",
            insurance = listOf(Insurance("i", "Seguro", "fixed", "10", false, 3))))
        assertEquals(listOf("110.00", "10.00", "10.00"), f05.rows.map { sumMoney(paidComponents(it)) })
        assertEquals("0.00", resultBalance(f05, 3))
        listOf(f01, f02, f03, f04, f05).forEach { result ->
            result.rows.forEach { assertEquals(it.cashPaid, sumMoney(paidComponents(it))) }
        }
    }

    private fun allocation(principal: String, interest: String = "0.00", insurance: String = "0.00") =
        Allocation("1000.00", "0.00", insurance, interest, principal, "100.00")

    @Test fun paidComponentsIncludeRecoveryAndFeesButExcludeOfferedAndUnapplied() {
        val row = MonthRow(2, "2026-11", "1000.00", "99.00", "88.00",
            allocation("100.00", "10.00", "5.00"), allocation("75.00"), allocation("25.00"),
            "800.00", "0.00", "0.00", "0.00", "0.00", "800.00", "218.63", "3.00", "0.63")
        val paid = paidComponents(row)
        assertEquals(listOf("100.00", "100.00", "10.00", "5.00", "3.63"), paid)
        assertEquals(row.cashPaid, sumMoney(paid))
        assertEquals("100,00", clipboardMoney(paid[1]))
        assertEquals("-1014,02", clipboardMoney("-1014.02"))
        assertEquals("0,00", clipboardMoney("0"))
    }
}
