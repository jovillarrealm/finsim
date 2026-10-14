package com.finsim.ingestion

private fun readJsNavigatorDeviceMemory(): Double {
    val mem = js("typeof navigator !== 'undefined' && 'deviceMemory' in navigator ? navigator.deviceMemory : 4.0")
    return (mem as Number).toDouble()
}

actual fun getDeviceCapabilityGate(): DeviceCapabilityGate {
    val ramGb = try {
        readJsNavigatorDeviceMemory()
    } catch (_: Throwable) {
        4.0
    }
    return DeviceCapabilityGate(totalRamGb = ramGb)
}
