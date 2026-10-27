package com.higg.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.higg.LocalStrings
import com.higg.Strings

sealed interface EnrollState {
    enum class Message { SPEAK, GOOD, UNCLEAR, INCONSISTENT }

    data class Idle(val enrolled: Boolean) : EnrollState
    data object Loading : EnrollState
    data class Listening(val count: Int, val total: Int, val message: Message) : EnrollState
    data object Done : EnrollState
}

class EnrollActions(
    val back: () -> Unit,
    val start: () -> Unit,
    val cancel: () -> Unit,
    val delete: () -> Unit,
)

@Composable
fun EnrollScreen(state: EnrollState, actions: EnrollActions) {
    val s = LocalStrings.current
    val colors = MaterialTheme.colorScheme
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    var confirmDelete by remember { mutableStateOf(false) }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = insets.calculateTopPadding(), bottom = insets.calculateBottomPadding()),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(Space.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = actions.back) {
                    Icon(HiggIcons.ArrowBack, contentDescription = s.back, tint = colors.onSurface)
                }
                Text(
                    s.enrollTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(Space.lg))
                ProgressOrb(state)
                Spacer(Modifier.height(Space.lg))
                Column(
                    Modifier.widthIn(max = 480.dp).semantics(mergeDescendants = true) {
                        liveRegion = LiveRegionMode.Polite
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        title(s, state), style = MaterialTheme.typography.headlineSmall,
                        color = colors.onSurface, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        subtitle(s, state), style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant, textAlign = TextAlign.Center,
                    )
                }
                if (state is EnrollState.Idle) {
                    Spacer(Modifier.height(Space.xl))
                    GroupCard(Modifier.widthIn(max = 480.dp)) {
                        InfoRow(HiggIcons.Quiet, s.hintQuietTitle, s.hintQuietDescription)
                        RowDivider()
                        InfoRow(HiggIcons.Hand, s.hintHoldTitle, s.hintHoldDescription)
                        RowDivider()
                        InfoRow(HiggIcons.Repeat, s.hintRepeatTitle, s.hintRepeatDescription)
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                val buttonModifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().height(56.dp)
                when (state) {
                    is EnrollState.Idle -> {
                        Button(actions.start, buttonModifier) {
                            Text(if (state.enrolled) s.enrollStartAgain else s.enrollStart)
                        }
                        if (state.enrolled) {
                            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.height(48.dp)) {
                                Icon(HiggIcons.Trash, contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(Space.sm))
                                Text(s.deleteVoice, color = colors.error)
                            }
                        }
                    }
                    EnrollState.Loading, is EnrollState.Listening ->
                        FilledTonalButton(actions.cancel, buttonModifier) { Text(s.cancel) }
                    EnrollState.Done -> Button(actions.back, buttonModifier) { Text(s.finish) }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(s.confirmDeleteTitle) },
            text = { Text(s.confirmDeleteText) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; actions.delete() }) {
                    Text(s.delete, color = colors.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(s.keep) } },
        )
    }
}

@Composable
private fun ProgressOrb(state: EnrollState) {
    val s = LocalStrings.current
    val colors = MaterialTheme.colorScheme
    val (count, total) = when (state) {
        is EnrollState.Listening -> state.count to state.total
        EnrollState.Done -> 1 to 1
        else -> 0 to 1
    }
    val progress by animateFloatAsState(count / total.toFloat(), tween(Motion.STATE), label = "progress")
    // Màn hình thấp (xoay ngang) thì thu nhỏ để còn chỗ cho tiêu đề và hướng dẫn.
    val compact = LocalConfiguration.current.screenHeightDp < 520
    val ring = if (compact) 120.dp else 200.dp
    val orb = if (compact) 56.dp else 96.dp
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(ring).semantics {
                contentDescription = if (state is EnrollState.Listening) s.enrollProgress(state.count, state.total) else ""
            },
            strokeWidth = 6.dp,
            trackColor = colors.surfaceContainerHigh,
            color = if (state == EnrollState.Done) colors.success else colors.primary,
            strokeCap = StrokeCap.Round,
        )
        when (state) {
            is EnrollState.Listening -> VoiceOrb(active = true, size = orb)
            EnrollState.Done -> Icon(
                HiggIcons.Shield, contentDescription = null, tint = colors.success, modifier = Modifier.size(orb * 2 / 3),
            )
            EnrollState.Loading -> CircularProgressIndicator(Modifier.size(40.dp), color = colors.primary)
            is EnrollState.Idle -> VoiceOrb(active = false, size = orb)
        }
    }
}

private fun title(s: Strings, state: EnrollState) = when (state) {
    is EnrollState.Idle -> if (state.enrolled) s.enrollIdleSavedTitle else s.enrollIdleNewTitle
    EnrollState.Loading -> s.enrollLoadingTitle
    is EnrollState.Listening -> s.enrollListeningTitle(state.count, state.total)
    EnrollState.Done -> s.enrollDoneTitle
}

private fun subtitle(s: Strings, state: EnrollState) = when (state) {
    is EnrollState.Idle -> if (state.enrolled) s.enrollIdleSavedDescription else s.enrollIdleNewDescription
    EnrollState.Loading -> s.enrollLoadingDescription
    is EnrollState.Listening -> when (state.message) {
        EnrollState.Message.SPEAK -> s.enrollSpeak
        EnrollState.Message.GOOD -> s.enrollGood
        EnrollState.Message.UNCLEAR -> s.enrollUnclear
        EnrollState.Message.INCONSISTENT -> s.enrollInconsistent
    }
    EnrollState.Done -> s.enrollDoneDescription
}
