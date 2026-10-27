package com.higg

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager

object Gemini {
    const val GOOGLE_PKG = "com.google.android.googlequicksearchbox"

    /** Gọi khi máy đang khóa: chờ người dùng mở khóa trong khoảng này rồi mới mở Gemini. */
    private const val UNLOCK_WAIT_MS = 20_000L

    private val main = Handler(Looper.getMainLooper())
    private var pendingUntil = 0L
    private var unlockReceiverRegistered = false

    fun isGoogleInstalled(context: Context): Boolean =
        runCatching { context.packageManager.getPackageInfo(GOOGLE_PKG, 0) }.isSuccess

    /**
     * VOICE_COMMAND gửi thẳng tới app Google mở bảng nổi Gemini (FloatyActivity) ở chế độ
     * đang nghe — đã kiểm chứng trên OriginOS 6. ASSIST / VOICE_ASSIST chỉ mở trang chủ Google.
     */
    fun launch(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_VOICE_COMMAND)
            .setPackage(GOOGLE_PKG)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w("HiGG", "Không mở được Gemini", e)
            false
        }
    }

    /**
     * Mở Gemini khi app đang chạy nền. Máy đang khóa thì bật sáng màn hình và đợi người dùng
     * mở khóa (vân tay / khuôn mặt) — OriginOS không cho app thường tự gọi khung mở khóa.
     */
    fun launchFromBackground(context: Context) {
        val app = context.applicationContext
        val power = app.getSystemService(PowerManager::class.java)
        if (!power.isInteractive) {
            @Suppress("DEPRECATION")
            power.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "HiGG:wake"
            ).acquire(3000)
        }
        main.post {
            if (app.getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
                pendingUntil = SystemClock.elapsedRealtime() + UNLOCK_WAIT_MS
                registerUnlockReceiver(app)
                DebugLog.write(app, "máy đang khóa, chờ mở khóa")
            } else {
                launchWithOverlay(app)
            }
        }
    }

    private fun registerUnlockReceiver(app: Context) {
        if (unlockReceiverRegistered) return
        unlockReceiverRegistered = true
        app.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (SystemClock.elapsedRealtime() > pendingUntil) return
                pendingUntil = 0L
                launchWithOverlay(app)
            }
        }, IntentFilter(Intent.ACTION_USER_PRESENT))
    }

    /**
     * Từ Android 15, quyền "hiển thị trên ứng dụng khác" chỉ cho mở màn hình từ nền khi app
     * đang có một cửa sổ nổi hiện trên màn hình, nên thêm tạm một cửa sổ trong suốt 1×1 px.
     */
    private fun launchWithOverlay(app: Context) {
        val windowManager = app.getSystemService(WindowManager::class.java)
        // Trong suốt hoàn toàn, không vẽ gì: không để lại vệt màu hay gây lưu ảnh trên màn hình.
        val pixel = View(app).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setWillNotDraw(true)
        }
        val added = Settings.canDrawOverlays(app) && runCatching {
            windowManager.addView(pixel, WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSPARENT
            ).apply { gravity = Gravity.TOP or Gravity.START })
        }.isSuccess
        // Đợi cửa sổ thật sự hiện lên rồi mới mở Gemini, gỡ ngay sau khi Gemini đã mở.
        main.postDelayed({
            DebugLog.write(app, "mở Gemini (cửa sổ nổi: $added): ${launch(app)}")
            if (added) main.postDelayed({ runCatching { windowManager.removeView(pixel) } }, 600)
        }, 100)
    }
}
