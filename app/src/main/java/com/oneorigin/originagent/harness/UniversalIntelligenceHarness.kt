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
        var engineId = engines.selectedChat()?.id ?: "no-model"

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
            val attempted = execute(step, outcomes.toList())
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

    private suspend fun execute(step: PlanStep, earlier: List<StepOutcome>): ToolResult {
        if (step.toolId == "engine.generate") {
            // Tools may gather verified context first, but the user-facing answer is always
            // generated by a real AI model. There is intentionally no rule-based fallback.
            val prompt = buildPrompt(step.input["prompt"] ?: "", earlier)
            // Chat is only ever conducted with a real AI model the user obtained through
            // Get Model (verified local GGUF) or explicitly configured as a web adapter.
            // There is intentionally no rule-based fallback here.
            val engine = engines.selectedChat() ?: return ToolResult(
                status = ExecutionStatus.FAILED,
                error = "No AI model is ready. Tap Get Model to download one, then chat."
            )
            return when (val generated = engine.generate(GenerationRequest(prompt))) {
                is GenerationResult.Success -> ToolResult(
                    output = generated.text,
                    data = mapOf("engineId" to generated.engineId)
                )
                is GenerationResult.Unavailable -> ToolResult(
                    status = ExecutionStatus.FAILED,
                    error = generated.reason
                )
                is GenerationResult.Failure -> ToolResult(
                    status = ExecutionStatus.FAILED,
                    error = generated.error
                )
            }
        }

        val tool = tools.get(step.toolId)
            ?: return ToolResult(status = ExecutionStatus.FAILED, error = "Unknown tool: ${step.toolId}")
        return runCatching { tool.execute(step.input) }
            .getOrElse { ToolResult(status = ExecutionStatus.FAILED, error = it.message ?: "Tool failed") }
    }

    /** Gives the real model verified tool results as context without letting tools answer. */
    private fun buildPrompt(userPrompt: String, earlier: List<StepOutcome>): String {
        val context = earlier
            .filter { it.status == ExecutionStatus.VERIFIED && it.step.toolId != "engine.generate" }
            .joinToString("\n") { "- ${it.step.toolId}: ${it.output}" }
        return if (context.isBlank()) {
            userPrompt
        } else {
            "User request: $userPrompt\n\nVerified local tool results (use them when answering):\n$context"
        }
    }

    /**
     * A conversation is only answered by the real model step. Tool output can inform that
     * step, but it is never presented as the assistant's answer.
     */
    private fun answerFrom(outcomes: List<StepOutcome>): String {
        val model = outcomes.firstOrNull { it.step.toolId == "engine.generate" }
        if (model != null) {
            return when (model.status) {
                ExecutionStatus.VERIFIED -> model.output
                ExecutionStatus.BLOCKED -> "I did not perform that action. ${model.output}."
                ExecutionStatus.FAILED -> "I could not complete that action: ${model.error ?: "unknown error"}."
                ExecutionStatus.ATTEMPTED, ExecutionStatus.NOT_RUN -> model.output.ifBlank {
                    "I couldn't verify the model's answer."
                }
            }
        }
        // Non-chat intents (tool-only diagnostics) keep their deterministic result.
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
