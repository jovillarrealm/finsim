package com.finsim.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finsim.app.theme.*
import com.finsim.app.ui.formatMoney
import com.finsim.app.ui.formatMonth
import com.finsim.core.Decimal
import com.finsim.domain.Currency
import com.finsim.domain.LoanResult

@Composable
fun LoanSummary(
    currency: Currency,
    result: LoanResult,
    original: LoanResult? = null,
    modifier: Modifier = Modifier
) {
    val origInterest = original?.totals?.interest?.let { Decimal.parse(it) } ?: Decimal.ZERO
    val resultInterest = Decimal.parse(result.totals.interest)
    val interestDelta = if (original != null) resultInterest - origInterest else Decimal.ZERO

    val origCash = original?.totals?.cashPaid?.let { Decimal.parse(it) } ?: Decimal.ZERO
    val resultCash = Decimal.parse(result.totals.cashPaid)
    val cashDelta = if (original != null) resultCash - origCash else Decimal.ZERO

    val origInsurance = original?.totals?.insurance?.let { Decimal.parse(it) } ?: Decimal.ZERO
    val resultInsurance = Decimal.parse(result.totals.insurance)
    val insuranceDelta = if (original != null) resultInsurance - origInsurance else Decimal.ZERO

    val origPayoff = original?.payoffMonth ?: original?.rows?.size ?: result.rows.size
    val modPayoff = result.payoffMonth ?: result.rows.size
    val monthsSaved = origPayoff - modPayoff

    val interestPctSaved = if (original != null && origInterest > Decimal.ZERO && interestDelta < Decimal.ZERO) {
        val ratio = ((-interestDelta) / origInterest) * Decimal.fromInt(100)
        ratio.toFixed(0).toIntOrNull()
    } else null

    val firstRow = result.rows.firstOrNull()
    val monthlyInstallment = if (firstRow != null) {
        (Decimal.parse(result.basePayment) + Decimal.parse(firstRow.insuranceCharged)).toFixed(2)
    } else result.basePayment

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tu préstamo en cifras",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandDark
                    )
                    Surface(
                        color = if (result.complete) BrandSageLight else BrandAmberLight,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (result.complete) KpiMintBorder else KpiAmberBorder)
                    ) {
                        Text(
                            text = if (result.complete) "Simulación completa" else "Simulación incompleta",
                            color = if (result.complete) BrandSage else BrandAmberText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Valores consolidados según las condiciones del escenario.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextForestMuted
                )
            }

            if (!result.complete) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Resultado incompleto: importes acumulados hasta el tope simulado de 1.200 meses.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjeta Hero Principal: Ahorro Neto Proyectado
            if (original != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceHighlight,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KpiMintBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ahorro Neto Proyectado",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandSage,
                                letterSpacing = 0.5.sp
                            )
                            if (interestPctSaved != null && interestPctSaved > 0) {
                                Surface(
                                    color = BrandSageLight,
                                    shape = RoundedCornerShape(999.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, KpiMintBorder)
                                ) {
                                    Text(
                                        text = "↓ -$interestPctSaved% intereses",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandSage,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val savingsText = when {
                            cashDelta < Decimal.ZERO -> "- ${formatMoney((-cashDelta).toFixed(2), currency)}"
                            cashDelta > Decimal.ZERO -> "+ ${formatMoney(cashDelta.toFixed(2), currency)}"
                            else -> formatMoney("0.00", currency)
                        }

                        Text(
                            text = savingsText,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = BrandSage,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSage)
                                Text(
                                    text = if (monthsSaved > 0) {
                                        "Tiempo ganado: $monthsSaved ${if (monthsSaved == 1) "mes" else "meses"}${
                                            if (monthsSaved >= 12) " (${monthsSaved / 12} ${if (monthsSaved / 12 == 1) "año" else "años"})" else ""
                                        }"
                                    } else {
                                        "Al día con el plazo contractual"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = BrandForest
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(999.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSage)
                            ) {
                                Text(
                                    text = if (result.complete && result.payoffMonth != null) {
                                        "Libre en Mes ${result.payoffMonth}/$origPayoff"
                                    } else {
                                        "En curso · Mes ${result.rows.size}"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandForest,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // 2x2 Modern KPI Grid with Thematic Semantic Tints & Strikethrough
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryKpiCard(
                    modifier = Modifier.weight(1f),
                    label = "Cuota base",
                    copyLabel = "Cuota base Modificado",
                    value = formatMoney(result.basePayment, currency),
                    clipboardValue = clipboardMoney(result.basePayment),
                    caption = "Pago mensual habitual",
                    containerColor = KpiMintBg,
                    borderColor = KpiMintBorder,
                    labelColor = KpiMintText,
                    captionColor = KpiMintText
                )
                SummaryKpiCard(
                    modifier = Modifier.weight(1f),
                    label = "Liquidación",
                    copyLabel = "Liquidación Modificado",
                    value = result.payoffDate?.let { formatMonth(it) } ?: "No alcanzada",
                    copyCaptionLabel = "duración Modificado",
                    caption = result.payoffMonth?.let { "Mes $it de amortización" } ?: "Sin fecha final",
                    containerColor = KpiSkyBg,
                    borderColor = KpiSkyBorder,
                    labelColor = KpiSkyText,
                    captionColor = KpiSkyText
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryKpiCard(
                    modifier = Modifier.weight(1f),
                    label = "Total intereses",
                    copyLabel = "Total intereses Modificado",
                    value = formatMoney(result.totals.interest, currency),
                    clipboardValue = clipboardMoney(result.totals.interest),
                    deltaBadge = if (interestDelta < Decimal.ZERO) "-${formatMoney((-interestDelta).toFixed(2), currency)}" else null,
                    caption = if (original != null && interestDelta < Decimal.ZERO) "Pactados: ${formatMoney(original.totals.interest, currency)}" else "Costo financiero neto",
                    strikethroughCaption = if (original != null && interestDelta < Decimal.ZERO) "Pactados: ${formatMoney(original.totals.interest, currency)}" else null,
                    containerColor = KpiAmberBg,
                    borderColor = KpiAmberBorder,
                    labelColor = KpiAmberText,
                    captionColor = KpiAmberText
                )
                SummaryKpiCard(
                    modifier = Modifier.weight(1f),
                    label = "Caja total pagada",
                    copyLabel = "Caja total pagada Modificado",
                    value = formatMoney(result.totals.cashPaid, currency),
                    clipboardValue = clipboardMoney(result.totals.cashPaid),
                    caption = if (original != null && cashDelta < Decimal.ZERO) "Antes: ${formatMoney(original.totals.cashPaid, currency)}" else "Capital, interés, seguros y otros cargos",
                    strikethroughCaption = if (original != null && cashDelta < Decimal.ZERO) "Antes: ${formatMoney(original.totals.cashPaid, currency)}" else null,
                    containerColor = Color(0xFFD1FAE5),
                    borderColor = Color(0xFF6EE7B7),
                    labelColor = Color(0xFF064E3B),
                    captionColor = Color(0xFF047857),
                    isHighlight = true
                )
            }

            if (result.totals.unapplied != "0.00" || result.totals.insurance != "0.00") {
                Spacer(modifier = Modifier.height(10.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (result.totals.insurance != "0.00") {
                        Surface(
                            color = KpiPlumBg,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, KpiPlumBorder)
                        ) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("Seguros totales", style = MaterialTheme.typography.labelSmall, color = KpiPlumText, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(2.dp))
                                CopyMoney("seguros totales Modificado", result.totals.insurance, currency)
                            }
                        }
                    }
                    if (result.totals.unapplied != "0.00") {
                        Surface(
                            color = KpiAmberBg,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, KpiAmberBorder)
                        ) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("Excedente no aplicado", style = MaterialTheme.typography.labelSmall, color = KpiAmberText, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(2.dp))
                                CopyMoney("excedente no aplicado Modificado", result.totals.unapplied, currency)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryKpiCard(
    label: String,
    value: String,
    caption: String,
    strikethroughCaption: String? = null,
    deltaBadge: String? = null,
    clipboardValue: String = value,
    copyLabel: String = "$label Modificado",
    copyCaptionLabel: String? = null,
    containerColor: Color = Color.White,
    borderColor: Color = BorderSage,
    labelColor: Color = Color(0xFF435845),
    captionColor: Color = Color(0xFF5A6E59),
    isHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = labelColor,
                        maxLines = 1,
                        softWrap = true,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (deltaBadge != null) {
                        Surface(
                            color = BrandSageLight,
                            shape = RoundedCornerShape(999.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, KpiMintBorder)
                        ) {
                            Text(
                                text = deltaBadge,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandSage,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                CopyResult(copyLabel, value, clipboardValue)
            }
            Spacer(modifier = Modifier.height(6.dp))
            if (strikethroughCaption != null) {
                Text(
                    text = strikethroughCaption,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = captionColor,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.LineThrough,
                    maxLines = 1,
                    softWrap = true
                )
            } else if (copyCaptionLabel != null) {
                CopyResult(copyCaptionLabel, caption)
            } else {
                Text(
                    text = caption,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = captionColor,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    softWrap = true
                )
            }
        }
    }
}
