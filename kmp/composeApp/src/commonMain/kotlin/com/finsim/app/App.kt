package com.finsim.app

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
import com.finsim.app.theme.*
import com.finsim.app.ui.components.*
import com.finsim.core.FinsimLog
import com.finsim.domain.*
import com.finsim.engine.simulateLoan
import com.finsim.persistence.*

data class Workspace(
    val scenario: Scenario,
    val original: LoanResult,
    val modified: LoanResult
)

fun createWorkspace(scenario: Scenario): Workspace {
    FinsimLog.ui.i { "createWorkspace: '${scenario.name}', moneda=${scenario.currency}, eventos=${scenario.events.size}" }
    val origScenario = scenario.copy(events = emptyList())
    return Workspace(
        scenario = scenario,
        original = simulateLoan(origScenario),
        modified = simulateLoan(scenario)
    )
}

@Composable
fun App(
    storage: ScenarioStorage = remember { InMemoryScenarioStorage() },
    onExportJson: ((String) -> Unit)? = null,
    onImportJsonRequest: (() -> Unit)? = null
) {
    var workspace by remember {
        val initial = try {
            val saved = loadScenario(storage)
            if (saved != null) createWorkspace(saved) else createWorkspace(exampleScenario)
        } catch (_: Exception) {
            createWorkspace(exampleScenario)
        }
        mutableStateOf(initial)
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: loan, 1: rates, 2: reconcile
    var selectedMonth by remember { mutableStateOf(1) }
    var pendingEvents by remember { mutableStateOf(false) }
    var tableMode by remember { mutableStateOf("modified") } // "modified" or "original"

    var noticeMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    FinsimTheme {
        Scaffold(
            topBar = {
                // Top App Bar with safe status bar padding for camera notches
                Surface(
                    color = SurfaceLight,
                    shadowElevation = 1.dp,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "finsim",
                                    color = BrandNavy,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = ".",
                                    color = BrandTeal,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = BrandBlue.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Simulador",
                                        color = BrandBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Active currency and scenario badge
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${workspace.scenario.currency.name} · ${workspace.scenario.termMonths}m",
                                    color = BrandNavy,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(BackgroundLight)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Navigation Segmented Pills
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf(
                            "💳 Préstamo",
                            "🔄 Conversor",
                            "📑 Conciliación"
                        )
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Surface(
                                color = if (isSelected) SurfaceLight else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        FinsimLog.ui.d { "Navegación de pestaña: $selectedTab -> $index" }
                                        selectedTab = index
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) BrandNavy else TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // Scenario Toolbar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ESCENARIO ACTIVO",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = workspace.scenario.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandNavy
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        try {
                                            saveScenario(workspace.scenario, storage)
                                            FinsimLog.ui.i { "Escenario '${workspace.scenario.name}' guardado exitosamente." }
                                            noticeMessage = "Escenario guardado en este dispositivo."
                                            errorMessage = ""
                                        } catch (e: Exception) {
                                            FinsimLog.ui.e(e) { "Error al guardar escenario '${workspace.scenario.name}'" }
                                            errorMessage = e.message ?: "Error al guardar."
                                        }
                                    },
                                    enabled = !pendingEvents,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Guardar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val json = serializeScenario(workspace.scenario)
                                        FinsimLog.ui.i { "Exportando copia JSON de '${workspace.scenario.name}' (${json.length} chars)" }
                                        onExportJson?.invoke(json)
                                        noticeMessage = "Copia JSON exportada."
                                    },
                                    enabled = !pendingEvents,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Exportar", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        FinsimLog.ui.i { "Restableciendo a escenario de ejemplo predeterminado." }
                                        workspace = createWorkspace(exampleScenario)
                                        noticeMessage = "Ejemplo cargado."
                                        errorMessage = ""
                                    },
                                    enabled = !pendingEvents,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Ejemplo", fontSize = 12.sp)
                                }
                            }
                        }

                        if (pendingEvents) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = BrandOrange.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Hay cambios de eventos pendientes. Aplica o descarta el lote antes de guardar.",
                                    color = BrandOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (noticeMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = BrandTeal.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = noticeMessage,
                                    color = BrandTeal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (errorMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Tab Content
                when (selectedTab) {
                    0 -> {
                        // Loan Summary Cards
                        LoanSummary(
                            currency = workspace.scenario.currency,
                            result = workspace.modified
                        )

                        // Visual Comparison Charts (Untouched: Option 1 & Option 3 with toggle)
                        HorizontalComparisonChart(
                            currency = workspace.scenario.currency,
                            original = workspace.original,
                            modified = workspace.modified,
                            selectedMonth = selectedMonth,
                            onSelectMonth = { selectedMonth = it }
                        )

                        // Event Editor
                        EventEditor(
                            currency = workspace.scenario.currency,
                            selectedMonth = selectedMonth,
                            currentEvents = workspace.scenario.events,
                            onApplyEvents = { newEvents ->
                                try {
                                    val updated = workspace.scenario.copy(events = newEvents)
                                    workspace = createWorkspace(updated)
                                    noticeMessage = "Eventos aplicados."
                                    errorMessage = ""
                                    true
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Error al aplicar eventos."
                                    false
                                }
                            },
                            onPendingChange = { pendingEvents = it }
                        )

                        // Loan Conditions Form
                        LoanForm(
                            scenario = workspace.scenario,
                            onApplyScenario = { newScen ->
                                try {
                                    workspace = createWorkspace(newScen)
                                    noticeMessage = "Condiciones actualizadas."
                                    errorMessage = ""
                                    true
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "No se pudo calcular."
                                    false
                                }
                            }
                        )

                        // Monthly Table Mode Toggle and Table
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cronograma visible:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandNavy
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = tableMode == "modified",
                                        onClick = { tableMode = "modified" },
                                        label = { Text("Modificado con eventos", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandBlue,
                                            selectedLabelColor = Color.White
                                        ),
                                        border = null
                                    )
                                    FilterChip(
                                        selected = tableMode == "original",
                                        onClick = { tableMode = "original" },
                                        label = { Text("Original base", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandBlue,
                                            selectedLabelColor = Color.White
                                        ),
                                        border = null
                                    )
                                }
                            }
                        }

                        MonthlyTable(
                            currency = workspace.scenario.currency,
                            result = if (tableMode == "modified") workspace.modified else workspace.original,
                            selectedMonth = selectedMonth,
                            onSelectMonth = { selectedMonth = it }
                        )
                    }

                    1 -> {
                        RateConverterView()
                    }

                    2 -> {
                        ReconciliationView(
                            currency = workspace.scenario.currency,
                            original = workspace.original
                        )
                    }
                }

                // Footer & Navigation Bar safe spacing
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Column {
                        Text(
                            text = "Un espacio para explorar, no una promesa bancaria. Modelo educativo mensual. Los abonos extraordinarios se aplican tras el pago habitual (seguros → intereses → capital).",
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Los escenarios se almacenan localmente en este dispositivo. Exporta copias JSON para archivarlos o transferirlos.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                // Extra breathing room at bottom for navigation bar / gesture notch
                Spacer(modifier = Modifier.navigationBarsPadding().height(24.dp))
            }
        }
    }
}
