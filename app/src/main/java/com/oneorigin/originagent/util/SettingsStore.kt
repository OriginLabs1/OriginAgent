package com.oneorigin.originagent.util

import android.content.Context
import androidx.core.content.edit

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Minimal local preferences. Included in user exports. */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("origin_settings", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = runCatching { ThemeMode.valueOf(prefs.getString("theme", "SYSTEM") ?: "SYSTEM") }
            .getOrDefault(ThemeMode.SYSTEM)
        set(value) = prefs.edit { putString("theme", value.name) }

    var dynamicColor: Boolean
        get() = prefs.getBoolean("dynamic_color", true)
        set(value) = prefs.edit { putBoolean("dynamic_color", value) }

    var reducedMotion: Boolean
        get() = prefs.getBoolean("reduced_motion", false)
        set(value) = prefs.edit { putBoolean("reduced_motion", value) }

    var autoApproveLowRisk: Boolean
        get() = prefs.getBoolean("auto_approve_low", true)
        set(value) = prefs.edit { putBoolean("auto_approve_low", value) }

    var preferredEngineId: String
        get() = prefs.getString("engine", "") ?: ""
        set(value) = prefs.edit { putString("engine", value) }

    /** Web access is possible and enabled by default; the user can switch it off at any time. */
    var webAccessEnabled: Boolean
        get() = prefs.getBoolean("web_access", true)
        set(value) = prefs.edit { putBoolean("web_access", value) }

    fun asMap(): Map<String, Any?> = mapOf(
        "type" to "preferences",
        "theme" to themeMode.name,
        "dynamicColor" to dynamicColor,
        "reducedMotion" to reducedMotion,
        "autoApproveLowRisk" to autoApproveLowRisk,
        "preferredEngineId" to preferredEngineId,
        "webAccessEnabled" to webAccessEnabled
    )
}
