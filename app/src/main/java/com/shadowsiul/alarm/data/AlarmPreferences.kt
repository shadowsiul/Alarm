package com.shadowsiul.alarm.data

import android.content.Context

object AlarmPreferences {
    private const val PREFS = "alarm_prefs"
    private const val KEY_SHOW_UPCOMING = "show_upcoming_notification"

    fun showUpcomingNotification(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_SHOW_UPCOMING, true)
    }

    fun setShowUpcomingNotification(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_UPCOMING, show).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
