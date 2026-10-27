package com.higg

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "restart"
        private const val NOTIFICATION_ID = 2

        /**
         * Android 14+ không cho dịch vụ dùng micro tự chạy sau khi khởi động máy,
         * nên nhắc người dùng chạm một lần để bật lại.
         */
        fun notifyNeedsRestart(context: Context) {
            val s = strings(context)
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, s.restartChannel, NotificationManager.IMPORTANCE_DEFAULT)
            )
            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_AUTOSTART, true),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_higg)
                .setContentTitle(s.restartTitle)
                .setContentText(s.restartText)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            manager.notify(NOTIFICATION_ID, notification)
        }

        fun cancelRestartNotice(context: Context) {
            context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (!Prefs.isEnabled(context)) return
        try {
            HotwordService.start(context)
        } catch (e: Exception) {
            Log.w("HiGG", "Không tự bật được sau khởi động", e)
            notifyNeedsRestart(context)
        }
    }
}
