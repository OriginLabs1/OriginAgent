package com.oneorigin.originagent

import com.oneorigin.originagent.util.JsonCodec
import org.junit.Assert.assertEquals
import org.junit.Test

class JsonCodecTest {

    @Test
    fun `round trips escaped strings`() {
        val original = mapOf(
            "text" to "Say \"hi\"\nnext\tline \\ path",
            "count" to 3,
            "flag" to true
        )
        val decoded = JsonCodec.decode(JsonCodec.encode(original))
        assertEquals(original["text"], decoded["text"])
        assertEquals("3", decoded["count"])
        assertEquals("true", decoded["flag"])
    }

    @Test
    fun `export document is valid flat json`() {
        val document = JsonCodec.exportDocument(listOf(mapOf("type" to "note", "content" to "a,b\"c")))
        assertTrue(document.startsWith("{\"schema\":\"origin-agent/export\""))
        assertTrue(document.contains("a,b\\\"c"))
    }
}

private fun assertTrue(value: Boolean) = org.junit.Assert.assertTrue(value)
