package com.shadowsiul.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shadowsiul.alarm.data.AlarmRepository
import com.shadowsiul.alarm.holiday.UsFederalHolidays
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_FIRE) return
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (id < 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = AlarmRepository(context)
                val alarm = repo.getAlarm(id) ?: return@launch
                if (!alarm.enabled) return@launch

                if (UsFederalHolidays.isHoliday(LocalDate.now()) && !alarm.ringOnHolidays) {
                    repo.rescheduleAll()
                    return@launch
                }

                AlarmNotifications.ensureChannel(context)
                context.startForegroundService(
                    Intent(context, AlarmRingtoneService::class.java).apply {
                        putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)
                    },
                )
                AlarmScheduler(context).schedule(alarm)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
