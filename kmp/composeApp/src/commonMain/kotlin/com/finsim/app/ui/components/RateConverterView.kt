package com.finsim.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finsim.app.theme.*
import com.finsim.core.Decimal
import com.finsim.domain.Rate
import com.finsim.engine.convertRate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateConverterView(modifier: Modifier = Modifier) {
    var rateKind by remember { mutableStateOf("monthly") }
    var inputPercent by remember { mutableStateOf("1") }
    var nominalFreq by remember { mutableStateOf("12") }

    val rateObj = remember(rateKind, inputPercent, nominalFreq) {
        try {
            val decVal = (Decimal.parse(inputPercent) / Decimal.parse("100")).toString()
            when (rateKind) {
                "monthly" -> Rate.Monthly(decVal)
                "annual" -> Rate.Annual(decVal)
                "nominal" -> Rate.Nominal(decVal, nominalFreq.toIntOrNull() ?: 12)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    val emConverted = remember(rateObj) {
        rateObj?.let {
            val frac = convertRate(it, "monthly")
            val pct = (Decimal.parse(frac) * Decimal.parse("100")).toString()
            pct
        } ?: "—"
    }

    val eaConverted = remember(rateObj) {
        rateObj?.let {
            val frac = convertRate(it, "annual")
            val pct = (Decimal.parse(frac) * Decimal.parse("100")).toString()
            pct
        } ?: "—"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Conversor de tasas de interés",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Convierte con precisión decimal entre Efectiva Mensual (EM), Efectiva Anual (EA) y Nominal.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1.2f)
                ) {
                    OutlinedTextField(
                        value = when (rateKind) {
                            "monthly" -> "Efectiva mensual (EM)"
                            "annual" -> "Efectiva anual (EA)"
                            else -> "Nominal anual"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tasa de origen") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("Efectiva mensual (EM)") }, onClick = { rateKind = "monthly"; expanded = false })
                        DropdownMenuItem(text = { Text("Efectiva anual (EA)") }, onClick = { rateKind = "annual"; expanded = false })
                        DropdownMenuItem(text = { Text("Nominal anual") }, onClick = { rateKind = "nominal"; expanded = false })
                    }
                }

                OutlinedTextField(
                    value = inputPercent,
                    onValueChange = { inputPercent = it },
                    label = { Text("Tasa (%)") },
                    modifier = Modifier.weight(1f)
                )
            }

            if (rateKind == "nominal") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = nominalFreq,
                    onValueChange = { nominalFreq = it },
                    label = { Text("Frecuencia (capitalizaciones/año)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tasas equivalentes resultantes:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Efectiva Anual (EA): $eaConverted %",
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Efectiva Mensual (EM): $emConverted %",
                        fontWeight = FontWeight.Bold,
                        color = BrandTeal,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Ejemplo: 1% EM equivale exactamente a 12,6825030132% EA según la fórmula EA = (1 + EM)^12 - 1.",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}
