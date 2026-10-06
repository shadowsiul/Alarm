package com.shadowsiul.alarm

import android.app.Application
import com.shadowsiul.alarm.alarm.AlarmNotifications
import com.shadowsiul.alarm.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmApp : Application() {
    lateinit var repository: AlarmRepository
        private set

    override fun onCreate() {
        super.onCreate()
        AlarmNotifications.ensureChannel(this)
        repository = AlarmRepository(this)
        CoroutineScope(Dispatchers.IO).launch {
            repository.restoreFromBackupIfEmpty()
            repository.rescheduleAll()
        }
    }
}
