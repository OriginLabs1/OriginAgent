package com.oneorigin.originagent.models

import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

    /** Releases are queued on IO behind any generation holding [mutex], never on the UI thread. */
    private val releaseScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

        val systemPrompt = "You are OpenDuet. Answer in at most 35 words: one to three short " +
            "sentences, each thought its own paragraph, in first person. No headings, tables " +
            "or bold labels. Never claim to have read a source unless a tool returned it. " +
            "Say when unsure."
        val result = Llama.complete(active, prompt, systemPrompt, maxTokens)
        result.text.trim().ifBlank { "The model returned an empty response." }
    }

    /**
     * Called when weights are removed so stale native memory is never kept. Serialized with
     * [generate] through the same mutex: a release can never free a model mid-generation,
     * and callers on the UI thread return immediately.
     */
    fun release(modelId: String? = null) {
        releaseScope.launch {
            mutex.withLock {
                val current = loaded ?: return@withLock
                if (modelId != null && current.modelId != modelId) return@withLock
                runCatching { Llama.releaseModel(current.model) }
                loaded = null
            }
        }
    }
}
