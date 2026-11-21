package com.oneorigin.originagent.util

import android.content.Context
import com.oneorigin.originagent.models.DownloadableModelSpec
import com.oneorigin.originagent.models.LlamaRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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
 * never become active.
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

    private fun lock(id: String) = locks.getOrPut(id) { Mutex() }

    private fun target(spec: DownloadableModelSpec) = File(root, spec.fileName)
    private fun partial(spec: DownloadableModelSpec) = File(root, "${spec.fileName}.part")

    fun addListener(listener: (String, Status) -> Unit) {
        listeners.addIfAbsent(listener)
    }

    fun removeListener(listener: (String, Status) -> Unit) {
        listeners.remove(listener)
    }

    fun status(spec: DownloadableModelSpec): Status {
        val file = target(spec)
        if (file.exists() && file.length() == spec.sizeBytes) {
            val verified = prefs.getString("verified.${spec.modelId}", null) == spec.sha256
            if (verified || sha256(file).equals(spec.sha256, ignoreCase = true)) {
                if (!verified) {
                    prefs.edit().putString("verified.${spec.modelId}", spec.sha256).apply()
                }
                return Status(State.READY, 100, "Verified and ready offline")
            }
        }
        val job = jobs[spec.modelId]
        if (job?.isActive == true) {
            return Status(State.DOWNLOADING, progress(spec.modelId), "Downloading verified weights…")
        }
        val failure = prefs.getString("error.${spec.modelId}", null)
        return if (failure.isNullOrBlank()) {
            Status(State.NOT_DOWNLOADED, 0, "Not downloaded · ${formatSize(spec.sizeBytes)}")
        } else {
            Status(State.FAILED, 0, failure)
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
        lock(spec.modelId).withLock {
            val destination = target(spec)
            if (destination.exists() && destination.length() == spec.sizeBytes &&
                sha256(destination).equals(spec.sha256, ignoreCase = true)
            ) {
                publish(spec, Status(State.READY, 100, "Verified and ready offline"))
                return@withLock Result.success(destination)
            }

            val temp = partial(spec)
            runCatching {
                publish(spec, Status(State.DOWNLOADING, 0, "Connecting…"))
                downloadFromUrl(spec, temp) { percent ->
                    publish(spec, Status(State.DOWNLOADING, percent, "Downloading verified weights…"))
                }
                verifyAndPromote(temp, destination, spec)
                publish(spec, Status(State.READY, 100, "Verified and ready offline"))
                destination
            }.onFailure { throwable ->
                temp.delete()
                publish(spec, Status(State.FAILED, 0, throwable.message ?: "Download failed"))
            }
        }
    }

    private suspend fun downloadFromUrl(
        spec: DownloadableModelSpec,
        temp: File,
        onProgress: (Int) -> Unit
    ) {
        val connection = URL(spec.url).openConnection() as HttpURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        connection.requestMethod = "GET"
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("Download failed (HTTP $code)")
            val expected = connection.contentLength
            var total = 0L
            var lastPercent = -1
            connection.inputStream.use { input ->
                FileOutputStream(temp).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        total += read
                        if (expected > 0) {
                            val percent = (total * 100 / expected).toInt().coerceIn(0, 99)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun verifyAndPromote(temp: File, destination: File, spec: DownloadableModelSpec) {
        if (temp.length() != spec.sizeBytes) {
            error("Unexpected size: ${temp.length()} bytes (expected ${spec.sizeBytes})")
        }
        val digest = MessageDigest.getInstance("SHA-256")
        temp.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actual.equals(spec.sha256, ignoreCase = true)) {
            error("Checksum mismatch — weights discarded")
        }
        if (!temp.renameTo(destination)) {
            temp.copyTo(destination, overwrite = true)
            temp.delete()
        }
    }

    fun cancel(spec: DownloadableModelSpec) {
        jobs[spec.modelId]?.cancel()
        partial(spec).delete()
        LlamaRuntime.release(spec.modelId)
        publish(spec, Status(State.NOT_DOWNLOADED, 0, "Download cancelled"))
    }

    fun delete(spec: DownloadableModelSpec) {
        jobs[spec.modelId]?.cancel()
        partial(spec).delete()
        target(spec).delete()
        prefs.edit().remove("verified.${spec.modelId}").remove("error.${spec.modelId}").apply()
        LlamaRuntime.release(spec.modelId)
        publish(spec, Status(State.NOT_DOWNLOADED, 0, "Weights removed from this device"))
    }

    private fun publish(spec: DownloadableModelSpec, status: Status) {
        prefs.edit()
            .putInt("progress.${spec.modelId}", status.progress)
            .putString(
                "verified.${spec.modelId}",
                if (status.state == State.READY) spec.sha256 else null
            )
            .putString("error.${spec.modelId}", if (status.state == State.FAILED) status.message else null)
            .apply()
        listeners.forEach { it(spec.modelId, status) }
    }

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
