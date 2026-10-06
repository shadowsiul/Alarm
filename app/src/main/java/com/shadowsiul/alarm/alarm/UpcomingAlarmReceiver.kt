package com.shadowsiul.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shadowsiul.alarm.data.AlarmPreferences
import com.shadowsiul.alarm.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpcomingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (id >= 0) {
            AlarmPreferences.setDismissedUpcoming(context, id)
        }
        AlarmNotifications.cancelUpcoming(context)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (id < 0) return@launch
                val repo = AlarmRepository(context)
                when (intent.action) {
                    ACTION_DISMISS -> repo.skipNext(id)
                    ACTION_TURN_OFF -> repo.setEnabled(id, false)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.shadowsiul.alarm.DISMISS_UPCOMING"
        const val ACTION_TURN_OFF = "com.shadowsiul.alarm.TURN_OFF"
    }
}
