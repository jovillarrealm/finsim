package com.finsim.ingestion

import android.app.ActivityManager
import android.content.Context

class AndroidDeviceCapabilityGate(context: Context) : DeviceCapabilityGate {
    private val manager = context.applicationContext.getSystemService(ActivityManager::class.java)

    override val totalRamGb: Double = runCatching {
        ActivityManager.MemoryInfo().also { manager?.getMemoryInfo(it) }.totalMem.toDouble() / GIB
    }.getOrDefault(0.0)

    override val isUnderMemoryPressure: Boolean
        get() = runCatching {
            val activeManager = manager ?: return@runCatching true
            ActivityManager.MemoryInfo().also(activeManager::getMemoryInfo).lowMemory
        }.getOrDefault(true)

    override val isEligibleForVlm: Boolean
        get() = totalRamGb >= DeviceCapabilityGate.MINIMUM_RAM_GB_FOR_VLM && !isUnderMemoryPressure

    override val ineligibilityReason: String?
        get() = when {
            totalRamGb < DeviceCapabilityGate.MINIMUM_RAM_GB_FOR_VLM ->
                "Android informa ${"%.1f".format(totalRamGb)} GiB de RAM total; Gemma requiere al menos 8 GiB."
            isUnderMemoryPressure ->
                "Android informa presión de memoria baja; libera memoria y vuelve a intentar."
            else -> null
        }

    override val hasHardwareAcceleration: Boolean = true

    private companion object {
        const val GIB = 1024.0 * 1024.0 * 1024.0
    }
}

actual fun getDeviceCapabilityGate(): DeviceCapabilityGate {
    return runCatching { AndroidDeviceCapabilityGate(AndroidIngestionContext.require()) }
        .getOrElse {
            DeviceCapabilityGate(
                totalRamGb = 0.0,
                isEligible = false,
                ineligibilityReason = "No se pudo consultar la memoria actual de Android; Gemma permanece deshabilitado."
            )
        }
}
