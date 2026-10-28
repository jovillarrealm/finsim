package com.finsim.ingestion

/** Maps model-generated DocTags into the existing editable extraction candidates. */
object DoclingDocTagsParser {
    private val tag = Regex("""</?([A-Za-z][A-Za-z0-9_-]*)(?:\s+[^<>]*)?\s*/?>""")
    private val blockTags = setOf(
        "text", "title", "paragraph", "page_header", "page_footer", "caption", "list_item",
        "section_header_level_1", "section_header_level_2", "section_header_level_3",
        "section_header_level_4", "section_header_level_5", "section_header_level_6", "table", "otsl"
    )
    private val tableCellStartTags = setOf("fcel", "ecel", "lcel", "ucel", "xcel", "ched", "rhed")
    private val contentTags = setOf(
        "text", "title", "paragraph", "page_header", "page_footer", "caption", "list_item", "table", "otsl",
        "picture", "chart", "formula", "code", "section_header_level_1", "section_header_level_2",
        "section_header_level_3", "section_header_level_4", "section_header_level_5", "section_header_level_6"
    )
    private val totalWithInsurance = Regex(
        """\b((?:cuota|pago)(?:\s+mensual)?\s+total)\s+con\s+seguros?\b""",
        RegexOption.IGNORE_CASE
    )
    private val explicitMonthlyInsuranceTotal = Regex(
        """\b(?:total\s+mensual\s+(?:de\s+)?seguros?|seguros?\s+mensuales?|total\s+seguros?\s+(?:del\s+)?mes)\b""",
        RegexOption.IGNORE_CASE
    )

    fun parse(
        docTags: String,
        framework: RegulatoryFramework,
        pageIndex: Int = 1,
        stoppedAtTokenLimit: Boolean = false
    ): ExtractedDocumentData {
        val trimmedDocTags = docTags.trim()
        require(Regex("""<doctag(?:\s|>)""").containsMatchIn(trimmedDocTags) &&
            contentTags.any { content -> Regex("""<$content(?:\s|/?>)""").containsMatchIn(trimmedDocTags) }
        ) { "La inferencia no devolvió DocTags de documento válidos." }

        var inTable = false
        val text = tag.replace(trimmedDocTags) { match ->
            val name = match.groupValues[1]
            val isClosing = match.value.startsWith("</")
            when {
                name == "otsl" -> {
                    inTable = !isClosing
                    "\n"
                }
                name == "nl" || name == "srow" -> "\n"
                name in tableCellStartTags -> if (isClosing) "" else "\t"
                name in blockTags && !inTable -> if (isClosing) "\n" else ""
                else -> ""
            }
        }
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("[ \\t]*\\n[ \\t]*"), "\n")
            .replace(Regex("\\n{2,}"), "\n")

        val normalizedText = totalWithInsurance.replace(text) { it.groupValues[1] }.trim()
        require(normalizedText.any(Char::isLetterOrDigit)) {
            "La inferencia devolvió DocTags vacíos o sin contenido legible."
        }

        val parsed = DocumentTextParser.parse(normalizedText, framework, pageIndex)
        val lineCandidates = normalizedText.lines().filter(String::isNotBlank)
            .map { DocumentTextParser.parse(it, framework, pageIndex) }
        fun <T> distinctCandidates(selector: (ExtractedDocumentData) -> T?): List<T> =
            lineCandidates.mapNotNull(selector).distinct()
        val ambiguousMultiValuePaymentRow = normalizedText.lines().any { line ->
            Regex("""(?i)\b(?:cuota|pago|mensualidad)\b""").containsMatchIn(line) &&
                !Regex("""(?i)\b(?:base|total)\b""").containsMatchIn(line) &&
                Regex("""(?:\$\s*)?(?:[0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{1,2})?|[0-9]+(?:[.,][0-9]{1,2})?)""")
                    .findAll(line)
                    .take(2)
                    .count() > 1
        }

        val warnings = mutableListOf<String>()
        val monthlyInsuranceEvidence = parsed.insurances.any {
            it.amountMeaning == InsuranceAmountMeaning.PERIODIC_CHARGE &&
                it.periodicity == InsurancePeriodicity.MONTHLY &&
                it.monthlyCost != null
        }
        val ambiguousTotalInsurance = parsed.totalInsurance != null &&
            !monthlyInsuranceEvidence &&
            !explicitMonthlyInsuranceTotal.containsMatchIn(normalizedText)
        if (stoppedAtTokenLimit) warnings += "La salida de Granite-Docling alcanzó el límite de tokens y puede estar incompleta; revisa el resultado antes de usarlo."
        val nonMonthlyInsuranceAmount = parsed.insurances.any {
            it.monthlyCost != null &&
                it.amountMeaning != InsuranceAmountMeaning.INSURED_VALUE &&
                it.periodicity != InsurancePeriodicity.MONTHLY
        }
        val conflictingOriginalPrincipal = distinctCandidates { it.originalPrincipal }.size > 1
        val conflictingCapitalBalance = distinctCandidates { it.capitalBalance }.size > 1
        val conflictingOriginalTerm = distinctCandidates { it.originalTermMonths }.size > 1
        val conflictingRemainingTerm = distinctCandidates { it.remainingTermMonths }.size > 1
        val conflictingBasePayment = distinctCandidates { it.basePayment }.size > 1 || ambiguousMultiValuePaymentRow
        val conflictingTotalPayment = distinctCandidates { it.totalPayment }.size > 1 || ambiguousMultiValuePaymentRow
        val conflictingRate = distinctCandidates { it.rate }.size > 1
        val conflictingPrincipalPaid = distinctCandidates { it.principalPaid }.size > 1
        val conflictingInterest = distinctCandidates { it.interestCharged }.size > 1
        val conflictingInsurance = distinctCandidates { it.totalInsurance }.size > 1

        if (conflictingOriginalPrincipal) warnings += "Se detectaron montos originales diferentes; revisa el capital original en el documento."
        if (conflictingCapitalBalance) warnings += "Se detectaron saldos de capital diferentes; revisa el saldo pendiente en el documento."
        if (conflictingOriginalTerm || conflictingRemainingTerm) warnings += "Se detectaron plazos distintos; revisa si son originales o restantes."
        if (conflictingRate) warnings += "Se detectaron tasas diferentes o con convenciones distintas; indica la tasa que corresponde al préstamo."
        if (ambiguousMultiValuePaymentRow) warnings += "Se detectaron varias cuotas en una fila sin una columna inequívoca; revisa cuál corresponde al préstamo."
        if (conflictingBasePayment && !ambiguousMultiValuePaymentRow) warnings += "Se detectaron cuotas base diferentes; revisa el importe antes de corregirlo."
        if (conflictingTotalPayment && !ambiguousMultiValuePaymentRow) warnings += "Se detectaron cuotas totales diferentes; revisa el importe antes de corregirlo."
        if (conflictingPrincipalPaid) warnings += "Se detectaron abonos a capital diferentes; revisa el periodo correspondiente."
        if (conflictingInterest) warnings += "Se detectaron intereses diferentes; revisa el periodo correspondiente."
        if (conflictingInsurance) warnings += "Se detectaron cargos de seguro diferentes; revisa su periodicidad y significado."
        if (nonMonthlyInsuranceAmount || ambiguousTotalInsurance) {
            warnings += "Se detectó un importe o total de seguro sin periodicidad mensual confirmada; no se asignará como cargo mensual."
        }

        return parsed.copy(
            sourceEngine = ExtractionEngine.TIER_2_LOCAL_DOCLING,
            rawExtractedText = trimmedDocTags,
            originalPrincipal = parsed.originalPrincipal.takeUnless { conflictingOriginalPrincipal },
            capitalBalance = parsed.capitalBalance.takeUnless { conflictingCapitalBalance },
            closingBalance = parsed.closingBalance.takeUnless { conflictingCapitalBalance },
            originalTermMonths = parsed.originalTermMonths.takeUnless { conflictingOriginalTerm },
            remainingTermMonths = parsed.remainingTermMonths.takeUnless { conflictingRemainingTerm },
            basePayment = parsed.basePayment.takeUnless { conflictingBasePayment },
            totalPayment = parsed.totalPayment.takeUnless { conflictingTotalPayment },
            monthlyPayment = parsed.monthlyPayment.takeUnless { conflictingBasePayment },
            paymentMeaning = if (conflictingBasePayment) PaymentMeaning.UNKNOWN else parsed.paymentMeaning,
            rate = parsed.rate.takeUnless { conflictingRate },
            principalPaid = parsed.principalPaid.takeUnless { conflictingPrincipalPaid },
            interestCharged = parsed.interestCharged.takeUnless { conflictingInterest },
            totalInsurance = parsed.totalInsurance.takeUnless {
                conflictingInsurance || nonMonthlyInsuranceAmount || ambiguousTotalInsurance
            },
            reviewWarnings = warnings
        )
    }
}
