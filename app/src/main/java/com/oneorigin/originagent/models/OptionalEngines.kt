package com.oneorigin.originagent.models

import com.oneorigin.originagent.util.NativeBridge

/**
 * Research-only native adapters. They are deliberately NOT registered in the user-facing
 * model catalog: Origin never suggests a runtime whose weights cannot be downloaded and
 * verified in-app. These scaffolds exist only for future evidence-gated native bridges.
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
    description = "Research bridge for ternary inference (BitNet b1.58 2B4T). Not offered in the picker.",
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
