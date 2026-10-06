package com.shadowsiul.alarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
    /** Bitmask of Calendar.SUNDAY (1) through Calendar.SATURDAY (64). Zero means one-time. */
    val repeatDays: Int = 0,
    val soundUri: String? = null,
    val soundName: String = "Default",
    val vibrate: Boolean = true,
    val ringOnHolidays: Boolean = false,
) {
    fun repeatsOn(calendarDay: Int): Boolean {
        val bit = 1 shl (calendarDay - 1)
        return repeatDays and bit != 0
    }
}

fun dayBit(calendarDay: Int): Int = 1 shl (calendarDay - 1)
