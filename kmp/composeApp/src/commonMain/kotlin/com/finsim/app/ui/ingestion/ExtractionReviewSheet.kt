package com.finsim.app.ui.ingestion

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
import com.finsim.core.Decimal
import com.finsim.domain.*
import com.finsim.ingestion.*
import com.finsim.reconciliation.StatementRow

/**
 * Interactive review sheet / modal where users review and adjust
 * fields extracted via Tier 1 digital text before confirming import into the active scenario or reconciliation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractionReviewSheet(
    extractedData: ExtractedDocumentData,
    framework: RegulatoryFramework,
    targetMode: ImportTargetMode = ImportTargetMode.SCENARIO,
    onDismiss: () -> Unit,
    onConfirmImport: (Scenario) -> Unit = {},
    onConfirmStatementImport: ((StatementRow) -> Unit)? = null
) {
    var loanName by remember {
        mutableStateOf(
            extractedData.bankNameCandidate?.let { "$it · Préstamo" }
                ?: "Extracto (${framework.label})"
        )
    }

    var principal by remember {
        mutableStateOf(
            extractedData.closingBalance
                ?: extractedData.originalPrincipal
                ?: "20000000.00"
        )
    }

    var termMonths by remember {
        mutableStateOf((extractedData.termMonths ?: 36).toString())
    }

    var startDate by remember {
        mutableStateOf(extractedData.statementCutDate ?: "2026-10")
    }

    var rateKind by remember {
        mutableStateOf(extractedData.rate?.kind ?: framework.defaultRateKind)
    }

    var ratePercent by remember {
        mutableStateOf(extractedData.rate?.valuePercent ?: "18.5")
    }

    var nominalFreq by remember {
        mutableStateOf((extractedData.rate?.nominalFrequency ?: 12).toString())
    }

    var payment by remember {
        mutableStateOf(extractedData.monthlyPayment ?: "")
    }

    // Editable list of insurance candidates with explicit typing
    var insurances by remember {
        val initialList: List<Insurance> = if (extractedData.insurances.isNotEmpty()) {
            extractedData.insurances.mapIndexed { idx, ins ->
                Insurance(
                    id = "ins_${idx + 1}",
                    name = ins.name,
                    kind = "fixed",
                    value = ins.monthlyCost,
                    endsAtPayoff = true
                )
            }
        } else {
            emptyList()
        }
        mutableStateOf(initialList)
    }

    // Reconciliation mode states
    var auditMonth by remember {
        mutableStateOf((extractedData.statementMonth ?: 1).toString())
    }
    var principalPaid by remember {
        mutableStateOf(extractedData.principalPaid ?: "")
    }
    var interestCharged by remember {
        mutableStateOf(extractedData.interestCharged ?: "")
    }
    var insuranceCharged by remember {
        mutableStateOf(extractedData.totalInsurance ?: extractedData.toStatementRow().insurance ?: "")
    }
    var balanceResult by remember {
        mutableStateOf(extractedData.closingBalance ?: "")
    }

    var errorMessage by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceWarm,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage),
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
                // Header & Engine Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (targetMode == ImportTargetMode.RECONCILIATION) "Auditoría de extracto" else "Revisión de extracción",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandDark
                        )
                        Text(
                            text = if (targetMode == ImportTargetMode.RECONCILIATION)
                                "Verifica los cobros efectivos del extracto para auditar."
                            else
                                "Verifica y edita las condiciones detectadas antes de importar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextForestMuted
                        )
                    }

                    // Engine Badge
                    val isVlm = extractedData.engine == ExtractionEngine.TIER_2_LOCAL_VLM
                    Surface(
                        color = if (isVlm) BrandLime else BrandSageLight,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isVlm) BrandForest else BrandSage)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isVlm) "⚡ Visión local Gemma 4" else "⚡ Texto digital",
                                color = if (isVlm) BrandForest else BrandSage,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata chip
                Surface(
                    color = BackgroundWarm,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Marco: ${framework.label} · Pág. ${extractedData.analyzedPage}",
                            fontSize = 11.sp,
                            color = TextForestMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Confianza: ${(extractedData.confidence * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandForest
                        )
                    }
                }

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

                Spacer(modifier = Modifier.height(14.dp))

                // Editable Fields
                Text(
                    text = if (targetMode == ImportTargetMode.RECONCILIATION) "COBROS DEL PERÍODO" else "CONDICIONES DETECTADAS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandForest,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (targetMode == ImportTargetMode.RECONCILIATION) {
                    OutlinedTextField(
                        value = auditMonth,
                        onValueChange = { auditMonth = it },
                        label = { Text("Mes a auditar") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = principalPaid,
                            onValueChange = { principalPaid = it },
                            label = { Text("Capital pagado (${framework.defaultCurrency.name})") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = interestCharged,
                            onValueChange = { interestCharged = it },
                            label = { Text("Interés cobrado (${framework.defaultCurrency.name})") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = insuranceCharged,
                            onValueChange = { insuranceCharged = it },
                            label = { Text("Seguro cobrado (${framework.defaultCurrency.name})") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = balanceResult,
                            onValueChange = { balanceResult = it },
                            label = { Text("Saldo final (${framework.defaultCurrency.name})") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (extractedData.insurances.isNotEmpty()) {
                        Surface(
                            color = BackgroundWarm,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Desglose de seguros detectados:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandDark
                                )
                                extractedData.insurances.forEach { ins ->
                                    Text(
                                        text = "• ${ins.name}: ${ins.monthlyCost} ${framework.defaultCurrency.name}",
                                        fontSize = 11.sp,
                                        color = TextForestMuted
                                    )
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = loanName,
                        onValueChange = { loanName = it },
                        label = { Text("Nombre del préstamo") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = principal,
                            onValueChange = { principal = it },
                            label = { Text("Capital / Saldo (${framework.defaultCurrency.name})") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = termMonths,
                            onValueChange = { termMonths = it },
                            label = { Text("Plazo (meses)") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ratePercent,
                            onValueChange = { ratePercent = it },
                            label = { Text("Tasa de interés (%)") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )

                        var rateExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = rateExpanded,
                            onExpandedChange = { rateExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = when (rateKind) {
                                    "monthly" -> "Efectiva mensual (EM)"
                                    "annual" -> "Efectiva anual (EA)"
                                    else -> "Nominal anual"
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tipo de tasa") },
                                shape = RoundedCornerShape(8.dp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rateExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            )
                            ExposedDropdownMenu(
                                expanded = rateExpanded,
                                onDismissRequest = { rateExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Efectiva anual (EA)") },
                                    onClick = { rateKind = "annual"; rateExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Efectiva mensual (EM)") },
                                    onClick = { rateKind = "monthly"; rateExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Nominal anual") },
                                    onClick = { rateKind = "nominal"; rateExpanded = false }
                                )
                            }
                        }
                    }

                    if (rateKind == "nominal") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = nominalFreq,
                            onValueChange = { nominalFreq = it },
                            label = { Text("Capitalizaciones / año") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = { Text("Primer periodo (AAAA-MM)") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = payment,
                            onValueChange = { payment = it },
                            label = { Text("Cuota fija (opcional)") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Insurances section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SEGUROS DETECTADOS (${insurances.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandForest,
                            letterSpacing = 0.5.sp
                        )

                        TextButton(
                            onClick = {
                                val newId = "ins_${insurances.size + 1}"
                                val newIns = Insurance(
                                    id = newId,
                                    name = "Seguro ${insurances.size + 1}",
                                    kind = "fixed",
                                    value = "0.00",
                                    endsAtPayoff = true
                                )
                                insurances = insurances + listOf(newIns)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("+ Añadir seguro", fontSize = 11.sp, color = BrandSage, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (insurances.isEmpty()) {
                        Text(
                            text = "No se detectaron cargos de seguros en el extracto.",
                            fontSize = 12.sp,
                            color = TextForestMuted,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        for ((idx, ins) in insurances.withIndex()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = ins.name,
                                    onValueChange = { newName ->
                                        val list = insurances.toMutableList()
                                        list[idx] = ins.copy(name = newName)
                                        insurances = list
                                    },
                                    label = { Text("Seguro") },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.3f)
                                )
                                OutlinedTextField(
                                    value = ins.value,
                                    onValueChange = { newVal ->
                                        val list = insurances.toMutableList()
                                        list[idx] = ins.copy(value = newVal)
                                        insurances = list
                                    },
                                    label = { Text("Valor") },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        val list = insurances.toMutableList()
                                        list.removeAt(idx)
                                        insurances = list
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("✕", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions: Cancel & Confirm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSage),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Cancelar", color = TextForestMuted, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            if (targetMode == ImportTargetMode.RECONCILIATION) {
                                try {
                                    val m = auditMonth.trim().toIntOrNull() ?: error("Indica un mes a auditar válido.")
                                    val pClean = principalPaid.trim().replace(',', '.').ifBlank { null }?.let {
                                        Decimal.parse(it).toFixed(2)
                                    }
                                    val iClean = interestCharged.trim().replace(',', '.').ifBlank { null }?.let {
                                        Decimal.parse(it).toFixed(2)
                                    }
                                    val insClean = insuranceCharged.trim().replace(',', '.').ifBlank { null }?.let {
                                        Decimal.parse(it).toFixed(2)
                                    }
                                    val bClean = balanceResult.trim().replace(',', '.').ifBlank { null }?.let {
                                        Decimal.parse(it).toFixed(2)
                                    }
                                    val stmt = StatementRow(
                                        month = m,
                                        principal = pClean,
                                        interest = iClean,
                                        insurance = insClean,
                                        balance = bClean
                                    )
                                    stmt.validate()
                                    onConfirmStatementImport?.invoke(stmt)
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Verifica los datos del extracto."
                                }
                            } else {
                                try {
                                    val rateDecValue = (Decimal.parse(ratePercent.replace(',', '.')) / Decimal.parse("100")).toString()
                                    val newRate = when (rateKind) {
                                        "monthly" -> Rate.Monthly(rateDecValue)
                                        "annual" -> Rate.Annual(rateDecValue)
                                        "nominal" -> Rate.Nominal(rateDecValue, nominalFreq.toIntOrNull() ?: 12)
                                        else -> Rate.Annual(rateDecValue)
                                    }

                                    val validStartDate = if (Regex("""^\d{4}-(0[1-9]|1[0-2])$""").matches(startDate.trim())) {
                                        startDate.trim()
                                    } else {
                                        "2026-10"
                                    }

                                    val scenario = Scenario(
                                        version = 1,
                                        currency = framework.defaultCurrency,
                                        name = loanName.trim().ifBlank { "Préstamo importado" },
                                        source = "current",
                                        principal = Decimal.parse(principal.replace(',', '.')).toFixed(2),
                                        startDate = validStartDate,
                                        termMonths = termMonths.trim().toIntOrNull() ?: error("Plazo en meses inválido"),
                                        rate = newRate,
                                        payment = payment.trim().ifBlank { null },
                                        insurance = insurances.map { item ->
                                            item.copy(
                                                value = Decimal.parse(item.value.replace(',', '.')).toFixed(2)
                                            )
                                        },
                                        events = emptyList()
                                    )

                                    scenario.validate()
                                    onConfirmImport(scenario)
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Verifica los datos antes de importar."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandForest, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.5f).height(44.dp)
                    ) {
                        Text("Confirmar e importar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
