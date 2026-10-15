package com.finsim.ingestion

import java.lang.management.ManagementFactory

actual fun getDeviceCapabilityGate(): DeviceCapabilityGate {
    val totalRamGb = try {
        val osBean = ManagementFactory.getOperatingSystemMXBean()
        val method = try {
            osBean.javaClass.getMethod("getTotalMemorySize")
        } catch (_: NoSuchMethodException) {
            osBean.javaClass.getMethod("getTotalPhysicalMemorySize")
        }
        val bytes = method.invoke(osBean) as Long
        bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    } catch (_: Throwable) {
        val maxMemoryBytes = Runtime.getRuntime().maxMemory()
        // If max heap is configured, estimate physical memory or fallback to runtime max
        maxMemoryBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    }

    return DeviceCapabilityGate(totalRamGb = totalRamGb)
}
