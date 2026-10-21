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
    val isUnderMemoryPressure: Boolean get() = false

    companion object {
        const val MINIMUM_RAM_GB_FOR_VLM: Double = 8.0

        operator fun invoke(
            totalRamGb: Double,
            hasHardwareAcceleration: Boolean = true,
            isEligible: Boolean? = null,
            ineligibilityReason: String? = null,
            isUnderMemoryPressure: Boolean = false
        ): DeviceCapabilityGate {
            val enoughRam = totalRamGb >= MINIMUM_RAM_GB_FOR_VLM
            val eligible = enoughRam && !isUnderMemoryPressure && (isEligible ?: true)
            val reason = ineligibilityReason ?: when {
                !enoughRam -> "Memoria RAM insuficiente (${(totalRamGb * 10).toLong() / 10.0} GiB). Se requiere un mínimo de 8.0 GiB de RAM."
                isUnderMemoryPressure -> "Android informa presión de memoria baja; libera memoria y vuelve a intentar."
                !eligible -> "Gemma solo está disponible en Android."
                else -> null
            }
            return DefaultDeviceCapabilityGate(
                totalRamGb = totalRamGb,
                hasHardwareAcceleration = hasHardwareAcceleration,
                platformSupported = isEligible,
                reasonOverride = reason,
                isUnderMemoryPressure = isUnderMemoryPressure
            )
        }
    }
}

/**
 * Default implementation of [DeviceCapabilityGate].
 */
data class DefaultDeviceCapabilityGate(
    override val totalRamGb: Double,
    override val hasHardwareAcceleration: Boolean = true,
    private val platformSupported: Boolean? = null,
    private val reasonOverride: String? = null,
    override val isUnderMemoryPressure: Boolean = false
) : DeviceCapabilityGate {
    override val isEligibleForVlm: Boolean = totalRamGb >= DeviceCapabilityGate.MINIMUM_RAM_GB_FOR_VLM &&
        !isUnderMemoryPressure && (platformSupported ?: true)

    override val ineligibilityReason: String? = reasonOverride ?: when {
        totalRamGb < DeviceCapabilityGate.MINIMUM_RAM_GB_FOR_VLM -> {
            val formatted = (totalRamGb * 10).toLong() / 10.0
            "Memoria RAM insuficiente ($formatted GiB). Se requiere un mínimo de 8.0 GiB de RAM para ejecutar el modelo de visión local y evitar cierres forzosos por LMKD."
        }
        isUnderMemoryPressure -> "Android informa presión de memoria baja; libera memoria y vuelve a intentar."
        platformSupported == false -> "Gemma solo está disponible en Android."
        else -> null
    }
}

/**
 * Fake capability gate implementation for unit testing and headless simulation.
 */
class FakeDeviceCapabilityGate(
    override val totalRamGb: Double,
    override val hasHardwareAcceleration: Boolean = true,
    isEligible: Boolean? = null,
    ineligibilityReason: String? = null,
    override val isUnderMemoryPressure: Boolean = false
) : DeviceCapabilityGate {
    override val isEligibleForVlm: Boolean = totalRamGb >= DeviceCapabilityGate.MINIMUM_RAM_GB_FOR_VLM &&
        !isUnderMemoryPressure && (isEligible ?: true)
    override val ineligibilityReason: String? = ineligibilityReason ?: when {
        totalRamGb < DeviceCapabilityGate.MINIMUM_RAM_GB_FOR_VLM -> {
            val formatted = (totalRamGb * 10).toLong() / 10.0
            "Memoria RAM insuficiente ($formatted GiB). Se requiere un mínimo de 8.0 GiB de RAM para ejecutar el modelo de visión local y evitar cierres forzosos por LMKD."
        }
        isUnderMemoryPressure -> "Android informa presión de memoria baja; libera memoria y vuelve a intentar."
        isEligible == false -> "Gemma solo está disponible en Android."
        else -> null
    }
}

/**
 * Expect declaration providing platform-specific hardware capability inspection.
 */
expect fun getDeviceCapabilityGate(): DeviceCapabilityGate
