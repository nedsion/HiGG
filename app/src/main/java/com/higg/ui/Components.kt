package com.higg.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val CardShape = RoundedCornerShape(20.dp)
private val TileShape = RoundedCornerShape(12.dp)

/** Quả cầu micro: sáng màu và có vòng sóng nhẹ khi đang nghe. */
@Composable
fun VoiceOrb(active: Boolean, size: Dp = 112.dp, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val reducedMotion = rememberReducedMotion()
    Box(modifier.size(size * 1.4f), contentAlignment = Alignment.Center) {
        if (active && !reducedMotion) {
            val wave = rememberInfiniteTransition(label = "wave")
            val progress by wave.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(Motion.PULSE, easing = LinearEasing), RepeatMode.Restart),
                label = "progress",
            )
            Box(
                Modifier
                    .size(size)
                    .scale(1f + 0.38f * progress)
                    .graphicsLayer { alpha = 0.35f * (1f - progress) }
                    .background(colors.primary, CircleShape)
            )
        }
        val background = if (active) {
            Brush.linearGradient(listOf(colors.primary, colors.secondary))
        } else {
            Brush.linearGradient(listOf(colors.surfaceContainerHigh, colors.surfaceContainerHigh))
        }
        Box(
            Modifier.size(size).clip(CircleShape).background(background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (active) HiggIcons.Mic else HiggIcons.MicOff,
                contentDescription = null,
                tint = if (active) colors.onPrimary else colors.onSurfaceVariant,
                modifier = Modifier.size(size * 0.36f),
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, trailing: String? = null, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = Space.xs, end = Space.xs, top = Space.lg, bottom = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Nhóm các dòng trong một thẻ, ngăn cách bằng đường kẻ mảnh. */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column { content() }
    }
}

@Composable
fun RowDivider() {
    HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

/** Ô icon tròn góc ở đầu dòng; đổi sang màu "đã xong" khi hoàn tất. */
@Composable
fun IconTile(icon: ImageVector, done: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (done) colors.tertiaryContainer else colors.primaryContainer, tween(Motion.STATE), label = "tile",
    )
    val tint by animateColorAsState(
        if (done) colors.onTertiaryContainer else colors.onPrimaryContainer, tween(Motion.STATE), label = "tint",
    )
    Box(Modifier.size(40.dp).clip(TileShape).background(container), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/**
 * Một bước thiết lập. Cả dòng là một nút (≥ 72dp) để dễ chạm; trạng thái được đọc bằng chữ,
 * không chỉ dựa vào màu.
 */
@Composable
fun SetupRow(
    icon: ImageVector,
    title: String,
    description: String,
    done: Boolean?,
    actionLabel: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val s = com.higg.LocalStrings.current
    val stateText = when (done) {
        true -> s.stateDone
        false -> actionLabel
        null -> s.stateOpenSettings
    }
    Surface(
        onClick = onClick,
        color = colors.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { role = Role.Button },
    ) {
        Row(
            Modifier.defaultMinSize(minHeight = 72.dp).padding(horizontal = Space.md, vertical = Space.md - Space.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon, done == true)
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.width(Space.sm))
            when (done) {
                true -> Icon(
                    HiggIcons.CheckCircle, contentDescription = stateText,
                    tint = colors.success, modifier = Modifier.size(24.dp),
                )
                false -> Text(
                    stateText,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onPrimaryContainer,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.primaryContainer)
                        .padding(horizontal = Space.md - Space.xs, vertical = Space.sm - 2.dp),
                )
                null -> Icon(
                    HiggIcons.ChevronRight, contentDescription = stateText,
                    tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Nhãn trạng thái nhỏ có chấm màu và chữ (màu không phải dấu hiệu duy nhất). */
@Composable
fun StatusPill(text: String, on: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .clip(CircleShape)
            .border(1.dp, colors.outlineVariant, CircleShape)
            .padding(horizontal = Space.md - Space.xs, vertical = Space.xs + 2.dp)
            .clearAndSetSemantics { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Box(Modifier.size(8.dp).background(if (on) colors.success else colors.outline, CircleShape))
        Text(text, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
    }
}
