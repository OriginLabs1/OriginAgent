package com.oneorigin.originagent.models

/** How a model actually runs. Display identity always follows the real serving engine. */
enum class ModelKind {
    LOCAL_DETERMINISTIC,
    LOCAL_NATIVE,
    REMOTE_OPENAI_COMPATIBLE
}

/**
 * A user-visible model choice. Every field describes real behaviour: no model is presented
 * as available unless its adapter can actually serve the conversation.
 */
data class ModelDescriptor(
    val id: String,
    val displayName: String,
    val modelName: String,
    val provider: String,
    val providerId: String,
    val kind: ModelKind,
    val description: String,
    val capabilities: Set<String>,
    val limitations: String,
    val defaultEndpoint: String? = null,
    val requiresApiKey: Boolean = false,
    val docsUrl: String? = null,
    val researchOnly: Boolean = false,
    /** Present only when Origin can fetch, verify and run the weights by itself. */
    val downloadSpec: DownloadableModelSpec? = null,
    /**
     * False for the deterministic tool engine. Chat is only ever conducted with a real AI
     * model the user downloaded with "Get Model" or explicitly configured as a web adapter.
     */
    val chatCapable: Boolean = true
) {
    val isRemote: Boolean get() = kind == ModelKind.REMOTE_OPENAI_COMPATIBLE
    val isDownloadable: Boolean get() = downloadSpec != null

    val kindLabel: String get() = when (kind) {
        ModelKind.LOCAL_DETERMINISTIC -> "Tool engine"
        ModelKind.LOCAL_NATIVE -> "Local GGUF"
        ModelKind.REMOTE_OPENAI_COMPATIBLE -> "Remote endpoint"
    }
}

/** Selectable models. Every local entry is a real GGUF that Origin downloads and runs. */
object ModelCatalog {

    /**
     * The deterministic engine still powers tools and status, but it is deliberately not
     * chat-capable: conversation is only with a real downloaded or configured AI model.
     */
    val LOCAL = ModelDescriptor(
        id = "local.deterministic",
        displayName = "Origin Tool Engine",
        modelName = "origin-local-deterministic",
        provider = "OneOrigin",
        providerId = "oneorigin",
        kind = ModelKind.LOCAL_DETERMINISTIC,
        description = "Deterministic offline tools and status; it does not hold conversations.",
        capabilities = setOf("offline", "tools", "knowledge", "trust"),
        limitations = "Rule-based tool engine, not a language model. Get a model to chat.",
        chatCapable = false,
        docsUrl = "https://github.com/OriginLabs1/OriginAgent/blob/main/docs/MODEL_REGISTRY.md"
    )

    private fun localModel(spec: DownloadableModelSpec) = ModelDescriptor(
        id = spec.modelId,
        displayName = spec.label,
        modelName = spec.format,
        provider = "OneOrigin · verified download",
        providerId = "oneorigin.local",
        kind = ModelKind.LOCAL_NATIVE,
        description = "${spec.family}. ${spec.summary} Origin downloads, checks the SHA-256 and runs it on-device.",
        capabilities = setOf("chat", "offline", "local", "downloadable", "gguf"),
        limitations = "${spec.license}. ${spec.summary}",
        docsUrl = spec.licenseUrl,
        downloadSpec = spec
    )

    val BITNET = localModel(DownloadableModels.BITNET_2B4T)
    val LFM2_5 = localModel(DownloadableModels.LFM2_5_350M)
    val SUB_1B = localModel(DownloadableModels.SUB_1B)
    val QWEN = localModel(DownloadableModels.QWEN_COMPACT)

    /** The four strategic families, in the order the constitution lists them. */
    val LOCAL_MODELS: List<ModelDescriptor> = listOf(BITNET, LFM2_5, SUB_1B, QWEN)

    val OPENAI = ModelDescriptor(
        id = "remote.openai",
        displayName = "OpenAI-compatible",
        modelName = "gpt-4o-mini",
        provider = "OpenAI (or compatible endpoint)",
        providerId = "openai",
        kind = ModelKind.REMOTE_OPENAI_COMPATIBLE,
        description = "Any OpenAI-style /v1 endpoint. You choose the endpoint, model name and credential.",
        capabilities = setOf("chat", "code", "remote"),
        limitations = "Uses the network only after you configure it. Capabilities depend on the model you name.",
        defaultEndpoint = "https://api.openai.com/v1",
        requiresApiKey = true,
        docsUrl = "https://platform.openai.com/docs/api-reference/chat/create"
    )

    val OLLAMA = ModelDescriptor(
        id = "remote.ollama",
        displayName = "Ollama",
        modelName = "llama3.2",
        provider = "Ollama local server",
        providerId = "ollama",
        kind = ModelKind.REMOTE_OPENAI_COMPATIBLE,
        description = "Connect to an Ollama OpenAI-compatible server running on your machine or network.",
        capabilities = setOf("local-network", "chat", "remote"),
        limitations = "Requires a reachable Ollama server and a pulled model; no key by default.",
        defaultEndpoint = "http://127.0.0.1:11434/v1",
        docsUrl = "https://docs.ollama.com/api/openai-compatibility"
    )

    val LM_STUDIO = ModelDescriptor(
        id = "remote.lmstudio",
        displayName = "LM Studio",
        modelName = "local-model",
        provider = "LM Studio local server",
        providerId = "lmstudio",
        kind = ModelKind.REMOTE_OPENAI_COMPATIBLE,
        description = "Connect to LM Studio's local OpenAI-compatible server.",
        capabilities = setOf("local-network", "chat", "remote"),
        limitations = "Requires LM Studio running with a model loaded.",
        defaultEndpoint = "http://127.0.0.1:1234/v1",
        docsUrl = "https://lmstudio.ai/docs/app/api/endpoints/openai"
    )

    val OPENROUTER = ModelDescriptor(
        id = "remote.openrouter",
        displayName = "OpenRouter",
        modelName = "openai/gpt-4o-mini",
        provider = "OpenRouter",
        providerId = "openrouter",
        kind = ModelKind.REMOTE_OPENAI_COMPATIBLE,
        description = "Route to many providers through one OpenAI-compatible endpoint you control.",
        capabilities = setOf("chat", "code", "remote"),
        limitations = "Requires an API key; availability and licensing follow the chosen upstream model.",
        defaultEndpoint = "https://openrouter.ai/api/v1",
        requiresApiKey = true,
        docsUrl = "https://openrouter.ai/docs/api-reference"
    )

    val CUSTOM = ModelDescriptor(
        id = "remote.custom",
        displayName = "Custom endpoint",
        modelName = "model-name",
        provider = "User configured",
        providerId = "custom",
        kind = ModelKind.REMOTE_OPENAI_COMPATIBLE,
        description = "Bring your own documented OpenAI-compatible /v1 endpoint and model name.",
        capabilities = setOf("chat", "remote", "user-configured"),
        limitations = "Only use endpoints you trust. Credentials stay on this device.",
        defaultEndpoint = "",
        docsUrl = "https://github.com/OriginLabs1/OriginAgent/blob/main/docs/MODEL_REGISTRY.md"
    )

    val REMOTE_MODELS: List<ModelDescriptor> = listOf(OPENAI, OLLAMA, LM_STUDIO, OPENROUTER, CUSTOM)

    val ALL: List<ModelDescriptor> = listOf(LOCAL) + LOCAL_MODELS + REMOTE_MODELS

    /** Models that can actually hold a conversation. The tool engine is excluded. */
    val CHAT_MODELS: List<ModelDescriptor> = ALL.filter { it.chatCapable }

    fun byId(id: String): ModelDescriptor? = ALL.firstOrNull { it.id == id }
}
