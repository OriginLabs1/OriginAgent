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
        val steps = plan("Remember: buy oat milk").steps
        val remember = steps.first()
        assertEquals("notes.add", remember.toolId)
        assertEquals(RiskLevel.HIGH, remember.risk)
        assertTrue(remember.requiresAuthorization)
        assertEquals("buy oat milk", remember.input["content"])
        assertEquals("engine.generate", steps.last().toolId)
    }

    @Test
    fun `calculation gathers deterministic context but a real model answers`() {
        val steps = plan("Calculate 12% of 840").steps
        assertEquals(2, steps.size)
        assertEquals("calculator", steps.first().toolId)
        assertEquals("12% of 840", steps.first().input["expression"])
        assertEquals("engine.generate", steps.last().toolId)
        assertEquals("Calculate 12% of 840", steps.last().input["prompt"])
    }

    @Test
    fun `status creates observable steps then a real model answer`() {
        val steps = plan("What can you do?").steps
        assertEquals(3, steps.size)
        assertTrue(steps.any { it.toolId == "system.status" })
        assertTrue(steps.any { it.toolId == "model.status" })
        assertEquals("engine.generate", steps.last().toolId)
    }

    @Test
    fun `every user conversation ends with a real model step`() {
        val prompts = listOf(
            "What time is it?",
            "Explain trustworthy execution",
            "Which model is answering?",
            "Search my notes about BitNet",
            "Tell me a story about a lighthouse"
        )
        prompts.forEach { prompt ->
            val steps = plan(prompt).steps
            assertTrue("$prompt must not be answered by tools alone", steps.isNotEmpty())
            assertEquals("$prompt must end with a real AI model", "engine.generate", steps.last().toolId)
        }
    }
}
