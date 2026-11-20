package com.oneorigin.originagent.domain

/** Where an intent originated. All intents are recorded, none are implicit. */
enum class IntentSource { USER, VOICE, TOOL, SYSTEM }

data class Intent(
    val id: String,
    val text: String,
    val source: IntentSource = IntentSource.USER,
    val createdAt: Long = System.currentTimeMillis(),
    val locale: String = "en"
)

enum class RiskLevel { LOW, MEDIUM, HIGH }

data class PlanStep(
    val id: String,
    val description: String,
    val toolId: String,
    val input: Map<String, String> = emptyMap(),
    val risk: RiskLevel = RiskLevel.LOW,
    val permission: String = "tool." + toolId.replace('.', '_'),
    val requiresAuthorization: Boolean = risk != RiskLevel.LOW
)

data class Plan(
    val id: String,
    val intentId: String,
    val summary: String,
    val steps: List<PlanStep>
)

enum class AuthorizationDecision { GRANTED, DENIED, NOT_REQUIRED, PENDING }

data class Authorization(
    val id: String,
    val planId: String,
    val stepId: String,
    val decision: AuthorizationDecision,
    val reason: String,
    val decidedAt: Long = System.currentTimeMillis()
)

/**
 * Execution states are deliberately distinct: an attempted action is never reported as
 * a verified success, and a blocked action was never attempted.
 */
enum class ExecutionStatus { NOT_RUN, BLOCKED, ATTEMPTED, FAILED, VERIFIED }

data class Verification(
    val checkedAt: Long = System.currentTimeMillis(),
    val checks: List<String> = emptyList(),
    val passed: Boolean = false,
    val note: String = ""
)

data class ToolResult(
    val status: ExecutionStatus = ExecutionStatus.ATTEMPTED,
    val output: String = "",
    val error: String? = null,
    val data: Map<String, String> = emptyMap()
)

data class StepOutcome(
    val step: PlanStep,
    val status: ExecutionStatus,
    val output: String,
    val error: String? = null,
    val verification: Verification? = null
)

data class MemoryEntry(
    val id: String,
    val kind: String,
    val content: String,
    val source: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class ProvenanceRecord(
    val id: String,
    val action: String,
    val actor: String,
    val subjectId: String,
    val details: String,
    val timestamp: Long,
    val previousHash: String,
    val hash: String
)

enum class HarnessEventType {
    INTENT_RECEIVED, PLANNED, AWAITING_AUTHORIZATION, EXECUTING,
    VERIFIED, FAILED, BLOCKED, PERSISTED, COMPLETED, REJECTED
}

data class HarnessEvent(
    val type: HarnessEventType,
    val message: String,
    val at: Long = System.currentTimeMillis()
)

data class HarnessResult(
    val intent: Intent,
    val plan: Plan,
    val authorizations: List<Authorization>,
    val outcomes: List<StepOutcome>,
    val answer: String,
    val verified: Boolean,
    val provenance: List<ProvenanceRecord>,
    val engineId: String
) {
    val allVerified: Boolean get() = outcomes.isNotEmpty() && outcomes.all { it.status == ExecutionStatus.VERIFIED }
}
