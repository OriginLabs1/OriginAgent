package com.oneorigin.originagent.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * v0.0.4 — the "Satin" identity.
 *
 * OpenDuet's restraint, in native Material You clothing: neutral paper and graphite surfaces,
 * one violet accent used only for attention, and four status colours used only for status.
 * The website keeps its Aurora identity; the phone app deliberately looks and feels different.
 */
object Duet {
    val AccentLight = Color(0xFF6430AB)
    val AccentDark = Color(0xFFB49AE8)
    val AccentSoftLight = Color(0xFFF0EBFA)
    val AccentSoftDark = Color(0xFF2A2140)

    val InkLight = Color(0xFF18181B)
    val Ink2Light = Color(0xFF55555E)
    val Ink3Light = Color(0xFF6B6B75)
    val InkDark = Color(0xFFF4F4F5)
    val Ink2Dark = Color(0xFFA1A1AA)
    val Ink3Dark = Color(0xFF8E8E98)

    val CanvasLight = Color(0xFFF6F6F8)
    val CanvasDark = Color(0xFF111113)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceDark = Color(0xFF1C1C1F)
    val BubbleLight = Color(0xFFECECF0)
    val BubbleDark = Color(0xFF26262B)
    val TileLight = Color(0xFFF1F1F4)
    val TileDark = Color(0xFF26262B)

    val DoneLight = Color(0xFF147A3A)
    val DoneDark = Color(0xFF4ADE80)
    val NeedsLight = Color(0xFFA34A06)
    val NeedsDark = Color(0xFFFBBF24)
    val WorkingLight = Color(0xFF1D4ED8)
    val WorkingDark = Color(0xFF7AA2FF)
    val ProblemLight = Color(0xFFB91C1C)
    val ProblemDark = Color(0xFFF87171)

    val DoneSoftLight = Color(0xFFE6F4EA)
    val DoneSoftDark = Color(0xFF14301F)
    val NeedsSoftLight = Color(0xFFFDF3E1)
    val NeedsSoftDark = Color(0xFF3A2A0C)
    val WorkingSoftLight = Color(0xFFEAF0FD)
    val WorkingSoftDark = Color(0xFF18233D)
    val ProblemSoftLight = Color(0xFFFBEAAA)
    val ProblemSoftDark = Color(0xFF3A1717)

    /** Legacy token names remain so older Origin screens keep compiling during the transition. */
    val Violet = AccentDark
    val Indigo = AccentLight
    val Cyan = WorkingDark
    val Mint = DoneDark
    val Amber = NeedsDark
    val Rose = ProblemDark
    val Night = CanvasDark
    val Day = CanvasLight
    val Brand = listOf(AccentDark, WorkingDark, DoneDark)
    val Conversation = listOf(AccentLight, AccentDark)
}

/** Compatibility alias used by existing Origin components and contrast tests. */
object Aurora {
    val Violet get() = Duet.Violet
    val Indigo get() = Duet.Indigo
    val Cyan get() = Duet.Cyan
    val Mint get() = Duet.Mint
    val Amber get() = Duet.Amber
    val Rose get() = Duet.Rose
    val Night get() = Duet.Night
    val Day get() = Duet.Day
    val Brand get() = Duet.Brand
    val Conversation get() = Duet.Conversation
}

/** Satin Night — graphite, quiet and readable in low light. */
val OriginDarkColors = darkColorScheme(
    primary = Duet.AccentDark,
    onPrimary = Color(0xFF1A1033),
    primaryContainer = Duet.AccentSoftDark,
    onPrimaryContainer = Color(0xFFEFEAFF),
    secondary = Duet.DoneDark,
    onSecondary = Color(0xFF0B2314),
    secondaryContainer = Duet.DoneSoftDark,
    onSecondaryContainer = Color(0xFFC6F5D5),
    tertiary = Duet.WorkingDark,
    onTertiary = Color(0xFF0B1833),
    tertiaryContainer = Duet.WorkingSoftDark,
    onTertiaryContainer = Color(0xFFDCE8FF),
    error = Duet.ProblemDark,
    onError = Color(0xFF2A0B0B),
    errorContainer = Duet.ProblemSoftDark,
    onErrorContainer = Color(0xFFFFD9D9),
    background = Duet.CanvasDark,
    onBackground = Duet.InkDark,
    surface = Duet.SurfaceDark,
    onSurface = Duet.InkDark,
    surfaceVariant = Duet.BubbleDark,
    onSurfaceVariant = Duet.Ink2Dark,
    surfaceContainerLow = Duet.CanvasDark,
    surfaceContainer = Duet.SurfaceDark,
    surfaceContainerHigh = Duet.TileDark,
    outline = Color(0xFF404047),
    outlineVariant = Color(0xFF2C2C31)
)

/** Satin Day — warm-neutral paper with the same single accent. */
val OriginLightColors = lightColorScheme(
    primary = Duet.AccentLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Duet.AccentSoftLight,
    onPrimaryContainer = Color(0xFF251044),
    secondary = Duet.Ink2Light,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Duet.TileLight,
    onSecondaryContainer = Duet.InkLight,
    tertiary = Duet.WorkingLight,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Duet.WorkingSoftLight,
    onTertiaryContainer = Color(0xFF0B1E4A),
    error = Duet.ProblemLight,
    onError = Color(0xFFFFFFFF),
    errorContainer = Duet.ProblemSoftLight,
    onErrorContainer = Color(0xFF3A0B0B),
    background = Duet.CanvasLight,
    onBackground = Duet.InkLight,
    surface = Duet.SurfaceLight,
    onSurface = Duet.InkLight,
    surfaceVariant = Duet.TileLight,
    onSurfaceVariant = Duet.Ink2Light,
    surfaceContainerLow = Duet.CanvasLight,
    surfaceContainer = Duet.SurfaceLight,
    surfaceContainerHigh = Duet.TileLight,
    outline = Color(0xFF8A8A93),
    outlineVariant = Color(0xFFD9D9DF)
)
