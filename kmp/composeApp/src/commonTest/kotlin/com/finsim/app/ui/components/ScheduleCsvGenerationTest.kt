package com.finsim.app.ui.components

import com.finsim.domain.*
import com.finsim.engine.simulateLoan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScheduleCsvGenerationTest {

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
}
