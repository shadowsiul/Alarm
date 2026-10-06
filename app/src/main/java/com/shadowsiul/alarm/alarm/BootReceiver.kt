package com.shadowsiul.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shadowsiul.alarm.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmRepository(context).rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
