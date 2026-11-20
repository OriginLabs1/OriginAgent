package com.oneorigin.originagent.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oneorigin.originagent.domain.MemoryEntry
import com.oneorigin.originagent.domain.ProvenanceRecord
import com.oneorigin.originagent.models.InferenceEngine
import com.oneorigin.originagent.tools.Tool
import com.oneorigin.originagent.ui.AgentUiState
import com.oneorigin.originagent.ui.components.AmbientOrb
import com.oneorigin.originagent.ui.components.MessageBubble
import com.oneorigin.originagent.ui.components.StatusPill
import com.oneorigin.originagent.ui.components.SuggestionRow
import com.oneorigin.originagent.util.ThemeMode
import kotlinx.coroutines.launch

private val SUGGESTIONS = listOf(
    "What can you do?",
    "Calculate 12% of 840",
    "Explain trustworthy execution",
    "Remember: prefer local-first tools",
    "What time is it?"
)

@Composable
fun OriginScreen(ui: AgentUiState, onSend: (String) -> Unit) {
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(ui.messages.size) {
        if (ui.messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(ui.messages.lastIndex) }
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AmbientOrb(reducedMotion = ui.reducedMotion)
            Spacer(Modifier.height(10.dp))
            Text(
                "Origin",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Universal intelligence. Trustworthy execution.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                StatusPill(ui.engineLabel, MaterialTheme.colorScheme.primary)
                StatusPill(if (ui.chainValid) "Chain verified" else "Chain alert", if (ui.chainValid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                StatusPill(ui.nativeLabel, MaterialTheme.colorScheme.secondary)
            }
        }

        SuggestionRow(SUGGESTIONS) { onSend(it) }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(ui.messages, key = { it.id }) { message -> MessageBubble(message) }
        }

        AnimatedVisibility(visible = ui.busy) {
            Text(
                ui.lastEvent,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask Origin — works offline") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )
            IconButton(
                onClick = {
                    if (draft.isNotBlank()) {
                        onSend(draft)
                        draft = ""
                    }
                },
                modifier = Modifier.size(56.dp).background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(18.dp)
                )
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun CapabilitiesScreen(
    tools: List<Tool>,
    engines: List<InferenceEngine>,
    activeEngineId: String,
    onSelectEngine: (String) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        item {
            Text("Capabilities", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Every tool is explicit, local and gated by the trust pipeline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }
        items(tools, key = { it.id }) { tool ->
            Card(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(tool.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(tool.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    StatusPill(
                        "${tool.risk.name} risk · ${tool.permission}",
                        if (tool.risk.name == "HIGH") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item {
            Spacer(Modifier.height(20.dp))
            Text("Intelligence engines", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Candidates, never dependencies. No model weights are bundled.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
        }
        items(engines, key = { it.id }) { engine ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(engine.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(engine.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { onSelectEngine(engine.id) },
                            enabled = engine.isAvailable(),
                            label = {
                                Text(
                                    when {
                                        engine.id == activeEngineId -> "Active"
                                        engine.isAvailable() -> "Select"
                                        else -> "Not installed"
                                    }
                                )
                            }
                        )
                        StatusPill(
                            if (engine.isAvailable()) "available" else "optional",
                            if (engine.isAvailable()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryScreen(
    entries: List<MemoryEntry>,
    onExport: () -> Unit,
    onClear: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    val shown = remember(query, entries) {
        if (query.isBlank()) entries else entries.filter {
            "${it.kind} ${it.content}".contains(query, ignoreCase = true)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Memory", style = MaterialTheme.typography.headlineMedium)
        Text("Local, user-controlled and exportable.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search memory") },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onExport) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Export JSON")
            }
            TextButton(onClick = { confirmClear = true }) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Clear")
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(shown, key = { it.id }) { entry ->
                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(entry.kind, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Text(entry.content, style = MaterialTheme.typography.bodyMedium,
                            maxLines = 4, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear local memory?") },
            text = { Text("This deletes notes and conversation history on this device. Export first if you want a copy.") },
            confirmButton = {
                TextButton(onClick = { onClear(); confirmClear = false }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun TrustScreen(records: List<ProvenanceRecord>, chainValid: Boolean) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Trust ledger", style = MaterialTheme.typography.headlineMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                if (chainValid) Icons.Filled.CheckCircle else Icons.Filled.Info,
                contentDescription = null,
                tint = if (chainValid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
            )
            Text(
                if (chainValid) "Provenance chain verified offline" else "Provenance chain check failed",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            "Intent → Plan → Authorization → Execute → Verify → Persist → Provenance → Result",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        LazyColumn(Modifier.weight(1f)) {
            items(records.takeLast(60).asReversed(), key = { it.id }) { record ->
                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(record.action, style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Text(record.details, style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("hash ${record.hash.take(12)}… · ${record.actor}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(ui: AgentUiState, onTheme: (ThemeMode) -> Unit, onDynamic: (Boolean) -> Unit,
                    onMotion: (Boolean) -> Unit, onLowRisk: (Boolean) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = ui.themeMode == mode,
                        onClick = { onTheme(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        item {
            SettingSwitch("Material You dynamic colors", ui.dynamicColor, onDynamic)
            SettingSwitch("Reduce motion", ui.reducedMotion, onMotion)
            SettingSwitch("Auto-approve low-risk local tools", ui.autoApproveLowRisk, onLowRisk)
            Spacer(Modifier.height(16.dp))
            Text("Privacy & distribution", style = MaterialTheme.typography.titleMedium)
            Text(
                "No INTERNET permission. No telemetry. Distributed via GitHub Releases, not the Google Play Store.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text("Origin Agent v0.0.1 · OneOrigin initiative", style = MaterialTheme.typography.bodyMedium)
            Text("Universal intelligence. Trustworthy execution. Human empowerment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
