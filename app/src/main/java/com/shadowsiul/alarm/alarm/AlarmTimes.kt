package com.shadowsiul.alarm.alarm

import com.shadowsiul.alarm.data.AlarmEntity
import com.shadowsiul.alarm.holiday.UsFederalHolidays
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZonedDateTime
import java.util.Calendar

object AlarmTimes {

    fun nextTrigger(alarm: AlarmEntity, from: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
        if (!alarm.enabled) return null
        var start = from
        if (alarm.skipAfterMillis > 0) {
            val skipFrom = Instant.ofEpochMilli(alarm.skipAfterMillis).atZone(from.zone)
            if (!skipFrom.isBefore(start)) {
                start = skipFrom
            }
        }
        var candidate = start.withHour(alarm.hour).withMinute(alarm.minute).withSecond(0).withNano(0)
        if (!candidate.isAfter(start)) {
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

    fun nextFire(alarm: AlarmEntity, from: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
        val regular = nextTrigger(alarm, from)
        val snooze = alarm.snoozeUntilMillis.takeIf { it > from.toInstant().toEpochMilli() }
            ?.let { Instant.ofEpochMilli(it).atZone(from.zone) }
        return listOfNotNull(regular, snooze).minOrNull()
    }

    fun nextAmong(alarms: List<AlarmEntity>, from: ZonedDateTime = ZonedDateTime.now()): Pair<AlarmEntity, ZonedDateTime>? {
        return alarms.mapNotNull { alarm ->
            nextFire(alarm, from)?.let { alarm to it }
        }.minByOrNull { it.second }
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
