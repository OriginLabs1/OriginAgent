package com.oneorigin.originagent

import com.oneorigin.originagent.models.DownloadableModelSpec
import com.oneorigin.originagent.models.DownloadableModels
import com.oneorigin.originagent.models.ModelCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the product rule: Origin Agent never suggests a model whose weights
 * the user must supply. Every chat model must have a real public GGUF URL, a pinned SHA-256,
 * a published size, and a runtime that can actually execute it.
 */
class DownloadableModelTest {

    @Test
    fun allStrategicFamiliesAreDownloadable() {
        assertEquals(5, DownloadableModels.ALL.size)
        val families = DownloadableModels.ALL.map { it.family }
        assertTrue(families.contains("BitNet ternary"))
        assertTrue(families.contains("LFM2.5-class"))
        assertTrue(families.contains("Optimized sub-1B"))
        assertTrue(families.contains("Qwen-class compact"))
    }

    @Test
    fun everyDownloadableModelIsSelfContained() {
        val ids = DownloadableModels.ALL.map { it.modelId }
        assertEquals(ids.size, ids.toSet().size)
        DownloadableModels.ALL.forEach { spec ->
            assertTrue("${spec.modelId}: URL must be https", spec.url.startsWith("https://"))
            assertTrue("${spec.modelId}: must be a public GGUF", spec.url.contains(".gguf"))
            assertTrue("${spec.modelId}: SHA-256 must be 64 hex chars", spec.sha256.matches(Regex("[0-9a-f]{64}")))
            assertTrue("${spec.modelId}: size must be published", spec.sizeBytes > 10L * 1024 * 1024)
            assertTrue("${spec.modelId}: file name required", spec.fileName.endsWith(".gguf"))
            assertTrue("${spec.modelId}: format required", spec.format.startsWith("gguf/"))
            assertTrue("${spec.modelId}: license required", spec.license.isNotBlank())
            assertTrue("${spec.modelId}: license URL required", spec.licenseUrl.startsWith("https://"))
            assertTrue("${spec.modelId}: attribution required", spec.attribution.isNotBlank())
            assertTrue("${spec.modelId}: context window required", spec.contextSize >= 1024)
            assertTrue("${spec.modelId}: summary required", spec.summary.isNotBlank())
        }
    }

    @Test
    fun everyLocalPickerEntryIsDownloadableOrAlwaysOnToolEngine() {
        ModelCatalog.ALL.filter { !it.isRemote }.forEach { model ->
            assertTrue(
                "${model.id}: local entries must be downloadable GGUF or the tool engine",
                model.isDownloadable || model.id == ModelCatalog.LOCAL.id
            )
            if (model.isDownloadable) assertTrue(model.chatCapable)
        }
    }

    @Test
    fun noSuggestedModelAsksForUserSuppliedWeights() {
        ModelCatalog.CHAT_MODELS.forEach { model ->
            val copy = "${model.description} ${model.limitations} ${model.modelName}".lowercase()
            assertFalse("${model.id} still mentions user-supplied weights", copy.contains("user-supplied"))
            assertFalse("${model.id} still mentions user supplied weights", copy.contains("user supplied"))
        }
    }

    @Test
    fun pinnedChecksumsMatchThePublishedArtifacts() {
        assertSha(DownloadableModels.SMOLM2_135M_POCKET, "bda484992f9655d22504b14e57985257fa6a86937c61f957cf99c10a3bcae169")
        assertSha(DownloadableModels.BITNET_2B4T, "db8852edfaa08476ab8476052c7777990cf73df16f2618604115a2c954e06f3a")
        assertSha(DownloadableModels.LFM2_5_350M, "7e6f72643caafc9a68256686638c4d7916f2cec76d1df478d4c3ddcd95a6aed4")
        assertSha(DownloadableModels.SUB_1B, "e55c2a5df7f8f6eed1a843ff709fa8008dbce65d82e57c5c06dec5ee8d060415")
        assertSha(DownloadableModels.QWEN_COMPACT, "74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db")
    }

    private fun assertSha(spec: DownloadableModelSpec, sha: String) =
        assertEquals("${spec.label}: pinned SHA-256 changed", sha, spec.sha256)
}
