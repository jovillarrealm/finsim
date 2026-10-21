package com.finsim.ingestion

import com.finsim.core.Decimal
import com.finsim.domain.Currency

/**
 * Parses raw plain text from a bank document page under a specific [RegulatoryFramework]
 * into structured financial candidates ([ExtractedDocumentData]).
 */
object DocumentTextParser {

    private val KNOWN_BANKS = listOf(
        "Bancolombia", "Davivienda", "Banco de Bogotá", "BBVA Colombia", "BBVA México", "BBVA Argentina", "BBVA",
        "Banco de Occidente", "Scotiabank Colpatria", "Colpatria", "Banco AV Villas",
        "Banco Popular", "Banco Agrario", "Banco Caja Social", "Nu Colombia", "Nu",
        "Banorte", "Santander México", "Santander Argentina", "Banco Santander Chile", "Santander",
        "Citibanamex", "HSBC México", "HSBC", "Banco Azteca", "Inbursa",
        "Banco Galicia", "Banco Nación", "Banco Provincia", "Banco Macro", "ICBC Argentina", "Banco Ciudad",
        "Banco de Chile", "Banco Estado", "BCI", "Scotiabank Chile", "Itaú Chile", "Itaú"
    )

    private val SPANISH_MONTHS = mapOf(
        "enero" to "01", "febrero" to "02", "marzo" to "03", "abril" to "04",
        "mayo" to "05", "junio" to "06", "julio" to "07", "agosto" to "08",
        "septiembre" to "09", "setiembre" to "09", "octubre" to "10",
        "noviembre" to "11", "diciembre" to "12"
    )

    fun parse(pageText: String, framework: RegulatoryFramework, pageIndex: Int = 1): ExtractedDocumentData {
        val lines = pageText.lines().map { it.trim() }.filter { it.isNotBlank() }

        val bankCandidate = extractBankName(pageText)
        val balances = extractBalances(pageText)
        val rate = extractRate(pageText, framework)
        val terms = extractTerms(pageText)
        val payment = extractPayments(pageText)
        val statementCutDate = extractCutDate(pageText)
        val cuotaMonth = extractStatementMonth(pageText)
        val statementMonth = cuotaMonth
        val insurances = extractInsurances(lines)
        val principalPaid = extractPrincipalPaid(pageText)
        val interestCharged = extractInterestCharged(pageText)
        val totalInsurance = extractTotalInsurance(pageText, insurances)
        val currencies = detectCurrencies(pageText)

        var confidence = 0.0f
        if (balances.closing != null || balances.original != null) confidence += 0.35f
        if (rate != null) {
            confidence += 0.35f
        } else if (interestCharged != null && principalPaid != null) {
            confidence += 0.35f
        } else if (interestCharged != null || principalPaid != null) {
            confidence += 0.20f
        }
        if (terms.value != null) confidence += 0.15f
        if (payment.value != null) confidence += 0.08f
        if (insurances.isNotEmpty() || totalInsurance != null) confidence += 0.07f

        return ExtractedDocumentData(
            sourceEngine = ExtractionEngine.TIER_1_DIGITAL_TEXT,
            analyzedPage = pageIndex,
            bankNameCandidate = bankCandidate,
            statementMonth = statementMonth,
            statementCutDate = statementCutDate,
            closingBalance = balances.closing,
            originalPrincipal = balances.original,
            termMonths = terms.value,
            rate = rate,
            monthlyPayment = payment.value,
            principalPaid = principalPaid,
            interestCharged = interestCharged,
            totalInsurance = totalInsurance,
            insurances = insurances,
            rawExtractedText = pageText,
            confidence = confidence.coerceIn(0.0f, 1.0f),
            documentKind = classifyDocument(pageText),
            detectedCurrencies = currencies.first,
            hasUnsupportedCurrency = currencies.second,
            capitalBalance = balances.capital,
            compoundBalance = balances.compound,
            balanceMeaning = balances.meaning,
            originalTermMonths = terms.original,
            remainingTermMonths = terms.remaining,
            basePayment = payment.base,
            totalPayment = payment.total,
            paymentMeaning = payment.meaning
        )
    }

    private data class BalanceCandidates(
        val closing: String?,
        val original: String?,
        val capital: String?,
        val compound: String?,
        val meaning: BalanceMeaning
    )

    private data class TermCandidates(
        val value: Int?,
        val original: Int?,
        val remaining: Int?,
        val meaning: TermMeaning
    )

    private data class PaymentCandidates(
        val value: String?,
        val base: String?,
        val total: String?,
        val meaning: PaymentMeaning
    )

    private fun extractBankName(text: String): String? {
        for (bank in KNOWN_BANKS) {
            if (Regex("\\b${Regex.escape(bank)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                return bank
            }
        }
        val genericMatch = Regex("""\b(Banco|Financiera|Cooperativa)\s+([A-ZÁÉÍÓÚ][a-záéíóúñ]+(?:\s+[A-ZÁÉÍÓÚ][a-záéíóúñ]+)?)""").find(text)
        return genericMatch?.value
    }

    private fun classifyDocument(text: String): ImportDocumentKind {
        val accountSummary = Regex(
            """(?i)\b(?:resumen\s+de\s+cuenta|estado\s+de\s+cuenta|movimientos\s+de\s+cuenta)\b"""
        ).containsMatchIn(text)
        val explicitLoanTitle = Regex(
            """(?i)\b(?:extracto|estado\s+de\s+cuenta|resumen)\s+(?:mensual\s+)?(?:de\s+)?(?:pr[eé]stamo|cr[eé]dito|obligaci[oó]n)\b"""
        ).containsMatchIn(text)
        val loanStatement = Regex(
            """(?i)\b(?:pr[eé]stamo|cr[eé]dito|obligaci[oó]n|amortizaci[oó]n|abono\s+a\s+capital|pago\s+(?:a|de)\s+capital|inter[eé]s\s+corriente)\b"""
        ).containsMatchIn(text)
        return when {
            explicitLoanTitle -> ImportDocumentKind.LOAN_STATEMENT
            accountSummary -> ImportDocumentKind.ACCOUNT_SUMMARY
            loanStatement -> ImportDocumentKind.LOAN_STATEMENT
            else -> ImportDocumentKind.UNKNOWN
        }
    }

    private fun detectCurrencies(text: String): Pair<Set<Currency>, Boolean> {
        val currencies = mutableSetOf<Currency>()
        if (Regex("""(?i)\bCOP\b|pesos?\s+colombianos?""").containsMatchIn(text)) currencies += Currency.COP
        if (Regex("""(?i)\bARS\b|AR[$]|pesos?\s+argentinos?""").containsMatchIn(text)) currencies += Currency.ARS
        if (Regex("""(?i)\bUSD\b|US[$]|U[$]S|d[oó]lares?""").containsMatchIn(text)) currencies += Currency.USD
        if (Regex("""(?i)\bEUR\b|€|euros?""").containsMatchIn(text)) currencies += Currency.EUR
        val unsupported = Regex("""(?i)\b(?:MXN|CLP|BRL|GBP)\b|pesos?\s+mexicanos?""").containsMatchIn(text)
        return currencies to unsupported
    }

    private fun extractBalances(text: String): BalanceCandidates {
        val amount = "([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{1,2})?|[0-9]+(?:[.,][0-9]{1,2})?)"
        fun extract(pattern: String): String? = Regex(pattern).find(text)?.groupValues?.get(1)?.let(::cleanCurrencyNumber)

        val original = extract(
            """(?i)(?:capital\s+(?:inicial|original)|monto\s+(?:desembolsado|otorgado|del\s+pr[eé]stamo)|valor\s+(?:desembolsado|del\s+cr[eé]dito|inicial))[:\s]*\$?\s*$amount"""
        )
        val capital = extract(
            """(?i)(?:saldo\s+(?:de\s+)?capital(?:\s+(?:actual|vigente|a\s+la\s+fecha))?|saldo\s+insoluto|saldo\s+a\s+capital|capital\s+(?:vigente|pendiente))[:\s]*\$?\s*$amount"""
        )
        val compound = extract(
            """(?i)(?:saldo\s+compuesto|saldo\s+total|deuda\s+total|total\s+adeudado|total\s+a\s+pagar|saldo\s+deudor)[:\s]*\$?\s*$amount"""
        )
        val genericClosing = extract(
            """(?i)(?:saldo\s+final|nuevo\s+saldo|saldo\s+actual|saldo\s+pendiente)[:\s]*\$?\s*$amount"""
        )
        val fallback = if (capital == null && compound == null && original == null) {
            extract("""(?i)\b(?:saldo|capital)[:\s]*\$?\s*$amount""")
        } else null
        val meaning = when {
            capital != null -> BalanceMeaning.PRINCIPAL
            compound != null -> BalanceMeaning.COMPOUND_DEBT
            else -> BalanceMeaning.UNKNOWN
        }
        return BalanceCandidates(capital ?: compound ?: genericClosing ?: fallback, original, capital, compound, meaning)
    }

    private fun extractRate(text: String, framework: RegulatoryFramework): ExtractedRate? {
        // Framework-specific heuristics first
        when (framework) {
            RegulatoryFramework.COLOMBIA_SFC -> {
                // Priority 1: Efectiva Anual (E.A.)
                val eaRegex = Regex("""(?i)(?:tasa\s+(?:efectiva\s+anual|e\.?a\.?)|e\.?a\.?)(?:\s*\(\s*e\.?a\.?\s*\))?[:\s]*([0-9]{1,2}(?:[.,][0-9]{1,4})?)\s*%""")
                eaRegex.find(text)?.let { m ->
                    val num = m.groupValues[1].replace(',', '.')
                    return ExtractedRate(valuePercent = num, kind = "annual")
                }
                // Priority 2: Mes Vencido / Efectiva Mensual (M.V. / E.M.)
                val emRegex = Regex("""(?i)(?:tasa\s+(?:efectiva\s+mensual|mes\s+vencido|e\.?m\.?|m\.?v\.?)|m\.?v\.?|e\.?m\.?)(?:\s*\(\s*(?:m\.?v\.?|e\.?m\.?)\s*\))?[:\s]*([0-9]{1,2}(?:[.,][0-9]{1,4})?)\s*%""")
                emRegex.find(text)?.let { m ->
                    val num = m.groupValues[1].replace(',', '.')
                    return ExtractedRate(valuePercent = num, kind = "monthly")
                }
                // Priority 3: Nominal Anual (N.A.M.V.)
                val nominalRegex = Regex("""(?i)(?:tasa\s+(?:nominal(?:\s+anual)?|n\.?a\.?m\.?v\.?)|(?:n\.?a\.?m\.?v\.?))[:\s]*([0-9]{1,2}(?:[.,][0-9]{1,4})?)\s*%""")
                nominalRegex.find(text)?.let { m ->
                    val num = m.groupValues[1].replace(',', '.')
                    return ExtractedRate(valuePercent = num, kind = "nominal")
                }
            }

            RegulatoryFramework.MEXICO_CONDUSEF -> {
                // Priority 1: Contractual Tasa Ordinaria Anual
                val ordinariaRegex = Regex("""(?i)(?:tasa(?:\s+de\s+inter[eé]s)?\s+ordinaria(?:[a-záéíóúñ\s()]*)|tasa\s+anual\s+fija)[:\s]*([0-9]{1,2}(?:[.,][0-9]{1,4})?)\s*%""")
                ordinariaRegex.find(text)?.let { m ->
                    val num = m.groupValues[1].replace(',', '.')
                    return ExtractedRate(valuePercent = num, kind = "annual")
                }
                // Priority 2: CAT (Costo Anual Total)
                val catRegex = Regex("""(?i)\bcat(?:\s+promedio)?[:\s]*([0-9]{1,2}(?:[.,][0-9]{1,4})?)\s*%""")
                catRegex.find(text)?.let { m ->
                    val num = m.groupValues[1].replace(',', '.')
                    return ExtractedRate(valuePercent = num, kind = "annual")
                }
            }

            RegulatoryFramework.ARGENTINA_BCRA -> {
                // Priority 1: TNA (Tasa Nominal Anual)
                val tnaRegex = Regex("""(?i)\btna(?:\s+nominal\s+anual)?[:\s]*([0-9]{1,3}(?:[.,][0-9]{1,4})?)\s*%""")
                tnaRegex.find(text)?.let { m ->
                    val num = m.groupValues[1].replace(',', '.')
                    return ExtractedRate(valuePercent = num, kind = "nominal")
                }
                // Priority 2: TEA (Tasa Efectiva Anual)
                val teaRegex = Regex("""(?i)\btea(?:\s+efectiva\s+anual)?[:\s]*([0-9]{1,3}(?:[.,][0-9]{1,4})?)\s*%""")
                teaRegex.find(text)?.let { m ->
                    val num = m.groupValues[1].replace(',', '.')
                    return ExtractedRate(valuePercent = num, kind = "annual")
                }
                // Priority 3: CFT
                val cftRegex = Regex("""(?i)\bcft(?:\s+tea)?[:\s]*([0-9]{1,3}(?:[.,][0-9]{1,4})?)\s*%""")
                cftRegex.find(text)?.let { m ->
                    val num = m.groupValues[1].replace(',', '.')
                    return ExtractedRate(valuePercent = num, kind = "annual")
                }
            }

            RegulatoryFramework.CHILE_CMF -> {
                // Priority 1: Tasa mensual
                val tmRegex = Regex("""(?i)(?:tasa\s+(?:de\s+inter[eé]s\s+)?mensual)[:\s]*([0-9]{1,2}(?:[.,][0-9]{1,4})?)\s*%""")
                tmRegex.find(text)?.let { m ->
                    val num = m.groupValues[1].replace(',', '.')
                    return ExtractedRate(valuePercent = num, kind = "monthly")
                }
                // Priority 2: CAE
                val caeRegex = Regex("""(?i)\bcae[:\s]*([0-9]{1,2}(?:[.,][0-9]{1,4})?)\s*%""")
                caeRegex.find(text)?.let { m ->
                    val num = m.groupValues[1].replace(',', '.')
                    return ExtractedRate(valuePercent = num, kind = "annual")
                }
            }

            RegulatoryFramework.GENERIC -> {}
        }

        // Generic fallback regex scanning for any "%" accompanied by rate markers
        val genericRegex = Regex("""(?i)(?:tasa(?:\s+de\s+inter[eé]s)?|inter[eé]s)[:\s]*([0-9]{1,3}(?:[.,][0-9]{1,4})?)\s*%""")
        genericRegex.find(text)?.let { m ->
            val num = m.groupValues[1].replace(',', '.')
            val surrounding = text.substring(
                (m.range.first - 40).coerceAtLeast(0),
                (m.range.last + 40).coerceAtMost(text.length)
            ).lowercase()

            val kind = when {
                surrounding.contains("mes") || surrounding.contains("mensual") || surrounding.contains("e.m") || surrounding.contains("m.v") -> "monthly"
                surrounding.contains("nominal") || surrounding.contains("tna") -> "nominal"
                surrounding.contains("anual") || surrounding.contains("e.a") || surrounding.contains("tea") -> "annual"
                else -> "unknown"
            }
            return ExtractedRate(
                valuePercent = num,
                kind = kind,
                nominalFrequency = null
            )
        }

        // Fallback: any standalone percentage matching e.g. "18.5 %"
        val loosePercentRegex = Regex("""\b([0-9]{1,2}(?:[.,][0-9]{1,4})?)\s*%""")
        loosePercentRegex.find(text)?.let { m ->
            val num = m.groupValues[1].replace(',', '.')
            return ExtractedRate(valuePercent = num, kind = "unknown")
        }

        return null
    }

    private fun extractTerms(text: String): TermCandidates {
        fun term(pattern: String): Int? = Regex(pattern).find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?.takeIf { it in 1..1200 }

        val remaining = term(
            """(?i)(?:plazo\s+restante|cuotas?\s+(?:restantes|pendientes)|meses\s+(?:restantes|pendientes))[:\s]*([0-9]{1,3})\b"""
        )
        val original = term(
            """(?i)plazo\s+(?:original|total|pactado)[:\s]*([0-9]{1,3})\b"""
        ) ?: term("""(?i)cuota\s+[0-9]{1,3}\s+(?:de|\/)\s+([0-9]{1,3})\b""")
        val generic = term(
            """(?i)(?:plazo(?:\s+en\s+meses)?|n[uú]mero\s+de\s+cuotas|cuotas\s+totales)[:\s]*([0-9]{1,3})\b"""
        ) ?: term("""(?i)\b([0-9]{1,3})\s*(?:meses|cuotas)\b""")
        val value = remaining ?: original ?: generic
        val meaning = when {
            remaining != null -> TermMeaning.REMAINING
            original != null -> TermMeaning.ORIGINAL
            else -> TermMeaning.UNKNOWN
        }
        return TermCandidates(value, original, remaining, meaning)
    }

    private fun extractPayments(text: String): PaymentCandidates {
        val baseRegex = Regex(
            """(?i)(?:cuota|pago)(?:\s+mensual)?\s+base[:\s]*\$?\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{1,2})?|[0-9]+(?:[.,][0-9]{1,2})?)"""
        )
        val totalRegex = Regex(
            """(?i)(?:cuota|pago)(?:\s+mensual)?\s+total[:\s]*\$?\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{1,2})?|[0-9]+(?:[.,][0-9]{1,2})?)"""
        )
        val genericRegex = Regex(
            """(?i)(?:valor\s+(?:de\s+la\s+)?cuota(?:\s+mensual)?|cuota\s+(?:mensual|corriente|a\s+pagar)|pago\s+mensual|mensualidad)[:\s]*\$?\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{1,2})?|[0-9]+(?:[.,][0-9]{1,2})?)"""
        )
        fun amount(regex: Regex): String? = regex.find(text)?.groupValues?.get(1)?.let(::cleanCurrencyNumber)

        val base = amount(baseRegex)
        val total = amount(totalRegex)
        val generic = amount(genericRegex)
        val meaning = when {
            base != null -> PaymentMeaning.BASE_PAYMENT
            total != null -> PaymentMeaning.TOTAL_PAYMENT
            else -> PaymentMeaning.UNKNOWN
        }
        return PaymentCandidates(base ?: total ?: generic, base, total, meaning)
    }

    private fun extractCutDate(text: String): String? {
        // Pattern 1: YYYY-MM-DD or YYYY-MM
        val ymdRegex = Regex("""\b(20[2-3][0-9])[-/](0[1-9]|1[0-2])(?:[-/][0-3][0-9])?\b""")
        ymdRegex.find(text)?.let { m ->
            return "${m.groupValues[1]}-${m.groupValues[2]}"
        }

        // Pattern 2: DD/MM/YYYY or DD-MM-YYYY
        val dmyRegex = Regex("""\b[0-3]?[0-9][-/](0[1-9]|1[0-2])[-/](20[2-3][0-9])\b""")
        dmyRegex.find(text)?.let { m ->
            return "${m.groupValues[2]}-${m.groupValues[1]}"
        }

        // Pattern 3: Month name + Year: "Octubre 2026" or "15 de Octubre de 2026"
        val textMonthRegex = Regex("""(?i)\b(?:[0-3]?[0-9]\s+de\s+)?([a-z]+)\s+(?:de\s+)?(20[2-3][0-9])\b""")
        textMonthRegex.find(text)?.let { m ->
            val monthWord = m.groupValues[1].lowercase()
            val monthNum = SPANISH_MONTHS[monthWord]
            val year = m.groupValues[2]
            if (monthNum != null) {
                return "$year-$monthNum"
            }
        }

        return null
    }

    private fun extractInsurances(lines: List<String>): List<ExtractedInsurance> {
        val insurances = mutableListOf<ExtractedInsurance>()
        val insuranceNamesFound = mutableSetOf<String>()

        val insuranceLineRegex = Regex("""(?i)(?:^|.*?\b)(seguro(?:\s+de\s+[a-záéíóúñ]+|\s+vida|\s+deudores|\s+desgravamen|\s+incendio|\s+terremoto|\s+desempleo|\s+cesant[ií]a)?).*?[:\s]+\$?([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{1,2})?|[0-9]+(?:[.,][0-9]{1,2})?)""")
        val insuranceNameRegex = Regex(
            """(?i)\bseguro(?:\s+de\s+[a-záéíóúñ]+|\s+vida|\s+deudores|\s+desgravamen|\s+incendio|\s+terremoto|\s+desempleo|\s+cesant[ií]a)?"""
        )

        fun meaningFor(line: String): InsuranceAmountMeaning = when {
            line.contains("valor asegurado") || line.contains("suma asegurada") -> InsuranceAmountMeaning.INSURED_VALUE
            line.contains("prima") || line.contains("cargo") || line.contains("cobro") || line.contains("mensual") ->
                InsuranceAmountMeaning.PERIODIC_CHARGE
            else -> InsuranceAmountMeaning.UNKNOWN
        }

        for (line in lines) {
            val lower = line.lowercase()
            if (lower.contains("seguro") || lower.contains("desgravamen")) {
                if (lower.contains("total seguro") && insurances.isNotEmpty()) {
                    // Skip aggregate if individual line items already parsed
                    continue
                }
                val match = insuranceLineRegex.find(line)
                val rawName = (match?.groupValues?.get(1) ?: insuranceNameRegex.find(line)?.value)
                    ?.trim()?.replaceFirstChar { it.uppercase() }
                if (rawName != null && !insuranceNamesFound.contains(rawName)) {
                    val cleanAmount = match?.groupValues?.get(2)?.let(::cleanCurrencyNumber)
                    val meaning = meaningFor(lower)
                    if (cleanAmount == null) {
                        insuranceNamesFound.add(rawName)
                        insurances.add(ExtractedInsurance(name = rawName, monthlyCost = null, amountMeaning = meaning))
                    } else {
                        try {
                            val dec = Decimal.parse(cleanAmount)
                            if (!dec.isNegative) {
                                insuranceNamesFound.add(rawName)
                                insurances.add(
                                    ExtractedInsurance(name = rawName, monthlyCost = dec.toFixed(2), amountMeaning = meaning)
                                )
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        return insurances
    }

    private fun extractStatementMonth(text: String): Int? {
        // Pattern 1: "Cuota 12 de 36" or "Mes 12 de 36" or "Cuota 12/36"
        val fractionRegex = Regex("""(?i)\b(?:cuota|mes)\s+([0-9]{1,3})\s*(?:de|\/)\s*[0-9]{1,3}\b""")
        fractionRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let {
            if (it in 1..1200) return it
        }

        // Pattern 2: "Cuota No. 12", "Cuota N° 12", "Cuota # 12", "Cuota número 12", "Mes No. 12"
        val cuotaNumberRegex = Regex("""(?i)\b(?:cuota|mes)\s+(?:n[uú]mero|no\.?|n[°º]|#)\s*([0-9]{1,3})\b""")
        cuotaNumberRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let {
            if (it in 1..1200) return it
        }

        // Pattern 3: Explicit label "Número de cuota: 12", "Nro. de cuota: 12", "Cuota actual: 12", "Mes actual: 12"
        val labelRegex = Regex("""(?i)(?:n[uú]mero\s+de\s+cuota|nro\.?\s+de\s+cuota|cuota\s+actual|mes\s+actual)[:\s]+([0-9]{1,3})\b""")
        labelRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let {
            if (it in 1..1200) return it
        }

        // Pattern 4: Standalone "Cuota 12" or "Mes 12"
        val standaloneRegex = Regex("""(?i)\b(?:cuota|mes)\s+([0-9]{1,3})\b(?!\s*(?:de\s+[a-záéíóúñ]+|mensual|fija|\$|pesos|usd))""")
        standaloneRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let {
            if (it in 1..1200) return it
        }

        return null
    }

    private fun extractPrincipalPaid(text: String): String? {
        val principalPaidRegex = Regex(
            """(?i)(?<!tabla\s+de\s+)(?<!sistema\s+de\s+)(?:abono\s+(?:a\s+)?capital|pago\s+(?:a|de)\s+capital|capital\s+pagado|capital\s+amortizado|amortizaci[oó]n(?:\s+(?:a\s+capital|de\s+capital|ordinaria|del\s+periodo))?)[:\s]*\$?\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{1,2})?|[0-9]+(?:[.,][0-9]{1,2})?)"""
        )
        return principalPaidRegex.find(text)?.groupValues?.get(1)?.let { cleanCurrencyNumber(it) }
    }

    private fun extractInterestCharged(text: String): String? {
        val interestRegex = Regex(
            """(?i)(?:inter[eé]s\s+corriente|intereses\s+corrientes|intereses?\s+del\s+periodo|intereses?\s+del\s+mes|inter[eé]s\s+mes|intereses?\s+cobrados?|inter[eé]s\s+cobrado|intereses?\s+ordinarios?|inter[eé]s\s+ordinario|intereses?\s+facturados?|inter[eé]s\s+facturado|intereses?\s+liquidados?|inter[eé]s\s+liquidado)[:\s]*\$?\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{1,2})?|[0-9]+(?:[.,][0-9]{1,2})?)(?!\s*%)"""
        )
        return interestRegex.find(text)?.groupValues?.get(1)?.let { cleanCurrencyNumber(it) }
    }

    private fun extractTotalInsurance(text: String, insurances: List<ExtractedInsurance>): String? {
        // 1. Only a clearly identified insurance total is a monthly insurance charge.
        val totalRegex = Regex(
            """(?i)(?:total\s+seguros?|total\s+cargos?\s+de\s+seguro)[:\s]*\$?\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{1,2})?|[0-9]+(?:[.,][0-9]{1,2})?)"""
        )
        totalRegex.find(text)?.groupValues?.get(1)?.let { cleanCurrencyNumber(it) }?.let {
            return it
        }

        // 2. Sum only individually identified periodic charges, including an explicit zero.
        val charges = insurances.filter {
            it.amountMeaning == InsuranceAmountMeaning.PERIODIC_CHARGE && it.monthlyCost != null
        }
        if (charges.isNotEmpty()) {
            try {
                val sum = charges.fold(Decimal.fromInt(0)) { acc, ins ->
                    acc + Decimal.parse(ins.monthlyCost!!.replace(',', '.'))
                }
                if (!sum.isNegative) return sum.toFixed(2)
            } catch (_: Exception) {}
        }

        return null
    }

    /**
     * Cleans currency numbers with multiple thousand separators and comma/dot decimals.
     */
    fun cleanCurrencyNumber(raw: String): String? {
        val trimmed = raw.replace("$", "").replace("COP", "").replace("USD", "").replace("ARS", "").replace("MXN", "").trim()
            .trimEnd('.', ',', ';', ':')
        if (trimmed.isBlank()) return null

        val result = if (trimmed.contains('.') && trimmed.contains(',')) {
            val lastDot = trimmed.lastIndexOf('.')
            val lastComma = trimmed.lastIndexOf(',')
            if (lastDot > lastComma) {
                trimmed.replace(",", "")
            } else {
                trimmed.replace(".", "").replace(',', '.')
            }
        } else if (trimmed.contains(',')) {
            val parts = trimmed.split(',')
            if (parts.size == 2 && parts[1].length in 1..2) {
                "${parts[0].replace(".", "")}.${parts[1]}"
            } else {
                trimmed.replace(",", "")
            }
        } else if (trimmed.contains('.')) {
            val parts = trimmed.split('.')
            if (parts.size > 2) {
                trimmed.replace(".", "")
            } else if (parts.size == 2) {
                if (parts[1].length == 3 && parts[0].length in 1..3) {
                    parts[0] + parts[1]
                } else {
                    trimmed
                }
            } else {
                trimmed
            }
        } else {
            trimmed
        }

        return try {
            Decimal.parse(result).toFixed(2)
        } catch (_: Exception) {
            null
        }
    }
}
