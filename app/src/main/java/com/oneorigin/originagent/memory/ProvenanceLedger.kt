package com.oneorigin.originagent.memory

import com.oneorigin.originagent.domain.ProvenanceRecord
import com.oneorigin.originagent.util.Hash
import com.oneorigin.originagent.util.Ids
import com.oneorigin.originagent.util.JsonCodec
import com.oneorigin.originagent.util.NativeBridge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/** Append-only hash chain. Verification works offline and detects any edited record. */
class ProvenanceLedger(baseDir: File) {
    private val directory = File(baseDir, "origin").apply { mkdirs() }
    private val file = File(directory, "provenance.jsonl")
    private val mutex = Mutex()

    suspend fun append(
        action: String,
        actor: String,
        subjectId: String,
        details: String
    ): ProvenanceRecord = mutex.withLock {
        val previous = readLocked().lastOrNull()?.hash ?: "0000000000000000"
        val timestamp = System.currentTimeMillis()
        val payload = Hash.chainPayload(action, actor, subjectId, details, timestamp, previous)
        val hash = Hash.fnv1a64(payload)
        val record = ProvenanceRecord(
            id = Ids.next("prov"),
            action = action,
            actor = actor,
            subjectId = subjectId,
            details = details,
            timestamp = timestamp,
            previousHash = previous,
            hash = hash
        )
        file.appendText(
            JsonCodec.encode(
                mapOf(
                    "id" to record.id, "action" to action, "actor" to actor,
                    "subjectId" to subjectId, "details" to details,
                    "timestamp" to timestamp, "previousHash" to previous, "hash" to hash,
                    "payload" to payload
                )
            ) + "\n"
        )
        record
    }

    suspend fun all(): List<ProvenanceRecord> = mutex.withLock { readLocked() }

    suspend fun verify(): Boolean = mutex.withLock {
        val records = readLocked()
        val serialized = file.takeIf { it.exists() }?.readLines().orEmpty().mapNotNull { line ->
            val map = JsonCodec.decode(line)
            val payload = map["payload"] ?: return@mapNotNull null
            val hash = map["hash"] ?: return@mapNotNull null
            Hash.serialize(payload, hash)
        }
        if (serialized.isEmpty()) return@withLock true
        Hash.verifySerializedChain(serialized) && NativeBridge.structuralChainCheck(serialized) &&
            records.zip(serialized).all { (record, line) -> line.endsWith(record.hash) }
    }

    suspend fun exportRecords(): List<Map<String, Any?>> = all().map { record ->
        mapOf(
            "type" to "provenance", "id" to record.id, "action" to record.action,
            "actor" to record.actor, "subjectId" to record.subjectId,
            "details" to record.details, "timestamp" to record.timestamp,
            "previousHash" to record.previousHash, "hash" to record.hash
        )
    }

    private fun readLocked(): List<ProvenanceRecord> {
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull { line ->
            val map = JsonCodec.decode(line)
            val id = map["id"] ?: return@mapNotNull null
            val hash = map["hash"] ?: return@mapNotNull null
            ProvenanceRecord(
                id = id,
                action = map["action"] ?: "unknown",
                actor = map["actor"] ?: "unknown",
                subjectId = map["subjectId"] ?: "",
                details = map["details"] ?: "",
                timestamp = map["timestamp"]?.toLongOrNull() ?: 0L,
                previousHash = map["previousHash"] ?: "0000000000000000",
                hash = hash
            )
        }
    }
}
