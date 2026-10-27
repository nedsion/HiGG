package com.higg

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

/**
 * Công cụ thử của bản debug: cho file WAV (16 kHz, mono, 16-bit) chạy qua đúng chuỗi xử lý của
 * dịch vụ nền thay cho micro, để thử độ chính xác mà không phát ra tiếng.
 *
 *   adb shell am broadcast -n com.higg/.TestFeedReceiver -a com.higg.TEST_FEED --es set <tên>
 *     → chạy mọi file trong files/test/<tên>/, ghi kết quả ra files/test/result-<tên>.txt
 *   ... --es mode enroll --es set <tên>
 *     → đăng ký giọng từ các file đó (như màn hình Đăng ký giọng nói)
 *   ... --es mode launch
 *     → mở Gemini như khi nghe thấy từ khóa (thử việc mở màn hình từ nền)
 */
class TestFeedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        // ... --es mode launch → mở Gemini đúng như khi nghe thấy từ khóa lúc app đang chạy nền.
        if (intent.getStringExtra("mode") == "launch") {
            Gemini.launchFromBackground(app)
            return
        }
        val set = intent.getStringExtra("set") ?: return
        val enroll = intent.getStringExtra("mode") == "enroll"
        thread(name = "test-feed") {
            val dir = File(app.filesDir, "test/$set")
            val out = File(app.filesDir, "test/result-$set.txt")
            out.delete()
            val files = dir.listFiles { f -> f.name.endsWith(".wav") }.orEmpty().sortedBy { it.name }
            HotwordDetector.Models(app).use { models ->
                if (enroll) enroll(app, models, files, out) else run(app, models, files, out)
            }
            out.appendText("XONG\n")
        }
    }

    private fun run(context: Context, models: HotwordDetector.Models, files: List<File>, out: File) {
        HotwordPipeline(models, VoiceProfile.load(context)).use { pipeline ->
            out.appendText("# ${pipeline.description}\n")
            for (file in files) {
                pipeline.reset()
                val decisions = feed(file) { chunk, n -> pipeline.accept(chunk, n) }
                val triggered = decisions.any { it.triggered }
                out.appendText(
                    "${file.name}\t${if (triggered) "KÍCH HOẠT" else "-"}\t" +
                        decisions.joinToString(" | ") { it.describe() } + "\n"
                )
            }
        }
    }

    /** Giống EnrollActivity: lấy dấu vân giọng từ từ vựng lỏng, bỏ mẫu lệch, thử từ vựng chặt. */
    private fun enroll(context: Context, models: HotwordDetector.Models, files: List<File>, out: File) {
        val samples = mutableListOf<FloatArray>()
        var strictHits = 0
        HotwordDetector(models, HotwordDetector.GRAMMAR_LOOSE).use { loose ->
            HotwordDetector(models, HotwordDetector.GRAMMAR_STRICT).use { strict ->
                for (file in files) {
                    loose.reset()
                    strict.reset()
                    var strictHit = false
                    val detections = feed(file) { chunk, n ->
                        strict.accept(chunk, n)?.let { if (it.isHotword) strictHit = true }
                        loose.accept(chunk, n)
                    }
                    if (strictHit) strictHits++
                    val sample = detections.firstOrNull { it.isHotword }?.voiceprint
                    if (sample != null) samples += sample
                    out.appendText("${file.name}\tmẫu: ${sample != null}\tchặt: $strictHit\n")
                    while (samples.size >= 6) {
                        val outlier = VoiceProfile.outlier(samples) ?: break
                        samples.removeAt(outlier)
                        out.appendText("  bỏ mẫu lệch số ${outlier + 1}\n")
                    }
                }
            }
        }
        if (samples.size < 3) {
            out.appendText("Không đủ mẫu (${samples.size})\n")
            return
        }
        val profile = VoiceProfile.fromSamples(samples, strictGrammar = strictHits >= samples.size - 1)
        VoiceProfile.save(context, profile)
        out.appendText("Đã lưu giọng: ${samples.size} mẫu, ngưỡng ${profile.threshold}, từ vựng chặt ${profile.strictGrammar}\n")
    }

    /** Đưa file vào theo từng đoạn 100 ms như micro, thêm 1,5 giây im lặng để câu cuối kết thúc. */
    private fun <T> feed(file: File, accept: (ShortArray, Int) -> T?): List<T> {
        val pcm = readPcm(file)
        val padded = pcm + ShortArray(HotwordDetector.SAMPLE_RATE * 3 / 2)
        val chunk = HotwordDetector.SAMPLE_RATE / 10
        val results = mutableListOf<T>()
        var i = 0
        while (i < padded.size) {
            val n = minOf(chunk, padded.size - i)
            accept(padded.copyOfRange(i, i + n), n)?.let { results += it }
            i += n
        }
        return results
    }

    /** Đọc phần "data" của file WAV PCM 16-bit. */
    private fun readPcm(file: File): ShortArray {
        val bytes = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        var pos = 12
        while (pos + 8 <= bytes.limit()) {
            val id = String(ByteArray(4) { bytes.get(pos + it) }, Charsets.US_ASCII)
            val size = bytes.getInt(pos + 4)
            if (id == "data") {
                val samples = ShortArray(size / 2)
                bytes.position(pos + 8)
                bytes.asShortBuffer().get(samples)
                return samples
            }
            pos += 8 + size + (size and 1)
        }
        return ShortArray(0)
    }
}
