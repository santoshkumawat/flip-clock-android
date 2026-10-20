package com.example.flipclock

import android.app.AlarmManager
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Switch

class SettingsActivity : Activity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_settings)

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(
                RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            )
        }

        val twentyFourHour = findViewById<Switch>(R.id.format_24_switch)
        twentyFourHour.isChecked = ClockTime.uses24Hour(this)
        twentyFourHour.setOnCheckedChangeListener { _, checked ->
            ClockTime.setUses24Hour(this, checked)
            FlipClockWidgetProvider.updateAll(this)
        }

        val showSeconds = findViewById<Switch>(R.id.show_seconds)
        showSeconds.isChecked = ClockTime.showsSeconds(this)
        showSeconds.setOnCheckedChangeListener { _, checked ->
            ClockTime.setShowsSeconds(this, checked)
            FlipClockWidgetProvider.updateAll(this)
            ClockScheduler.schedule(this)
        }

        val designOptions = findViewById<RadioGroup>(R.id.design_options)
        designOptions.check(
            when (ClockTime.design(this)) {
                2 -> R.id.design_2
                3 -> R.id.design_3
                else -> R.id.design_1
            }
        )
        designOptions.setOnCheckedChangeListener { _, checkedId ->
            val selectedDesign = when (checkedId) {
                R.id.design_2 -> 2
                R.id.design_3 -> 3
                R.id.design_1 -> 1
                else -> return@setOnCheckedChangeListener
            }
            ClockTime.setDesign(this, selectedDesign)
            FlipClockWidgetProvider.updateAll(this)
        }

        findViewById<Button>(R.id.precise_updates).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(Uri.parse("package:$packageName"))
                )
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true)
        ) {
            FlipClockWidgetProvider.updateAll(this)
            ClockScheduler.schedule(this)
        }
    }
}
