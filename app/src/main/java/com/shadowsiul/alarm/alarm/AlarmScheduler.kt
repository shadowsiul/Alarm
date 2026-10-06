package com.shadowsiul.alarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.shadowsiul.alarm.data.AlarmEntity
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: AlarmEntity) {
        if (!alarm.enabled) {
            cancel(alarm.id)
            return
        }
        val triggerAt = nextFireMillis(alarm) ?: run {
            cancel(alarm.id)
            return
        }
        val show = pendingShowIntent()
        val operation = pendingAlarmIntent(alarm.id)
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, show), operation)
    }

    fun cancel(alarmId: Long) {
        alarmManager.cancel(pendingAlarmIntent(alarmId))
    }

    fun scheduleHolidayRefresh() {
        val now = ZonedDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone).plusMinutes(1)
        val intent = Intent(context, HolidayRefreshReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            HOLIDAY_REFRESH_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextMidnight.toInstant().toEpochMilli(),
            pending,
        )
    }

    fun nextFireMillis(alarm: AlarmEntity, from: ZonedDateTime = ZonedDateTime.now()): Long? {
        return AlarmTimes.nextFire(alarm, from)?.toInstant()?.toEpochMilli()
    }

    private fun pendingAlarmIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pendingShowIntent(): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, com.shadowsiul.alarm.ui.MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            SHOW_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_FIRE = "com.shadowsiul.alarm.FIRE"
        const val EXTRA_ALARM_ID = "alarm_id"
        private const val SHOW_REQUEST = 9001
        private const val HOLIDAY_REFRESH_REQUEST = 9002
    }
}
