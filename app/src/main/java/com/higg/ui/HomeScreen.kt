package com.higg.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.higg.Lang
import com.higg.LocalStrings
import com.higg.Prefs

/** Trạng thái các bước thiết lập, đọc lại mỗi khi quay về màn hình. */
data class SetupState(
    val googleInstalled: Boolean,
    val microphone: Boolean,
    val overlay: Boolean,
    val battery: Boolean,
    val keepPermissions: Boolean,
    val voiceEnrolled: Boolean,
    val trigger: Prefs.Trigger,
    val joviInstalled: Boolean,
    val usageAccess: Boolean,
    /** Đã tắt "Sử dụng văn bản trên màn hình" (null: không đọc được cài đặt). */
    val noScreenFlash: Boolean?,
) {
    val jovi get() = trigger == Prefs.Trigger.JOVI
    private val required get() =
        listOf(microphone, overlay, battery, keepPermissions) + if (jovi) listOf(usageAccess) else emptyList()
    val requiredDone get() = required.count { it }
    val requiredTotal get() = required.size
}

class HomeActions(
    val toggleListening: () -> Unit,
    val grantMicrophone: () -> Unit,
    val grantAndListen: () -> Unit,
    val openOverlaySettings: () -> Unit,
    val openBatterySettings: () -> Unit,
    val openKeepPermissionsSettings: () -> Unit,
    val openAutostartSettings: () -> Unit,
    val openUsageAccessSettings: () -> Unit,
    val openAssistSettings: () -> Unit,
    val setTrigger: (Prefs.Trigger) -> Unit,
    val openEnroll: () -> Unit,
    val testGemini: () -> Unit,
    val openDonate: () -> Unit,
    val setLanguage: (Lang) -> Unit,
)

private val MaxWidth = Modifier.widthIn(max = 600.dp)

@Composable
fun HomeScreen(state: SetupState, listening: Boolean, lang: Lang, actions: HomeActions) {
    val s = LocalStrings.current
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        // Cắt nội dung ngay dưới thanh trạng thái để khi cuộn chữ không đè lên giờ/pin.
        LazyColumn(
            modifier = Modifier.padding(top = insets.calculateTopPadding()),
            contentPadding = PaddingValues(
                start = Space.md, end = Space.md,
                top = Space.lg,
                bottom = insets.calculateBottomPadding() + Space.xl,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { Header(lang, actions.setLanguage) }
            item { Hero(state, listening, actions) }

            if (state.joviInstalled) {
                item { SectionHeader(s.sectionTrigger, modifier = MaxWidth) }
                item {
                    GroupCard(MaxWidth) {
                        TriggerRow(s.triggerJoviTitle, s.triggerJoviDescription, state.jovi) { actions.setTrigger(Prefs.Trigger.JOVI) }
                        RowDivider()
                        TriggerRow(s.triggerGoogleTitle, s.triggerGoogleDescription, !state.jovi) { actions.setTrigger(Prefs.Trigger.GOOGLE) }
                    }
                }
            }

            item { SectionHeader(s.sectionSetup, "${state.requiredDone}/${state.requiredTotal}", MaxWidth) }
            item {
                GroupCard(MaxWidth) {
                    SetupRow(HiggIcons.Mic, s.micTitle, s.micDescription, state.microphone, s.actionGrant, actions.grantMicrophone)
                    RowDivider()
                    SetupRow(HiggIcons.Layers, s.overlayTitle, s.overlayDescription, state.overlay, s.actionAllow, actions.openOverlaySettings)
                    RowDivider()
                    SetupRow(HiggIcons.Battery, s.batteryTitle, s.batteryDescription, state.battery, s.actionAllow, actions.openBatterySettings)
                    RowDivider()
                    SetupRow(HiggIcons.Lock, s.keepPermissionsTitle, s.keepPermissionsDescription, state.keepPermissions, s.actionTurnOff, actions.openKeepPermissionsSettings)
                    if (state.jovi) {
                        RowDivider()
                        SetupRow(HiggIcons.Activity, s.usageTitle, s.usageDescription, state.usageAccess, s.actionAllow, actions.openUsageAccessSettings)
                    }
                    RowDivider()
                    SetupRow(HiggIcons.Power, s.autostartTitle, s.autostartDescription, null, "", actions.openAutostartSettings)
                    RowDivider()
                    // Tùy chọn, không tính vào số bước: Android luôn phủ màu cầu vồng ("nháy màn hình")
                    // khi trợ lý không phải app hệ thống đọc màn hình — app Google trên ROM nội địa là vậy.
                    SetupRow(HiggIcons.Sparkle, s.noFlashTitle, s.noFlashDescription, state.noScreenFlash, s.actionTurnOff, actions.openAssistSettings)
                }
            }

            // Khóa giọng chỉ dùng cho chế độ HiGG tự nghe; chế độ Jovi dùng cài đặt giọng của Jovi.
            if (!state.jovi) {
                item { SectionHeader(s.sectionVoiceLock, modifier = MaxWidth) }
                item { VoiceLockCard(state.voiceEnrolled, actions.openEnroll) }
            }

            item { SectionHeader(s.sectionOtherWays, modifier = MaxWidth) }
            item {
                GroupCard(MaxWidth) {
                    InfoRow(HiggIcons.Button, s.shortcutTitle, s.shortcutDescription)
                    RowDivider()
                    if (state.jovi) {
                        InfoRow(HiggIcons.Mic, s.joviEnableTitle, s.joviEnableDescription)
                    } else {
                        InfoRow(HiggIcons.Quiet, s.joviTitle, s.joviDescription)
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = actions.testGemini,
                    modifier = MaxWidth.fillMaxWidth().padding(top = Space.md).height(52.dp),
                ) {
                    Icon(HiggIcons.Sparkle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(Space.sm))
                    Text(s.testGemini)
                }
            }

            item { SectionHeader(s.sectionSupport, modifier = MaxWidth) }
            item { SupportCard(actions.openDonate) }

            item {
                Row(
                    MaxWidth.padding(top = Space.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        HiggIcons.Lock, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.size(Space.sm))
                    Text(s.privacyNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Header(lang: Lang, onLanguage: (Lang) -> Unit) {
    val s = LocalStrings.current
    Row(
        MaxWidth.fillMaxWidth().padding(horizontal = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("HiGG", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(s.tagline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(Space.sm))
        SingleChoiceSegmentedButtonRow(Modifier.semantics { contentDescription = s.languageLabel }) {
            Lang.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = lang == option,
                    onClick = { onLanguage(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, Lang.entries.size),
                    icon = {},
                    modifier = Modifier.height(48.dp).semantics {
                        contentDescription = if (option == Lang.VI) "Tiếng Việt" else "English"
                    },
                ) {
                    Text(option.name, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun Hero(state: SetupState, listening: Boolean, actions: HomeActions) {
    val s = LocalStrings.current
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CardShape,
        color = colors.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
        modifier = MaxWidth.fillMaxWidth().padding(top = Space.lg),
    ) {
        Column(Modifier.padding(Space.lg), horizontalAlignment = Alignment.CenterHorizontally) {
            VoiceOrb(active = listening)
            Spacer(Modifier.height(Space.sm))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(
                    if (listening) s.statusListening else s.statusOff,
                    style = MaterialTheme.typography.headlineSmall, color = colors.onSurface,
                )
                Spacer(Modifier.height(Space.xs))
                Text(
                    when {
                        !state.googleInstalled -> s.hintNeedGoogle
                        listening -> if (state.jovi) s.hintListeningJovi else s.hintListening
                        else -> s.hintOff
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(Space.md))
            when {
                state.jovi -> StatusPill(s.pillJovi, on = true)
                else -> StatusPill(if (state.voiceEnrolled) s.pillOwnerOnly else s.pillAnyVoice, on = state.voiceEnrolled)
            }
            Spacer(Modifier.height(Space.lg))
            val buttonModifier = Modifier.fillMaxWidth().height(56.dp)
            when {
                !state.microphone -> Button(actions.grantAndListen, buttonModifier) { Text(s.grantMicrophone) }
                listening -> FilledTonalButton(actions.toggleListening, buttonModifier) { Text(s.stopListening) }
                else -> Button(actions.toggleListening, buttonModifier) { Text(s.startListening) }
            }
        }
    }
}

@Composable
private fun VoiceLockCard(enrolled: Boolean, onEnroll: () -> Unit) {
    val s = LocalStrings.current
    val colors = MaterialTheme.colorScheme
    GroupCard(MaxWidth) {
        Column(Modifier.padding(Space.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(HiggIcons.Shield, done = enrolled)
                Spacer(Modifier.size(Space.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (enrolled) s.voiceLockedTitle else s.voiceUnlockedTitle,
                        style = MaterialTheme.typography.titleSmall, color = colors.onSurface,
                    )
                    Text(
                        if (enrolled) s.voiceLockedDescription else s.voiceUnlockedDescription,
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(Space.md))
            val modifier = Modifier.fillMaxWidth().height(48.dp)
            if (enrolled) {
                OutlinedButton(onEnroll, modifier) { Text(s.manageVoice) }
            } else {
                FilledTonalButton(onEnroll, modifier) { Text(s.enrollVoice) }
            }
        }
    }
}

@Composable
private fun SupportCard(onDonate: () -> Unit) {
    val s = LocalStrings.current
    val colors = MaterialTheme.colorScheme
    GroupCard(MaxWidth) {
        Column(Modifier.padding(Space.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(HiggIcons.Heart)
                Spacer(Modifier.size(Space.md))
                Column(Modifier.weight(1f)) {
                    Text(s.supportTitle, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                    Text(s.supportDescription, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(Space.md))
            FilledTonalButton(onDonate, Modifier.fillMaxWidth().height(48.dp)) { Text(s.supportButton) }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, title: String, description: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.md, vertical = Space.md - Space.xs)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon)
        Spacer(Modifier.size(Space.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Một lựa chọn cách gọi: cả dòng là nút chọn (radio), trạng thái đọc bằng chữ "Đang dùng". */
@Composable
private fun TriggerRow(title: String, description: String, selected: Boolean, onSelect: () -> Unit) {
    val s = LocalStrings.current
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onSelect,
        color = colors.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                this.selected = selected
            },
    ) {
        Row(
            Modifier.padding(horizontal = Space.md, vertical = Space.md - Space.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            if (selected) {
                Spacer(Modifier.width(Space.sm))
                Text(s.stateInUse, style = MaterialTheme.typography.labelMedium, color = colors.primary)
            }
        }
    }
}
