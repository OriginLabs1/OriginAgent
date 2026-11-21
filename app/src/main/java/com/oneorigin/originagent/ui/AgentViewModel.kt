package com.oneorigin.originagent.ui

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oneorigin.originagent.OriginAgentApplication
import com.oneorigin.originagent.domain.HarnessEvent
import com.oneorigin.originagent.domain.Intent
import com.oneorigin.originagent.domain.ExecutionStatus
import com.oneorigin.originagent.domain.IntentSource
import com.oneorigin.originagent.domain.PlanStep
import com.oneorigin.originagent.models.AdapterTemplates
import com.oneorigin.originagent.models.ModelCatalog
import com.oneorigin.originagent.models.ModelDescriptor
import com.oneorigin.originagent.util.Ids
import com.oneorigin.originagent.util.ModelDownloadManager
import com.oneorigin.originagent.util.JsonCodec
import com.oneorigin.originagent.util.ThemeMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val verified: Boolean = true,
    val status: String = "",
    /** The real model that produced this answer — never a simulated identity. */
    val engineLabel: String = ""
)

data class AgentUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            id = "welcome",
            text = "I'm Origin. Conversation happens only with a real AI model that you download here — tap Get Model and choose one. Your chats stay on this device, and every reply names the model that answered.",
            isUser = false
        )
    ),
    val busy: Boolean = false,
    val lastEvent: String = "Ready — offline and local",
    val engineLabel: String = "Origin Local",
    val nativeLabel: String = "Kotlin fallback",
    val memoryCount: Int = 0,
    val chainValid: Boolean = true,
    val pendingConsent: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val reducedMotion: Boolean = false,
    val autoApproveLowRisk: Boolean = true,
    val notice: String? = null,
    // v0.0.3 — real AI models only; the tool engine is not a chat option
    val models: List<ModelDescriptor> = ModelCatalog.CHAT_MODELS,
    val selectedModelId: String = "",
    val chatReady: Boolean = false,
    val nativeRuntimeReady: Boolean = false,
    val modelStatuses: Map<String, String> = emptyMap(),
    val modelPickerOpen: Boolean = false,
    val configuringModelId: String? = null,
    val configEndpoint: String = "",
    val configModelName: String = "",
    val configApiKey: String = "",
    val addAdapterOpen: Boolean = false,
    val adapterTemplateId: String = AdapterTemplates.ALL.first().id,
    val newAdapterLabel: String = "",
    val newAdapterEndpoint: String = "",
    val newAdapterModel: String = "",
    val newAdapterKey: String = "",
    val webAccessEnabled: Boolean = true,
    /** Per-model download progress; drives the picker's Download / Ready controls. */
    val downloadStatuses: Map<String, ModelDownloadManager.Status> = emptyMap()
)

class AgentViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<OriginAgentApplication>()
    private val _ui = mutableStateOf(AgentUiState())
    val ui: State<AgentUiState> = _ui

    private var consentDeferred: CompletableDeferred<Boolean>? = null

    private val downloadListener: (String, ModelDownloadManager.Status) -> Unit = { id, status ->
        _ui.value = _ui.value.copy(
            downloadStatuses = _ui.value.downloadStatuses + (id to status),
            modelStatuses = _ui.value.modelStatuses + (
                id to when (status.state) {
                    ModelDownloadManager.State.READY -> "Verified · ready to use offline"
                    ModelDownloadManager.State.DOWNLOADING -> "${status.progress}% of verified weights"
                    ModelDownloadManager.State.FAILED -> status.message
                    ModelDownloadManager.State.NOT_DOWNLOADED -> status.message
                }
                )
        )
        if (status.state == ModelDownloadManager.State.READY && !app.models.hasChatModel()) {
            app.models.select(id)
        }
        refreshStatus()
    }

    init {
        readSettings()
        app.consentRequest = { step -> requestConsent(step) }
        app.modelDownloads.addListener(downloadListener)
        refreshDownloads()
        refreshStatus()
    }

    private fun refreshDownloads() {
        val statuses = app.models.descriptors.mapNotNull { descriptor ->
            app.models.downloadStatus(descriptor.id)?.let { descriptor.id to it }
        }.toMap()
        _ui.value = _ui.value.copy(downloadStatuses = statuses)
    }

    /** One tap: fetch, verify and activate a model — no user-supplied weights. */
    fun downloadModel(id: String) {
        val descriptor = app.models.descriptor(id) ?: return
        val spec = descriptor.downloadSpec ?: return
        _ui.value = _ui.value.copy(
            modelStatuses = _ui.value.modelStatuses + (id to "Downloading ${app.modelDownloads.formatSize(spec.sizeBytes)} of verified weights…"),
            notice = "Getting ${descriptor.displayName} — the download is checksum-verified before chat can start."
        )
        app.models.download(id, viewModelScope)
    }

    fun cancelDownload(id: String) {
        app.models.cancelDownload(id)
        _ui.value = _ui.value.copy(
            modelStatuses = _ui.value.modelStatuses + (id to "Download cancelled")
        )
    }

    fun deleteWeights(id: String) {
        app.models.deleteWeights(id)
        _ui.value = _ui.value.copy(
            modelStatuses = _ui.value.modelStatuses + (id to "Weights removed — tap Get Model to fetch again"),
            models = app.models.descriptors
        )
        refreshDownloads()
        refreshStatus()
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _ui.value.busy) return
        if (!app.models.hasChatModel()) {
            _ui.value = _ui.value.copy(
                notice = "Get a model first — chatting is only with a real downloaded AI model."
            )
            openModelPicker()
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                busy = true,
                notice = null,
                messages = _ui.value.messages + ChatMessage(Ids.next("msg"), trimmed, isUser = true)
            )
            val intent = Intent(Ids.next("intent"), trimmed, IntentSource.USER)
            val result = app.harness.run(intent) { event -> onEvent(event) }
            val label = app.models.descriptor(result.engineId)?.displayName ?: result.engineId
            _ui.value = _ui.value.copy(
                busy = false,
                messages = _ui.value.messages + ChatMessage(
                    id = Ids.next("msg"),
                    text = result.answer,
                    isUser = false,
                    verified = result.verified,
                    engineLabel = label,
                    status = buildString {
                        append("${result.plan.steps.size} step(s) · ")
                        append("${result.outcomes.count { it.status == ExecutionStatus.VERIFIED }} verified · ")
                        append("${result.outcomes.count { it.status == ExecutionStatus.BLOCKED }} blocked")
                    }
                )
            )
            refreshStatus()
        }
    }

    fun capabilities() = app.tools.all()

    fun engines() = app.models.engines

    /** Select a real, available model. Messages are intentionally left untouched. */
    fun selectModel(id: String): Boolean {
        val descriptor = app.models.descriptor(id)
        val selected = app.models.select(id)
        _ui.value = _ui.value.copy(
            selectedModelId = if (selected) id else _ui.value.selectedModelId,
            modelPickerOpen = false,
            notice = if (selected) {
                "Now answering with ${descriptor?.displayName ?: id}. Conversation continues unchanged."
            } else {
                if (descriptor?.isDownloadable == true) {
                    "${descriptor.displayName} needs its weights first — tap Get Model in the picker."
                } else {
                    "${descriptor?.displayName ?: id} is not available yet — configure or test it first."
                }
            }
        )
        refreshStatus()
        return selected
    }

    /** Backwards-compatible alias used by the Capabilities screen. */
    fun selectEngine(id: String): Boolean = selectModel(id)

    fun isModelAvailable(id: String): Boolean = app.models.isAvailable(id)

    fun openModelPicker() {
        _ui.value = _ui.value.copy(modelPickerOpen = true)
    }

    /** The primary chat entry point: acquire a real AI model, never user-supplied weights. */
    fun getModel() = openModelPicker()

    fun closeModelPicker() {
        _ui.value = _ui.value.copy(modelPickerOpen = false)
    }

    fun checkModel(id: String) = viewModelScope.launch {
        _ui.value = _ui.value.copy(
            modelStatuses = _ui.value.modelStatuses + (id to "Checking connection…")
        )
        val result = app.models.checkConnection(id)
        _ui.value = _ui.value.copy(
            modelStatuses = _ui.value.modelStatuses + (id to result.detail)
        )
    }

    fun openModelConfig(id: String) {
        val descriptor = app.models.descriptor(id) ?: return
        val config = app.models.configFor(id)
        _ui.value = _ui.value.copy(
            configuringModelId = id,
            configEndpoint = config?.endpoint ?: descriptor.defaultEndpoint.orEmpty(),
            configModelName = config?.modelName ?: descriptor.modelName,
            configApiKey = ""
        )
    }

    fun closeModelConfig() {
        _ui.value = _ui.value.copy(
            configuringModelId = null,
            configEndpoint = "",
            configModelName = "",
            configApiKey = ""
        )
    }

    fun setConfigEndpoint(value: String) {
        _ui.value = _ui.value.copy(configEndpoint = value)
    }

    fun setConfigModelName(value: String) {
        _ui.value = _ui.value.copy(configModelName = value)
    }

    fun setConfigApiKey(value: String) {
        _ui.value = _ui.value.copy(configApiKey = value)
    }

    fun saveModelConfig() {
        val id = _ui.value.configuringModelId ?: return
        val saved = app.models.saveRemoteConfig(
            id,
            _ui.value.configEndpoint,
            _ui.value.configModelName,
            _ui.value.configApiKey
        )
        _ui.value = _ui.value.copy(
            configuringModelId = null,
            configEndpoint = "",
            configModelName = "",
            configApiKey = "",
            modelStatuses = _ui.value.modelStatuses + (id to "Configured — test the connection to verify"),
            notice = if (saved) "Connection details saved on this device." else "This model cannot be configured."
        )
        refreshStatus()
    }

    fun clearModelConfig(id: String) {
        app.remoteModels.clear(id)
        _ui.value = _ui.value.copy(
            modelStatuses = _ui.value.modelStatuses + (id to "Configuration cleared"),
            notice = "Saved connection details removed."
        )
        refreshStatus()
    }

    fun setWebAccess(enabled: Boolean) {
        app.settings.webAccessEnabled = enabled
        _ui.value = _ui.value.copy(
            webAccessEnabled = enabled,
            notice = if (enabled) "Web access enabled for web adapters." else "Web access disabled. Local models still work."
        )
        refreshStatus()
    }

    fun openAddAdapter() {
        val template = AdapterTemplates.ALL.first()
        _ui.value = _ui.value.copy(
            addAdapterOpen = true,
            adapterTemplateId = template.id,
            newAdapterLabel = template.label,
            newAdapterEndpoint = template.endpoint,
            newAdapterModel = template.model,
            newAdapterKey = ""
        )
    }

    fun closeAddAdapter() {
        _ui.value = _ui.value.copy(addAdapterOpen = false, newAdapterKey = "")
    }

    fun applyAdapterTemplate(id: String) {
        val template = AdapterTemplates.byId(id) ?: return
        _ui.value = _ui.value.copy(
            adapterTemplateId = id,
            newAdapterLabel = template.label,
            newAdapterEndpoint = template.endpoint,
            newAdapterModel = template.model,
            newAdapterKey = ""
        )
    }

    fun setNewAdapterLabel(value: String) {
        _ui.value = _ui.value.copy(newAdapterLabel = value)
    }

    fun setNewAdapterEndpoint(value: String) {
        _ui.value = _ui.value.copy(newAdapterEndpoint = value)
    }

    fun setNewAdapterModel(value: String) {
        _ui.value = _ui.value.copy(newAdapterModel = value)
    }

    fun setNewAdapterKey(value: String) {
        _ui.value = _ui.value.copy(newAdapterKey = value)
    }

    fun addAdapter() {
        val id = app.models.addAdapter(
            _ui.value.newAdapterLabel,
            _ui.value.newAdapterEndpoint,
            _ui.value.newAdapterModel,
            _ui.value.newAdapterKey
        )
        _ui.value = _ui.value.copy(
            models = app.models.descriptors,
            addAdapterOpen = false,
            newAdapterKey = "",
            notice = if (id != null) {
                "Adapter added. Test its connection, then select it."
            } else {
                "An endpoint URL and model name are required."
            }
        )
        refreshStatus()
    }

    fun removeAdapter(id: String) {
        app.models.removeAdapter(id)
        _ui.value = _ui.value.copy(
            models = app.models.descriptors,
            modelStatuses = _ui.value.modelStatuses - id,
            notice = "Adapter removed."
        )
        refreshStatus()
    }

    suspend fun chainRecords() = app.ledger.all()

    suspend fun memoryEntries() = app.memory.all()

    fun respondConsent(granted: Boolean) {
        consentDeferred?.complete(granted)
        consentDeferred = null
        _ui.value = _ui.value.copy(pendingConsent = null)
    }

    fun refreshStatus() = viewModelScope.launch {
        _ui.value = _ui.value.copy(
            engineLabel = app.models.selectedChat()?.displayName ?: "No model · Get Model",
            selectedModelId = app.models.selectedChat()?.id ?: "",
            chatReady = app.models.hasChatModel(),
            nativeRuntimeReady = com.oneorigin.originagent.models.LlamaRuntime.available(),
            nativeLabel = if (app.nativeBridge.isAvailable()) "C/C++ native" else "Kotlin fallback",
            memoryCount = app.memory.all().size,
            chainValid = app.ledger.verify()
        )
    }

    fun setTheme(mode: ThemeMode) {
        app.settings.themeMode = mode
        readSettings()
    }

    fun setDynamicColor(enabled: Boolean) {
        app.settings.dynamicColor = enabled
        readSettings()
    }

    fun setReducedMotion(enabled: Boolean) {
        app.settings.reducedMotion = enabled
        readSettings()
    }

    fun setAutoApproveLowRisk(enabled: Boolean) {
        app.settings.autoApproveLowRisk = enabled
        readSettings()
    }

    fun clearMemory() = viewModelScope.launch {
        app.memory.clear()
        _ui.value = _ui.value.copy(notice = "Local memory cleared")
        refreshStatus()
    }

    suspend fun exportJson(): String {
        val remote = app.remoteModels.redactedMap()
        val records = app.memory.exportRecords() + app.ledger.exportRecords() +
            listOf(app.settings.asMap()) + if (remote.isEmpty()) emptyList() else listOf(remote)
        return JsonCodec.exportDocument(records)
    }

    private suspend fun requestConsent(step: PlanStep): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        consentDeferred = deferred
        val detail = step.input["content"]?.let { "\n\n\"$it\"" }.orEmpty()
        _ui.value = _ui.value.copy(
            pendingConsent = "${step.description}$detail\n\nPermission: ${step.permission}",
            lastEvent = "Awaiting explicit consent"
        )
        return deferred.await()
    }

    private fun onEvent(event: HarnessEvent) {
        _ui.value = _ui.value.copy(lastEvent = "${event.type.name}: ${event.message}")
    }

    private fun readSettings() {
        _ui.value = _ui.value.copy(
            themeMode = app.settings.themeMode,
            dynamicColor = app.settings.dynamicColor,
            reducedMotion = app.settings.reducedMotion,
            autoApproveLowRisk = app.settings.autoApproveLowRisk,
            webAccessEnabled = app.settings.webAccessEnabled
        )
    }

    override fun onCleared() {
        app.modelDownloads.removeListener(downloadListener)
        app.consentRequest = null
        consentDeferred?.cancel()
        super.onCleared()
    }
}
