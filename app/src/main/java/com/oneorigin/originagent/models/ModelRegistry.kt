package com.oneorigin.originagent.models

import com.oneorigin.originagent.util.RemoteModelConfig
import com.oneorigin.originagent.util.RemoteModelConfigStore
import com.oneorigin.originagent.util.SettingsStore
import com.oneorigin.originagent.util.StoredAdapter

/**
 * One registry, one honest selection rule: prefer the user's choice when its adapter is
 * genuinely available, otherwise fall back to the always-present local engine.
 * Web access is possible and enabled by default, but the user can switch it off; no
 * network call is ever made for a local model or without an explicitly added/configured adapter.
 */
class ModelRegistry(
    private val settings: SettingsStore,
    private val remoteConfig: RemoteModelConfigStore
) {
    private val local = LocalDeterministicEngine()
    private val native: List<InferenceEngine> = listOf(BitnetCppEngine(), LlamaCppEngine())
    private val webAccess: () -> Boolean = { settings.webAccessEnabled }

    private fun builtInRemote(): List<InferenceEngine> = ModelCatalog.ALL
        .filter { it.isRemote }
        .map { OpenAiCompatibleEngine(it, remoteConfig, webAccess) }

    /** Adapters added in the UI — built-in web templates or fully custom endpoints. */
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

    /** Rebuilt on access so newly added adapters appear immediately. */
    val engines: List<InferenceEngine>
        get() = listOf(local) + native + builtInRemote() + addedRemote()

    val descriptors: List<ModelDescriptor>
        get() = ModelCatalog.ALL + addedDescriptors()

    fun byId(id: String): InferenceEngine? = engines.firstOrNull { it.id == id }

    fun descriptor(id: String): ModelDescriptor? = descriptors.firstOrNull { it.id == id }

    fun isAvailable(id: String): Boolean = byId(id)?.isAvailable() == true

    fun selected(): InferenceEngine =
        byId(settings.preferredEngineId)?.takeIf { it.isAvailable() } ?: local

    fun selectedDescriptor(): ModelDescriptor =
        descriptor(selected().id) ?: ModelCatalog.LOCAL

    fun select(id: String): Boolean {
        val engine = byId(id) ?: return false
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
        if (settings.preferredEngineId == id) settings.preferredEngineId = ModelCatalog.LOCAL.id
    }

    fun summary(): String = buildString {
        append("Models (selection follows the real serving engine):\n")
        engines.forEach { engine ->
            val descriptor = descriptor(engine.id)
            val status = if (engine.isAvailable()) "available" else "not configured"
            append("• ${engine.displayName} — ${descriptor?.provider ?: "unknown provider"} — $status\n")
        }
        append("Active: ${selectedDescriptor().displayName}")
    }
}
