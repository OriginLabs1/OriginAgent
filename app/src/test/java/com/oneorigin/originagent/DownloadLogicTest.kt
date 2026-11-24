package com.oneorigin.originagent

import com.oneorigin.originagent.util.ModelDownloadManager
import com.oneorigin.originagent.util.download.DownloadPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure resume/retry policy tests: no Android, no sockets, no file system. */
class DownloadLogicTest {

    @Test
    fun onlyStrictlyPartialFilesAreResumed() {
        assertEquals(0L, DownloadPlan.resumeOffset(0L, 1_000L))
        assertEquals(250L, DownloadPlan.resumeOffset(250L, 1_000L))
        assertEquals(999L, DownloadPlan.resumeOffset(999L, 1_000L))
        assertFalse(DownloadPlan.shouldRestart(250L, 1_000L))

        assertTrue(DownloadPlan.shouldRestart(0L, 1_000L) == false)
        assertTrue(DownloadPlan.shouldRestart(1_000L, 1_000L))
        assertTrue(DownloadPlan.shouldRestart(1_001L, 1_000L))
        assertTrue(DownloadPlan.shouldRestart(-1L, 1_000L))
        assertTrue(DownloadPlan.shouldRestart(10L, 0L))
        assertTrue(DownloadPlan.shouldRestart(10L, -5L))
    }

    @Test
    fun resumePlanCarriesRangeHeader() {
        val resume = DownloadPlan.fromPartialLength(512L, 1_024L)
        assertTrue(resume.shouldResume)
        assertFalse(resume.shouldRestart)
        assertEquals(512L, resume.resumeOffset)
        assertEquals("bytes=512-", resume.rangeHeader)

        val fresh = DownloadPlan.fromPartialLength(0L, 1_024L)
        assertFalse(fresh.shouldResume)
        assertNull(fresh.rangeHeader)
        assertEquals(DownloadPlan.decide(512L, 1_024L), resume)
    }

    @Test
    fun matching206IsAcceptedAnd200OnResumeRestarts() {
        val accepted = DownloadPlan.fromResponse(206, 512L, 512L)
        assertTrue(accepted.shouldResume)
        assertEquals(512L, accepted.resumeOffset)

        val ignoredRange = DownloadPlan.fromResponse(200, null, 512L)
        assertTrue(ignoredRange.shouldRestart)
        assertFalse(ignoredRange.shouldResume)

        val wrongStart = DownloadPlan.fromResponse(206, 256L, 512L)
        assertTrue(wrongStart.shouldRestart)
        assertTrue(DownloadPlan.fromResponse(206, null, 512L).shouldRestart)
        assertTrue(DownloadPlan.fromResponse(500, 512L, 512L).shouldRestart)
        assertEquals(accepted, DownloadPlan.afterResponse(206, 512L, 512L))
    }

    @Test
    fun freshRequestAccepts200Or206FromZero() {
        val ok = DownloadPlan.fromResponse(200, null, 0L)
        assertFalse(ok.shouldRestart)
        assertEquals(0L, ok.resumeOffset)
        assertFalse(DownloadPlan.fromResponse(206, 0L, 0L).shouldRestart)
        assertTrue(DownloadPlan.fromResponse(206, 10L, 0L).shouldRestart)
        assertTrue(DownloadPlan.fromResponse(503, null, 0L).shouldRestart)
    }

    @Test
    fun contentRangeParsingIsStrict() {
        assertEquals(100L, DownloadPlan.contentRangeStart("bytes 100-199/200"))
        assertEquals(0L, DownloadPlan.contentRangeStart("bytes 0-0/*"))
        assertEquals(4096L, DownloadPlan.contentRangeStart("BYTES 4096-8191/8192"))
        assertNull(DownloadPlan.contentRangeStart(null))
        assertNull(DownloadPlan.contentRangeStart(""))
        assertNull(DownloadPlan.contentRangeStart("bytes */200"))
        assertNull(DownloadPlan.contentRangeStart("bytes 200-100/200"))
        assertNull(DownloadPlan.contentRangeStart("bytes -1-10/200"))
        assertNull(DownloadPlan.contentRangeStart("items 1-2/3"))
    }

    @Test
    fun retryBackoffIsExponentialAndCapped() {
        assertEquals(200L, DownloadPlan.retryDelayMs(1))
        assertEquals(400L, DownloadPlan.retryDelayMs(2))
        assertEquals(800L, DownloadPlan.retryDelayMs(3))
        assertEquals(1_600L, DownloadPlan.retryDelayMs(4))
        assertEquals(2_000L, DownloadPlan.retryDelayMs(5))
        assertEquals(2_000L, DownloadPlan.retryDelayMs(50))
        assertEquals(200L, DownloadPlan.retryDelayMs(0))
        assertEquals(400L, DownloadPlan.retryDelay(2))
        assertEquals(5, DownloadPlan.MAX_ATTEMPTS)
    }

    @Test
    fun progressUsesPinnedSizeInsteadOfContentLength() {
        assertEquals(0, DownloadPlan.progressPercent(0L, 1_000L))
        assertEquals(42, DownloadPlan.progressPercent(420L, 1_000L))
        assertEquals(99, DownloadPlan.progressPercent(999L, 1_000L))
        assertEquals(100, DownloadPlan.progressPercent(1_000L, 1_000L))
        assertEquals(0, DownloadPlan.progressPercent(-1L, 1_000L))
        assertEquals(0, DownloadPlan.progressPercent(10L, 0L))
        assertEquals(100, DownloadPlan.progressPercent(5_000L, 1_000L))
        assertEquals(42, DownloadPlan.progress(420L, 1_000L))
    }

    @Test
    fun failureMessagesAreFriendly() {
        assertEquals("Connection interrupted — resuming…", DownloadPlan.RETRY_MESSAGE)
        assertEquals(
            "Download paused at 42% — tap Get Model to resume",
            DownloadPlan.pausedMessage(42)
        )
        assertEquals("Download paused at 0% — tap Get Model to resume", DownloadPlan.pausedMessage(-1))
    }

    @Test
    fun downloadManagerPublicApiIsUnchanged() {
        val manager = ModelDownloadManager::class.java
        val listenerType = Class.forName("kotlin.jvm.functions.Function2")
        assertNotNull(manager.getMethod("addListener", listenerType))
        assertNotNull(manager.getMethod("removeListener", listenerType))
        assertNotNull(manager.getMethod("status", com.oneorigin.originagent.models.DownloadableModelSpec::class.java))
        assertNotNull(manager.getMethod("isReady", com.oneorigin.originagent.models.DownloadableModelSpec::class.java))
        assertNotNull(manager.getMethod("file", com.oneorigin.originagent.models.DownloadableModelSpec::class.java))
        assertNotNull(manager.getMethod("path", com.oneorigin.originagent.models.DownloadableModelSpec::class.java))
        assertNotNull(manager.getMethod("progress", String::class.java))
        assertNotNull(manager.getMethod("formatSize", Long::class.javaPrimitiveType))
        assertNotNull(
            manager.getMethod(
                "download",
                com.oneorigin.originagent.models.DownloadableModelSpec::class.java,
                kotlinx.coroutines.CoroutineScope::class.java
            )
        )
        assertTrue(
            manager.declaredMethods.any {
                it.name.startsWith("downloadSuspend") && it.parameterCount == 2
            }
        )
        assertNotNull(manager.getMethod("cancel", com.oneorigin.originagent.models.DownloadableModelSpec::class.java))
        assertNotNull(manager.getMethod("delete", com.oneorigin.originagent.models.DownloadableModelSpec::class.java))
        assertEquals(4, ModelDownloadManager.State.values().size)
    }
}
