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
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Resumen del préstamo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                    Text(
                        text = "Valores consolidados según las condiciones del escenario.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Surface(
                    color = if (result.complete) BrandTeal.copy(alpha = 0.12f) else BrandOrange.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (result.complete) "Liquidado" else "En curso",
                        color = if (result.complete) BrandTeal else BrandOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryKpiCard(
                    modifier = Modifier.weight(1f),
                    label = "Cuota base",
                    value = formatMoney(result.basePayment, currency),
                    caption = "Pago mensual habitual"
                )
                SummaryKpiCard(
                    modifier = Modifier.weight(1f),
                    label = "Liquidación",
                    value = result.payoffDate?.let { formatMonth(it) } ?: "No alcanzada",
                    caption = result.payoffMonth?.let { "Mes $it de amortización" } ?: "Sin fecha final"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryKpiCard(
                    modifier = Modifier.weight(1f),
                    label = "Total intereses",
                    value = formatMoney(result.totals.interest, currency),
                    caption = "Costo financiero neto"
                )
                SummaryKpiCard(
                    modifier = Modifier.weight(1f),
                    label = "Caja total pagada",
                    value = formatMoney(result.totals.cashPaid, currency),
                    caption = "Capital + interés + seguros",
                    isHighlight = true
                )
            }

            if (result.totals.unapplied != "0.00" || result.totals.insurance != "0.00") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (result.totals.insurance != "0.00") {
                        Surface(
                            color = BrandPurple.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Seguros totales: ${formatMoney(result.totals.insurance, currency)}",
                                color = BrandPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (result.totals.unapplied != "0.00") {
                        Surface(
                            color = BrandOrange.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Excedente no aplicado: ${formatMoney(result.totals.unapplied, currency)}",
                                color = BrandOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
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
    isHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = if (isHighlight) BrandBlue.copy(alpha = 0.06f) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isHighlight) BrandBlue else TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHighlight) BrandBlue else BrandNavy
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = caption,
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}
