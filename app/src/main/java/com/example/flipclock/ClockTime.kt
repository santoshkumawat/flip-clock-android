package com.example.flipclock

import android.content.Context
import android.text.format.DateFormat
import java.util.Calendar

data class ClockDisplay(val digits: String, val period: String?, val seconds: Int)

object ClockTime {
    private const val PREFS = "flip_clock_preferences"
    private const val KEY_24_HOUR = "use_24_hour"
    private const val KEY_SHOW_SECONDS = "show_seconds"
    private const val KEY_DESIGN = "clock_design"

    fun uses24Hour(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_24_HOUR, DateFormat.is24HourFormat(context))

    fun setUses24Hour(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_24_HOUR, value).apply()
    }

    fun showsSeconds(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_SECONDS, false)

    fun setShowsSeconds(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_SECONDS, value).apply()
    }

    fun design(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_DESIGN, 1).coerceIn(1, 3)

    fun setDesign(context: Context, value: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_DESIGN, value.coerceIn(1, 3)).apply()
    }

    fun current(context: Context): ClockDisplay {
        val calendar = Calendar.getInstance()
        val is24Hour = uses24Hour(context)
        val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val hour = if (is24Hour) hour24 else ((hour24 + 11) % 12) + 1
        val digits = "%02d%02d".format(hour, calendar.get(Calendar.MINUTE))
        val period = if (is24Hour) null else if (hour24 < 12) "AM" else "PM"
        val seconds = calendar.get(Calendar.SECOND)
        return ClockDisplay(digits, period, seconds)
    }
}
