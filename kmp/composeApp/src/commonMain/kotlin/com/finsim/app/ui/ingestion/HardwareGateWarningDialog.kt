package com.finsim.app.ui.ingestion

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.window.Dialog
import com.finsim.app.theme.*
import com.finsim.ingestion.DeviceCapabilityGate

/**
 * Dialog alerting users when device RAM < 8 GiB, explaining that on-device deep VLM (Tier 2)
 * cannot execute to prevent Low Memory Killer (LMKD) crashes.
 * Reassures that digital text extraction (Tier 1) remains 100% functional and private.
 */
@Composable
fun HardwareGateWarningDialog(
    gate: DeviceCapabilityGate,
    onDismiss: () -> Unit,
    onContinueWithTier1: () -> Unit = onDismiss
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceWarm,
            border = BorderStroke(1.dp, BorderSage),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header with warning badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Capacidad de hardware",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandDark
                    )
                    Surface(
                        color = BrandAmberLight,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "RAM < 8 GiB",
                            color = BrandAmberText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // RAM details summary
                Surface(
                    color = SurfaceHighlight,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderSage),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Memoria detectada:", fontSize = 12.sp, color = TextForestMuted)
                            Text(
                                "${(gate.totalRamGb * 10).toLong() / 10.0} GiB",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandDark
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Requerimiento VLM (Tier 2):", fontSize = 12.sp, color = TextForestMuted)
                            Text(
                                ">= ${DeviceCapabilityGate.MINIMUM_RAM_GB_FOR_VLM} GiB RAM",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandAmber
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Ineligibility explanation
                Text(
                    text = gate.ineligibilityReason
                        ?: "Para evitar cierres forzosos causados por el gestor de memoria (Android LMKD), el modelo de visión profunda Gemma 4 E2B INT4 no puede ejecutarse en este dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandDark,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Reassurance regarding Tier 1
                Surface(
                    color = BrandSageLight,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "✓",
                            color = BrandSage,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "La extracción de texto digital (Tier 1) se mantiene 100% activa, privada y sin costo de memoria. Puedes importar PDFs digitales o ingresar los valores manualmente.",
                            color = BrandSage,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action button
                Button(
                    onClick = onContinueWithTier1,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandForest, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text(
                        text = "Entendido, continuar con Tier 1",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
