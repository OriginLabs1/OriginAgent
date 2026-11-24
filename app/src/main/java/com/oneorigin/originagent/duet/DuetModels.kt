package com.oneorigin.originagent.duet

/** Workflow states for a Duet task. [label] is the human-readable UI text. */
enum class DuetTaskStatus(val label: String) {
    UP_NEXT("Up next"),
    WORKING("Working"),
    READY_FOR_REVIEW("Ready for review"),
    NEEDS_INPUT("Needs input"),
    RESOLVED("Resolved")
}

/** One item on the local Duet board. Timestamps are epoch milliseconds. */
data class DuetTask(
    val id: String,
    val title: String,
    val status: DuetTaskStatus,
    val channelId: String?,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long
)

/** A user-created place to group work. [colorArgb] is an Android ARGB color int. */
data class DuetChannel(
    val id: String,
    val name: String,
    val emoji: String,
    val colorArgb: Int,
    val purpose: String,
    val archived: Boolean,
    val createdAt: Long
)

/** The kind of draft a Duet page holds. [label] is the human-readable UI text. */
enum class DuetPageKind(val label: String) {
    DESIGN_SYSTEM("Design system"),
    DESIGN("Design"),
    DASHBOARD("Dashboard"),
    SLIDES("Slides"),
    DOC("Doc"),
    PAGE("Page")
}

/** A locally stored, editable document. Timestamps are epoch milliseconds. */
data class DuetPage(
    val id: String,
    val title: String,
    val kind: DuetPageKind,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Concise starting drafts written entirely on the device. These are outlines with
 * bracketed prompts, not model output: fill them in and edit freely.
 */
fun templateFor(kind: DuetPageKind, topic: String): String {
    val subject = topic.trim().replace('\n', ' ').replace('\r', ' ').ifBlank { "Untitled" }
    return when (kind) {
        DuetPageKind.DESIGN_SYSTEM -> """
            # $subject — design tokens (local draft)

            Replace the placeholders with values the app actually uses.

            ## Color
            - Primary: #6430AB (placeholder)
            - Surface: #FFFFFF / #121212 (placeholder)
            - Text: #1B1B1F on light, #ECECF1 on dark
            - Semantic: success #2E7D32, warning #B26A00, error #B3261E (placeholders)

            ## Type
            - Display 32/40 semibold · Title 20/28 medium · Body 16/24 · Caption 12/16

            ## Space and shape
            - Spacing scale: 4, 8, 12, 16, 24, 32 dp
            - Radius: 8 dp cards, 999 dp pills
            - Elevation: 0, 2, 8 dp

            ## Checks
            - Body text contrast >= 4.5:1; touch targets >= 48 dp
            - Confirm every token appears on a real screen
        """.trimIndent()
        DuetPageKind.DESIGN -> """
            # $subject — design brief (local draft)

            ## Goal
            - Problem being solved: [fill in]

            ## Audience
            - Who uses this, and in what context: [fill in]

            ## Scope
            - In: [fill in]
            - Out: [fill in]

            ## Success signal
            - One observable outcome: [fill in]

            ## Constraints and open questions
            - Platform, accessibility, deadline: [fill in]
            - Decisions still needed: [fill in]
        """.trimIndent()
        DuetPageKind.DASHBOARD -> """
            # $subject — dashboard outline (local draft)

            ## Purpose
            - Decision this dashboard supports: [fill in]

            ## Audience and cadence
            - Primary viewer: [fill in] · Checked: [daily / weekly, choose]

            ## Panels
            1. Headline metric — source: [fill in], target: [fill in]
            2. Trend over time — range: [fill in]
            3. Breakdown by [dimension] — source: [fill in]
            4. Alerts / needs attention — rule: [fill in]

            ## Data and checks
            - Sources and freshness: [fill in]
            - Empty, loading, and error states: [fill in]
        """.trimIndent()
        DuetPageKind.SLIDES -> """
            # $subject — slide outline (local draft)

            1. Title: $subject — subtitle: [fill in]
            2. Why now: [one problem statement]
            3. Audience and context: [fill in]
            4. Proposal: [one clear idea]
            5. Evidence: [metric, quote, or example — fill in]
            6. Trade-offs: [fill in]
            7. Next steps: [owners and dates]

            Keep one idea per slide; replace every bracket before presenting.
        """.trimIndent()
        DuetPageKind.DOC -> """
            # $subject — document outline (local draft)

            ## Summary
            - What this is and why it exists: [fill in]

            ## Background
            - Context and prior decisions: [fill in]

            ## Proposal
            - Details, scope, and non-goals: [fill in]

            ## Open questions
            - [fill in]

            ## Decisions and owners
            - Decision: [fill in] · Owner: [fill in] · Date: [fill in]
        """.trimIndent()
        DuetPageKind.PAGE -> """
            # $subject — page outline (local draft)

            ## Above the fold
            - Heading: $subject
            - One-sentence purpose: [fill in]
            - Primary action: [fill in]

            ## Supporting sections
            - Details / content: [fill in]
            - Secondary actions: [fill in]

            ## States
            - Empty: [fill in] · Loading: [fill in] · Error: [fill in]

            ## Checks
            - Reading order, contrast, and mobile width verified
        """.trimIndent()
    }
}
