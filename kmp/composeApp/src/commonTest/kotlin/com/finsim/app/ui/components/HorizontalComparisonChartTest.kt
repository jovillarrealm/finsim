package com.finsim.app.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HorizontalComparisonChartTest {
    @Test fun windowsContainAtMostTwelvePaymentMonthsIncludingTheLastMonth() {
        assertEquals(0..2, monthWindow(1, 2))
        assertEquals(0..12, monthWindow(12, 1200))
        assertEquals(13..24, monthWindow(13, 1200))
        assertEquals(1189..1200, monthWindow(1200, 1200))
        assertEquals(0..2, monthWindow(1200, 2))
        for (month in 0..1200) {
            val range = monthWindow(month, 1200)
            assertTrue(month in range)
            assertTrue(range.count { it > 0 } <= 12)
        }
    }
}
