package com.oneorigin.originagent.harness

import com.oneorigin.originagent.domain.Authorization
import com.oneorigin.originagent.domain.AuthorizationDecision
import com.oneorigin.originagent.domain.ExecutionStatus
import com.oneorigin.originagent.domain.HarnessEvent
import com.oneorigin.originagent.domain.HarnessEventType
import com.oneorigin.originagent.domain.HarnessResult
import com.oneorigin.originagent.domain.Intent
import com.oneorigin.originagent.domain.Plan
import com.oneorigin.originagent.domain.PlanStep
import com.oneorigin.originagent.domain.StepOutcome
import com.oneorigin.originagent.domain.ToolResult
import com.oneorigin.originagent.memory.MemoryStore
import com.oneorigin.originagent.memory.ProvenanceLedger
import com.oneorigin.originagent.models.GenerationRequest
import com.oneorigin.originagent.models.GenerationResult
import com.oneorigin.originagent.models.ModelRegistry
import com.oneorigin.originagent.tools.ToolRegistry

/**
 * The single execution path:
 * Intent → Plan → Authorization → Execute → Verify → Persist → Provenance → Result
 */
class UniversalIntelligenceHarness(
    private val planner: Planner,
    private val authorizer: AuthorizationManager,
    private val tools: ToolRegistry,
    private val verifier: Verifier,
    private val engines: ModelRegistry,
    private val memory: MemoryStore,
    private val ledger: ProvenanceLedger
) {
    private val actor = "origin.harness"

    suspend fun run(intent: Intent, onEvent: (HarnessEvent) -> Unit = {}): HarnessResult {
        emit(onEvent, HarnessEventType.INTENT_RECEIVED, "Intent received from ${intent.source}")
        ledger.append("intent.received", actor, intent.id, intent.text.take(200))

        val plan = planner.plan(intent)
        emit(onEvent, HarnessEventType.PLANNED, "Plan: ${plan.summary}")
        ledger.append("plan.created", actor, plan.id, plan.summary)

        val authorizations = mutableListOf<Authorization>()
        val outcomes = mutableListOf<StepOutcome>()
        var engineId = engines.selected().id

        for (step in plan.steps) {
            val authorization = authorizer.decide(step, plan)
            authorizations += authorization
            if (authorization.decision == AuthorizationDecision.DENIED ||
                authorization.decision == AuthorizationDecision.PENDING
            ) {
                emit(onEvent, HarnessEventType.AWAITING_AUTHORIZATION, "Blocked: ${step.description}")
                emit(onEvent, HarnessEventType.BLOCKED, "${step.toolId} was not executed")
                ledger.append("authorization.${authorization.decision.name}", actor, step.id, authorization.reason)
                outcomes += StepOutcome(step, ExecutionStatus.BLOCKED, authorization.reason)
                continue
            }
            ledger.append("authorization.granted", actor, step.id, authorization.reason)

            emit(onEvent, HarnessEventType.EXECUTING, "Executing ${step.toolId}")
            val attempted = execute(step)
            if (step.toolId == "engine.generate") {
                engineId = attempted.data["engineId"] ?: engineId
            }
            val verification = verifier.verify(step, attempted)
            val status = if (verification.passed) ExecutionStatus.VERIFIED else ExecutionStatus.FAILED
            outcomes += StepOutcome(step, status, attempted.output, attempted.error, verification)
            ledger.append(
                "execution.${status.name}", actor, step.id,
                "${step.toolId}: ${verification.note}"
            )
            emit(
                onEvent,
                if (status == ExecutionStatus.VERIFIED) HarnessEventType.VERIFIED else HarnessEventType.FAILED,
                "${step.toolId}: ${verification.note}"
            )
        }

        memory.add("conversation.user", intent.text, intent.source.name)
        memory.add("conversation.assistant", answerFrom(outcomes), engineId)
        ledger.append("memory.persisted", actor, intent.id, "Conversation saved locally")
        emit(onEvent, HarnessEventType.PERSISTED, "Conversation saved to local memory")

        val answer = answerFrom(outcomes)
        val result = HarnessResult(
            intent = intent,
            plan = plan,
            authorizations = authorizations,
            outcomes = outcomes,
            answer = answer,
            verified = outcomes.isNotEmpty() && outcomes.all { it.status == ExecutionStatus.VERIFIED },
            provenance = ledger.all(),
            engineId = engineId
        )
        ledger.append("result.completed", actor, result.intent.id, if (result.verified) "verified" else "partial")
        emit(onEvent, HarnessEventType.COMPLETED, if (result.verified) "Verified" else "Completed with warnings")
        return result
    }

    private suspend fun execute(step: PlanStep): ToolResult {
        if (step.toolId == "engine.generate") {
            val prompt = step.input["prompt"] ?: ""
            val engine = engines.selected()
            return when (val generated = engine.generate(GenerationRequest(prompt))) {
                is GenerationResult.Success -> ToolResult(
                    output = generated.text,
                    data = mapOf("engineId" to generated.engineId)
                )
                is GenerationResult.Unavailable -> {
                    val fallback = engines.byId("local.deterministic")!!
                    val fallbackResult = fallback.generate(GenerationRequest(prompt))
                    if (fallbackResult is GenerationResult.Success) ToolResult(
                        output = fallbackResult.text,
                        data = mapOf("engineId" to fallbackResult.engineId, "fallback" to generated.reason)
                    ) else ToolResult(
                        status = ExecutionStatus.FAILED,
                        error = generated.reason
                    )
                }
                is GenerationResult.Failure -> ToolResult(status = ExecutionStatus.FAILED, error = generated.error)
            }
        }

        val tool = tools.get(step.toolId)
            ?: return ToolResult(status = ExecutionStatus.FAILED, error = "Unknown tool: ${step.toolId}")
        return runCatching { tool.execute(step.input) }
            .getOrElse { ToolResult(status = ExecutionStatus.FAILED, error = it.message ?: "Tool failed") }
    }

    private fun answerFrom(outcomes: List<StepOutcome>): String {
        val successes = outcomes.filter { it.status == ExecutionStatus.VERIFIED }
        if (successes.isNotEmpty()) return successes.joinToString("\n\n") { it.output }
        val blocked = outcomes.filter { it.status == ExecutionStatus.BLOCKED }
        if (blocked.isNotEmpty()) return "I did not perform that action. ${blocked.first().output}."
        val failures = outcomes.filter { it.status == ExecutionStatus.FAILED }
        if (failures.isNotEmpty()) return "I could not complete that action: ${failures.first().error ?: "unknown error"}."
        return "I wasn't able to produce a result."
    }

    private fun emit(onEvent: (HarnessEvent) -> Unit, type: HarnessEventType, message: String) {
        onEvent(HarnessEvent(type, message))
    }
}
