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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finsim.app.theme.*
import com.finsim.app.ui.formatMoney
import com.finsim.app.ui.formatMonth
import com.finsim.domain.Currency
import com.finsim.domain.LoanResult

@Composable
fun LoanSummary(
    currency: Currency,
    result: LoanResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
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
                        text = "Resumen del préstamo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandDark
                    )
                    Surface(
                        color = if (result.complete) BrandSageLight else BrandAmberLight,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (result.complete) Color(0xFFC7D9BA) else Color(0xFFE8D4A2))
                    ) {
                        Text(
                            text = if (result.complete) "Liquidado" else "En curso",
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

            // 2x2 Modern KPI Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryKpiCard(
                    modifier = Modifier.weight(1f),
                    label = "Cuota base",
                    value = formatMoney(result.basePayment, currency),
                    clipboardValue = clipboardMoney(result.basePayment),
                    caption = "Pago mensual habitual"
                )
                SummaryKpiCard(
                    modifier = Modifier.weight(1f),
                    label = "Liquidación",
                    value = result.payoffDate?.let { formatMonth(it) } ?: "No alcanzada",
                    copyCaptionLabel = "duración Modificado",
                    caption = result.payoffMonth?.let { "Mes $it de amortización" } ?: "Sin fecha final"
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
                    value = formatMoney(result.totals.interest, currency),
                    clipboardValue = clipboardMoney(result.totals.interest),
                    caption = "Costo financiero neto"
                )
                SummaryKpiCard(
                    modifier = Modifier.weight(1f),
                    label = "Caja total pagada",
                    value = formatMoney(result.totals.cashPaid, currency),
                    clipboardValue = clipboardMoney(result.totals.cashPaid),
                    caption = "Capital, interés, seguros y otros cargos",
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
                            color = BrandPlumLight,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE2D4EA))
                        ) {
                            Column {
                                Text("Seguros totales")
                                CopyMoney("seguros totales Modificado", result.totals.insurance, currency)
                            }
                        }
                    }
                    if (result.totals.unapplied != "0.00") {
                        Surface(
                            color = BrandAmberLight,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE8D4A2))
                        ) {
                            Column {
                                Text("Excedente no aplicado")
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
    clipboardValue: String = value,
    copyCaptionLabel: String? = null,
    isHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = if (isHighlight) SurfaceHighlight else Color.White,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isHighlight) Color(0xFFBED1B8) else BorderSage)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isHighlight) BrandForest else Color(0xFF435845),
                    maxLines = 1,
                    softWrap = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                CopyResult("$label Modificado", value, clipboardValue)
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (copyCaptionLabel != null) CopyResult(copyCaptionLabel, caption) else Text(
                text = caption,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = if (isHighlight) BrandSage else Color(0xFF5A6E59),
                maxLines = 2,
                softWrap = true
            )
        }
    }
}
