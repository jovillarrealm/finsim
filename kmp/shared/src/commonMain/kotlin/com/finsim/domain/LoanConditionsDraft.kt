package com.finsim.domain

import com.finsim.core.Decimal
import kotlin.jvm.JvmInline

/**
 * Discriminante para el origen del saldo base del préstamo.
 *
 * Satisface la interfaz del módulo en el seam del formulario de condiciones.
 */
enum class PaymentOrigin(val wire: String, val displayName: String) {
    Original("original", "Préstamo original"),
    Current("current", "Saldo actual");

    companion object {
        fun fromWire(wire: String): PaymentOrigin = when (wire.trim().lowercase()) {
            "current" -> Current
            else -> Original
        }
    }
}

/**
 * Discriminante para las modalidades de tasa admitidas por el préstamo.
 */
enum class RateKind(val wire: String, val displayName: String) {
    Monthly("monthly", "Efectiva mensual (EM)"),
    Annual("annual", "Efectiva anual (EA)"),
    Nominal("nominal", "Nominal anual");

    companion object {
        fun fromDomain(rate: Rate): RateKind = when (rate) {
            is Rate.Monthly -> Monthly
            is Rate.Annual -> Annual
            is Rate.Nominal -> Nominal
        }

        fun fromWire(wire: String): RateKind = when (wire.trim().lowercase()) {
            "monthly", "em" -> Monthly
            "annual", "ea" -> Annual
            "nominal" -> Nominal
            else -> Monthly
        }
    }
}

/**
 * Campos editables del borrador de condiciones de préstamo.
 */
enum class LoanField {
    Name,
    Currency,
    PaymentOrigin,
    Principal,
    StartDate,
    TermMonths,
    RateKind,
    RatePercentage,
    NominalFrequency,
    Payment,
    General
}

/**
 * Estado inmutable de los parámetros en borrador del préstamo.
 */
data class LoanDraftState(
    val name: String,
    val currency: Currency,
    val paymentOrigin: PaymentOrigin,
    val principalStr: String,
    val startDate: String,
    val termMonthsStr: String,
    val rateKind: RateKind,
    val ratePercentageStr: String,
    val nominalFrequencyStr: String = "12",
    val paymentStr: String = ""
)

/**
 * Módulo profundo para la preparación y validación de condiciones del préstamo (LoanConditionsDraft).
 *
 * Concentra el análisis de cadenas, normalización numérica, conversión automática
 * de porcentajes de entrada a fracciones decimales del dominio, seguimiento de
 * modificaciones (dirty tracking) en tiempo constante O(1) y verificación rigurosa
 * de invariantes financieras detrás de una interfaz cohesiva y compacta.
 *
 * Provee leverage a los adaptadores de interfaz de usuario y garantiza localidad
 * para el mantenimiento de las reglas de negocio del préstamo.
 */
class LoanConditionsDraft private constructor(
    private val baseline: Scenario,
    private val baselineState: LoanDraftState,
    val state: LoanDraftState,
    val errors: Map<LoanField, String>
) {

    /**
     * Propiedades de lectura directa que exponen el estado del borrador en la interfaz.
     */
    val name: String get() = state.name
    val currency: Currency get() = state.currency
    val paymentOrigin: PaymentOrigin get() = state.paymentOrigin
    val principalStr: String get() = state.principalStr
    val startDate: String get() = state.startDate
    val termMonthsStr: String get() = state.termMonthsStr
    val rateKind: RateKind get() = state.rateKind
    val ratePercentageStr: String get() = state.ratePercentageStr
    val nominalFrequencyStr: String get() = state.nominalFrequencyStr
    val paymentStr: String get() = state.paymentStr

    /**
     * Indicador O(1) de si el borrador ha sufrido modificaciones respecto a la línea base.
     */
    val isDirty: Boolean
        get() = state != baselineState

    /**
     * Indica si el borrador satisface todas las invariantes requeridas.
     */
    val isValid: Boolean
        get() = errors.isEmpty()

    /**
     * Primer mensaje de error encontrado, si existe alguno.
     */
    val firstErrorMessage: String?
        get() = errors.values.firstOrNull()

    /**
     * Actualiza el capital solicitado en el préstamo.
     */
    fun updatePrincipal(raw: String): LoanConditionsDraft =
        transition(state.copy(principalStr = raw))

    /**
     * Actualiza el plazo del préstamo en meses.
     */
    fun updateTermMonths(raw: String): LoanConditionsDraft =
        transition(state.copy(termMonthsStr = raw))

    /**
     * Actualiza la tasa y su porcentaje numérico (ej. "19.5" para 19.5%).
     */
    fun updateRate(kind: RateKind, percentageStr: String): LoanConditionsDraft =
        transition(state.copy(rateKind = kind, ratePercentageStr = percentageStr))

    /**
     * Actualiza la tasa, su porcentaje y la frecuencia de capitalización para tasas nominales.
     */
    fun updateRate(kind: RateKind, percentageStr: String, nominalFrequencyStr: String): LoanConditionsDraft =
        transition(
            state.copy(
                rateKind = kind,
                ratePercentageStr = percentageStr,
                nominalFrequencyStr = nominalFrequencyStr
            )
        )

    /**
     * Actualiza únicamente la modalidad de la tasa.
     */
    fun updateRateKind(kind: RateKind): LoanConditionsDraft =
        transition(state.copy(rateKind = kind))

    /**
     * Actualiza únicamente el porcentaje de la tasa.
     */
    fun updateRatePercentage(percentageStr: String): LoanConditionsDraft =
        transition(state.copy(ratePercentageStr = percentageStr))

    /**
     * Actualiza la frecuencia de capitalización anual para tasas nominales.
     */
    fun updateNominalFrequency(frequencyStr: String): LoanConditionsDraft =
        transition(state.copy(nominalFrequencyStr = frequencyStr))

    /**
     * Actualiza el nombre descriptivo del escenario de préstamo.
     */
    fun updateName(raw: String): LoanConditionsDraft =
        transition(state.copy(name = raw))

    /**
     * Actualiza la moneda del préstamo.
     */
    fun updateCurrency(currency: Currency): LoanConditionsDraft =
        transition(state.copy(currency = currency))

    /**
     * Actualiza el origen del saldo base (original o saldo actual).
     */
    fun updatePaymentOrigin(origin: PaymentOrigin): LoanConditionsDraft =
        transition(state.copy(paymentOrigin = origin))

    /**
     * Actualiza la fecha de inicio / primer pago en formato AAAA-MM.
     */
    fun updateStartDate(raw: String): LoanConditionsDraft =
        transition(state.copy(startDate = raw))

    /**
     * Actualiza la cuota pactada opcional.
     */
    fun updatePayment(raw: String): LoanConditionsDraft =
        transition(state.copy(paymentStr = raw))

    /**
     * Restablece el borrador a la línea base actual en tiempo O(1).
     */
    fun reset(): LoanConditionsDraft = from(baseline)

    /**
     * Restablece el borrador contra un nuevo escenario baseline.
     */
    fun reset(scenario: Scenario): LoanConditionsDraft = from(scenario)

    /**
     * Confirma el borrador devolviendo un nuevo [Scenario] validado, o null si existen errores.
     */
    fun commit(): Scenario? = validateAndCommit().getOrNull()

    /**
     * Valida exhaustivamente y confirma el borrador produciendo un [Result] con el [Scenario] resultante.
     */
    fun validateAndCommit(): Result<Scenario> = runCatching {
        check(isValid) { firstErrorMessage ?: "Condiciones del préstamo inválidas." }
        val scenario = buildScenario()
        scenario.validate()
        scenario
    }

    private fun transition(newState: LoanDraftState): LoanConditionsDraft {
        if (newState == state) return this
        val newErrors = validate(newState)
        return LoanConditionsDraft(
            baseline = baseline,
            baselineState = baselineState,
            state = newState,
            errors = newErrors
        )
    }

    private fun buildScenario(): Scenario {
        val ratePercentNormalized = normalizeNumber(state.ratePercentageStr)
        val rateDec = Decimal.parse(ratePercentNormalized) / Decimal.parse("100")
        val domainRateValue = rateDec.toString()

        val domainRate: Rate = when (state.rateKind) {
            RateKind.Monthly -> Rate.Monthly(domainRateValue)
            RateKind.Annual -> Rate.Annual(domainRateValue)
            RateKind.Nominal -> Rate.Nominal(
                value = domainRateValue,
                frequency = state.nominalFrequencyStr.trim().toIntOrNull() ?: 12
            )
        }

        return baseline.copy(
            name = state.name.trim(),
            currency = state.currency,
            source = state.paymentOrigin.wire,
            principal = state.principalStr.trim(),
            startDate = state.startDate.trim(),
            termMonths = state.termMonthsStr.trim().toInt(),
            rate = domainRate,
            payment = state.paymentStr.trim().ifBlank { null }
        )
    }

    companion object {
        private fun normalizeNumber(raw: String): String =
            raw.trim().removeSuffix("%").trim().replace(',', '.')

        /**
         * Inicializa un borrador a partir de un escenario base de dominio.
         */
        fun from(scenario: Scenario): LoanConditionsDraft {
            val ratePercentStr = try {
                (Decimal.parse(scenario.rate.value) * Decimal.parse("100")).toString()
            } catch (_: Exception) {
                scenario.rate.value
            }

            val nominalFreqStr = if (scenario.rate is Rate.Nominal) {
                scenario.rate.frequency.toString()
            } else {
                "12"
            }

            val initialState = LoanDraftState(
                name = scenario.name,
                currency = scenario.currency,
                paymentOrigin = PaymentOrigin.fromWire(scenario.source),
                principalStr = scenario.principal,
                startDate = scenario.startDate,
                termMonthsStr = scenario.termMonths.toString(),
                rateKind = RateKind.fromDomain(scenario.rate),
                ratePercentageStr = ratePercentStr,
                nominalFrequencyStr = nominalFreqStr,
                paymentStr = scenario.payment ?: ""
            )

            val initialErrors = validate(initialState)

            return LoanConditionsDraft(
                baseline = scenario,
                baselineState = initialState,
                state = initialState,
                errors = initialErrors
            )
        }

        private fun validate(state: LoanDraftState): Map<LoanField, String> {
            val errors = mutableMapOf<LoanField, String>()

            // 1. Nombre
            val trimmedName = state.name.trim()
            if (trimmedName.isEmpty() || trimmedName.length > 120) {
                errors[LoanField.Name] = "El nombre debe tener entre 1 y 120 caracteres."
            }

            // 2. Capital
            val rawPrincipal = normalizeNumber(state.principalStr)
            if (rawPrincipal.isEmpty()) {
                errors[LoanField.Principal] = "El capital no puede estar vacío."
            } else {
                try {
                    val p = Decimal.parse(rawPrincipal)
                    if (!p.isPositive) {
                        errors[LoanField.Principal] = "El capital debe ser mayor que cero."
                    } else if (p > Decimal.parse("1000000000000000")) {
                        errors[LoanField.Principal] = "El máximo admitido es 1.000.000.000.000.000 en la moneda seleccionada."
                    }
                } catch (_: Exception) {
                    errors[LoanField.Principal] = "El capital debe ser un número válido."
                }
            }

            // 3. Plazo en meses
            val rawTerm = state.termMonthsStr.trim()
            val termInt = rawTerm.toIntOrNull()
            if (termInt == null) {
                errors[LoanField.TermMonths] = "Indica un plazo en meses válido."
            } else if (termInt !in 1..MAX_MONTHS) {
                errors[LoanField.TermMonths] = "El plazo debe estar entre 1 y $MAX_MONTHS meses."
            }

            // 4. Tasa de interés
            val rawRate = normalizeNumber(state.ratePercentageStr)
            if (rawRate.isEmpty()) {
                errors[LoanField.RatePercentage] = "Indica una tasa de interés válida."
            } else {
                try {
                    val r = Decimal.parse(rawRate)
                    if (r.isNegative) {
                        errors[LoanField.RatePercentage] = "La tasa de interés no puede ser negativa."
                    }
                } catch (_: Exception) {
                    errors[LoanField.RatePercentage] = "Indica una tasa de interés válida."
                }
            }

            // 5. Frecuencia nominal
            if (state.rateKind == RateKind.Nominal) {
                val rawFreq = state.nominalFrequencyStr.trim()
                val freqInt = rawFreq.toIntOrNull()
                if (freqInt == null || freqInt <= 0) {
                    errors[LoanField.NominalFrequency] = "Indica una frecuencia de capitalización válida."
                }
            }

            // 6. Fecha de inicio
            val rawStartDate = state.startDate.trim()
            if (!Regex("""^\d{4}-(0[1-9]|1[0-2])$""").matches(rawStartDate)) {
                errors[LoanField.StartDate] = "Usa año-mes válido."
            }

            // 7. Cuota pactada opcional
            val rawPayment = normalizeNumber(state.paymentStr)
            if (rawPayment.isNotEmpty()) {
                try {
                    val pay = Decimal.parse(rawPayment)
                    if (!pay.isPositive) {
                        errors[LoanField.Payment] = "La cuota debe ser mayor que cero."
                    }
                } catch (_: Exception) {
                    errors[LoanField.Payment] = "La cuota debe ser un número válido."
                }
            }

            return errors
        }
    }
}
