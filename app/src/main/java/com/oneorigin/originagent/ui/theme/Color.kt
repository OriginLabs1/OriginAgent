package com.oneorigin.originagent.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Aurora design tokens.
 *
 * The signature accents stay constant even when Material You dynamic color is enabled,
 * so Origin keeps a recognizable identity on every device while still honoring the
 * user's wallpaper-derived scheme.
 */
object Aurora {
    val Violet = Color(0xFF9B7BFF)
    val Indigo = Color(0xFF5B6CFF)
    val Cyan = Color(0xFF49D6E8)
    val Mint = Color(0xFF5EEAD4)
    val Amber = Color(0xFFFFC978)
    val Rose = Color(0xFFFF8FA3)

    val Night = Color(0xFF070A12)
    val Day = Color(0xFFF6F8FF)

    /** Brand gradient used for primary actions, marks and active navigation. */
    val Brand = listOf(Violet, Indigo, Cyan, Mint)

    /** A calm two-stop gradient for user messages and highlighted cards. */
    val Conversation = listOf(Violet, Indigo)
}

/** Aurora Night — the default dark canvas. */
val OriginDarkColors = darkColorScheme(
    primary = Aurora.Violet,
    onPrimary = Color(0xFF0B0716),
    primaryContainer = Color(0xFF2A2050),
    onPrimaryContainer = Color(0xFFEFEAFF),
    secondary = Aurora.Mint,
    onSecondary = Color(0xFF04231E),
    secondaryContainer = Color(0xFF123B36),
    onSecondaryContainer = Color(0xFFC9FFF3),
    tertiary = Aurora.Cyan,
    onTertiary = Color(0xFF04222B),
    tertiaryContainer = Color(0xFF113A45),
    onTertiaryContainer = Color(0xFFCDF7FF),
    error = Aurora.Rose,
    onError = Color(0xFF2A0710),
    errorContainer = Color(0xFF48202B),
    onErrorContainer = Color(0xFFFFD9E1),
    background = Aurora.Night,
    onBackground = Color(0xFFF2F5FF),
    surface = Color(0xFF101728),
    onSurface = Color(0xFFEDF1FF),
    surfaceVariant = Color(0xFF17213A),
    onSurfaceVariant = Color(0xFFADB8D6),
    surfaceContainerLow = Color(0xFF0D1322),
    surfaceContainer = Color(0xFF121B30),
    surfaceContainerHigh = Color(0xFF18233C),
    outline = Color(0xFF46557A),
    outlineVariant = Color(0xFF2C3854)
)

/** Aurora Day — a light canvas with the same accents and hierarchy. */
val OriginLightColors = lightColorScheme(
    primary = Color(0xFF5B3FD6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE7E0FF),
    onPrimaryContainer = Color(0xFF1C1054),
    secondary = Color(0xFF0B8578),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC6F5EB),
    onSecondaryContainer = Color(0xFF003A33),
    tertiary = Color(0xFF0C6E9E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD4F1FF),
    onTertiaryContainer = Color(0xFF00243A),
    error = Color(0xFFB3264A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFD8E2),
    onErrorContainer = Color(0xFF41001C),
    background = Aurora.Day,
    onBackground = Color(0xFF141A2E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141A2E),
    surfaceVariant = Color(0xFFEDF0FA),
    onSurfaceVariant = Color(0xFF525C78),
    surfaceContainerLow = Color(0xFFF8F9FF),
    surfaceContainer = Color(0xFFF1F4FC),
    surfaceContainerHigh = Color(0xFFE8EDF8),
    outline = Color(0xFF7B86A3),
    outlineVariant = Color(0xFFD6DCEC)
)
