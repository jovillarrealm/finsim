package com.finsim.engine

import com.finsim.core.Decimal
import com.finsim.domain.Rate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
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
        val monthlyDiff = (Decimal.parse(em) - Decimal.parse("0.01")).abs()
        assertTrue(monthlyDiff < Decimal.parse("0.000000000001"), "Nominal 12 debe convertir a EM 0.01 con error < 1e-12; error $monthlyDiff")

        val roundtripNominal12 = convertRate(Rate.Monthly(em), "nominal", frequency = 12)
        val nominal12Diff = (Decimal.parse(roundtripNominal12) - Decimal.parse("0.12")).abs()
        assertTrue(nominal12Diff < Decimal.parse("0.000000000001"), "Nominal 12 debe volver a 0.12 con error < 1e-12; error $nominal12Diff")

        val nominal4 = Rate.Nominal("0.12", frequency = 4)
        val quarterlyEm = convertRate(nominal4, "monthly")
        val roundtripNominal = convertRate(Rate.Monthly(quarterlyEm), "nominal", frequency = 4)
        val diff = (Decimal.parse(roundtripNominal) - Decimal.parse("0.12")).abs()
        assertTrue(diff < Decimal.parse("0.000000000001"), "Error $diff debe ser < 1e-12")
    }

    @Test
    fun testTargetRateMonthlyAndAnnual() {
        val em = Rate.Monthly("0.01")
        val convertedEa = convertRate(em, TargetRate.Annual)
        assertIs<Rate.Annual>(convertedEa)
        assertTrue(convertedEa.value.startsWith("0.126825030131969720661201"))

        val roundtripEm = convertRate(convertedEa, TargetRate.Monthly)
        assertIs<Rate.Monthly>(roundtripEm)
        val diff = (Decimal.parse(roundtripEm.value) - Decimal.parse("0.01")).abs()
        assertTrue(diff < Decimal.parse("0.000000000001"), "Error de ida y vuelta < 1e-12")
    }

    @Test
    fun testTargetRateNominalFrequencies() {
        val monthly = Rate.Monthly("0.01")

        val nom12 = convertRate(monthly, TargetRate.Nominal(frequency = 12))
        assertIs<Rate.Nominal>(nom12)
        assertEquals(12, nom12.frequency)
        val nominal12Diff = (Decimal.parse(nom12.value) - Decimal.parse("0.12")).abs()
        assertTrue(nominal12Diff < Decimal.parse("0.000000000001"), "Nominal 12 debe ser 0.12 con error < 1e-12; error $nominal12Diff")

        val roundtripEm12 = convertRate(nom12, TargetRate.Monthly)
        assertIs<Rate.Monthly>(roundtripEm12)
        val monthly12Diff = (Decimal.parse(roundtripEm12.value) - Decimal.parse("0.01")).abs()
        assertTrue(monthly12Diff < Decimal.parse("0.000000000001"), "Nominal 12 debe volver a EM 0.01 con error < 1e-12; error $monthly12Diff")

        val nom4 = convertRate(monthly, TargetRate.Nominal(frequency = 4))
        assertIs<Rate.Nominal>(nom4)
        assertEquals(4, nom4.frequency)

        val nom1 = convertRate(monthly, TargetRate.Nominal(frequency = 1))
        assertIs<Rate.Nominal>(nom1)
        assertEquals(1, nom1.frequency)

        // Roundtrip from nominal back to monthly
        val roundtripEm = convertRate(nom4, TargetRate.Monthly)
        assertIs<Rate.Monthly>(roundtripEm)
        val diff = (Decimal.parse(roundtripEm.value) - Decimal.parse("0.01")).abs()
        assertTrue(diff < Decimal.parse("0.000000000001"))
    }

    @Test
    fun testTargetRateTypeSafetyAndValidation() {
        // Nominal frequency must strictly be > 0
        assertFailsWith<IllegalArgumentException> {
            TargetRate.Nominal(0)
        }
        assertFailsWith<IllegalArgumentException> {
            TargetRate.Nominal(-1)
        }
        assertFailsWith<IllegalArgumentException> {
            TargetRate.Nominal(-12)
        }

        // Adapter without frequency must fail
        assertFailsWith<IllegalArgumentException> {
            convertRate(Rate.Monthly("0.01"), "nominal")
        }

        // Adapter with typo must fail
        assertFailsWith<IllegalStateException> {
            convertRate(Rate.Monthly("0.01"), "mothly")
        }
    }

    @Test
    fun testStrictParityBetweenTargetRateAndStringAdapter() {
        val testRates = listOf(
            Rate.Monthly("0.01"),
            Rate.Monthly("0"),
            Rate.Annual("0.18"),
            Rate.Annual("0.0001"),
            Rate.Nominal("0.12", frequency = 12),
            Rate.Nominal("0.15", frequency = 4),
            Rate.Nominal("0.08", frequency = 1)
        )

        for (rate in testRates) {
            val typedMonthly = convertRate(rate, TargetRate.Monthly)
            assertIs<Rate.Monthly>(typedMonthly)
            val stringMonthly = convertRate(rate, "monthly")
            assertEquals(stringMonthly, typedMonthly.value, "Monthly parity failed for $rate")

            val typedAnnual = convertRate(rate, TargetRate.Annual)
            assertIs<Rate.Annual>(typedAnnual)
            val stringAnnual = convertRate(rate, "annual")
            assertEquals(stringAnnual, typedAnnual.value, "Annual parity failed for $rate")

            for (freq in listOf(1, 2, 4, 6, 12, 360)) {
                val typedNominal = convertRate(rate, TargetRate.Nominal(freq))
                assertIs<Rate.Nominal>(typedNominal)
                val stringNominal = convertRate(rate, "nominal", freq)
                assertEquals(stringNominal, typedNominal.value, "Nominal($freq) parity failed for $rate")
                assertEquals(freq, typedNominal.frequency)
            }
        }
    }
}
