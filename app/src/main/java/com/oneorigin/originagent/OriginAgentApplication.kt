package com.oneorigin.originagent

import android.app.Application
import com.oneorigin.originagent.harness.AuthorizationManager
import com.oneorigin.originagent.harness.Planner
import com.oneorigin.originagent.harness.UniversalIntelligenceHarness
import com.oneorigin.originagent.harness.Verifier
import com.oneorigin.originagent.memory.KnowledgeBase
import com.oneorigin.originagent.memory.MemoryStore
import com.oneorigin.originagent.memory.ProvenanceLedger
import com.oneorigin.originagent.models.ImportedModelStore
import com.oneorigin.originagent.models.ModelRegistry
import com.oneorigin.originagent.tools.CalculatorTool
import com.oneorigin.originagent.tools.DateTimeTool
import com.oneorigin.originagent.tools.KnowledgeTool
import com.oneorigin.originagent.tools.ModelStatusTool
import com.oneorigin.originagent.tools.NotesAddTool
import com.oneorigin.originagent.tools.NotesSearchTool
import com.oneorigin.originagent.tools.SystemStatusTool
import com.oneorigin.originagent.tools.ToolRegistry
import com.oneorigin.originagent.util.ModelDownloadManager
import com.oneorigin.originagent.util.NativeBridge
import com.oneorigin.originagent.util.RemoteModelConfigStore
import com.oneorigin.originagent.util.SettingsStore

/** Composition root. Everything is local, explicit and replaceable. */
class OriginAgentApplication : Application() {

    val settings: SettingsStore by lazy { SettingsStore(this) }
    val remoteModels: RemoteModelConfigStore by lazy { RemoteModelConfigStore(this) }
    val memory: MemoryStore by lazy { MemoryStore(filesDir) }
    val ledger: ProvenanceLedger by lazy { ProvenanceLedger(filesDir) }
    val knowledge: KnowledgeBase by lazy { KnowledgeBase() }
    val modelDownloads: ModelDownloadManager by lazy { ModelDownloadManager(this) }
    val importedModels: ImportedModelStore by lazy { ImportedModelStore(this) }
    val models: ModelRegistry by lazy {
        ModelRegistry(settings, remoteModels, modelDownloads, importedModels)
    }
    val nativeBridge: NativeBridge get() = NativeBridge

    val tools: ToolRegistry by lazy {
        ToolRegistry(
            listOf(
                CalculatorTool(),
                DateTimeTool(),
                KnowledgeTool(knowledge),
                NotesAddTool(memory),
                NotesSearchTool(memory),
                SystemStatusTool(
                    statusProvider = { "Chat model: ${models.selectedChat()?.displayName ?: "none — tap Get Model"}" },
                    nativeAvailable = { NativeBridge.isAvailable() }
                ),
                ModelStatusTool { models.summary() }
            )
        )
    }

    val harness: UniversalIntelligenceHarness by lazy {
        UniversalIntelligenceHarness(
            planner = Planner(),
            authorizer = AuthorizationManager(
                autoApproveLowRisk = { settings.autoApproveLowRisk }
            ) { step, _ ->
                consentRequest?.invoke(step) ?: false
            },
            tools = tools,
            verifier = Verifier(),
            engines = models,
            memory = memory,
            ledger = ledger
        )
    }

    /** Set by the UI while a consequential action awaits explicit consent. */
    var consentRequest: (suspend (com.oneorigin.originagent.domain.PlanStep) -> Boolean)? = null

    override fun onCreate() {
        super.onCreate()
        // Touch nothing on the network; no permission is requested.
    }
}
