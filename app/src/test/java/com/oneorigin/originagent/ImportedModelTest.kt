package com.oneorigin.originagent

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.oneorigin.originagent.models.GenerationRequest
import com.oneorigin.originagent.models.GenerationResult
import com.oneorigin.originagent.models.ImportedModel
import com.oneorigin.originagent.models.ImportedModelEngine
import com.oneorigin.originagent.models.ImportedModelStore
import com.oneorigin.originagent.models.ImportedModels
import com.oneorigin.originagent.models.ModelRegistry
import com.oneorigin.originagent.util.JsonCodec
import com.oneorigin.originagent.util.ModelDownloadManager
import com.oneorigin.originagent.util.RemoteModelConfigStore
import com.oneorigin.originagent.util.SettingsStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** JVM-only coverage: no ContentResolver, network or native runtime is needed. */
class ImportedModelTest {

    private class FakePrefs : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = HashMap(values)
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (values[key] as? Set<*>)?.toMutableSet() as? MutableSet<String> ?: defValues
        override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = Editor(values)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = Unit

        private class Editor(private val values: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private fun put(key: String?, value: Any?): SharedPreferences.Editor {
                if (key != null) {
                    if (value == null) values.remove(key) else values[key] = value
                }
                return this
            }
            override fun putString(key: String?, value: String?): SharedPreferences.Editor = put(key, value)
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = put(key, values)
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = put(key, value)
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = put(key, value)
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = put(key, value)
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = put(key, value)
            override fun remove(key: String?): SharedPreferences.Editor {
                key?.let(values::remove)
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                values.clear()
                return this
            }
            override fun commit(): Boolean = true
            override fun apply() = Unit
        }
    }

    private class FakeContext(
        private val prefs: SharedPreferences,
        private val filesDir: File
    ) : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
        override fun getFilesDir(): File = filesDir
    }

    private class Fixture {
        val dir: File = Files.createTempDirectory("origin-imported-model").toFile()
        val prefs = FakePrefs()
        val context = FakeContext(prefs, dir)
        val store = ImportedModelStore(context)
        val settings = SettingsStore(context)
        val remote = RemoteModelConfigStore(context)
        val downloads = ModelDownloadManager(context)
        val registry = ModelRegistry(settings, remote, downloads, store)

        fun cleanup() {
            dir.deleteRecursively()
        }
    }

    private fun model(
        sha: String = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    ) = ImportedModel(
        id = ImportedModels.idForHash(sha),
        displayName = "My imported GGUF",
        fileName = ImportedModels.fileNameForHash(sha),
        sizeBytes = 4096L,
        sha256 = sha,
        contextSize = ImportedModels.CONTEXT_SIZE,
        addedAt = 1_700_000_000_000L
    )

    @Test
    fun ggufMagicIsCheckedByteForByte() {
        assertTrue(ImportedModels.hasGgufMagic("GGUF model bytes".toByteArray(Charsets.US_ASCII)))
        assertFalse(ImportedModels.hasGgufMagic("GGML model bytes".toByteArray(Charsets.US_ASCII)))
        assertFalse(ImportedModels.hasGgufMagic("gguf lower case".toByteArray(Charsets.US_ASCII)))
        assertFalse(ImportedModels.hasGgufMagic("GG".toByteArray(Charsets.US_ASCII)))

        val dir = Files.createTempDirectory("origin-gguf-magic").toFile()
        try {
            val good = File(dir, "good.gguf")
            good.writeBytes("GGUF".toByteArray(Charsets.US_ASCII) + ByteArray(64))
            assertTrue(ImportedModels.hasGgufMagic(good))
            val bad = File(dir, "bad.gguf")
            bad.writeBytes("NOPE".toByteArray(Charsets.US_ASCII) + ByteArray(64))
            assertFalse(ImportedModels.hasGgufMagic(bad))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun idsAndFileNamesAreStableAndContentAddressed() {
        val sha = "ABCDEF0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
        assertEquals("local.imported.abcdef012345", ImportedModels.idForHash(sha))
        assertEquals("local.imported.abcdef012345", ImportedModels.idForHash(sha.lowercase()))
        assertEquals("imported-abcdef0123456789.gguf", ImportedModels.fileNameForHash(sha))
        assertFalse(ImportedModels.idForHash(sha) == ImportedModels.idForHash("1" + sha.drop(1)))
        assertTrue(ImportedModels.isValidSha256(sha))
        assertFalse(ImportedModels.isValidSha256("not-a-hash"))
    }

    @Test
    fun metadataRoundTripsThroughJsonLinesAndRejectsCorruption() {
        val original = model()
        val map = JsonCodec.decode(JsonCodec.encode(LinkedHashMap<String, Any?>(ImportedModels.toMap(original))))
        assertEquals(original, ImportedModels.fromMap(map))

        val encoded = ImportedModels.encodeAll(listOf(original, model(sha = "f".repeat(64))))
        assertEquals(2, encoded.lines().size)
        assertEquals(2, ImportedModels.decodeAll(encoded).size)
        assertEquals(original, ImportedModels.decodeAll(encoded).first())

        val corrupted = JsonCodec.encode(LinkedHashMap<String, Any?>(ImportedModels.toMap(original).plus("sha" to "broken")))
        assertFalse(ImportedModels.decodeAll(corrupted).isNotEmpty())
        assertTrue(ImportedModels.decodeAll("not json\n$encoded").size == 2)
    }

    @Test
    fun registryExposesImportedModelsWithoutNetworkOrNativeLoad() {
        val fixture = Fixture()
        try {
            val imported = model()
            val weights = File(fixture.dir, "imported-models/${imported.fileName}")
            weights.parentFile?.mkdirs()
            weights.writeBytes(ByteArray(imported.sizeBytes.toInt()))

            fixture.prefs.edit()
                .putString(ImportedModels.PREFS_KEY, ImportedModels.encodeAll(listOf(imported)))
                .apply()

            assertEquals(listOf(imported), fixture.store.models())
            assertEquals(imported, fixture.store.model(imported.id))
            assertTrue(fixture.store.isReady(imported))
            assertEquals(weights.absolutePath, fixture.store.path(imported).absolutePath)

            assertTrue(fixture.registry.importedModels.contains(imported))
            assertTrue(fixture.registry.isImported(imported.id))
            assertTrue(fixture.registry.isDownloaded(imported.id))

            val descriptor = fixture.registry.descriptor(imported.id)
            assertNotNull(descriptor)
            assertEquals("gguf/imported", descriptor!!.downloadSpec?.format)
            assertEquals("", descriptor.downloadSpec?.url)
            assertEquals(imported.sha256, descriptor.downloadSpec?.sha256)
            assertEquals(imported.sizeBytes, descriptor.downloadSpec?.sizeBytes)
            assertEquals(ImportedModels.CONTEXT_SIZE, descriptor.downloadSpec?.contextSize)
            assertTrue(descriptor.capabilities.contains("imported"))
            assertTrue(fixture.registry.descriptors.contains(descriptor))

            assertTrue(fixture.registry.engines.any { it is ImportedModelEngine && it.id == imported.id })
            assertTrue(fixture.registry.chatEngines.any { it.id == imported.id })
            assertNotNull(fixture.registry.byId(imported.id))

            // Readiness trusts metadata + exact length only.
            weights.writeBytes(ByteArray(imported.sizeBytes.toInt() - 1))
            assertFalse(fixture.store.isReady(imported))

            fixture.store.remove(imported.id)
            assertTrue(fixture.store.models().isEmpty())
            assertFalse(weights.exists())
            assertFalse(fixture.registry.isImported(imported.id))
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun missingRuntimeReturnsTypedUnavailableInsteadOfAFakeAnswer() = runBlocking {
        val imported = model()
        val engine = ImportedModelEngine(imported, File("/nonexistent/${imported.fileName}")) { false }
        assertFalse(engine.isAvailable())
        val result = engine.generate(GenerationRequest("hello"))
        assertTrue(result is GenerationResult.Unavailable)
        assertEquals(imported.id, (result as GenerationResult.Unavailable).engineId)
    }
}
