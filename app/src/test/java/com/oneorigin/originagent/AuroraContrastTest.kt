package com.oneorigin.originagent

import androidx.compose.ui.graphics.Color
import com.oneorigin.originagent.ui.theme.Aurora
import com.oneorigin.originagent.ui.theme.OriginDarkColors
import com.oneorigin.originagent.ui.theme.OriginLightColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Aurora redesign promises accessible contrast in both palettes. These checks are
 * intentionally pure JVM computations, so they run in the normal unit-test task.
 */
class AuroraContrastTest {

    @Test
    fun darkCanvasMeetsAaContrast() {
        assertTrue(
            "onBackground/background contrast too low",
            contrast(OriginDarkColors.onBackground, OriginDarkColors.background) >= 4.5
        )
        assertTrue(
            "onSurface/surface contrast too low",
            contrast(OriginDarkColors.onSurface, OriginDarkColors.surface) >= 4.5
        )
    }

    @Test
    fun lightCanvasMeetsAaContrast() {
        assertTrue(
            "onBackground/background contrast too low",
            contrast(OriginLightColors.onBackground, OriginLightColors.background) >= 4.5
        )
        assertTrue(
            "onSurface/surface contrast too low",
            contrast(OriginLightColors.onSurface, OriginLightColors.surface) >= 4.5
        )
    }

    @Test
    fun fallbackSchemeKeepsBrandIdentity() {
        assertEquals(Aurora.Violet, OriginDarkColors.primary)
        assertEquals(Aurora.Mint, OriginDarkColors.secondary)
        assertTrue(OriginLightColors.primary != OriginLightColors.background)
    }

    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val v = value.toDouble()
            return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val l1 = luminance(a)
        val l2 = luminance(b)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }
}
