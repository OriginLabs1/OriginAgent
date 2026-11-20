package com.oneorigin.originagent.tools

import com.oneorigin.originagent.domain.RiskLevel
import com.oneorigin.originagent.domain.ToolResult
import com.oneorigin.originagent.memory.KnowledgeBase
import com.oneorigin.originagent.memory.MemoryStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Deterministic arithmetic. No eval, no scripting, no network. */
class CalculatorTool : Tool {
    override val id = "calculator"
    override val displayName = "Calculator"
    override val description = "Deterministic arithmetic with parentheses, decimals and percentages."
    override val risk = RiskLevel.LOW
    override val permission = "tool.calculator"

    override suspend fun execute(input: Map<String, String>): ToolResult {
        val raw = input["expression"] ?: input["text"]
        if (raw.isNullOrBlank()) return ToolResult(status = com.oneorigin.originagent.domain.ExecutionStatus.FAILED, error = "No expression provided")
        return runCatching {
            val value = evaluate(normalize(raw))
            ToolResult(
                output = format(value),
                data = mapOf("numericResult" to value.toString(), "expression" to raw)
            )
        }.getOrElse {
            ToolResult(status = com.oneorigin.originagent.domain.ExecutionStatus.FAILED, error = it.message ?: "Invalid expression")
        }
    }

    private fun normalize(raw: String): String {
        var value = raw.trim()
            .replace(Regex("(?i)\\bplus\\b"), "+")
            .replace(Regex("(?i)\\bminus\\b"), "-")
            .replace(Regex("(?i)\\btimes\\b"), "*")
            .replace(Regex("(?i)\\bmultiplied by\\b"), "*")
            .replace(Regex("(?i)\\bdivided by\\b"), "/")
            .replace(Regex("(?i)\\bof\\b"), " ")
        // "12% 840" -> "(12/100)*840"
        value = value.replace(Regex("([0-9]+(?:\\.[0-9]+)?)\\s*%\\s*([0-9]+)"), "($1/100)*$2")
        // "12%" alone -> "(12/100)"
        value = value.replace(Regex("([0-9]+(?:\\.[0-9]+)?)\\s*%"), "($1/100)")
        return value.replace(Regex("[^0-9.+\\-*/() ]"), "")
    }

    private fun evaluate(expression: String): Double {
        val tokens = Regex("[0-9]+(?:\\.[0-9]+)?|[()+\\-*/]").findAll(expression).map { it.value }.toList()
        if (tokens.isEmpty()) throw IllegalArgumentException("No numbers found")

        val output = ArrayDeque<String>()
        val operators = ArrayDeque<String>()
        var previous: String? = null

        for (token in tokens) {
            when {
                token[0].isDigit() || token == "." -> output.addLast(token)
                token == "(" -> operators.addLast(token)
                token == ")" -> {
                    while (operators.isNotEmpty() && operators.last() != "(") applyTop(operators, output)
                    if (operators.isEmpty()) throw IllegalArgumentException("Unbalanced parentheses")
                    operators.removeLast()
                }
                else -> {
                    val unary = previous == null || previous == "(" || isOperator(previous)
                    if (unary) {
                        output.addLast("0")
                        operators.addLast(token)
                    } else {
                        while (operators.isNotEmpty() && operators.last() != "(" &&
                            precedence(operators.last()) >= precedence(token)
                        ) applyTop(operators, output)
                        operators.addLast(token)
                    }
                }
            }
            previous = token
        }
        while (operators.isNotEmpty()) {
            val op = operators.removeLast()
            if (op == "(") throw IllegalArgumentException("Unbalanced parentheses")
            apply(op, output)
        }
        if (output.size != 1) throw IllegalArgumentException("Incomplete expression")
        return output.removeLast().toDoubleOrNull() ?: throw IllegalArgumentException("Invalid result")
    }

    private fun applyTop(operators: ArrayDeque<String>, output: ArrayDeque<String>) = apply(operators.removeLast(), output)

    private fun apply(operator: String, output: ArrayDeque<String>) {
        if (output.size < 2) throw IllegalArgumentException("Missing operand")
        val b = output.removeLast().toDoubleOrNull() ?: throw IllegalArgumentException("Invalid operand")
        val a = output.removeLast().toDoubleOrNull() ?: throw IllegalArgumentException("Invalid operand")
        val result = when (operator) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b == 0.0) throw IllegalArgumentException("Division by zero") else a / b
            else -> throw IllegalArgumentException("Unknown operator")
        }
        output.addLast(result.toString())
    }

    private fun isOperator(token: String?) = token in setOf("+", "-", "*", "/")
    private fun precedence(operator: String) = if (operator == "+" || operator == "-") 1 else 2

    private fun format(value: Double): String {
        if (value.isNaN() || value.isInfinite()) throw IllegalArgumentException("Result is not finite")
        if (value % 1.0 == 0.0 && kotlin.math.abs(value) < 1e15) return value.toLong().toString()
        return String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
    }
}

class DateTimeTool : Tool {
    override val id = "datetime"
    override val displayName = "Date & time"
    override val description = "Local or UTC time using the device clock."
    override val risk = RiskLevel.LOW
    override val permission = "tool.datetime"

    override suspend fun execute(input: Map<String, String>): ToolResult {
        val zoneId = if (input["zone"] == "utc") ZoneId.of("UTC") else ZoneId.systemDefault()
        val now = Instant.now().atZone(zoneId)
        val formatter = when (input["format"]) {
            "date" -> DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
            "time" -> DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())
            else -> DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy • HH:mm:ss", Locale.getDefault())
        }
        return ToolResult(output = "${now.format(formatter)} (${zoneId.id})", data = mapOf("zone" to zoneId.id))
    }
}

class KnowledgeTool(private val knowledge: KnowledgeBase) : Tool {
    override val id = "knowledge.search"
    override val displayName = "Knowledge search"
    override val description = "Search bundled constitutional knowledge on the device."
    override val risk = RiskLevel.LOW
    override val permission = "tool.knowledge"

    override suspend fun execute(input: Map<String, String>): ToolResult {
        val query = input["query"] ?: return ToolResult(
            status = com.oneorigin.originagent.domain.ExecutionStatus.FAILED,
            error = "No query provided"
        )
        val results = knowledge.search(query)
        if (results.isEmpty()) return ToolResult(output = "No bundled knowledge matched '$query'. Your data was not sent anywhere.")
        return ToolResult(
            output = results.joinToString("\n\n") { "${it.title}: ${it.content}\n[${it.source}]" },
            data = mapOf("resultCount" to results.size.toString())
        )
    }
}

class NotesAddTool(private val memory: MemoryStore) : Tool {
    override val id = "notes.add"
    override val displayName = "Save note"
    override val description = "Store a note in your local, exportable memory. Requires explicit consent."
    override val risk = RiskLevel.HIGH
    override val permission = "memory.write"

    override suspend fun execute(input: Map<String, String>): ToolResult {
        val content = input["content"]
        if (content.isNullOrBlank()) return ToolResult(
            status = com.oneorigin.originagent.domain.ExecutionStatus.FAILED,
            error = "Nothing to save"
        )
        val entry = memory.add("note", content, "user")
        return ToolResult(output = "Saved locally: ${entry.content}", data = mapOf("memoryId" to entry.id))
    }
}

class NotesSearchTool(private val memory: MemoryStore) : Tool {
    override val id = "notes.search"
    override val displayName = "Search notes"
    override val description = "Find notes in your local memory."
    override val risk = RiskLevel.LOW
    override val permission = "memory.read"

    override suspend fun execute(input: Map<String, String>): ToolResult {
        val query = input["query"] ?: ""
        val results = memory.search(query).filter { it.kind == "note" }.take(10)
        return ToolResult(
            output = if (results.isEmpty()) "No notes found." else results.joinToString("\n") { "• ${it.content}" },
            data = mapOf("resultCount" to results.size.toString())
        )
    }
}

class SystemStatusTool(
    private val statusProvider: () -> String,
    private val nativeAvailable: () -> Boolean
) : Tool {
    override val id = "system.status"
    override val displayName = "System status"
    override val description = "Local engine, storage and native foundation status."
    override val risk = RiskLevel.LOW
    override val permission = "tool.status"

    override suspend fun execute(input: Map<String, String>): ToolResult = ToolResult(
        output = buildString {
            append(statusProvider()).append('\n')
            append("Native foundation: ${if (nativeAvailable()) "available" else "Kotlin fallback"}\n")
            append("Network permission: none. Data stays on this device.")
        }
    )
}

class ModelStatusTool(private val summaryProvider: () -> String) : Tool {
    override val id = "model.status"
    override val displayName = "Model status"
    override val description = "Truthful availability of optional inference engines."
    override val risk = RiskLevel.LOW
    override val permission = "tool.status"

    override suspend fun execute(input: Map<String, String>): ToolResult = ToolResult(output = summaryProvider())
}
