package com.higg

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * Cửa sổ nổi 1×1 px trong suốt hoàn toàn, không vẽ gì, không nhận chạm. Ở chế độ "Hi Jovi"
 * HiGG không thu âm (để chip nghe của vivo hoạt động), và OriginOS đóng băng app chạy nền không
 * thu âm; app đang có cửa sổ nổi hiển thị thì không bị đóng băng, nên vẫn nhận được tin báo
 * lúc Jovi thức dậy.
 */
object KeepAliveOverlay {
    private var view: View? = null

    fun show(context: Context): Boolean {
        if (view != null) return true
        if (!Settings.canDrawOverlays(context)) return false
        val app = context.applicationContext
        val pixel = View(app).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setWillNotDraw(true)
        }
        return runCatching {
            app.getSystemService(WindowManager::class.java).addView(pixel, WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSPARENT
            ).apply { gravity = Gravity.TOP or Gravity.START })
            view = pixel
        }.isSuccess
    }

    fun hide(context: Context) {
        val pixel = view ?: return
        runCatching { context.applicationContext.getSystemService(WindowManager::class.java).removeView(pixel) }
        view = null
    }
}
