package com.finsim.app

import androidx.compose.foundation.background
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
                Surface(
                    color = BrandNavy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "finsim",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = ".",
                                color = BrandTeal,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Tu laboratorio financiero",
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BrandBlue.copy(alpha = 0.05f)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "MENOS INCERTIDUMBRE. MÁS PERSPECTIVA.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                        Text(
                            text = "Decide con los números claros.",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            text = "Explora tu préstamo, cambia un mes y entiende qué pasa con tu dinero.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceLight,
                    contentColor = BrandBlue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Explorar préstamo") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Convertir tasas") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Comparar extracto") }
                    )
                }

                // Scenario Toolbar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "ESCENARIO ACTUAL", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                Text(text = workspace.scenario.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
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
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Guardar", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val json = serializeScenario(workspace.scenario)
                                        FinsimLog.ui.i { "Exportando copia JSON de '${workspace.scenario.name}' (${json.length} chars)" }
                                        onExportJson?.invoke(json)
                                        noticeMessage = "Copia JSON exportada."
                                    },
                                    enabled = !pendingEvents,
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
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Ejemplo", fontSize = 12.sp)
                                }
                            }
                        }

                        if (pendingEvents) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Hay cambios de meses pendientes. Aplica o descarta el lote antes de guardar o exportar.",
                                color = BrandOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (noticeMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = noticeMessage, color = BrandTeal, fontSize = 11.sp)
                        }
                        if (errorMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                        }
                    }
                }

                // Tab Content
                when (selectedTab) {
                    0 -> {
                        // Loan exploration screen
                        LoanForm(
                            scenario = workspace.scenario,
                            onApplyScenario = { newScen ->
                                try {
                                    workspace = createWorkspace(newScen)
                                    noticeMessage = "Escenario actualizado."
                                    errorMessage = ""
                                    true
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "No se pudo calcular."
                                    false
                                }
                            }
                        )

                        LoanSummary(
                            currency = workspace.scenario.currency,
                            result = workspace.modified
                        )

                        HorizontalComparisonChart(
                            currency = workspace.scenario.currency,
                            original = workspace.original,
                            modified = workspace.modified,
                            selectedMonth = selectedMonth,
                            onSelectMonth = { selectedMonth = it }
                        )

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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Modo de tabla:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Row {
                                FilterChip(
                                    selected = tableMode == "modified",
                                    onClick = { tableMode = "modified" },
                                    label = { Text("Modificado") }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                FilterChip(
                                    selected = tableMode == "original",
                                    onClick = { tableMode = "original" },
                                    label = { Text("Original") }
                                )
                            }
                        }

                        MonthlyTable(
                            currency = workspace.scenario.currency,
                            result = if (tableMode == "modified") workspace.modified else workspace.original,
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

                // Footer
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Column {
                        Text(
                            text = "Un espacio para explorar, no una promesa de tu banco. Modelo educativo mensual. Los extras se aplican después del pago habitual; primero seguros, luego intereses y capital.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Los escenarios se guardan solo en este dispositivo. Exporta una copia JSON para conservarlos.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
