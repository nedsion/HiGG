package com.higg

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.concurrent.thread

/**
 * Dịch vụ nền nghe "Ok Google" / "Hey Google" bằng Vosk (chạy hoàn toàn trên máy),
 * nghe thấy thì mở bảng nổi Gemini, đợi Gemini dùng xong micro rồi nghe tiếp.
 */
class HotwordService : Service() {

    companion object {
        private const val TAG = "HiGG"
        private const val ACTION_STOP = "com.higg.STOP"
        private const val ACTION_REFRESH = "com.higg.REFRESH"
        private const val CHANNEL_ID = "hotword"
        private const val NOTIFICATION_ID = 1
        private const val SAMPLE_RATE = HotwordDetector.SAMPLE_RATE

        /** Chế độ Jovi: chỉ tính lần Jovi thức dậy trong chừng này ms gần nhất. */
        private const val JOVI_WINDOW_MS = 3000L

        /** Sự kiện của Jovi có thể ghi chậm hơn lúc nó mở micro một chút, nên kiểm tra vài lần. */
        private val JOVI_CHECK_DELAYS_MS = longArrayOf(0, 300, 800, 1500)

        /** Lúc màn hình vừa bật: HiGG có thể vừa được rã đông, Jovi đã thức dậy từ vài giây trước. */
        private const val SCREEN_ON_WINDOW_MS = 8000L

        private val SCREEN_ON_CHECK_DELAYS_MS = longArrayOf(0, 500, 1500, 3000)

        private val runningState = MutableStateFlow(false)

        /** Để giao diện tự cập nhật khi dịch vụ bật/tắt. */
        val running: StateFlow<Boolean> = runningState

        val isRunning get() = runningState.value

        fun start(context: Context) {
            context.startForegroundService(Intent(context, HotwordService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, HotwordService::class.java))
        }

        /** Vẽ lại thông báo đang chạy (vd. sau khi đổi ngôn ngữ). */
        fun refreshNotification(context: Context) {
            context.startService(Intent(context, HotwordService::class.java).setAction(ACTION_REFRESH))
        }
    }

    @Volatile
    private var stopRequested = false
    private var worker: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Prefs.setEnabled(this, false)
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_REFRESH && worker != null) {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
            return START_STICKY
        }
        try {
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } catch (e: Exception) {
            // Android không cho bật micro khi app đang ở nền (vd. hệ thống tự khởi động lại dịch vụ).
            Log.w(TAG, "Không thể chạy nền với micro", e)
            BootReceiver.notifyNeedsRestart(this)
            stopSelf()
            return START_NOT_STICKY
        }
        if (worker == null && recordingCallback == null) {
            if (Prefs.trigger(this) == Prefs.Trigger.JOVI) {
                startJoviMode()
            } else {
                stopRequested = false
                worker = thread(name = "hotword") { listenLoop() }
            }
        }
        runningState.value = true
        return START_STICKY
    }

    override fun onDestroy() {
        recordingCallback?.let { getSystemService(AudioManager::class.java).unregisterAudioRecordingCallback(it) }
        recordingCallback = null
        KeepAliveOverlay.hide(this)
        screenReceiver?.let { unregisterReceiver(it) }
        screenReceiver = null
        main.removeCallbacksAndMessages(null)
        stopRequested = true
        worker?.interrupt()
        worker = null
        runningState.value = false
        super.onDestroy()
    }

    private val main = Handler(Looper.getMainLooper())
    private var recordingCallback: AudioManager.AudioRecordingCallback? = null
    private var handledJoviWake = 0L
    private var screenReceiver: BroadcastReceiver? = null

    /**
     * Chế độ "Hi Jovi": để chip nghe của vivo bắt từ khóa, HiGG không thu âm (chip của vivo tự
     * tạm dừng khi có app khác giữ micro) và không chạy mô hình nào. Android báo mỗi khi có app
     * bắt đầu thu âm; nếu đó là Jovi vừa thức dậy thì mở Gemini — Gemini mở lên thì Jovi tự nhả
     * micro và tắt viền sáng (đã thử trên OriginOS 6).
     */
    private fun startJoviMode() {
        handledJoviWake = System.currentTimeMillis()
        val callback = object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
                if (configs.isEmpty()) return
                for (delay in JOVI_CHECK_DELAYS_MS) main.postDelayed({ checkJoviWake() }, delay)
            }
        }
        getSystemService(AudioManager::class.java).registerAudioRecordingCallback(callback, main)
        recordingCallback = callback
        // Màn hình tắt thì cửa sổ nổi không hiển thị và OriginOS đóng băng HiGG. Jovi thức dậy sẽ
        // bật màn hình; tín hiệu "màn hình bật" rã đông HiGG, nên kiểm tra lại Jovi lúc đó.
        val wakeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                for (delay in SCREEN_ON_CHECK_DELAYS_MS) {
                    main.postDelayed({ checkJoviWake(SCREEN_ON_WINDOW_MS) }, delay)
                }
            }
        }
        registerReceiver(wakeReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        })
        screenReceiver = wakeReceiver
        val alive = KeepAliveOverlay.show(this)
        DebugLog.write(this, "bắt đầu chờ Jovi (cửa sổ giữ chạy: $alive)")
    }

    private fun checkJoviWake(window: Long = JOVI_WINDOW_MS) {
        val since = maxOf(handledJoviWake, System.currentTimeMillis() - window)
        val wake = JoviWatcher.lastWake(this, since) ?: return
        handledJoviWake = wake + 1
        DebugLog.write(this, "Jovi vừa thức dậy → mở Gemini")
        // Mở ngay: Gemini lấy micro thì Jovi đóng hẳn. (App khác giành micro hay quyền âm thanh thì
        // Jovi không đóng mà chuyển sang thẻ nhập chữ.)
        // Gemini chụp màn hình lúc mở và giữ làm nền, nên nền dính hiệu ứng xanh của Jovi — trừ khi
        // tắt "Sử dụng văn bản trên màn hình". Đã thử và không được: chờ Jovi đóng trước (Jovi đổi
        // sang thẻ nhập chữ), gọi lại Gemini sau 1,2 s (Gemini giữ ảnh cũ).
        Gemini.launchFromBackground(this)
    }

    private fun listenLoop() {
        val models = try {
            HotwordDetector.Models(this)
        } catch (e: Exception) {
            Log.e(TAG, "Không tải được mô hình", e)
            stopSelf()
            return
        }
        val detector = HotwordPipeline(models, VoiceProfile.load(this))
        val buffer = ShortArray(SAMPLE_RATE / 10) // 100 ms
        val capture = DebugLog.captureStream(this)
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        DebugLog.write(this, "bắt đầu nghe (${detector.description})")

        // Giữ micro mở suốt: OriginOS đóng băng app chạy nền không thu âm, kể cả khi có dịch vụ nền.
        // Khi Gemini dùng micro, Android cho app này nhận toàn số 0 (bị "silenced"),
        // nên chỉ nghe tiếp khi đã qua thời gian chờ và có âm thanh thật trở lại.
        var pausedUntil = 0L
        var audibleChunks = 0
        var paused = false

        try {
            while (!stopRequested) {
                val audio = try {
                    AudioRecord(
                        MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        maxOf(minBuffer, buffer.size * 4)
                    ).also { it.startRecording() }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Chưa có quyền micro", e)
                    break
                }

                while (!stopRequested) {
                    val n = audio.read(buffer, 0, buffer.size)
                    if (n <= 0) break // micro lỗi, mở lại

                    capture?.let { out ->
                        val bytes = java.nio.ByteBuffer.allocate(n * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                        bytes.asShortBuffer().put(buffer, 0, n)
                        out.write(bytes.array())
                    }

                    if (paused) {
                        val audible = (0 until n).any { buffer[it].toInt() != 0 }
                        audibleChunks = if (audible) audibleChunks + 1 else 0
                        if (SystemClock.elapsedRealtime() < pausedUntil || audibleChunks < 15) continue
                        paused = false
                        detector.reset()
                        DebugLog.write(this, "nghe tiếp")
                    }

                    val gate = detector.gate
                    if (gate.total % 3000 == 0L && gate.total > 0) { // mỗi 5 phút
                        DebugLog.write(this, "bộ nhận diện chạy ${gate.passed * 100 / gate.total}% thời gian")
                    }
                    val decision = detector.accept(buffer, n) ?: continue
                    DebugLog.write(this, decision.describe())
                    if (!decision.triggered) continue
                    detector.reset()
                    paused = true
                    audibleChunks = 0
                    pausedUntil = SystemClock.elapsedRealtime() + 2500
                    Gemini.launchFromBackground(this)
                }
                audio.stop()
                audio.release()
                if (!stopRequested) SystemClock.sleep(500)
            }
        } finally {
            detector.close()
            capture?.close()
            models.close()
        }
    }

    private fun buildNotification(): Notification {
        val s = strings(this)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, s.notificationChannel, NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, HotwordService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_higg)
            .setContentTitle(s.notificationTitle)
            .setContentText(if (Prefs.trigger(this) == Prefs.Trigger.JOVI) s.notificationTextJovi else s.notificationText)
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, s.notificationStop, stop).build())
            .setOngoing(true)
            .build()
    }
}
