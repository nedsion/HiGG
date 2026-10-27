package com.higg

import android.content.Context

object Prefs {
    private const val FILE = "settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LANGUAGE = "language"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** Cách gọi Gemini: nhờ "Hi Jovi" của vivo, hay HiGG tự nghe "Hey Google". */
    enum class Trigger { JOVI, GOOGLE }

    private const val KEY_TRIGGER = "trigger"

    /** Mặc định dùng "Hi Jovi" trên máy có Jovi: chip nghe của vivo chính xác và tiết kiệm pin hơn. */
    fun trigger(context: Context): Trigger =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_TRIGGER, null)
            ?.let { runCatching { Trigger.valueOf(it) }.getOrNull() }
            ?: if (JoviWatcher.isJoviInstalled(context)) Trigger.JOVI else Trigger.GOOGLE

    fun setTrigger(context: Context, trigger: Trigger) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY_TRIGGER, trigger.name).apply()
    }

    private const val KEY_WAIT_FOR_JOVI = "wait_for_jovi"

    /** Chờ Jovi tự tắt hẳn rồi mới mở Gemini: chậm hơn vài giây nhưng ảnh nền Gemini sạch. */
    fun waitForJovi(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_WAIT_FOR_JOVI, false)

    fun setWaitForJovi(context: Context, wait: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_WAIT_FOR_JOVI, wait).apply()
    }

    fun language(context: Context): Lang? =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)
            ?.let { runCatching { Lang.valueOf(it) }.getOrNull() }

    fun setLanguage(context: Context, lang: Lang) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY_LANGUAGE, lang.name).apply()
    }
}
