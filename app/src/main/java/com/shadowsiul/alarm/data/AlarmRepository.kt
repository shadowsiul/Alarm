package com.shadowsiul.alarm.data

import android.content.Context
import com.shadowsiul.alarm.alarm.AlarmNotifications
import com.shadowsiul.alarm.alarm.AlarmScheduler
import com.shadowsiul.alarm.alarm.AlarmTimes
import com.shadowsiul.alarm.holiday.UsFederalHolidays
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class AlarmRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = AlarmDatabase.get(appContext).alarmDao()
    private val scheduler = AlarmScheduler(appContext)

    fun observeAlarms(): Flow<List<AlarmEntity>> = dao.observeAll()

    suspend fun getAlarm(id: Long): AlarmEntity? = dao.getById(id)

    suspend fun save(alarm: AlarmEntity): Long {
        val cleared = alarm.copy(skipAfterMillis = 0, snoozeUntilMillis = 0)
        val id = dao.upsert(cleared)
        val stored = if (cleared.id == 0L) cleared.copy(id = id) else cleared.copy(id = cleared.id)
        scheduler.schedule(stored)
        scheduler.scheduleHolidayRefresh()
        refreshUpcomingNotification()
        return stored.id
    }

    suspend fun setEnabled(alarm: AlarmEntity, enabled: Boolean) {
        setEnabled(alarm.id, enabled)
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        val alarm = dao.getById(id) ?: return
        val updated = alarm.copy(
            enabled = enabled,
            skipAfterMillis = if (enabled) 0 else alarm.skipAfterMillis,
            snoozeUntilMillis = 0,
        )
        dao.update(updated)
        if (enabled) scheduler.schedule(updated) else scheduler.cancel(updated.id)
        refreshUpcomingNotification()
    }

    suspend fun delete(alarm: AlarmEntity) {
        dao.delete(alarm)
        scheduler.cancel(alarm.id)
        refreshUpcomingNotification()
    }

    suspend fun rescheduleAll() {
        scheduler.scheduleHolidayRefresh()
        dao.getAll().forEach { alarm ->
            if (alarm.enabled) scheduler.schedule(alarm) else scheduler.cancel(alarm.id)
        }
        refreshUpcomingNotification()
    }

    suspend fun disableOneShot(id: Long) {
        val alarm = dao.getById(id) ?: return
        if (alarm.repeatDays == 0) {
            dao.update(alarm.copy(enabled = false, snoozeUntilMillis = 0))
            scheduler.cancel(id)
        } else {
            val updated = alarm.copy(snoozeUntilMillis = 0)
            if (updated != alarm) dao.update(updated)
            scheduler.schedule(updated)
        }
        refreshUpcomingNotification()
    }

    suspend fun skipNext(id: Long) {
        val alarm = dao.getById(id) ?: return
        if (!alarm.enabled) {
            refreshUpcomingNotification()
            return
        }
        val now = System.currentTimeMillis()
        val next = AlarmTimes.nextFire(alarm) ?: run {
            refreshUpcomingNotification()
            return
        }
        val nextMillis = next.toInstant().toEpochMilli()
        if (alarm.snoozeUntilMillis > now && nextMillis == alarm.snoozeUntilMillis) {
            val updated = alarm.copy(snoozeUntilMillis = 0)
            dao.update(updated)
            scheduler.schedule(updated)
            refreshUpcomingNotification()
            return
        }
        if (alarm.repeatDays == 0) {
            dao.update(alarm.copy(enabled = false, snoozeUntilMillis = 0))
            scheduler.cancel(id)
        } else {
            val updated = alarm.copy(skipAfterMillis = nextMillis, snoozeUntilMillis = 0)
            dao.update(updated)
            scheduler.schedule(updated)
        }
        refreshUpcomingNotification()
    }

    suspend fun setSnooze(id: Long, atMillis: Long) {
        val alarm = dao.getById(id) ?: return
        val updated = alarm.copy(snoozeUntilMillis = atMillis)
        dao.update(updated)
        scheduler.schedule(updated)
        refreshUpcomingNotification()
    }

    suspend fun onFired(id: Long): AlarmEntity? {
        val alarm = dao.getById(id) ?: return null
        if (!alarm.enabled) return null
        if (UsFederalHolidays.isHoliday(LocalDate.now()) && !alarm.ringOnHolidays) {
            rescheduleAll()
            return null
        }
        val cleared = alarm.copy(snoozeUntilMillis = 0)
        if (cleared != alarm) dao.update(cleared)
        scheduler.schedule(cleared)
        return cleared
    }

    suspend fun refreshUpcomingNotification() {
        AlarmNotifications.refreshUpcoming(appContext, dao.getAll())
    }
}
