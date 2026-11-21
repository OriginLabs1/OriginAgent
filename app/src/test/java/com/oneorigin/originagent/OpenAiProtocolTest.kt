package com.oneorigin.originagent

import com.oneorigin.originagent.models.OpenAiProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiProtocolTest {

    @Test
    fun escapesJsonControlCharacters() {
        assertEquals(
            "a\\\"b\\\\c\\nd\\te",
            OpenAiProtocol.escape("a\"b\\c\nd\te")
        )
    }

    @Test
    fun buildsRequestWithSystemAndUserMessages() {
        val json = OpenAiProtocol.requestJson("model-x", "Be brief", "Hello \"Origin\"", 128)
        assertTrue(json.contains("\"model\":\"model-x\""))
        assertTrue(json.contains("\"role\":\"system\""))
        assertTrue(json.contains("\"content\":\"Be brief\""))
        assertTrue(json.contains("\"role\":\"user\""))
        assertTrue(json.contains("Hello \\\"Origin\\\""))
        assertTrue(json.contains("\"max_tokens\":128"))
    }

    @Test
    fun omitsSystemMessageWhenBlank() {
        val json = OpenAiProtocol.requestJson("m", "", "hi", 64)
        assertTrue(!json.contains("\"role\":\"system\""))
    }

    @Test
    fun parsesAssistantContent() {
        val payload = """{"id":"c1","choices":[{"index":0,"message":{"role":"assistant","content":"All good."},"finish_reason":"stop"}]}"""
        assertEquals("All good.", OpenAiProtocol.responseText(payload))
    }

    @Test
    fun parsesEscapedMultilineContent() {
        val payload = """{"choices":[{"message":{"content":"line one\nline two"}}]}"""
        assertEquals("line one\nline two", OpenAiProtocol.responseText(payload))
    }

    @Test
    fun parsesUnicodeContent() {
        val payload = """{"choices":[{"message":{"content":"caf\u00e9"}}]}"""
        assertEquals("café", OpenAiProtocol.responseText(payload))
    }

    @Test
    fun parsesErrorDetail() {
        val payload = """{"error":{"message":"Incorrect API key provided","type":"invalid_request_error"}}"""
        assertEquals("Incorrect API key provided", OpenAiProtocol.errorMessage(payload))
    }

    @Test
    fun parsesModelList() {
        val payload = """{"object":"list","data":[{"id":"gpt-4o-mini"},{"id":"gpt-4o"}]}"""
        assertEquals(listOf("gpt-4o-mini", "gpt-4o"), OpenAiProtocol.modelNames(payload))
    }

    @Test
    fun returnsNullWhenContentMissing() {
        assertNull(OpenAiProtocol.responseText("""{"choices":[]}"""))
        assertNull(OpenAiProtocol.responseText("{}"))
    }
}
