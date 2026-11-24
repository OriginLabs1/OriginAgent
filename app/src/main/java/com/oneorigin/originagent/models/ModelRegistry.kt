package com.oneorigin.originagent.models

import android.net.Uri
import com.oneorigin.originagent.util.ModelDownloadManager
import com.oneorigin.originagent.util.RemoteModelConfig
import com.oneorigin.originagent.util.RemoteModelConfigStore
import com.oneorigin.originagent.util.SettingsStore
import com.oneorigin.originagent.util.StoredAdapter
import kotlinx.coroutines.CoroutineScope

/**
 * One registry, one honest rule: conversation is served only by a real AI model — a verified
 * local GGUF the user downloaded, a GGUF they imported, or an explicitly configured web
 * adapter. The deterministic tool engine is never allowed to answer a chat message.
 */
class ModelRegistry(
    private val settings: SettingsStore,
    private val remoteConfig: RemoteModelConfigStore,
    private val downloads: ModelDownloadManager,
    private val imported: ImportedModelStore
) {
    private val local = LocalDeterministicEngine()
    private val webAccess: () -> Boolean = { settings.webAccessEnabled }

    private fun localEngines(): List<InferenceEngine> = ModelCatalog.LOCAL_MODELS.map { descriptor ->
        val spec = descriptor.downloadSpec!!
        LlamaEngine(spec, downloads.path(spec))
    }

    /** Imported GGUFs are real local models: no rule-based fallback, no network. */
    val importedModels: List<ImportedModel>
        get() = imported.models()

    private fun importedDescriptors(): List<ModelDescriptor> =
        importedModels.map(ImportedModels::descriptorFor)

    private fun importedEngines(): List<InferenceEngine> = importedModels.map { model ->
        ImportedModelEngine(model, imported.path(model))
    }

    private fun builtInRemote(): List<InferenceEngine> = ModelCatalog.REMOTE_MODELS
        .map { OpenAiCompatibleEngine(it, remoteConfig, webAccess) }

    private fun addedAdapters(): List<StoredAdapter> = remoteConfig.adapters()

    private fun addedDescriptors(): List<ModelDescriptor> = addedAdapters().map { adapter ->
        ModelDescriptor(
            id = adapter.id,
            displayName = adapter.label.ifBlank { "Added adapter" },
            modelName = adapter.modelName,
            provider = "Added adapter",
            providerId = "added",
            kind = ModelKind.REMOTE_OPENAI_COMPATIBLE,
            description = "OpenAI-compatible adapter at ${adapter.endpoint}",
            capabilities = setOf("chat", "remote", "user-added"),
            limitations = "Availability and licensing follow the endpoint you added.",
            defaultEndpoint = adapter.endpoint,
            requiresApiKey = adapter.apiKey.isNotBlank(),
            docsUrl = "https://github.com/OriginLabs1/OriginAgent/blob/main/docs/MODEL_REGISTRY.md"
        )
    }

    private fun addedRemote(): List<InferenceEngine> = addedDescriptors().map { descriptor ->
        OpenAiCompatibleEngine(descriptor, remoteConfig, webAccess)
    }

    /** Rebuilt on access so newly added adapters and downloaded weights appear immediately. */
    val engines: List<InferenceEngine>
        get() = listOf(local) + localEngines() + importedEngines() + builtInRemote() + addedRemote()

    val descriptors: List<ModelDescriptor>
        get() = ModelCatalog.ALL + importedDescriptors() + addedDescriptors()

    /** Only real AI engines may be selected for conversation. */
    val chatEngines: List<InferenceEngine>
        get() = engines.filter { descriptor(it.id)?.chatCapable == true }

    fun byId(id: String): InferenceEngine? = engines.firstOrNull { it.id == id }

    fun descriptor(id: String): ModelDescriptor? = descriptors.firstOrNull { it.id == id }

    fun isAvailable(id: String): Boolean = descriptor(id)?.chatCapable == true &&
        byId(id)?.isAvailable() == true

    /**
     * The real model that will answer chat, or null when the user must Get Model first.
     * An explicit user choice wins; otherwise any available local GGUF may take over — a
     * verified download or an imported model, but never the deterministic tool engine.
     * Remote adapters are never a silent fallback — the user has to select one after
     * configuring it.
     */
    fun selectedChat(): InferenceEngine? {
        val preferred = byId(settings.preferredEngineId)
        if (preferred != null && descriptor(preferred.id)?.chatCapable == true && preferred.isAvailable()) {
            return preferred
        }
        return chatEngines.firstOrNull { engine ->
            descriptor(engine.id)?.kind == ModelKind.LOCAL_NATIVE && engine.isAvailable()
        }
    }

    /** For status display only; never used to generate a chat answer. */
    fun selected(): InferenceEngine = selectedChat() ?: local

    fun selectedDescriptor(): ModelDescriptor =
        descriptor(selected().id) ?: ModelCatalog.LOCAL

    fun hasChatModel(): Boolean = selectedChat() != null

    fun select(id: String): Boolean {
        val engine = byId(id)?.takeIf { descriptor(it.id)?.chatCapable == true } ?: return false
        if (!engine.isAvailable()) return false
        settings.preferredEngineId = id
        return true
    }

    suspend fun checkConnection(id: String): ConnectionCheck {
        val engine = byId(id) as? OpenAiCompatibleEngine
            ?: return ConnectionCheck(false, "This model does not use a remote endpoint.")
        return engine.checkConnection()
    }

    fun configFor(id: String): RemoteModelConfig? {
        val descriptor = descriptor(id) ?: return null
        return remoteConfig.get(id, descriptor.defaultEndpoint.orEmpty(), descriptor.modelName)
    }

    fun saveRemoteConfig(id: String, endpoint: String, modelName: String, apiKey: String): Boolean {
        val descriptor = descriptor(id)?.takeIf { it.isRemote } ?: return false
        remoteConfig.save(descriptor.id, endpoint, modelName, apiKey)
        return true
    }

    fun addAdapter(label: String, endpoint: String, modelName: String, apiKey: String): String? {
        if (endpoint.isBlank() || modelName.isBlank()) return null
        return remoteConfig.addAdapter(label, endpoint, modelName, apiKey).id
    }

    fun removeAdapter(id: String) {
        if (!id.startsWith("adapter.")) return
        remoteConfig.removeAdapter(id)
        if (settings.preferredEngineId == id) settings.preferredEngineId = ""
    }

    // ---- User-imported GGUF files (stored in app-private storage) ------------------------

    /** Copies and hashes the content URI on IO; failure is returned, never faked as a model. */
    suspend fun importGguf(uri: Uri, displayName: String? = null): Result<ImportedModel> =
        imported.import(uri, displayName)

    fun isImported(id: String): Boolean = imported.model(id) != null

    fun removeImported(id: String) {
        if (!isImported(id)) return
        imported.remove(id)
        LlamaRuntime.release(id)
        if (settings.preferredEngineId == id) settings.preferredEngineId = ""
    }

    // ---- Verified model downloads -------------------------------------------------------

    fun downloadSpec(id: String): DownloadableModelSpec? = DownloadableModels.forModel(id)

    fun downloadStatus(id: String): ModelDownloadManager.Status? =
        DownloadableModels.forModel(id)?.let { downloads.status(it) }

    fun isDownloaded(id: String): Boolean =
        imported.model(id)?.let { imported.isReady(it) }
            ?: DownloadableModels.forModel(id)?.let { downloads.isReady(it) }
            ?: false

    fun download(id: String, scope: CoroutineScope) {
        DownloadableModels.forModel(id)?.let { downloads.download(it, scope) }
    }

    fun cancelDownload(id: String) {
        DownloadableModels.forModel(id)?.let { downloads.cancel(it) }
    }

    fun deleteWeights(id: String) {
        DownloadableModels.forModel(id)?.let { downloads.delete(it) }
        if (settings.preferredEngineId == id) settings.preferredEngineId = ""
    }

    fun summary(): String = buildString {
        append("Models (chat only uses a real downloaded, imported or configured AI model):\n")
        chatEngines.forEach { engine ->
            val descriptor = descriptor(engine.id)
            val status = if (engine.isAvailable()) "available" else "not downloaded / not configured"
            append("• ${engine.displayName} — ${descriptor?.provider ?: "unknown provider"} — $status\n")
        }
        append("Active: ${selectedChat()?.displayName ?: "none — tap Get Model"}")
    }
}
