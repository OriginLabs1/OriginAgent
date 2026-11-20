package com.oneorigin.originagent.memory

/** Small, portable knowledge source. Bundled seeds never pretend to be user data. */
class KnowledgeBase {

    data class Entry(val title: String, val content: String, val source: String)

    private val seeds = listOf(
        Entry(
            "Origin Agent identity",
            "Origin Agent is a universal, extensible and trustworthy intelligent agent environment. Origin AI is its model-agnostic intelligence foundation. The product is Origin Agent.",
            "CONSTITUTION.md §1"
        ),
        Entry(
            "Trustworthy execution",
            "Every consequential action follows Intent, Plan, Authorization, Execute, Verify, Persist, Provenance, Result. Origin Agent distinguishes proposed actions from authorized actions and attempts from verified success.",
            "CONSTITUTION.md §9"
        ),
        Entry(
            "Offline usefulness",
            "The Universal Intelligence Harness remains useful with no AI model and no internet connection. Deterministic native tools and the local engine provide a baseline.",
            "CONSTITUTION.md §5"
        ),
        Entry(
            "Model agnosticism",
            "No model earns permanent preference without demonstrable superiority. BitNet b1.58, LFM2.5-class, optimized sub-1B and Qwen-class compact models are strategic candidates, not a ranking.",
            "CONSTITUTION.md §6"
        ),
        Entry(
            "User sovereignty",
            "Memory is user-controlled, structured, portable and exportable. There is no mandatory subscription, provider or centralized cloud.",
            "CONSTITUTION.md §10 and §13"
        ),
        Entry(
            "Reference device",
            "Samsung Galaxy S9 (Snapdragon 845, 4 GB RAM, Android 10) is an initial demanding reference device, not the architectural boundary.",
            "CONSTITUTION.md §14"
        )
    )

    fun search(query: String): List<Entry> {
        val tokens = query.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length > 2 }
        if (tokens.isEmpty()) return seeds.take(3)
        return seeds.map { entry ->
            val haystack = "${entry.title} ${entry.content}".lowercase()
            val score = tokens.sumOf { token ->
                if (haystack.contains(token)) 2 + token.length.coerceAtMost(6) else 0
            }
            entry to score
        }.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }
    }

    fun all(): List<Entry> = seeds
}
