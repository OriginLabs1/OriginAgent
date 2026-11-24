package com.oneorigin.originagent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oneorigin.originagent.models.ModelDescriptor
import com.oneorigin.originagent.models.ModelKind
import com.oneorigin.originagent.models.ModelSources
import com.oneorigin.originagent.ui.AgentUiState
import com.oneorigin.originagent.util.ModelDownloadManager
import com.oneorigin.originagent.ui.components.DuetCard
import com.oneorigin.originagent.ui.components.DuetPrimaryButton
import com.oneorigin.originagent.ui.components.SectionLabel
import com.oneorigin.originagent.ui.components.StatusChip
import com.oneorigin.originagent.ui.components.StatusTone

/**
 * The Satin model library: a small verified download, trusted browsing, device import and
 * explicit switching. Everything here is local until the user taps an action; no template
 * makes a network request by itself.
 */
@Composable
fun ModelLibraryDialog(
    ui: AgentUiState,
    availability: (String) -> Boolean,
    onSelect: (String) -> Unit,
    onDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteWeights: (String) -> Unit,
    onImportGguf: () -> Unit,
    onBrowse: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onAddAdapter: () -> Unit,
    onCheck: (String) -> Unit,
    onConfigure: (String) -> Unit,
    onClear: (String) -> Unit,
    onRemoveAdapter: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val localModels = ui.models
        .filter { it.kind == ModelKind.LOCAL_NATIVE }
        .sortedWith(
            compareByDescending<ModelDescriptor> { it.id in ui.recommendedModelIds }
                .thenBy { if (availability(it.id)) 0 else 1 }
                .thenBy { it.displayName }
        )
    val remoteModels = ui.models.filter { it.kind == ModelKind.REMOTE_OPENAI_COMPATIBLE }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Get a model", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Start with a small verified model, keep several on this device, and switch whenever you like. Chat only runs on a real GGUF or a web adapter you explicitly configured.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                DuetCard {
                    SectionLabel("This phone")
                    Spacer(Modifier.size(6.dp))
                    Text(ui.deviceMemoryLabel, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Suggestions use a conservative RAM estimate; actual speed depends on the device.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DuetPrimaryButton(
                            text = "Import .gguf",
                            onClick = onImportGguf,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = onBrowse, modifier = Modifier.weight(1f)) {
                            Text("Browse GGUF", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                if (!ui.nativeRuntimeReady) {
                    StatusChip("Local GGUF runtime unavailable on this device", StatusTone.PROBLEM)
                }

                SectionLabel("Local models · kept in this app")
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(localModels, key = { it.id }) { model ->
                        LocalModelCard(
                            model = model,
                            ui = ui,
                            selected = model.id == ui.selectedModelId,
                            recommended = model.id in ui.recommendedModelIds,
                            available = availability(model.id),
                            onSelect = { onSelect(model.id) },
                            onDownload = { onDownload(model.id) },
                            onCancelDownload = { onCancelDownload(model.id) },
                            onDelete = { onDeleteWeights(model.id) }
                        )
                    }

                    item {
                        Spacer(Modifier.size(4.dp))
                        SectionLabel("Trusted GGUF sources")
                        Spacer(Modifier.size(6.dp))
                        DuetCard {
                            ModelSources.ALL.forEach { source ->
                                TextButton(
                                    onClick = { onOpenUrl(source.url) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "${source.name} · ${source.whyTrusted}",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.size(4.dp))
                        SectionLabel("Web adapters · opt-in")
                        Spacer(Modifier.size(6.dp))
                        DuetPrimaryButton("Add adapter", onAddAdapter, Modifier.fillMaxWidth())
                    }

                    items(remoteModels, key = { it.id }) { model ->
                        RemoteModelRow(
                            model = model,
                            selected = model.id == ui.selectedModelId,
                            status = ui.modelStatuses[model.id],
                            available = availability(model.id),
                            onSelect = { onSelect(model.id) },
                            onCheck = { onCheck(model.id) },
                            onConfigure = { onConfigure(model.id) },
                            onClear = { onClear(model.id) },
                            onRemove = { onRemoveAdapter(model.id) }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun LocalModelCard(
    model: ModelDescriptor,
    ui: AgentUiState,
    selected: Boolean,
    recommended: Boolean,
    available: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val imported = model.capabilities.contains("imported")
    val download = ui.downloadStatuses[model.id]
        ?: ModelDownloadManager.Status(ModelDownloadManager.State.NOT_DOWNLOADED)
    DuetCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                model.displayName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (selected) StatusChip("Active", StatusTone.DONE)
            else if (recommended) StatusChip("Fits this phone", StatusTone.DONE)
        }
        Spacer(Modifier.size(4.dp))
        Text(
            model.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.size(6.dp))
        Text(
            when {
                imported -> "Imported GGUF · stored privately in Origin"
                else -> ui.modelFitLabels[model.id] ?: model.limitations
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ui.modelStatuses[model.id]?.takeIf { it.isNotBlank() }?.let { status ->
            Spacer(Modifier.size(4.dp))
            Text(status, style = MaterialTheme.typography.labelSmall)
        }

        when {
            imported -> {
                Spacer(Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onDelete) { Text("Remove") }
                }
            }
            download.state == ModelDownloadManager.State.DOWNLOADING -> {
                Spacer(Modifier.size(8.dp))
                LinearProgressIndicator(
                    progress = { download.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(4.dp))
                TextButton(onClick = onCancelDownload) { Text("Cancel") }
            }
            download.state == ModelDownloadManager.State.READY -> {
                Spacer(Modifier.size(4.dp))
                Text(
                    "Weights verified · offline ready",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                Spacer(Modifier.size(8.dp))
                val spec = model.downloadSpec
                DuetPrimaryButton(
                    text = if (download.state == ModelDownloadManager.State.FAILED) {
                        "Retry download"
                    } else if (spec != null) {
                        "Get model · ${modelSize(spec.sizeBytes)}"
                    } else {
                        "Get model"
                    },
                    onClick = onDownload
                )
            }
        }

        Spacer(Modifier.size(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DuetPrimaryButton(
                text = if (selected) "Active" else "Use",
                onClick = onSelect,
                enabled = available || selected
            )
            if (!imported && download.state == ModelDownloadManager.State.READY) {
                OutlinedButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun RemoteModelRow(
    model: ModelDescriptor,
    selected: Boolean,
    status: String?,
    available: Boolean,
    onSelect: () -> Unit,
    onCheck: () -> Unit,
    onConfigure: () -> Unit,
    onClear: () -> Unit,
    onRemove: () -> Unit
) {
    DuetCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                model.displayName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (selected) StatusChip("Active", StatusTone.DONE)
        }
        status?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.size(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onSelect, enabled = available || selected) {
                Text(if (selected) "Active" else "Use")
            }
            TextButton(onClick = onCheck) { Text("Test") }
            TextButton(onClick = onConfigure) { Text("Configure") }
            if (status != null) TextButton(onClick = onClear) { Text("Clear") }
            if (model.id.startsWith("adapter.")) TextButton(onClick = onRemove) { Text("Remove") }
        }
    }
}

private fun modelSize(bytes: Long): String = when {
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}
