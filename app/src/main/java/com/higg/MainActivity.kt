package com.higg

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.higg.ui.HiggTheme
import com.higg.ui.HomeActions
import com.higg.ui.HomeScreen
import com.higg.ui.SetupState

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_AUTOSTART = "autostart"

        private const val VIVO_AUTOSTART = "com.iqoo.secure.BGSTARTUPMANAGER"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_AUTOSTART, false)) startListening()

        setContent {
            var lang by remember { mutableStateOf(currentLang(this)) }
            HiggTheme {
              CompositionLocalProvider(LocalStrings provides stringsFor(lang)) {
                var state by remember { mutableStateOf(readSetupState()) }
                // Quay lại từ màn hình Cài đặt thì đọc lại trạng thái quyền.
                LifecycleResumeEffect(Unit) {
                    state = readSetupState()
                    onPauseOrDispose { }
                }
                val listening by HotwordService.running.collectAsStateWithLifecycle()
                val permissions = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    state = readSetupState()
                    // Cấp micro từ nút chính thì bật nghe luôn.
                    if (state.microphone && Prefs.isEnabled(this)) startListening()
                }
                fun requestPermissions() = permissions.launch(
                    arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
                )

                HomeScreen(
                    state = state,
                    listening = listening,
                    lang = lang,
                    actions = HomeActions(
                        toggleListening = { if (listening) stopListening() else startListening() },
                        grantMicrophone = { requestPermissions() },
                        grantAndListen = {
                            Prefs.setEnabled(this, true)
                            requestPermissions()
                        },
                        openOverlaySettings = {
                            openSettings(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        },
                        openBatterySettings = {
                            openSettings(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        },
                        openKeepPermissionsSettings = {
                            openSettings(Intent.ACTION_AUTO_REVOKE_PERMISSIONS)
                        },
                        openUsageAccessSettings = {
                            try {
                                openSettings(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            } catch (e: ActivityNotFoundException) {
                                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                        },
                        openAssistSettings = { startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) },
                        setWaitForJovi = {
                            Prefs.setWaitForJovi(this, it)
                            state = readSetupState()
                        },
                        setTrigger = {
                            Prefs.setTrigger(this, it)
                            state = readSetupState()
                            // Dịch vụ chọn cách nghe lúc khởi động, nên khởi động lại để áp dụng.
                            if (HotwordService.isRunning) {
                                HotwordService.stop(this)
                                HotwordService.start(this)
                            }
                        },
                        openAutostartSettings = {
                            // Trang "Tự động khởi động" của vivo; máy khác thì mở Thông tin ứng dụng.
                            try {
                                startActivity(Intent(VIVO_AUTOSTART))
                            } catch (e: ActivityNotFoundException) {
                                openSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            }
                        },
                        openEnroll = { startActivity(Intent(this, EnrollActivity::class.java)) },
                        testGemini = { Gemini.launch(this) },
                        openDonate = { startActivity(Intent(this, DonateActivity::class.java)) },
                        setLanguage = {
                            lang = it
                            Prefs.setLanguage(this, it)
                            // Thông báo đang hiện vẫn dùng ngôn ngữ cũ, vẽ lại cho khớp.
                            if (HotwordService.isRunning) HotwordService.refreshNotification(this)
                        },
                    ),
                )
              }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_AUTOSTART, false)) startListening()
    }

    private fun readSetupState() = SetupState(
        googleInstalled = Gemini.isGoogleInstalled(this),
        microphone = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        overlay = Settings.canDrawOverlays(this),
        battery = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName),
        keepPermissions = packageManager.isAutoRevokeWhitelisted,
        voiceEnrolled = VoiceProfile.load(this) != null,
        trigger = Prefs.trigger(this),
        waitForJovi = Prefs.waitForJovi(this),
        joviInstalled = JoviWatcher.isJoviInstalled(this),
        usageAccess = JoviWatcher.hasUsageAccess(this),
        noScreenFlash = runCatching {
            Settings.Secure.getInt(contentResolver, "assist_structure_enabled", 1) == 0
        }.getOrNull(),
    )

    private fun openSettings(action: String) {
        startActivity(Intent(action, Uri.parse("package:$packageName")))
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        Prefs.setEnabled(this, true)
        BootReceiver.cancelRestartNotice(this)
        HotwordService.start(this)
    }

    private fun stopListening() {
        Prefs.setEnabled(this, false)
        HotwordService.stop(this)
    }
}
