package com.finsim.ingestion

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GemmaEligibilityTest {
    @Test
    fun gemmaRequiresAtLeastEightGiBOfTotalMemory() {
        assertFalse(DeviceCapabilityGate(totalRamGb = 7.999).isEligibleForVlm)
        assertTrue(DeviceCapabilityGate(totalRamGb = 8.0).isEligibleForVlm)
    }

    @Test
    fun eligibilityOverrideCannotBypassTotalMemoryRequirement() {
        assertFalse(DeviceCapabilityGate(totalRamGb = 4.0, isEligible = true).isEligibleForVlm)
    }

    @Test
    fun lowMemoryPressureBlocksGemmaSeparatelyFromTotalRam() {
        val gate = DeviceCapabilityGate(totalRamGb = 8.0, isUnderMemoryPressure = true)

        assertFalse(gate.isEligibleForVlm)
        assertTrue(gate.ineligibilityReason.orEmpty().contains("presión de memoria"))
    }
}
