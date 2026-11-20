package com.oneorigin.originagent.models

import com.oneorigin.originagent.util.NativeBridge

/**
 * Adapters for optional runtimes. They never fake availability: an engine reports itself
 * available only when a real native generation bridge has been compiled in.
 * Model weights are always user-supplied and are never packaged with the app.
 */
abstract class OptionalEngine(
    override val id: String,
    override val displayName: String,
    override val description: String,
    override val capabilities: Set<String>,
    private val libraryName: String
) : InferenceEngine {
    override fun isAvailable(): Boolean = NativeBridge.isEngineSupported(libraryName)

    override suspend fun generate(request: GenerationRequest): GenerationResult {
        if (!isAvailable()) {
            return GenerationResult.Unavailable(id, "$libraryName is not installed or has no generation bridge.")
        }
        // The scaffold does not pretend to generate. Wire a verified native bridge here
        // only after benchmarks and licensing are recorded in docs/MODEL_REGISTRY.md.
        return GenerationResult.Failure(id, "Generation bridge for $libraryName is not enabled in this build.")
    }
}

class BitnetCppEngine : OptionalEngine(
    id = "engine.bitnet.cpp",
    displayName = "bitnet.cpp",
    description = "Ternary inference direction (BitNet b1.58 2B4T). Requires user-supplied weights.",
    capabilities = setOf("ternary", "local", "optional"),
    libraryName = "bitnet_bridge"
)

class LlamaCppEngine : OptionalEngine(
    id = "engine.llama.cpp",
    displayName = "llama.cpp",
    description = "Optional GGUF baseline runtime. Do not force incompatible models into it.",
    capabilities = setOf("gguf", "local", "optional"),
    libraryName = "llama_bridge"
)
