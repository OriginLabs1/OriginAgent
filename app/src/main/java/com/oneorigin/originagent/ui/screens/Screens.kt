package com.oneorigin.originagent.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oneorigin.originagent.domain.MemoryEntry
import com.oneorigin.originagent.domain.ProvenanceRecord
import com.oneorigin.originagent.models.AdapterTemplates
import com.oneorigin.originagent.models.ModelDescriptor
import com.oneorigin.originagent.tools.Tool
import com.oneorigin.originagent.ui.AgentUiState
import com.oneorigin.originagent.util.ModelDownloadManager
import com.oneorigin.originagent.ui.components.AuroraBackground
import com.oneorigin.originagent.ui.components.AuroraMark
import com.oneorigin.originagent.ui.components.AuroraPill
import com.oneorigin.originagent.ui.components.AuroraRule
import com.oneorigin.originagent.ui.components.AuroraScreen
import com.oneorigin.originagent.ui.components.GlassCard
import com.oneorigin.originagent.ui.components.GradientButton
import com.oneorigin.originagent.ui.components.MessageBubble
import com.oneorigin.originagent.ui.components.ModelChip
import com.oneorigin.originagent.ui.components.SectionHeading
import com.oneorigin.originagent.ui.components.SuggestionRow
import com.oneorigin.originagent.ui.theme.Aurora
import com.oneorigin.originagent.util.ThemeMode
import kotlinx.coroutines.launch

private val SUGGESTIONS = listOf(
    "What can you do?",
    "Calculate 12% of 840",
    "Explain trustworthy execution",
    "Remember: prefer local-first tools",
    "What time is it?",
    "Which model is answering?"
)

private val DialogShape = RoundedCornerShape(28.dp)

private fun appSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
    else -> String.format("%.0f MB", bytes / (1024.0 * 1024.0))
}

@Composable
private fun ScreenHeader(kicker: String, title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(kicker.uppercase(), style = MaterialTheme.typography.labelSmall, color = Aurora.Mint)
        Spacer(Modifier.size(8.dp))
        Text(title, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.size(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AuroraHero(chatReady: Boolean, onGetModel: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().padding(vertical = 6.dp), emphasized = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AuroraMark(34.dp)
            Spacer(Modifier.size(10.dp))
            Text("ORIGIN · ON DEVICE", style = MaterialTheme.typography.labelSmall, color = Aurora.Cyan)
        }
        Spacer(Modifier.size(18.dp))
        Text("Ask anything.\nStay in control.", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.size(12.dp))
        Text(
            "Chat happens only with a real AI model you download here. Origin verifies the weights, runs them on this device, and names the model in every reply.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(16.dp))
        if (chatReady) {
            ModelChip(label = "Choose your model", onClick = onGetModel)
        } else {
            GradientButton("Get Model", onGetModel, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SuggestionGrid(items: List<String>, onClick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { item ->
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        onClick = { onClick(item) }
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Aurora.Violet,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(item, style = MaterialTheme.typography.labelMedium, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
fun OriginScreen(
    ui: AgentUiState,
    onSend: (String) -> Unit,
    onOpenModelPicker: () -> Unit,
    onGetModel: () -> Unit
) {
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(ui.messages.size) {
        if (ui.messages.size > 1) {
            scope.launch { listState.animateScrollToItem(ui.messages.lastIndex) }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuroraMark(36.dp)
                Spacer(Modifier.size(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("ORIGIN AGENT", style = MaterialTheme.typography.labelSmall, color = Aurora.Mint)
                    Text("Universal intelligence", style = MaterialTheme.typography.titleMedium)
                }
                AuroraPill(
                    if (ui.chainValid) "CHAIN VERIFIED" else "CHAIN ALERT",
                    if (ui.chainValid) Aurora.Mint else Aurora.Rose
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModelChip(
                    if (ui.chatReady) ui.engineLabel else "No model · Get Model",
                    onOpenModelPicker,
                    Modifier.weight(1f)
                )
                AuroraPill(ui.nativeLabel, Aurora.Cyan, dot = false)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (ui.messages.size <= 1) {
                    item { AuroraHero(ui.chatReady, onGetModel) }
                    if (ui.chatReady) {
                        item { SuggestionGrid(SUGGESTIONS, onSend) }
                    }
                }
                items(ui.messages, key = { it.id }) { message -> MessageBubble(message) }
            }

            AnimatedVisibility(visible = ui.busy) {
                Text(
                    ui.lastEvent,
                    style = MaterialTheme.typography.labelSmall,
                    color = Aurora.Mint,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }

            if (!ui.chatReady) {
                GlassCard(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    emphasized = true
                ) {
                    Text(
                        "Get a model to start chatting",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        "Origin never imitates a conversation with rules. Tap Get Model, choose a real AI model and verify the download once — it then works offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(12.dp))
                    GradientButton("Get Model", onGetModel, Modifier.fillMaxWidth())
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val submit = {
                    if (draft.isNotBlank()) {
                        onSend(draft)
                        draft = ""
                    }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    enabled = ui.chatReady,
                    placeholder = {
                        Text(
                            if (ui.chatReady) "Ask the selected model…" else "Get a model to start chatting",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submit() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Aurora.Violet,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.66f),
                        cursorColor = Aurora.Violet
                    )
                )
                Surface(
                    modifier = Modifier.size(54.dp).clickable(enabled = ui.chatReady, onClick = submit),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        Modifier.background(
                            if (ui.chatReady) Brush.linearGradient(Aurora.Conversation)
                            else Brush.linearGradient(listOf(Color.Gray, Color.Gray)),
                            RoundedCornerShape(18.dp)
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message",
                            tint = Color.White,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
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
    AuroraScreen {
        Column(Modifier.fillMaxSize().padding(top = 20.dp).padding(horizontal = 16.dp)) {
            ScreenHeader("CAPABILITY MAP", "Tools & models", "Every tool is explicit, local and gated by the trust pipeline.")
            Spacer(Modifier.size(14.dp))
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(tools, key = { it.id }) { tool ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text(tool.displayName, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.size(5.dp))
                        Text(tool.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.size(12.dp))
                        AuroraPill(
                            "${tool.risk.name} risk · ${tool.permission}",
                            if (tool.risk.name == "HIGH") Aurora.Rose else Aurora.Cyan
                        )
                    }
                }
                item {
                    GlassCard(Modifier.fillMaxWidth(), emphasized = true) {
                        SectionHeading("INTELLIGENCE", "Choose what answers")
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Chat uses only real AI models: a verified local GGUF downloaded with Get Model, or a web adapter you configure. Rule-based tools never pretend to be a conversation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.size(14.dp))
                        GradientButton("Open universal model picker", onOpenModelPicker, Modifier.fillMaxWidth())
                    }
                }
                items(models, key = { it.id }) { model ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(model.displayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            AuroraPill(
                                if (model.id == activeModelId) "ACTIVE" else model.kindLabel,
                                if (model.id == activeModelId) Aurora.Mint else Aurora.Cyan
                            )
                        }
                        Spacer(Modifier.size(5.dp))
                        Text(
                            "${model.provider} · ${model.modelName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(model.limitations, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.size(10.dp))
                        AssistChip(
                            onClick = { onSelectModel(model.id) },
                            enabled = availability(model.id),
                            label = {
                                Text(
                                    when {
                                        model.id == activeModelId -> "Active"
                                        availability(model.id) -> "Use this model"
                                        model.isDownloadable -> "Get Model"
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
}

/** The universal model picker. Selection never clears the conversation. */
@Composable
fun ModelPickerDialog(
    models: List<ModelDescriptor>,
    selectedId: String,
    statuses: Map<String, String>,
    downloadStatuses: Map<String, ModelDownloadManager.Status>,
    nativeRuntimeReady: Boolean,
    availability: (String) -> Boolean,
    onSelect: (String) -> Unit,
    onCheck: (String) -> Unit,
    onConfigure: (String) -> Unit,
    onClear: (String) -> Unit,
    onDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteWeights: (String) -> Unit,
    onAddAdapter: () -> Unit,
    onRemoveAdapter: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text("UNIVERSAL PICKER", style = MaterialTheme.typography.labelSmall, color = Aurora.Mint)
                Text("Models & adapters", style = MaterialTheme.typography.headlineMedium)
            }
        },
        text = {
            Column {
                Text(
                    "Chat is only with a real AI model. Tap Get Model to download and verify local weights, or configure a web adapter — Origin never substitutes rule-based answers for a model.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!nativeRuntimeReady) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "This device cannot load the arm64 local runtime. Web adapters still work; local GGUF models will stay unavailable.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Aurora.Amber
                    )
                }
                Spacer(Modifier.size(10.dp))
                GradientButton("Add adapter", onAddAdapter, Modifier.fillMaxWidth())
                Spacer(Modifier.size(12.dp))
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(models, key = { it.id }) { model ->
                        GlassCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(model.displayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                AuroraPill(
                                    if (model.id == selectedId) "ACTIVE" else model.kindLabel,
                                    if (model.id == selectedId) Aurora.Mint else Aurora.Cyan
                                )
                            }
                            Spacer(Modifier.size(4.dp))
                            Text(
                                "${model.provider} · ${model.modelName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.size(6.dp))
                            Text(model.description, style = MaterialTheme.typography.bodySmall)
                            Text("Capabilities: ${model.capabilities.joinToString(", ")}", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "Limits: ${model.limitations}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val status = statuses[model.id]
                            if (!status.isNullOrBlank()) {
                                Spacer(Modifier.size(5.dp))
                                Text(status, style = MaterialTheme.typography.labelSmall, color = Aurora.Mint)
                            }

                            if (model.isDownloadable) {
                                val spec = model.downloadSpec!!
                                val download = downloadStatuses[model.id]
                                    ?: ModelDownloadManager.Status(ModelDownloadManager.State.NOT_DOWNLOADED)
                                Spacer(Modifier.size(10.dp))
                                Text(
                                    "${spec.family} · ${spec.license} · ${spec.attribution}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.size(8.dp))
                                AuroraRule()
                                Spacer(Modifier.size(9.dp))
                                when (download.state) {
                                    ModelDownloadManager.State.READY -> {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            AuroraPill("WEIGHTS VERIFIED", Aurora.Mint)
                                            TextButton(onClick = { onDeleteWeights(model.id) }) { Text("Remove") }
                                        }
                                    }
                                    ModelDownloadManager.State.DOWNLOADING -> {
                                        LinearProgressIndicator(
                                            progress = { download.progress / 100f },
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                        )
                                        Spacer(Modifier.size(6.dp))
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("${download.progress}% · verified download", style = MaterialTheme.typography.labelSmall)
                                            TextButton(onClick = { onCancelDownload(model.id) }) { Text("Cancel") }
                                        }
                                    }
                                    else -> {
                                        Text(download.message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.size(8.dp))
                                        GradientButton(
                                            text = if (download.state == ModelDownloadManager.State.FAILED) {
                                                "Retry Get Model"
                                            } else {
                                                "Get Model · ${appSize(spec.sizeBytes)}"
                                            },
                                            onClick = { onDownload(model.id) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.size(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { onSelect(model.id) },
                                    enabled = availability(model.id)
                                ) { Text(if (model.id == selectedId) "Active" else "Use") }
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
        shape = DialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text("ADD ADAPTER", style = MaterialTheme.typography.labelSmall, color = Aurora.Mint)
                Text("Connect a model", style = MaterialTheme.typography.headlineMedium)
            }
        },
        text = {
            Column {
                Text(
                    "Start from a built-in web adapter template or bring your own endpoint. Web access is toggled in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(10.dp))
                Text("TEMPLATES", style = MaterialTheme.typography.labelSmall, color = Aurora.Cyan)
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
                    Spacer(Modifier.size(5.dp))
                    Text(template.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.size(10.dp))
                OutlinedTextField(value = label, onValueChange = onLabel, modifier = Modifier.fillMaxWidth(), label = { Text("Adapter name") }, singleLine = true)
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = endpoint, onValueChange = onEndpoint, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Endpoint base URL (include /v1)") },
                    placeholder = { Text("https://api.example.com/v1") }, singleLine = true
                )
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(value = modelName, onValueChange = onModelName, modifier = Modifier.fillMaxWidth(), label = { Text("Model name") }, singleLine = true)
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = apiKey, onValueChange = onApiKey, modifier = Modifier.fillMaxWidth(),
                    label = { Text("API key (optional)") }, singleLine = true,
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
        shape = DialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text("CONNECTION", style = MaterialTheme.typography.labelSmall, color = Aurora.Mint)
                Text("Configure ${model.displayName}", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column {
                Text(
                    "Use a documented OpenAI-compatible /v1 endpoint you trust. Details stay on this device and credentials are masked in exports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(10.dp))
                OutlinedTextField(
                    value = endpoint, onValueChange = onEndpoint, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Endpoint base URL (include /v1)") },
                    placeholder = { Text("https://api.example.com/v1") }, singleLine = true
                )
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = modelName, onValueChange = onModelName, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Model name sent to the endpoint") }, singleLine = true
                )
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = apiKey, onValueChange = onApiKey, modifier = Modifier.fillMaxWidth(),
                    label = { Text("API key${if (model.requiresApiKey) " (required)" else " (optional)"}") },
                    placeholder = { Text("Leave blank to keep the saved key") }, singleLine = true,
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

    AuroraScreen {
        Column(Modifier.fillMaxSize().padding(top = 20.dp).padding(horizontal = 16.dp)) {
            ScreenHeader("YOUR DATA", "Memory", "Local, user-controlled and exportable.")
            Spacer(Modifier.size(14.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search notes and facts") }, singleLine = true,
                shape = RoundedCornerShape(20.dp),
                leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Aurora.Violet) }
            )
            Spacer(Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GradientButton("Export JSON", onExport)
                OutlinedButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(7.dp))
                    Text("Clear")
                }
            }
            Spacer(Modifier.size(14.dp))
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(shown, key = { it.id }) { entry ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        AuroraPill(entry.kind.uppercase(), Aurora.Mint)
                        Spacer(Modifier.size(10.dp))
                        Text(entry.content, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            shape = DialogShape,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Clear local memory?") },
            text = { Text("This deletes notes and conversation history on this device. Export first if you want a copy.") },
            confirmButton = { TextButton(onClick = { onClear(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun TrustScreen(records: List<ProvenanceRecord>, chainValid: Boolean) {
    AuroraScreen {
        Column(Modifier.fillMaxSize().padding(top = 20.dp).padding(horizontal = 16.dp)) {
            ScreenHeader("TRUST LEDGER", "Provenance", "Every consequential action is inspectable.")
            Spacer(Modifier.size(14.dp))
            GlassCard(Modifier.fillMaxWidth(), emphasized = true) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        if (chainValid) Icons.Filled.CheckCircle else Icons.Filled.Info,
                        contentDescription = null,
                        tint = if (chainValid) Aurora.Mint else Aurora.Rose,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        if (chainValid) "Provenance chain verified offline" else "Provenance chain check failed",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.size(12.dp))
                AuroraRule()
                Spacer(Modifier.size(12.dp))
                Text(
                    "Intent → Plan → Authorization → Execute → Verify → Persist → Provenance → Result",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.size(14.dp))
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(records.takeLast(60).asReversed(), key = { it.id }) { record ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text(record.action, style = MaterialTheme.typography.labelMedium, color = Aurora.Violet)
                        Spacer(Modifier.size(5.dp))
                        Text(record.details, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.size(7.dp))
                        Text(
                            "hash ${record.hash.take(12)}… · ${record.actor}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    ui: AgentUiState,
    onTheme: (ThemeMode) -> Unit,
    onDynamic: (Boolean) -> Unit,
    onMotion: (Boolean) -> Unit,
    onLowRisk: (Boolean) -> Unit,
    onWebAccess: (Boolean) -> Unit
) {
    AuroraScreen {
        LazyColumn(
            Modifier.fillMaxSize().padding(top = 20.dp).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { ScreenHeader("PREFERENCES", "Settings", "Your choices stay on this device.") }
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("APPEARANCE", style = MaterialTheme.typography.labelSmall, color = Aurora.Mint)
                    Spacer(Modifier.size(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = ui.themeMode == mode,
                                onClick = { onTheme(mode) },
                                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                    AuroraRule()
                    Spacer(Modifier.size(8.dp))
                    SettingSwitch("Material You dynamic colors", ui.dynamicColor, onDynamic)
                    SettingSwitch("Reduce motion", ui.reducedMotion, onMotion)
                    SettingSwitch("Auto-approve low-risk local tools", ui.autoApproveLowRisk, onLowRisk)
                }
            }
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("CONNECTIONS", style = MaterialTheme.typography.labelSmall, color = Aurora.Cyan)
                    Spacer(Modifier.size(10.dp))
                    SettingSwitch("Web access (for web adapters)", ui.webAccessEnabled, onWebAccess)
                    Text(
                        "Local models stay offline. A network request happens only when you select an adapter you configured.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("PRIVACY & DISTRIBUTION", style = MaterialTheme.typography.labelSmall, color = Aurora.Violet)
                    Spacer(Modifier.size(10.dp))
                    Text(
                        "No telemetry. Local by default. Distributed via GitHub Releases with published checksums — not the Google Play Store.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("ABOUT", style = MaterialTheme.typography.labelSmall, color = Aurora.Mint)
                    Spacer(Modifier.size(8.dp))
                    Text("Origin Agent v0.0.3 · Aurora design", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Universal intelligence. Trustworthy execution. Human empowerment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
