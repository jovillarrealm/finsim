package com.finsim.ingestion

import java.math.BigDecimal
import java.security.MessageDigest

// Temporary synthetic-only instrumentation. Remove after the coordinated diagnostic run.
internal object TemporarySyntheticFixtureProbe {
    const val PDF_BYTES = 2_391
    const val PDF_SHA256 = "2324b53f2250c8627a5f30c4a115712d5e92a5b5c549114b20ef6c843f84c5a0"
    const val PAGE_INDEX = 1
    const val CONTROL_MAX_OUTPUT_TOKENS = 64
    const val CONTROL_PROMPT =
        "Copia literalmente la línea que identifica la cuota mensual de capital e intereses antes de seguros, incluyendo rótulo, importe y moneda. No expliques ni calcules. Si no puedes leerla, responde NO_LEGIBLE."

    val rasterGate = SyntheticFixtureRasterGate(PDF_BYTES, PDF_SHA256, PAGE_INDEX)
}

internal data class SyntheticFixtureRasterIdentity(
    val pdfSha256: String,
    val imageSha256: String,
    val width: Int,
    val height: Int
)

/** Binds a one-shot probe marker to the exact source PDF page and its in-memory PNG raster. */
internal class SyntheticFixtureRasterGate(
    private val expectedPdfBytes: Int,
    private val expectedPdfSha256: String,
    private val expectedPageIndex: Int
) {
    private var generation = 0L
    private var pendingRaster: SyntheticFixtureRasterIdentity? = null

    fun beginPdf(pdfBytes: ByteArray, pageIndex: Int): RasterTicket? {
        val requestGeneration = synchronized(this) {
            generation += 1
            pendingRaster = null
            generation
        }
        if (pageIndex != expectedPageIndex || pdfBytes.size != expectedPdfBytes) return null
        if (sha256(pdfBytes) != expectedPdfSha256) return null
        return RasterTicket(requestGeneration)
    }

    fun armAfterRaster(ticket: RasterTicket?, imageBytes: ByteArray, width: Int, height: Int) {
        if (ticket == null || width <= 0 || height <= 0) return
        if (synchronized(this) { ticket.generation == generation }.not()) return
        val identity = SyntheticFixtureRasterIdentity(
            pdfSha256 = expectedPdfSha256,
            imageSha256 = sha256(imageBytes),
            width = width,
            height = height
        )
        synchronized(this) {
            if (ticket.generation == generation) pendingRaster = identity
        }
    }

    fun consumeForInference(imageBytes: ByteArray): SyntheticFixtureRasterIdentity? {
        val expectedRaster = synchronized(this) {
            pendingRaster.also { pendingRaster = null }
        } ?: return null
        return expectedRaster.takeIf { it.imageSha256 == sha256(imageBytes) }
    }

    internal data class RasterTicket(val generation: Long)

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}

internal enum class SyntheticLiteralStatus {
    MATCH,
    MISMATCH,
    OMITTED
}

internal enum class SyntheticFieldStatus {
    MATCH,
    MISMATCH,
    OMITTED,
    AMBIGUOUS,
    UNAVAILABLE
}

/** Local, value-free comparison against the public fixture reference; never returns model text. */
internal object SyntheticFixtureComparison {
    private const val EXPECTED_LITERAL_LABEL = "cuota base mensual (capital e intereses)"
    private val expectedLiteralLine = Regex(
        """(?<![a-z0-9])cuota\s+base\s+mensual\s*\(\s*capital\s+e\s+intereses\s*\)\s*:\s*507\s*,\s*51\s+cop\b\.?"""
    )
    private val otherPaymentMeaning = Regex("""\bcuota\b.{0,80}\b(?:total|seguro)\b""")

    fun compareLiteralLine(response: String): SyntheticLiteralStatus {
        val normalized = normalize(response)
        return when {
            expectedLiteralLine.containsMatchIn(normalized) -> SyntheticLiteralStatus.MATCH
            normalized.contains(EXPECTED_LITERAL_LABEL) || otherPaymentMeaning.containsMatchIn(normalized) ->
                SyntheticLiteralStatus.MISMATCH
            else -> SyntheticLiteralStatus.OMITTED
        }
    }

    fun compareStructuredFields(data: ExtractedDocumentData): Map<String, SyntheticFieldStatus> = linkedMapOf(
        "basePayment.direct" to moneyStatus("507.51", data.basePayment),
        "basePayment.reviewedMonthly" to monthlyPaymentStatus(
            expected = "507.51",
            actual = data.monthlyPayment,
            actualMeaning = data.paymentMeaning,
            expectedMeaning = PaymentMeaning.BASE_PAYMENT
        ),
        "totalPayment.direct" to moneyStatus("527.51", data.totalPayment),
        "totalPayment.reviewedMonthly" to monthlyPaymentStatus(
            expected = "527.51",
            actual = data.monthlyPayment,
            actualMeaning = data.paymentMeaning,
            expectedMeaning = PaymentMeaning.TOTAL_PAYMENT
        ),
        "capitalBalance.postParse" to moneyStatus("502.49", data.capitalBalance),
        "balanceMeaning" to balanceMeaningStatus(data),
        "principalPaid" to moneyStatus("497.51", data.principalPaid),
        "interestPaid" to moneyStatus("10.00", data.interestCharged),
        "insurancePaid" to moneyStatus("20.00", data.totalInsurance),
        "insurance.periodicCharge" to insuranceChargeStatus(data),
        "insurance.amountMeaning" to insuranceMeaningStatus(data)
    )

    fun unavailableStatuses(): Map<String, SyntheticFieldStatus> =
        FIELD_NAMES.associateWith { SyntheticFieldStatus.UNAVAILABLE }

    fun formatStatuses(statuses: Map<String, SyntheticFieldStatus>): String =
        statuses.entries.joinToString(",") { (field, status) -> "$field=${status.name.lowercase()}" }

    private fun monthlyPaymentStatus(
        expected: String,
        actual: String?,
        actualMeaning: PaymentMeaning,
        expectedMeaning: PaymentMeaning
    ): SyntheticFieldStatus = when {
        actual == null -> SyntheticFieldStatus.OMITTED
        actualMeaning == PaymentMeaning.UNKNOWN -> SyntheticFieldStatus.AMBIGUOUS
        actualMeaning != expectedMeaning -> SyntheticFieldStatus.OMITTED
        else -> moneyStatus(expected, actual)
    }

    private fun balanceMeaningStatus(data: ExtractedDocumentData): SyntheticFieldStatus = when {
        data.capitalBalance == null && data.balanceMeaning == BalanceMeaning.UNKNOWN -> SyntheticFieldStatus.OMITTED
        data.capitalBalance == null -> SyntheticFieldStatus.AMBIGUOUS
        data.balanceMeaning == BalanceMeaning.PRINCIPAL -> SyntheticFieldStatus.MATCH
        data.balanceMeaning == BalanceMeaning.UNKNOWN -> SyntheticFieldStatus.AMBIGUOUS
        else -> SyntheticFieldStatus.MISMATCH
    }

    private fun insuranceChargeStatus(data: ExtractedDocumentData): SyntheticFieldStatus {
        val insurance = data.insurances.singleOrNull() ?: return when {
            data.insurances.isEmpty() -> SyntheticFieldStatus.OMITTED
            else -> SyntheticFieldStatus.AMBIGUOUS
        }
        return when (insurance.amountMeaning) {
            InsuranceAmountMeaning.PERIODIC_CHARGE -> moneyStatus("20.00", insurance.monthlyCost)
            InsuranceAmountMeaning.INSURED_VALUE -> SyntheticFieldStatus.MISMATCH
            InsuranceAmountMeaning.UNKNOWN -> if (insurance.monthlyCost == null) {
                SyntheticFieldStatus.OMITTED
            } else {
                SyntheticFieldStatus.AMBIGUOUS
            }
        }
    }

    private fun insuranceMeaningStatus(data: ExtractedDocumentData): SyntheticFieldStatus {
        val insurance = data.insurances.singleOrNull() ?: return when {
            data.insurances.isEmpty() -> SyntheticFieldStatus.OMITTED
            else -> SyntheticFieldStatus.AMBIGUOUS
        }
        return when (insurance.amountMeaning) {
            InsuranceAmountMeaning.PERIODIC_CHARGE -> SyntheticFieldStatus.MATCH
            InsuranceAmountMeaning.INSURED_VALUE -> SyntheticFieldStatus.MISMATCH
            InsuranceAmountMeaning.UNKNOWN -> SyntheticFieldStatus.AMBIGUOUS
        }
    }

    private fun moneyStatus(expected: String, actual: String?): SyntheticFieldStatus {
        if (actual.isNullOrBlank()) return SyntheticFieldStatus.OMITTED
        return runCatching {
            if (BigDecimal(expected).compareTo(BigDecimal(actual)) == 0) {
                SyntheticFieldStatus.MATCH
            } else {
                SyntheticFieldStatus.MISMATCH
            }
        }.getOrDefault(SyntheticFieldStatus.MISMATCH)
    }

    private fun normalize(value: String): String = value
        .replace('\u00a0', ' ')
        .trim()
        .replace(Regex("\\s+"), " ")
        .lowercase()

    private val FIELD_NAMES = listOf(
        "basePayment.direct", "basePayment.reviewedMonthly", "totalPayment.direct", "totalPayment.reviewedMonthly",
        "capitalBalance.postParse", "balanceMeaning", "principalPaid", "interestPaid", "insurancePaid",
        "insurance.periodicCharge", "insurance.amountMeaning"
    )
}
