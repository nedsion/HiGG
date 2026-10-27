package com.higg

/**
 * Quyết định một câu có mở Gemini hay không. Dùng chung cho dịch vụ nền và công cụ thử của bản
 * debug, để kết quả thử đúng với cách app thật hoạt động.
 */
class HotwordPipeline(models: HotwordDetector.Models, val profile: VoiceProfile?) : AutoCloseable {

    class Decision(val detection: Detection, val similarity: Float?, val triggered: Boolean) {
        fun describe(): String =
            "nghe: ${detection.text} (tin cậy: ${detection.confidence}, giống giọng: $similarity" +
                (detection.recheck?.let { ", nghe lại: ${it.take(2)}" } ?: "") + ")" +
                when {
                    triggered -> " → KÍCH HOẠT"
                    detection.isHotword -> " → bỏ qua: không phải giọng chủ máy"
                    else -> ""
                }
    }

    // Đã đăng ký giọng thì dùng bộ từ vựng hợp với giọng đó và bỏ qua giọng người khác.
    private val grammar =
        if (profile?.strictGrammar == false) HotwordDetector.GRAMMAR_LOOSE else HotwordDetector.GRAMMAR_STRICT

    // Có khóa giọng thì tiếng TV / người xung quanh đã bị chặn bằng giọng, bỏ nghe lại để không
    // bỏ sót chính chủ máy. Chưa khóa giọng thì nghe lại để lọc tiếng ồn bị ép thành từ khóa.
    private val recheck = profile == null

    private val detector = HotwordDetector(models, grammar, recheck)

    val gate = SpeechGate()

    val description
        get() = "khóa giọng: ${profile != null}, ngưỡng: ${profile?.threshold}, " +
            "từ vựng chặt: ${grammar == HotwordDetector.GRAMMAR_STRICT}, nghe lại: $recheck"

    /**
     * Trả về quyết định khi một câu vừa kết thúc, còn đang nói (hoặc đang im lặng, bộ nhận
     * diện không chạy) thì trả về null.
     */
    fun accept(buffer: ShortArray, length: Int): Decision? {
        var decision: Decision? = null
        for (chunk in gate.process(buffer, length)) {
            val detection = detector.accept(chunk, chunk.size) ?: continue
            val similarity = profile?.similarity(detection.voiceprint)
            val triggered = detection.isHotword && (profile == null || profile.matches(detection.voiceprint))
            decision = Decision(detection, similarity, triggered)
            if (triggered) break
        }
        return decision
    }

    fun reset() {
        detector.reset()
        gate.reset()
    }

    override fun close() = detector.close()
}
