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
}
