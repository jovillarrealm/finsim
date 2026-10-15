package com.finsim.domain

import kotlin.test.*

class LoanConditionsDraftTest {

    private fun sampleScenario(
        principal: String = "100000.00",
        termMonths: Int = 36,
        rate: Rate = Rate.Monthly("0.015"),
        currency: Currency = Currency.COP,
        name: String = "Crédito Hipotecario",
        source: String = "original",
        startDate: String = "2026-01",
        payment: String? = null
    ): Scenario = Scenario(
        name = name,
        currency = currency,
        source = source,
        principal = principal,
        startDate = startDate,
        termMonths = termMonths,
        rate = rate,
        payment = payment
    )

    @Test
    fun `baseline draft initializes with isDirty false and full parity with scenario`() {
        val scenario = sampleScenario(
            principal = "25000000.00",
            termMonths = 60,
            rate = Rate.Monthly("0.0185"),
            currency = Currency.COP,
            name = "Préstamo Vehículo",
            source = "original",
            startDate = "2026-06",
            payment = "550000.00"
        )

        val draft = LoanConditionsDraft.from(scenario)

        assertFalse(draft.isDirty)
        assertTrue(draft.isValid)
        assertTrue(draft.errors.isEmpty())
        assertNull(draft.firstErrorMessage)

        assertEquals("Préstamo Vehículo", draft.name)
        assertEquals(Currency.COP, draft.currency)
        assertEquals(PaymentOrigin.Original, draft.paymentOrigin)
        assertEquals("25000000.00", draft.principalStr)
        assertEquals("2026-06", draft.startDate)
        assertEquals("60", draft.termMonthsStr)
        assertEquals(RateKind.Monthly, draft.rateKind)
        // Rate percentage 0.0185 * 100 = 1.85
        assertEquals("1.85", draft.ratePercentageStr)
        assertEquals("550000.00", draft.paymentStr)
    }

    @Test
    fun `percentage to decimal fraction rate conversion on commit`() {
        val scenario = sampleScenario(rate = Rate.Monthly("0.01"))
        val draft = LoanConditionsDraft.from(scenario)

        // 1. Efectiva mensual 19.5% -> 0.195
        val updatedMonthly = draft.updateRate(RateKind.Monthly, "19.5")
        val committedMonthly = updatedMonthly.commit()
        assertNotNull(committedMonthly)
        assertTrue(committedMonthly.rate is Rate.Monthly)
        assertEquals("0.195", committedMonthly.rate.value)

        // 2. Tasa en 0% -> "0"
        val updatedZero = draft.updateRate(RateKind.Monthly, "0")
        val committedZero = updatedZero.commit()
        assertNotNull(committedZero)
        assertEquals("0", committedZero.rate.value)

        // 3. Efectiva anual 12.5% -> 0.125
        val updatedAnnual = draft.updateRate(RateKind.Annual, "12.5")
        val committedAnnual = updatedAnnual.commit()
        assertNotNull(committedAnnual)
        assertTrue(committedAnnual.rate is Rate.Annual)
        assertEquals("0.125", committedAnnual.rate.value)

        // 4. Nominal anual 18% con capitalización trimestral (4) -> Rate.Nominal("0.18", 4)
        val updatedNominal = draft.updateRate(RateKind.Nominal, "18.0", "4")
        val committedNominal = updatedNominal.commit()
        assertNotNull(committedNominal)
        assertTrue(committedNominal.rate is Rate.Nominal)
        assertEquals("0.18", (committedNominal.rate as Rate.Nominal).value)
        assertEquals(4, (committedNominal.rate as Rate.Nominal).frequency)
    }

    @Test
    fun `dirty tracking marks modifications and reverts cleanly to baseline in O(1)`() {
        val scenario = sampleScenario(principal = "50000.00", termMonths = 24)
        val draft = LoanConditionsDraft.from(scenario)
        assertFalse(draft.isDirty)

        // Modificar capital
        val draftP = draft.updatePrincipal("75000.00")
        assertTrue(draftP.isDirty)
        val revertedP = draftP.updatePrincipal("50000.00")
        assertFalse(revertedP.isDirty)

        // Modificar plazo
        val draftT = draft.updateTermMonths("48")
        assertTrue(draftT.isDirty)
        val revertedT = draftT.updateTermMonths("24")
        assertFalse(revertedT.isDirty)

        // Modificar tasa
        val draftR = draft.updateRate(RateKind.Annual, "15.0")
        assertTrue(draftR.isDirty)
        val revertedR = draftR.updateRate(RateKind.Monthly, "1.5")
        assertFalse(revertedR.isDirty)

        // Modificar moneda
        val draftC = draft.updateCurrency(Currency.USD)
        assertTrue(draftC.isDirty)
        val revertedC = draftC.updateCurrency(Currency.COP)
        assertFalse(revertedC.isDirty)

        // Modificar origen de saldo
        val draftO = draft.updatePaymentOrigin(PaymentOrigin.Current)
        assertTrue(draftO.isDirty)
        val revertedO = draftO.updatePaymentOrigin(PaymentOrigin.Original)
        assertFalse(revertedO.isDirty)

        // Modificar nombre
        val draftN = draft.updateName("Nuevo nombre")
        assertTrue(draftN.isDirty)
        val revertedN = draftN.updateName("Crédito Hipotecario")
        assertFalse(revertedN.isDirty)

        // Restablecer mediante reset()
        val modifiedMulti = draft
            .updatePrincipal("99999.00")
            .updateTermMonths("120")
            .updateCurrency(Currency.EUR)
        assertTrue(modifiedMulti.isDirty)
        val resetDraft = modifiedMulti.reset()
        assertFalse(resetDraft.isDirty)
        assertEquals("50000.00", resetDraft.principalStr)
        assertEquals("24", resetDraft.termMonthsStr)
        assertEquals(Currency.COP, resetDraft.currency)
    }

    @Test
    fun `validation rejections on negative principal, non-numeric term, out-of-range term, and empty name`() {
        val scenario = sampleScenario()
        val draft = LoanConditionsDraft.from(scenario)

        // 1. Capital negativo
        val invalidPrincipal = draft.updatePrincipal("-5000.00")
        assertFalse(invalidPrincipal.isValid)
        assertTrue(invalidPrincipal.errors.containsKey(LoanField.Principal))
        assertEquals("El capital debe ser mayor que cero.", invalidPrincipal.errors[LoanField.Principal])
        assertNull(invalidPrincipal.commit())
        assertTrue(invalidPrincipal.validateAndCommit().isFailure)

        // Capital no numérico
        val nonNumericPrincipal = draft.updatePrincipal("abc")
        assertFalse(nonNumericPrincipal.isValid)
        assertTrue(nonNumericPrincipal.errors.containsKey(LoanField.Principal))
        assertNull(nonNumericPrincipal.commit())

        // 2. Plazo no numérico
        val nonNumericTerm = draft.updateTermMonths("meses")
        assertFalse(nonNumericTerm.isValid)
        assertTrue(nonNumericTerm.errors.containsKey(LoanField.TermMonths))
        assertEquals("Indica un plazo en meses válido.", nonNumericTerm.errors[LoanField.TermMonths])
        assertNull(nonNumericTerm.commit())

        // 3. Plazo fuera de rango: 0
        val zeroTerm = draft.updateTermMonths("0")
        assertFalse(zeroTerm.isValid)
        assertTrue(zeroTerm.errors.containsKey(LoanField.TermMonths))
        assertEquals("El plazo debe estar entre 1 y 1200 meses.", zeroTerm.errors[LoanField.TermMonths])
        assertNull(zeroTerm.commit())

        // Plazo fuera de rango: 1500 (> MAX_MONTHS 1200)
        val highTerm = draft.updateTermMonths("1500")
        assertFalse(highTerm.isValid)
        assertTrue(highTerm.errors.containsKey(LoanField.TermMonths))
        assertEquals("El plazo debe estar entre 1 y 1200 meses.", highTerm.errors[LoanField.TermMonths])
        assertNull(highTerm.commit())

        // 4. Nombre vacío o sólo espacios
        val emptyName = draft.updateName("   ")
        assertFalse(emptyName.isValid)
        assertTrue(emptyName.errors.containsKey(LoanField.Name))
        assertEquals("El nombre debe tener entre 1 y 120 caracteres.", emptyName.errors[LoanField.Name])
        assertNull(emptyName.commit())

        // 5. Tasa negativa
        val negativeRate = draft.updateRate(RateKind.Monthly, "-3.5")
        assertFalse(negativeRate.isValid)
        assertTrue(negativeRate.errors.containsKey(LoanField.RatePercentage))
        assertEquals("La tasa de interés no puede ser negativa.", negativeRate.errors[LoanField.RatePercentage])
        assertNull(negativeRate.commit())

        // 6. Fecha de inicio inválida
        val invalidDate = draft.updateStartDate("2026-15")
        assertFalse(invalidDate.isValid)
        assertTrue(invalidDate.errors.containsKey(LoanField.StartDate))
        assertEquals("Usa año-mes válido.", invalidDate.errors[LoanField.StartDate])
        assertNull(invalidDate.commit())

        // 7. Frecuencia nominal inválida
        val invalidFreq = draft.updateRate(RateKind.Nominal, "12.0", "0")
        assertFalse(invalidFreq.isValid)
        assertTrue(invalidFreq.errors.containsKey(LoanField.NominalFrequency))
        assertEquals("Indica una frecuencia de capitalización válida.", invalidFreq.errors[LoanField.NominalFrequency])
        assertNull(invalidFreq.commit())
    }

    @Test
    fun `successful commit returns updated Scenario preserving intact secondary structures`() {
        val scenario = sampleScenario(
            principal = "10000.00",
            termMonths = 12,
            rate = Rate.Monthly("0.01")
        ).copy(
            insurance = listOf(
                Insurance(id = "ins1", name = "Seguro Vida", kind = "fixed", value = "25.00")
            ),
            events = listOf(
                LoanEvent.Extra(id = "ev1", month = 3, amount = "500.00")
            )
        )

        val draft = LoanConditionsDraft.from(scenario)
            .updateName("Préstamo Libre Inversión")
            .updatePrincipal("20000.00")
            .updateTermMonths("24")
            .updateRate(RateKind.Monthly, "2.5")
            .updateCurrency(Currency.USD)
            .updatePaymentOrigin(PaymentOrigin.Current)
            .updateStartDate("2026-03")
            .updatePayment("1100.00")

        assertTrue(draft.isDirty)
        assertTrue(draft.isValid)

        val committed = draft.commit()
        assertNotNull(committed)

        assertEquals("Préstamo Libre Inversión", committed.name)
        assertEquals("20000.00", committed.principal)
        assertEquals(24, committed.termMonths)
        assertEquals("0.025", committed.rate.value)
        assertEquals(Currency.USD, committed.currency)
        assertEquals("current", committed.source)
        assertEquals("2026-03", committed.startDate)
        assertEquals("1100.00", committed.payment)

        // Las estructuras preexistentes se preservan intactas
        assertEquals(1, committed.insurance.size)
        assertEquals("ins1", committed.insurance[0].id)
        assertEquals(1, committed.events.size)
        assertEquals("ev1", committed.events[0].id)
    }

    @Test
    fun `reset against a new scenario updates baseline and clears dirty tracking`() {
        val scenario1 = sampleScenario(name = "Original 1", principal = "1000.00")
        val scenario2 = sampleScenario(name = "Original 2", principal = "2000.00")

        val draft1 = LoanConditionsDraft.from(scenario1).updatePrincipal("9999.00")
        assertTrue(draft1.isDirty)

        val draft2 = draft1.reset(scenario2)
        assertFalse(draft2.isDirty)
        assertEquals("Original 2", draft2.name)
        assertEquals("2000.00", draft2.principalStr)
    }
}
