package com.shadowsiul.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shadowsiul.alarm.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_FIRE) return
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (id < 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = AlarmRepository(context)
                val alarm = repo.onFired(id) ?: return@launch
                AlarmNotifications.ensureChannel(context)
                try {
                    AlarmNotifications.ringing = true
                    AlarmNotifications.cancelUpcoming(context)
                    context.startForegroundService(
                        Intent(context, AlarmRingtoneService::class.java).apply {
                            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
                        },
                    )
                } catch (_: Exception) {
                    AlarmNotifications.ringing = false
                    repo.refreshUpcomingNotification()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
