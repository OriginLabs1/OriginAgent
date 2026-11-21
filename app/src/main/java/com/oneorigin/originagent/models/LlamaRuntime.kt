package com.oneorigin.originagent.models

import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * The single on-device GGUF runtime used by every downloadable model.
 *
 * llama.cpp is memory hungry, so only one model is resident at a time. Switching models
 * releases the previous one before loading the new one; all native access is serialized
 * because a LlamaModel is not thread-safe.
 */
object LlamaRuntime {

    private data class Loaded(val modelId: String, val model: LlamaModel)

    private val mutex = Mutex()
    private var loaded: Loaded? = null
    private var threads: Int = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)

    @Volatile
    private var nativeChecked: Boolean? = null

    /** True when the bundled arm64 llama.cpp libraries can actually be loaded here. */
    fun available(): Boolean {
        nativeChecked?.let { return it }
        val ok = runCatching { Llama.getSystemInfo().isNotBlank() }
            .getOrDefault(false)
            .also { nativeChecked = it }
        return ok
    }

    fun systemInfo(): String = runCatching { Llama.getSystemInfo() }
        .getOrDefault("Native runtime unavailable on this device")

    fun setThreads(count: Int) {
        threads = count.coerceIn(1, 8)
    }

    /**
     * Generates a reply from a verified GGUF file. The model is loaded on first use and
     * reused afterwards; failures never fall back to a rule-based imitation of a chat.
     */
    suspend fun generate(
        spec: DownloadableModelSpec,
        file: File,
        prompt: String,
        maxTokens: Int = 256
    ): String = mutex.withLock {
        val current = loaded
        val active = if (current != null && current.modelId == spec.modelId) {
            current.model
        } else {
            current?.let { runCatching { Llama.releaseModel(it.model) } }
            loaded = null
            val config = LlamaConfig(
                contextSize = spec.contextSize,
                threads = threads,
                gpuLayers = 0,
                temperature = 0.7f,
                topP = 0.9f,
                topK = 40,
                seed = -1
            )
            val model = Llama.loadModel(file.absolutePath, config)
            loaded = Loaded(spec.modelId, model)
            model
        }

        val systemPrompt = "You are Origin Agent, a private assistant running on the user's " +
            "device. Answer directly, accurately and honestly. If you do not know, say so. " +
            "Never pretend to be a different model or a cloud service."
        val result = Llama.complete(active, prompt, systemPrompt, maxTokens)
        result.text.trim().ifBlank { "The model returned an empty response." }
    }

    /** Called when weights are removed so stale native memory is never kept. */
    fun release(modelId: String? = null) {
        val current = loaded ?: return
        if (modelId != null && current.modelId != modelId) return
        runCatching { Llama.releaseModel(current.model) }
        loaded = null
    }
}
