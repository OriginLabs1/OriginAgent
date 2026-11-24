package com.oneorigin.originagent

import com.oneorigin.originagent.models.DownloadableModelSpec
import com.oneorigin.originagent.models.DownloadableModels
import com.oneorigin.originagent.models.FitGrade
import com.oneorigin.originagent.models.ModelCatalog
import com.oneorigin.originagent.models.ModelSources
import com.oneorigin.originagent.models.DeviceMemory
import com.oneorigin.originagent.models.estimateRuntimeMb
import com.oneorigin.originagent.models.grade
import com.oneorigin.originagent.models.label
import com.oneorigin.originagent.models.recommendedOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Small-model catalog, memory-aware ordering and trusted browsing constants. */
class ModelCatalogSelectionTest {

    private val SHA_HEX = Regex("[0-9a-f]{64}")

    @Test
    fun everyArtifactHasHttpsPinnedMetadataAndTrustedSource() {
        assertTrue(DownloadableModels.ALL.size >= 5)
        assertEquals(DownloadableModels.ALL.size, DownloadableModels.ALL.map { it.modelId }.toSet().size)
        DownloadableModels.ALL.forEach { spec ->
            assertTrue("${spec.modelId}: URL must be https", spec.url.startsWith("https://"))
            assertTrue(
                "${spec.modelId}: SHA-256 must be 64 lowercase hex chars",
                spec.sha256.matches(SHA_HEX)
            )
            assertTrue("${spec.modelId}: size must be positive", spec.sizeBytes > 0L)
            assertTrue(
                "${spec.modelId}: source URL must be https and non-empty",
                spec.sourceUrl.startsWith("https://") && spec.sourceUrl.length > "https://".length
            )
            assertTrue("${spec.modelId}: badge required", spec.badge.isNotBlank())
            assertTrue("${spec.modelId}: minimum RAM required", spec.minimumRamMb > 0L)
            assertTrue("${spec.modelId}: estimated RAM required", spec.estimatedRamMb > 0L)
            assertEquals(
                "${spec.modelId}: published estimate must match ModelFit's estimate",
                estimateRuntimeMb(spec),
                spec.estimatedRamMb
            )
        }
    }

    @Test
    fun pocketArtifactMatchesVerifiedHuggingFaceMetadata() {
        val spec = DownloadableModels.SMOLM2_135M_POCKET
        assertEquals("local.smollm2.135m", spec.modelId)
        assertEquals("SmolLM2 135M · Pocket", spec.label)
        assertEquals("Optimized sub-1B", spec.family)
        assertEquals("SmolLM2-135M-Instruct-Q4_K_M.gguf", spec.fileName)
        assertEquals("https://huggingface.co/lmstudio-community/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q4_K_M.gguf", spec.url)
        assertEquals(105_454_144L, spec.sizeBytes)
        assertEquals(
            "bda484992f9655d22504b14e57985257fa6a86937c61f957cf99c10a3bcae169",
            spec.sha256
        )
        assertEquals("gguf/q4_k_m", spec.format)
        assertEquals("Apache-2.0", spec.license)
        assertEquals("Hugging Face TB SmolLM2-135M-Instruct", spec.attribution)
        assertEquals(2048, spec.contextSize)
        assertTrue(spec.recommended)
        assertTrue(spec.summary.contains("smallest", ignoreCase = true))
        assertTrue(spec.summary.contains("less capable", ignoreCase = true))
        assertEquals(spec, DownloadableModels.forModel(spec.modelId))
    }

    @Test
    fun catalogLeadsWithPocketButKeepsBitnetAndEveryLegacyEntry() {
        assertEquals(ModelCatalog.POCKET, ModelCatalog.LOCAL_MODELS.first())
        assertEquals(ModelCatalog.BITNET, ModelCatalog.LOCAL_MODELS[1])
        assertTrue(
            "legacy entries must remain selectable",
            ModelCatalog.LOCAL_MODELS.containsAll(
                listOf(ModelCatalog.BITNET, ModelCatalog.LFM2_5, ModelCatalog.SUB_1B, ModelCatalog.QWEN)
            )
        )
        assertTrue(ModelCatalog.ALL.contains(ModelCatalog.POCKET))
        assertTrue(ModelCatalog.CHAT_MODELS.containsAll(ModelCatalog.LOCAL_MODELS))
        assertEquals(ModelCatalog.POCKET.downloadSpec, ModelCatalog.byId("local.smollm2.135m")?.downloadSpec)
        assertTrue(ModelCatalog.POCKET.description.contains(ModelCatalog.POCKET.downloadSpec!!.badge))
        assertEquals(
            DownloadableModels.ALL.map { it.modelId },
            ModelCatalog.LOCAL_MODELS.map { it.downloadSpec!!.modelId }
        )
    }

    @Test
    fun memoryEstimatesAreConservativeAndContextAware() {
        assertEquals(304L, estimateRuntimeMb(DownloadableModels.SMOLM2_135M_POCKET))
        assertEquals(433L, estimateRuntimeMb(DownloadableModels.LFM2_5_350M))
        assertEquals(573L, estimateRuntimeMb(DownloadableModels.SUB_1B))
        assertEquals(708L, estimateRuntimeMb(DownloadableModels.QWEN_COMPACT))
        assertEquals(1353L, estimateRuntimeMb(DownloadableModels.BITNET_2B4T))

        // More context must never lower the estimate; size must never be under-counted.
        val longerContext = DownloadableModels.SMOLM2_135M_POCKET.copy(contextSize = 4096)
        assertTrue(estimateRuntimeMb(longerContext) > estimateRuntimeMb(DownloadableModels.SMOLM2_135M_POCKET))
        assertTrue(estimateRuntimeMb(DownloadableModels.SMOLM2_135M_POCKET) > 105L)
    }

    @Test
    fun gradesUseFreeMemoryWithHeadroomAndNeverOverPromise() {
        val spacious = DeviceMemory(8192L, 6000L)
        val modest = DeviceMemory(4096L, 804L)
        val cramped = DeviceMemory(2048L, 320L)
        val unknown = DeviceMemory(0L, 4096L)

        assertEquals(FitGrade.GREAT, grade(DownloadableModels.BITNET_2B4T, spacious))
        assertEquals(FitGrade.GREAT, grade(DownloadableModels.SMOLM2_135M_POCKET, modest))
        assertEquals(FitGrade.OKAY, grade(DownloadableModels.SUB_1B, modest))
        assertEquals(FitGrade.OKAY, grade(DownloadableModels.QWEN_COMPACT, modest))
        assertEquals(FitGrade.TIGHT, grade(DownloadableModels.BITNET_2B4T, modest))
        assertEquals(FitGrade.TIGHT, grade(DownloadableModels.SMOLM2_135M_POCKET, cramped))
        assertEquals(FitGrade.TIGHT, grade(DownloadableModels.SMOLM2_135M_POCKET, unknown))

        // A nonsensical "available > total" reading is clamped to the real device total.
        assertEquals(
            FitGrade.OKAY,
            grade(DownloadableModels.BITNET_2B4T, DeviceMemory(1536L, 6000L))
        )
        assertEquals(
            FitGrade.TIGHT,
            grade(DownloadableModels.BITNET_2B4T, DeviceMemory(512L, 6000L))
        )
    }

    @Test
    fun recommendedOrderIsDeterministicFitThenRecommendationThenSize() {
        val modest = DeviceMemory(4096L, 804L)
        val shuffled = listOf(
            DownloadableModels.BITNET_2B4T,
            DownloadableModels.QWEN_COMPACT,
            DownloadableModels.SUB_1B,
            DownloadableModels.SMOLM2_135M_POCKET,
            DownloadableModels.LFM2_5_350M
        )
        val expected = listOf(
            DownloadableModels.SMOLM2_135M_POCKET,
            DownloadableModels.LFM2_5_350M,
            DownloadableModels.SUB_1B,
            DownloadableModels.QWEN_COMPACT,
            DownloadableModels.BITNET_2B4T
        )
        assertEquals(expected, recommendedOrder(shuffled, modest))
        assertEquals(expected, recommendedOrder(expected, modest))

        // Equal fits still prefer the recommended artifact, then the smallest file.
        val spacious = DeviceMemory(8192L, 7000L)
        assertEquals(expected, recommendedOrder(shuffled, spacious))
    }

    @Test
    fun fitLabelsAreHeadlinesPlusAnExplicitEstimate() {
        val modest = DeviceMemory(4096L, 804L)
        val cramped = DeviceMemory(2048L, 320L)
        assertTrue(label(DownloadableModels.SMOLM2_135M_POCKET, modest).startsWith("Fits comfortably"))
        assertTrue(label(DownloadableModels.QWEN_COMPACT, modest).startsWith("Workable"))
        assertTrue(label(DownloadableModels.BITNET_2B4T, cramped).startsWith("May be tight"))
        DownloadableModels.ALL.forEach { spec ->
            val text = label(spec, modest)
            assertTrue("$text must not imply an exact measurement", text.contains("~"))
            assertTrue("$text must flag the estimate", text.contains("estimated RAM"))
        }
    }

    @Test
    fun curatedSourcesAreHttpsAndUsefulForCatalogModels() {
        assertTrue(ModelSources.ALL.size >= 5)
        assertEquals(ModelSources.ALL.size, ModelSources.ALL.map { it.url }.toSet().size)
        ModelSources.ALL.forEach { source ->
            assertTrue("${source.name}: URL must be https", source.url.startsWith("https://"))
            assertTrue("${source.name}: name required", source.name.isNotBlank())
            assertTrue("${source.name}: trust rationale required", source.whyTrusted.isNotBlank())
            assertTrue("${source.name}: families required", source.families.isNotEmpty())
        }
        val urls = ModelSources.ALL.map { it.url }
        assertTrue(urls.contains(ModelSources.HUGGING_FACE_TB_URL))
        assertTrue(urls.contains(ModelSources.QWEN_URL))
        assertTrue(urls.contains(ModelSources.LIQUID_AI_URL))
        assertTrue(urls.contains(ModelSources.MICROSOFT_BITNET_URL))
        assertTrue(urls.contains(ModelSources.HUGGING_FACE_GGUF_SEARCH_URL))

        assertEquals(ModelSources.HUGGING_FACE_TB_URL, sourceUrlFor(DownloadableModels.SMOLM2_135M_POCKET))
        assertEquals(ModelSources.QWEN_URL, sourceUrlFor(DownloadableModels.QWEN_COMPACT))
        assertEquals(ModelSources.LIQUID_AI_URL, sourceUrlFor(DownloadableModels.LFM2_5_350M))
        assertEquals(ModelSources.MICROSOFT_BITNET_URL, sourceUrlFor(DownloadableModels.BITNET_2B4T))
    }

    private fun sourceUrlFor(spec: DownloadableModelSpec): String {
        val source = ModelSources.sourceFor(spec)
        assertNotNull("No curated source for ${spec.modelId}", source)
        return source!!.url
    }
}
