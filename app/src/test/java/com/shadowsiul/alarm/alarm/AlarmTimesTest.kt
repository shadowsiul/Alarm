package com.shadowsiul.alarm.alarm

import com.shadowsiul.alarm.data.AlarmEntity
import com.shadowsiul.alarm.data.dayBit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar

class AlarmTimesTest {

    private val zone = ZoneId.of("America/New_York")

    @Test
    fun dailyAlarmBeforeTimeIsToday() {
        val from = ZonedDateTime.of(2026, 4, 15, 6, 0, 0, 0, zone)
        val next = AlarmTimes.nextTrigger(daily(hour = 7), from)
        assertEquals(ZonedDateTime.of(2026, 4, 15, 7, 0, 0, 0, zone), next)
    }

    @Test
    fun dailyAlarmAfterTimeIsTomorrow() {
        val from = ZonedDateTime.of(2026, 4, 15, 8, 0, 0, 0, zone)
        val next = AlarmTimes.nextTrigger(daily(hour = 7), from)
        assertEquals(ZonedDateTime.of(2026, 4, 16, 7, 0, 0, 0, zone), next)
    }

    @Test
    fun skipThisTimeMovesRepeatingAlarmToNextDay() {
        val from = ZonedDateTime.of(2026, 4, 15, 6, 50, 0, 0, zone)
        val todayFire = ZonedDateTime.of(2026, 4, 15, 7, 0, 0, 0, zone)
        val alarm = daily(hour = 7).copy(skipAfterMillis = todayFire.toInstant().toEpochMilli())
        val next = AlarmTimes.nextTrigger(alarm, from)
        assertEquals(ZonedDateTime.of(2026, 4, 16, 7, 0, 0, 0, zone), next)
    }

    @Test
    fun snoozeWinsWhenSoonerThanRegular() {
        val from = ZonedDateTime.of(2026, 4, 15, 7, 1, 0, 0, zone)
        val snooze = ZonedDateTime.of(2026, 4, 15, 7, 10, 0, 0, zone)
        val alarm = daily(hour = 7).copy(snoozeUntilMillis = snooze.toInstant().toEpochMilli())
        val next = AlarmTimes.nextFire(alarm, from)
        assertEquals(snooze, next)
    }

    @Test
    fun nextAmongPicksSoonestAlarm() {
        val from = ZonedDateTime.of(2026, 4, 15, 6, 0, 0, 0, zone)
        val later = daily(id = 1, hour = 8)
        val sooner = daily(id = 2, hour = 7)
        val next = AlarmTimes.nextAmong(listOf(later, sooner), from)
        assertEquals(2L, next?.first?.id)
        assertEquals(ZonedDateTime.of(2026, 4, 15, 7, 0, 0, 0, zone), next?.second)
    }

    @Test
    fun disabledAlarmHasNoTrigger() {
        val from = ZonedDateTime.of(2026, 4, 15, 6, 0, 0, 0, zone)
        assertNull(AlarmTimes.nextTrigger(daily(hour = 7).copy(enabled = false), from))
    }

    @Test
    fun weekdayAlarmSkipsObservedHoliday() {
        // Friday July 3, 2026 is the observed Independence Day.
        val from = ZonedDateTime.of(2026, 7, 3, 6, 0, 0, 0, zone)
        val weekdays = (Calendar.MONDAY..Calendar.FRIDAY).fold(0) { acc, day -> acc or dayBit(day) }
        val alarm = AlarmEntity(id = 1, hour = 7, minute = 0, repeatDays = weekdays)
        val next = AlarmTimes.nextTrigger(alarm, from)
        assertEquals(ZonedDateTime.of(2026, 7, 6, 7, 0, 0, 0, zone), next)
    }

    private fun daily(id: Long = 1, hour: Int): AlarmEntity {
        val everyDay = (Calendar.SUNDAY..Calendar.SATURDAY).fold(0) { acc, day -> acc or dayBit(day) }
        return AlarmEntity(id = id, hour = hour, minute = 0, repeatDays = everyDay)
    }
}
