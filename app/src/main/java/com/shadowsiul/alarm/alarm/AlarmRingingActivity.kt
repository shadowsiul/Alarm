package com.shadowsiul.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.shadowsiul.alarm.R
import com.shadowsiul.alarm.data.AlarmEntity
import com.shadowsiul.alarm.data.AlarmRepository
import com.shadowsiul.alarm.ui.theme.AlarmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AlarmRingingActivity : ComponentActivity() {
    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    private var promptMath by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        promptMath = intent.getBooleanExtra(AlarmRingtoneService.EXTRA_REQUIRE_MATH, false)
        ContextCompat.registerReceiver(
            this,
            stopReceiver,
            IntentFilter(AlarmRingtoneService.ACTION_STOP_RINGING_UI),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)

        setContent {
            AlarmTheme {
                var alarm by remember { mutableStateOf<AlarmEntity?>(null) }
                var answer by remember { mutableStateOf("") }
                var wrong by remember { mutableStateOf(false) }
                var problem by remember { mutableStateOf(SnoozeMath.generate()) }
                LaunchedEffect(alarmId) {
                    alarm = withContext(Dispatchers.IO) {
                        AlarmRepository(this@AlarmRingingActivity).getAlarm(alarmId)
                    }
                }
                val snoozeMinutes = when (alarm?.snoozeMinutes) {
                    5, 15 -> alarm?.snoozeMinutes ?: 10
                    else -> 10
                }
                val needsMath = promptMath || (alarm?.snoozeCount ?: 0) >= 1
                fun snoozeNow() {
                    startService(
                        Intent(this@AlarmRingingActivity, AlarmRingtoneService::class.java).apply {
                            action = AlarmRingtoneService.ACTION_SNOOZE
                            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                            putExtra(AlarmRingtoneService.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                            putExtra(AlarmRingtoneService.EXTRA_SNOOZE_CONFIRMED, true)
                        },
                    )
                    finish()
                }
                fun trySnooze() {
                    if (needsMath) {
                        promptMath = true
                    } else {
                        snoozeNow()
                    }
                }
                fun submitAnswer() {
                    val entered = answer.trim().toIntOrNull()
                    if (entered == problem.answer) {
                        snoozeNow()
                    } else {
                        wrong = true
                        answer = ""
                        problem = SnoozeMath.generate()
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
                        if (promptMath) {
                            Text(
                                stringResource(R.string.snooze_math_prompt, problem.prompt),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = answer,
                                onValueChange = {
                                    answer = it.filter { ch -> ch.isDigit() || ch == '-' }.take(4)
                                    wrong = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = wrong,
                                label = { Text(stringResource(R.string.snooze_math_answer)) },
                                supportingText = if (wrong) {
                                    { Text(stringResource(R.string.snooze_math_wrong)) }
                                } else {
                                    null
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions = KeyboardActions(onDone = { submitAnswer() }),
                            )
                            Spacer(Modifier.height(12.dp))
                            FilledTonalButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { submitAnswer() },
                            ) {
                                Text(stringResource(R.string.snooze_math_submit))
                            }
                        } else {
                            FilledTonalButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { trySnooze() },
                            ) {
                                Text(
                                    stringResource(
                                        when (snoozeMinutes) {
                                            5 -> R.string.snooze_5
                                            15 -> R.string.snooze_15
                                            else -> R.string.snooze_10
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        promptMath = intent.getBooleanExtra(AlarmRingtoneService.EXTRA_REQUIRE_MATH, promptMath)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(stopReceiver) }
        super.onDestroy()
    }
}
