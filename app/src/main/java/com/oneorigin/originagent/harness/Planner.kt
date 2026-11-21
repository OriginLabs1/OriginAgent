package com.oneorigin.originagent.harness

import com.oneorigin.originagent.domain.Intent
import com.oneorigin.originagent.domain.Plan
import com.oneorigin.originagent.domain.PlanStep
import com.oneorigin.originagent.domain.RiskLevel
import com.oneorigin.originagent.util.Ids

/**
 * Deterministic, inspectable planning. Tools may gather verified context, but every user
 * conversation finishes with a real AI model step — a rule-based tool never becomes the answer.
 */
class Planner {

    fun plan(intent: Intent): Plan {
        val text = intent.text.trim()
        val primary = when {
            REMEMBER.matches(text) -> PlanStep(
                id = Ids.next("step"),
                description = "Save the user's note to local memory",
                toolId = "notes.add",
                input = mapOf(
                    "content" to REMEMBER.find(text)?.groupValues?.get(1)?.trim().orEmpty()
                ),
                risk = RiskLevel.HIGH,
                permission = "memory.write"
            )

            MY_NOTES.matches(text) -> PlanStep(
                id = Ids.next("step"),
                description = "Search local notes",
                toolId = "notes.search",
                input = mapOf("query" to MY_NOTES.find(text)?.groupValues?.get(1)?.trim().orEmpty())
            )

            isCalculation(text) -> PlanStep(
                id = Ids.next("step"),
                description = "Evaluate the expression deterministically",
                toolId = "calculator",
                input = mapOf("expression" to stripPrefix(text))
            )

            TIME.matches(text) -> PlanStep(
                id = Ids.next("step"),
                description = "Read the device clock",
                toolId = "datetime",
                input = mapOf("zone" to if (text.contains("utc", true)) "utc" else "local")
            )

            KNOWLEDGE.matches(text) || text.contains("constitution", true) -> PlanStep(
                id = Ids.next("step"),
                description = "Search bundled local knowledge",
                toolId = "knowledge.search",
                input = mapOf("query" to (KNOWLEDGE.find(text)?.groupValues?.get(1)?.trim() ?: text))
            )

            STATUS.containsMatchIn(text) || text.contains("what can you do", true) -> PlanStep(
                id = Ids.next("step"),
                description = "Report local system status",
                toolId = "system.status"
            )

            MODELS.containsMatchIn(text) -> PlanStep(
                id = Ids.next("step"),
                description = "Report model registry honestly",
                toolId = "model.status"
            )

            else -> generationStep(text)
        }

        val steps = when (primary.toolId) {
            "engine.generate" -> listOf(primary)
            "system.status" -> listOf(
                primary,
                PlanStep(
                    id = Ids.next("step"),
                    description = "Report engine availability",
                    toolId = "model.status"
                ),
                generationStep(text)
            )
            else -> listOf(primary, generationStep(text))
        }

        return Plan(
            id = Ids.next("plan"),
            intentId = intent.id,
            summary = steps.joinToString(" → ") { it.toolId },
            steps = steps
        )
    }

    private fun generationStep(prompt: String) = PlanStep(
        id = Ids.next("step"),
        description = "Answer with the selected real AI model",
        toolId = "engine.generate",
        input = mapOf("prompt" to prompt)
    )

    private fun isCalculation(text: String): Boolean {
        if (!text.any { it.isDigit() }) return false
        if (CALC_PREFIX.containsMatchIn(text)) return true
        return text.any { it in "+-*/%" } && text.length < 80
    }

    private fun stripPrefix(text: String): String = CALC_PREFIX.replace(text, "").trim().trimEnd('?')
        .replace(Regex("(?i)^what is\\s+"), "")

    companion object {
        private val REMEMBER = Regex("(?i)^(?:please\\s+)?(?:remember|note|save)(?:\\s+that)?[\\s:]+(.+)")
        private val MY_NOTES = Regex("(?i)^(?:show|find|search)(?:\\s+my)?\\s+notes?(?:\\s+(?:about|for)\\s+(.+))?")
        private val TIME = Regex("(?i)^(?:what(?:'s| is)|tell me) the (?:current )?(?:time|date)(?:\\s+(utc|local))?.*")
        private val KNOWLEDGE = Regex("(?i)^(?:search|find|explain|tell me about)\\s+(.+)")
        private val CALC_PREFIX = Regex("(?i)^(?:calculate|compute|solve|what is|what's)\\s+")
        private val STATUS = Regex("(?i)status|capabilit|how does .*work|trust|provenance|verif")
        private val MODELS = Regex("(?i)model|bitnet|llama|lfm|qwen")
    }
}
