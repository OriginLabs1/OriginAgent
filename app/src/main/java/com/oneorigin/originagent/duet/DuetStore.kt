package com.oneorigin.originagent.duet

import com.oneorigin.originagent.util.JsonCodec
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Local-only store for the Duet board. Each collection is a UTF-8 JSON Lines file
 * under [root]. Mutations rewrite the collection through a temp file and rename,
 * so readers never see a half-written line. No network, no SQL, no dependencies.
 */
class DuetStore(val root: File) {
    private val tasksFile = File(root, "tasks.jsonl")
    private val channelsFile = File(root, "channels.jsonl")
    private val pagesFile = File(root, "pages.jsonl")
    private val lock = Any()

    init {
        if (!root.isDirectory && !root.mkdirs()) {
            throw IOException("Cannot create Duet directory: ${root.absolutePath}")
        }
    }

    /** Newest first. Resolved tasks are hidden unless [includeResolved] is true. */
    fun tasks(includeResolved: Boolean = false): List<DuetTask> = synchronized(lock) {
        val all = readRecords(tasksFile, ::decodeTask)
        val visible = if (includeResolved) all else all.filterNot { it.status == DuetTaskStatus.RESOLVED }
        visible.tasksNewestFirst()
    }

    fun addTask(title: String, channelId: String? = null, note: String = ""): DuetTask = synchronized(lock) {
        val now = System.currentTimeMillis()
        val task = DuetTask(
            id = UUID.randomUUID().toString(),
            title = title,
            status = DuetTaskStatus.UP_NEXT,
            channelId = channelId,
            note = note,
            createdAt = now,
            updatedAt = now
        )
        writeRecords(tasksFile, readRecords(tasksFile, ::decodeTask).map(::encodeTask) + encodeTask(task))
        task
    }

    /** A null argument leaves that field unchanged; pass an empty string to clear text. */
    fun updateTask(
        id: String,
        title: String? = null,
        status: DuetTaskStatus? = null,
        channelId: String? = null,
        note: String? = null
    ): DuetTask? = synchronized(lock) {
        val existing = readRecords(tasksFile, ::decodeTask).firstOrNull { it.id == id } ?: return@synchronized null
        val updated = existing.copy(
            title = title ?: existing.title,
            status = status ?: existing.status,
            channelId = channelId ?: existing.channelId,
            note = note ?: existing.note,
            updatedAt = System.currentTimeMillis()
        )
        writeRecords(tasksFile, readRecords(tasksFile, ::decodeTask).map { if (it.id == id) updated else it }.map(::encodeTask))
        updated
    }

    fun removeTask(id: String): Boolean = synchronized(lock) {
        val records = readRecords(tasksFile, ::decodeTask)
        if (records.none { it.id == id }) return@synchronized false
        writeRecords(tasksFile, records.filterNot { it.id == id }.map(::encodeTask))
        true
    }

    /** In insertion order. Archived channels are hidden unless [includeArchived] is true. */
    fun channels(includeArchived: Boolean = false): List<DuetChannel> = synchronized(lock) {
        val all = readRecords(channelsFile, ::decodeChannel)
        if (includeArchived) all else all.filterNot { it.archived }
    }

    fun addChannel(
        name: String,
        emoji: String = "•",
        colorArgb: Int = 0xFF6430AB.toInt(),
        purpose: String = ""
    ): DuetChannel = synchronized(lock) {
        val channel = DuetChannel(
            id = UUID.randomUUID().toString(),
            name = name,
            emoji = emoji,
            colorArgb = colorArgb,
            purpose = purpose,
            archived = false,
            createdAt = System.currentTimeMillis()
        )
        writeRecords(channelsFile, readRecords(channelsFile, ::decodeChannel).map(::encodeChannel) + encodeChannel(channel))
        channel
    }

    /** A null argument leaves that field unchanged. */
    fun updateChannel(
        id: String,
        name: String? = null,
        emoji: String? = null,
        colorArgb: Int? = null,
        purpose: String? = null,
        archived: Boolean? = null
    ): DuetChannel? = synchronized(lock) {
        val existing = readRecords(channelsFile, ::decodeChannel).firstOrNull { it.id == id }
            ?: return@synchronized null
        val updated = existing.copy(
            name = name ?: existing.name,
            emoji = emoji ?: existing.emoji,
            colorArgb = colorArgb ?: existing.colorArgb,
            purpose = purpose ?: existing.purpose,
            archived = archived ?: existing.archived
        )
        writeRecords(
            channelsFile,
            readRecords(channelsFile, ::decodeChannel).map { if (it.id == id) updated else it }.map(::encodeChannel)
        )
        updated
    }

    /** Removes the channel and clears its id from every task that referenced it. */
    fun removeChannel(id: String): Boolean = synchronized(lock) {
        val channels = readRecords(channelsFile, ::decodeChannel)
        if (channels.none { it.id == id }) return@synchronized false
        writeRecords(channelsFile, channels.filterNot { it.id == id }.map(::encodeChannel))
        val now = System.currentTimeMillis()
        val tasks = readRecords(tasksFile, ::decodeTask).map { task ->
            if (task.channelId == id) task.copy(channelId = null, updatedAt = now) else task
        }
        writeRecords(tasksFile, tasks.map(::encodeTask))
        true
    }

    /** Newest first. */
    fun pages(): List<DuetPage> = synchronized(lock) {
        readRecords(pagesFile, ::decodePage).pagesNewestFirst()
    }

    fun addPage(title: String, kind: DuetPageKind, content: String): DuetPage = synchronized(lock) {
        val now = System.currentTimeMillis()
        val page = DuetPage(
            id = UUID.randomUUID().toString(),
            title = title,
            kind = kind,
            content = content,
            createdAt = now,
            updatedAt = now
        )
        writeRecords(pagesFile, readRecords(pagesFile, ::decodePage).map(::encodePage) + encodePage(page))
        page
    }

    /** A null argument leaves that field unchanged. */
    fun updatePage(id: String, title: String? = null, content: String? = null): DuetPage? = synchronized(lock) {
        val existing = readRecords(pagesFile, ::decodePage).firstOrNull { it.id == id } ?: return@synchronized null
        val updated = existing.copy(
            title = title ?: existing.title,
            content = content ?: existing.content,
            updatedAt = System.currentTimeMillis()
        )
        writeRecords(pagesFile, readRecords(pagesFile, ::decodePage).map { if (it.id == id) updated else it }.map(::encodePage))
        updated
    }

    fun removePage(id: String): Boolean = synchronized(lock) {
        val records = readRecords(pagesFile, ::decodePage)
        if (records.none { it.id == id }) return@synchronized false
        writeRecords(pagesFile, records.filterNot { it.id == id }.map(::encodePage))
        true
    }

    private fun encodeTask(task: DuetTask): String = JsonCodec.encode(
        mapOf(
            "id" to task.id,
            "title" to task.title,
            "status" to task.status.name,
            "channelId" to task.channelId,
            "note" to task.note,
            "createdAt" to task.createdAt,
            "updatedAt" to task.updatedAt
        )
    )

    private fun encodeChannel(channel: DuetChannel): String = JsonCodec.encode(
        mapOf(
            "id" to channel.id,
            "name" to channel.name,
            "emoji" to channel.emoji,
            "colorArgb" to channel.colorArgb,
            "purpose" to channel.purpose,
            "archived" to channel.archived,
            "createdAt" to channel.createdAt
        )
    )

    private fun encodePage(page: DuetPage): String = JsonCodec.encode(
        mapOf(
            "id" to page.id,
            "title" to page.title,
            "kind" to page.kind.name,
            "content" to page.content,
            "createdAt" to page.createdAt,
            "updatedAt" to page.updatedAt
        )
    )

    private fun decodeTask(line: String): DuetTask? {
        val fields = JsonCodec.decode(line)
        val id = fields["id"]?.takeIf { it.isNotBlank() && it != "null" } ?: return null
        val title = fields["title"] ?: return null
        val status = fields["status"]?.let { value ->
            runCatching { DuetTaskStatus.valueOf(value) }.getOrNull() ?: return null
        } ?: DuetTaskStatus.UP_NEXT
        return DuetTask(
            id = id,
            title = title,
            status = status,
            channelId = fields["channelId"]?.takeIf { it.isNotBlank() && it != "null" },
            note = fields["note"] ?: "",
            createdAt = fields["createdAt"]?.toLongOrNull() ?: 0L,
            updatedAt = fields["updatedAt"]?.toLongOrNull() ?: 0L
        )
    }

    private fun decodeChannel(line: String): DuetChannel? {
        val fields = JsonCodec.decode(line)
        val id = fields["id"]?.takeIf { it.isNotBlank() && it != "null" } ?: return null
        val name = fields["name"] ?: return null
        return DuetChannel(
            id = id,
            name = name,
            emoji = fields["emoji"]?.takeIf { it.isNotBlank() } ?: "•",
            colorArgb = fields["colorArgb"]?.toIntOrNull() ?: 0xFF6430AB.toInt(),
            purpose = fields["purpose"] ?: "",
            archived = fields["archived"]?.toBooleanStrictOrNull() ?: false,
            createdAt = fields["createdAt"]?.toLongOrNull() ?: 0L
        )
    }

    private fun decodePage(line: String): DuetPage? {
        val fields = JsonCodec.decode(line)
        val id = fields["id"]?.takeIf { it.isNotBlank() && it != "null" } ?: return null
        val title = fields["title"] ?: return null
        val kind = fields["kind"]?.let { value ->
            runCatching { DuetPageKind.valueOf(value) }.getOrNull() ?: return null
        } ?: return null
        return DuetPage(
            id = id,
            title = title,
            kind = kind,
            content = fields["content"] ?: "",
            createdAt = fields["createdAt"]?.toLongOrNull() ?: 0L,
            updatedAt = fields["updatedAt"]?.toLongOrNull() ?: 0L
        )
    }

    private fun <T> readRecords(file: File, decode: (String) -> T?): List<T> {
        if (!file.isFile) return emptyList()
        val lines = runCatching { file.readLines(Charsets.UTF_8) }.getOrDefault(emptyList())
        return lines.mapNotNull { line -> runCatching { decode(line) }.getOrNull() }
    }

    private fun writeRecords(file: File, lines: List<String>) {
        val temp = File(file.parentFile, "${file.name}.tmp")
        try {
            temp.bufferedWriter(Charsets.UTF_8).use { writer ->
                lines.forEach { line ->
                    writer.write(line)
                    writer.newLine()
                }
            }
            if (!temp.renameTo(file) && !(file.delete() && temp.renameTo(file))) {
                throw IOException("Could not replace ${file.absolutePath}")
            }
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    /** Stable newest-first order: timestamp first, then insertion order for ties. */
    private fun List<DuetTask>.tasksNewestFirst(): List<DuetTask> = withIndex()
        .sortedWith(compareByDescending<IndexedValue<DuetTask>> { it.value.createdAt }.thenByDescending { it.index })
        .map { it.value }

    private fun List<DuetPage>.pagesNewestFirst(): List<DuetPage> = withIndex()
        .sortedWith(compareByDescending<IndexedValue<DuetPage>> { it.value.createdAt }.thenByDescending { it.index })
        .map { it.value }

    companion object {
        /** Uses the app's private files directory so data stays on the device. */
        @JvmStatic
        fun from(context: android.content.Context): DuetStore =
            DuetStore(File(context.applicationContext.filesDir, "duet"))
    }
}
