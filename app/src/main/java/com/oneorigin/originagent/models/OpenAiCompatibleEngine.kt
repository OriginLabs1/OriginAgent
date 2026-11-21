package com.oneorigin.originagent.models

import com.oneorigin.originagent.util.RemoteModelConfigStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Result of an explicit "Test connection" action. Never inferred from saved settings alone. */
data class ConnectionCheck(
    val ok: Boolean,
    val detail: String,
    val models: List<String> = emptyList()
)

/**
 * Real adapter for any documented OpenAI-compatible /v1 endpoint.
 * It never fakes availability or identity: a saved endpoint is only a candidate until a
 * request succeeds, and responses are attributed to the exact model name that served them.
 */
class OpenAiCompatibleEngine(
    private val descriptor: ModelDescriptor,
    private val configStore: RemoteModelConfigStore,
    private val webAccess: () -> Boolean = { true }
) : InferenceEngine {

    override val id: String get() = descriptor.id
    override val displayName: String get() = descriptor.displayName
    override val description: String get() = descriptor.description
    override val capabilities: Set<String> get() = descriptor.capabilities

    val modelName: String get() = config().modelName
    val endpoint: String get() = config().endpoint

    private fun config() = configStore.get(
        id,
        descriptor.defaultEndpoint.orEmpty(),
        descriptor.modelName
    )

    override fun isAvailable(): Boolean = webAccess() && config().isReady

    override suspend fun generate(request: GenerationRequest): GenerationResult =
        withContext(Dispatchers.IO) {
            val config = config()
            if (!webAccess()) {
                return@withContext GenerationResult.Unavailable(
                    id,
                    "Web access is switched off in Settings. Enable it to use this model."
                )
            }
            if (!config.isReady) {
                return@withContext GenerationResult.Unavailable(
                    id,
                    "Configure a valid endpoint and model name in the model picker first."
                )
            }

            val system = buildString {
                append("You are Origin Agent, a helpful and honest assistant. ")
                append("Be concise and accurate; if you do not know, say so.")
                if (request.context.isNotEmpty()) {
                    append(" Relevant local context: ")
                    append(request.context.joinToString(" | ") { it.take(400) })
                }
            }
            val body = OpenAiProtocol.requestJson(
                config.modelName,
                system,
                request.prompt,
                request.maxTokens
            )

            var connection: HttpURLConnection? = null
            try {
                connection = URL("${config.endpoint.trimEnd('/')}/chat/completions")
                    .openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    connectTimeout = 8_000
                    readTimeout = 30_000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    if (config.apiKey.isNotBlank()) {
                        setRequestProperty("Authorization", "Bearer ${config.apiKey}")
                    }
                    doOutput = true
                }
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val payload = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

                if (code !in 200..299) {
                    val detail = OpenAiProtocol.errorMessage(payload) ?: "Endpoint returned HTTP $code"
                    return@withContext GenerationResult.Failure(id, "$detail (HTTP $code)")
                }
                val text = OpenAiProtocol.responseText(payload)
                if (text.isNullOrBlank()) {
                    return@withContext GenerationResult.Failure(id, "Endpoint returned no message content.")
                }
                GenerationResult.Success(text, id)
            } catch (error: IOException) {
                GenerationResult.Failure(id, "Connection failed: ${error.message ?: "network error"}")
            } catch (error: Exception) {
                GenerationResult.Failure(id, "Request failed: ${error.message ?: "unknown error"}")
            } finally {
                connection?.disconnect()
            }
        }

    /** Explicit, user-triggered check against the configured endpoint's /models route. */
    suspend fun checkConnection(): ConnectionCheck = withContext(Dispatchers.IO) {
        val config = config()
        if (!webAccess()) {
            return@withContext ConnectionCheck(false, "Web access is switched off in Settings.")
        }
        if (!config.isReady) {
            return@withContext ConnectionCheck(false, "Endpoint and model name are not configured yet.")
        }
        var connection: HttpURLConnection? = null
        try {
            connection = URL("${config.endpoint.trimEnd('/')}/models").openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
                if (config.apiKey.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer ${config.apiKey}")
                }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val payload = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val detail = OpenAiProtocol.errorMessage(payload) ?: "HTTP $code"
                return@withContext ConnectionCheck(false, "Rejected: $detail")
            }
            val models = OpenAiProtocol.modelNames(payload)
            ConnectionCheck(
                ok = true,
                detail = if (models.isEmpty()) {
                    "Connected · ${config.endpoint} · ${config.modelName}"
                } else {
                    "Connected · ${models.size} model(s) · ${config.modelName}"
                },
                models = models
            )
        } catch (error: IOException) {
            ConnectionCheck(false, "Could not reach ${config.endpoint}: ${error.message ?: "network error"}")
        } catch (error: Exception) {
            ConnectionCheck(false, "Check failed: ${error.message ?: "unknown error"}")
        } finally {
            connection?.disconnect()
        }
    }
}
