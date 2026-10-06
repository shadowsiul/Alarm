package com.shadowsiul.alarm.holiday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UsFederalHolidaysTest {

    @Test
    fun thanksgivingIsFourthThursday() {
        val holiday = UsFederalHolidays.holidayOn(LocalDate.of(2026, 11, 26))
        assertEquals(HolidayId.THANKSGIVING, holiday?.id)
    }

    @Test
    fun independenceDayObservedFridayWhenSaturday() {
        // July 4, 2026 is a Saturday → observed Friday July 3
        assertTrue(UsFederalHolidays.isHoliday(LocalDate.of(2026, 7, 3)))
        assertFalse(UsFederalHolidays.isHoliday(LocalDate.of(2026, 7, 4)))
    }

    @Test
    fun memorialDayIsLastMondayInMay() {
        assertEquals(
            HolidayId.MEMORIAL,
            UsFederalHolidays.holidayOn(LocalDate.of(2026, 5, 25))?.id,
        )
    }

    @Test
    fun nextNonHolidaySkipsHoliday() {
        val next = UsFederalHolidays.nextNonHoliday(LocalDate.of(2026, 12, 25))
        assertEquals(LocalDate.of(2026, 12, 26), next)
    }
}
