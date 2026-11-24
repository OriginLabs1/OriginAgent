package com.oneorigin.originagent

import com.oneorigin.originagent.models.ModelCatalog
import com.oneorigin.originagent.models.ModelKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {

    @Test
    fun everyModelHasAUniqueId() {
        val ids = ModelCatalog.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun toolEngineIsOfflineButNeverChatCapable() {
        val local = ModelCatalog.LOCAL
        assertEquals(ModelKind.LOCAL_DETERMINISTIC, local.kind)
        assertEquals(null, local.defaultEndpoint)
        assertFalse(local.requiresApiKey)
        assertTrue(local.capabilities.contains("offline"))
        assertFalse("rule-based tools must never answer a chat", local.chatCapable)
        assertFalse(ModelCatalog.CHAT_MODELS.contains(local))
    }

    @Test
    fun remoteDescriptorsDeclareAnEndpointContract() {
        val remote = ModelCatalog.ALL.filter { it.isRemote }
        assertTrue(remote.size >= 4)
        remote.forEach { model ->
            assertNotNull(model.modelName)
            assertTrue(model.description.isNotBlank())
            assertTrue(model.limitations.isNotBlank())
            assertTrue(model.providerId.isNotBlank())
        }
        assertTrue(ModelCatalog.OPENAI.requiresApiKey)
        assertTrue(ModelCatalog.OPENROUTER.requiresApiKey)
        assertFalse(ModelCatalog.OLLAMA.requiresApiKey)
    }

    @Test
    fun everyLocalChatModelIsRealDownloadableGguf() {
        assertEquals(5, ModelCatalog.LOCAL_MODELS.size)
        ModelCatalog.LOCAL_MODELS.forEach { model ->
            assertEquals(ModelKind.LOCAL_NATIVE, model.kind)
            assertTrue(model.chatCapable)
            assertTrue(model.isDownloadable)
            assertNotNull(model.downloadSpec)
            assertTrue(model.downloadSpec!!.fileName.endsWith(".gguf"))
            assertTrue(model.capabilities.contains("chat"))
            assertFalse(model.description.contains("user-supplied", ignoreCase = true))
        }
        assertTrue(ModelCatalog.CHAT_MODELS.containsAll(ModelCatalog.LOCAL_MODELS))
    }

    @Test
    fun lookupByKnownAndUnknownId() {
        assertNotNull(ModelCatalog.byId("local.deterministic"))
        assertNotNull(ModelCatalog.byId("remote.custom"))
        assertEquals(null, ModelCatalog.byId("does.not.exist"))
    }
}
