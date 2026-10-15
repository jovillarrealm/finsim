package com.finsim.ingestion

import java.io.File

actual fun getDeviceCapabilityGate(): DeviceCapabilityGate {
    val totalRamGb = try {
        val meminfo = File("/proc/meminfo")
        if (meminfo.exists()) {
            val line = meminfo.readLines().firstOrNull { it.startsWith("MemTotal:") }
            if (line != null) {
                val parts = line.split(Regex("\\s+"))
                val kb = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                kb.toDouble() / (1024.0 * 1024.0)
            } else {
                null
            }
        } else {
            null
        }
    } catch (_: Throwable) {
        null
    } ?: run {
        Runtime.getRuntime().maxMemory().toDouble() / (1024.0 * 1024.0 * 1024.0)
    }

    return DeviceCapabilityGate(totalRamGb = totalRamGb)
}
