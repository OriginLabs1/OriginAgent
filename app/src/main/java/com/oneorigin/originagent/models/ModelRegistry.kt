package com.oneorigin.originagent.models

import com.oneorigin.originagent.util.SettingsStore

/** All engines, and one honest selection rule: prefer the user's choice if available. */
class ModelRegistry(private val settings: SettingsStore) {
    private val local = LocalDeterministicEngine()
    val engines: List<InferenceEngine> = listOf(BitnetCppEngine(), LlamaCppEngine(), local)

    fun byId(id: String): InferenceEngine? = engines.firstOrNull { it.id == id }

    fun selected(): InferenceEngine =
        byId(settings.preferredEngineId)?.takeIf { it.isAvailable() } ?: local

    fun select(id: String): Boolean {
        val engine = byId(id) ?: return false
        if (!engine.isAvailable()) return false
        settings.preferredEngineId = id
        return true
    }

    fun summary(): String = buildString {
        append("Intelligence engines (no weights bundled):\n")
        engines.forEach {
            append("• ${it.displayName}: ${if (it.isAvailable()) "available" else "not installed"} — ${it.description}\n")
        }
        append("Active: ${selected().displayName}")
    }
}
