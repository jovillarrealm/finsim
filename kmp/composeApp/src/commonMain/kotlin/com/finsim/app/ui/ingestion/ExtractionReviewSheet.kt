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
    targetCurrency: Currency? = null,
    onDismiss: () -> Unit,
    onConfirmImport: (Scenario) -> Unit = {},
    onConfirmStatementImport: ((StatementRow) -> Unit)? = null
) {
    var selectedCurrency by remember(targetCurrency, extractedData.detectedCurrencies, framework.defaultCurrency) {
        mutableStateOf(targetCurrency ?: extractedData.detectedCurrencies.singleOrNull() ?: framework.defaultCurrency)
    }
    var useOriginalLoan by remember {
        mutableStateOf(
            !(extractedData.balanceMeaning == BalanceMeaning.PRINCIPAL &&
                (extractedData.capitalBalance != null || extractedData.closingBalance != null)) &&
                extractedData.originalPrincipal != null
        )
    }
    var loanTypeConfirmed by remember {
        mutableStateOf(extractedData.documentKind == ImportDocumentKind.LOAN_STATEMENT)
    }
    var reviewConfirmed by remember { mutableStateOf(false) }
    var currencyAssumptionConfirmed by remember { mutableStateOf(false) }
    var noInsuranceConfirmed by remember { mutableStateOf(false) }
    var calculatedPaymentConfirmed by remember { mutableStateOf(false) }
    var balanceIsPrincipalConfirmed by remember {
        mutableStateOf(extractedData.balanceMeaning == BalanceMeaning.PRINCIPAL)
    }

    var loanName by remember {
        mutableStateOf(
            extractedData.bankNameCandidate?.let { "$it · Préstamo" }
                ?: "Extracto (${framework.label})"
        )
    }

    var principal by remember {
        mutableStateOf(
            if (useOriginalLoan) (extractedData.originalPrincipal ?: extractedData.unclassifiedPrincipal).orEmpty()
            else if (extractedData.balanceMeaning == BalanceMeaning.PRINCIPAL) {
                extractedData.capitalBalance ?: extractedData.closingBalance.orEmpty()
            } else ""
        )
    }

    var termMonths by remember {
        mutableStateOf(
            (if (useOriginalLoan) extractedData.originalTermMonths
                ?: extractedData.termMonths.takeIf { extractedData.termMeaning == TermMeaning.ORIGINAL }
            else extractedData.remainingTermMonths
                ?: extractedData.termMonths.takeIf { extractedData.termMeaning == TermMeaning.REMAINING })
                ?.toString().orEmpty()
        )
    }

    var startDate by remember {
        mutableStateOf("")
    }

    var rateKind by remember {
        mutableStateOf(extractedData.rate?.kind?.takeIf { it in setOf("annual", "monthly", "nominal") }.orEmpty())
    }

    var ratePercent by remember {
        mutableStateOf(extractedData.rate?.valuePercent.orEmpty())
    }

    var nominalFreq by remember {
        mutableStateOf(extractedData.rate?.nominalFrequency?.toString().orEmpty())
    }

    var payment by remember {
        mutableStateOf(
            extractedData.basePayment
                ?: extractedData.monthlyPayment.takeIf { extractedData.paymentMeaning == PaymentMeaning.BASE_PAYMENT }
                ?: ""
        )
    }

    // Editable list of insurance candidates with explicit typing
    var insurances by remember {
        val chargeCandidates = extractedData.insurances.filter {
            it.amountMeaning == InsuranceAmountMeaning.PERIODIC_CHARGE && it.monthlyCost != null
        }
        val initialList: List<Insurance> = if (chargeCandidates.isNotEmpty()) {
            chargeCandidates.mapIndexed { idx, ins ->
                Insurance(
                    id = "ins_${idx + 1}",
                    name = ins.name,
                    kind = "fixed",
                    value = ins.monthlyCost!!,
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
        mutableStateOf(extractedData.statementMonth?.toString().orEmpty())
    }
    var principalPaid by remember {
        mutableStateOf(extractedData.principalPaid ?: "")
    }
    var interestCharged by remember {
        mutableStateOf(extractedData.interestCharged ?: "")
    }
    var insuranceCharged by remember {
        mutableStateOf(extractedData.totalInsurance.orEmpty())
    }
    var balanceResult by remember {
        mutableStateOf(
            if (extractedData.balanceMeaning == BalanceMeaning.PRINCIPAL) {
                extractedData.capitalBalance ?: extractedData.closingBalance.orEmpty()
            } else ""
        )
    }

    val reviewedDocument = extractedData.withLoanStatementConfirmation(loanTypeConfirmed)
    val importBlockReason = when {
        targetMode == ImportTargetMode.RECONCILIATION && targetCurrency == null ->
            "No se pudo identificar la moneda del escenario activo."
        else -> reviewedDocument.importBlockReason(
            expectedCurrency = targetCurrency.takeIf { targetMode == ImportTargetMode.RECONCILIATION },
            confirmCurrencyAssumption = currencyAssumptionConfirmed
        )
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Marco: ${framework.label} · Pág. ${extractedData.analyzedPage}",
                            fontSize = 11.sp,
                            color = TextForestMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Datos por revisar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextForestMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = BackgroundWarm,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "DATOS ORIGINALES DETECTADOS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandForest,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text("Documento: ${extractedData.documentKind}", fontSize = 11.sp, color = TextForestMuted)
                        Text(
                            "Moneda original: ${extractedData.detectedCurrencies.takeIf { it.isNotEmpty() }?.joinToString { it.name } ?: "No informada"}",
                            fontSize = 11.sp,
                            color = TextForestMuted
                        )
                        Text("Capital original: ${extractedData.originalPrincipal ?: "No informado"}", fontSize = 11.sp, color = TextForestMuted)
                        Text("Capital detectado sin sentido confirmado: ${extractedData.unclassifiedPrincipal ?: "No informado"}", fontSize = 11.sp, color = TextForestMuted)
                        Text("Saldo de capital: ${extractedData.capitalBalance ?: "No informado"}", fontSize = 11.sp, color = TextForestMuted)
                        Text("Saldo compuesto: ${extractedData.compoundBalance ?: "No informado"}", fontSize = 11.sp, color = TextForestMuted)
                        if (extractedData.closingBalance != null && extractedData.capitalBalance == null && extractedData.compoundBalance == null) {
                            Text("Saldo sin sentido confirmado: ${extractedData.closingBalance}", fontSize = 11.sp, color = TextForestMuted)
                        }
                        Text("Plazo original: ${extractedData.originalTermMonths?.toString() ?: "No informado"} meses", fontSize = 11.sp, color = TextForestMuted)
                        Text("Plazo restante: ${extractedData.remainingTermMonths?.toString() ?: "No informado"} meses", fontSize = 11.sp, color = TextForestMuted)
                        if (extractedData.termMonths != null && extractedData.termMeaning == TermMeaning.UNKNOWN) {
                            Text("Plazo detectado sin sentido confirmado: ${extractedData.termMonths} meses", fontSize = 11.sp, color = TextForestMuted)
                        }
                        Text("Cuota base: ${extractedData.basePayment ?: "No informada"}", fontSize = 11.sp, color = TextForestMuted)
                        Text("Cuota total: ${extractedData.totalPayment ?: "No informada"}", fontSize = 11.sp, color = TextForestMuted)
                        if (extractedData.monthlyPayment != null && extractedData.paymentMeaning == PaymentMeaning.UNKNOWN) {
                            Text("Cuota detectada sin sentido confirmado: ${extractedData.monthlyPayment}", fontSize = 11.sp, color = TextForestMuted)
                        }
                        Text(
                            "Tasa: ${extractedData.rate?.let { "${it.valuePercent}% · ${it.kind}" } ?: "No informada"}",
                            fontSize = 11.sp,
                            color = TextForestMuted
                        )
                        Text("Fecha de corte: ${extractedData.statementCutDate ?: "No informada"}", fontSize = 11.sp, color = TextForestMuted)
                        Text("Mes de extracto: ${extractedData.statementMonth?.toString() ?: "No informado"}", fontSize = 11.sp, color = TextForestMuted)
                        Text("Capital pagado: ${extractedData.principalPaid ?: "No informado"}", fontSize = 11.sp, color = TextForestMuted)
                        Text("Interés cobrado: ${extractedData.interestCharged ?: "No informado"}", fontSize = 11.sp, color = TextForestMuted)
                        Text("Seguro cobrado: ${extractedData.totalInsurance ?: "No informado"}", fontSize = 11.sp, color = TextForestMuted)
                        if (extractedData.insurances.isEmpty()) {
                            Text("Seguros detectados: No informados", fontSize = 11.sp, color = TextForestMuted)
                        } else {
                            extractedData.insurances.forEach { insurance ->
                                Text(
                                    "Seguro ${insurance.name.ifBlank { "Sin nombre informado" }}: ${insurance.monthlyCost ?: "No informado"} · ${insurance.amountMeaning}",
                                    fontSize = 11.sp,
                                    color = TextForestMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Los datos originales se conservan aquí; los campos siguientes son correcciones revisadas.",
                            fontSize = 11.sp,
                            color = BrandForest,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (extractedData.documentKind == ImportDocumentKind.UNKNOWN) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = loanTypeConfirmed, onCheckedChange = {
                            loanTypeConfirmed = it
                            reviewConfirmed = false
                        })
                        Text("Confirmo que el original es un extracto de préstamo", fontSize = 12.sp, color = TextForestMuted)
                    }
                }

                if (importBlockReason != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(
                            text = importBlockReason,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(
                        if (targetMode == ImportTargetMode.RECONCILIATION) "Moneda del escenario activo"
                        else "Moneda que se usará",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandDark
                    )
                    if (targetMode == ImportTargetMode.RECONCILIATION) {
                        Text(targetCurrency?.name ?: "No disponible", fontSize = 12.sp, color = TextForestMuted)
                        if (extractedData.detectedCurrencies.isEmpty() && targetCurrency != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = currencyAssumptionConfirmed, onCheckedChange = {
                                    currencyAssumptionConfirmed = it
                                    reviewConfirmed = false
                                })
                                Text(
                                    "Confirmo que los importes están en ${targetCurrency.name}",
                                    fontSize = 12.sp,
                                    color = TextForestMuted
                                )
                            }
                        }
                    } else {
                        Currency.entries.forEach { currency ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedCurrency = currency
                                    if (currency != extractedData.detectedCurrencies.singleOrNull()) {
                                        currencyAssumptionConfirmed = false
                                    }
                                    reviewConfirmed = false
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedCurrency == currency,
                                    onClick = {
                                        selectedCurrency = currency
                                        if (currency != extractedData.detectedCurrencies.singleOrNull()) {
                                            currencyAssumptionConfirmed = false
                                        }
                                        reviewConfirmed = false
                                    }
                                )
                                Text(currency.name, fontSize = 12.sp, color = TextForestMuted)
                            }
                        }
                        if (extractedData.detectedCurrencies.singleOrNull() != selectedCurrency) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = currencyAssumptionConfirmed, onCheckedChange = {
                                    currencyAssumptionConfirmed = it
                                    reviewConfirmed = false
                                })
                                Text("Confirmo esta moneda como corrección o supuesto", fontSize = 12.sp, color = TextForestMuted)
                            }
                        }
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
                    text = if (targetMode == ImportTargetMode.RECONCILIATION) "CORRECCIONES PARA CONCILIAR" else "CORRECCIONES PARA EL ESCENARIO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandForest,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (targetMode == ImportTargetMode.RECONCILIATION) {
                    OutlinedTextField(
                        value = auditMonth,
                        onValueChange = { auditMonth = it; reviewConfirmed = false },
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
                            onValueChange = { principalPaid = it; reviewConfirmed = false },
                            label = { Text("Capital pagado (${selectedCurrency.name})") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = interestCharged,
                            onValueChange = { interestCharged = it; reviewConfirmed = false },
                            label = { Text("Interés cobrado (${selectedCurrency.name})") },
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
                            onValueChange = { insuranceCharged = it; reviewConfirmed = false },
                            label = { Text("Seguro cobrado (${selectedCurrency.name})") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = balanceResult,
                            onValueChange = { balanceResult = it; reviewConfirmed = false },
                            label = { Text("Saldo final (${selectedCurrency.name})") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = balanceIsPrincipalConfirmed, onCheckedChange = {
                            balanceIsPrincipalConfirmed = it
                            reviewConfirmed = false
                        })
                        Text("Confirmo que el saldo final es de capital, no deuda compuesta", fontSize = 11.sp, color = TextForestMuted)
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
                                        text = "• ${ins.name.ifBlank { "Sin nombre informado" }}: ${ins.monthlyCost ?: "No informado"} · ${ins.amountMeaning}",
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
                        onValueChange = { loanName = it; reviewConfirmed = false },
                        label = { Text("Nombre del préstamo") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Capital y plazo que se aplicarán", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandDark)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !useOriginalLoan,
                            onClick = {
                                useOriginalLoan = false
                                principal = if (extractedData.balanceMeaning == BalanceMeaning.PRINCIPAL) {
                                    extractedData.capitalBalance ?: extractedData.closingBalance.orEmpty()
                                } else extractedData.capitalBalance ?: extractedData.unclassifiedPrincipal.orEmpty()
                                termMonths = (extractedData.remainingTermMonths
                                    ?: extractedData.termMonths.takeIf { extractedData.termMeaning == TermMeaning.REMAINING })
                                    ?.toString().orEmpty()
                                reviewConfirmed = false
                            }
                        )
                        Text("Saldo actual y plazo restante", fontSize = 12.sp, color = TextForestMuted)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = useOriginalLoan,
                            onClick = {
                                useOriginalLoan = true
                                principal = (extractedData.originalPrincipal ?: extractedData.unclassifiedPrincipal).orEmpty()
                                termMonths = (extractedData.originalTermMonths
                                    ?: extractedData.termMonths.takeIf { extractedData.termMeaning == TermMeaning.ORIGINAL })
                                    ?.toString().orEmpty()
                                reviewConfirmed = false
                            }
                        )
                        Text("Capital original y plazo original", fontSize = 12.sp, color = TextForestMuted)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = principal,
                            onValueChange = { principal = it; reviewConfirmed = false },
                            label = { Text("${if (useOriginalLoan) "Capital original" else "Saldo de capital actual"} (${selectedCurrency.name})") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = termMonths,
                            onValueChange = { termMonths = it; reviewConfirmed = false },
                            label = { Text("${if (useOriginalLoan) "Plazo original" else "Plazo restante"} (meses)") },
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
                            onValueChange = { ratePercent = it; reviewConfirmed = false },
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
                                    "nominal" -> "Nominal anual"
                                    else -> "Selecciona convención"
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
                                    onClick = { rateKind = "annual"; rateExpanded = false; reviewConfirmed = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Efectiva mensual (EM)") },
                                    onClick = { rateKind = "monthly"; rateExpanded = false; reviewConfirmed = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Nominal anual") },
                                    onClick = { rateKind = "nominal"; rateExpanded = false; reviewConfirmed = false }
                                )
                            }
                        }
                    }

                    if (rateKind == "nominal") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = nominalFreq,
                            onValueChange = { nominalFreq = it; reviewConfirmed = false },
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
                            onValueChange = { startDate = it; reviewConfirmed = false },
                            label = { Text("Primer periodo del escenario (AAAA-MM)") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = payment,
                            onValueChange = { payment = it; reviewConfirmed = false },
                            label = { Text("Cuota base revisada (${selectedCurrency.name})") },
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
                                    value = "",
                                    endsAtPayoff = true
                                )
                                insurances = insurances + listOf(newIns)
                                noInsuranceConfirmed = false
                                reviewConfirmed = false
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("+ Añadir seguro", fontSize = 11.sp, color = BrandSage, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (insurances.isEmpty()) {
                        Text(
                            text = "No hay un importe revisado de cargo periódico de seguro.",
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
                                        reviewConfirmed = false
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
                                        reviewConfirmed = false
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
                                        if (list.isEmpty()) noInsuranceConfirmed = false
                                        reviewConfirmed = false
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("✕", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (insurances.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = noInsuranceConfirmed, onCheckedChange = {
                                noInsuranceConfirmed = it
                                reviewConfirmed = false
                            })
                            Text("Confirmo que no aplicaré cargos periódicos de seguro", fontSize = 11.sp, color = TextForestMuted)
                        }
                    }

                    if (payment.isBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = calculatedPaymentConfirmed, onCheckedChange = {
                                calculatedPaymentConfirmed = it
                                reviewConfirmed = false
                            })
                            Text("Confirmo que la cuota base se calculará con estos datos", fontSize = 11.sp, color = TextForestMuted)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = reviewConfirmed, onCheckedChange = { reviewConfirmed = it })
                    Text("Revisé los datos originales, las correcciones y los supuestos antes de importar", fontSize = 12.sp, color = BrandDark)
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
                        enabled = reviewConfirmed && importBlockReason == null &&
                            (targetMode != ImportTargetMode.RECONCILIATION || onConfirmStatementImport != null),
                        onClick = {
                            if (targetMode == ImportTargetMode.RECONCILIATION) {
                                try {
                                    val m = auditMonth.trim().toIntOrNull()?.takeIf { it in 1..1200 }
                                        ?: error("Indica un mes a auditar válido.")
                                    val reconciliationCurrency = targetCurrency
                                        ?: error("No se pudo identificar la moneda del escenario activo.")
                                    val pClean = principalPaid.trim().replace(',', '.').ifBlank { null }?.let {
                                        parseImportMoney(it, "El capital pagado").toFixed(2)
                                    }
                                    val iClean = interestCharged.trim().replace(',', '.').ifBlank { null }?.let {
                                        parseImportMoney(it, "El interés cobrado").toFixed(2)
                                    }
                                    val insClean = insuranceCharged.trim().replace(',', '.').ifBlank { null }?.let {
                                        parseImportMoney(it, "El seguro cobrado").toFixed(2)
                                    }
                                    val bClean = balanceResult.trim().replace(',', '.').ifBlank { null }?.let {
                                        parseImportMoney(it, "El saldo final").toFixed(2)
                                    }
                                    require(bClean == null || balanceIsPrincipalConfirmed) {
                                        "Confirma que el saldo final corresponde al capital y no a deuda compuesta."
                                    }
                                    val reviewedData = reviewedDocument.copy(
                                        statementMonth = m,
                                        principalPaid = pClean,
                                        interestCharged = iClean,
                                        totalInsurance = insClean,
                                        capitalBalance = bClean,
                                        closingBalance = bClean,
                                        balanceMeaning = if (bClean != null) BalanceMeaning.PRINCIPAL else BalanceMeaning.UNKNOWN
                                    )
                                    val stmt = reviewedData.toStatementRow(
                                        expectedCurrency = reconciliationCurrency,
                                        reviewedCurrency = reconciliationCurrency,
                                        confirmCurrencyAssumption = currencyAssumptionConfirmed
                                    )
                                    onConfirmStatementImport?.invoke(stmt)
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Verifica los datos del extracto."
                                }
                            } else {
                                try {
                                    require(rateKind in setOf("annual", "monthly", "nominal")) {
                                        "Selecciona la convención de la tasa."
                                    }
                                    val nominalFrequency = if (rateKind == "nominal") {
                                        nominalFreq.trim().toIntOrNull()?.takeIf { it > 0 }
                                            ?: error("Indica la frecuencia de capitalización nominal.")
                                    } else null
                                    val reviewedInsurance = insurances.map { item ->
                                        val amount = parseImportMoney(item.value.trim(), "El cargo periódico de seguro").toFixed(2)
                                        ExtractedInsurance(
                                            name = item.name.trim().ifBlank { error("Indica el nombre de cada seguro.") },
                                            monthlyCost = amount,
                                            amountMeaning = InsuranceAmountMeaning.PERIODIC_CHARGE
                                        )
                                    }
                                    val reviewedTerm = termMonths.trim().toIntOrNull()
                                        ?: error("Indica el plazo que corresponda al capital seleccionado.")
                                    val reviewedData = reviewedDocument.copy(
                                        capitalBalance = if (useOriginalLoan) null else principal.trim(),
                                        closingBalance = if (useOriginalLoan) null else principal.trim(),
                                        originalPrincipal = if (useOriginalLoan) principal.trim() else extractedData.originalPrincipal,
                                        balanceMeaning = if (useOriginalLoan) BalanceMeaning.UNKNOWN else BalanceMeaning.PRINCIPAL,
                                        termMonths = reviewedTerm,
                                        originalTermMonths = if (useOriginalLoan) reviewedTerm else extractedData.originalTermMonths,
                                        remainingTermMonths = if (useOriginalLoan) extractedData.remainingTermMonths else reviewedTerm,
                                        termMeaning = if (useOriginalLoan) TermMeaning.ORIGINAL else TermMeaning.REMAINING,
                                        rate = ExtractedRate(
                                            valuePercent = ratePercent.trim(),
                                            kind = rateKind,
                                            nominalFrequency = nominalFrequency
                                        ),
                                        basePayment = payment.trim().ifBlank { null },
                                        monthlyPayment = payment.trim().ifBlank { null },
                                        paymentMeaning = if (payment.isBlank()) PaymentMeaning.UNKNOWN else PaymentMeaning.BASE_PAYMENT,
                                        insurances = reviewedInsurance
                                    )
                                    val scenario = reviewedData.toScenario(
                                        framework = framework,
                                        customName = loanName.trim().ifBlank { null },
                                        startDate = startDate.trim().ifBlank { null },
                                        confirmNoInsurance = insurances.isEmpty() && noInsuranceConfirmed,
                                        confirmCalculatedPayment = payment.isBlank() && calculatedPaymentConfirmed,
                                        confirmCurrencyAssumption = currencyAssumptionConfirmed,
                                        reviewedCurrency = selectedCurrency
                                    )
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
