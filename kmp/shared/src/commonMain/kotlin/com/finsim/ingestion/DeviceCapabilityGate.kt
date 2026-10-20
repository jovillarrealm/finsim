package com.finsim.ingestion

/**
 * Interface and evaluation gate for host hardware capability before allowing
 * heavy local VLM (Vision-Language Model) inference.
 *
 * Gemma requires at least 8 GiB of kernel-visible total RAM on Android.
 * Total RAM alone does not guarantee enough available memory for inference.
 */
interface DeviceCapabilityGate {
    val totalRamGb: Double
    val isEligibleForVlm: Boolean
    val ineligibilityReason: String?
    val hasHardwareAcceleration: Boolean

    companion object {
        const val MINIMUM_RAM_GB_FOR_VLM: Double = 8.0

        operator fun invoke(
            totalRamGb: Double,
            hasHardwareAcceleration: Boolean = true,
            isEligible: Boolean? = null,
            ineligibilityReason: String? = null
        ): DeviceCapabilityGate = DefaultDeviceCapabilityGate(
            totalRamGb = totalRamGb,
            hasHardwareAcceleration = hasHardwareAcceleration,
            isEligibleForVlm = isEligible ?: (totalRamGb >= MINIMUM_RAM_GB_FOR_VLM),
            ineligibilityReason = ineligibilityReason ?: if ((isEligible ?: (totalRamGb >= MINIMUM_RAM_GB_FOR_VLM)).not()) {
                val formatted = (totalRamGb * 10).toLong() / 10.0
                "Memoria RAM insuficiente ($formatted GiB). Se requiere un mínimo de 8.0 GiB de RAM para ejecutar el modelo de visión local y evitar cierres forzosos por LMKD."
            } else null
        )
    }
}

/**
 * Default implementation of [DeviceCapabilityGate].
 */
data class DefaultDeviceCapabilityGate(
    override val totalRamGb: Double,
    override val hasHardwareAcceleration: Boolean = true,
    override val isEligibleForVlm: Boolean = totalRamGb >= DeviceCapabilityGate.MINIMUM_RAM_GB_FOR_VLM,
    override val ineligibilityReason: String? = if (totalRamGb < DeviceCapabilityGate.MINIMUM_RAM_GB_FOR_VLM) {
        val formatted = (totalRamGb * 10).toLong() / 10.0
        "Memoria RAM insuficiente ($formatted GiB). Se requiere un mínimo de 8.0 GiB de RAM para ejecutar el modelo de visión local y evitar cierres forzosos por LMKD."
    } else null
) : DeviceCapabilityGate

/**
 * Fake capability gate implementation for unit testing and headless simulation.
 */
class FakeDeviceCapabilityGate(
    override val totalRamGb: Double,
    override val hasHardwareAcceleration: Boolean = true,
    isEligible: Boolean? = null,
    ineligibilityReason: String? = null
) : DeviceCapabilityGate {
    override val isEligibleForVlm: Boolean = isEligible ?: (totalRamGb >= DeviceCapabilityGate.MINIMUM_RAM_GB_FOR_VLM)
    override val ineligibilityReason: String? = ineligibilityReason ?: if (!this.isEligibleForVlm) {
        val formatted = (totalRamGb * 10).toLong() / 10.0
        "Memoria RAM insuficiente ($formatted GiB). Se requiere un mínimo de 8.0 GiB de RAM para ejecutar el modelo de visión local y evitar cierres forzosos por LMKD."
    } else null
}

/**
 * Expect declaration providing platform-specific hardware capability inspection.
 */
expect fun getDeviceCapabilityGate(): DeviceCapabilityGate
