package com.shadowsiul.alarm.alarm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.shadowsiul.alarm.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowsiul.alarm.data.AlarmEntity
import com.shadowsiul.alarm.data.AlarmRepository
import com.shadowsiul.alarm.ui.theme.AlarmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AlarmRingingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)

        setContent {
            AlarmTheme {
                var alarm by remember { mutableStateOf<AlarmEntity?>(null) }
                LaunchedEffect(alarmId) {
                    alarm = withContext(Dispatchers.IO) {
                        AlarmRepository(this@AlarmRingingActivity).getAlarm(alarmId)
                    }
                }
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = alarm?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "--:--",
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Light,
                        )
                        Text(
                            text = alarm?.label?.ifBlank { stringResource(R.string.alarm_fallback) }
                                ?: stringResource(R.string.alarm_fallback),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(48.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                startService(
                                    Intent(this@AlarmRingingActivity, AlarmRingtoneService::class.java).apply {
                                        action = AlarmRingtoneService.ACTION_DISMISS
                                        putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                                    },
                                )
                                finish()
                            },
                        ) {
                            Text(stringResource(R.string.dismiss))
                        }
                        Spacer(Modifier.height(12.dp))
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                startService(
                                    Intent(this@AlarmRingingActivity, AlarmRingtoneService::class.java).apply {
                                        action = AlarmRingtoneService.ACTION_SNOOZE
                                        putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                                    },
                                )
                                finish()
                            },
                        ) {
                            Text(stringResource(R.string.snooze_10))
                        }
                    }
                }
            }
        }
    }
}
