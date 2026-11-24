package com.oneorigin.originagent.util

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

/** Connection settings for remote models. Kept local and never included in exports verbatim. */
data class RemoteModelConfig(
    val endpoint: String,
    val modelName: String,
    val apiKey: String
) {
    val hasEndpoint: Boolean
        get() = endpoint.startsWith("http://", ignoreCase = true) ||
            endpoint.startsWith("https://", ignoreCase = true)

    val isReady: Boolean get() = hasEndpoint && modelName.isNotBlank()

    val maskedKey: String
        get() = if (apiKey.isBlank()) "not set" else "••••" + apiKey.takeLast(4)
}

/** An adapter the user added. It may use a built-in web template or a custom endpoint. */
data class StoredAdapter(
    val id: String,
    val label: String,
    val endpoint: String,
    val modelName: String,
    val apiKey: String
)

/** Per-provider endpoint, model name and credential, stored only on this device. */
class RemoteModelConfigStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "origin_remote_models",
        Context.MODE_PRIVATE
    )

    fun get(id: String, defaultEndpoint: String, defaultModelName: String): RemoteModelConfig =
        RemoteModelConfig(
            endpoint = prefs.getString("$id.endpoint", defaultEndpoint) ?: defaultEndpoint,
            modelName = prefs.getString("$id.model", defaultModelName) ?: defaultModelName,
            apiKey = prefs.getString("$id.key", "") ?: ""
        )

    fun save(id: String, endpoint: String, modelName: String, apiKey: String) {
        prefs.edit {
            putString("$id.endpoint", endpoint.trim())
            putString("$id.model", modelName.trim())
            if (apiKey.isBlank()) remove("$id.key") else putString("$id.key", apiKey.trim())
        }
    }

    fun clear(id: String) {
        prefs.edit {
            remove("$id.endpoint")
            remove("$id.model")
            remove("$id.key")
        }
    }

    fun configuredIds(): Set<String> = prefs.all.keys
        .filter { it.endsWith(".endpoint") && !it.startsWith("adapter.") }
        .map { it.removeSuffix(".endpoint") }
        .toSet()

    /**
     * True only when the user actually saved settings for this adapter. Descriptor defaults
     * (the built-in endpoint templates) never count as explicit configuration: no web
     * request may happen just because a template exists.
     */
    fun isExplicitlyConfigured(id: String): Boolean =
        id.isNotBlank() && (prefs.contains("$id.endpoint") || adapters().any { it.id == id })

    /** Adapters added through "Add adapter" — not limited to the built-in list. */
    fun adapters(): List<StoredAdapter> {
        val ids = prefs.getString("adapter.ids", "").orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return ids.mapNotNull { id ->
            val endpoint = prefs.getString("$id.endpoint", "").orEmpty()
            if (endpoint.isBlank()) return@mapNotNull null
            StoredAdapter(
                id = id,
                label = prefs.getString("$id.label", "Added adapter").orEmpty(),
                endpoint = endpoint,
                modelName = prefs.getString("$id.model", "").orEmpty(),
                apiKey = prefs.getString("$id.key", "").orEmpty()
            )
        }
    }

    /** UUID-based ids stay unique across devices, restarts and rapid consecutive taps. */
    private fun nextAdapterId(): String {
        val used = prefs.getString("adapter.ids", "").orEmpty()
            .split(',')
            .map { it.trim() }
            .toSet()
        var id: String
        do {
            id = "adapter.${UUID.randomUUID()}"
        } while (id in used)
        return id
    }

    fun addAdapter(label: String, endpoint: String, modelName: String, apiKey: String): StoredAdapter {
        val id = nextAdapterId()
        prefs.edit {
            putString("$id.label", label.trim())
            putString("$id.endpoint", endpoint.trim())
            putString("$id.model", modelName.trim())
            if (apiKey.isBlank()) remove("$id.key") else putString("$id.key", apiKey.trim())
            putString("adapter.ids", (adapters().map { it.id } + id).joinToString(","))
        }
        return StoredAdapter(id, label.trim(), endpoint.trim(), modelName.trim(), apiKey.trim())
    }

    fun removeAdapter(id: String) {
        prefs.edit {
            remove("$id.label")
            remove("$id.endpoint")
            remove("$id.model")
            remove("$id.key")
            putString("adapter.ids", adapters().filter { it.id != id }.map { it.id }.joinToString(","))
        }
    }

    /** Safe for JSON export: endpoint and model name only, credential masked. */
    fun redactedMap(): Map<String, Any?> = buildMap {
        configuredIds().forEach { id ->
            val config = get(id, "", "")
            put("remoteModel.$id.endpoint", config.endpoint)
            put("remoteModel.$id.model", config.modelName)
            put("remoteModel.$id.apiKey", config.maskedKey)
        }
        adapters().forEach { adapter ->
            put("addedAdapter.${adapter.id}.label", adapter.label)
            put("addedAdapter.${adapter.id}.endpoint", adapter.endpoint)
            put("addedAdapter.${adapter.id}.model", adapter.modelName)
            put(
                "addedAdapter.${adapter.id}.apiKey",
                if (adapter.apiKey.isBlank()) "not set" else "••••" + adapter.apiKey.takeLast(4)
            )
        }
    }
}
