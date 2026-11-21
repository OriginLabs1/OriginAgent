package com.oneorigin.originagent

import com.oneorigin.originagent.models.AdapterTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdapterTemplatesTest {

    @Test
    fun builtInWebTemplatesAreFirstClass() {
        val labels = AdapterTemplates.ALL.map { it.label }
        assertTrue(labels.contains("OpenAI"))
        assertTrue(labels.contains("OpenRouter"))
        assertTrue(labels.contains("Groq"))
        assertTrue(labels.contains("Together AI"))
        assertTrue(labels.contains("DeepSeek"))
        assertTrue(labels.contains("Ollama"))
        assertTrue(labels.contains("LM Studio"))
        assertTrue("Custom web endpoint" in labels)
    }

    @Test
    fun templatesDeclareAUsableV1Contract() {
        AdapterTemplates.ALL.filter { it.endpoint.isNotBlank() }.forEach { template ->
            assertTrue("${template.label} must use an http(s) /v1 endpoint: ${template.endpoint}",
                template.endpoint.startsWith("https://") || template.endpoint.startsWith("http://"))
            assertTrue("${template.label} must include /v1", template.endpoint.trimEnd('/').endsWith("/v1"))
            assertTrue("${template.label} needs a model name", template.model.isNotBlank())
        }
    }

    @Test
    fun customTemplateIsAvailableAndKeyIsOptional() {
        val custom = AdapterTemplates.byId("custom")
        assertNotNull(custom)
        assertEquals("", custom!!.endpoint)
        assertEquals(false, custom.keyRequired)
        assertEquals(null, AdapterTemplates.byId("does-not-exist"))
    }
}
