package com.finsim.domain.types

import com.finsim.core.Decimal
import com.finsim.domain.Currency
import com.finsim.domain.MAX_MONTHS
import kotlin.jvm.JvmInline

/**
 * Type-safe identifier for events, insurance items, and reconciliation records.
 *
 * Invariant: Cannot be empty or whitespace.
 */
@JvmInline
value class EventId(val value: String) {
    init {
        require(value.isNotBlank()) { "El identificador del evento no puede estar vacío." }
    }

    override fun toString(): String = value
}

/**
 * 1-based domain index for monthly simulation steps.
 *
 * Invariant: Must strictly satisfy 1 <= value <= MAX_MONTHS (1200).
 */
@JvmInline
value class MonthIndex(val value: Int) : Comparable<MonthIndex> {
    init {
        require(value in 1..MAX_MONTHS) { "El mes debe estar entre 1 y $MAX_MONTHS: $value" }
    }

    override fun compareTo(other: MonthIndex): Int = this.value.compareTo(other.value)

    operator fun plus(months: Int): MonthIndex = MonthIndex(value + months)

    operator fun minus(other: MonthIndex): Int = this.value - other.value

    fun next(): MonthIndex? = if (value < MAX_MONTHS) MonthIndex(value + 1) else null

    /**
     * Projects this month index into a calendar string "YYYY-MM" relative to loan origination.
     */
    fun toDate(originationYear: Int, originationMonth: Int): String {
        val dateIndex = originationYear * 12 + originationMonth - 1 + value - 1
        val year = dateIndex / 12
        val month = dateIndex % 12 + 1
        return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}"
    }

    override fun toString(): String = value.toString()

    companion object {
        val FIRST = MonthIndex(1)
        val MAX = MonthIndex(MAX_MONTHS)
    }
}

/**
 * Exact monetary value pinned to 2 decimal places (cents) with currency tagging.
 *
 * Invariants:
 * - Cents must be non-negative (loans in this simulator never forgive capital into negative balances).
 * - Cents must not exceed MAX_CENTS (1.000.000.000.000.000,00).
 * - Multi-currency operations across distinct currencies are rejected at the seam.
 * - External ingestion rejects strings with more than two decimal places.
 */
data class Money(
    val cents: Long,
    val currency: Currency = Currency.COP
) : Comparable<Money> {

    init {
        require(cents >= 0L) { "El importe monetario no puede ser negativo: $cents centavos" }
        require(cents <= MAX_CENTS) { "El importe excede el límite máximo admitido: $cents centavos" }
    }

    val isZero: Boolean get() = cents == 0L
    val isPositive: Boolean get() = cents > 0L

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return this.cents.compareTo(other.cents)
    }

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(this.cents + other.cents, currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        require(this.cents >= other.cents) {
            "El saldo no puede ser negativo ($this - $other en $currency)"
        }
        return Money(this.cents - other.cents, currency)
    }

    /**
     * Safe subtraction floor at zero: max(0, this - other).
     */
    fun minusOrZero(other: Money): Money {
        requireSameCurrency(other)
        return if (this.cents <= other.cents) zero(currency) else Money(this.cents - other.cents, currency)
    }

    /**
     * Multiplies this monetary amount by a 40-digit rate fraction, rounding half-up to exact cents.
     */
    fun timesFraction(fraction: Decimal): Money {
        if (isZero || fraction.isZero) return zero(currency)
        val moneyDec = Decimal.parse(toFormattedString())
        val product = moneyDec * fraction
        val roundedStr = product.toFixed(2)
        return parse(roundedStr, currency)
    }

    fun toFormattedString(): String {
        val units = cents / 100L
        val rem = cents % 100L
        return "$units.${rem.toString().padStart(2, '0')}"
    }

    override fun toString(): String = toFormattedString()

    private fun requireSameCurrency(other: Money) {
        require(this.currency == other.currency) {
            "Operación monetaria entre divisas incompatibles: ${this.currency} vs ${other.currency}"
        }
    }

    companion object {
        const val MAX_CENTS = 100_000_000_000_000_000L // 10^17 cents = 10^15 units

        fun zero(currency: Currency = Currency.COP) = Money(0L, currency)

        fun ofUnits(units: Long, currency: Currency = Currency.COP) = Money(units * 100L, currency)

        /**
         * Parses a monetary string like "100.00" or "50".
         *
         * Enforces financial convention:
         * "Los importes ingresados o importados deben tener como máximo dos decimales; rechazar mayor precisión con una explicación."
         */
        fun parse(str: String, currency: Currency = Currency.COP): Money {
            val s = str.trim()
            require(s.isNotEmpty()) { "El importe monetario no puede estar vacío." }
            require(!s.startsWith("-")) { "El importe monetario no puede ser negativo: $s" }

            val dotIdx = s.indexOf('.')
            val cents: Long = if (dotIdx < 0) {
                val units = s.toLongOrNull() ?: throw IllegalArgumentException("Formato monetario inválido: $s")
                units * 100L
            } else {
                val intPartStr = s.substring(0, dotIdx).ifEmpty { "0" }
                val fracPartStr = s.substring(dotIdx + 1)
                require(fracPartStr.length <= 2) {
                    "Los importes ingresados o importados deben tener como máximo dos decimales; se rechazó '$s'."
                }
                val units = intPartStr.toLongOrNull() ?: throw IllegalArgumentException("Formato monetario inválido: $s")
                val fracPadded = fracPartStr.padEnd(2, '0')
                val frac = fracPadded.toLongOrNull() ?: throw IllegalArgumentException("Formato monetario inválido: $s")
                units * 100L + frac
            }
            return Money(cents, currency)
        }
    }
}

/**
 * Sealed value hierarchy for financial compounding rates.
 *
 * Distinguishes Effective Monthly, Effective Annual (EA), and Nominal Annual rates at compile-time.
 * Encapsulates 40-digit high-precision decimal conversion behind the seam.
 */
sealed class RateValue {
    abstract val fractionDecimal: Decimal
    abstract val effectiveMonthlyDecimal: Decimal

    val isZero: Boolean get() = effectiveMonthlyDecimal.isZero

    data class EffectiveMonthly(override val fractionDecimal: Decimal) : RateValue() {
        init {
            require(!fractionDecimal.isNegative) { "La tasa mensual no puede ser negativa: $fractionDecimal" }
        }

        override val effectiveMonthlyDecimal: Decimal get() = fractionDecimal

        companion object {
            val ZERO = EffectiveMonthly(Decimal.ZERO)
            fun of(str: String) = EffectiveMonthly(Decimal.parse(str))
        }
    }

    data class EffectiveAnnual(override val fractionDecimal: Decimal) : RateValue() {
        init {
            require(!fractionDecimal.isNegative) { "La tasa anual no puede ser negativa: $fractionDecimal" }
        }

        override val effectiveMonthlyDecimal: Decimal by lazy {
            val one = Decimal.ONE
            val twelfth = one / Decimal.fromInt(12)
            (fractionDecimal + one).pow(twelfth) - one
        }

        companion object {
            fun of(str: String) = EffectiveAnnual(Decimal.parse(str))
        }
    }

    data class NominalAnnual(
        override val fractionDecimal: Decimal,
        val frequency: Int
    ) : RateValue() {
        init {
            require(!fractionDecimal.isNegative) { "La tasa nominal no puede ser negativa: $fractionDecimal" }
            require(frequency >= 1) { "La frecuencia de capitalización debe ser mayor o igual a 1: $frequency" }
        }

        override val effectiveMonthlyDecimal: Decimal by lazy {
            val one = Decimal.ONE
            val freq = Decimal.fromInt(frequency)
            val exp = freq / Decimal.fromInt(12)
            (fractionDecimal / freq + one).pow(exp) - one
        }

        companion object {
            fun of(str: String, frequency: Int) = NominalAnnual(Decimal.parse(str), frequency)
        }
    }

    fun toEffectiveMonthly(): EffectiveMonthly = EffectiveMonthly(effectiveMonthlyDecimal)

    fun toEffectiveAnnual(): EffectiveAnnual {
        val one = Decimal.ONE
        val annual = (effectiveMonthlyDecimal + one).pow(12) - one
        return EffectiveAnnual(annual)
    }

    fun toNominalAnnual(frequency: Int): NominalAnnual {
        require(frequency >= 1) { "Frecuencia requerida >= 1" }
        val one = Decimal.ONE
        val freq = Decimal.fromInt(frequency)
        val exp = Decimal.fromInt(12) / freq
        val base = (effectiveMonthlyDecimal + one).pow(exp) - one
        return NominalAnnual(base * freq, frequency)
    }
}

/**
 * Domain charge model for insurance items, replacing stringly-typed kinds.
 */
sealed class InsuranceCharge {
    data class Fixed(val amount: Money) : InsuranceCharge()
    data class Percentage(val monthlyRateFraction: Decimal) : InsuranceCharge() {
        init {
            require(!monthlyRateFraction.isNegative) { "La tasa de seguro no puede ser negativa: $monthlyRateFraction" }
        }
    }
}

data class TypedInsurance(
    val id: EventId,
    val name: String,
    val charge: InsuranceCharge,
    val endsAtPayoff: Boolean = true,
    val endMonth: MonthIndex? = null
) {
    init {
        require(name.isNotBlank()) { "El nombre del seguro no puede estar vacío." }
        if (!endsAtPayoff) {
            requireNotNull(endMonth) { "Indica el último mes del seguro si continúa tras liquidación." }
        }
    }
}

sealed class TypedLoanEvent {
    abstract val id: EventId
    abstract val month: MonthIndex

    data class Extra(
        override val id: EventId,
        override val month: MonthIndex,
        val amount: Money
    ) : TypedLoanEvent() {
        init {
            require(amount.isPositive) { "El importe del abono extraordinario debe ser mayor que cero." }
        }
    }

    data class Recurring(
        override val id: EventId,
        override val month: MonthIndex,
        val endMonth: MonthIndex,
        val everyMonths: Int = 1,
        val amount: Money
    ) : TypedLoanEvent() {
        init {
            require(endMonth >= month) { "El mes final debe ser igual o posterior al inicial." }
            require(everyMonths in 1..MAX_MONTHS) { "La frecuencia debe ser de al menos 1 mes." }
            require(amount.isPositive) { "El importe del abono recurrente debe ser mayor que cero." }
        }
    }

    data class Missed(
        override val id: EventId,
        override val month: MonthIndex
    ) : TypedLoanEvent()

    data class Catchup(
        override val id: EventId,
        override val month: MonthIndex,
        val amount: Money
    ) : TypedLoanEvent() {
        init {
            require(amount.isPositive) { "El importe de la recuperación debe ser mayor que cero." }
        }
    }
}

data class ArgentinaPrepaymentPolicy(
    val originalTermMonths: Int?,
    val commissionRate: Decimal = Decimal.parse("0.03"), // 3%
    val vatRate: Decimal = Decimal.parse("0.21") // 21%
)

data class TypedScenario(
    val currency: Currency = Currency.COP,
    val name: String,
    val source: String = "original",
    val principal: Money,
    val startDate: String, // YYYY-MM
    val termMonths: Int,
    val rate: RateValue,
    val payment: Money? = null,
    val insurance: List<TypedInsurance> = emptyList(),
    val events: List<TypedLoanEvent> = emptyList(),
    val argentinaPolicy: ArgentinaPrepaymentPolicy? = null
) {
    init {
        require(name.isNotBlank() && name.length <= 120) { "El nombre debe tener entre 1 y 120 caracteres." }
        require(source == "original" || source == "current") { "El origen debe ser 'original' o 'current'." }
        require(termMonths in 1..MAX_MONTHS) { "El plazo debe estar entre 1 y $MAX_MONTHS meses." }
        require(Regex("""^\d{4}-(0[1-9]|1[0-2])$""").matches(startDate)) { "Usa año-mes válido." }
        require(principal.isPositive) { "El capital debe ser mayor que cero." }
        require(principal.currency == currency) { "La divisa del capital no coincide con la del escenario." }
        payment?.let {
            require(it.isPositive) { "La cuota debe ser mayor que cero." }
            require(it.currency == currency) { "La divisa de la cuota no coincide con la del escenario." }
        }
        val insIds = insurance.map { it.id }.toSet()
        require(insIds.size == insurance.size) { "Los identificadores de seguro no deben repetirse." }
        val evIds = events.map { it.id }.toSet()
        require(evIds.size == events.size) { "Los identificadores de eventos no deben repetirse." }
    }
}

data class TypedAllocation(
    val offered: Money,
    val applied: Money,
    val insurance: Money,
    val interest: Money,
    val principal: Money,
    val unapplied: Money
)

data class TypedMonthRow(
    val month: MonthIndex,
    val date: String,
    val openingBalance: Money,
    val interestCharged: Money,
    val insuranceCharged: Money,
    val scheduled: TypedAllocation,
    val catchup: TypedAllocation,
    val extra: TypedAllocation,
    val closingBalance: Money,
    val overduePrincipal: Money,
    val pendingInterest: Money,
    val pendingInsurance: Money,
    val arrears: Money,
    val debt: Money,
    val cashPaid: Money,
    val commissionPaid: Money,
    val vatPaid: Money
)

data class TypedTotals(
    var principal: Money,
    var interest: Money,
    var insurance: Money,
    var cashPaid: Money,
    var unapplied: Money,
    var commissionPaid: Money,
    var vatPaid: Money
) {
    companion object {
        fun zero(currency: Currency) = TypedTotals(
            principal = Money.zero(currency),
            interest = Money.zero(currency),
            insurance = Money.zero(currency),
            cashPaid = Money.zero(currency),
            unapplied = Money.zero(currency),
            commissionPaid = Money.zero(currency),
            vatPaid = Money.zero(currency)
        )
    }
}

data class TypedUnappliedEvent(
    val eventId: EventId,
    val month: MonthIndex,
    val amount: Money,
    val reason: String
)

data class TypedLoanResult(
    val rows: List<TypedMonthRow>,
    val basePayment: Money,
    val payoffMonth: MonthIndex?,
    val payoffDate: String?,
    val lastInsuranceMonth: MonthIndex?,
    val complete: Boolean,
    val totals: TypedTotals,
    val remainingDebt: Money,
    val unappliedEvents: List<TypedUnappliedEvent>
)
