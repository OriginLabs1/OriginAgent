package com.oneorigin.originagent.util

/**
 * Tiny JSON Lines codec for the flat records Origin Agent persists.
 * Deliberately dependency-free: no reflective serializer, no generated code.
 */
object JsonCodec {

    fun escape(value: String): String = buildString(value.length + 8) {
        for (ch in value) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                else -> if (ch.code < 0x20) append(String.format("\\u%04x", ch.code)) else append(ch)
            }
        }
    }

    fun encode(fields: Map<String, Any?>): String = fields.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
        val encoded = when (value) {
            null -> "null"
            is String -> "\"${escape(value)}\""
            is Boolean, is Number -> value.toString()
            is List<*> -> value.joinToString(prefix = "[", postfix = "]") { item ->
                if (item == null) "null" else "\"${escape(item.toString())}\""
            }
            else -> "\"${escape(value.toString())}\""
        }
        "\"${escape(key)}\":$encoded"
    }

    /** Parses a flat JSON object into string values. Numbers/booleans stay textual. */
    fun decode(line: String): Map<String, String> {
        val source = line.trim()
        if (source.length < 2 || source.first() != '{' || source.last() != '}') return emptyMap()
        val result = LinkedHashMap<String, String>()
        var i = 1
        while (i < source.length - 1) {
            while (i < source.length - 1 && source[i].isWhitespace()) i++
            if (source[i] != '"') break
            val (key, afterKey) = readString(source, i)
            i = afterKey
            while (i < source.length - 1 && source[i].isWhitespace()) i++
            if (source[i] != ':') break
            i++
            while (i < source.length - 1 && source[i].isWhitespace()) i++
            val (value, afterValue) = when {
                source[i] == '"' -> readString(source, i)
                else -> {
                    val start = i
                    while (i < source.length - 1 && source[i] != ',' && source[i] != '}') i++
                    source.substring(start, i).trim() to i
                }
            }
            result[key] = value
            i = afterValue
            while (i < source.length - 1 && source[i].isWhitespace()) i++
            if (i < source.length - 1 && source[i] == ',') i++ else break
        }
        return result
    }

    private fun readString(source: String, start: Int): Pair<String, Int> {
        val out = StringBuilder()
        var i = start + 1
        while (i < source.length) {
            val ch = source[i]
            if (ch == '"') return out.toString() to i + 1
            if (ch == '\\' && i + 1 < source.length) {
                i++
                when (val escaped = source[i]) {
                    'n' -> out.append('\n')
                    'r' -> out.append('\r')
                    't' -> out.append('\t')
                    'b' -> out.append('\b')
                    'f' -> out.append('\u000C')
                    'u' -> {
                        if (i + 4 < source.length) {
                            val hex = source.substring(i + 1, i + 5)
                            val code = hex.toIntOrNull(16)
                            if (code != null) out.append(code.toChar())
                            i += 4
                        }
                    }
                    else -> out.append(escaped)
                }
            } else {
                out.append(ch)
            }
            i++
        }
        return out.toString() to i
    }

    fun exportDocument(records: List<Map<String, Any?>>): String = buildString {
        append("{\"schema\":\"origin-agent/export\",\"version\":1,")
        append("\"exportedAt\":").append(System.currentTimeMillis()).append(',')
        append("\"records\":[")
        records.forEachIndexed { index, record ->
            if (index > 0) append(',')
            append(encode(record))
        }
        append("]}")
    }
}
