package com.shadowsiul.alarm.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shadowsiul.alarm.R
import com.shadowsiul.alarm.data.AlarmEntity
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    viewModel: AlarmViewModel,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val holiday = viewModel.todayHoliday

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_alarms)) },
                actions = {
                    IconButton(onClick = onOptions) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = stringResource(R.string.tab_options),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_alarm))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (holiday != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.silenced_today),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(
                                R.string.holiday_banner_body,
                                stringResource(holiday.id.titleRes),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            if (alarms.isEmpty()) {
                Text(
                    stringResource(R.string.empty_alarms),
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmRow(
                            alarm = alarm,
                            silencedToday = holiday != null,
                            onToggle = { viewModel.setEnabled(alarm, it) },
                            onClick = { onEdit(alarm.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: AlarmEntity,
    silencedToday: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "%02d:%02d".format(alarm.hour, alarm.minute),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light,
                )
                if (alarm.label.isNotBlank()) {
                    Text(alarm.label, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    repeatLabel(
                        alarm = alarm,
                        oneTime = stringResource(R.string.repeat_one_time),
                        everyDay = stringResource(R.string.repeat_every_day),
                        shortDays = listOf(
                            Calendar.SUNDAY to stringResource(R.string.day_sun),
                            Calendar.MONDAY to stringResource(R.string.day_mon),
                            Calendar.TUESDAY to stringResource(R.string.day_tue),
                            Calendar.WEDNESDAY to stringResource(R.string.day_wed),
                            Calendar.THURSDAY to stringResource(R.string.day_thu),
                            Calendar.FRIDAY to stringResource(R.string.day_fri),
                            Calendar.SATURDAY to stringResource(R.string.day_sat),
                        ),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    alarm.soundName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (silencedToday && alarm.enabled && alarm.ringOnHolidays) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.holiday_override),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                } else if (silencedToday && alarm.enabled) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.holiday_silence),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
        }
    }
}

fun repeatLabel(
    alarm: AlarmEntity,
    oneTime: String,
    everyDay: String,
    shortDays: List<Pair<Int, String>>,
): String {
    if (alarm.repeatDays == 0) return oneTime
    val selected = shortDays.filter { alarm.repeatsOn(it.first) }.map { it.second }
    return if (selected.size == 7) everyDay else selected.joinToString(" ")
}
