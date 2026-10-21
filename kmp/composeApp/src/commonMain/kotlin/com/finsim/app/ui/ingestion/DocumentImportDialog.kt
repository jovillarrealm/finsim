package com.finsim.app.ui.ingestion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.finsim.app.theme.*
import com.finsim.domain.Currency
import com.finsim.domain.Scenario
import com.finsim.ingestion.*
import com.finsim.reconciliation.StatementRow
import kotlinx.coroutines.launch

private val SAMPLE_COLOMBIA_STATEMENT = """
    Bancolombia S.A. - Extracto Crédito de Consumo / Libre Inversión
    Fecha de corte: 15/10/2026
    Titular: Juan Pérez - Obligación No. 450-89123-01
    Cuota 12 de 48
    
    RESUMEN DE LA OBLIGACIÓN
    Saldo de capital a la fecha: $ 24.500.000,00
    Capital inicial desembolsado: $ 30.000.000,00
    Tasa Efectiva Anual (E.A.): 19.50%
    Tasa Mes Vencido (M.V.): 1.50%
    Plazo pactado: 48 meses
    Valor cuota mensual: $ 850.000,00
    
    DESGLOSE DE CARGOS Y SEGUROS
    Abono a capital: $ 350.000,00
    Interés corriente: $ 477.500,00
    Seguro deudores: $ 12.500,00
    Seguro de vida grupo: $ 10.000,00
    Total otros cobros: $ 22.500,00
    Saldo final: $ 24.150.000,00
""".trimIndent()

private val SAMPLE_MEXICO_STATEMENT = """
    BBVA México - Estado de Cuenta de Crédito al Consumo
    Fecha de corte: 2026-10-01
    Número de crédito: 0148-9923812
    Cuota 5 de 24
    
    CONDICIONES DEL CRÉDITO
    Saldo insoluto: $ 120,000.00
    Capital original: $ 150,000.00
    Tasa de interés ordinaria anual fija: 22.50%
    CAT Promedio: 26.8% sin IVA
    Plazo total: 24 meses
    Pago mensual regular: $ 6,250.00
    
    COMISIONES Y SEGUROS
    Amortización a capital: $ 4,120.00
    Interés ordinario: $ 1,360.00
    Seguro de desempleo: $ 450.00
    Seguro de vida: $ 320.00
    Saldo final: $ 115,880.00
""".trimIndent()

private val SAMPLE_ARGENTINA_STATEMENT = """
    Banco Galicia - Resumen de Préstamo Personal
    Fecha de liquidación: 10/10/2026
    Cuenta Nro: 4012948-1
    Cuota 3 de 36
    
    DETALLE FINANCIERO
    Saldo deudor: $ 850.000,50
    Monto desembolsado: $ 1.200.000,00
    TNA (Tasa Nominal Anual): 48.00%
    TEA (Tasa Efectiva Anual): 60.10%
    CFT TEA: 78.50%
    Plazo restante: 36 meses
    Cuota mensual: $ 45.200,00
    Abono a capital: $ 12.500,00
    Interés del periodo: $ 30.850,00
    Seguro de vida sobre saldo deudor: $ 1.850,00
    Saldo final: $ 837.500,50
""".trimIndent()

/**
 * Main ingestion modal where users select their jurisdiction / regulatory framework,
 * provide the document text (paste, sample, or loaded document), and trigger Tier 1 parsing
 * and review digital statement text before importing. Image inference is unavailable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentImportDialog(
    targetMode: ImportTargetMode = ImportTargetMode.SCENARIO,
    targetCurrency: Currency? = null,
    onDismiss: () -> Unit,
    onScenarioImported: (Scenario) -> Unit = {},
    onStatementRowImported: (StatementRow) -> Unit = {}
) {
    var selectedFramework by remember { mutableStateOf(RegulatoryFramework.COLOMBIA_SFC) }
    var rawText by remember { mutableStateOf("") }
    var frameworkMenuExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var noticeMessage by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var reviewData by remember { mutableStateOf<ExtractedDocumentData?>(null) }
    val scrollState = rememberScrollState()

    if (reviewData != null) {
        ExtractionReviewSheet(
            extractedData = reviewData!!,
            framework = selectedFramework,
            targetMode = targetMode,
            targetCurrency = targetCurrency,
            onDismiss = { reviewData = null },
            onConfirmImport = { scenario ->
                onScenarioImported(scenario)
                reviewData = null
                onDismiss()
            },
            onConfirmStatementImport = { stmt ->
                onStatementRowImported(stmt)
                reviewData = null
                onDismiss()
            }
        )
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceWarm,
            border = BorderStroke(1.dp, BorderSage),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (targetMode == ImportTargetMode.RECONCILIATION) "Auditar extracto mensual" else "Importar extracto bancario",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandDark
                        )
                        Text(
                            text = if (targetMode == ImportTargetMode.RECONCILIATION)
                                "Lectura nativa en edge (Tier 1: 0 MB · 100% privado)."
                            else
                                "Lectura nativa en dispositivo (Tier 1: 0 MB · 100% privado).",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextForestMuted
                        )
                    }

                    Surface(
                        color = BrandLime.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Tier 1",
                            color = BrandForest,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selector de Marco Regulatorio
                Text(
                    text = "MARCO REGULATORIO / PAÍS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandForest,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = frameworkMenuExpanded,
                    onExpandedChange = { frameworkMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedFramework.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Regulación y convención financiera") },
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frameworkMenuExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = frameworkMenuExpanded,
                        onDismissRequest = { frameworkMenuExpanded = false }
                    ) {
                        RegulatoryFramework.entries.forEach { framework ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(framework.label, fontWeight = FontWeight.SemiBold, color = BrandDark)
                                        Text(
                                            "Moneda: ${framework.defaultCurrency.name} · Tasa base: ${framework.defaultRateKind}",
                                            fontSize = 11.sp,
                                            color = TextForestMuted
                                        )
                                    }
                                },
                                onClick = {
                                    selectedFramework = framework
                                    frameworkMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick load samples
                Text(
                    text = "EJEMPLO RÁPIDO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandForest,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedFramework = RegulatoryFramework.COLOMBIA_SFC
                            rawText = SAMPLE_COLOMBIA_STATEMENT
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🇨🇴 Bancolombia", fontSize = 11.sp, color = BrandForest)
                    }
                    OutlinedButton(
                        onClick = {
                            selectedFramework = RegulatoryFramework.MEXICO_CONDUSEF
                            rawText = SAMPLE_MEXICO_STATEMENT
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🇲🇽 BBVA Mex", fontSize = 11.sp, color = BrandForest)
                    }
                    OutlinedButton(
                        onClick = {
                            selectedFramework = RegulatoryFramework.ARGENTINA_BCRA
                            rawText = SAMPLE_ARGENTINA_STATEMENT
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🇦🇷 Galicia", fontSize = 11.sp, color = BrandForest)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input de texto del extracto
                Text(
                    text = "CONTENIDO DEL EXTRACTO (TEXTO DIGITAL O PDF)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandForest,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = rawText,
                    onValueChange = {
                        rawText = it
                        if (errorMessage.isNotBlank()) errorMessage = ""
                    },
                    label = { Text("Pega el texto del extracto o carga las páginas...") },
                    placeholder = { Text("Copia y pega el texto del PDF bancario aquí...") },
                    minLines = 5,
                    maxLines = 8,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (noticeMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = BrandSageLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = noticeMessage,
                            color = BrandSage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Text("El análisis visual de imágenes no está disponible. Pega texto seleccionable y revisa los datos antes de importar.")

                // Actions: Cancel & Analyze
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderSage),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Cancelar", color = TextForestMuted, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isAnalyzing = true
                                errorMessage = ""
                                try {

                                        if (rawText.isBlank()) {
                                            errorMessage = "Pega o carga el texto del extracto para continuar."
                                            return@launch
                                        }
                                        val pages = DigitalPdfTextExtractor.extractPages(rawText.encodeToByteArray())
                                        if (pages.isEmpty() || pages.all { it.isBlank() }) {
                                            errorMessage = "No se detectó texto digital seleccionable en el documento."
                                            return@launch
                                        }
                                        val bestPageIndex = PageDensityScanner.findBestPageIndex(pages)
                                        val bestPageText = pages[bestPageIndex - 1]
                                        reviewData = DocumentTextParser.parse(bestPageText, selectedFramework, bestPageIndex)
                                        return@launch
                                } catch (e: Exception) {
                                    errorMessage = "Error al procesar el extracto: ${e.message}"
                                } finally {
                                    isAnalyzing = false
                                }
                            }
                        },
                        enabled = !isAnalyzing,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandForest, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.5f).height(46.dp)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = BrandLime,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Procesando...", fontSize = 12.sp, color = Color.White)
                        } else {
                            Text("Analizar extracto", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
