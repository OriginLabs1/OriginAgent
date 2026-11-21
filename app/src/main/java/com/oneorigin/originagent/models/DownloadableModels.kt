package com.oneorigin.originagent.models

/**
 * A model weight file that Origin Agent can fetch, verify and activate by itself.
 *
 * Every entry is a real GGUF artifact at a stable public URL with a pinned SHA-256 and size.
 * Nothing here asks the user to find or supply weights: one "Get Model" tap downloads the
 * file, the checksum is verified, and the matching llama.cpp engine becomes selectable.
 * The four families below are the strategic shortlist from the constitution; each now has
 * an actual downloadable, runnable artifact rather than a research note.
 */
data class DownloadableModelSpec(
    val modelId: String,
    val label: String,
    val family: String,
    val fileName: String,
    val url: String,
    val sha256: String,
    val sizeBytes: Long,
    val format: String,
    val license: String,
    val licenseUrl: String,
    val attribution: String,
    /** Conversation context window used when the model is loaded. */
    val contextSize: Int = 2048,
    /** Short, honest description shown in the picker. */
    val summary: String
)

object DownloadableModels {

    /**
     * Microsoft's MIT-licensed BitNet b1.58 2B4T weights, converted to llama.cpp-compatible
     * TQ1_0 GGUF. The upstream i2_s quant uses a tensor type the Android runtime cannot load;
     * this artifact preserves the same 2B4T weights in a supported quantization.
     */
    val BITNET_2B4T = DownloadableModelSpec(
        modelId = "local.bitnet.b1.58.2b4t",
        label = "BitNet b1.58 2B4T",
        family = "BitNet ternary",
        fileName = "bitnet-b1.58-2B-4T-tq1_0.gguf",
        url = "https://huggingface.co/ihtesham0345/bitnet-b1.58-2B-4T-llamakind/resolve/main/bitnet-b1.58-2B-4T-tq1_0.gguf",
        sha256 = "db8852edfaa08476ab8476052c7777990cf73df16f2618604115a2c954e06f3a",
        sizeBytes = 1_105_874_048L,
        format = "gguf/tq1_0",
        license = "MIT (upstream weights)",
        licenseUrl = "https://huggingface.co/microsoft/bitnet-b1.58-2B-4T/blob/main/LICENSE",
        attribution = "Microsoft BitNet b1.58 2B4T; llama.cpp-compatible GGUF conversion by ihtesham0345",
        contextSize = 2048,
        summary = "Ternary 2B model. Largest download; needs roughly 1.3 GB of free RAM."
    )

    /** Liquid AI LFM2.5 350M — compact edge model in a standard Q4_K_M GGUF. */
    val LFM2_5_350M = DownloadableModelSpec(
        modelId = "local.lfm2.5.350m",
        label = "LFM2.5 350M",
        family = "LFM2.5-class",
        fileName = "LFM2.5-350M-Q4_K_M.gguf",
        url = "https://huggingface.co/LiquidAI/LFM2.5-350M-GGUF/resolve/main/LFM2.5-350M-Q4_K_M.gguf",
        sha256 = "7e6f72643caafc9a68256686638c4d7916f2cec76d1df478d4c3ddcd95a6aed4",
        sizeBytes = 229_312_224L,
        format = "gguf/q4_k_m",
        license = "LFM 1.0",
        licenseUrl = "https://huggingface.co/LiquidAI/LFM2.5-350M/blob/main/LICENSE",
        attribution = "Liquid AI LFM2.5-350M",
        contextSize = 2048,
        summary = "Efficient compact model. Fast on 4 GB devices; about 220 MB download."
    )

    /** Optimized sub-1B baseline: SmolLM2 360M instruct, Q4_K_M. */
    val SUB_1B = DownloadableModelSpec(
        modelId = "local.smollm2.360m",
        label = "Optimized sub-1B · SmolLM2",
        family = "Optimized sub-1B",
        fileName = "SmolLM2-360M-Instruct-Q4_K_M.gguf",
        url = "https://huggingface.co/lmstudio-community/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q4_K_M.gguf",
        sha256 = "e55c2a5df7f8f6eed1a843ff709fa8008dbce65d82e57c5c06dec5ee8d060415",
        sizeBytes = 270_590_592L,
        format = "gguf/q4_k_m",
        license = "Apache-2.0",
        licenseUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct/blob/main/LICENSE",
        attribution = "Hugging Face TB SmolLM2-360M-Instruct",
        contextSize = 4096,
        summary = "Everyday responsiveness on 4 GB phones; about 260 MB download."
    )

    /** Qwen-class compact: Qwen2.5 0.5B instruct, Q4_K_M. */
    val QWEN_COMPACT = DownloadableModelSpec(
        modelId = "local.qwen2.5.0.5b",
        label = "Qwen-class compact · 0.5B",
        family = "Qwen-class compact",
        fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
        url = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
        sha256 = "74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db",
        sizeBytes = 491_400_032L,
        format = "gguf/q4_k_m",
        license = "Apache-2.0",
        licenseUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct/blob/main/LICENSE",
        attribution = "Qwen Team Qwen2.5-0.5B-Instruct",
        contextSize = 2048,
        summary = "Broad-capability baseline; about 470 MB download."
    )

    /** Ordered as the constitution lists the strategic families. */
    val ALL: List<DownloadableModelSpec> = listOf(
        BITNET_2B4T, LFM2_5_350M, SUB_1B, QWEN_COMPACT
    )

    fun forModel(modelId: String): DownloadableModelSpec? = ALL.firstOrNull { it.modelId == modelId }
}
