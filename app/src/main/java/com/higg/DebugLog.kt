package com.higg

import android.content.Context
import android.content.pm.ApplicationInfo
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * OriginOS chặn logcat của app thường, nên bản debug ghi ra file để đọc bằng
 * `adb shell run-as com.higg cat files/debug.log`.
 */
object DebugLog {
    private val time = SimpleDateFormat("HH:mm:ss", Locale.US)

    /**
     * Bản debug: nếu có file `files/record` thì ghi âm thanh thô (PCM 16 kHz mono) ra
     * `files/capture.pcm` để tinh chỉnh nhận diện trên máy tính.
     */
    fun captureStream(context: Context): OutputStream? {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return null
        if (!File(context.filesDir, "record").exists()) return null
        return File(context.filesDir, "capture.pcm").outputStream().buffered()
    }

    fun write(context: Context, message: String) {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        val file = File(context.filesDir, "debug.log")
        if (file.length() > 200_000) file.delete()
        file.appendText("${time.format(Date())} $message\n")
    }
}
