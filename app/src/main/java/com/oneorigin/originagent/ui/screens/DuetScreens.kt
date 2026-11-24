package com.oneorigin.originagent.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oneorigin.originagent.duet.DuetChannel
import com.oneorigin.originagent.duet.DuetPage
import com.oneorigin.originagent.duet.DuetPageKind
import com.oneorigin.originagent.duet.DuetTask
import com.oneorigin.originagent.duet.DuetTaskStatus
import com.oneorigin.originagent.ui.AgentUiState
import com.oneorigin.originagent.ui.components.DuetCard
import com.oneorigin.originagent.ui.components.DuetDivider
import com.oneorigin.originagent.ui.components.DuetPrimaryButton
import com.oneorigin.originagent.ui.components.DuetTextField
import com.oneorigin.originagent.ui.components.DuetTopBar
import com.oneorigin.originagent.ui.components.EmptyState
import com.oneorigin.originagent.ui.components.DuetModelChip
import com.oneorigin.originagent.ui.components.OrbButton
import com.oneorigin.originagent.ui.components.SectionLabel
import com.oneorigin.originagent.ui.components.StatusChip
import com.oneorigin.originagent.ui.components.StatusTone
import com.oneorigin.originagent.ui.components.SuggestionPills
import com.oneorigin.originagent.ui.voice.VoiceDictation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun timeLabel(epoch: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epoch))

private fun toneFor(status: DuetTaskStatus): StatusTone = when (status) {
    DuetTaskStatus.UP_NEXT -> StatusTone.NEUTRAL
    DuetTaskStatus.WORKING -> StatusTone.WORKING
    DuetTaskStatus.READY_FOR_REVIEW -> StatusTone.NEEDS
    DuetTaskStatus.NEEDS_INPUT -> StatusTone.NEEDS
    DuetTaskStatus.RESOLVED -> StatusTone.DONE
}

@Composable
fun ChatScreen(
    ui: AgentUiState,
    onSend: (String) -> Unit,
    onGetModel: () -> Unit,
    onOpenModelPicker: () -> Unit,
    onVoice: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenSetup: () -> Unit
) {
    var draft by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        DuetTopBar(
            title = "Origin",
            subtitle = if (ui.chatReady) "Private conversation on this device" else "Get a real AI model to start chatting"
        ) {
            DuetModelChip(
                label = if (ui.chatReady) ui.engineLabel else "No model · Get Model",
                ready = ui.chatReady,
                onClick = onOpenModelPicker
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BriefCard(
                    taskCount = ui.duetTasks.count { it.status != DuetTaskStatus.RESOLVED },
                    needsInput = ui.duetTasks.count { it.status == DuetTaskStatus.NEEDS_INPUT },
                    ready = ui.chatReady,
                    onGetModel = onGetModel,
                    onOpenTasks = onOpenTasks,
                    onOpenSetup = onOpenSetup
                )
            }
            items(ui.messages, key = { it.id }) { message ->
                MessageRow(message.isUser, message.text, message.engineLabel, message.verified, message.status)
            }
            if (ui.busy) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip("Working", StatusTone.WORKING)
                        Text(
                            ui.lastEvent,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        SuggestionPills(
            items = if (ui.chatReady) {
                listOf("What can you do?", "Make a short plan", "Remember this")
            } else {
                listOf("Get a model", "How does this work?")
            },
            onPick = { picked ->
                if (picked == "Get a model") onGetModel() else onSend(picked)
            },
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OrbButton(listening = false, onClick = onVoice)
            DuetTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = if (ui.chatReady) "Message ${ui.engineLabel}" else "Get a model to chat",
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    if (draft.isNotBlank()) {
                        onSend(draft)
                        draft = ""
                    } else if (!ui.chatReady) onGetModel()
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun BriefCard(
    taskCount: Int,
    needsInput: Int,
    ready: Boolean,
    onGetModel: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenSetup: () -> Unit
) {
    DuetCard(tonal = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Today · from this device")
            Text(
                if (ready) {
                    "You're set. I can work offline with your local model and keep tasks on this device."
                } else {
                    "I'm Origin. Conversation needs a real AI model — download one once, then it works offline. Tools and tasks work right now."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!ready) {
                    TextButton(onClick = onGetModel) { Text("Get Model") }
                }
                TextButton(onClick = onOpenTasks) {
                    Text(if (taskCount == 0) "Add a task" else "$taskCount task${if (taskCount == 1) "" else "s"}")
                }
                TextButton(onClick = onOpenSetup) { Text("Setup") }
                if (needsInput > 0) StatusChip("Needs input $needsInput", StatusTone.NEEDS)
            }
        }
    }
}

@Composable
private fun MessageRow(
    isUser: Boolean,
    text: String,
    engineLabel: String,
    verified: Boolean,
    status: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            Surface(
                shape = RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 0.dp
            ) {
                Text(
                    text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .animateContentSize()
            ) {
                val chunks = remember(text) {
                    text.split("\n\n").map { it.trim() }.filter { it.isNotBlank() }
                }
                chunks.forEachIndexed { index, chunk ->
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = if (index == 0) 18.dp else 8.dp,
                            topEnd = 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = if (index == chunks.lastIndex) 6.dp else 8.dp
                        ),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        shadowElevation = 0.dp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            chunk,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (engineLabel.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            engineLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!verified) StatusChip("Attempted", StatusTone.PROBLEM)
                        if (status.isNotBlank()) {
                            Text(
                                status,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TasksScreen(
    ui: AgentUiState,
    onAdd: (String, String?) -> Unit,
    onStatus: (String, DuetTaskStatus) -> Unit,
    onChannel: (String, String?) -> Unit,
    onOpenChannels: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var newTask by remember { mutableStateOf("") }
    var showResolved by remember { mutableStateOf(false) }

    val channelName = { id: String? -> ui.duetChannels.firstOrNull { it.id == id }?.name }
    val tasks = ui.duetTasks.filter { task ->
        (showResolved || task.status != DuetTaskStatus.RESOLVED) &&
            (query.isBlank() || task.title.contains(query, ignoreCase = true))
    }

    Column(Modifier.fillMaxSize()) {
        DuetTopBar(title = "Tasks", subtitle = "${tasks.size} on this device") {
            IconButton(onClick = { adding = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        }
        DuetTextField(value = query, onValueChange = { query = it }, placeholder = "Search tasks", modifier = Modifier.padding(horizontal = 18.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterToggle("Up next", !showResolved) { showResolved = false }
            FilterToggle("All", showResolved) { showResolved = true }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onOpenChannels) { Text("Channels") }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (tasks.isEmpty()) {
                item {
                    EmptyState("Nothing here yet", "Add a task or start one from Chat.") {
                        DuetPrimaryButton("Add a task", { adding = true })
                    }
                }
            }
            items(tasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    channelName = channelName(task.channelId),
                    channels = ui.duetChannels.filter { !it.archived },
                    onStatus = { onStatus(task.id, it) },
                    onChannel = { onChannel(task.id, it) }
                )
            }
        }
    }

    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Add a task", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DuetTextField(value = newTask, onValueChange = { newTask = it }, placeholder = "What needs doing?")
                    Text(
                        "It stays on this device. Assign a channel from the task card.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTask.isNotBlank()) onAdd(newTask.trim(), ui.activeChannelId)
                    newTask = ""
                    adding = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun FilterToggle(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 0.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TaskCard(
    task: DuetTask,
    channelName: String?,
    channels: List<DuetChannel>,
    onStatus: (DuetTaskStatus) -> Unit,
    onChannel: (String?) -> Unit
) {
    var showChannels by remember { mutableStateOf(false) }
    DuetCard {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    task.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                StatusChip(task.status.label, toneFor(task.status))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (channelName != null) StatusChip(channelName, StatusTone.NEUTRAL)
                Text(
                    timeLabel(task.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                when (task.status) {
                    DuetTaskStatus.UP_NEXT -> TextButton(onClick = { onStatus(DuetTaskStatus.WORKING) }) { Text("Start") }
                    DuetTaskStatus.WORKING -> TextButton(onClick = { onStatus(DuetTaskStatus.READY_FOR_REVIEW) }) { Text("Done") }
                    DuetTaskStatus.READY_FOR_REVIEW, DuetTaskStatus.NEEDS_INPUT ->
                        TextButton(onClick = { onStatus(DuetTaskStatus.RESOLVED) }) { Text("Resolve") }
                    DuetTaskStatus.RESOLVED -> Unit
                }
                IconButton(onClick = { showChannels = !showChannels }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Change channel", modifier = Modifier.size(18.dp))
                }
            }
            if (showChannels) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterToggle("None", task.channelId == null) { onChannel(null) }
                    channels.take(3).forEach { channel ->
                        FilterToggle(channel.name, task.channelId == channel.id) { onChannel(channel.id) }
                    }
                }
            }
        }
    }
}

@Composable
fun MakeScreen(
    ui: AgentUiState,
    onAddPage: (String, DuetPageKind) -> Unit,
    onSavePage: (String, String) -> Unit,
    onDeletePage: (String) -> Unit
) {
    var topic by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(DuetPageKind.DOC) }
    var editing by remember { mutableStateOf<DuetPage?>(null) }

    Column(Modifier.fillMaxSize()) {
        DuetTopBar(title = "Make", subtitle = "Local drafts you can edit and keep")
        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DuetTextField(value = topic, onValueChange = { topic = it }, placeholder = "What should I draft?")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DuetPageKind.entries.take(4).forEach { option ->
                    FilterToggle(option.label, kind == option) { kind = option }
                }
            }
            DuetPrimaryButton(
                text = "Create ${kind.label.lowercase()} draft",
                onClick = {
                    onAddPage(topic, kind)
                    topic = ""
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (ui.duetPages.isEmpty()) {
                item { EmptyState("No drafts yet", "Make a local draft above; edit it any time.") }
            }
            items(ui.duetPages, key = { it.id }) { page ->
                DuetCard(onClick = { editing = page }) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                page.title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            StatusChip(page.kind.label, StatusTone.NEUTRAL)
                        }
                        Text(
                            page.content.lines().firstOrNull().orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            timeLabel(page.updatedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    editing?.let { page ->
        var text by remember(page.id) { mutableStateOf(page.content) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(page.title, style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .verticalScroll(rememberScrollState()),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSavePage(page.id, text)
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        onDeletePage(page.id)
                        editing = null
                    }) { Text("Delete") }
                    TextButton(onClick = { editing = null }) { Text("Close") }
                }
            }
        )
    }
}

@Composable
fun ChannelsScreen(
    ui: AgentUiState,
    onAdd: (String, String, String) -> Unit,
    onSelect: (String?) -> Unit,
    onArchive: (String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("•") }
    var purpose by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val active = ui.duetChannels.filter { !it.archived }
    val archived = ui.duetChannels.filter { it.archived }

    Column(Modifier.fillMaxSize()) {
        DuetTopBar(title = "Channels", subtitle = "Keep a piece of work in its own place")
        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DuetCard(onClick = { expanded = !expanded }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("New channel", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Icon(
                        if (expanded) Icons.Filled.Close else Icons.Filled.Add,
                        contentDescription = null
                    )
                }
            }
            if (expanded) {
                DuetTextField(value = name, onValueChange = { name = it }, placeholder = "Name")
                DuetTextField(value = emoji, onValueChange = { emoji = it }, placeholder = "Emoji or marker")
                DuetTextField(value = purpose, onValueChange = { purpose = it }, placeholder = "What is this for?")
                DuetPrimaryButton(
                    text = "Create channel",
                    onClick = {
                        if (name.isNotBlank()) {
                            onAdd(name, emoji, purpose)
                            name = ""; emoji = "•"; purpose = ""; expanded = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { SectionLabel("Active") }
            items(active, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    selected = ui.activeChannelId == channel.id,
                    taskCount = ui.duetTasks.count { it.channelId == channel.id && it.status != DuetTaskStatus.RESOLVED },
                    onSelect = { onSelect(if (ui.activeChannelId == channel.id) null else channel.id) },
                    onArchive = { onArchive(channel.id, true) }
                )
            }
            if (archived.isNotEmpty()) {
                item { SectionLabel("Archived") }
                items(archived, key = { it.id }) { channel ->
                    ChannelCard(
                        channel = channel,
                        selected = false,
                        taskCount = ui.duetTasks.count { it.channelId == channel.id && it.status != DuetTaskStatus.RESOLVED },
                        onSelect = {},
                        onArchive = { onArchive(channel.id, false) }
                    )
                }
            }
            if (active.isEmpty() && archived.isEmpty()) {
                item { EmptyState("No channels yet", "Create one to group tasks by purpose.") }
            }
        }
    }
}

@Composable
private fun ChannelCard(
    channel: DuetChannel,
    selected: Boolean,
    taskCount: Int,
    onSelect: () -> Unit,
    onArchive: () -> Unit
) {
    DuetCard(
        onClick = onSelect,
        tonal = selected
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(channel.colorArgb)),
                contentAlignment = Alignment.Center
            ) {
                Text(channel.emoji, style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(channel.name, style = MaterialTheme.typography.titleMedium)
                    if (selected) StatusChip("Active", StatusTone.WORKING)
                }
                Text(
                    channel.purpose.ifBlank { "No purpose set" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (taskCount > 0) StatusChip("$taskCount", StatusTone.NEUTRAL)
            IconButton(onClick = onArchive) {
                Icon(Icons.Filled.Archive, contentDescription = if (channel.archived) "Restore channel" else "Archive channel")
            }
        }
    }
}

@Composable
fun SetupDialog(
    ui: AgentUiState,
    onCheck: () -> Unit,
    onGetModel: () -> Unit,
    onDismiss: () -> Unit
) {
    val rows = listOf(
        SetupRow("AI model", ui.chatReady, ui.engineLabel) to onGetModel,
        SetupRow("Native runtime", ui.nativeRuntimeReady, ui.nativeLabel) to onCheck,
        SetupRow("Trust ledger", ui.chainValid, if (ui.chainValid) "Chain verified offline" else "Chain check failed") to onCheck,
        SetupRow("Web adapters", !ui.webAccessEnabled || ui.models.any { it.isRemote && ui.modelStatuses[it.id]?.contains("success", true) == true }, if (ui.webAccessEnabled) "Off unless you configure one" else "Switched off") to onCheck
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Setup", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { (row, action) ->
                    DuetCard(onClick = action) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatusChip(if (row.ready) "Ready" else "Needs setup", if (row.ready) StatusTone.DONE else StatusTone.NEEDS)
                            Column(Modifier.weight(1f)) {
                                Text(row.title, style = MaterialTheme.typography.titleMedium)
                                Text(row.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onCheck(); onDismiss() }) { Text("Check again") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private data class SetupRow(val title: String, val ready: Boolean, val detail: String)

@Composable
fun VoiceSheet(
    state: VoiceDictation.State,
    onSend: (String) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dictation", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OrbButton(listening = state is VoiceDictation.State.Listening, onClick = onRetry)
                Text(
                    when (state) {
                        VoiceDictation.State.Listening -> "I'm listening, just start talking."
                        VoiceDictation.State.Transcribing -> "Writing it down…"
                        VoiceDictation.State.WaitingForPermission -> "Waiting for microphone permission…"
                        is VoiceDictation.State.Error -> state.message
                        is VoiceDictation.State.Result -> "Got it."
                        VoiceDictation.State.Idle -> "Tap the orb to start."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                (state as? VoiceDictation.State.Result)?.let {
                    Text(it.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = state is VoiceDictation.State.Result,
                onClick = { (state as? VoiceDictation.State.Result)?.let { onSend(it.text) } }
            ) { Text("Send") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRetry) { Text("Retry") }
                TextButton(onClick = onDismiss) { Text("Type instead") }
            }
        }
    )
}

@Composable
fun DuetDividerRow(modifier: Modifier = Modifier) {
    DuetDivider(modifier)
    Spacer(Modifier.height(4.dp))
}

@Composable
fun StatusLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StatusChip("Ready", StatusTone.DONE)
        StatusChip("Needs you", StatusTone.NEEDS)
        StatusChip("Working", StatusTone.WORKING)
        StatusChip("Stopped", StatusTone.PROBLEM)
    }
}
