package com.oneorigin.originagent.models

import android.content.Context
import android.net.Uri
import com.oneorigin.originagent.util.JsonCodec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.UUID

/** A GGUF the user already had on the device, copied into Origin's private storage. */
data class ImportedModel(
    val id: String,
    val displayName: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
    val contextSize: Int,
    val addedAt: Long
)

/**
 * Pure, JVM-testable helpers for imported GGUFs. No Android `ContentResolver` and no native
 * runtime is touched here: an unknown file is never described as a model.
 */
object ImportedModels {

    const val MAGIC = "GGUF"
    const val CONTEXT_SIZE = 2048

    /** A header alone cannot run a model; anything smaller is rejected at import time. */
    const val MIN_SIZE_BYTES: Long = 1024L

    const val PREFS_NAME = "origin_imported_models"
    const val PREFS_KEY = "models"

    private val MAGIC_BYTES = MAGIC.toByteArray(Charsets.US_ASCII)

    fun hasGgufMagic(bytes: ByteArray): Boolean =
        bytes.size >= MAGIC_BYTES.size &&
            bytes[0] == MAGIC_BYTES[0] && bytes[1] == MAGIC_BYTES[1] &&
            bytes[2] == MAGIC_BYTES[2] && bytes[3] == MAGIC_BYTES[3]

    /** Reads only four bytes, so callers never load a multi-gigabyte GGUF into memory. */
    fun hasGgufMagic(file: File): Boolean = runCatching {
        RandomAccessFile(file, "r").use { input ->
            val header = ByteArray(MAGIC_BYTES.size)
            var filled = 0
            while (filled < header.size) {
                val read = input.read(header, filled, header.size - filled)
                if (read <= 0) break
                filled += read
            }
            filled == header.size && hasGgufMagic(header)
        }
    }.getOrDefault(false)

    fun isValidSha256(value: String): Boolean = value.length == 64 &&
        value.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

    /** Content-addressed id: the same GGUF always keeps the same identity. */
    fun idForHash(sha256: String): String = "local.imported.${sha256.take(12).lowercase()}"

    fun fileNameForHash(sha256: String): String = "imported-${sha256.take(16).lowercase()}.gguf"

    /** Flat metadata record; kept dependency-free so it is round-trip testable on the JVM. */
    fun toMap(model: ImportedModel): Map<String, String> = linkedMapOf(
        "id" to model.id,
        "name" to model.displayName,
        "file" to model.fileName,
        "size" to model.sizeBytes.toString(),
        "sha" to model.sha256,
        "ctx" to model.contextSize.toString(),
        "at" to model.addedAt.toString()
    )

    /** Rejects malformed or tampered metadata instead of presenting a phantom model. */
    fun fromMap(fields: Map<String, String>): ImportedModel? {
        val sha = fields["sha"]?.trim().orEmpty()
        val id = fields["id"]?.trim().orEmpty()
        val size = fields["size"]?.toLongOrNull() ?: return null
        val context = fields["ctx"]?.toIntOrNull() ?: return null
        val addedAt = fields["at"]?.toLongOrNull() ?: return null
        val name = fields["name"]?.trim().orEmpty()
        val fileName = fields["file"]?.trim().orEmpty()
        if (!isValidSha256(sha) || id != idForHash(sha)) return null
        if (fileName != fileNameForHash(sha)) return null
        if (size < MIN_SIZE_BYTES || context <= 0 || addedAt < 0L || name.isEmpty()) return null
        return ImportedModel(id, name, fileName, size, sha, context, addedAt)
    }

    /** One compact JSON object per line, so existing metadata files stay human-readable. */
    fun encodeAll(models: List<ImportedModel>): String = models.joinToString("\n") { model ->
        JsonCodec.encode(LinkedHashMap<String, Any?>(toMap(model)))
    }

    fun decodeAll(text: String): List<ImportedModel> = text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull(JsonCodec::decode)
        .mapNotNull(::fromMap)
        .toList()

    /** Synthesized spec for the real llama.cpp runtime — never a downloadable URL. */
    fun specFor(model: ImportedModel): DownloadableModelSpec = DownloadableModelSpec(
        modelId = model.id,
        label = model.displayName,
        family = "Imported GGUF",
        fileName = model.fileName,
        url = "",
        sha256 = model.sha256,
        sizeBytes = model.sizeBytes,
        format = "gguf/imported",
        license = "User-supplied",
        licenseUrl = "",
        attribution = "Imported by the user from device storage",
        contextSize = if (model.contextSize > 0) model.contextSize else CONTEXT_SIZE,
        summary = "GGUF imported from this device and stored privately in Origin."
    )

    fun descriptorFor(model: ImportedModel): ModelDescriptor = ModelDescriptor(
        id = model.id,
        displayName = model.displayName,
        modelName = "gguf/imported",
        provider = "Imported GGUF · on this device",
        providerId = "oneorigin.imported",
        kind = ModelKind.LOCAL_NATIVE,
        description = "A GGUF you imported, copied into Origin's private storage. " +
            "SHA-256 ${model.sha256.take(12)}…; stored offline on this device.",
        capabilities = setOf("chat", "offline", "local", "gguf", "imported"),
        limitations = "User-supplied weights: licence, quality and behaviour are the model " +
            "author's responsibility. Context ${if (model.contextSize > 0) model.contextSize else CONTEXT_SIZE} tokens.",
        docsUrl = null,
        downloadSpec = specFor(model),
        chatCapable = true
    )

    internal fun hex(bytes: ByteArray): String {
        val digits = "0123456789abcdef"
        return buildString(bytes.size * 2) {
            for (byte in bytes) {
                val value = byte.toInt() and 0xff
                append(digits[value shr 4])
                append(digits[value and 0x0f])
            }
        }
    }
}

/** A real local GGUF engine. It never imitates an answer if the runtime or weights are absent. */
class ImportedModelEngine(
    private val model: ImportedModel,
    private val file: File,
    private val runtimeAvailable: () -> Boolean = { LlamaRuntime.available() }
) : InferenceEngine {

    private val spec: DownloadableModelSpec = ImportedModels.specFor(model)

    override val id: String = spec.modelId
    override val displayName: String = spec.label
    override val description: String = "${spec.summary} ${spec.attribution}."
    override val capabilities: Set<String> =
        setOf("chat", "offline", "local", "gguf", "imported")

    override fun isAvailable(): Boolean =
        runtimeAvailable() && file.isFile && file.length() == spec.sizeBytes

    override suspend fun generate(request: GenerationRequest): GenerationResult {
        if (!runtimeAvailable()) {
            return GenerationResult.Unavailable(
                id, "This device cannot load the arm64 local GGUF runtime."
            )
        }
        if (!file.isFile || file.length() != spec.sizeBytes) {
            return GenerationResult.Unavailable(
                id, "The imported GGUF copy is missing or incomplete. Import it again."
            )
        }
        return runCatching {
            val answer = LlamaRuntime.generate(spec, file, request.prompt, request.maxTokens)
            GenerationResult.Success(answer, id)
        }.getOrElse { error ->
            GenerationResult.Failure(id, error.message ?: "Local inference failed")
        }
    }
}

/**
 * Copies a content-URI GGUF into app-private storage while hashing it. The file is promoted
 * only after the header and length check pass, and metadata is persisted as JSON Lines so it
 * survives restarts. Readiness is decided from metadata + exact file length only — never by
 * hashing a large file on the UI thread.
 */
class ImportedModelStore(context: Context) {

    private class FriendlyFailure(message: String) : IOException(message)

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(
        ImportedModels.PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val root = File(appContext.filesDir, "imported-models").apply { mkdirs() }

    private val writeMutex = Mutex()
    private val stateLock = Any()

    suspend fun import(uri: Uri, displayName: String? = null): Result<ImportedModel> =
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                runCatching { copyAndVerify(uri, displayName) }.fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        Result.failure(asFriendly(error))
                    }
                )
            }
        }

    fun models(): List<ImportedModel> = synchronized(stateLock) { readAll() }

    fun model(id: String): ImportedModel? = models().firstOrNull { it.id == id }

    /** Always derived from the digest, so tampered metadata cannot point outside our folder. */
    fun path(model: ImportedModel): File = File(root, ImportedModels.fileNameForHash(model.sha256))

    fun isReady(model: ImportedModel): Boolean {
        if (model(model.id) != model || model.id != ImportedModels.idForHash(model.sha256)) return false
        val file = path(model)
        return file.isFile && file.length() == model.sizeBytes
    }

    fun remove(id: String) {
        synchronized(stateLock) {
            val current = readAll()
            val removed = current.firstOrNull { it.id == id } ?: return
            writeAll(current.filterNot { it.id == id })
            runCatching { path(removed).delete() }
        }
    }

    private fun readAll(): List<ImportedModel> =
        ImportedModels.decodeAll(prefs.getString(ImportedModels.PREFS_KEY, "").orEmpty())

    private fun writeAll(models: List<ImportedModel>) {
        prefs.edit().putString(ImportedModels.PREFS_KEY, ImportedModels.encodeAll(models)).apply()
    }

    private fun asFriendly(error: Throwable): Throwable = when (error) {
        is FriendlyFailure -> error
        is FileNotFoundException ->
            FriendlyFailure("That file is no longer available. Choose the .gguf file again.")
        is SecurityException ->
            FriendlyFailure("Permission was denied reading that file. Choose a readable .gguf file.")
        else -> FriendlyFailure("Could not read that file. Choose a readable .gguf file.")
    }

    private fun copyAndVerify(uri: Uri, displayName: String?): ImportedModel {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw FriendlyFailure("Could not open that file. Choose a readable .gguf file.")
        val temp = File(root, "import-${UUID.randomUUID()}.part")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            val header = ByteArray(ImportedModels.MAGIC.length)
            var headerFilled = 0
            var total = 0L

            input.use { stream ->
                while (true) {
                    val read = stream.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                    total += read
                    if (headerFilled < header.size) {
                        val take = minOf(header.size - headerFilled, read)
                        System.arraycopy(buffer, 0, header, headerFilled, take)
                        headerFilled += take
                    }
                    if (headerFilled == header.size && !ImportedModels.hasGgufMagic(header)) {
                        throw FriendlyFailure("That file is not a GGUF model — its header does not start with GGUF.")
                    }
                }
            }

            if (total < ImportedModels.MIN_SIZE_BYTES) {
                throw FriendlyFailure("That file is too small to be a GGUF model (${total} bytes).")
            }

            val sha = ImportedModels.hex(digest.digest())
            val id = ImportedModels.idForHash(sha)
            val fileName = ImportedModels.fileNameForHash(sha)
            val existing = synchronized(stateLock) { readAll().firstOrNull { it.id == id } }
            if (existing != null && existing.sha256.equals(sha, ignoreCase = true)) {
                val existingFile = File(root, fileName)
                if (existingFile.isFile && existingFile.length() == existing.sizeBytes) {
                    return existing
                }
            }

            val target = File(root, fileName)
            if (target.exists() && !target.delete()) {
                throw FriendlyFailure("Could not replace an older copy of this model in app storage.")
            }
            if (!temp.renameTo(target)) {
                throw FriendlyFailure("Could not store the imported GGUF in app-private storage.")
            }

            val label = displayName?.trim()?.takeIf { it.isNotBlank() }
                ?: runCatching { uri.lastPathSegment?.substringAfterLast('/') }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: "Imported GGUF"
            val imported = existing?.copy(fileName = fileName, sizeBytes = total, sha256 = sha)
                ?: ImportedModel(
                    id = id,
                    displayName = label,
                    fileName = fileName,
                    sizeBytes = total,
                    sha256 = sha,
                    contextSize = ImportedModels.CONTEXT_SIZE,
                    addedAt = System.currentTimeMillis()
                )
            synchronized(stateLock) {
                writeAll(listOf(imported) + readAll().filterNot { it.id == id })
            }
            return imported
        } finally {
            runCatching { temp.delete() }
        }
    }
}
