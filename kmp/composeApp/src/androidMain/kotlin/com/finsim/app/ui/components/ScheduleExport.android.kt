package com.finsim.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.finsim.domain.LoanResult

@Composable
actual fun rememberScheduleDownloader(): ScheduleDownloader {
    val context = LocalContext.current
    var pendingCsv by remember { mutableStateOf<String?>(null) }
    var currentMessage by remember { mutableStateOf<(String) -> Unit>({}) }

    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val text = pendingCsv
        pendingCsv = null
        if (uri != null && text != null) {
            try {
                val stream = context.contentResolver.openOutputStream(uri) ?: error("No se pudo abrir el archivo.")
                stream.use { it.write(text.encodeToByteArray()) }
                currentMessage("Cronograma CSV exportado.")
            } catch (e: Exception) {
                currentMessage("No se pudo exportar: " + e.message)
            }
        }
    }

    return remember {
        object : ScheduleDownloader {
            override fun downloadCsv(fileName: String, csvContent: String, onMessage: (String) -> Unit) {
                pendingCsv = csvContent
                currentMessage = onMessage
                createDoc.launch(fileName)
            }

            override fun downloadArrow(fileName: String, result: LoanResult, onMessage: (String) -> Unit) {
                // In Android without Native Arrow engine, export as CSV with notice
                pendingCsv = generateScheduleCsv(result, "prestamo")
                currentMessage = onMessage
                onMessage("Exportando cronograma estructurado.")
                createDoc.launch(fileName.replace(".arrow", ".csv"))
            }
        }
    }
}
