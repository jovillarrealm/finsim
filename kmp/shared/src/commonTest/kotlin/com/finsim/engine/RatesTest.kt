package com.finsim.engine

import com.finsim.core.Decimal
import com.finsim.domain.Rate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RatesTest {

    private fun Decimal.abs(): Decimal = if (this.isNegative) -this else this

    @Test
    fun testRatesConversionsAndRoundtrips() {
        val em = Rate.Monthly("0.01")
        val convertedEaStr = convertRate(em, "annual")
        assertTrue(convertedEaStr.startsWith("0.126825030131969720661201"))

        val ea = Rate.Annual(convertedEaStr)
        val roundtripEmStr = convertRate(ea, "monthly")
        val diff = (Decimal.parse(roundtripEmStr) - Decimal.parse("0.01")).abs()
        assertTrue(diff < Decimal.parse("0.000000000001"), "Error absoluto $diff debe ser < 1e-12")
    }

    @Test
    fun testZeroRate() {
        val zero = Rate.Monthly("0")
        assertEquals("0", convertRate(zero, "annual"))
        assertEquals("0", convertRate(zero, "nominal", 12))
    }

    @Test
    fun testTinyRate() {
        val tiny = Rate.Annual("0.0001")
        val emStr = convertRate(tiny, "monthly")
        val roundtripEaStr = convertRate(Rate.Monthly(emStr), "annual")
        val diff = (Decimal.parse(roundtripEaStr) - Decimal.parse("0.0001")).abs()
        assertTrue(diff < Decimal.parse("0.000000000001"), "Error $diff debe ser < 1e-12")
    }

    @Test
    fun testNominalFrequencies() {
        val nominal = Rate.Nominal("0.12", frequency = 12)
        val em = convertRate(nominal, "monthly")
        assertEquals("0.01", Decimal.parse(em).toFixed(2))

        val nominal4 = Rate.Nominal("0.12", frequency = 4)
        val quarterlyEm = convertRate(nominal4, "monthly")
        val roundtripNominal = convertRate(Rate.Monthly(quarterlyEm), "nominal", frequency = 4)
        val diff = (Decimal.parse(roundtripNominal) - Decimal.parse("0.12")).abs()
        assertTrue(diff < Decimal.parse("0.000000000001"), "Error $diff debe ser < 1e-12")
    }
}
