package com.oneorigin.originagent.models

/** Always-available offline engine. Honest, deterministic and useful without a model. */
class LocalDeterministicEngine : InferenceEngine {
    override val id = "local.deterministic"
    override val displayName = "Origin Local"
    override val description = "Deterministic offline reasoning and capability guidance. No model required."
    override val capabilities = setOf("offline", "tools", "knowledge", "trust")

    override fun isAvailable(): Boolean = true

    override suspend fun generate(request: GenerationRequest): GenerationResult {
        val text = request.prompt.trim().lowercase()
        val answer = when {
            text.isEmpty() -> "I'm here. Ask me to calculate something, remember a note, or explain how Origin Agent protects your actions."
            text.contains("what can you do") || text.contains("capabilit") -> capabilityAnswer()
            text.contains("trust") || text.contains("provenance") || text.contains("verif") -> trustAnswer()
            text.contains("offline") || text.contains("model") -> modelAnswer()
            text.contains("hello") || text.contains("hi ") || text == "hi" -> "Hello. I'm Origin, running locally. I can calculate, search knowledge, store notes and show your trust ledger."
            else -> fallbackAnswer()
        }
        return GenerationResult.Success(answer, id)
    }

    private fun capabilityAnswer() = buildString {
        append("I can help with: arithmetic (try 'calculate 12% of 840'), date and time, local knowledge, ")
        append("notes you approve, and an inspectable trust ledger. ")
        append("Every consequential action is planned, authorized, verified and provenance-recorded.")
    }

    private fun trustAnswer() = buildString {
        append("Origin Agent follows Intent → Plan → Authorization → Execute → Verify → Persist → Provenance → Result. ")
        append("Blocked, attempted and verified outcomes are shown separately, and the provenance chain is hash-linked so tampering is detectable offline.")
    }

    private fun modelAnswer() = buildString {
        append("I'm the deterministic local engine, so I work without a model or internet. ")
        append("BitNet b1.58, LFM2.5-class, compact and Qwen-class models are optional candidates; ")
        append("each must earn its place through evidence. No weights are bundled.")
    }

    private fun fallbackAnswer() = buildString {
        append("I don't have a confident answer from the offline engine. ")
        append("I can still calculate, search local knowledge, save a note with your approval, or show your trust ledger. ")
        append("No network request was made.")
    }
}
