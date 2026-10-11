package com.finsim.domain

import com.finsim.core.Decimal
import com.finsim.engine.simulateLoan
import com.finsim.persistence.parseScenario
import com.finsim.persistence.serializeScenario
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParityCorpusTest {

    @Test
    fun testCorpusInvariantsAcrossAllScenarios() {
        val corpus = listOf(
            exampleScenario,
            exampleScenario.copy(currency = Currency.USD, principal = "5000.00", termMonths = 12),
            exampleScenario.copy(currency = Currency.EUR, principal = "8000.00", termMonths = 24),
            exampleScenario.copy(
                currency = Currency.ARS,
                principal = "100000.00",
                argentinaProfile = ArgentinaProfile(commissionRate = "0.03", vatRate = "0.21"),
                events = listOf(LoanEvent.Extra(id = "e1", month = 2, amount = "10000.00"))
            ),
            exampleScenario.copy(
                events = listOf(
                    LoanEvent.Recurring(id = "r1", month = 3, endMonth = 12, every = 3, amount = "500000.00"),
                    LoanEvent.Missed(id = "m1", month = 5),
                    LoanEvent.Catchup(id = "c1", month = 6, amount = "1000000.00")
                )
            ),
            exampleScenario.copy(
                source = "current",
                payment = "1500000.00",
                insurance = listOf(
                    Insurance(id = "fix", name = "Seguro Fijo", kind = "fixed", value = "50000.00", endsAtPayoff = false, endMonth = 40)
                )
            )
        )

        for (scen in corpus) {
            // 1. JSON Roundtrip
            val json = serializeScenario(scen)
            val parsed = parseScenario(json)
            assertEquals(scen.name, parsed.name)
            assertEquals(scen.currency, parsed.currency)
            assertEquals(scen.principal, parsed.principal)
            assertEquals(scen.termMonths, parsed.termMonths)

            // 2. Simulation Invariants
            val res = simulateLoan(parsed)
            assertTrue(res.rows.isNotEmpty())

            // Total cash paid = principal + interest + insurance + commission + vat
            val t = res.totals
            val sumParts = Decimal.parse(t.principal) + Decimal.parse(t.interest) + Decimal.parse(t.insurance) +
                    Decimal.parse(t.commissionPaid) + Decimal.parse(t.vatPaid)
            val diffCash = (Decimal.parse(t.cashPaid) - sumParts).abs()
            assertTrue(diffCash < Decimal.parse("0.05"), "Caja total en ${scen.name} debe coincidir con la suma de componentes")

            // Principal conservation if complete: total principal == initial principal
            if (res.complete) {
                assertEquals(scen.principal, t.principal, "Capital pagado debe igualar capital inicial si está liquidado")
                assertEquals("0.00", res.remainingDebt)
            }
        }
    }

    private fun Decimal.abs(): Decimal = if (this.isNegative) -this else this
}
