package com.oneorigin.originagent

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.oneorigin.originagent.ui.screens.CapabilitiesScreen
import com.oneorigin.originagent.ui.screens.MemoryScreen
import com.oneorigin.originagent.ui.screens.OriginScreen
import com.oneorigin.originagent.ui.screens.SettingsScreen
import com.oneorigin.originagent.ui.screens.TrustScreen
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
                dynamicColor = vm.ui.value.dynamicColor
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

private data class Destination(val route: String, val label: String, val icon: @Composable () -> Unit)

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
        Destination("origin", "Origin") { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
        Destination("capabilities", "Tools") { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
        Destination("memory", "Memory") { Icon(Icons.Filled.Storage, contentDescription = null) },
        Destination("trust", "Trust") { Icon(Icons.Filled.VerifiedUser, contentDescription = null) },
        Destination("settings", "Settings") { Icon(Icons.Filled.Settings, contentDescription = null) }
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
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    val selected = current?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = destination.icon,
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "origin",
            modifier = Modifier.padding(padding)
        ) {
            composable("origin") { OriginScreen(ui = ui, onSend = vm::send) }
            composable("capabilities") {
                CapabilitiesScreen(
                    tools = vm.capabilities(),
                    engines = vm.engines(),
                    activeEngineId = vm.ui.value.engineLabel,
                    onSelectEngine = { vm.selectEngine(it); vm.refreshStatus() }
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
                    onLowRisk = vm::setAutoApproveLowRisk
                )
            }
        }
    }

    ui.pendingConsent?.let { detail ->
        AlertDialog(
            onDismissRequest = { vm.respondConsent(false) },
            title = { Text("Consent required") },
            text = { Text(detail) },
            confirmButton = { TextButton(onClick = { vm.respondConsent(true) }) { Text("Approve") } },
            dismissButton = { TextButton(onClick = { vm.respondConsent(false) }) { Text("Decline") } }
        )
    }
}
