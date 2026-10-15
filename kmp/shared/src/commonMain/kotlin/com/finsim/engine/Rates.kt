package com.finsim.engine

import com.finsim.core.Decimal
import com.finsim.core.FinsimLog
import com.finsim.domain.Rate

/**
 * Type-safe target specification for financial rate conversions.
 *
 * Eliminates primitive obsession (raw string literals) and prevents
 * runtime errors such as missing frequency for nominal rates.
 */
sealed interface TargetRate {
    data object Monthly : TargetRate
    data object Annual : TargetRate
    data class Nominal(val frequency: Int) : TargetRate {
        init {
            require(frequency > 0) { "La frecuencia nominal debe ser mayor a 0." }
        }
    }
}

fun toMonthlyRate(rate: Rate): Decimal {
    val value = Decimal.parse(rate.value)
    val monthly = when (rate) {
        is Rate.Monthly -> value
        is Rate.Annual -> {
            val one = Decimal.ONE
            val twelfth = one / Decimal.fromInt(12)
            (value + one).pow(twelfth) - one
        }
        is Rate.Nominal -> {
            val one = Decimal.ONE
            val freq = Decimal.fromInt(rate.frequency)
            val exp = freq / Decimal.fromInt(12)
            (value / freq + one).pow(exp) - one
        }
    }
    FinsimLog.rates.d { "toMonthlyRate: input=${rate::class.simpleName}(${rate.value}) -> monthly=$monthly" }
    return monthly
}

/**
 * Deep typed interface for rate conversion across compounding periods.
 */
fun convertRate(rate: Rate, target: TargetRate): Rate {
    val monthly = toMonthlyRate(rate)
    val one = Decimal.ONE
    val resultRate = when (target) {
        is TargetRate.Monthly -> Rate.Monthly(monthly.toString())
        is TargetRate.Annual -> {
            val annual = (monthly + one).pow(12) - one
            Rate.Annual(annual.toString())
        }
        is TargetRate.Nominal -> {
            val freq = Decimal.fromInt(target.frequency)
            val exp = Decimal.fromInt(12) / freq
            val base = (monthly + one).pow(exp) - one
            Rate.Nominal((base * freq).toString(), frequency = target.frequency)
        }
    }
    FinsimLog.rates.i { "convertRate: ${rate::class.simpleName} -> target=$target => result=${resultRate.value}" }
    return resultRate
}

/**
 * Adapter at the outer seam maintaining backward compatibility with untyped string callers.
 */
fun convertRate(rate: Rate, target: String, frequency: Int? = null): String {
    val targetRate = when (target.trim().lowercase()) {
        "monthly" -> TargetRate.Monthly
        "annual" -> TargetRate.Annual
        "nominal" -> {
            requireNotNull(frequency) { "Frecuencia requerida para tasa nominal" }
            TargetRate.Nominal(frequency)
        }
        else -> error("Tipo de tasa desconocido: $target")
    }
    return convertRate(rate, targetRate).value
}
