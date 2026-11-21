package com.oneorigin.originagent.models

/**
 * Built-in web adapter templates. "Add adapter" is not limited to user-supplied endpoints:
 * these first-party templates prefill a documented, OpenAI-compatible /v1 connection, and
 * a fully custom option remains available.
 */
data class AdapterTemplate(
    val id: String,
    val label: String,
    val endpoint: String,
    val model: String,
    val keyRequired: Boolean,
    val note: String
)

object AdapterTemplates {
    val ALL: List<AdapterTemplate> = listOf(
        AdapterTemplate(
            id = "openai",
            label = "OpenAI",
            endpoint = "https://api.openai.com/v1",
            model = "gpt-4o-mini",
            keyRequired = true,
            note = "Official OpenAI Chat Completions API."
        ),
        AdapterTemplate(
            id = "openrouter",
            label = "OpenRouter",
            endpoint = "https://openrouter.ai/api/v1",
            model = "openai/gpt-4o-mini",
            keyRequired = true,
            note = "One endpoint for many providers; licensing follows the upstream model."
        ),
        AdapterTemplate(
            id = "groq",
            label = "Groq",
            endpoint = "https://api.groq.com/openai/v1",
            model = "llama-3.3-70b-versatile",
            keyRequired = true,
            note = "Fast OpenAI-compatible inference."
        ),
        AdapterTemplate(
            id = "together",
            label = "Together AI",
            endpoint = "https://api.together.xyz/v1",
            model = "meta-llama/Llama-3.3-70B-Instruct-Turbo",
            keyRequired = true,
            note = "Hosted open-model inference."
        ),
        AdapterTemplate(
            id = "deepseek",
            label = "DeepSeek",
            endpoint = "https://api.deepseek.com/v1",
            model = "deepseek-chat",
            keyRequired = true,
            note = "OpenAI-compatible chat endpoint."
        ),
        AdapterTemplate(
            id = "ollama",
            label = "Ollama",
            endpoint = "http://127.0.0.1:11434/v1",
            model = "llama3.2",
            keyRequired = false,
            note = "Local server on your machine; no key needed by default."
        ),
        AdapterTemplate(
            id = "lmstudio",
            label = "LM Studio",
            endpoint = "http://127.0.0.1:1234/v1",
            model = "local-model",
            keyRequired = false,
            note = "Local server; load a model first."
        ),
        AdapterTemplate(
            id = "custom",
            label = "Custom web endpoint",
            endpoint = "",
            model = "",
            keyRequired = false,
            note = "Any documented OpenAI-compatible /v1 endpoint you trust."
        )
    )

    fun byId(id: String): AdapterTemplate? = ALL.firstOrNull { it.id == id }
}
