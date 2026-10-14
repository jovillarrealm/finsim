package com.finsim.ingestion

import kotlin.test.*

class DeviceCapabilityGateTest {

    @Test
    fun testDeviceWith4GbRamIsIneligibleAndExplainsLmkdAnd6GbRequirement() {
        val gate = FakeDeviceCapabilityGate(totalRamGb = 4.0)

        assertEquals(4.0, gate.totalRamGb)
        assertFalse(gate.isEligibleForVlm, "Devices with 4.0 GB RAM must not be eligible for VLM")
        assertNotNull(gate.ineligibilityReason, "Ineligible devices must provide a human-readable reason")
        
        val reason = gate.ineligibilityReason!!
        assertTrue(
            reason.contains("LMKD", ignoreCase = true),
            "Reason should explain LMKD (Low Memory Killer Daemon) risk: '$reason'"
        )
        assertTrue(
            reason.contains("6", ignoreCase = false),
            "Reason should reference the 6.0 GB minimum requirement: '$reason'"
        )

        // Also verify via default invoke operator
        val defaultGate = DeviceCapabilityGate(totalRamGb = 4.0)
        assertFalse(defaultGate.isEligibleForVlm)
        assertNotNull(defaultGate.ineligibilityReason)
    }

    @Test
    fun testDeviceWith6GbRamIsEligible() {
        val gate = FakeDeviceCapabilityGate(totalRamGb = 6.0)

        assertEquals(6.0, gate.totalRamGb)
        assertTrue(gate.isEligibleForVlm, "Devices with exactly 6.0 GB RAM must be eligible for VLM")
        assertNull(gate.ineligibilityReason, "Eligible devices must have null ineligibilityReason")

        val defaultGate = DeviceCapabilityGate(totalRamGb = 6.0)
        assertTrue(defaultGate.isEligibleForVlm)
        assertNull(defaultGate.ineligibilityReason)
    }

    @Test
    fun testDevicesWith8GbAnd12GbRamAreEligible() {
        val gate8 = FakeDeviceCapabilityGate(totalRamGb = 8.0)
        assertTrue(gate8.isEligibleForVlm, "Devices with 8.0 GB RAM must be eligible")
        assertNull(gate8.ineligibilityReason)

        val gate12 = FakeDeviceCapabilityGate(totalRamGb = 12.0)
        assertTrue(gate12.isEligibleForVlm, "Devices with 12.0 GB RAM must be eligible")
        assertNull(gate12.ineligibilityReason)
    }

    @Test
    fun testPlatformGateProvidesValidValues() {
        val platformGate = getDeviceCapabilityGate()
        assertTrue(platformGate.totalRamGb >= 0.0)
        if (platformGate.totalRamGb < 6.0) {
            assertFalse(platformGate.isEligibleForVlm)
            assertNotNull(platformGate.ineligibilityReason)
        } else {
            assertTrue(platformGate.isEligibleForVlm)
            assertNull(platformGate.ineligibilityReason)
        }
    }
}
