package com.oneorigin.originagent.util

/**
 * Safe facade over the optional C/C++ harness.
 *
 * The app is fully functional when the native library is absent: every call has a
 * Kotlin fallback. This is intentional — no platform or implementation is mandatory.
 */
object NativeBridge {

    private val loaded: Boolean = runCatching { System.loadLibrary("origin_harness") }.isSuccess

    private val engineAvailability = HashMap<String, Boolean>()

    fun isAvailable(): Boolean = loaded

    fun version(): String = if (loaded) runCatching { nativeVersion() }.getOrDefault("cpp-fallback") else "kotlin-fallback"

    fun normalize(input: String): String {
        val trimmed = input.trim()
        return if (loaded) runCatching { nativeNormalize(trimmed) }.getOrDefault(trimmed.lowercase()) else trimmed.lowercase()
    }

    /** Additional offline check layered on top of the authoritative Kotlin verification. */
    fun structuralChainCheck(serializedLines: List<String>): Boolean {
        if (!loaded) return true
        return runCatching { nativeVerifyChain(serializedLines.joinToString("\n")) }.getOrDefault(true) == 1
    }

    /**
     * Engine libraries are never assumed present. Generation is only reported as
     * supported when a real native bridge exists — absent engines are unavailable.
     */
    fun isEngineSupported(libraryName: String): Boolean = synchronized(engineAvailability) {
        engineAvailability.getOrPut(libraryName) {
            loaded && runCatching { nativeEngineSupported(libraryName) }.getOrDefault(0) == 1
        }
    }

    private external fun nativeVersion(): String
    private external fun nativeNormalize(input: String): String
    private external fun nativeVerifyChain(chain: String): Int
    private external fun nativeEngineSupported(libraryName: String): Int
}
