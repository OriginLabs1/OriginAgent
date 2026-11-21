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
    fun localEngineIsAlwaysAvailableAndHasNoEndpoint() {
        val local = ModelCatalog.LOCAL
        assertEquals(ModelKind.LOCAL_DETERMINISTIC, local.kind)
        assertEquals(null, local.defaultEndpoint)
        assertFalse(local.requiresApiKey)
        assertTrue(local.capabilities.contains("offline"))
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
    fun researchEnginesAreClearlyMarked() {
        assertTrue(ModelCatalog.BITNET.researchOnly)
        assertTrue(ModelCatalog.LLAMA.researchOnly)
        assertEquals(ModelKind.LOCAL_NATIVE, ModelCatalog.BITNET.kind)
    }

    @Test
    fun lookupByKnownAndUnknownId() {
        assertNotNull(ModelCatalog.byId("local.deterministic"))
        assertNotNull(ModelCatalog.byId("remote.custom"))
        assertEquals(null, ModelCatalog.byId("does.not.exist"))
    }
}
