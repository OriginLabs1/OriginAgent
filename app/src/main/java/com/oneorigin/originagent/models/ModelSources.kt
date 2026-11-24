package com.oneorigin.originagent.models

/**
 * Curated, read-only list of trustworthy places to discover GGUF weights.
 *
 * These are URL constants only: no WebView, browser or network code lives here. The UI may
 * open a link only after an explicit tap. Only official upstream organisations and the
 * Hugging Face GGUF catalogue are listed.
 */
data class ModelSource(
    val name: String,
    val url: String,
    val whyTrusted: String,
    val families: List<String>
)

object ModelSources {

    const val HUGGING_FACE_GGUF_SEARCH_URL =
        "https://huggingface.co/models?library=gguf&sort=downloads"
    const val HUGGING_FACE_TB_URL = "https://huggingface.co/HuggingFaceTB"
    const val QWEN_URL = "https://huggingface.co/Qwen"
    const val LIQUID_AI_URL = "https://huggingface.co/LiquidAI"
    const val MICROSOFT_BITNET_URL = "https://huggingface.co/microsoft/bitnet-b1.58-2B-4T"

    val ALL: List<ModelSource> = listOf(
        ModelSource(
            name = "Hugging Face TB",
            url = HUGGING_FACE_TB_URL,
            whyTrusted = "Official Hugging Face Transformer Models organisation: original SmolLM2 weights and licences.",
            families = listOf("smollm2", "optimized sub-1b")
        ),
        ModelSource(
            name = "Qwen",
            url = QWEN_URL,
            whyTrusted = "Official Qwen Team organisation: upstream Qwen2.5 weights and model cards.",
            families = listOf("qwen", "qwen-class compact")
        ),
        ModelSource(
            name = "Liquid AI",
            url = LIQUID_AI_URL,
            whyTrusted = "Official Liquid AI organisation: upstream LFM2.5 weights and licence terms.",
            families = listOf("liquid ai", "lfm2.5")
        ),
        ModelSource(
            name = "Microsoft BitNet",
            url = MICROSOFT_BITNET_URL,
            whyTrusted = "Official Microsoft BitNet b1.58 2B4T repository with the upstream MIT licence.",
            families = listOf("bitnet", "bitnet ternary")
        ),
        ModelSource(
            name = "Hugging Face GGUF catalogue",
            url = HUGGING_FACE_GGUF_SEARCH_URL,
            whyTrusted = "Hugging Face's own GGUF-filtered model search; check each model card for provenance and licence.",
            families = listOf("gguf", "quantized")
        )
    )

    /** Suggested browsing origin for a verified artifact, matched on family or attribution. */
    fun sourceFor(spec: DownloadableModelSpec): ModelSource? = forFamily(
        "${spec.family} ${spec.attribution}"
    )

    fun forFamily(family: String): ModelSource? {
        val needle = family.lowercase()
        return ALL.firstOrNull { source ->
            source.families.any { token -> needle.contains(token.lowercase()) }
        }
    }
}
