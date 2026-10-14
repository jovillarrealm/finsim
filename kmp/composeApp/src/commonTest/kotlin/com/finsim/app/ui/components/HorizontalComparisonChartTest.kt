package com.finsim.app.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HorizontalComparisonChartTest {

    @Test
    fun testCalculateMonthTicks_singleOrZeroMonths() {
        assertEquals(listOf(1), calculateMonthTicks(0))
        assertEquals(listOf(1), calculateMonthTicks(1))
    }

    @Test
    fun testCalculateMonthTicks_shortTerm() {
        assertEquals(listOf(1, 2, 3), calculateMonthTicks(3))
        assertEquals(listOf(1, 2, 3, 4, 5), calculateMonthTicks(5))
    }

    @Test
    fun testCalculateMonthTicks_oneYear() {
        val ticks = calculateMonthTicks(12)
        assertEquals(listOf(1, 3, 6, 9, 12), ticks)
    }

    @Test
    fun testCalculateMonthTicks_twoYears() {
        val ticks = calculateMonthTicks(24)
        assertEquals(listOf(1, 6, 12, 18, 24), ticks)
    }

    @Test
    fun testCalculateMonthTicks_threeYears() {
        val ticks = calculateMonthTicks(36)
        assertEquals(listOf(1, 12, 24, 36), ticks)
    }

    @Test
    fun testCalculateMonthTicks_fiveYears() {
        val ticks = calculateMonthTicks(60)
        // Step 18 for <= 96: 1, 18, 36, 54, 60 (or replaced if close)
        assertTrue(ticks.contains(1))
        assertTrue(ticks.contains(60))
        assertTrue(ticks.size in 4..7)
    }

    @Test
    fun testCalculateMonthTicks_longTerms() {
        for (months in listOf(120, 180, 240, 360)) {
            val ticks = calculateMonthTicks(months)
            assertEquals(1, ticks.first(), "First tick must always be M1")
            assertEquals(months, ticks.last(), "Last tick must always be totalMonths ($months)")
            assertTrue(ticks.size in 4..8, "Tick count for $months should be between 4 and 8, but was ${ticks.size}: $ticks")
            // Ensure strictly ascending
            for (i in 0 until ticks.size - 1) {
                assertTrue(ticks[i] < ticks[i + 1], "Ticks should be sorted: $ticks")
            }
        }
    }
}
