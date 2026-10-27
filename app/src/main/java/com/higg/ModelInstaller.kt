package com.higg

import android.content.Context
import java.io.File

/** Chép mô hình Vosk từ assets ra bộ nhớ trong (Vosk cần đường dẫn file thật). */
object ModelInstaller {
    const val SPEECH = "model"
    const val SPEAKER = "spk"

    private val VERSIONS = mapOf(
        SPEECH to "vosk-model-small-en-us-0.15",
        SPEAKER to "vosk-model-spk-0.4",
    )

    fun install(context: Context, assetDir: String): File {
        val version = VERSIONS.getValue(assetDir)
        val target = File(context.filesDir, assetDir)
        val marker = File(target, ".installed")
        if (marker.exists() && marker.readText() == version) return target

        target.deleteRecursively()
        copyDir(context, assetDir, target)
        marker.writeText(version)
        return target
    }

    private fun copyDir(context: Context, assetPath: String, dest: File) {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            dest.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
            return
        }
        dest.mkdirs()
        for (child in children) copyDir(context, "$assetPath/$child", File(dest, child))
    }
}
