package com.oneorigin.originagent.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oneorigin.originagent.domain.MemoryEntry
import com.oneorigin.originagent.domain.ProvenanceRecord
import com.oneorigin.originagent.models.AdapterTemplates
import com.oneorigin.originagent.models.ModelCatalog
import com.oneorigin.originagent.models.ModelDescriptor
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
fun OriginScreen(
    ui: AgentUiState,
    onSend: (String) -> Unit,
    onOpenModelPicker: () -> Unit
) {
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
                StatusPill(if (ui.chainValid) "Chain verified" else "Chain alert", if (ui.chainValid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                StatusPill(ui.nativeLabel, MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.height(8.dp))
            FilterChip(
                selected = true,
                onClick = onOpenModelPicker,
                leadingIcon = {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text("Model: ${ui.engineLabel}  ·  change") }
            )
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
    models: List<ModelDescriptor>,
    activeModelId: String,
    availability: (String) -> Boolean,
    onSelectModel: (String) -> Unit,
    onOpenModelPicker: () -> Unit
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
            Text("Models", style = MaterialTheme.typography.headlineMedium)
            Text(
                "The picker shows the real serving model, provider, capabilities and status. Candidates are never dependencies.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = onOpenModelPicker, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Open universal model picker")
            }
            Spacer(Modifier.height(8.dp))
        }
        items(models, key = { it.id }) { model ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(model.displayName, style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f))
                        StatusPill(
                            if (model.id == activeModelId) "Active" else model.kindLabel,
                            if (model.id == activeModelId) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text("${model.provider} · ${model.modelName}", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(model.limitations, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    AssistChip(
                        onClick = { onSelectModel(model.id) },
                        enabled = availability(model.id) || model.id == ModelCatalog.LOCAL.id,
                        label = {
                            Text(
                                when {
                                    model.id == activeModelId -> "Active"
                                    availability(model.id) -> "Use this model"
                                    else -> "Not available yet"
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

/** The v0.0.2 universal model picker. Selection never clears the conversation. */
@Composable
fun ModelPickerDialog(
    models: List<ModelDescriptor>,
    selectedId: String,
    statuses: Map<String, String>,
    availability: (String) -> Boolean,
    onSelect: (String) -> Unit,
    onCheck: (String) -> Unit,
    onConfigure: (String) -> Unit,
    onClear: (String) -> Unit,
    onAddAdapter: () -> Unit,
    onRemoveAdapter: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Models & adapters") },
        text = {
            Column {
                Text(
                    "The model chip in chat always shows who is actually answering. Switching keeps your conversation and its provenance intact.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onAddAdapter, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Add adapter")
                }
                Spacer(Modifier.height(10.dp))
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(models, key = { it.id }) { model ->
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(model.displayName, style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f))
                                    StatusPill(
                                        if (model.id == selectedId) "Active" else model.kindLabel,
                                        if (model.id == selectedId) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Text("${model.provider} · ${model.modelName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Text(model.description, style = MaterialTheme.typography.bodySmall)
                                Text("Capabilities: ${model.capabilities.joinToString(", ")}",
                                    style = MaterialTheme.typography.labelSmall)
                                Text("Limits: ${model.limitations}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val status = statuses[model.id]
                                if (!status.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(status, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { onSelect(model.id) },
                                        enabled = availability(model.id) || model.id == ModelCatalog.LOCAL.id
                                    ) {
                                        Text(if (model.id == selectedId) "Active" else "Use")
                                    }
                                    if (model.isRemote) {
                                        OutlinedButton(onClick = { onCheck(model.id) }) { Text("Test") }
                                        TextButton(onClick = { onConfigure(model.id) }) { Text("Configure") }
                                        if (statuses.containsKey(model.id)) {
                                            TextButton(onClick = { onClear(model.id) }) { Text("Clear") }
                                        }
                                        if (model.id.startsWith("adapter.")) {
                                            TextButton(onClick = { onRemoveAdapter(model.id) }) { Text("Remove") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

/** Add a web adapter from a built-in template or a custom endpoint. */
@Composable
fun AddAdapterDialog(
    templateId: String,
    label: String,
    endpoint: String,
    modelName: String,
    apiKey: String,
    onTemplate: (String) -> Unit,
    onLabel: (String) -> Unit,
    onEndpoint: (String) -> Unit,
    onModelName: (String) -> Unit,
    onApiKey: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add adapter") },
        text = {
            Column {
                Text(
                    "Start from a built-in web adapter template or bring your own endpoint. Web access is toggled in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text("Templates", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AdapterTemplates.ALL.forEach { template ->
                        FilterChip(
                            selected = templateId == template.id,
                            onClick = { onTemplate(template.id) },
                            label = { Text(template.label) }
                        )
                    }
                }
                AdapterTemplates.byId(templateId)?.let { template ->
                    Spacer(Modifier.height(4.dp))
                    Text(template.note, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = label, onValueChange = onLabel,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Adapter name") }, singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = endpoint, onValueChange = onEndpoint,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Endpoint base URL (include /v1)") },
                    placeholder = { Text("https://api.example.com/v1") }, singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelName, onValueChange = onModelName,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Model name") }, singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey, onValueChange = onApiKey,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API key (optional)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** User-controlled endpoint, model name and credential. Nothing is sent until saved. */
@Composable
fun RemoteConfigDialog(
    model: ModelDescriptor,
    endpoint: String,
    modelName: String,
    apiKey: String,
    onEndpoint: (String) -> Unit,
    onModelName: (String) -> Unit,
    onApiKey: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure ${model.displayName}") },
        text = {
            Column {
                Text(
                    "Use a documented OpenAI-compatible /v1 endpoint you trust. Details stay on this device and credentials are masked in exports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = onEndpoint,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Endpoint base URL (include /v1)") },
                    placeholder = { Text("https://api.example.com/v1") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelName,
                    onValueChange = onModelName,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Model name sent to the endpoint") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKey,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API key${if (model.requiresApiKey) " (required)" else " (optional)"}") },
                    placeholder = { Text("Leave blank to keep the saved key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
                    onMotion: (Boolean) -> Unit, onLowRisk: (Boolean) -> Unit,
                    onWebAccess: (Boolean) -> Unit) {
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
            Text("Connections", style = MaterialTheme.typography.titleMedium)
            SettingSwitch("Web access (for web adapters)", ui.webAccessEnabled, onWebAccess)
            Spacer(Modifier.height(16.dp))
            Text("Privacy & distribution", style = MaterialTheme.typography.titleMedium)
            Text(
                "No telemetry. Local by default. Network is used only for a remote model you explicitly select and configure. Distributed via GitHub Releases, not the Google Play Store.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text("Models", style = MaterialTheme.typography.titleMedium)
            Text(
                "Choose and configure models in the picker. Network access is used only for a remote model you select; credentials are masked in exports.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text("Origin Agent v0.0.2 · OneOrigin initiative", style = MaterialTheme.typography.bodyMedium)
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
