package com.oneorigin.originagent

import android.Manifest
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.oneorigin.originagent.domain.MemoryEntry
import com.oneorigin.originagent.domain.ProvenanceRecord
import com.oneorigin.originagent.duet.DuetTaskStatus
import com.oneorigin.originagent.models.ModelSources
import com.oneorigin.originagent.ui.AgentViewModel
import com.oneorigin.originagent.ui.components.DuetCanvas
import com.oneorigin.originagent.ui.components.DuetDock
import com.oneorigin.originagent.ui.components.DuetTab
import com.oneorigin.originagent.ui.screens.AddAdapterDialog
import com.oneorigin.originagent.ui.screens.CapabilitiesScreen
import com.oneorigin.originagent.ui.screens.ChannelsScreen
import com.oneorigin.originagent.ui.screens.ChatScreen
import com.oneorigin.originagent.ui.screens.MakeScreen
import com.oneorigin.originagent.ui.screens.MemoryScreen
import com.oneorigin.originagent.ui.screens.ModelLibraryDialog
import com.oneorigin.originagent.ui.screens.RemoteConfigDialog
import com.oneorigin.originagent.ui.screens.SetupDialog
import com.oneorigin.originagent.ui.screens.SettingsScreen
import com.oneorigin.originagent.ui.screens.TasksScreen
import com.oneorigin.originagent.ui.screens.TrustScreen
import com.oneorigin.originagent.ui.screens.VoiceSheet
import com.oneorigin.originagent.ui.theme.OriginTheme
import com.oneorigin.originagent.ui.voice.VoiceDictation
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as OriginAgentApplication
            val vm: AgentViewModel = viewModel(factory = OriginViewModelFactory(app))
            OriginTheme(
                themeMode = vm.ui.value.themeMode,
                dynamicColor = vm.ui.value.dynamicColor,
                reducedMotion = vm.ui.value.reducedMotion
            ) {
                OriginAgentApp(vm)
            }
        }
    }
}

class OriginViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AgentViewModel(app) as T
}

private sealed interface Screen {
    data class Main(val tab: DuetTab) : Screen
    data object Tools : Screen
    data object Memory : Screen
    data object Trust : Screen
    data object Settings : Screen
}

@Composable
private fun OriginAgentApp(vm: AgentViewModel) {
    val context = LocalContext.current
    val ui by vm.ui
    val snackbar = remember { SnackbarHostState() }
    var screen by remember { mutableStateOf<Screen>(Screen.Main(DuetTab.CHAT)) }
    var menuOpen by remember { mutableStateOf(false) }
    var setupOpen by remember { mutableStateOf(false) }
    var voiceRequested by remember { mutableStateOf(false) }

    val voice = remember { VoiceDictation(context) }
    val voiceState by voice.state.collectAsStateWithLifecycle()
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) voice.start() else voiceRequested = false
    }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importGguf(uri) }
    val openBrowser: (String) -> Unit = { url ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            context.startActivity(Intent.createChooser(intent, "Browse GGUF sources"))
        } catch (_: Exception) {
            vm.setNotice("No browser is available on this device.")
        }
    }
    DisposableEffect(voice) { onDispose { voice.release() } }
    LaunchedEffect(voiceState) {
        if (voiceState is VoiceDictation.State.WaitingForPermission && voiceRequested) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            voiceRequested = false
        }
    }

    val memories by produceState(initialValue = emptyList<MemoryEntry>(), key1 = ui.memoryCount) {
        value = vm.memoryEntries()
    }
    val records by produceState(initialValue = emptyList<ProvenanceRecord>(), key1 = ui.lastEvent) {
        value = vm.chainRecords()
    }
    LaunchedEffect(ui.notice) { ui.notice?.let { snackbar.showSnackbar(it) } }

    val openVoice = {
        voiceRequested = true
        voice.start()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (screen is Screen.Main) {
                DuetDock(
                    current = (screen as Screen.Main).tab,
                    waiting = ui.duetTasks.count {
                        it.status == DuetTaskStatus.NEEDS_INPUT || it.status == DuetTaskStatus.READY_FOR_REVIEW
                    },
                    onSelect = { screen = Screen.Main(it) },
                    onMenu = { menuOpen = true }
                )
            }
        }
    ) { padding ->
        DuetCanvas(modifier = Modifier.padding(padding)) {
            when (val current = screen) {
                is Screen.Main -> when (current.tab) {
                    DuetTab.CHAT -> ChatScreen(
                        ui = ui,
                        onSend = vm::send,
                        onGetModel = vm::getModel,
                        onOpenModelPicker = vm::openModelPicker,
                        onVoice = openVoice,
                        onOpenTasks = { screen = Screen.Main(DuetTab.TASKS) },
                        onOpenSetup = { setupOpen = true }
                    )
                    DuetTab.TASKS -> TasksScreen(
                        ui = ui,
                        onAdd = { title, channel -> vm.addDuetTask(title, channel) },
                        onStatus = { id, status -> vm.setDuetTaskStatus(id, status) },
                        onChannel = { id, channel -> vm.setDuetTaskChannel(id, channel) },
                        onOpenChannels = { screen = Screen.Main(DuetTab.CHANNELS) }
                    )
                    DuetTab.MAKE -> MakeScreen(
                        ui = ui,
                        onAddPage = { topic, kind -> vm.addDuetPage(topic, kind) },
                        onSavePage = { id, content -> vm.updateDuetPage(id, content) },
                        onDeletePage = vm::removeDuetPage
                    )
                    DuetTab.CHANNELS -> ChannelsScreen(
                        ui = ui,
                        onAdd = { name, emoji, purpose -> vm.addDuetChannel(name, emoji, purpose) },
                        onSelect = vm::setActiveChannel,
                        onArchive = { id, archived -> vm.archiveDuetChannel(id, archived) }
                    )
                }
                Screen.Tools -> ExtraScreen("Capabilities", { screen = Screen.Main(DuetTab.CHAT) }) {
                    CapabilitiesScreen(
                        tools = vm.capabilities(),
                        models = ui.models.filter { it.chatCapable },
                        activeModelId = ui.selectedModelId,
                        availability = vm::isModelAvailable,
                        onSelectModel = { vm.selectModel(it) },
                        onOpenModelPicker = vm::openModelPicker
                    )
                }
                Screen.Memory -> ExtraScreen("Memory", { screen = Screen.Main(DuetTab.CHAT) }) {
                    MemoryScreen(
                        entries = memories,
                        onExport = {
                            vm.viewModelScope.launch {
                                val json = vm.exportJson()
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_TEXT, json)
                                }
                                runCatching { context.startActivity(Intent.createChooser(share, "Export Origin data")) }
                            }
                        },
                        onClear = vm::clearMemory
                    )
                }
                Screen.Trust -> ExtraScreen("Trust", { screen = Screen.Main(DuetTab.CHAT) }) {
                    TrustScreen(records = records, chainValid = ui.chainValid)
                }
                Screen.Settings -> ExtraScreen("Settings", { screen = Screen.Main(DuetTab.CHAT) }) {
                    SettingsScreen(
                        ui = ui,
                        onTheme = vm::setTheme,
                        onDynamic = vm::setDynamicColor,
                        onMotion = vm::setReducedMotion,
                        onLowRisk = vm::setAutoApproveLowRisk,
                        onWebAccess = vm::setWebAccess
                    )
                }
            }
        }
    }

    ui.pendingConsent?.let { detail ->
        AlertDialog(
            onDismissRequest = { vm.respondConsent(false) },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
            title = {
                Column {
                    Text("AUTHORIZATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("Consent required", style = MaterialTheme.typography.titleLarge)
                }
            },
            text = { Text(detail) },
            confirmButton = { TextButton(onClick = { vm.respondConsent(true) }) { Text("Allow once") } },
            dismissButton = { TextButton(onClick = { vm.respondConsent(false) }) { Text("Refuse") } }
        )
    }

    if (ui.modelPickerOpen) {
        ModelLibraryDialog(
            ui = ui,
            availability = vm::isModelAvailable,
            onSelect = { vm.selectModel(it) },
            onDownload = vm::downloadModel,
            onCancelDownload = vm::cancelDownload,
            onDeleteWeights = vm::deleteWeights,
            onImportGguf = { importLauncher.launch(arrayOf("*/*")) },
            onBrowse = { openBrowser(ModelSources.HUGGING_FACE_GGUF_SEARCH_URL) },
            onOpenUrl = openBrowser,
            onAddAdapter = vm::openAddAdapter,
            onCheck = { vm.checkModel(it) },
            onConfigure = { vm.openModelConfig(it) },
            onClear = { vm.clearModelConfig(it) },
            onRemoveAdapter = { vm.removeAdapter(it) },
            onDismiss = vm::closeModelPicker
        )
    }

    if (ui.addAdapterOpen) {
        AddAdapterDialog(
            templateId = ui.adapterTemplateId,
            label = ui.newAdapterLabel,
            endpoint = ui.newAdapterEndpoint,
            modelName = ui.newAdapterModel,
            apiKey = ui.newAdapterKey,
            onTemplate = vm::applyAdapterTemplate,
            onLabel = vm::setNewAdapterLabel,
            onEndpoint = vm::setNewAdapterEndpoint,
            onModelName = vm::setNewAdapterModel,
            onApiKey = vm::setNewAdapterKey,
            onSave = vm::addAdapter,
            onDismiss = vm::closeAddAdapter
        )
    }

    val configuringModel = ui.models.firstOrNull { it.id == ui.configuringModelId }
    if (configuringModel != null) {
        RemoteConfigDialog(
            model = configuringModel,
            endpoint = ui.configEndpoint,
            modelName = ui.configModelName,
            apiKey = ui.configApiKey,
            onEndpoint = vm::setConfigEndpoint,
            onModelName = vm::setConfigModelName,
            onApiKey = vm::setConfigApiKey,
            onSave = vm::saveModelConfig,
            onDismiss = vm::closeModelConfig
        )
    }

    if (setupOpen) {
        SetupDialog(
            ui = ui,
            onCheck = { vm.refreshStatus(); vm.refreshDownloads() },
            onGetModel = { setupOpen = false; vm.openModelPicker() },
            onDismiss = { setupOpen = false }
        )
    }

    if (menuOpen) {
        AlertDialog(
            onDismissRequest = { menuOpen = false },
            title = { Text("Origin", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MenuRow("Setup") { setupOpen = true; menuOpen = false }
                    MenuRow("Get a model") { vm.openModelPicker(); menuOpen = false }
                    MenuRow("Import a .gguf file") {
                        menuOpen = false
                        importLauncher.launch(arrayOf("*/*"))
                    }
                    MenuRow("Browse GGUF sources") {
                        menuOpen = false
                        openBrowser(ModelSources.HUGGING_FACE_GGUF_SEARCH_URL)
                    }
                    MenuRow("Capabilities") { screen = Screen.Tools; menuOpen = false }
                    MenuRow("Memory") { screen = Screen.Memory; menuOpen = false }
                    MenuRow("Trust") { screen = Screen.Trust; menuOpen = false }
                    MenuRow("Settings") { screen = Screen.Settings; menuOpen = false }
                }
            },
            confirmButton = { TextButton(onClick = { menuOpen = false }) { Text("Close") } }
        )
    }

    if (voiceState !is VoiceDictation.State.Idle) {
        VoiceSheet(
            state = voiceState,
            onSend = { text -> vm.send(text); voice.release() },
            onRetry = {
                voiceRequested = true
                voice.start()
            },
            onDismiss = { voice.release() }
        )
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ExtraScreen(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        Box(Modifier.weight(1f)) { content() }
    }
}
