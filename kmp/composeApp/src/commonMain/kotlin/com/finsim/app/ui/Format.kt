package com.finsim.app.ui

import com.finsim.domain.Currency

fun formatMoney(amount: String, currency: Currency): String {
    val clean = amount.trim()
    val isNegative = clean.startsWith("-")
    val absStr = if (isNegative) clean.substring(1) else clean
    val parts = absStr.split(".")
    val intPart = parts[0]
    val decPart = if (parts.size > 1) parts[1].padEnd(2, '0').take(2) else "00"

    // Formatter using Spanish Colombian style: dot for thousands, comma for decimals
    val withThousands = intPart.reversed().chunked(3).joinToString(".").reversed()
    val formattedNumber = "$withThousands,$decPart"
    val prefix = if (isNegative) "-" else ""
    return "$currency $prefix$formattedNumber"
}

fun formatMonth(dateStr: String): String {
    val parts = dateStr.split("-")
    if (parts.size < 2) return dateStr
    val year = parts[0]
    val monthInt = parts[1].toIntOrNull() ?: return dateStr
    val monthNames = listOf(
        "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic"
    )
    val name = if (monthInt in 1..12) monthNames[monthInt - 1] else parts[1]
    return "$name de $year"
}
