package com.higg

import android.app.Activity
import android.os.Bundle

/** Màn hình trong suốt để gán vào Nút lối tắt: mở Gemini rồi đóng ngay. */
class LaunchGeminiActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Gemini.launch(this)
        finish()
    }
}
