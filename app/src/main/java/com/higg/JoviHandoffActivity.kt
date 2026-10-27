package com.higg

import android.annotation.SuppressLint
import android.app.Activity
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlin.concurrent.thread

/**
 * Màn hình trong suốt chen giữa Jovi và Gemini. Gemini chụp màn hình lúc mở để hiểu ngữ cảnh
 * (Circle to Search cần điều này) và giữ ảnh đó làm nền; mở lúc hiệu ứng xanh của Jovi còn chạy
 * thì ảnh nền dính màu xanh.
 *
 * Jovi đóng hẳn khi app trên cùng giành micro (như khi Gemini mở). Màn hình này giành micro, chờ
 * Jovi nhả micro rồi đợi hiệu ứng tắt, sau đó mới mở Gemini nên Gemini chụp được màn hình sạch.
 */
class JoviHandoffActivity : Activity() {

    companion object {
        private const val SAMPLE_RATE = 16000

        /** Jovi đã nhả micro: chờ hiệu ứng của Jovi tắt hẳn. */
        private const val SETTLE_MS = 350L

        /** Jovi không chịu nhả micro thì sau chừng này vẫn mở Gemini. */
        private const val MAX_WAIT_MS = 2000L
        private const val POLL_MS = 80L
    }

    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var recording = true
    private var started = 0L
    private var joviGoneAt = 0L
    private var sessionId = 0

    @SuppressLint("MissingPermission") // chỉ được mở khi dịch vụ nghe đang chạy, tức đã có quyền micro
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        started = SystemClock.elapsedRealtime()
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val audio = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuffer * 2
            ).also { it.startRecording() }
        }.getOrNull()
        if (audio == null) {
            openGemini()
            return
        }
        sessionId = audio.audioSessionId
        thread(name = "handoff") {
            val buffer = ShortArray(minBuffer)
            while (recording) audio.read(buffer, 0, buffer.size)
            audio.stop()
            audio.release()
        }
        main.post(::poll)
    }

    /** Jovi đã nhả micro khi chỉ còn màn hình này thu âm và không bị Android chặn tiếng. */
    private fun poll() {
        if (isFinishing) return
        val now = SystemClock.elapsedRealtime()
        val configs = getSystemService(AudioManager::class.java).activeRecordingConfigurations
        val mine = configs.firstOrNull { it.clientAudioSessionId == sessionId }
        val joviGone = mine != null && !mine.isClientSilenced && configs.size == 1
        if (joviGone && joviGoneAt == 0L) joviGoneAt = now
        if (!joviGone) joviGoneAt = 0L
        when {
            joviGoneAt != 0L && now - joviGoneAt >= SETTLE_MS -> openGemini()
            now - started >= MAX_WAIT_MS -> openGemini()
            else -> main.postDelayed(::poll, POLL_MS)
        }
    }

    private fun openGemini() {
        recording = false
        DebugLog.write(this, "chuyển sang Gemini sau ${SystemClock.elapsedRealtime() - started} ms")
        // Nhả micro trước để Gemini nghe được ngay.
        main.postDelayed({
            Gemini.launch(this)
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }, 60)
    }

    override fun onDestroy() {
        recording = false
        main.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
