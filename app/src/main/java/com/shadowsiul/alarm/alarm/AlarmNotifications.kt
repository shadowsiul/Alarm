package com.shadowsiul.alarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.shadowsiul.alarm.R
import com.shadowsiul.alarm.ui.MainActivity

object AlarmNotifications {
    const val CHANNEL_ID = "alarms"
    const val RINGING_NOTIFICATION_ID = 42

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_alarm_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_alarm_desc)
            setSound(null, null)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun ringingNotification(context: Context, alarmId: Long, title: String, text: String): Notification {
        val fullScreen = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            Intent(context, AlarmRingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val content = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismiss = PendingIntent.getService(
            context,
            1000 + alarmId.toInt(),
            Intent(context, AlarmRingtoneService::class.java).apply {
                action = AlarmRingtoneService.ACTION_DISMISS
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snooze = PendingIntent.getService(
            context,
            2000 + alarmId.toInt(),
            Intent(context, AlarmRingtoneService::class.java).apply {
                action = AlarmRingtoneService.ACTION_SNOOZE
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setContentIntent(content)
            .setFullScreenIntent(fullScreen, true)
            .addAction(0, context.getString(R.string.snooze), snooze)
            .addAction(0, context.getString(R.string.dismiss), dismiss)
            .build()
    }
}
