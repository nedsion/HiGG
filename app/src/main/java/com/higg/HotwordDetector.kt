package com.higg

import android.content.Context
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.SpeakerModel

/**
 * Một câu vừa nói xong: chữ nghe được, độ tin cậy của từ khóa, "dấu vân giọng" của người nói,
 * và (nếu có kiểm tra lại) chữ mà mô hình đầy đủ nghe được.
 */
class Detection(
    val text: String,
    val confidence: Double,
    val voiceprint: FloatArray?,
    val recheck: List<String>? = null,
) {
    val isHotword get() = confidence >= MIN_CONFIDENCE

    companion object {
        const val MIN_CONFIDENCE = 0.6
    }
}

/**
 * Nhận diện "Ok Google" / "Hey Google" bằng Vosk, chạy hoàn toàn trên máy.
 *
 * Từ vựng giới hạn khiến tiếng nói chuyện xung quanh (vd. "Ê gu gồ ơi") đôi khi bị ép thành
 * "hey google". Với [recheck], câu nghi là từ khóa được nghe lại bằng mô hình đầy đủ (không giới
 * hạn từ vựng) và chỉ được nhận khi mô hình đó cũng nghe ra "ok/okay/hey google".
 */
class HotwordDetector(models: Models, grammar: String, recheck: Boolean = false) : AutoCloseable {

    companion object {
        const val SAMPLE_RATE = 16000

        // Từ vựng giới hạn: Vosk chỉ phân biệt từ khóa với "tiếng khác" ([unk]).
        // Không có "okay google": "ok" và "okay" chia nhau độ tin cậy.
        const val GRAMMAR_LOOSE = """["ok google", "hey google", "[unk]"]"""

        // Thêm các từ dễ nhầm, nếu không "OK, go ahead" / "Hey girl" bị ép thành "ok/hey google".
        // Ít kích hoạt nhầm hơn nhưng có thể bỏ sót nếu giọng người dùng không rõ chữ "google".
        const val GRAMMAR_STRICT = """["ok google", "hey google", "ok", "okay", "hey", "go", "good",
            "got", "get", "girl", "go ahead", "going", "goodbye", "gold", "back", "it", "[unk]"]"""

        private val WAKE_PREFIXES = setOf("ok", "hey")

        /** Từ khóa phải nằm trong mấy từ đầu câu (cho phép một tiếng [unk] lọt trước "ok"). */
        private const val MAX_HOTWORD_POSITION = 2

        private val RECHECK_HOTWORD = Regex("""\b(ok|okay|hey) google\b""")
        private const val RECHECK_ALTERNATIVES = 5

        /** Giữ tối đa 6 giây âm thanh của câu đang nói để nghe lại. */
        private const val MAX_UTTERANCE_CHUNKS = 60
    }

    /** Mô hình nặng nên chỉ tải một lần và dùng chung cho nhiều bộ nhận diện. */
    class Models(context: Context) : AutoCloseable {
        val speech = Model(ModelInstaller.install(context, ModelInstaller.SPEECH).absolutePath)
        val speaker = SpeakerModel(ModelInstaller.install(context, ModelInstaller.SPEAKER).absolutePath)

        override fun close() {
            speaker.close()
            speech.close()
        }
    }

    private val recognizer = Recognizer(models.speech, SAMPLE_RATE.toFloat(), grammar).apply {
        setWords(true)
        setSpeakerModel(models.speaker)
    }

    private val verifier = if (recheck) {
        Recognizer(models.speech, SAMPLE_RATE.toFloat()).apply { setMaxAlternatives(RECHECK_ALTERNATIVES) }
    } else null

    private val utterance = ArrayDeque<ShortArray>()

    /** Trả về kết quả khi một câu vừa kết thúc, còn đang nói thì trả về null. */
    fun accept(buffer: ShortArray, length: Int): Detection? {
        if (verifier != null) {
            utterance.addLast(buffer.copyOf(length))
            if (utterance.size > MAX_UTTERANCE_CHUNKS) utterance.removeFirst()
        }
        // Chỉ xét kết quả cuối của mỗi câu (có độ tin cậy từng từ), kết quả tạm
        // dễ nhầm "going to" thành "google".
        if (!recognizer.acceptWaveForm(buffer, length)) return null
        val audio = utterance.toList()
        utterance.clear()

        val result = JSONObject(recognizer.result)
        val text = result.optString("text")
        if (text.isEmpty()) return null
        val voiceprint = result.optJSONArray("spk")?.let { spk ->
            FloatArray(spk.length()) { spk.getDouble(it).toFloat() }
        }
        var confidence = hotwordConfidence(result)
        var heard: List<String>? = null
        if (verifier != null && confidence >= Detection.MIN_CONFIDENCE) {
            heard = transcribe(verifier, audio)
            if (heard.none { RECHECK_HOTWORD.containsMatchIn(it) }) confidence = 0.0
        }
        return Detection(text, confidence, voiceprint, heard)
    }

    fun reset() {
        recognizer.reset()
        verifier?.reset()
        utterance.clear()
    }

    override fun close() {
        recognizer.close()
        verifier?.close()
    }

    /** Các cách nghe khả dĩ nhất của mô hình đầy đủ cho đoạn âm thanh. */
    private fun transcribe(verifier: Recognizer, audio: List<ShortArray>): List<String> {
        for (chunk in audio) verifier.acceptWaveForm(chunk, chunk.size)
        val alternatives = JSONObject(verifier.finalResult).optJSONArray("alternatives")
        verifier.reset()
        return List(alternatives?.length() ?: 0) { alternatives!!.getJSONObject(it).optString("text") }
    }

    /** Độ tin cậy thấp nhất của cặp "ok/hey" + "google" ở đầu câu, 0 nếu không có. */
    private fun hotwordConfidence(result: JSONObject): Double {
        val words = result.optJSONArray("result") ?: return 0.0
        var best = 0.0
        for (i in 1..minOf(MAX_HOTWORD_POSITION, words.length() - 1)) {
            val first = words.getJSONObject(i - 1)
            val second = words.getJSONObject(i)
            if (second.optString("word") == "google" && first.optString("word") in WAKE_PREFIXES) {
                best = maxOf(best, minOf(first.optDouble("conf"), second.optDouble("conf")))
            }
        }
        return best
    }
}
