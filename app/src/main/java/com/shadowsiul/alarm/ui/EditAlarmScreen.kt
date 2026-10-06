package com.shadowsiul.alarm.ui

import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shadowsiul.alarm.R
import com.shadowsiul.alarm.data.AlarmEntity
import com.shadowsiul.alarm.data.dayBit
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditAlarmScreen(
    alarmId: Long,
    viewModel: AlarmViewModel,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var label by remember { mutableStateOf("") }
    var repeatDays by remember { mutableIntStateOf(0) }
    var vibrate by remember { mutableStateOf(true) }
    var ringOnHolidays by remember { mutableStateOf(false) }
    var soundUri by remember { mutableStateOf<String?>(null) }
    var soundName by remember { mutableStateOf("") }
    val defaultSoundName = stringResource(R.string.sound_default)
    val now = LocalTime.now()
    val timeState = rememberTimePickerState(initialHour = now.hour, initialMinute = now.minute, is24Hour = false)
    var existing by remember { mutableStateOf<AlarmEntity?>(null) }

    LaunchedEffect(alarmId, defaultSoundName) {
        if (alarmId == 0L && soundName.isEmpty()) {
            soundName = defaultSoundName
        }
        if (alarmId != 0L) {
            val alarm = viewModel.alarmById(alarmId)
            if (alarm != null) {
                existing = alarm
                label = alarm.label
                repeatDays = alarm.repeatDays
                vibrate = alarm.vibrate
                ringOnHolidays = alarm.ringOnHolidays
                soundUri = alarm.soundUri
                soundName = alarm.soundName
                timeState.hour = alarm.hour
                timeState.minute = alarm.minute
            }
        }
    }

    val picker = rememberLauncherForActivityResult(PickRingtone()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        soundUri = uri.toString()
        soundName = ringtoneTitle(context, uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (alarmId == 0L) R.string.new_alarm else R.string.edit_alarm)) },
                navigationIcon = { TextButton(onClick = onDone) { Text(stringResource(R.string.close)) } },
                actions = {
                    if (existing != null) {
                        TextButton(onClick = {
                            existing?.let { viewModel.delete(it) }
                            onDone()
                        }) { Text(stringResource(R.string.delete)) }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            TimePicker(state = timeState, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.repeat))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val days = listOf(
                    Calendar.SUNDAY to stringResource(R.string.day_sun_short),
                    Calendar.MONDAY to stringResource(R.string.day_mon_short),
                    Calendar.TUESDAY to stringResource(R.string.day_tue_short),
                    Calendar.WEDNESDAY to stringResource(R.string.day_wed_short),
                    Calendar.THURSDAY to stringResource(R.string.day_thu_short),
                    Calendar.FRIDAY to stringResource(R.string.day_fri_short),
                    Calendar.SATURDAY to stringResource(R.string.day_sat_short),
                )
                days.forEach { (day, letter) ->
                    val bit = dayBit(day)
                    val selected = repeatDays and bit != 0
                    FilterChip(
                        selected = selected,
                        onClick = {
                            repeatDays = if (selected) repeatDays and bit.inv() else repeatDays or bit
                        },
                        label = { Text(letter) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sound, soundName), modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = {
                        picker.launch(
                            soundUri?.let(Uri::parse)
                                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                        )
                    },
                ) {
                    Text(stringResource(R.string.choose_sound))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.vibrate), modifier = Modifier.weight(1f))
                Switch(checked = vibrate, onCheckedChange = { vibrate = it })
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(stringResource(R.string.ring_on_holidays))
                    Text(
                        stringResource(R.string.ring_on_holidays_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = ringOnHolidays, onCheckedChange = { ringOnHolidays = it })
            }
            Spacer(Modifier.height(24.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        viewModel.save(
                            AlarmEntity(
                                id = existing?.id ?: 0,
                                hour = timeState.hour,
                                minute = timeState.minute,
                                label = label.trim(),
                                enabled = true,
                                repeatDays = repeatDays,
                                soundUri = soundUri,
                                soundName = soundName,
                                vibrate = vibrate,
                                ringOnHolidays = ringOnHolidays,
                            ),
                        )
                        onDone()
                    }
                },
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
