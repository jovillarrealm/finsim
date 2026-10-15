package com.finsim.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DecimalTest {

    @Test
    fun testBasicArithmetic() {
        val a = Decimal.parse("100.50")
        val b = Decimal.parse("50.25")
        assertEquals("150.75", (a + b).toFixed(2))
        assertEquals("50.25", (a - b).toFixed(2))
        assertEquals("5050.125", (a * b).toString())
        assertEquals("2.00", (a / b).toFixed(2))
    }

    @Test
    fun testHalfUpRounding() {
        assertEquals("1.01", Decimal.parse("1.005").toFixed(2))
        assertEquals("1.00", Decimal.parse("1.004").toFixed(2))
        assertEquals("1.01", Decimal.parse("1.006").toFixed(2))
    }

    @Test
    fun testConservationDivision() {
        val hundred = Decimal.parse("100.00")
        val three = Decimal.parse("3")
        val payment = hundred / three
        assertEquals("33.33", payment.toFixed(2))
    }

    @Test
    fun testRateConversionFractionalPower() {
        // EM = 0.01 -> EA = (1 + 0.01)^12 - 1 = 0.126825030131969720661201...
        val em = Decimal.parse("0.01")
        val onePlusEm = Decimal.ONE + em
        val ea = onePlusEm.pow(12) - Decimal.ONE
        assertTrue(ea.toString().startsWith("0.126825030131969720661201"))

        // Roundtrip: EA = 0.126825030131969720661201 -> EM = (1 + EA)^(1/12) - 1
        val onePlusEa = Decimal.ONE + Decimal.parse("0.126825030131969720661201")
        val twelfth = Decimal.ONE / Decimal.fromInt(12)
        val computedEm = onePlusEa.pow(twelfth) - Decimal.ONE
        val diff = (computedEm - em).abs()
        assertTrue(diff < Decimal.parse("0.000000000001"), "Difference $diff should be < 1e-12")
    }

    private fun Decimal.abs(): Decimal = if (this.isNegative) -this else this
}
