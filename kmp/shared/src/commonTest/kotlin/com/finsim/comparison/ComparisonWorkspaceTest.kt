package com.finsim.comparison

import com.finsim.persistence.InMemoryScenarioStorage
import kotlin.test.*

class ComparisonWorkspaceTest {
    @Test fun incompleteQuoteShowsKnownPaymentsWithoutInventingInterest() {
        val workspace = ComparisonWorkspace(InMemoryScenarioStorage())
        workspace.edit(ComparisonDraft(country = Country.CO, currency = com.finsim.domain.Currency.COP,
            cashGoal = Datum("1000"), offers = listOf(
                Offer(id = "A", count = "8", payment = Datum("150")),
                Offer(id = "B"))))
        assertTrue(workspace.apply())
        val a = workspace.state.result!!.offers[0]
        assertEquals("1200.00", a.knownTotal)
        assertNull(a.financeCost)
        assertNull(a.rows.first().interest)
        assertEquals("150.00", a.rows.first { it.month == 1 }.payment)
        val applied = workspace.state.result
        workspace.edit(workspace.state.draft.copy(cashGoal = Datum("1.001")))
        assertFalse(workspace.apply())
        assertEquals(applied, workspace.state.result)
        assertTrue(workspace.state.errors.containsKey("cashGoal"))
    }

    @Test fun savesIncompleteAppliedAndPendingWithoutApplyingOrLosingLegacyData() {
        val storage = InMemoryScenarioStorage()
        storage.setItem("finsim.scenario", "legacy")
        val w = ComparisonWorkspace(storage)
        w.edit(ComparisonDraft(country = Country.AR, currency = com.finsim.domain.Currency.ARS,
            cashGoal = Datum("1000"), offers = listOf(Offer("A", count = "8", payment = Datum("150")), Offer("B"))))
        assertTrue(w.apply())
        val pending = w.state.draft.copy(cashGoal = Datum("1200", Provenance.ESTIMATED))
        w.edit(pending)
        assertTrue(w.save())
        val resumed = ComparisonWorkspace(storage)
        assertEquals(pending.cashGoal, resumed.state.draft.cashGoal)
        assertEquals("1000", resumed.state.applied!!.cashGoal.text)
        assertEquals("1200.00", resumed.state.result!!.offers[0].knownTotal)
        assertTrue(resumed.state.hasPending)
        assertEquals("legacy", storage.getItem("finsim.scenario"))
        val before = resumed.state
        assertFalse(resumed.importJson("""{"type":"other","version":1}"""))
        assertEquals(before.draft, resumed.state.draft)
        val saved = storage.getItem(COMPARISON_STORAGE_KEY)
        resumed.edit(pending.copy(cashGoal = Datum("NaN")))
        assertFalse(resumed.save())
        assertEquals(saved, storage.getItem(COMPARISON_STORAGE_KEY))
        assertFalse(resumed.importJson("é".repeat(1_000_001)))
        assertTrue(resumed.state.message.contains("2 MB"))
    }


    @Test fun fixedRateSimulationUsesCanonicalRoundingAndCanRemainIncomplete() {
        val w = ComparisonWorkspace(InMemoryScenarioStorage())
        fun simulate(principal: String, rate: String, count: String): OfferResult {
            w.edit(ComparisonDraft(country = Country.CO, currency = com.finsim.domain.Currency.COP,
                cashGoal = Datum(principal), offers = listOf(Offer("A", mode = OfferMode.SIMULATION,
                    principal = Datum(principal), rate = Datum(rate), count = count, chargesConfirmed = true), Offer("B"))))
            assertTrue(w.apply(), w.state.message)
            return w.state.result!!.offers[0]
        }
        val a = simulate("1000", "1", "2")
        assertEquals(listOf("507.51", "507.51"), a.rows.filter { it.month > 0 }.map { it.payment })
        assertEquals(listOf("10.00", "5.02"), a.rows.filter { it.month > 0 }.map { it.interest })
        assertEquals("502.49", a.rows.first { it.month == 1 }.balance)
        assertEquals("1015.02", a.knownTotal)
        assertEquals(listOf("33.33", "33.33", "33.34"), simulate("100", "0", "3").rows.filter { it.month > 0 }.map { it.payment })
        assertFalse(simulate("100", "", "3").complete)
        assertTrue(simulate("100", "", "3").rows.all { it.payment == null })
    }


    @Test fun financedDeductedAndOwnFeesAreCountedOnlyOnce() {
        val w = ComparisonWorkspace(InMemoryScenarioStorage())
        val a = Offer("A", mode = OfferMode.SIMULATION, principal = Datum("1060"),
            rate = Datum("0"), count = "2", chargesConfirmed = true, fees = listOf(
                Fee("financed", amount = Datum("40"), treatment = FeeTreatment.FINANCED),
                Fee("deducted", amount = Datum("20"), treatment = FeeTreatment.DEDUCTED),
                Fee("own", amount = Datum("30"), treatment = FeeTreatment.INITIAL)))
        w.edit(ComparisonDraft(country = Country.CO, currency = com.finsim.domain.Currency.COP,
            cashGoal = Datum("1000"), offers = listOf(a, Offer("B"))))
        assertTrue(w.apply(), w.state.message)
        val r = w.state.result!!.offers[0]
        assertEquals("1000.00", r.net)
        assertEquals("30.00", r.initial)
        assertEquals("1090.00", r.knownTotal)
        assertEquals("90.00", r.financeCost)
        w.edit(w.state.draft.copy(offers = listOf(a.copy(fees = a.fees + Fee("future",
            amount = Datum("5"), treatment = FeeTreatment.SEPARATE, start = "3", recurring = true, end = "4")), Offer("B"))))
        assertTrue(w.apply())
        assertEquals(4, w.state.result!!.horizon)
        assertEquals("1100.00", w.state.result!!.offers[0].knownTotal)
        w.edit(w.state.draft.copy(offers = listOf(a.copy(fees = a.fees + Fee("unknown",
            treatment = FeeTreatment.INCLUDED)), Offer("B"))))
        assertTrue(w.apply())
        assertNull(w.state.result!!.offers[0].financeCost)
    }


    @Test fun samePurchaseSeparatesPriceInitialOutlayAndFinanceCost() {
        val w = ComparisonWorkspace(InMemoryScenarioStorage())
        w.edit(ComparisonDraft(country = Country.CO, currency = com.finsim.domain.Currency.COP,
            need = Need.PURCHASE, purchase = "Bicicleta", offers = listOf(
                Offer("A", mode = OfferMode.SIMULATION, price = Datum("900"), downPayment = Datum("200"),
                    principal = Datum("700"), rate = Datum("0"), count = "2", chargesConfirmed = true,
                    fees = listOf(Fee("own", amount = Datum("10"), treatment = FeeTreatment.INITIAL))),
                Offer("B", product = Product.CARD_EXISTING, price = Datum("1000"), downPayment = Datum("0"),
                    count = "4", payment = Datum("250"), chargesConfirmed = true))))
        assertTrue(w.apply(), w.state.message)
        val r = w.state.result!!
        assertEquals("910.00", r.offers[0].knownTotal)
        assertEquals("10.00", r.offers[0].financeCost)
        assertEquals("0.00", r.offers[1].financeCost)
        assertEquals("90.00", r.differences["Total pagado"])
        assertEquals("-210.00", r.differences["Salida inicial"])
        assertEquals("2", r.differences["Último mes"])
        assertEquals("100.00", r.differences["Precio"])
    }


    @Test fun existingCardChargesRemainCommonWhileNewCardFeesIncreaseOnlyNewCost() {
        val w = ComparisonWorkspace(InMemoryScenarioStorage())
        val base = Offer("A", product = Product.CARD_EXISTING, price = Datum("600"), downPayment = Datum("0"),
            count = "5", payment = Datum("120"), chargesConfirmed = true,
            fees = listOf(Fee("old", amount = Datum("20"), existing = true, coverage = Coverage.EXPENSES,
                recurring = true, start = "1", end = "5")))
        val new = base.copy(id = "B", product = Product.CARD_NEW, fees = listOf(
            Fee("new", amount = Datum("20"), recurring = true, start = "1", end = "5")))
        w.edit(ComparisonDraft(country = Country.AR, currency = com.finsim.domain.Currency.ARS,
            need = Need.PURCHASE, purchase = "Compra", offers = listOf(base, new)))
        assertTrue(w.apply(), w.state.message)
        assertEquals(listOf("600.00", "700.00"), w.state.result!!.offers.map { it.knownTotal })
        assertEquals(listOf("0.00", "100.00"), w.state.result!!.offers.map { it.financeCost })
        val included = base.copy(payment = Datum("140"), fees = listOf(base.fees[0].copy(treatment = FeeTreatment.INCLUDED)))
        w.edit(w.state.draft.copy(offers = listOf(included, new)))
        assertTrue(w.apply())
        assertEquals("600.00", w.state.result!!.offers[0].knownTotal)
        w.edit(w.state.draft.copy(offers = listOf(included.copy(fees = listOf(included.fees[0].copy(amount = Datum()))), new)))
        assertTrue(w.apply())
        assertNull(w.state.result!!.offers[0].financeCost)
    }


    @Test fun optionalPersonalContextShowsSignedMarginAndNeverDuplicatesExistingExpenses() {
        val w = ComparisonWorkspace(InMemoryScenarioStorage())
        val old = Fee("old", amount = Datum("20"), existing = true, coverage = Coverage.EXPENSES,
            recurring = true, start = "1", end = "5")
        val a = Offer("A", product = Product.CARD_EXISTING, price = Datum("600"), downPayment = Datum("0"),
            count = "5", payment = Datum("120"), chargesConfirmed = true, fees = listOf(old))
        val b = a.copy(id = "B", product = Product.CARD_NEW, fees = listOf(Fee("new",
            amount = Datum("20"), recurring = true, start = "1", end = "5")))
        val draft = ComparisonDraft(country = Country.AR, currency = com.finsim.domain.Currency.ARS,
            need = Need.PURCHASE, purchase = "Compra", offers = listOf(a, b),
            context = PersonalContext(Datum("1000"), Datum("400"), Datum("100")))
        w.edit(draft)
        assertTrue(w.apply(), w.state.message)
        assertEquals(listOf("380.00", "360.00"), w.state.result!!.offers.map { it.rows.first { r -> r.month == 1 }.margin })
        w.edit(draft.copy(context = draft.context!!.copy(income = Datum("600"))))
        assertTrue(w.apply())
        assertEquals(listOf("-20.00", "-40.00"), w.state.result!!.offers.map { it.rows.first { r -> r.month == 1 }.margin })
        w.edit(draft.copy(offers = listOf(a.copy(fees = listOf(old.copy(coverage = Coverage.UNKNOWN))), b)))
        assertTrue(w.apply())
        assertTrue(w.state.result!!.offers.all { it.rows.first { r -> r.month == 1 }.margin == null })
    }


    @Test fun hypotheticalEditsPreserveOriginalAndRevalidateQuotedPayments() {
        val storage = InMemoryScenarioStorage()
        val w = ComparisonWorkspace(storage)
        val original = ComparisonDraft(country = Country.CO, currency = com.finsim.domain.Currency.COP,
            cashGoal = Datum("1000"), offers = listOf(Offer("A", principal = Datum("1000"),
                count = "8", payment = Datum("150"), chargesConfirmed = true), Offer("B")))
        w.edit(original)
        assertTrue(w.apply())
        val edited = original.copy(offers = listOf(original.offers[0].copy(principal = Datum("1200")), Offer("B")))
        w.edit(edited)
        assertTrue(w.state.draft.offers[0].requiresPaymentReview)
        assertTrue(w.apply())
        assertEquals("150.00", w.state.result!!.offers[0].rows.first { it.month == 1 }.payment)
        assertNull(w.state.result!!.offers[0].financeCost)
        assertEquals(original, w.state.reference)
        w.edit(w.state.draft.copy(cashGoal = Datum("NaN")))
        assertFalse(w.apply())
        w.discard()
        assertEquals(w.state.applied, w.state.draft)
        assertTrue(w.save())
        val resumed = ComparisonWorkspace(storage)
        assertEquals(original, resumed.state.reference)
        assertTrue(resumed.state.draft.offers[0].requiresPaymentReview)
    }


    @Test fun bothCountriesKeepPublishedCostIndicatorsSeparateFromInterest() {
        for (country in Country.entries) for (currency in com.finsim.domain.Currency.entries) for (product in Product.entries) for (need in if (product == Product.LOAN) Need.entries else listOf(Need.PURCHASE)) {
            val w = ComparisonWorkspace(InMemoryScenarioStorage())
            val quote = Offer("A", product = product, price = Datum("100"), downPayment = Datum("0"),
                count = "1", payment = Datum("110"), chargesConfirmed = true,
                indicators = listOf(PublishedIndicator(IndicatorKind.CFT, Datum("88.50"), unit = "% anual", taxBase = "con IVA")))
            w.edit(ComparisonDraft(country = country, currency = currency, cashGoal = Datum("100"), need = need,
                purchase = "Compra", offers = listOf(quote, quote.copy(id = "B", mode = OfferMode.SIMULATION,
                    principal = Datum("100"), rate = Datum("0"), payment = Datum()))))
            assertTrue(w.apply(), w.state.message)
            assertEquals("110.00", w.state.result!!.offers[0].quotedPayments)
            assertEquals("100.00", w.state.result!!.offers[1].quotedPayments)
            val json = w.exportJson().getOrThrow()
            assertFalse(w.importJson(json.replace("\"rateKind\":\"EM\"", "\"rateKind\":\"CFT\"")))
            assertFalse(w.importJson(json.replace("\"name\":\"\"", "\"profile\":\"contrasted\",\"name\":\"\"")))
        }
    }


    @Test fun commonHorizonIncludesContinuingUnknownFeesWithoutInventingFutureZero() {
        val w = ComparisonWorkspace(InMemoryScenarioStorage())
        w.edit(ComparisonDraft(country = Country.CO, currency = com.finsim.domain.Currency.COP, cashGoal = Datum("200"),
            offers = listOf(
                Offer("A", count = "2", payment = Datum("100"), chargesConfirmed = true,
                    fees = listOf(Fee("open", amount = Datum("10"), recurring = true, start = "1"))),
                Offer("B", count = "4", payment = Datum("50"), chargesConfirmed = true))))
        assertTrue(w.apply())
        val a = w.state.result!!.offers[0]
        assertEquals(4, w.state.result!!.horizon)
        assertEquals("240.00", a.knownTotal)
        assertEquals("10.00", a.rows.first { it.month == 4 }.extra)
        assertTrue(a.rows.first { it.month == 4 }.uncertain)
        assertNull(a.financeCost)
    }


    @Test fun monthZeroSeparatesInitialCashFromSameMonthInstallment() {
        val w = ComparisonWorkspace(InMemoryScenarioStorage())
        w.edit(ComparisonDraft(country = Country.CO, currency = com.finsim.domain.Currency.COP, cashGoal = Datum("100"),
            context = PersonalContext(Datum("100"), Datum("0"), Datum("0")),
            offers = listOf(Offer("A", count = "2", firstMonth = "0", payment = Datum("50"),
                chargesConfirmed = true, fees = listOf(Fee("upfront", amount = Datum("10"), treatment = FeeTreatment.INITIAL))), Offer("B"))))
        assertTrue(w.apply(), w.state.message)
        val row = w.state.result!!.offers[0].rows.first()
        assertEquals("50.00", row.payment)
        assertEquals("10.00", row.initial)
        assertEquals("40.00", row.margin)
        assertEquals("110.00", w.state.result!!.offers[0].knownTotal)
    }

    @Test fun insurancePercentPrecisionAndPostLoanPaymentsUseCanonicalInsurance() {
        val w = ComparisonWorkspace(InMemoryScenarioStorage())
        val a = Offer("A", mode = OfferMode.SIMULATION, principal = Datum("100"), rate = Datum("0"), count = "1",
            chargesConfirmed = true, insurance = listOf(OfferInsurance("pct", percentage = true, value = Datum("0.125"))))
        w.edit(ComparisonDraft(country = Country.CO, currency = com.finsim.domain.Currency.COP,
            cashGoal = Datum("100"), offers = listOf(a, Offer("B"))))
        assertTrue(w.apply(), w.state.errors.toString())
        assertEquals("100.13", w.state.result!!.offers[0].knownTotal)
        w.edit(w.state.draft.copy(offers = listOf(a.copy(insurance = listOf(OfferInsurance("fixed", value = Datum("10"),
            endsAtPayoff = false, endMonth = "3"))), Offer("B"))))
        assertTrue(w.apply())
        assertEquals("130.00", w.state.result!!.offers[0].knownTotal)
        assertEquals(3, w.state.result!!.horizon)
    }

    @Test fun incompatibleCurrencyAndInvalidReferencesDoNotBecomeEquivalentComparisons() {
        val w = ComparisonWorkspace(InMemoryScenarioStorage())
        val a = Offer("A", count = "2", payment = Datum("50"), chargesConfirmed = true)
        val d = ComparisonDraft(country = Country.CO, currency = com.finsim.domain.Currency.COP, cashGoal = Datum("100"),
            offers = listOf(a, a.copy(id = "B", currency = com.finsim.domain.Currency.ARS)))
        w.edit(d)
        assertTrue(w.apply())
        assertFalse(w.state.result!!.equivalent)
        assertTrue(w.state.result!!.differences.isEmpty())
        val last = w.state.result
        for (invalid in listOf("-1", "NaN", "Infinity", "0.001", "1000000000000001")) {
            w.edit(d.copy(cashGoal = Datum(invalid)))
            assertFalse(w.apply(), invalid)
            assertEquals(last, w.state.result)
        }
        w.edit(d.copy(offers = listOf(a.copy(exceptions = listOf(PaymentException("1", Datum("50")), PaymentException("01", Datum("50")))), d.offers[1])))
        assertFalse(w.apply())
        w.edit(d.copy(offers = listOf(a.copy(count = "1200", firstMonth = "2"), d.offers[1])))
        assertFalse(w.apply())
    }

    @Test fun storageFailureAndCorruptSavedDataNeverOverwriteValidMemoryOrDisk() {
        val storage = object : com.finsim.persistence.ScenarioStorage {
            var text: String? = null
            var fail = false
            override fun getItem(key: String) = text
            override fun setItem(key: String, value: String) { if (fail) error("quota"); text = value }
        }
        val w = ComparisonWorkspace(storage)
        w.edit(ComparisonDraft(cashGoal = Datum("100")))
        assertTrue(w.save())
        val saved = storage.text
        storage.fail = true
        w.edit(w.state.draft.copy(cashGoal = Datum("200")))
        assertFalse(w.save())
        assertEquals("200", w.state.draft.cashGoal.text)
        assertEquals(saved, storage.text)
        storage.text = "{corrupt"
        val resumed = ComparisonWorkspace(storage)
        assertTrue(resumed.state.message.isNotBlank())
        assertNull(resumed.state.result)
        assertEquals("{corrupt", storage.text)
    }

    @Test fun simulatedIncludedFeeIsPartOfPaymentAndNotChargedAgain() {
        val w = ComparisonWorkspace(InMemoryScenarioStorage())
        w.edit(ComparisonDraft(country = Country.CO, currency = com.finsim.domain.Currency.COP, cashGoal = Datum("100"),
            offers = listOf(Offer("A", mode = OfferMode.SIMULATION, principal = Datum("100"), rate = Datum("0"), count = "1",
                chargesConfirmed = true, fees = listOf(Fee("included", amount = Datum("10"), treatment = FeeTreatment.INCLUDED))), Offer("B"))))
        assertTrue(w.apply())
        assertEquals("110.00", w.state.result!!.offers[0].knownTotal)
        assertEquals("110.00", w.state.result!!.offers[0].rows.first { it.month == 1 }.payment)
        assertEquals("0.00", w.state.result!!.offers[0].rows.first { it.month == 1 }.extra)
    }


    @Test fun unknownInstallmentDoesNotBecomeANegativeSubtotalWhenExistingFeeIsIncluded() {
        val w = ComparisonWorkspace(InMemoryScenarioStorage())
        w.edit(ComparisonDraft(country = Country.CO, currency = com.finsim.domain.Currency.COP, cashGoal = Datum("100"),
            offers = listOf(Offer("A", count = "1", chargesConfirmed = true,
                fees = listOf(Fee("existing", existing = true, amount = Datum("20"), treatment = FeeTreatment.INCLUDED))), Offer("B"))))
        assertTrue(w.apply())
        assertEquals("0.00", w.state.result!!.offers[0].knownTotal)
        assertEquals("0.00", w.state.result!!.offers[0].rows.first { it.month == 1 }.cash)
        assertNull(w.state.result!!.offers[0].financeCost)
    }

    @Test fun discardingSavedPendingEditsMarksTheChangedDocumentUnsaved() {
        val storage = InMemoryScenarioStorage()
        val w = ComparisonWorkspace(storage)
        w.edit(ComparisonDraft(cashGoal = Datum("100")))
        assertTrue(w.apply())
        w.edit(w.state.draft.copy(cashGoal = Datum("200")))
        assertTrue(w.save())
        assertFalse(w.state.isDirty)
        w.discard()
        assertTrue(w.state.isDirty)
        assertEquals("100", w.state.draft.cashGoal.text)
        assertTrue(w.save())
        assertEquals("100", ComparisonWorkspace(storage).state.draft.cashGoal.text)
    }

    @Test fun knownCompatiblePaymentAbsorbsOnlyRoundingResidueAtMaturity() {
        val w = ComparisonWorkspace(InMemoryScenarioStorage())
        w.edit(ComparisonDraft(country = Country.CO, currency = com.finsim.domain.Currency.COP, cashGoal = Datum("100"),
            offers = listOf(Offer("A", mode = OfferMode.SIMULATION, principal = Datum("100"), rate = Datum("0"), count = "3",
                payment = Datum("33.33"), chargesConfirmed = true), Offer("B"))))
        assertTrue(w.apply())
        assertEquals(listOf("33.33", "33.33", "33.34"), w.state.result!!.offers[0].rows.filter { it.installment }.map { it.payment })
        assertEquals(3, w.state.result!!.offers[0].lastMonth)
    }

}
