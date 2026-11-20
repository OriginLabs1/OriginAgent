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
import com.oneorigin.originagent.util.Ids
import com.oneorigin.originagent.util.JsonCodec
import com.oneorigin.originagent.util.ThemeMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val verified: Boolean = true,
    val status: String = ""
)

data class AgentUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            id = "welcome",
            text = "I'm Origin. I work offline, keep your data local, and show you exactly what I did. Ask me to calculate something, remember a note, or explain the trust flow.",
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
    val notice: String? = null
)

class AgentViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<OriginAgentApplication>()
    private val _ui = mutableStateOf(AgentUiState())
    val ui: State<AgentUiState> = _ui

    private var consentDeferred: CompletableDeferred<Boolean>? = null

    init {
        readSettings()
        app.consentRequest = { step -> requestConsent(step) }
        refreshStatus()
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _ui.value.busy) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                busy = true,
                notice = null,
                messages = _ui.value.messages + ChatMessage(Ids.next("msg"), trimmed, isUser = true)
            )
            val intent = Intent(Ids.next("intent"), trimmed, IntentSource.USER)
            val result = app.harness.run(intent) { event -> onEvent(event) }
            _ui.value = _ui.value.copy(
                busy = false,
                messages = _ui.value.messages + ChatMessage(
                    id = Ids.next("msg"),
                    text = result.answer,
                    isUser = false,
                    verified = result.verified,
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

    fun selectEngine(id: String): Boolean {
        val selected = app.models.select(id)
        _ui.value = _ui.value.copy(
            engineLabel = app.models.selected().displayName,
            notice = if (selected) "Engine selected (only if available)" else "Engine unavailable — no model was loaded"
        )
        return selected
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
            engineLabel = app.models.selected().displayName,
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
        val records = app.memory.exportRecords() + app.ledger.exportRecords() + listOf(app.settings.asMap())
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
            autoApproveLowRisk = app.settings.autoApproveLowRisk
        )
    }

    override fun onCleared() {
        app.consentRequest = null
        consentDeferred?.cancel()
        super.onCleared()
    }
}
