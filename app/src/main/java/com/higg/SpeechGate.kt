package com.higg

import kotlin.math.sqrt

/**
 * Bỏ qua lúc im lặng để tiết kiệm pin: chỉ cho bộ nhận diện chạy khi có âm thanh to hơn hẳn
 * tiếng ồn nền của phòng. Đo trên máy tính: giảm khoảng 45% CPU khi nửa thời gian im lặng.
 *
 * Mức ồn nền tự thích nghi (giảm nhanh khi phòng yên lại, tăng chậm để tiếng nói không bị
 * tính là ồn nền). Giữ 0,3 giây trước khi có tiếng để không mất chữ "Hey"/"OK" đầu câu, và
 * cho chạy thêm 1,5 giây sau khi hết tiếng để bộ nhận diện kết thúc câu.
 */
class SpeechGate {

    companion object {
        /** Tiếng phải to hơn ồn nền chừng này lần (≈ 8 dB) mới tính là có âm thanh. */
        private const val SPEECH_RATIO = 2.5

        /** Dưới mức này (RMS trên thang 16-bit) coi như im lặng dù phòng rất yên. */
        private const val MIN_SPEECH_RMS = 120.0

        private const val PRE_ROLL_CHUNKS = 3 // 0,3 giây
        private const val HANGOVER_CHUNKS = 15 // 1,5 giây
    }

    // Bắt đầu thấp rồi tự tăng theo phòng: nếu lấy đoạn đầu làm ồn nền mà người dùng nói ngay
    // lúc vừa bật thì chính giọng nói bị coi là ồn nền.
    private var noiseFloor = 0.0
    private var hangover = 0
    private val preRoll = ArrayDeque<ShortArray>()

    /** Tỉ lệ đoạn âm thanh được cho qua, để ghi log/đo đạc. */
    var passed = 0L
        private set
    var total = 0L
        private set

    /**
     * Trả về các đoạn cần đưa cho bộ nhận diện (rỗng khi đang im lặng; gồm cả phần đệm phía
     * trước khi vừa có tiếng).
     */
    fun process(buffer: ShortArray, length: Int): List<ShortArray> {
        val chunk = buffer.copyOf(length)
        val level = rms(chunk)
        total++
        val loud = level > maxOf(noiseFloor * SPEECH_RATIO, MIN_SPEECH_RMS)
        noiseFloor = if (level < noiseFloor) {
            noiseFloor * 0.8 + level * 0.2
        } else if (!loud) {
            noiseFloor * 0.98 + level * 0.02
        } else {
            noiseFloor * 0.999 + level * 0.001
        }

        val wasOpen = hangover > 0
        if (loud) hangover = HANGOVER_CHUNKS else if (hangover > 0) hangover--

        if (hangover == 0) {
            preRoll.addLast(chunk)
            if (preRoll.size > PRE_ROLL_CHUNKS) preRoll.removeFirst()
            return emptyList()
        }
        val out = if (!wasOpen) preRoll.toList() + chunk else listOf(chunk)
        preRoll.clear()
        passed += out.size
        return out
    }

    fun reset() {
        hangover = 0
        preRoll.clear()
    }

    private fun rms(chunk: ShortArray): Double {
        if (chunk.isEmpty()) return 0.0
        var sum = 0.0
        for (s in chunk) sum += s.toDouble() * s
        return sqrt(sum / chunk.size)
    }
}
