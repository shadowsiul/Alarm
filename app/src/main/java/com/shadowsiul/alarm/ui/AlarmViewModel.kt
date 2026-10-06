package com.shadowsiul.alarm.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shadowsiul.alarm.AlarmApp
import com.shadowsiul.alarm.data.AlarmEntity
import com.shadowsiul.alarm.holiday.UsFederalHolidays
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as AlarmApp).repository

    val alarms: StateFlow<List<AlarmEntity>> = repo.observeAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayHoliday = UsFederalHolidays.holidayOn(LocalDate.now())

    fun save(alarm: AlarmEntity) {
        viewModelScope.launch { repo.save(alarm) }
    }

    fun delete(alarm: AlarmEntity) {
        viewModelScope.launch { repo.delete(alarm) }
    }

    fun setEnabled(alarm: AlarmEntity, enabled: Boolean) {
        viewModelScope.launch { repo.setEnabled(alarm, enabled) }
    }

    suspend fun alarmById(id: Long): AlarmEntity? = repo.getAlarm(id)
}
