package com.finsim.ingestion

private fun readNavigatorDeviceMemory(): Double =
    js("typeof navigator !== 'undefined' && 'deviceMemory' in navigator ? Number(navigator.deviceMemory) : 4.0")

actual fun getDeviceCapabilityGate(): DeviceCapabilityGate {
    val ramGb = try {
        readNavigatorDeviceMemory()
    } catch (_: Throwable) {
        4.0
    }
    return DeviceCapabilityGate(
        totalRamGb = ramGb,
        isEligible = false,
        ineligibilityReason = "Gemma solo está disponible en Android."
    )
}
