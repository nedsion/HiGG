package com.higg

import android.content.Context
import kotlin.math.sqrt

/**
 * Giọng của chủ máy: trung bình "dấu vân giọng" (x-vector) của các lần đọc mẫu, cùng ngưỡng
 * giống nhau tối thiểu và bộ từ vựng hợp với giọng đó — đều tính lúc đăng ký giọng.
 */
class VoiceProfile(val voiceprint: FloatArray, val threshold: Float, val strictGrammar: Boolean) {

    fun similarity(other: FloatArray?): Float = if (other == null) 0f else cosine(voiceprint, other)

    fun matches(other: FloatArray?): Boolean = similarity(other) >= threshold

    companion object {
        private const val FILE = "voice"
        private const val MIN_THRESHOLD = 0.6f

        /** Mỗi mẫu phải giống trung bình các mẫu còn lại ít nhất mức này. */
        const val MIN_SAMPLE_CONSISTENCY = 0.6f

        /**
         * Vị trí mẫu lệch nhất (giống trung bình các mẫu còn lại dưới mức cho phép), null nếu
         * mọi mẫu đều ổn. So với tất cả mẫu khác chứ không chỉ mẫu đầu, để một mẫu đầu hỏng
         * không làm hỏng cả lần đăng ký.
         */
        fun outlier(samples: List<FloatArray>): Int? {
            if (samples.size < 3) return null
            val scores = leaveOneOut(samples)
            val worst = scores.indices.minBy { scores[it] }
            return worst.takeIf { scores[it] < MIN_SAMPLE_CONSISTENCY }
        }

        private fun leaveOneOut(samples: List<FloatArray>) = samples.indices.map { i ->
            cosine(average(samples.filterIndexed { j, _ -> j != i }), samples[i])
        }

        fun load(context: Context): VoiceProfile? {
            val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            val saved = prefs.getString("voiceprint", null) ?: return null
            return VoiceProfile(
                saved.split(',').map { it.toFloat() }.toFloatArray(),
                // Hồ sơ tạo bởi bản cũ có thể có ngưỡng thấp hơn mức sàn hiện tại.
                prefs.getFloat("threshold", MIN_THRESHOLD).coerceAtLeast(MIN_THRESHOLD),
                prefs.getBoolean("strict", true),
            )
        }

        fun save(context: Context, profile: VoiceProfile) {
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .putString("voiceprint", profile.voiceprint.joinToString(","))
                .putFloat("threshold", profile.threshold)
                .putBoolean("strict", profile.strictGrammar)
                .apply()
        }

        fun clear(context: Context) {
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().apply()
        }

        /**
         * Lấy trung bình các mẫu. Ngưỡng dựa trên độ giống của từng mẫu so với các mẫu còn lại,
         * trừ hao vì giọng thật thay đổi theo ngày, khoảng cách tới micro, tiếng ồn.
         */
        fun fromSamples(samples: List<FloatArray>, strictGrammar: Boolean): VoiceProfile {
            val leaveOneOut = leaveOneOut(samples)
            // Sàn 0.6: đo thực tế giọng chủ máy ≈ 0.8–0.9, giọng người khác ≤ 0.6.
            val threshold = (leaveOneOut.min() - 0.1f).coerceIn(MIN_THRESHOLD, 0.75f)
            return VoiceProfile(average(samples), threshold, strictGrammar)
        }

        private fun normalize(v: FloatArray): FloatArray {
            val norm = sqrt(v.sumOf { (it * it).toDouble() }).toFloat()
            return FloatArray(v.size) { v[it] / norm }
        }

        private fun average(samples: List<FloatArray>): FloatArray {
            val sum = FloatArray(samples[0].size)
            for (s in samples.map(::normalize)) for (i in sum.indices) sum[i] += s[i]
            return normalize(sum)
        }

        private fun cosine(a: FloatArray, b: FloatArray): Float {
            val x = normalize(a)
            val y = normalize(b)
            return x.indices.sumOf { (x[it] * y[it]).toDouble() }.toFloat()
        }
    }
}
