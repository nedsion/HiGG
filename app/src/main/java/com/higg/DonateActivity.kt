package com.higg

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.higg.ui.DonateActions
import com.higg.ui.DonateScreen
import com.higg.ui.HiggTheme
import kotlinx.coroutines.launch

class DonateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val s = strings(this)
        val qr = Donation.qrMatrix()
        setContent {
            HiggTheme {
                CompositionLocalProvider(LocalStrings provides s) {
                    val snackbar = remember { SnackbarHostState() }
                    val scope = rememberCoroutineScope()
                    DonateScreen(
                        qr = qr,
                        snackbar = snackbar,
                        actions = DonateActions(
                            back = { finish() },
                            copy = { label, value ->
                                getSystemService(ClipboardManager::class.java)
                                    .setPrimaryClip(ClipData.newPlainText(label, value))
                                // Luôn báo: OriginOS không hiện xác nhận sao chép của Android 13+.
                                scope.launch { snackbar.showSnackbar(s.copied(label)) }
                            },
                            saveQr = {
                                val ok = Donation.saveQrToGallery(this)
                                scope.launch { snackbar.showSnackbar(if (ok) s.qrSaved else s.qrSaveFailed) }
                            },
                        ),
                    )
                }
            }
        }
    }
}
