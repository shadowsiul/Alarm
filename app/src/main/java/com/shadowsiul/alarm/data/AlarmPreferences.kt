package com.shadowsiul.alarm.data

import android.content.Context
import java.time.LocalDate

object AlarmPreferences {
    private const val PREFS = "alarm_prefs"
    private const val KEY_SHOW_UPCOMING = "show_upcoming_notification"
    private const val KEY_HIDDEN_ID = "dismissed_upcoming_id"
    private const val KEY_HIDDEN_DAY = "dismissed_upcoming_day"

    fun showUpcomingNotification(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_SHOW_UPCOMING, true)
    }

    fun setShowUpcomingNotification(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_UPCOMING, show).commit()
    }

    fun setDismissedUpcoming(context: Context, alarmId: Long) {
        prefs(context).edit()
            .putLong(KEY_HIDDEN_ID, alarmId)
            .putLong(KEY_HIDDEN_DAY, LocalDate.now().toEpochDay())
            .commit()
    }

    fun clearDismissedUpcoming(context: Context) {
        prefs(context).edit()
            .remove(KEY_HIDDEN_ID)
            .remove(KEY_HIDDEN_DAY)
            .commit()
    }

    fun shouldHideUpcoming(context: Context, alarmId: Long): Boolean {
        val stored = prefs(context)
        val hiddenDay = stored.getLong(KEY_HIDDEN_DAY, -1L)
        if (hiddenDay >= 0 && LocalDate.now().toEpochDay() > hiddenDay) {
            clearDismissedUpcoming(context)
            return false
        }
        return stored.getLong(KEY_HIDDEN_ID, -1L) == alarmId
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
