package com.finsim.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream

@Composable
actual fun ComparisonFileActions(
    fileName: String,
    exportJson: () -> Result<String>,
    importJson: (String) -> Boolean,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val currentImport by rememberUpdatedState(importJson)
    val currentMessage by rememberUpdatedState(onMessage)
    var pendingExport by remember { mutableStateOf<String?>(null) }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val text = pendingExport
        pendingExport = null
        if (uri != null && text != null) {
            try {
                val stream = context.contentResolver.openOutputStream(uri) ?: error("No se pudo abrir el archivo.")
                stream.use { it.write(text.encodeToByteArray()) }
                currentMessage("Copia JSON exportada.")
            } catch (e: Exception) { currentMessage("No se pudo exportar: " + e.message) }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val stream = context.contentResolver.openInputStream(uri) ?: error("No se pudo leer el archivo.")
                val bytes = stream.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        require(output.size() + count <= 2_000_000) { "El archivo supera 2 MB." }
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                val valid = currentImport(bytes.decodeToString(throwOnInvalidSequence = true))
                currentMessage(if (valid) "Archivo importado; guarda el borrador explícitamente." else "No se importó; revisa el error. Los datos previos se conservaron.")
            } catch (e: Exception) { currentMessage("No se pudo importar: " + e.message) }
        }
    }
    OutlinedButton(onClick = {
        exportJson().fold(onSuccess = { pendingExport = it; create.launch(fileName) },
            onFailure = { currentMessage(it.message ?: "Corrige el borrador antes de exportar.") })
    }) { Text("Exportar archivo JSON") }
    OutlinedButton(onClick = { open.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text("Importar archivo JSON") }
}
