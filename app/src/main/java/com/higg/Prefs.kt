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

    fun language(context: Context): Lang? =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)
            ?.let { runCatching { Lang.valueOf(it) }.getOrNull() }

    fun setLanguage(context: Context, lang: Lang) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(KEY_LANGUAGE, lang.name).apply()
    }
}
