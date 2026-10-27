package com.higg.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.google.zxing.common.BitMatrix
import com.higg.Donation
import com.higg.LocalStrings

class DonateActions(
    val back: () -> Unit,
    val copy: (label: String, value: String) -> Unit,
    val saveQr: () -> Unit,
)

@Composable
fun DonateScreen(qr: BitMatrix, snackbar: SnackbarHostState, actions: DonateActions) {
    val s = LocalStrings.current
    val colors = MaterialTheme.colorScheme
    val insets = WindowInsets.safeDrawing.asPaddingValues()

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(top = insets.calculateTopPadding())) {
                Row(
                    Modifier.fillMaxWidth().padding(Space.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = actions.back) {
                        Icon(HiggIcons.ArrowBack, contentDescription = s.back, tint = colors.onSurface)
                    }
                    Text(
                        s.donateTitle, style = MaterialTheme.typography.titleLarge, color = colors.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Space.md)
                        .padding(bottom = insets.calculateBottomPadding() + Space.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        s.donateIntro,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = Space.xs, vertical = Space.sm),
                    )

                    SectionHeader(s.donateBank, modifier = Modifier.widthIn(max = 600.dp))
                    GroupCard(Modifier.widthIn(max = 600.dp)) {
                        Column(Modifier.padding(Space.md), horizontalAlignment = Alignment.CenterHorizontally) {
                            QrCode(
                                qr,
                                Modifier.widthIn(max = 260.dp).fillMaxWidth().semantics {
                                    contentDescription = "VietQR ${Donation.BANK_NAME} ${Donation.ACCOUNT_NUMBER}"
                                },
                            )
                            Spacer(Modifier.height(Space.md))
                            Text(
                                Donation.BANK_NAME,
                                style = MaterialTheme.typography.titleMedium, color = colors.onSurface,
                            )
                            Spacer(Modifier.height(Space.xs))
                            Text(
                                s.donateScanHint,
                                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(Space.md))
                            FilledTonalButton(actions.saveQr, Modifier.fillMaxWidth().height(48.dp)) {
                                Text(s.saveQr)
                            }
                        }
                        RowDivider()
                        CopyRow(s.donateAccountNumber, Donation.ACCOUNT_NUMBER, mono = true, s.copy, actions.copy)
                        RowDivider()
                        CopyRow(s.donateAccountName, Donation.ACCOUNT_NAME, mono = false, s.copy, actions.copy)
                        RowDivider()
                        CopyRow(s.donateNote, Donation.TRANSFER_NOTE, mono = false, s.copy, actions.copy)
                    }

                    SectionHeader(s.donatePaypal, modifier = Modifier.widthIn(max = 600.dp))
                    GroupCard(Modifier.widthIn(max = 600.dp)) {
                        CopyRow("Email", Donation.PAYPAL, mono = false, s.copy, actions.copy)
                    }

                    Spacer(Modifier.height(Space.lg))
                    Text(s.donateThanks, style = MaterialTheme.typography.titleSmall, color = colors.primary)
                }
            }
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).padding(bottom = insets.calculateBottomPadding() + Space.md),
            )
        }
    }
}

/** Mã QR luôn đen trên nền trắng (cả khi giao diện tối) để app ngân hàng quét được, chừa lề 4 ô. */
@Composable
private fun QrCode(matrix: BitMatrix, modifier: Modifier = Modifier) {
    val quiet = 4
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cells = matrix.width + quiet * 2
            val cell = size.width / cells
            for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    drawRect(
                        Color.Black,
                        topLeft = Offset((x + quiet) * cell, (y + quiet) * cell),
                        size = Size(cell + 0.5f, cell + 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CopyRow(
    label: String,
    value: String,
    mono: Boolean,
    copyLabel: String,
    onCopy: (String, String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(start = Space.md, end = Space.sm, top = Space.sm, bottom = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge.let { if (mono) it.copy(fontFamily = FontFamily.Monospace) else it },
                color = colors.onSurface,
            )
        }
        TextButton(
            onClick = { onCopy(label, value) },
            // Đọc màn hình: "Sao chép Số tài khoản" thay vì chỉ "Sao chép".
            modifier = Modifier.height(48.dp).semantics { contentDescription = "$copyLabel $label" },
        ) {
            Text(copyLabel)
        }
    }
}
