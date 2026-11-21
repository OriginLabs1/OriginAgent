package com.oneorigin.originagent

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.oneorigin.originagent.domain.MemoryEntry
import com.oneorigin.originagent.domain.ProvenanceRecord
import com.oneorigin.originagent.ui.AgentUiState
import com.oneorigin.originagent.ui.AgentViewModel
import com.oneorigin.originagent.ui.components.AuroraMark
import com.oneorigin.originagent.ui.screens.AddAdapterDialog
import com.oneorigin.originagent.ui.screens.CapabilitiesScreen
import com.oneorigin.originagent.ui.screens.MemoryScreen
import com.oneorigin.originagent.ui.screens.ModelPickerDialog
import com.oneorigin.originagent.ui.screens.OriginScreen
import com.oneorigin.originagent.ui.screens.RemoteConfigDialog
import com.oneorigin.originagent.ui.screens.SettingsScreen
import com.oneorigin.originagent.ui.screens.TrustScreen
import com.oneorigin.originagent.ui.theme.Aurora
import com.oneorigin.originagent.ui.theme.OriginTheme
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

private data class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun OriginAgentApp(vm: AgentViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val ui by vm.ui

    val destinations = listOf(
        Destination("origin", "Origin", Icons.AutoMirrored.Filled.Chat),
        Destination("capabilities", "Tools", Icons.Filled.AutoAwesome),
        Destination("memory", "Memory", Icons.Filled.Storage),
        Destination("trust", "Trust", Icons.Filled.VerifiedUser),
        Destination("settings", "Settings", Icons.Filled.Settings)
    )

    val memories by produceState(initialValue = emptyList<MemoryEntry>(), key1 = ui.memoryCount) {
        value = vm.memoryEntries()
    }
    val records by produceState(initialValue = emptyList<ProvenanceRecord>(), key1 = ui.lastEvent) {
        value = vm.chainRecords()
    }

    LaunchedEffect(ui.notice) {
        ui.notice?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    destinations.forEach { destination ->
                        val selected = current?.hierarchy?.any { it.route == destination.route } == true
                        val accent = if (selected) Aurora.Violet else MaterialTheme.colorScheme.onSurfaceVariant
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (selected) Brush.linearGradient(
                                        listOf(
                                            Aurora.Violet.copy(alpha = 0.28f),
                                            Aurora.Cyan.copy(alpha = 0.16f)
                                        )
                                    ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .clickable {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                .padding(horizontal = 11.dp, vertical = 7.dp)
                        ) {
                            Icon(destination.icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                            androidx.compose.material3.Text(
                                destination.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "origin",
            modifier = Modifier.padding(padding)
        ) {
            composable("origin") {
                OriginScreen(
                    ui = ui,
                    onSend = vm::send,
                    onOpenModelPicker = vm::openModelPicker,
                    onGetModel = vm::getModel
                )
            }
            composable("capabilities") {
                CapabilitiesScreen(
                    tools = vm.capabilities(),
                    models = ui.models.filter { it.chatCapable },
                    activeModelId = ui.selectedModelId,
                    availability = vm::isModelAvailable,
                    onSelectModel = { vm.selectModel(it) },
                    onOpenModelPicker = vm::openModelPicker
                )
            }
            composable("memory") {
                MemoryScreen(
                    entries = memories,
                    onExport = {
                        scope.launch {
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
            composable("trust") { TrustScreen(records = records, chainValid = ui.chainValid) }
            composable("settings") {
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

    ui.pendingConsent?.let { detail ->
        AlertDialog(
            onDismissRequest = { vm.respondConsent(false) },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Column {
                    androidx.compose.material3.Text("AUTHORIZATION", style = MaterialTheme.typography.labelSmall, color = Aurora.Mint)
                    androidx.compose.material3.Text("Consent required", style = MaterialTheme.typography.headlineMedium)
                }
            },
            text = { androidx.compose.material3.Text(detail) },
            confirmButton = { TextButton(onClick = { vm.respondConsent(true) }) { androidx.compose.material3.Text("Approve") } },
            dismissButton = { TextButton(onClick = { vm.respondConsent(false) }) { androidx.compose.material3.Text("Decline") } }
        )
    }

    if (ui.modelPickerOpen) {
        ModelPickerDialog(
            models = ui.models.filter { it.chatCapable },
            selectedId = ui.selectedModelId,
            statuses = ui.modelStatuses,
            downloadStatuses = ui.downloadStatuses,
            nativeRuntimeReady = ui.nativeRuntimeReady,
            availability = vm::isModelAvailable,
            onSelect = { vm.selectModel(it) },
            onCheck = { vm.checkModel(it) },
            onConfigure = { vm.openModelConfig(it) },
            onClear = { vm.clearModelConfig(it) },
            onDownload = vm::downloadModel,
            onCancelDownload = vm::cancelDownload,
            onDeleteWeights = vm::deleteWeights,
            onAddAdapter = vm::openAddAdapter,
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
}
