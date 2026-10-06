package com.shadowsiul.alarm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmBackupTest {

    @Test
    fun roundTripKeepsAlarmFields() {
        val original = listOf(
            AlarmEntity(
                hour = 7,
                minute = 15,
                label = "Wake up \"early\"",
                enabled = true,
                repeatDays = 31,
                soundUri = "content://media/external/audio/1",
                soundName = "Bell",
                vibrate = false,
                ringOnHolidays = true,
            ),
            AlarmEntity(
                hour = 22,
                minute = 30,
                label = "Bed",
                soundUri = null,
            ),
        )
        val restored = AlarmBackup.decode(AlarmBackup.encode(original))
        assertEquals(2, restored.size)
        assertEquals(7, restored[0].hour)
        assertEquals(15, restored[0].minute)
        assertEquals("Wake up \"early\"", restored[0].label)
        assertEquals(31, restored[0].repeatDays)
        assertEquals("content://media/external/audio/1", restored[0].soundUri)
        assertEquals(false, restored[0].vibrate)
        assertEquals(true, restored[0].ringOnHolidays)
        assertEquals(22, restored[1].hour)
        assertNull(restored[1].soundUri)
    }
}
