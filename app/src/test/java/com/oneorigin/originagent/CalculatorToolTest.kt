package com.oneorigin.originagent

import com.oneorigin.originagent.tools.CalculatorTool
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorToolTest {

    private val tool = CalculatorTool()

    @Test
    fun `calculates percentage`() = runBlocking {
        val result = tool.execute(mapOf("expression" to "12% of 840"))
        assertEquals("100.8", result.output)
        assertTrue(result.error == null)
    }

    @Test
    fun `calculates nested expression`() = runBlocking {
        val result = tool.execute(mapOf("expression" to "(7 + 3) * 2 - 4 / 2"))
        assertEquals("18", result.output)
    }

    @Test
    fun `rejects division by zero without crashing`() = runBlocking {
        val result = tool.execute(mapOf("expression" to "10 / 0"))
        assertTrue(result.error?.contains("zero", true) == true)
    }

    @Test
    fun `rejects empty expression`() = runBlocking {
        val result = tool.execute(emptyMap())
        assertTrue(result.error != null)
    }
}
