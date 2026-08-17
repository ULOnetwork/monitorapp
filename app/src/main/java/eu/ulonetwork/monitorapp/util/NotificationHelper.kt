package eu.ulonetwork.monitorapp.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import eu.ulonetwork.monitorapp.MainActivity
import eu.ulonetwork.monitorapp.R
import eu.ulonetwork.monitorapp.data.db.AlertEventType

object NotificationHelper {

    const val CHANNEL_ID = "keyword-alerts"
    private const val NOTIFICATION_ID_BASE = 1000

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_description)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showKeywordAlert(
        context: Context,
        keyword: String,
        eventType: AlertEventType,
        appPackage: String,
        snippet: String,
        logEntryId: Long
    ) {
        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_LOG, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            logEntryId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleRes = if (eventType == AlertEventType.RESOLVED) {
            R.string.notification_title_resolved
        } else {
            R.string.notification_title_issue
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(titleRes, keyword))
            .setContentText(context.getString(R.string.notification_text, appPackage, snippet))
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                context.getString(R.string.notification_text, appPackage, snippet)
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(
                NOTIFICATION_ID_BASE + (logEntryId % 10000).toInt(),
                notification
            )
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted; silently skip local notification.
        }
    }
}
