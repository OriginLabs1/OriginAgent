package com.oneorigin.originagent.models

/**
 * Minimal, dependency-free OpenAI-compatible chat protocol helpers.
 * Kept pure so request construction and response parsing are unit-testable without Android.
 */
object OpenAiProtocol {

    fun escape(value: String): String = buildString {
        for (ch in value) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                else -> if (ch.code < 0x20) {
                    append("\\u")
                    append(ch.code.toString(16).padStart(4, '0'))
                } else {
                    append(ch)
                }
            }
        }
    }

    fun requestJson(model: String, system: String, user: String, maxTokens: Int): String {
        val messages = buildList {
            if (system.isNotBlank()) {
                add("""{"role":"system","content":"${escape(system)}"}""")
            }
            add("""{"role":"user","content":"${escape(user)}"}""")
        }.joinToString(",")
        return """{"model":"${escape(model)}","messages":[$messages],"max_tokens":$maxTokens,"temperature":0.2}"""
    }

    /** Returns the first assistant content, or null when the payload has none. */
    fun responseText(json: String): String? {
        val choices = indexOfKey(json, "choices") ?: return null
        val message = indexOfKey(json, "message", choices) ?: return null
        val content = indexOfKey(json, "content", message) ?: return null
        return parseStringAt(json, content)
    }

    fun errorMessage(json: String): String? {
        val error = indexOfKey(json, "error") ?: return null
        val message = indexOfKey(json, "message", error) ?: return null
        return parseStringAt(json, message)
    }

    /** Model ids from a GET /v1/models response, used by the connection check. */
    fun modelNames(json: String): List<String> {
        val data = indexOfKey(json, "data") ?: return emptyList()
        val start = json.indexOf('[', data).takeIf { it >= 0 } ?: return emptyList()
        val end = json.indexOf(']', start).takeIf { it > start } ?: return emptyList()
        val array = json.substring(start, end)
        val names = mutableListOf<String>()
        var cursor = 0
        while (cursor < array.length) {
            val id = indexOfKey(array, "id", cursor) ?: break
            val value = parseStringAt(array, id) ?: break
            names += value
            cursor = id + 1
        }
        return names
    }

    /** Index just after the colon following "key", searching from [from]. */
    private fun indexOfKey(json: String, key: String, from: Int = 0): Int? {
        val needle = "\"$key\""
        var index = json.indexOf(needle, from)
        while (index >= 0) {
            var cursor = index + needle.length
            while (cursor < json.length && json[cursor].isWhitespace()) cursor++
            if (cursor < json.length && json[cursor] == ':') return cursor + 1
            index = json.indexOf(needle, index + 1)
        }
        return null
    }

    private fun parseStringAt(json: String, valueStart: Int): String? {
        var cursor = valueStart
        while (cursor < json.length && json[cursor].isWhitespace()) cursor++
        if (cursor >= json.length || json[cursor] != '"') return null
        cursor++
        val out = StringBuilder()
        while (cursor < json.length) {
            val ch = json[cursor++]
            if (ch == '"') return out.toString()
            if (ch != '\\') {
                out.append(ch)
                continue
            }
            if (cursor >= json.length) return null
            when (val escaped = json[cursor++]) {
                '"' -> out.append('"')
                '\\' -> out.append('\\')
                '/' -> out.append('/')
                'b' -> out.append('\b')
                'f' -> out.append('\u000C')
                'n' -> out.append('\n')
                'r' -> out.append('\r')
                't' -> out.append('\t')
                'u' -> {
                    if (cursor + 4 > json.length) return null
                    val code = json.substring(cursor, cursor + 4).toIntOrNull(16) ?: return null
                    out.append(code.toChar())
                    cursor += 4
                }
                else -> return null
            }
        }
        return null
    }
}
