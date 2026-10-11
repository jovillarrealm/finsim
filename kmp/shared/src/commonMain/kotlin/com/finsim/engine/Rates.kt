package com.finsim.engine

import com.finsim.core.Decimal
import com.finsim.domain.Rate

fun toMonthlyRate(rate: Rate): Decimal {
    val value = Decimal.parse(rate.value)
    return when (rate) {
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
}

fun convertRate(rate: Rate, target: String, frequency: Int? = null): String {
    val monthly = toMonthlyRate(rate)
    val one = Decimal.ONE
    return when (target) {
        "monthly" -> monthly.toString()
        "annual" -> {
            ((monthly + one).pow(12) - one).toString()
        }
        "nominal" -> {
            requireNotNull(frequency) { "Frecuencia requerida para tasa nominal" }
            val freq = Decimal.fromInt(frequency)
            val exp = Decimal.fromInt(12) / freq
            val base = (monthly + one).pow(exp) - one
            (base * freq).toString()
        }
        else -> error("Tipo de tasa desconocido: $target")
    }
}
