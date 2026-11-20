package com.oneorigin.originagent.util

/** Dependency-free FNV-1a 64-bit. Used for the offline-verifiable provenance chain. */
object Hash {
    private const val OFFSET: ULong = 14695981039346656037u
    private const val PRIME: ULong = 1099511628211u
    private const val SEPARATOR = "\u0001"

    fun fnv1a64(input: String): String {
        var hash = OFFSET
        for (byte in input.toByteArray(Charsets.UTF_8)) {
            hash = (hash xor byte.toUInt().toULong()) * PRIME
        }
        return hash.toString(16).padStart(16, '0')
    }

    fun chainPayload(
        action: String,
        actor: String,
        subjectId: String,
        details: String,
        timestamp: Long,
        previousHash: String
    ): String = listOf(
        "action=$action", "actor=$actor", "subject=$subjectId",
        "details=$details", "time=$timestamp", "prev=$previousHash"
    ).joinToString("|")

    fun serialize(payload: String, hash: String): String = payload + SEPARATOR + hash

    /** Verifies a serialized chain of canonical payload-to-hash pairs (one per line). */
    fun verifySerializedChain(lines: List<String>): Boolean {
        var previous = "0000000000000000"
        for (line in lines) {
            val parts = line.split(SEPARATOR, limit = 2)
            if (parts.size != 2) return false
            if (fnv1a64(parts[0]) != parts[1]) return false
            if (!parts[0].contains("prev=$previous")) return false
            previous = parts[1]
        }
        return true
    }
}
