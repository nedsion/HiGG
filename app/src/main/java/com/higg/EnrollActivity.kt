package com.higg

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import com.higg.ui.EnrollActions
import com.higg.ui.EnrollScreen
import com.higg.ui.EnrollState
import com.higg.ui.HiggTheme
import kotlin.concurrent.thread

/**
 * Đăng ký giọng chủ máy: đọc "Hey Google" / "OK Google" vài lần để lấy dấu vân giọng.
 * Cùng lúc thử bộ từ vựng chặt trên chính giọng đó để chọn bộ từ vựng phù hợp.
 */
class EnrollActivity : ComponentActivity() {

    companion object {
        private const val SAMPLES_NEEDED = 6

        /** Giọng quá thất thường thì dừng sau chừng này lần đọc hợp lệ, dùng các mẫu đang có. */
        private const val MAX_ATTEMPTS = 12
    }

    private val state = mutableStateOf<EnrollState>(EnrollState.Idle(enrolled = false))

    @Volatile
    private var recording = false
    private var worker: Thread? = null
    private var serviceWasRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        showIdle()
        val s = strings(this)
        setContent {
            HiggTheme {
                CompositionLocalProvider(LocalStrings provides s) {
                EnrollScreen(
                    state = state.value,
                    actions = EnrollActions(
                        back = { finish() },
                        start = { startRecording() },
                        cancel = { stopRecording() },
                        delete = {
                            VoiceProfile.clear(this)
                            restartServiceIfRunning()
                            showIdle()
                        },
                    ),
                )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (recording) stopRecording()
    }

    private fun showIdle() {
        state.value = EnrollState.Idle(enrolled = VoiceProfile.load(this) != null)
    }

    /** Dịch vụ nền đọc hồ sơ giọng lúc khởi động, nên khởi động lại để áp dụng thay đổi. */
    private fun restartServiceIfRunning() {
        if (!HotwordService.isRunning) return
        HotwordService.stop(this)
        HotwordService.start(this)
    }

    private fun startRecording() {
        // Nhả micro của dịch vụ nền để màn hình này nghe rõ, xong thì bật lại.
        serviceWasRunning = HotwordService.isRunning
        if (serviceWasRunning) HotwordService.stop(this)
        recording = true
        state.value = EnrollState.Loading
        worker = thread(name = "enroll") { record() }
    }

    private fun stopRecording() {
        recording = false
        worker?.join(2000)
        worker = null
        if (serviceWasRunning) HotwordService.start(this)
        showIdle()
    }

    private fun record() {
        val samples = mutableListOf<FloatArray>()
        var strictHits = 0
        var attempts = 0
        HotwordDetector.Models(this).use { models ->
            HotwordDetector(models, HotwordDetector.GRAMMAR_LOOSE).use { loose ->
                HotwordDetector(models, HotwordDetector.GRAMMAR_STRICT).use { strict ->
                    val buffer = ShortArray(HotwordDetector.SAMPLE_RATE / 10)
                    val audio = AudioRecord(
                        MediaRecorder.AudioSource.VOICE_RECOGNITION, HotwordDetector.SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer.size * 8
                    )
                    audio.startRecording()
                    show(0, EnrollState.Message.SPEAK)
                    while (recording && samples.size < SAMPLES_NEEDED) {
                        val n = audio.read(buffer, 0, buffer.size)
                        if (n <= 0) continue
                        strict.accept(buffer, n)?.let { if (it.isHotword) strictHits++ }
                        val detection = loose.accept(buffer, n) ?: continue
                        DebugLog.write(this, "đăng ký: ${detection.text} (tin cậy: ${detection.confidence})")
                        val voiceprint = detection.voiceprint
                        if (detection.isHotword && voiceprint != null) {
                            samples += voiceprint
                            attempts++
                            // Đủ mẫu thì bỏ mẫu lệch hẳn (ho, ồn, đọc quá khác) và xin đọc thêm,
                            // để một lần hỏng không kéo tụt ngưỡng nhận giọng.
                            val outlier = if (samples.size >= SAMPLES_NEEDED && attempts < MAX_ATTEMPTS) {
                                VoiceProfile.outlier(samples)
                            } else null
                            if (outlier != null) {
                                samples.removeAt(outlier)
                                DebugLog.write(this, "đăng ký: bỏ mẫu lệch số ${outlier + 1}")
                                show(samples.size, EnrollState.Message.INCONSISTENT)
                            } else {
                                show(samples.size, EnrollState.Message.GOOD)
                            }
                        } else {
                            show(samples.size, EnrollState.Message.UNCLEAR)
                        }
                    }
                    audio.stop()
                    audio.release()
                }
            }
        }
        if (samples.size < SAMPLES_NEEDED) return

        // Bộ từ vựng chặt nhận ra gần đủ các lần đọc của chính người này thì dùng nó cho ít nhầm.
        val strictGrammar = strictHits >= SAMPLES_NEEDED - 1
        val profile = VoiceProfile.fromSamples(samples, strictGrammar)
        VoiceProfile.save(this, profile)
        DebugLog.write(
            this,
            "đã lưu giọng: ngưỡng ${profile.threshold}, từ vựng chặt nhận $strictHits/$SAMPLES_NEEDED"
        )
        runOnUiThread {
            recording = false
            worker = null
            if (serviceWasRunning) HotwordService.start(this)
            state.value = EnrollState.Done
        }
    }

    private fun show(count: Int, message: EnrollState.Message) = runOnUiThread {
        if (recording) state.value = EnrollState.Listening(count, SAMPLES_NEEDED, message)
    }
}
