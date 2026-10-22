package com.finsim.domain

import com.finsim.core.Decimal
import com.finsim.domain.types.Money
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
 * Normaliza una cadena numérica removiendo sufijos porcentuales y sustituyendo comas por puntos.
 */
internal fun normalizeNumber(raw: String): String =
    raw.trim().removeSuffix("%").trim().replace(',', '.')

/**
 * Borrador inmutable para un ítem de seguro del préstamo.
 */
data class InsuranceDraftItem(
    val id: String,
    val name: String,
    val kind: String = "fixed", // "fixed" or "percentage"
    val valueStr: String = "0.00",
    val endsAtPayoff: Boolean = true,
    val endMonthStr: String = ""
) {
    fun toDomainInsurance(): Insurance {
        val valNorm = normalizeNumber(valueStr)
        val domainVal = if (kind == "percentage") {
            if (valNorm.isNotBlank()) {
                (Decimal.parse(valNorm) / Decimal.parse("100")).toString()
            } else "0"
        } else {
            if (valNorm.isNotBlank()) valNorm else "0.00"
        }
        val endM = if (!endsAtPayoff) endMonthStr.trim().toIntOrNull() else null
        return Insurance(
            id = id.trim(),
            name = name.trim(),
            kind = kind,
            value = domainVal,
            endsAtPayoff = endsAtPayoff,
            endMonth = endM
        )
    }

    companion object {
        fun fromDomain(domain: Insurance): InsuranceDraftItem {
            val valStr = if (domain.kind == "percentage") {
                try {
                    (Decimal.parse(domain.value) * Decimal.parse("100")).toString()
                } catch (_: Exception) {
                    domain.value
                }
            } else {
                domain.value
            }
            return InsuranceDraftItem(
                id = domain.id,
                name = domain.name,
                kind = domain.kind,
                valueStr = valStr,
                endsAtPayoff = domain.endsAtPayoff,
                endMonthStr = domain.endMonth?.toString() ?: ""
            )
        }
    }
}

/**
 * Borrador inmutable para el régimen de prepago y precancelación argentino.
 */
data class ArgentinaProfileDraft(
    val enabled: Boolean = false,
    val originationDate: String = "",
    val originalTermMonthsStr: String = "",
    val contractualProfile: String = "",
    val commissionPercentStr: String = "3",
    val vatPercentStr: String = "21"
) {
    fun toDomainProfile(): ArgentinaProfile? {
        if (!enabled) return null
        val commNorm = normalizeNumber(commissionPercentStr)
        val commRate = if (commNorm.isNotBlank()) {
            (Decimal.parse(commNorm) / Decimal.parse("100")).toString()
        } else "0.03"

        val vatNorm = normalizeNumber(vatPercentStr)
        val vatRate = if (vatNorm.isNotBlank()) {
            (Decimal.parse(vatNorm) / Decimal.parse("100")).toString()
        } else "0.21"

        val origTerm = originalTermMonthsStr.trim().toIntOrNull()
        val origDate = originationDate.trim().ifBlank { null }
        val contractProfile = contractualProfile.trim().ifBlank { null }

        return ArgentinaProfile(
            originationDate = origDate,
            originalTermMonths = origTerm,
            contractualProfile = contractProfile,
            commissionRate = commRate,
            vatRate = vatRate
        )
    }

    companion object {
        fun fromDomain(profile: ArgentinaProfile?): ArgentinaProfileDraft {
            if (profile == null) {
                return ArgentinaProfileDraft(
                    enabled = false,
                    originationDate = "",
                    originalTermMonthsStr = "",
                    contractualProfile = "",
                    commissionPercentStr = "3",
                    vatPercentStr = "21"
                )
            }
            val commPercent = try {
                (Decimal.parse(profile.commissionRate) * Decimal.parse("100")).toString()
            } catch (_: Exception) {
                profile.commissionRate
            }
            val vatPercent = try {
                (Decimal.parse(profile.vatRate) * Decimal.parse("100")).toString()
            } catch (_: Exception) {
                profile.vatRate
            }
            return ArgentinaProfileDraft(
                enabled = true,
                originationDate = profile.originationDate ?: "",
                originalTermMonthsStr = profile.originalTermMonths?.toString() ?: "",
                contractualProfile = profile.contractualProfile ?: "",
                commissionPercentStr = commPercent,
                vatPercentStr = vatPercent
            )
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
    Insurance,
    ArgentinaProfile,
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
    val paymentStr: String = "",
    val insurances: List<InsuranceDraftItem> = emptyList(),
    val argentinaProfile: ArgentinaProfileDraft = ArgentinaProfileDraft()
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
    val insurances: List<InsuranceDraftItem> get() = state.insurances
    val argentinaProfile: ArgentinaProfileDraft get() = state.argentinaProfile

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
        transition(
            state.copy(
                currency = currency,
                argentinaProfile = if (currency == Currency.ARS) {
                    state.argentinaProfile
                } else {
                    state.argentinaProfile.copy(enabled = false)
                }
            )
        )

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
     * Agrega un nuevo seguro al borrador del préstamo.
     */
    fun addInsurance(item: InsuranceDraftItem): LoanConditionsDraft =
        transition(state.copy(insurances = state.insurances + item))

    /**
     * Agrega un seguro con valores por defecto o parametrizados.
     */
    fun addInsurance(
        name: String = "Nuevo seguro",
        kind: String = "fixed",
        valueStr: String = "0.00",
        endsAtPayoff: Boolean = true,
        endMonthStr: String = ""
    ): LoanConditionsDraft {
        val existingIds = state.insurances.map { it.id }.toSet()
        var idx = state.insurances.size + 1
        while ("ins-$idx" in existingIds) {
            idx++
        }
        val item = InsuranceDraftItem(
            id = "ins-$idx",
            name = name,
            kind = kind,
            valueStr = valueStr,
            endsAtPayoff = endsAtPayoff,
            endMonthStr = endMonthStr
        )
        return addInsurance(item)
    }

    /**
     * Actualiza un ítem de seguro existente.
     */
    fun updateInsurance(item: InsuranceDraftItem): LoanConditionsDraft =
        transition(state.copy(insurances = state.insurances.map { if (it.id == item.id) item else it }))

    /**
     * Actualiza un ítem de seguro aplicando una función de transformación sobre el existente.
     */
    fun updateInsurance(id: String, transform: (InsuranceDraftItem) -> InsuranceDraftItem): LoanConditionsDraft =
        transition(state.copy(insurances = state.insurances.map { if (it.id == id) transform(it) else it }))

    /**
     * Elimina un seguro por su identificador.
     */
    fun removeInsurance(id: String): LoanConditionsDraft =
        transition(state.copy(insurances = state.insurances.filterNot { it.id == id }))

    /**
     * Habilita o deshabilita el perfil de precancelación argentino.
     */
    fun toggleArgentinaProfile(enabled: Boolean): LoanConditionsDraft =
        transition(state.copy(argentinaProfile = state.argentinaProfile.copy(enabled = enabled)))

    /**
     * Actualiza el perfil de precancelación argentino.
     */
    fun updateArgentinaProfile(profile: ArgentinaProfileDraft): LoanConditionsDraft =
        transition(state.copy(argentinaProfile = profile))

    /**
     * Actualiza campos individuales del perfil argentino de precancelación.
     */
    fun updateArgentinaProfile(
        enabled: Boolean = state.argentinaProfile.enabled,
        originationDate: String = state.argentinaProfile.originationDate,
        originalTermMonthsStr: String = state.argentinaProfile.originalTermMonthsStr,
        contractualProfile: String = state.argentinaProfile.contractualProfile,
        commissionPercentStr: String = state.argentinaProfile.commissionPercentStr,
        vatPercentStr: String = state.argentinaProfile.vatPercentStr
    ): LoanConditionsDraft =
        transition(
            state.copy(
                argentinaProfile = state.argentinaProfile.copy(
                    enabled = enabled,
                    originationDate = originationDate,
                    originalTermMonthsStr = originalTermMonthsStr,
                    contractualProfile = contractualProfile,
                    commissionPercentStr = commissionPercentStr,
                    vatPercentStr = vatPercentStr
                )
            )
        )

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

        val domainInsurances = state.insurances.map { it.toDomainInsurance() }
        val domainArgProfile = state.argentinaProfile.toDomainProfile()

        return baseline.copy(
            name = state.name.trim(),
            currency = state.currency,
            source = state.paymentOrigin.wire,
            principal = state.principalStr.trim(),
            startDate = state.startDate.trim(),
            termMonths = state.termMonthsStr.trim().toInt(),
            rate = domainRate,
            payment = state.paymentStr.trim().ifBlank { null },
            insurance = domainInsurances,
            argentinaProfile = domainArgProfile
        )
    }

    companion object {
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
                paymentStr = scenario.payment ?: "",
                insurances = scenario.insurance.map { InsuranceDraftItem.fromDomain(it) },
                argentinaProfile = ArgentinaProfileDraft.fromDomain(scenario.argentinaProfile)
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

            // 8. Seguros
            val insuranceIds = mutableSetOf<String>()
            for (ins in state.insurances) {
                if (ins.id.isBlank()) {
                    errors[LoanField.Insurance] = "El identificador del seguro no puede estar vacío."
                    break
                }
                if (!insuranceIds.add(ins.id.trim())) {
                    errors[LoanField.Insurance] = "Los identificadores de seguro no deben repetirse."
                    break
                }
                if (ins.name.isBlank()) {
                    errors[LoanField.Insurance] = "El nombre del seguro no puede estar vacío."
                    break
                }
                if (ins.kind != "fixed" && ins.kind != "percentage") {
                    errors[LoanField.Insurance] = "El tipo de seguro debe ser 'fixed' o 'percentage'."
                    break
                }
                val rawVal = normalizeNumber(ins.valueStr)
                if (rawVal.isBlank()) {
                    errors[LoanField.Insurance] = "El valor del seguro no puede estar vacío."
                    break
                }
                try {
                    val decVal = Decimal.parse(rawVal)
                    if (decVal.isNegative) {
                        errors[LoanField.Insurance] = "El valor del seguro no puede ser negativo."
                        break
                    }
                } catch (_: Exception) {
                    errors[LoanField.Insurance] = "El valor del seguro debe ser un número válido."
                    break
                }
                if (ins.kind == "fixed") {
                    try {
                        Money.parse(rawVal, state.currency)
                    } catch (error: Exception) {
                        errors[LoanField.Insurance] = error.message ?: "El valor del seguro debe ser un importe monetario válido."
                        break
                    }
                }
                if (!ins.endsAtPayoff) {
                    val endM = ins.endMonthStr.trim().toIntOrNull()
                    if (endM == null || endM !in 1..MAX_MONTHS) {
                        errors[LoanField.Insurance] = "Indica el último mes del seguro (entre 1 y $MAX_MONTHS)."
                        break
                    }
                }
            }

            // 9. Régimen argentino / prepago
            if (state.argentinaProfile.enabled && state.currency != Currency.ARS) {
                errors[LoanField.ArgentinaProfile] = "El perfil argentino solo se admite para operaciones en ARS."
            }
            if (state.argentinaProfile.enabled) {
                val rawOrigDate = state.argentinaProfile.originationDate.trim()
                if (rawOrigDate.isNotBlank() && !Regex("""^\d{4}-(0[1-9]|1[0-2])$""").matches(rawOrigDate)) {
                    errors[LoanField.ArgentinaProfile] = "Usa año-mes válido para la fecha de originación (AAAA-MM)."
                }
                val rawOrigTerm = state.argentinaProfile.originalTermMonthsStr.trim()
                if (rawOrigTerm.isNotBlank()) {
                    val termInt = rawOrigTerm.toIntOrNull()
                    if (termInt == null || termInt !in 1..MAX_MONTHS) {
                        errors[LoanField.ArgentinaProfile] = "El plazo original debe estar entre 1 y $MAX_MONTHS meses."
                    }
                }
                val commNorm = normalizeNumber(state.argentinaProfile.commissionPercentStr)
                if (commNorm.isNotBlank()) {
                    try {
                        val c = Decimal.parse(commNorm)
                        if (c.isNegative) {
                            errors[LoanField.ArgentinaProfile] = "La comisión de precancelación no puede ser negativa."
                        }
                    } catch (_: Exception) {
                        errors[LoanField.ArgentinaProfile] = "La comisión de precancelación debe ser un número válido."
                    }
                }
                val vatNorm = normalizeNumber(state.argentinaProfile.vatPercentStr)
                if (vatNorm.isNotBlank()) {
                    try {
                        val v = Decimal.parse(vatNorm)
                        if (v.isNegative) {
                            errors[LoanField.ArgentinaProfile] = "La alícuota de IVA no puede ser negativa."
                        }
                    } catch (_: Exception) {
                        errors[LoanField.ArgentinaProfile] = "La alícuota de IVA debe ser un número válido."
                    }
                }
            }

            return errors
        }
    }
}
