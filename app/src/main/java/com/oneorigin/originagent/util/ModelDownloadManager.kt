package com.oneorigin.originagent.util

import android.content.Context
import com.oneorigin.originagent.models.DownloadableModelSpec
import com.oneorigin.originagent.models.LlamaRuntime
import com.oneorigin.originagent.util.download.DownloadPlan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Installs, verifies and stores model weights entirely on-device.
 *
 * No weight file is ever assumed present and no user-supplied file is accepted: the only
 * source is the published HTTPS URL in [DownloadableModelSpec]. A file is promoted into
 * place only after its size and SHA-256 match, so a truncated or tampered artifact can
 * never become active. Transfers are resumable: a `.part` file survives interrupted
 * connections and cancellation, and is retried with a Range request instead of being
 * thrown away.
 */
class ModelDownloadManager(context: Context) {

    enum class State { NOT_DOWNLOADED, DOWNLOADING, READY, FAILED }

    data class Status(
        val state: State,
        val progress: Int = 0,
        val message: String = ""
    )

    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, "models").apply { mkdirs() }
    private val prefs = appContext.getSharedPreferences("origin_model_downloads", Context.MODE_PRIVATE)
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val jobs = ConcurrentHashMap<String, Job>()
    private val listeners = CopyOnWriteArrayList<(String, Status) -> Unit>()

    /** Message we authored ourselves; safe to show verbatim. Raw exceptions are never published. */
    private class FriendlyFailure(message: String) : IOException(message)

    private class HttpFailure(val code: Int) : IOException("HTTP $code")

    private fun lock(id: String) = locks.getOrPut(id) { Mutex() }

    private fun target(spec: DownloadableModelSpec) = File(root, spec.fileName)
    private fun partial(spec: DownloadableModelSpec) = File(root, "${spec.fileName}.part")

    /** Keep the `.part` inode but discard its unusable bytes before starting over. */
    private fun truncate(file: File) {
        RandomAccessFile(file, "rw").use { it.setLength(0L) }
    }

    fun addListener(listener: (String, Status) -> Unit) {
        listeners.addIfAbsent(listener)
    }

    fun removeListener(listener: (String, Status) -> Unit) {
        listeners.remove(listener)
    }

    fun status(spec: DownloadableModelSpec): Status {
        val file = target(spec)
        val marker = prefs.getString("verified.${spec.modelId}", null)
        // Trust the marker only together with the pinned size. Hashing a 1 GB GGUF here
        // would block the UI thread, so an unmarked file is simply not ready yet.
        if (file.exists() && file.length() == spec.sizeBytes && marker == spec.sha256) {
            return Status(State.READY, 100, "Verified and ready offline")
        }
        val job = jobs[spec.modelId]
        if (job?.isActive == true) {
            return Status(State.DOWNLOADING, progress(spec.modelId), "Downloading verified weights…")
        }
        if (file.exists()) {
            return Status(
                State.NOT_DOWNLOADED,
                0,
                if (marker == spec.sha256) {
                    "Weights need checking — tap Get Model"
                } else {
                    "Incomplete weights — tap Get Model to try again"
                }
            )
        }
        val failure = prefs.getString("error.${spec.modelId}", null)
        return if (failure.isNullOrBlank()) {
            Status(State.NOT_DOWNLOADED, 0, "Not downloaded · ${formatSize(spec.sizeBytes)}")
        } else {
            Status(State.FAILED, progress(spec.modelId), failure)
        }
    }

    fun isReady(spec: DownloadableModelSpec): Boolean = status(spec).state == State.READY

    fun file(spec: DownloadableModelSpec): File? = target(spec).takeIf { isReady(spec) }

    /** Absolute destination path, whether or not the weights exist yet. */
    fun path(spec: DownloadableModelSpec): File = target(spec)

    fun progress(modelId: String): Int = prefs.getInt("progress.$modelId", 0)

    fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }

    /** Starts an install. Repeated calls while one is active return that job. */
    fun download(spec: DownloadableModelSpec, scope: kotlinx.coroutines.CoroutineScope): Job {
        jobs[spec.modelId]?.takeIf { it.isActive }?.let { return it }
        val job = scope.launch { downloadSuspend(spec) }
        jobs[spec.modelId] = job
        job.invokeOnCompletion { jobs.remove(spec.modelId, job) }
        return job
    }

    /** Suspending variant used by callers that need the final result. */
    suspend fun downloadSuspend(spec: DownloadableModelSpec): Result<File> = withContext(Dispatchers.IO) {
        try {
            lock(spec.modelId).withLock {
                val destination = target(spec)
                val temp = partial(spec)

                // A complete, already verified target needs no network at all. An unmarked
                // target of the right size is hashed here (on IO), never from status().
                if (destination.exists() && destination.length() == spec.sizeBytes &&
                    (prefs.getString("verified.${spec.modelId}", null) == spec.sha256 ||
                        sha256(destination).equals(spec.sha256, ignoreCase = true))
                ) {
                    publish(spec, Status(State.READY, 100, "Verified and ready offline"))
                    return@withLock Result.success(destination)
                }

                publish(spec, Status(State.DOWNLOADING, 0, "Connecting…"))

                // A previous attempt may have finished the bytes but not the hash yet.
                if (temp.exists() && temp.length() == spec.sizeBytes &&
                    verifyAndPromote(temp, destination, spec)
                ) {
                    publish(spec, Status(State.READY, 100, "Verified and ready offline"))
                    return@withLock Result.success(destination)
                }

                downloadFromUrl(spec, temp) { percent ->
                    publish(
                        spec,
                        Status(State.DOWNLOADING, percent.coerceAtMost(99), "Downloading verified weights…")
                    )
                }
                if (!verifyAndPromote(temp, destination, spec)) {
                    throw FriendlyFailure("Checksum mismatch — weights discarded. Tap Get Model to try again.")
                }

                publish(spec, Status(State.READY, 100, "Verified and ready offline"))
                Result.success(destination)
            }
        } catch (cancelled: CancellationException) {
            // Cancellation is not a download failure: keep the partial and stay quiet.
            throw cancelled
        } catch (error: Exception) {
            val percent = DownloadPlan.progressPercent(partial(spec).length(), spec.sizeBytes)
            val message = (error as? FriendlyFailure)?.message
                ?: DownloadPlan.pausedMessage(percent)
            publish(spec, Status(State.FAILED, percent, message))
            Result.failure(IOException(message))
        }
    }

    /**
     * Resumable transfer loop. Every attempt re-reads the part length, asks [DownloadPlan]
     * what to do, and keeps whatever valid bytes survived a premature EOF or IOException.
     */
    private suspend fun downloadFromUrl(
        spec: DownloadableModelSpec,
        temp: File,
        onProgress: (Int) -> Unit
    ) {
        var attempt = 1
        while (true) {
            currentCoroutineContext().ensureActive()
            if (attempt > DownloadPlan.MAX_ATTEMPTS) {
                val percent = DownloadPlan.progressPercent(temp.length(), spec.sizeBytes)
                throw FriendlyFailure(DownloadPlan.pausedMessage(percent))
            }

            val existing = if (temp.exists()) temp.length() else 0L
            val plan = DownloadPlan.fromPartialLength(existing, spec.sizeBytes)
            if (plan.restart && existing > 0L) truncate(temp)
            val offset = if (plan.restart) 0L else plan.resumeOffset

            var connection: HttpURLConnection? = null
            try {
                connection = URL(spec.url).openConnection() as HttpURLConnection
                connection.connectTimeout = 20_000
                connection.readTimeout = 30_000
                connection.instanceFollowRedirects = true
                connection.requestMethod = "GET"
                plan.rangeHeader?.let { connection.setRequestProperty("Range", it) }

                val code = connection.responseCode
                if (code !in 200..299) throw HttpFailure(code)

                val rangeStart = DownloadPlan.contentRangeStart(connection.getHeaderField("Content-Range"))
                val responsePlan = DownloadPlan.fromResponse(code, rangeStart, offset)
                if (responsePlan.restart) {
                    // The server ignored our Range or sent a different segment: start over.
                    if (offset > 0L && temp.exists()) truncate(temp)
                    publish(
                        spec,
                        Status(
                            State.DOWNLOADING,
                            DownloadPlan.progressPercent(0L, spec.sizeBytes),
                            DownloadPlan.RETRY_MESSAGE
                        )
                    )
                    delay(DownloadPlan.retryDelayMs(attempt))
                    attempt++
                    continue
                }

                FileOutputStream(temp, offset > 0L).use { output ->
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var written = offset
                        var lastPercent = -1
                        while (written < spec.sizeBytes) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) throw IOException("Premature end of stream")
                            if (read == 0) continue
                            output.write(buffer, 0, read)
                            written += read
                            val percent = DownloadPlan.progressPercent(written, spec.sizeBytes)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
                return
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (error is HttpFailure && error.code == 416 && temp.exists()) {
                    // The server no longer has a file at that offset; fetch it from zero.
                    truncate(temp)
                }
                val percent = DownloadPlan.progressPercent(temp.length(), spec.sizeBytes)
                publish(
                    spec,
                    Status(State.DOWNLOADING, percent.coerceAtMost(99), DownloadPlan.RETRY_MESSAGE)
                )
                if (attempt >= DownloadPlan.MAX_ATTEMPTS) {
                    throw FriendlyFailure(DownloadPlan.pausedMessage(percent))
                }
                delay(DownloadPlan.retryDelayMs(attempt))
                attempt++
            } finally {
                connection?.disconnect()
            }
        }
    }

    /** Size and SHA-256 are checked on Dispatchers.IO before an atomic rename. */
    private suspend fun verifyAndPromote(
        temp: File,
        destination: File,
        spec: DownloadableModelSpec
    ): Boolean = withContext(Dispatchers.IO) {
        if (!temp.exists() || temp.length() != spec.sizeBytes) return@withContext false
        if (!sha256(temp).equals(spec.sha256, ignoreCase = true)) {
            // Never promote corrupt bytes; truncate so the next tap downloads them again.
            truncate(temp)
            return@withContext false
        }
        if (destination.exists() && !destination.delete()) {
            throw FriendlyFailure("Could not replace old weights — tap Delete and try again.")
        }
        if (!temp.renameTo(destination)) {
            temp.copyTo(destination, overwrite = true)
            temp.delete()
        }
        true
    }

    fun cancel(spec: DownloadableModelSpec) {
        jobs[spec.modelId]?.cancel()
        // The `.part` file is deliberately kept: the next Get Model resumes from it.
        LlamaRuntime.release(spec.modelId)
        publish(spec, Status(State.NOT_DOWNLOADED, 0, "Download cancelled"))
    }

    fun delete(spec: DownloadableModelSpec) {
        jobs[spec.modelId]?.cancel()
        partial(spec).delete()
        target(spec).delete()
        prefs.edit()
            .remove("verified.${spec.modelId}")
            .remove("error.${spec.modelId}")
            .remove("progress.${spec.modelId}")
            .apply()
        LlamaRuntime.release(spec.modelId)
        publish(spec, Status(State.NOT_DOWNLOADED, 0, "Weights removed from this device"))
    }

    private fun publish(spec: DownloadableModelSpec, status: Status) {
        val editor = prefs.edit().putInt("progress.${spec.modelId}", status.progress)
        when (status.state) {
            State.READY -> editor.putString("verified.${spec.modelId}", spec.sha256)
            State.FAILED -> editor.remove("verified.${spec.modelId}")
                .putString("error.${spec.modelId}", status.message)
            State.DOWNLOADING, State.NOT_DOWNLOADED -> Unit
        }
        if (status.state != State.FAILED) editor.remove("error.${spec.modelId}")
        editor.apply()
        listeners.forEach { listener -> runCatching { listener(spec.modelId, status) } }
    }

    /** Only called from Dispatchers.IO: hashing a GGUF on the UI thread would jank it. */
    private fun sha256(file: File): String = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrDefault("")
}
