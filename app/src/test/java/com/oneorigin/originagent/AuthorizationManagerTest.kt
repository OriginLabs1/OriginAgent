package com.oneorigin.originagent

import com.oneorigin.originagent.domain.Plan
import com.oneorigin.originagent.domain.PlanStep
import com.oneorigin.originagent.domain.RiskLevel
import com.oneorigin.originagent.harness.AuthorizationManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthorizationManagerTest {

    private fun manager(autoApprove: Boolean, consent: () -> Boolean) = AuthorizationManager(
        autoApproveLowRisk = { autoApprove },
        requestConsent = { _, _ -> consent() }
    )

    @Test
    fun `high risk requires explicit consent even when low risk is auto approved`() = runBlocking {
        val manager = manager(autoApprove = true, consent = { false })
        val result = manager.decide(highRiskStep(), plan())
        assertEquals("DENIED", result.decision.name)
        assertEquals("User declined consent for memory.write", result.reason)
    }

    @Test
    fun `high risk granted after explicit consent`() = runBlocking {
        val manager = manager(autoApprove = true, consent = { true })
        val result = manager.decide(highRiskStep(), plan())
        assertEquals("GRANTED", result.decision.name)
        assertEquals("Explicit user consent recorded", result.reason)
    }

    @Test
    fun `low risk is granted by policy`() = runBlocking {
        val manager = manager(autoApprove = true, consent = { throw AssertionError("should not ask") })
        val result = manager.decide(lowRiskStep(), plan())
        assertEquals("GRANTED", result.decision.name)
        assertEquals("Low-risk local operation approved by policy", result.reason)
    }

    @Test
    fun `low risk asks when auto approve disabled`() = runBlocking {
        var asked = false
        val manager = manager(autoApprove = false, consent = { asked = true; true })
        val result = manager.decide(lowRiskStep(), plan())
        assertEquals("GRANTED", result.decision.name)
        assertEquals(true, asked)
    }

    private fun highRiskStep() = PlanStep(
        id = "step_1", description = "Save note", toolId = "notes.add",
        risk = RiskLevel.HIGH, permission = "memory.write"
    )

    private fun lowRiskStep() = PlanStep(
        id = "step_2", description = "Calculate", toolId = "calculator",
        risk = RiskLevel.LOW
    )

    private fun plan() = Plan("plan_1", "intent_1", "notes.add", listOf(highRiskStep()))
}
