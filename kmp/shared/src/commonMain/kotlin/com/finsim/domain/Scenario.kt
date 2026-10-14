package com.finsim.domain

import com.finsim.core.Decimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val MAX_MONTHS = 1200

@Serializable
enum class Currency {
    COP, ARS, USD, EUR
}

@Serializable
sealed class Rate {
    abstract val value: String

    @Serializable
    @SerialName("monthly")
    data class Monthly(override val value: String) : Rate()

    @Serializable
    @SerialName("annual")
    data class Annual(override val value: String) : Rate()

    @Serializable
    @SerialName("nominal")
    data class Nominal(
        override val value: String,
        val frequency: Int
    ) : Rate()
}

@Serializable
data class Insurance(
    val id: String,
    val name: String,
    val kind: String, // "fixed" or "percentage"
    val value: String,
    val endsAtPayoff: Boolean = true,
    val endMonth: Int? = null
) {
    fun validate() {
        require(id.isNotBlank()) { "El identificador del seguro no puede estar vacío." }
        require(name.isNotBlank()) { "El nombre del seguro no puede estar vacío." }
        require(kind == "fixed" || kind == "percentage") { "El tipo de seguro debe ser 'fixed' o 'percentage'." }
        if (!endsAtPayoff) {
            require(endMonth != null && endMonth in 1..MAX_MONTHS) { "Indica el último mes del seguro." }
        }
    }
}

@Serializable
sealed class LoanEvent {
    abstract val id: String
    abstract val month: Int

    @Serializable
    @SerialName("extra")
    data class Extra(
        override val id: String,
        override val month: Int,
        val amount: String
    ) : LoanEvent()

    @Serializable
    @SerialName("recurring")
    data class Recurring(
        override val id: String,
        override val month: Int,
        val endMonth: Int,
        val every: Int = 1,
        val amount: String
    ) : LoanEvent()

    @Serializable
    @SerialName("missed")
    data class Missed(
        override val id: String,
        override val month: Int
    ) : LoanEvent()

    @Serializable
    @SerialName("catchup")
    data class Catchup(
        override val id: String,
        override val month: Int,
        val amount: String
    ) : LoanEvent()
}

@Serializable
data class ArgentinaProfile(
    val originationDate: String? = null, // YYYY-MM
    val originalTermMonths: Int? = null,
    val contractualProfile: String? = null,
    val commissionRate: String = "0.03", // 3%
    val vatRate: String = "0.21" // 21%
)

@Serializable
data class Scenario(
    val version: Int = 1,
    val currency: Currency = Currency.COP,
    val name: String,
    val source: String = "original", // "original" or "current"
    val principal: String,
    val startDate: String, // YYYY-MM
    val termMonths: Int,
    val rate: Rate,
    val payment: String? = null,
    val insurance: List<Insurance> = emptyList(),
    val events: List<LoanEvent> = emptyList(),
    val argentinaProfile: ArgentinaProfile? = null
) {
    fun validate() {
        require(version == 1) { "La versión del escenario no es compatible. Se admite la versión 1." }
        require(name.isNotBlank() && name.length <= 120) { "El nombre debe tener entre 1 y 120 caracteres." }
        require(source == "original" || source == "current") { "El origen debe ser 'original' o 'current'." }
        require(termMonths in 1..MAX_MONTHS) { "El plazo debe estar entre 1 y $MAX_MONTHS meses." }
        require(Regex("""^\d{4}-(0[1-9]|1[0-2])$""").matches(startDate)) { "Usa año-mes válido." }

        val p = Decimal.parse(principal)
        require(p.isPositive) { "El capital debe ser mayor que cero." }
        require(p <= Decimal.parse("1000000000000000")) { "El máximo admitido es 1.000.000.000.000.000 en la moneda seleccionada." }

        if (payment != null) {
            val pay = Decimal.parse(payment)
            require(pay.isPositive) { "La cuota debe ser mayor que cero." }
        }

        insurance.forEach { it.validate() }
        val insuranceIds = insurance.map { it.id }.toSet()
        require(insuranceIds.size == insurance.size) { "Los identificadores de seguro no deben repetirse." }

        events.forEach { event ->
            require(event.month in 1..MAX_MONTHS) { "El mes del evento debe estar entre 1 y $MAX_MONTHS." }
            when (event) {
                is LoanEvent.Extra -> {
                    val amt = Decimal.parse(event.amount)
                    require(amt.isPositive) { "El importe debe ser mayor que cero." }
                }
                is LoanEvent.Recurring -> {
                    require(event.endMonth >= event.month) { "El mes final debe ser igual o posterior al inicial." }
                    require(event.endMonth in 1..MAX_MONTHS) { "El mes final debe estar entre 1 y $MAX_MONTHS." }
                    require(event.every in 1..MAX_MONTHS) { "La frecuencia debe ser de al menos 1 mes." }
                    val amt = Decimal.parse(event.amount)
                    require(amt.isPositive) { "El importe debe ser mayor que cero." }
                }
                is LoanEvent.Catchup -> {
                    val amt = Decimal.parse(event.amount)
                    require(amt.isPositive) { "El importe debe ser mayor que cero." }
                }
                is LoanEvent.Missed -> {}
            }
        }
        val eventIds = events.map { it.id }.toSet()
        require(eventIds.size == events.size) { "Los identificadores de eventos no deben repetirse." }
    }
}

@Serializable
data class Allocation(
    val offered: String,
    val applied: String,
    val insurance: String,
    val interest: String,
    val principal: String,
    val unapplied: String
)

@Serializable
data class MonthRow(
    val month: Int,
    val date: String,
    val openingBalance: String,
    val interestCharged: String,
    val insuranceCharged: String,
    val scheduled: Allocation,
    val catchup: Allocation,
    val extra: Allocation,
    val closingBalance: String,
    val overduePrincipal: String,
    val pendingInterest: String,
    val pendingInsurance: String,
    val arrears: String,
    val debt: String,
    val cashPaid: String,
    val commissionPaid: String = "0.00",
    val vatPaid: String = "0.00"
)

@Serializable
data class Totals(
    var principal: String = "0.00",
    var interest: String = "0.00",
    var insurance: String = "0.00",
    var cashPaid: String = "0.00",
    var unapplied: String = "0.00",
    var commissionPaid: String = "0.00",
    var vatPaid: String = "0.00"
)

@Serializable
data class UnappliedEvent(
    val eventId: String,
    val month: Int,
    val amount: String,
    val reason: String
)

@Serializable
data class LoanResult(
    val rows: List<MonthRow>,
    val basePayment: String,
    val payoffMonth: Int?,
    val payoffDate: String?,
    val lastInsuranceMonth: Int?,
    val complete: Boolean,
    val totals: Totals,
    val remainingDebt: String,
    val unappliedEvents: List<UnappliedEvent>
)

val exampleScenario = Scenario(
    version = 1,
    currency = Currency.COP,
    name = "Ejemplo · Crédito de libre inversión",
    source = "original",
    principal = "20000000.00",
    startDate = "2026-10",
    termMonths = 36,
    rate = Rate.Annual(value = "0.18"),
    insurance = listOf(
        Insurance(id = "life", name = "Seguro de vida", kind = "percentage", value = "0.0005", endsAtPayoff = true)
    ),
    events = emptyList()
)
