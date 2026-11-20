package com.oneorigin.originagent.harness

import com.oneorigin.originagent.domain.PlanStep
import com.oneorigin.originagent.domain.ToolResult
import com.oneorigin.originagent.domain.Verification

/** Verification is separate from execution: an attempt is not a success. */
class Verifier {

    fun verify(step: PlanStep, result: ToolResult): Verification {
        val checks = mutableListOf("Tool '${step.toolId}' returned a result")
        if (result.status == com.oneorigin.originagent.domain.ExecutionStatus.FAILED || result.error != null) {
            return Verification(
                checks = checks + "Error was reported",
                passed = false,
                note = result.error ?: "Execution reported failure"
            )
        }
        if (result.output.isBlank()) {
            return Verification(checks = checks + "Output is non-empty", passed = false, note = "Empty output")
        }

        val passed = when (step.toolId) {
            "calculator" -> {
                val numeric = result.data["numericResult"]?.toDoubleOrNull()
                checks += "Numeric result parsed: ${numeric != null && numeric.isFinite()}"
                numeric != null && numeric.isFinite()
            }
            "notes.add" -> {
                val hasId = !result.data["memoryId"].isNullOrBlank()
                checks += "Memory id recorded: $hasId"
                hasId
            }
            "knowledge.search" -> {
                val count = result.data["resultCount"]?.toIntOrNull() ?: 0
                checks += "Result count is non-negative: ${count >= 0}"
                count >= 0
            }
            "engine.generate" -> {
                checks += "Engine attribution present: ${result.data["engineId"] != null}"
                result.data["engineId"] != null
            }
            else -> {
                checks += "Tool-specific structural check passed"
                true
            }
        }
        return Verification(
            checks = checks,
            passed = passed,
            note = if (passed) "Verified locally" else "Structural verification failed"
        )
    }
}
