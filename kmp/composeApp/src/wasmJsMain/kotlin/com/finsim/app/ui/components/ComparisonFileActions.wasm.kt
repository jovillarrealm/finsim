package com.finsim.app.ui.components

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlin.js.js

private fun downloadComparison(text: String, onError: (String) -> Unit): Unit = js("""{
    try {
        const link = document.createElement('a');
        const url = URL.createObjectURL(new Blob([text], {type: 'application/json'}));
        link.href = url;
        link.download = 'comparacion-finsim.json';
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (error) { onError('No se pudo exportar el archivo.'); }
}""")

private fun openComparison(consume: (String) -> Unit, onError: (String) -> Unit): Unit = js("""{
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.json,application/json';
    input.style.display = 'none';
    input.onchange = async () => {
        try {
            const file = input.files[0];
            if (!file) return;
            if (file.size > 2000000) { onError('El archivo supera 2 MB.'); return; }
            const text = new TextDecoder('utf-8', {fatal: true}).decode(await file.arrayBuffer());
            consume(text);
        } catch (error) { onError('No se pudo leer el archivo.'); }
        finally { input.remove(); }
    };
    input.oncancel = () => input.remove();
    document.body.appendChild(input);
    input.click();
}""")

@Composable
actual fun ComparisonFileActions(
    exportJson: () -> Result<String>,
    importJson: (String) -> Boolean,
    onMessage: (String) -> Unit
) {
    val currentImport by rememberUpdatedState(importJson)
    val currentMessage by rememberUpdatedState(onMessage)
    OutlinedButton(onClick = {
        exportJson().fold(onSuccess = { downloadComparison(it, currentMessage) },
            onFailure = { currentMessage(it.message ?: "Corrige el borrador antes de exportar.") })
    }) { Text("Exportar archivo JSON") }
    OutlinedButton(onClick = {
        openComparison({ text ->
            val valid = currentImport(text)
            currentMessage(if (valid) "Archivo importado; guarda explícitamente." else "Archivo rechazado; los datos previos se conservaron.")
        }, currentMessage)
    }) { Text("Importar archivo JSON") }
}
