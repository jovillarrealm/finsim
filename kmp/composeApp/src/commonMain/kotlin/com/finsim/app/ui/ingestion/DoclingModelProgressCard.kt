package com.finsim.app.ui.ingestion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finsim.app.theme.BrandDark
import com.finsim.app.theme.BrandForest
import com.finsim.app.theme.BrandLime
import com.finsim.app.theme.BrandSage
import com.finsim.app.theme.BrandSageLight
import com.finsim.app.theme.SurfaceWarm
import com.finsim.app.theme.TextForestMuted
import com.finsim.ingestion.DoclingModelController
import com.finsim.ingestion.DoclingModelState
import com.finsim.ingestion.GRANITE_DOCLING_MODEL_SIZE_BYTES
import kotlinx.coroutines.launch

private fun formatModelMiB(bytes: Long): String = "${(bytes.toDouble() / (1024 * 1024) * 10).toLong() / 10.0} MiB"

@Composable
fun DoclingModelProgressCard(
    controller: DoclingModelController,
    state: DoclingModelState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = BorderStroke(1.dp, BrandSage)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (state) {
                DoclingModelState.Checking -> {
                    Text("Verificando Granite-Docling local…", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandDark)
                    Text(
                        "Se comprueba el tamaño y SHA-256 de cada archivo del modelo antes de habilitar el análisis.",
                        fontSize = 11.sp,
                        color = TextForestMuted
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = BrandForest, trackColor = BrandSageLight)
                }

                DoclingModelState.NotInstalled -> {
                    Text("Modelo local Granite-Docling 258M", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandDark)
                    Text(
                        "Instala ${formatModelMiB(GRANITE_DOCLING_MODEL_SIZE_BYTES)} una sola vez. Solo los archivos verificados del modelo se guardan en el navegador; PDFs, imágenes y su contenido permanecen en memoria.",
                        fontSize = 11.sp,
                        color = TextForestMuted
                    )
                    Button(
                        onClick = controller::startDownload,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandForest, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Instalar modelo local (${formatModelMiB(GRANITE_DOCLING_MODEL_SIZE_BYTES)})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                is DoclingModelState.Downloading -> {
                    Text("Descargando y verificando Granite-Docling…", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandDark)
                    Text(
                        "${formatModelMiB(state.downloadedBytes)} de ${formatModelMiB(state.totalBytes)}",
                        fontSize = 11.sp,
                        color = TextForestMuted
                    )
                    LinearProgressIndicator(
                        progress = { state.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = BrandForest,
                        trackColor = BrandSageLight
                    )
                    Text(
                        "${(state.progress.coerceIn(0f, 1f) * 100).toInt()}%",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = BrandForest
                    )
                    TextButton(onClick = controller::cancelDownload, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancelar descarga", color = TextForestMuted, fontSize = 12.sp)
                    }
                }

                is DoclingModelState.Installed -> {
                    Surface(color = BrandLime.copy(alpha = 0.6f), shape = RoundedCornerShape(6.dp)) {
                        Text("✓ Instalado y verificado", color = BrandForest, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Text("Granite-Docling 258M · ${formatModelMiB(state.totalBytes)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandDark)
                    Text("El análisis se ejecuta en este navegador y requiere conexión solo para instalar el modelo.", fontSize = 11.sp, color = TextForestMuted)
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                            controller.deleteModelAsync()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eliminar modelo local", fontSize = 11.sp, color = BrandForest)
                    }
                }

                is DoclingModelState.Error -> {
                    Text("No se pudo verificar Granite-Docling", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    Text(state.message, fontSize = 11.sp, color = TextForestMuted)
                    OutlinedButton(
                        onClick = { scope.launch { controller.refresh() } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Volver a comprobar", fontSize = 11.sp, color = BrandForest)
                    }
                    TextButton(onClick = controller::startDownload, modifier = Modifier.fillMaxWidth()) {
                        Text("Reintentar instalación", color = BrandForest, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
