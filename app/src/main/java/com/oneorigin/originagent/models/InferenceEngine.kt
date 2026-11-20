package com.oneorigin.originagent.models

data class GenerationRequest(
    val prompt: String,
    val context: List<String> = emptyList(),
    val maxTokens: Int = 256
)

sealed class GenerationResult {
    data class Success(val text: String, val engineId: String) : GenerationResult()
    data class Unavailable(val engineId: String, val reason: String) : GenerationResult()
    data class Failure(val engineId: String, val error: String) : GenerationResult()
}

/** The only contract the harness knows. Engines are interchangeable and never mandatory. */
interface InferenceEngine {
    val id: String
    val displayName: String
    val description: String
    val capabilities: Set<String>
    fun isAvailable(): Boolean
    suspend fun generate(request: GenerationRequest): GenerationResult
}
