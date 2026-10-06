package com.shadowsiul.alarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shadowsiul.alarm.R
import com.shadowsiul.alarm.data.AlarmEntity
import com.shadowsiul.alarm.data.AlarmPreferences
import com.shadowsiul.alarm.ui.MainActivity
import java.util.Date

object AlarmNotifications {
    const val CHANNEL_ID = "alarms"
    const val UPCOMING_CHANNEL_ID = "upcoming"
    const val RINGING_NOTIFICATION_ID = 42
    const val UPCOMING_NOTIFICATION_ID = 41

    @Volatile
    var ringing: Boolean = false

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_alarm_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_alarm_desc)
                setSound(null, null)
                enableVibration(true)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                UPCOMING_CHANNEL_ID,
                context.getString(R.string.channel_upcoming_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.channel_upcoming_desc)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            },
        )
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

    fun refreshUpcoming(context: Context, alarms: List<AlarmEntity>) {
        ensureChannel(context)
        if (ringing || !AlarmPreferences.showUpcomingNotification(context)) {
            cancelUpcoming(context)
            return
        }
        val next = AlarmTimes.nextAmong(alarms) ?: run {
            cancelUpcoming(context)
            return
        }
        val (alarm, trigger) = next
        val triggerAt = trigger.toInstant().toEpochMilli()
        if (AlarmPreferences.shouldHideUpcoming(context, alarm.id)) {
            cancelUpcoming(context)
            return
        }
        val timeText = DateFormat.getTimeFormat(context).format(Date(triggerAt))
        val title = context.getString(R.string.upcoming_title, timeText)
        val text = alarm.label.ifBlank { context.getString(R.string.upcoming_fallback) }
        val content = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismiss = PendingIntent.getBroadcast(
            context,
            3000 + alarm.id.toInt(),
            Intent(context, UpcomingAlarmReceiver::class.java).apply {
                action = UpcomingAlarmReceiver.ACTION_DISMISS
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val turnOff = PendingIntent.getBroadcast(
            context,
            4000 + alarm.id.toInt(),
            Intent(context, UpcomingAlarmReceiver::class.java).apply {
                action = UpcomingAlarmReceiver.ACTION_TURN_OFF
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, UPCOMING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(timeText)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOngoing(false)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(true)
            .setWhen(triggerAt)
            .setContentIntent(content)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, context.getString(R.string.dismiss), dismiss)
            .addAction(0, context.getString(R.string.turn_off), turnOff)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(UPCOMING_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted yet.
        }
    }

    fun cancelUpcoming(context: Context) {
        val appContext = context.applicationContext
        NotificationManagerCompat.from(appContext).cancel(UPCOMING_NOTIFICATION_ID)
        appContext.getSystemService(NotificationManager::class.java)
            .cancel(UPCOMING_NOTIFICATION_ID)
    }
}
