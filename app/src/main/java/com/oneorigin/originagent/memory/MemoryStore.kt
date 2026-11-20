package com.oneorigin.originagent.memory

import com.oneorigin.originagent.domain.MemoryEntry
import com.oneorigin.originagent.util.Ids
import com.oneorigin.originagent.util.JsonCodec
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/** Append-only, local, user-controlled memory. No network, no cloud, no hidden writes. */
class MemoryStore(baseDir: File) {
    private val directory = File(baseDir, "origin").apply { mkdirs() }
    private val file = File(directory, "memory.jsonl")
    private val mutex = Mutex()

    suspend fun add(entry: MemoryEntry): MemoryEntry = mutex.withLock {
        val line = JsonCodec.encode(
            mapOf(
                "id" to entry.id,
                "kind" to entry.kind,
                "content" to entry.content,
                "source" to entry.source,
                "createdAt" to entry.createdAt
            )
        )
        file.appendText(line + "\n")
        entry
    }

    suspend fun add(kind: String, content: String, source: String): MemoryEntry =
        add(MemoryEntry(Ids.next("mem"), kind, content, source))

    suspend fun all(): List<MemoryEntry> = mutex.withLock { readLocked() }

    suspend fun search(query: String): List<MemoryEntry> {
        val tokens = query.trim().lowercase().split(Regex("\\s+")).filter { it.length > 1 }
        if (tokens.isEmpty()) return all()
        return all().filter { entry ->
            val haystack = "${entry.kind} ${entry.content} ${entry.source}".lowercase()
            tokens.any { haystack.contains(it) }
        }
    }

    suspend fun exportRecords(): List<Map<String, Any?>> = all().map { entry ->
        mapOf(
            "type" to "memory", "id" to entry.id, "kind" to entry.kind,
            "content" to entry.content, "source" to entry.source,
            "createdAt" to entry.createdAt
        )
    }

    suspend fun clear() = mutex.withLock { if (file.exists()) file.delete() }

    private fun readLocked(): List<MemoryEntry> {
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull { line ->
            val map = JsonCodec.decode(line)
            val id = map["id"] ?: return@mapNotNull null
            val content = map["content"] ?: return@mapNotNull null
            MemoryEntry(
                id = id,
                kind = map["kind"] ?: "note",
                content = content,
                source = map["source"] ?: "unknown",
                createdAt = map["createdAt"]?.toLongOrNull() ?: 0L
            )
        }
    }
}
