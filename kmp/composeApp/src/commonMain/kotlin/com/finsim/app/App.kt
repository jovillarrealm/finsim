package com.finsim.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.horizontalScroll
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
import com.finsim.app.ui.ingestion.DocumentImportDialog
import com.finsim.core.FinsimLog
import com.finsim.domain.*
import com.finsim.engine.simulateLoan
import com.finsim.persistence.*

import com.finsim.workspace.WorkspaceOutcome
import com.finsim.workspace.WorkspaceState
import com.finsim.workspace.WorkspaceStore

// Typealias preserving compatibility with any internal references
typealias Workspace = WorkspaceState

@Composable
fun App(
    storage: ScenarioStorage = remember { InMemoryScenarioStorage() },
    store: WorkspaceStore = remember(storage) { WorkspaceStore(storage) }
) {
    val comparisonStore = remember(storage) { com.finsim.comparison.ComparisonWorkspace(storage) }
    var workspace by remember(store) { mutableStateOf(store.state) }

    var eventDraft by remember(workspace.scenario.events) { mutableStateOf(EventDraftBatch.from(workspace.scenario.events)) }
    var selectedTab by remember { mutableStateOf(0) } // 0: loan, 1: rates, 2: reconcile
    var selectedMonth by remember { mutableStateOf(1) }
    var hiddenResultComponents by remember { mutableStateOf(emptySet<Int>()) }
    var hiddenResultScenarios by remember { mutableStateOf(emptySet<Int>()) }
    var tableMode by remember { mutableStateOf("modified") } // "modified" or "original"

    var noticeMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    FinsimTheme {
        Scaffold(
            topBar = {
                // Top App Bar with safe status bar padding for camera notches
                Surface(
                    color = SurfaceWarm,
                    shadowElevation = 1.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage),
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
                                    color = BrandForest,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = ".",
                                    color = BrandChartreuse,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = BrandSageLight,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Simulador",
                                        color = BrandSage,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Active currency chip with Radiant Chartreuse background
                            Surface(
                                color = BrandChartreuse,
                                shape = RoundedCornerShape(999.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BrandForest.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = if (selectedTab == 3) "OFERTAS" else "${workspace.scenario.currency.name} $",
                                    color = BrandForest,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = SurfaceWarm.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(64.dp)
                    ) {
                        val navItems = listOf(
                            Triple(0, "Simulador", "🧮"),
                            Triple(1, "Conversor", "📈"),
                            Triple(2, "Conciliar", "🔍"),
                            Triple(3, "Ofertas", "📊")
                        )
                        navItems.forEach { (tabIndex, label, icon) ->
                            val isSelected = selectedTab == tabIndex
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    FinsimLog.ui.d { "Barra inferior navegación: $selectedTab -> $tabIndex" }
                                    selectedTab = tabIndex
                                },
                                icon = { Text(icon, fontSize = 18.sp) },
                                label = {
                                    Text(
                                        label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BrandForest,
                                    selectedTextColor = BrandForest,
                                    indicatorColor = BrandSageLight,
                                    unselectedIconColor = TextForestMuted,
                                    unselectedTextColor = TextForestMuted
                                )
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
                    .background(BackgroundWarm)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Navigation Segmented Pills
                Surface(
                    color = Color(0xFFEBEFE5),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSage),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf(
                            "Préstamo",
                            "Conversor",
                            "Conciliación",
                            "Ofertas"
                        )
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Surface(
                                color = if (isSelected) SurfaceWarm else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = if (isSelected) 1.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .selectable(selected = isSelected, role = Role.Tab) {
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
                                        color = if (isSelected) BrandForest else TextForestMuted
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedTab != 3) {
                // Scenario Toolbar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage)
                ) {
                    val handleSave = {
                        val outcome = store.save()
                        workspace = store.state
                        when (outcome) {
                            is WorkspaceOutcome.Success -> {
                                noticeMessage = outcome.message
                                errorMessage = ""
                            }
                            is WorkspaceOutcome.Failure -> {
                                errorMessage = outcome.error
                            }
                        }
                    }

                    val handleResetExample = {
                        val outcome = store.resetToExample()
                        workspace = store.state
                        noticeMessage = outcome.message
                        errorMessage = ""
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isCompact = maxWidth < 600.dp
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (isCompact) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "ESCENARIO ACTIVO",
                                                fontSize = 10.sp,
                                                color = TextForestMuted,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = workspace.scenario.name,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandDark
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = BrandSageLight.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSage)
                                        ) {
                                            Text(
                                                text = "${workspace.scenario.currency.name} · ${workspace.scenario.termMonths}m",
                                                color = BrandForest,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ScenarioActionButtons(
                                            enabled = !workspace.hasPendingDraft,
                                            onSave = handleSave,
                                            onResetExample = handleResetExample,
                                            onImport = { showImportDialog = true }
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ESCENARIO ACTIVO",
                                            fontSize = 10.sp,
                                            color = TextForestMuted,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = workspace.scenario.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandDark
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ScenarioActionButtons(
                                            enabled = !workspace.hasPendingDraft,
                                            onSave = handleSave,
                                            onResetExample = handleResetExample,
                                            onImport = { showImportDialog = true }
                                        )
                                    }
                                }
                            }

                        ComparisonFileActions(
                            fileName = "escenario-finsim.json",
                            exportJson = { store.exportJson() },
                            importJson = { text ->
                                val outcome = if (workspace.hasPendingDraft) {
                                    WorkspaceOutcome.Failure("Aplica o descarta los eventos pendientes antes de importar.")
                                } else store.importJson(text)
                                workspace = store.state
                                when (outcome) {
                                    is WorkspaceOutcome.Success -> {
                                        selectedMonth = 1
                                        errorMessage = ""
                                    }
                                    is WorkspaceOutcome.Failure -> errorMessage = outcome.error
                                }
                                outcome.isSuccess
                            },
                            onMessage = { noticeMessage = it }
                        )

                        if (workspace.hasPendingDraft) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = BrandAmberLight,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE8D4A2)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Hay cambios de eventos pendientes. Aplica o descarta el lote antes de guardar.",
                                    color = BrandAmberText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (noticeMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = BrandSageLight,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFC7D9BA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = noticeMessage,
                                    color = BrandSage,
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
            }

                }
                // Tab Content
                when (selectedTab) {
                    0 -> {
                        // Loan Summary Cards
                        LoanSummary(
                            currency = workspace.scenario.currency,
                            result = workspace.modified,
                            original = workspace.original
                        )

                        // Applied results share selection with the event draft and table.
                        HorizontalComparisonChart(
                            currency = workspace.scenario.currency,
                            original = workspace.original,
                            modified = workspace.modified,
                            hiddenComponents = hiddenResultComponents,
                            hiddenScenarios = hiddenResultScenarios,
                            onHiddenComponentsChange = { hiddenResultComponents = it },
                            onHiddenScenariosChange = { hiddenResultScenarios = it },
                            selectedMonth = selectedMonth,
                            onSelectMonth = { selectedMonth = it }
                        )

                        // Event Editor
                        EventEditor(
                            currency = workspace.scenario.currency,
                            selectedMonth = selectedMonth,
                            batch = eventDraft,
                            onBatchChange = { eventDraft = it },
                            onApplyEvents = { newEvents ->
                                val outcome = store.applyEvents(newEvents)
                                workspace = store.state
                                when (outcome) {
                                    is WorkspaceOutcome.Success -> {
                                        noticeMessage = outcome.message
                                        errorMessage = ""
                                        true
                                    }
                                    is WorkspaceOutcome.Failure -> {
                                        errorMessage = outcome.error
                                        false
                                    }
                                }
                            },
                            onPendingChange = { pending ->
                                store.setDraftPending(pending)
                                workspace = store.state
                            }
                        )

                        // Loan Conditions Form
                        LoanForm(
                            scenario = workspace.scenario,
                            onApplyScenario = { newScen ->
                                val outcome = store.updateConditions(newScen)
                                workspace = store.state
                                when (outcome) {
                                    is WorkspaceOutcome.Success -> {
                                        noticeMessage = outcome.message
                                        errorMessage = ""
                                        true
                                    }
                                    is WorkspaceOutcome.Failure -> {
                                        errorMessage = outcome.error
                                        false
                                    }
                                }
                            }
                        )

                        // Monthly Table Mode Toggle and Table
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Cronograma a consultar:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextForestMuted
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = tableMode == "modified",
                                        onClick = { tableMode = "modified" },
                                        label = { Text("Modificado", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandForest,
                                            selectedLabelColor = Color.White
                                        ),
                                        border = null
                                    )
                                    FilterChip(
                                        selected = tableMode == "original",
                                        onClick = { tableMode = "original" },
                                        label = { Text("Original (base)", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandForest,
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
                            scenarioName = if (tableMode == "modified") "Modificado" else "Original",
                            selectedMonth = selectedMonth,
                            onSelectMonth = { selectedMonth = it }
                        )
                    }

                    1 -> {
                        RateConverterView()
                    }

                    3 -> { ComparisonScreen(comparisonStore) }
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
                            text = "Un espacio para explorar, no una promesa bancaria. Modelo educativo mensual. Los abonos extraordinarios se aplican tras el pago habitual (seguros · intereses · capital).",
                            fontSize = 11.sp,
                            color = TextForestMuted,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Los escenarios se almacenan localmente en este dispositivo. Exporta copias JSON para archivarlos o transferirlos.",
                            fontSize = 11.sp,
                            color = TextForestMuted
                        )
                    }
                }

                // Extra breathing room at bottom for navigation bar / gesture notch
                Spacer(modifier = Modifier.navigationBarsPadding().height(24.dp))
            }
        }

        if (showImportDialog) {
            DocumentImportDialog(
                onDismiss = { showImportDialog = false },
                onScenarioImported = { importedScenario ->
                    val outcome = store.updateConditions(importedScenario)
                    workspace = store.state
                    when (outcome) {
                        is WorkspaceOutcome.Success -> {
                            noticeMessage = "Escenario importado: '${importedScenario.name}'."
                            errorMessage = ""
                        }
                        is WorkspaceOutcome.Failure -> {
                            errorMessage = outcome.error
                        }
                    }
                    showImportDialog = false
                }
            )
        }
    }
}

@Composable
private fun ScenarioActionButtons(
    enabled: Boolean,
    onSave: () -> Unit,
    onResetExample: () -> Unit,
    onImport: () -> Unit
) {
    Button(
        onClick = onSave,
        enabled = enabled,
        shape = RoundedCornerShape(9.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandForest),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text("Guardar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
    }

    Button(
        onClick = onResetExample,
        enabled = enabled,
        shape = RoundedCornerShape(9.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandLime,
            contentColor = BrandForest
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text("Ejemplo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandForest, maxLines = 1)
    }

    OutlinedButton(
        onClick = onImport,
        enabled = enabled,
        shape = RoundedCornerShape(9.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandForest),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text("Importar extracto", fontSize = 12.sp, color = BrandForest, maxLines = 1)
    }
}
