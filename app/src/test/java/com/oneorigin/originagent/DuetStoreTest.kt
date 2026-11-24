package com.oneorigin.originagent

import com.oneorigin.originagent.duet.DuetPageKind
import com.oneorigin.originagent.duet.DuetStore
import com.oneorigin.originagent.duet.DuetTaskStatus
import com.oneorigin.originagent.duet.templateFor
import com.oneorigin.originagent.util.JsonCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DuetStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun store(): DuetStore = DuetStore(folder.newFolder())

    private fun taskLine(
        id: String,
        title: String,
        status: DuetTaskStatus = DuetTaskStatus.UP_NEXT,
        channelId: String? = null,
        createdAt: Long = 1L
    ): String = JsonCodec.encode(
        mapOf(
            "id" to id,
            "title" to title,
            "status" to status.name,
            "channelId" to channelId,
            "note" to "",
            "createdAt" to createdAt,
            "updatedAt" to createdAt
        )
    )

    private fun pageLine(id: String, title: String, createdAt: Long): String = JsonCodec.encode(
        mapOf(
            "id" to id,
            "title" to title,
            "kind" to "DOC",
            "content" to "body",
            "createdAt" to createdAt,
            "updatedAt" to createdAt
        )
    )

    private fun channelLine(id: String, name: String): String = JsonCodec.encode(
        mapOf(
            "id" to id,
            "name" to name,
            "emoji" to "•",
            "colorArgb" to 0xFF6430AB.toInt(),
            "purpose" to "",
            "archived" to false,
            "createdAt" to 1L
        )
    )

    @Test
    fun `enum labels are human readable and storage still uses enum names`() {
        assertEquals(
            mapOf(
                DuetTaskStatus.UP_NEXT to "Up next",
                DuetTaskStatus.WORKING to "Working",
                DuetTaskStatus.READY_FOR_REVIEW to "Ready for review",
                DuetTaskStatus.NEEDS_INPUT to "Needs input",
                DuetTaskStatus.RESOLVED to "Resolved"
            ),
            DuetTaskStatus.entries.associateWith { it.label }
        )
        assertEquals(
            mapOf(
                DuetPageKind.DESIGN_SYSTEM to "Design system",
                DuetPageKind.DESIGN to "Design",
                DuetPageKind.DASHBOARD to "Dashboard",
                DuetPageKind.SLIDES to "Slides",
                DuetPageKind.DOC to "Doc",
                DuetPageKind.PAGE to "Page"
            ),
            DuetPageKind.entries.associateWith { it.label }
        )

        val store = store()
        val task = store.addTask("Labelled")
        assertTrue(File(store.root, "tasks.jsonl").readText().contains("\"status\":\"UP_NEXT\""))
        assertNotNull(store.updateTask(task.id, status = DuetTaskStatus.RESOLVED))
        assertTrue(store.tasks(includeResolved = true).first().status.label == "Resolved")
    }

    @Test
    fun `tasks persist as jsonl and round trip`() {
        val root = folder.newFolder("roundtrip")
        val store = DuetStore(root)
        val first = store.addTask("Draft launch checklist", note = "Ask Sam for dates")
        val second = store.addTask("Review colors", channelId = "design")

        assertTrue(first.id.isNotBlank())
        assertNotEquals(first.id, second.id)
        assertEquals(DuetTaskStatus.UP_NEXT, first.status)
        assertNull(first.channelId)
        assertEquals("Ask Sam for dates", first.note)
        assertEquals(first.createdAt, first.updatedAt)
        assertTrue(first.createdAt > 0L)

        val lines = File(root, "tasks.jsonl").readLines()
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("\"title\":\"Draft launch checklist\""))
        assertTrue(lines[1].contains("\"channelId\":\"design\""))

        val reloaded = DuetStore(root)
        assertEquals(listOf(second.id, first.id), reloaded.tasks().map { it.id })
        assertEquals(second, reloaded.tasks().first())
    }

    @Test
    fun `tasks sort by newest timestamp and hide resolved by default`() {
        val store = store()
        File(store.root, "tasks.jsonl").writeText(
            listOf(
                taskLine("old", "Old", DuetTaskStatus.RESOLVED, createdAt = 100L),
                taskLine("newest", "Newest", createdAt = 300L),
                taskLine("middle", "Middle", DuetTaskStatus.NEEDS_INPUT, createdAt = 200L)
            ).joinToString("\n", postfix = "\n")
        )

        assertEquals(listOf("Newest", "Middle"), store.tasks().map { it.title })
        assertEquals(
            listOf("Newest", "Middle", "Old"),
            store.tasks(includeResolved = true).map { it.title }
        )
    }

    @Test
    fun `equal timestamps keep the newest inserted task first`() {
        val store = store()
        store.addTask("First")
        store.addTask("Second")
        store.addTask("Third")
        assertEquals(listOf("Third", "Second", "First"), store.tasks().map { it.title })
    }

    @Test
    fun `updateTask changes only supplied fields`() {
        val store = store()
        val original = store.addTask("Original", channelId = "c1", note = "Initial")
        val updated = store.updateTask(original.id, status = DuetTaskStatus.WORKING, note = "Revised")
        assertNotNull(updated)
        assertEquals("Original", updated!!.title)
        assertEquals("c1", updated.channelId)
        assertEquals("Revised", updated.note)
        assertEquals(DuetTaskStatus.WORKING, updated.status)
        assertTrue(updated.updatedAt >= original.updatedAt)
        assertEquals(updated, store.tasks().single())

        val moved = store.updateTask(original.id, channelId = "c2")
        assertEquals("c2", moved!!.channelId)
        assertEquals("Revised", moved.note)
        assertNull(store.updateTask("missing", title = "No such task"))
        assertEquals(moved, store.tasks().single())
    }

    @Test
    fun `removeTask deletes only the requested task`() {
        val store = store()
        val first = store.addTask("Keep me")
        val second = store.addTask("Delete me")
        assertTrue(store.removeTask(second.id))
        assertFalse(store.removeTask(second.id))
        assertEquals(listOf(first.id), store.tasks().map { it.id })
        assertEquals(1, File(store.root, "tasks.jsonl").readLines().size)
    }

    @Test
    fun `channels support defaults updates and archived filtering`() {
        val store = store()
        val design = store.addChannel("Design", emoji = "🎨", colorArgb = 0xFF123456.toInt(), purpose = "Visual work")
        val build = store.addChannel("Build")
        assertEquals("•", build.emoji)
        assertEquals(0xFF6430AB.toInt(), build.colorArgb)
        assertEquals("", build.purpose)
        assertFalse(build.archived)
        assertEquals(listOf(design.id, build.id), store.channels().map { it.id })

        val updated = store.updateChannel(design.id, name = "Design system", purpose = "Tokens", archived = true)
        assertNotNull(updated)
        assertEquals("Design system", updated!!.name)
        assertEquals("🎨", updated.emoji)
        assertEquals(0xFF123456.toInt(), updated.colorArgb)
        assertEquals("Tokens", updated.purpose)
        assertTrue(updated.archived)
        assertEquals(listOf(build.id), store.channels().map { it.id })
        assertEquals(2, store.channels(includeArchived = true).size)

        assertNull(store.updateChannel("missing", name = "Ghost"))
        val recolored = store.updateChannel(build.id, emoji = "🛠", colorArgb = 0xFF000000.toInt())
        assertEquals("Build", recolored!!.name)
        assertEquals("🛠", recolored.emoji)
        assertEquals(0xFF000000.toInt(), recolored.colorArgb)
        assertFalse(store.channels().first().archived)
    }

    @Test
    fun `removeChannel clears channel id on its tasks`() {
        val store = store()
        val design = store.addChannel("Design")
        val build = store.addChannel("Build")
        val a = store.addTask("Palette", channelId = design.id)
        val b = store.addTask("Tokens", channelId = design.id)
        val c = store.addTask("Compile", channelId = build.id)

        assertTrue(store.removeChannel(design.id))
        assertFalse(store.removeChannel(design.id))
        assertEquals(listOf(build.id), store.channels().map { it.id })

        val tasks = store.tasks().associateBy { it.id }
        assertNull(tasks[a.id]!!.channelId)
        assertNull(tasks[b.id]!!.channelId)
        assertEquals(build.id, tasks[c.id]!!.channelId)
        assertEquals(a.title, tasks[a.id]!!.title)
        assertTrue(tasks[a.id]!!.updatedAt >= a.updatedAt)
        assertEquals(3, store.tasks(includeResolved = true).size)

        // The cascade survives a reload and no stray references remain on disk.
        val reloaded = DuetStore(store.root)
        assertTrue(reloaded.tasks().none { it.channelId == design.id })
    }

    @Test
    fun `pages support CRUD and newest first ordering`() {
        val store = store()
        File(store.root, "pages.jsonl").writeText(
            listOf(pageLine("p-old", "Old", 10L), pageLine("p-new", "New", 50L), pageLine("p-mid", "Mid", 30L))
                .joinToString("\n", postfix = "\n")
        )
        assertEquals(listOf("New", "Mid", "Old"), store.pages().map { it.title })

        val added = store.addPage("Tokens", DuetPageKind.DESIGN_SYSTEM, templateFor(DuetPageKind.DESIGN_SYSTEM, "Colors"))
        assertEquals(DuetPageKind.DESIGN_SYSTEM, store.pages().first().kind)
        val edited = store.updatePage(added.id, title = "Tokens v2", content = "# Local edit")
        assertNotNull(edited)
        assertEquals("Tokens v2", edited!!.title)
        assertEquals("# Local edit", edited.content)
        assertEquals(DuetPageKind.DESIGN_SYSTEM, edited.kind)
        assertTrue(edited.updatedAt >= added.updatedAt)
        assertNull(store.updatePage("missing", content = "nope"))

        assertTrue(store.removePage("p-new"))
        assertFalse(store.removePage("p-new"))
        assertEquals(setOf("p-old", "p-mid", added.id), store.pages().map { it.id }.toSet())
        assertEquals(3, File(store.root, "pages.jsonl").readLines().size)
    }

    @Test
    fun `malformed jsonl lines are skipped and dropped on the next write`() {
        val root = folder.newFolder("malformed")
        File(root, "tasks.jsonl").writeText(
            listOf(
                "",
                "not json at all",
                "{\"id\":",
                taskLine("t-good", "Good", createdAt = 50L),
                "{\"id\":\"t-missing-title\"}",
                "{\"id\":\"t-bad-status\",\"title\":\"Bad\",\"status\":\"WAT\"}"
            ).joinToString("\n", postfix = "\n")
        )
        File(root, "channels.jsonl").writeText(
            listOf("{broken", channelLine("c-good", "Good"), "{\"id\":\"c-bad\"}").joinToString("\n", postfix = "\n")
        )
        File(root, "pages.jsonl").writeText("{broken\n${pageLine("p-good", "Good", 2L)}\n")

        val store = DuetStore(root)
        assertEquals(listOf("t-good"), store.tasks().map { it.id })
        assertEquals(listOf("c-good"), store.channels(includeArchived = true).map { it.id })
        assertEquals(listOf("p-good"), store.pages().map { it.id })

        val repaired = store.addTask("After repair")
        val lines = File(root, "tasks.jsonl").readLines()
        assertEquals(2, lines.size)
        assertEquals(repaired.id, store.tasks().first().id)
        assertFalse(File(root, "tasks.jsonl").readText().contains("t-bad"))
        assertFalse(File(root, "tasks.jsonl").readText().contains("WAT"))
        assertFalse(File(root, "tasks.jsonl.tmp").exists())
    }

    @Test
    fun `templates are concise local drafts for every page kind`() {
        val aiWord = Regex("\\bAI\\b", RegexOption.IGNORE_CASE)
        DuetPageKind.entries.forEach { kind ->
            val text = templateFor(kind, "Origin Duet")
            assertTrue(kind.name, text.contains("Origin Duet"))
            assertTrue(kind.name, text.contains("local draft", ignoreCase = true))
            assertTrue(kind.name, text.length > 80)
            assertFalse(kind.name, aiWord.containsMatchIn(text))
            assertFalse(kind.name, text.contains("generated by", ignoreCase = true))
            assertEquals(text, templateFor(kind, "Origin Duet"))
        }

        val drafts = DuetPageKind.entries.map { templateFor(it, "Same topic") }
        assertEquals(drafts.size, drafts.toSet().size)

        assertTrue(templateFor(DuetPageKind.DESIGN_SYSTEM, "Colors").contains("#6430AB"))
        assertTrue(templateFor(DuetPageKind.DASHBOARD, "Health").contains("Panels"))
        assertTrue(templateFor(DuetPageKind.SLIDES, "Pitch").contains("1. Title: Pitch"))
        assertTrue(templateFor(DuetPageKind.DOC, "Spec").contains("## Summary"))
        assertTrue(templateFor(DuetPageKind.DESIGN, "Brief").contains("## Goal"))
        assertTrue(templateFor(DuetPageKind.PAGE, "Home").contains("## Above the fold"))

        val blank = templateFor(DuetPageKind.PAGE, "  \n  ")
        assertEquals(templateFor(DuetPageKind.PAGE, "Untitled"), blank)
        val multiline = templateFor(DuetPageKind.PAGE, "One\nTwo")
        assertTrue(multiline.contains("One Two"))
        assertFalse(multiline.contains("One\nTwo"))
    }
}
