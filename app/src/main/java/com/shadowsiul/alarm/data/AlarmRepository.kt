package com.shadowsiul.alarm.data

import android.content.Context
import com.shadowsiul.alarm.alarm.AlarmScheduler
import kotlinx.coroutines.flow.Flow

class AlarmRepository(context: Context) {
    private val dao = AlarmDatabase.get(context).alarmDao()
    private val scheduler = AlarmScheduler(context.applicationContext)

    fun observeAlarms(): Flow<List<AlarmEntity>> = dao.observeAll()

    suspend fun getAlarm(id: Long): AlarmEntity? = dao.getById(id)

    suspend fun save(alarm: AlarmEntity): Long {
        val id = dao.upsert(alarm)
        val stored = if (alarm.id == 0L) alarm.copy(id = id) else alarm.copy(id = alarm.id)
        scheduler.schedule(stored)
        scheduler.scheduleHolidayRefresh()
        return stored.id
    }

    suspend fun setEnabled(alarm: AlarmEntity, enabled: Boolean) {
        val updated = alarm.copy(enabled = enabled)
        dao.update(updated)
        if (enabled) scheduler.schedule(updated) else scheduler.cancel(updated.id)
    }

    suspend fun delete(alarm: AlarmEntity) {
        dao.delete(alarm)
        scheduler.cancel(alarm.id)
    }

    suspend fun rescheduleAll() {
        scheduler.scheduleHolidayRefresh()
        dao.getAll().forEach { alarm ->
            if (alarm.enabled) scheduler.schedule(alarm) else scheduler.cancel(alarm.id)
        }
    }

    suspend fun disableOneShot(id: Long) {
        val alarm = dao.getById(id) ?: return
        if (alarm.repeatDays == 0) {
            dao.update(alarm.copy(enabled = false))
            scheduler.cancel(id)
        } else {
            scheduler.schedule(alarm)
        }
    }
}
