package com.oneorigin.originagent.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import com.oneorigin.originagent.ui.ChatMessage
import com.oneorigin.originagent.ui.theme.Aurora

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OriginTopBar(title: String, subtitle: String, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

/** Backwards-compatible alias used by secondary screens. */
@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    AuroraPill(text = text, color = color, modifier = modifier)
}

/** A conversational turn: gradient for the user, glass with provenance for Origin. */
@Composable
fun MessageBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val user = message.isUser
    val bubbleShape = RoundedCornerShape(
        topStart = 22.dp,
        topEnd = 22.dp,
        bottomStart = if (user) 22.dp else 6.dp,
        bottomEnd = if (user) 6.dp else 22.dp
    )

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
        horizontalArrangement = if (user) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!user) {
            AuroraMark(size = 28.dp, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.size(10.dp))
        }

        Box(
            modifier = if (user) {
                Modifier.background(Brush.linearGradient(Aurora.Conversation), bubbleShape)
            } else {
                Modifier
            }
        ) {
            Surface(
                shape = bubbleShape,
                color = if (user) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                contentColor = if (user) Color.White else MaterialTheme.colorScheme.onSurface,
                border = if (user) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp
            ) {
                Column(Modifier.padding(horizontal = 17.dp, vertical = 14.dp)) {
                    if (!user) {
                        Text(
                            "ORIGIN · ${message.engineLabel.ifBlank { "Local" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Aurora.Mint
                        )
                        Spacer(Modifier.size(8.dp))
                    }
                    Text(
                        message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (user) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    if (!user && (message.status.isNotBlank() || !message.verified)) {
                        Spacer(Modifier.size(10.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )
                        Spacer(Modifier.size(7.dp))
                        Text(
                            if (message.verified) message.status else "Attempted — not verified · ${message.status}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (message.verified) MaterialTheme.colorScheme.onSurfaceVariant else Aurora.Rose
                        )
                    }
                }
            }
        }
    }
}

/** Suggestion chips become tactile starter cards in the Aurora UI. */
@Composable
fun SuggestionRow(items: List<String>, onClick: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                modifier = Modifier.clickable { onClick(item) }
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Aurora.Violet,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(item, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
