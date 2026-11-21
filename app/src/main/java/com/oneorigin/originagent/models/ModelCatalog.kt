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
    val researchOnly: Boolean = false
) {
    val isRemote: Boolean get() = kind == ModelKind.REMOTE_OPENAI_COMPATIBLE

    val kindLabel: String get() = when (kind) {
        ModelKind.LOCAL_DETERMINISTIC -> "Always offline"
        ModelKind.LOCAL_NATIVE -> "Local runtime"
        ModelKind.REMOTE_OPENAI_COMPATIBLE -> "Remote endpoint"
    }
}

/** Selectable models. Candidates are research directions, not performance claims. */
object ModelCatalog {

    val LOCAL = ModelDescriptor(
        id = "local.deterministic",
        displayName = "Origin Local",
        modelName = "origin-local-deterministic",
        provider = "OneOrigin",
        providerId = "oneorigin",
        kind = ModelKind.LOCAL_DETERMINISTIC,
        description = "Deterministic offline reasoning. Works with no model and no network.",
        capabilities = setOf("offline", "tools", "knowledge", "trust"),
        limitations = "Rule-based answers; not a general-purpose language model.",
        docsUrl = "https://github.com/OriginLabs1/OriginAgent/blob/main/docs/MODEL_REGISTRY.md"
    )

    val BITNET = ModelDescriptor(
        id = "engine.bitnet.cpp",
        displayName = "bitnet.cpp",
        modelName = "bitnet-b1.58-2B-4T (user supplied)",
        provider = "Microsoft BitNet / bitnet.cpp",
        providerId = "bitnet.cpp",
        kind = ModelKind.LOCAL_NATIVE,
        description = "Ternary inference direction. Requires user-supplied weights and a compiled bridge.",
        capabilities = setOf("local", "ternary", "research"),
        limitations = "Optional adapter; exact ARM/kernel compatibility must be validated on-device.",
        docsUrl = "https://github.com/microsoft/BitNet",
        researchOnly = true
    )

    val LLAMA = ModelDescriptor(
        id = "engine.llama.cpp",
        displayName = "llama.cpp",
        modelName = "GGUF (user supplied)",
        provider = "ggml / llama.cpp",
        providerId = "llama.cpp",
        kind = ModelKind.LOCAL_NATIVE,
        description = "Portable GGUF baseline runtime. Requires user-supplied weights and a compiled bridge.",
        capabilities = setOf("local", "gguf", "optional"),
        limitations = "Optional adapter; do not force incompatible models into this runtime.",
        docsUrl = "https://github.com/ggml-org/llama.cpp",
        researchOnly = true
    )

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

    val ALL: List<ModelDescriptor> = listOf(
        LOCAL, BITNET, LLAMA, OPENAI, OLLAMA, LM_STUDIO, OPENROUTER, CUSTOM
    )

    fun byId(id: String): ModelDescriptor? = ALL.firstOrNull { it.id == id }
}
