package com.oneorigin.originagent

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.oneorigin.originagent.models.ImportedModelStore
import com.oneorigin.originagent.models.ModelCatalog
import com.oneorigin.originagent.models.ModelRegistry
import com.oneorigin.originagent.models.OpenAiCompatibleEngine
import com.oneorigin.originagent.util.ModelDownloadManager
import com.oneorigin.originagent.util.RemoteModelConfigStore
import com.oneorigin.originagent.util.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Web adapters must be genuinely opt-in. These tests drive the real stores and registry with
 * an in-memory SharedPreferences implementation, so they never touch the network.
 */
class ModelGatingTest {

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
        val dir: File = Files.createTempDirectory("origin-model-gating").toFile()
        val prefs = FakePrefs()
        val context = FakeContext(prefs, dir)
        val settings = SettingsStore(context)
        val remote = RemoteModelConfigStore(context)
        val downloads = ModelDownloadManager(context)
        val imported = ImportedModelStore(context)
        val registry = ModelRegistry(settings, remote, downloads, imported)

        fun cleanup() {
            dir.deleteRecursively()
        }
    }

    @Test
    fun descriptorTemplatesAreNotExplicitConfiguration() {
        val fixture = Fixture()
        try {
            assertTrue(ModelCatalog.REMOTE_MODELS.any { it.defaultEndpoint.orEmpty().startsWith("https://") })
            ModelCatalog.REMOTE_MODELS.forEach { descriptor ->
                assertFalse("${descriptor.id} must stay opt-in", fixture.remote.isExplicitlyConfigured(descriptor.id))
                val engine = OpenAiCompatibleEngine(descriptor, fixture.remote) { true }
                assertFalse("${descriptor.id} must not be available on launch", engine.isAvailable())
            }
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun savingSettingsExplicitlyEnablesABuiltInAdapter() {
        val fixture = Fixture()
        try {
            val descriptor = ModelCatalog.OPENAI
            fixture.remote.save(descriptor.id, "https://example.invalid/v1", "gpt-4o-mini", "test-key")
            assertTrue(fixture.remote.isExplicitlyConfigured(descriptor.id))
            assertTrue(OpenAiCompatibleEngine(descriptor, fixture.remote) { true }.isAvailable())
            assertFalse(OpenAiCompatibleEngine(descriptor, fixture.remote) { false }.isAvailable())

            fixture.remote.clear(descriptor.id)
            assertFalse(fixture.remote.isExplicitlyConfigured(descriptor.id))
            assertFalse(OpenAiCompatibleEngine(descriptor, fixture.remote) { true }.isAvailable())
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun addedAdaptersAreExplicitAndGetStableUniqueIds() {
        val fixture = Fixture()
        try {
            val first = fixture.remote.addAdapter("Local", "http://127.0.0.1:1234/v1", "model-a", "")
            val second = fixture.remote.addAdapter("Local", "http://127.0.0.1:1234/v1", "model-b", "")
            assertNotEquals(first.id, second.id)
            assertTrue(first.id.matches(Regex("adapter\\.[0-9a-fA-F-]{36}")))
            assertTrue(fixture.remote.isExplicitlyConfigured(first.id))
            assertTrue(fixture.remote.isExplicitlyConfigured(second.id))
            assertEquals(2, fixture.remote.adapters().size)
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun registryNeverUsesARemoteAdapterAsSilentFallback() {
        val fixture = Fixture()
        try {
            assertNull(fixture.registry.selectedChat())
            assertFalse(fixture.registry.hasChatModel())
            assertFalse(fixture.registry.select("remote.openai"))

            fixture.registry.saveRemoteConfig(
                "remote.openai",
                "https://example.invalid/v1",
                "gpt-4o-mini",
                "test-key"
            )
            assertTrue(fixture.registry.select("remote.openai"))
            assertEquals("remote.openai", fixture.registry.selectedChat()?.id)
            assertTrue(fixture.registry.hasChatModel())

            // Once the explicit choice is gone, no remote template may answer by itself.
            fixture.remote.clear("remote.openai")
            fixture.settings.preferredEngineId = ""
            assertNull(fixture.registry.selectedChat())
            assertFalse(fixture.registry.hasChatModel())
        } finally {
            fixture.cleanup()
        }
    }

    @Test
    fun chatEngineListStillExposesAllRealEnginesButNotTheToolEngine() {
        val fixture = Fixture()
        try {
            val ids = fixture.registry.chatEngines.map { it.id }
            assertTrue(ids.containsAll(ModelCatalog.LOCAL_MODELS.map { it.id }))
            assertTrue(ids.containsAll(ModelCatalog.REMOTE_MODELS.map { it.id }))
            assertFalse(ids.contains(ModelCatalog.LOCAL.id))
        } finally {
            fixture.cleanup()
        }
    }
}
