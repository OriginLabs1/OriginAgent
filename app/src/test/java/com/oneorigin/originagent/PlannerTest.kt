package com.oneorigin.originagent

import com.oneorigin.originagent.domain.Intent
import com.oneorigin.originagent.domain.RiskLevel
import com.oneorigin.originagent.harness.Planner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerTest {

    private val planner = Planner()

    private fun plan(text: String) = planner.plan(Intent("i_1", text))

    @Test
    fun `remember is high risk and needs authorization`() {
        val step = plan("Remember: buy oat milk").steps.single()
        assertEquals("notes.add", step.toolId)
        assertEquals(RiskLevel.HIGH, step.risk)
        assertTrue(step.requiresAuthorization)
        assertEquals("buy oat milk", step.input["content"])
    }

    @Test
    fun `calculation routes to deterministic calculator`() {
        val step = plan("Calculate 12% of 840").steps.single()
        assertEquals("calculator", step.toolId)
        assertEquals("12% of 840", step.input["expression"])
    }

    @Test
    fun `status creates observable steps`() {
        val steps = plan("What can you do?").steps
        assertEquals(2, steps.size)
        assertTrue(steps.any { it.toolId == "system.status" })
        assertTrue(steps.any { it.toolId == "model.status" })
    }
}
