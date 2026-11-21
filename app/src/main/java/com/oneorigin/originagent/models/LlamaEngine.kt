package com.oneorigin.originagent.models

import java.io.File

/**
 * A real local language model: GGUF weights downloaded and SHA-256 verified by Origin,
 * executed through the bundled llama.cpp runtime. It never imitates an answer and never
 * silently falls back to the deterministic tool engine.
 */
class LlamaEngine(
    private val spec: DownloadableModelSpec,
    private val file: File
) : InferenceEngine {

    override val id: String = spec.modelId
    override val displayName: String = spec.label
    override val description: String = "${spec.summary} ${spec.attribution}."
    override val capabilities: Set<String> =
        setOf("chat", "offline", "local", "gguf", "downloadable")

    override fun isAvailable(): Boolean =
        LlamaRuntime.available() && file.exists() && file.length() == spec.sizeBytes

    override suspend fun generate(request: GenerationRequest): GenerationResult {
        if (!LlamaRuntime.available()) {
            return GenerationResult.Unavailable(
                id, "This device cannot load the arm64 local runtime."
            )
        }
        if (!file.exists() || file.length() != spec.sizeBytes) {
            return GenerationResult.Unavailable(
                id, "Verified weights are not installed. Tap Get Model first."
            )
        }
        return runCatching {
            val answer = LlamaRuntime.generate(spec, file, request.prompt, request.maxTokens)
            GenerationResult.Success(answer, id)
        }.getOrElse { error ->
            GenerationResult.Failure(id, error.message ?: "Local inference failed")
        }
    }
}
