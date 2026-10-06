package com.shadowsiul.alarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.shadowsiul.alarm.data.AlarmEntity
import com.shadowsiul.alarm.holiday.UsFederalHolidays
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: AlarmEntity) {
        if (!alarm.enabled) {
            cancel(alarm.id)
            return
        }
        val triggerAt = nextTriggerMillis(alarm) ?: run {
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
        var nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone).plusMinutes(1)
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

    fun nextTriggerMillis(alarm: AlarmEntity, from: ZonedDateTime = ZonedDateTime.now()): Long? {
        return nextTrigger(alarm, from)?.toInstant()?.toEpochMilli()
    }

    fun nextTrigger(alarm: AlarmEntity, from: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
        if (!alarm.enabled) return null
        var candidate = from.withHour(alarm.hour).withMinute(alarm.minute).withSecond(0).withNano(0)
        if (!candidate.isAfter(from)) {
            candidate = candidate.plusDays(1)
        }
        repeat(16) {
            val date = candidate.toLocalDate()
            val matchesRepeat = alarm.repeatDays == 0 || alarm.repeatsOnJavaDay(candidate.dayOfWeek)
            if (matchesRepeat && (alarm.ringOnHolidays || !UsFederalHolidays.isHoliday(date))) {
                return candidate
            }
            candidate = candidate.plusDays(1)
                .withHour(alarm.hour)
                .withMinute(alarm.minute)
                .withSecond(0)
                .withNano(0)
        }
        return null
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

private fun AlarmEntity.repeatsOnJavaDay(dayOfWeek: DayOfWeek): Boolean {
    val calendarDay = when (dayOfWeek) {
        DayOfWeek.SUNDAY -> Calendar.SUNDAY
        DayOfWeek.MONDAY -> Calendar.MONDAY
        DayOfWeek.TUESDAY -> Calendar.TUESDAY
        DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
        DayOfWeek.THURSDAY -> Calendar.THURSDAY
        DayOfWeek.FRIDAY -> Calendar.FRIDAY
        DayOfWeek.SATURDAY -> Calendar.SATURDAY
    }
    return repeatsOn(calendarDay)
}
