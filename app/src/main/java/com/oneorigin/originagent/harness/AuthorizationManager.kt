package com.oneorigin.originagent.harness

import com.oneorigin.originagent.domain.Authorization
import com.oneorigin.originagent.domain.AuthorizationDecision
import com.oneorigin.originagent.domain.Plan
import com.oneorigin.originagent.domain.PlanStep
import com.oneorigin.originagent.domain.RiskLevel
import com.oneorigin.originagent.util.Ids

/**
 * Policy + consent. A step cannot execute until a decision has been recorded.
 * The policy is injected as a function so it stays testable on the JVM.
 */
class AuthorizationManager(
    private val autoApproveLowRisk: () -> Boolean,
    private val requestConsent: suspend (PlanStep, Plan) -> Boolean
) {
    suspend fun decide(step: PlanStep, plan: Plan): Authorization {
        val decision = when {
            !step.requiresAuthorization && autoApproveLowRisk() -> AuthorizationDecision.GRANTED
            step.risk == RiskLevel.HIGH -> {
                val granted = requestConsent(step, plan)
                if (granted) AuthorizationDecision.GRANTED else AuthorizationDecision.DENIED
            }
            autoApproveLowRisk() -> AuthorizationDecision.GRANTED
            else -> {
                val granted = requestConsent(step, plan)
                if (granted) AuthorizationDecision.GRANTED else AuthorizationDecision.DENIED
            }
        }
        val reason = when (decision) {
            AuthorizationDecision.GRANTED -> if (step.risk == RiskLevel.LOW)
                "Low-risk local operation approved by policy"
            else "Explicit user consent recorded"
            AuthorizationDecision.DENIED -> "User declined consent for ${step.permission}"
            AuthorizationDecision.PENDING -> "Awaiting consent"
            AuthorizationDecision.NOT_REQUIRED -> "No authorization required"
        }
        return Authorization(Ids.next("auth"), plan.id, step.id, decision, reason)
    }
}
