package com.oneorigin.originagent.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oneorigin.originagent.ui.theme.Aurora
import com.oneorigin.originagent.ui.theme.LocalAuroraMotion

/**
 * Ambient Aurora field: three soft light wells over the app canvas.
 * It is decorative, cheap to draw and completely disabled by Reduce motion.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val motion = LocalAuroraMotion.current
    val infinite = rememberInfiniteTransition(label = "aurora-field")
    val drift by infinite.animateFloat(
        initialValue = -0.04f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(tween(9000), RepeatMode.Reverse),
        label = "aurora-drift"
    )
    val shift = if (motion) drift else 0f
    val canvas = MaterialTheme.colorScheme.background

    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(canvas)
                val well = size.minDimension * 0.9f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Aurora.Violet.copy(alpha = 0.26f), Color.Transparent),
                        radius = well
                    ),
                    radius = well,
                    center = Offset(size.width * (0.12f + shift), -size.height * 0.08f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Aurora.Mint.copy(alpha = 0.18f), Color.Transparent),
                        radius = well
                    ),
                    radius = well,
                    center = Offset(size.width * (0.96f - shift), size.height * 0.16f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Aurora.Cyan.copy(alpha = 0.14f), Color.Transparent),
                        radius = well
                    ),
                    radius = well,
                    center = Offset(size.width * 0.42f, size.height * (1.06f + shift))
                )
            }
    )
}

/** Standard screen wrapper: Aurora canvas behind scrollable content. */
@Composable
fun AuroraScreen(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier.fillMaxSize()) {
        AuroraBackground()
        Column(Modifier.fillMaxSize(), content = content)
    }
}

/** The Origin mark, drawn from primitives so the app ships no extra assets. */
@Composable
fun AuroraMark(size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(Aurora.Brand))
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(size * 0.27f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .size(size * 0.18f)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Aurora.Mint, Aurora.Cyan)))
        )
    }
}

/** Translucent card with a hairline edge — the workhorse of the Aurora UI. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = MaterialTheme.colorScheme.surface.copy(alpha = if (emphasized) 0.88f else 0.68f)
    val edge = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(24.dp),
        color = glass,
        border = BorderStroke(1.dp, edge),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

/** Small status capsule with a luminous dot. */
@Composable
fun AuroraPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    dot: Boolean = true
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.13f),
        contentColor = color,
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f))
    ) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (dot) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(color))
            }
            Text(text, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The genuinely live model chip — it only ever names the serving engine. */
@Composable
fun ModelChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, Aurora.Violet.copy(alpha = 0.45f))
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                Modifier.size(22.dp).clip(CircleShape).background(Brush.linearGradient(Aurora.Brand)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("change", style = MaterialTheme.typography.labelSmall, color = Aurora.Violet)
        }
    }
}

/** Gradient action used for primary navigation and send actions. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val alpha = if (enabled) 1f else 0.4f
    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        contentColor = Color.White
    ) {
        Box(
            Modifier
                .background(Brush.linearGradient(Aurora.Conversation.copy(alpha = alpha)), RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Section label used across secondary screens. */
@Composable
fun SectionHeading(kicker: String, title: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(kicker.uppercase(), style = MaterialTheme.typography.labelSmall, color = Aurora.Mint)
        Spacer(Modifier.size(6.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}

private fun List<Color>.copy(alpha: Float): List<Color> = map { it.copy(alpha = alpha) }

/** A quiet divider with a bright leading segment. */
@Composable
fun AuroraRule(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)))
}

@Composable
fun AuroraSpacer(width: Dp = 0.dp, height: Dp = 0.dp) {
    if (width.value != 0f) Spacer(Modifier.width(width)) else Spacer(Modifier.size(height))
}

@Composable
fun AuroraBreathingMark(reducedMotion: Boolean, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "aurora-mark")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "mark-scale"
    )
    AuroraMark(
        size = 54.dp,
        modifier = modifier.graphicsLayer(
            scaleX = if (reducedMotion) 1f else scale,
            scaleY = if (reducedMotion) 1f else scale
        )
    )
}
