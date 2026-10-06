package com.shadowsiul.alarm.holiday

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.TemporalAdjusters

/**
 * Nationwide US federal holidays observed by OPM for federal employees.
 * Fixed-date holidays that fall on Saturday are observed the Friday before;
 * those that fall on Sunday are observed the Monday after.
 */
object UsFederalHolidays {

    data class Holiday(val date: LocalDate, val id: HolidayId)

    fun holidayOn(date: LocalDate): Holiday? {
        return holidaysForYear(date.year).find { it.date == date }
            ?: holidaysForYear(date.year - 1).find { it.date == date }
            ?: holidaysForYear(date.year + 1).find { it.date == date }
    }

    fun isHoliday(date: LocalDate): Boolean = holidayOn(date) != null

    fun nextNonHoliday(date: LocalDate): LocalDate {
        var cursor = date
        while (isHoliday(cursor)) {
            cursor = cursor.plusDays(1)
        }
        return cursor
    }

    fun holidaysForYear(year: Int): List<Holiday> {
        val observed = mutableListOf<Holiday>()

        fun addFixed(month: Month, day: Int, id: HolidayId) {
            val actual = LocalDate.of(year, month, day)
            observed += Holiday(observedDate(actual), id)
        }

        addFixed(Month.JANUARY, 1, HolidayId.NEW_YEAR)
        observed += Holiday(nthWeekday(year, Month.JANUARY, DayOfWeek.MONDAY, 3), HolidayId.MLK)
        observed += Holiday(nthWeekday(year, Month.FEBRUARY, DayOfWeek.MONDAY, 3), HolidayId.WASHINGTON)
        observed += Holiday(lastWeekday(year, Month.MAY, DayOfWeek.MONDAY), HolidayId.MEMORIAL)
        addFixed(Month.JUNE, 19, HolidayId.JUNETEENTH)
        addFixed(Month.JULY, 4, HolidayId.INDEPENDENCE)
        observed += Holiday(nthWeekday(year, Month.SEPTEMBER, DayOfWeek.MONDAY, 1), HolidayId.LABOR)
        observed += Holiday(nthWeekday(year, Month.OCTOBER, DayOfWeek.MONDAY, 2), HolidayId.COLUMBUS)
        addFixed(Month.NOVEMBER, 11, HolidayId.VETERANS)
        observed += Holiday(nthWeekday(year, Month.NOVEMBER, DayOfWeek.THURSDAY, 4), HolidayId.THANKSGIVING)
        addFixed(Month.DECEMBER, 25, HolidayId.CHRISTMAS)

        return observed.distinctBy { it.date to it.id }.sortedBy { it.date }
    }

    private fun observedDate(actual: LocalDate): LocalDate {
        return when (actual.dayOfWeek) {
            DayOfWeek.SATURDAY -> actual.minusDays(1)
            DayOfWeek.SUNDAY -> actual.plusDays(1)
            else -> actual
        }
    }

    private fun nthWeekday(year: Int, month: Month, dayOfWeek: DayOfWeek, n: Int): LocalDate {
        return LocalDate.of(year, month, 1)
            .with(TemporalAdjusters.dayOfWeekInMonth(n, dayOfWeek))
    }

    private fun lastWeekday(year: Int, month: Month, dayOfWeek: DayOfWeek): LocalDate {
        return LocalDate.of(year, month, 1)
            .with(TemporalAdjusters.lastInMonth(dayOfWeek))
    }
}
