package com.finsim.app.ui.ingestion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.finsim.ingestion.*

private fun formatBytesToMb(bytes: Long): String {
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return "${(mb * 10).toLong() / 10.0} MB"
}

/**
 * Card / component displaying local model download progress, status, and
 * the explicit action to purge the model to free 2.58 GB of storage.
 */
@Composable
fun ModelDownloadProgressCard(
    manager: ModelDownloadManager,
    modifier: Modifier = Modifier,
    onModelDeleted: () -> Unit = {},
    onDownloadCompleted: () -> Unit = {}
) {
    val downloadState by manager.state.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(downloadState) {
        if (downloadState is DownloadState.Completed) {
            onDownloadCompleted()
        }
    }

    if (showDeleteConfirmDialog) {
        Dialog(onDismissRequest = { showDeleteConfirmDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWarm,
                border = BorderStroke(1.dp, BorderSage),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "¿Eliminar modelo local?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Se liberarán 2.58 GB de almacenamiento eliminando el modelo local Gemma 4 E2B. Podrás volver a descargarlo en cualquier momento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextForestMuted,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = false },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderSage),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", color = TextForestMuted)
                        }
                        Button(
                            onClick = {
                                manager.deleteModel()
                                showDeleteConfirmDialog = false
                                onModelDeleted()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text("Liberar espacio", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = BorderStroke(1.dp, BorderSage)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            when (val state = downloadState) {
                is DownloadState.Idle -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Modelo local Gemma 4 E2B INT4",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BrandDark
                            )
                            Text(
                                text = "Descarga de 2.58 GB · tus documentos se procesan en el dispositivo",
                                fontSize = 11.sp,
                                color = TextForestMuted
                            )
                        }
                        Button(
                            onClick = {
                                manager.startDownload()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandForest, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Descargar (2.58 GB)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is DownloadState.Downloading -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Descargando Gemma 4 E2B INT4...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BrandDark
                                )
                                Text(
                                    text = "${formatBytesToMb(state.downloadedBytes)} de ${formatBytesToMb(state.totalBytes)}",
                                    fontSize = 11.sp,
                                    color = TextForestMuted
                                )
                            }
                            Text(
                                text = "${(state.progress * 100).toInt()}%",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = BrandForest
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = BrandForest,
                            trackColor = BrandSageLight
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { manager.cancelDownload() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Cancelar descarga", color = TextForestMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }

                is DownloadState.Completed -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = BrandLime.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "✓ Instalado",
                                        color = BrandForest,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Modelo local en dispositivo",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = BrandDark
                                    )
                                    Text(
                                    text = "Gemma 4 E2B (${formatBytesToMb(state.totalSizeBytes)})",
                                        fontSize = 11.sp,
                                        color = TextForestMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = { showDeleteConfirmDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderSage),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Liberar espacio (2.58 GB)",
                                    fontSize = 11.sp,
                                    color = BrandForest,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                is DownloadState.Error -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Gemma no está disponible: ${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { manager.startDownload() },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandForest),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reintentar", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
