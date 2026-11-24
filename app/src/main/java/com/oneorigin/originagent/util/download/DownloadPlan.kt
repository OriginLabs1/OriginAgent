package com.oneorigin.originagent.util.download

/**
 * Pure, dependency-free decisions for a resumable download.
 *
 * No Android types, no networking and no I/O live here: every choice is derived from plain
 * numbers so the resume/retry policy can be tested exhaustively on the JVM. The transfer loop
 * in [com.oneorigin.originagent.util.ModelDownloadManager] applies these decisions to a real
 * HTTPS connection and a `.part` file.
 */
data class DownloadPlan(
    /** First byte that still needs to be fetched; zero when starting from scratch. */
    val resumeOffset: Long = 0L,
    /** True when the existing partial file is unusable and must be discarded. */
    val restart: Boolean = false,
    /** Milliseconds to wait before the retry that follows this decision. */
    val retryDelayMs: Long = 0L
) {
    val shouldResume: Boolean get() = !restart && resumeOffset > 0L
    val shouldRestart: Boolean get() = restart

    /** Value for the HTTP Range header, or null for a plain full GET. */
    val rangeHeader: String? get() = if (shouldResume) "bytes=$resumeOffset-" else null

    companion object {
        /** Total attempts for one download call, including the first one. */
        const val MAX_ATTEMPTS = 5
        const val BASE_RETRY_DELAY_MS = 200L
        const val MAX_RETRY_DELAY_MS = 2_000L
        const val RETRY_MESSAGE = "Connection interrupted — resuming…"

        fun pausedMessage(percent: Int): String =
            "Download paused at ${percent.coerceIn(0, 100)}% — tap Get Model to resume"

        /**
         * Decide what to do with an existing `.part` file before making a request.
         * Only a strictly partial file (1 .. expectedBytes-1) may be resumed; an empty,
         * complete or oversized file is started over.
         */
        fun fromPartialLength(partialLength: Long, expectedBytes: Long): DownloadPlan {
            if (expectedBytes <= 0L) return DownloadPlan(restart = true)
            if (partialLength <= 0L) return DownloadPlan(restart = partialLength < 0L)
            if (partialLength >= expectedBytes) return DownloadPlan(restart = true)
            return DownloadPlan(resumeOffset = partialLength)
        }

        /** Alias with a decision-oriented name. */
        fun decide(partialLength: Long, expectedBytes: Long): DownloadPlan =
            fromPartialLength(partialLength, expectedBytes)

        fun resumeOffset(partialLength: Long, expectedBytes: Long): Long =
            fromPartialLength(partialLength, expectedBytes).resumeOffset

        fun shouldRestart(partialLength: Long, expectedBytes: Long): Boolean =
            fromPartialLength(partialLength, expectedBytes).restart

        /**
         * Validate a server's response to a resume request. A 206 is only usable when its
         * Content-Range starts exactly where the partial file ends. A 200 (or a mismatched
         * range) means the server ignored the Range header: discard the partial and restart.
         */
        fun fromResponse(httpCode: Int, contentRangeStart: Long?, requestedOffset: Long): DownloadPlan {
            if (requestedOffset <= 0L) {
                // Fresh request: 200 is normal; a 206 beginning at zero is equally usable.
                return if (httpCode == 200 || (httpCode == 206 && (contentRangeStart ?: 0L) == 0L)) {
                    DownloadPlan()
                } else {
                    DownloadPlan(restart = true)
                }
            }
            return if (httpCode == 206 && contentRangeStart == requestedOffset) {
                DownloadPlan(resumeOffset = requestedOffset)
            } else {
                DownloadPlan(restart = true)
            }
        }

        /** Alias for [fromResponse]. */
        fun afterResponse(httpCode: Int, contentRangeStart: Long?, requestedOffset: Long): DownloadPlan =
            fromResponse(httpCode, contentRangeStart, requestedOffset)

        /** Parse a Content-Range such as "bytes 100-199/200" into its start value. */
        fun contentRangeStart(header: String?): Long? {
            if (header.isNullOrBlank()) return null
            val separator = header.indexOf(' ')
            if (separator <= 0) return null
            if (!header.take(separator).equals("bytes", ignoreCase = true)) return null
            val range = header.substring(separator + 1).substringBefore('/').trim()
            val start = range.substringBefore('-').trim().toLongOrNull() ?: return null
            val end = range.substringAfter('-', "").trim().toLongOrNull() ?: return null
            if (start < 0L || end < start) return null
            return start
        }

        /** Short, capped exponential backoff; [failedAttempt] is one-based. */
        fun retryDelayMs(failedAttempt: Int): Long {
            val shift = (failedAttempt - 1).coerceAtLeast(0).coerceAtMost(20)
            return (BASE_RETRY_DELAY_MS shl shift).coerceAtMost(MAX_RETRY_DELAY_MS)
        }

        /** Alias for [retryDelayMs]. */
        fun retryDelay(failedAttempt: Int): Long = retryDelayMs(failedAttempt)

        /** Progress derived from bytes written toward the pinned size, no Content-Length needed. */
        fun progressPercent(bytesWritten: Long, expectedBytes: Long): Int {
            if (expectedBytes <= 0L || bytesWritten <= 0L) return 0
            return ((bytesWritten.toDouble() * 100.0) / expectedBytes.toDouble())
                .toInt().coerceIn(0, 100)
        }

        /** Alias for [progressPercent]. */
        fun progress(bytesWritten: Long, expectedBytes: Long): Int =
            progressPercent(bytesWritten, expectedBytes)
    }
}
