package com.higg

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

/**
 * Nhận ra lúc Jovi (Tiểu V) vừa được đánh thức bằng "Hi Jovi" / "小V小V".
 *
 * Tín hiệu đánh thức của Jovi chỉ dành cho app hệ thống, nhưng mỗi lần thức dậy Jovi bật dịch
 * vụ nền `com.vivo.ai.copilot`, và sự kiện đó đọc được bằng quyền "Truy cập dữ liệu sử dụng"
 * (người dùng tự bật trong Cài đặt, không cần ADB hay Trợ năng).
 */
object JoviWatcher {
    private val JOVI_PACKAGES = setOf("com.vivo.ai.copilot", "com.vivo.agent")

    fun isJoviInstalled(context: Context): Boolean = JOVI_PACKAGES.any {
        runCatching { context.packageManager.getPackageInfo(it, 0) }.isSuccess
    }

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        return appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    /** Thời điểm Jovi thức dậy gần nhất sau [since] (ms, đồng hồ hệ thống), null nếu không có. */
    fun lastWake(context: Context, since: Long): Long? {
        val usage = context.getSystemService(UsageStatsManager::class.java)
        val events = usage.queryEvents(since, System.currentTimeMillis()) ?: return null
        val event = UsageEvents.Event()
        var last: Long? = null
        while (events.getNextEvent(event)) {
            if (event.eventType == UsageEvents.Event.FOREGROUND_SERVICE_START &&
                event.packageName in JOVI_PACKAGES
            ) {
                last = event.timeStamp
            }
        }
        return last
    }
}
