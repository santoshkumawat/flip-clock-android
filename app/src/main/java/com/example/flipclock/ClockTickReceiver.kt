package com.example.flipclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ClockTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        FlipClockWidgetProvider.updateAll(context)
        ClockScheduler.schedule(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        FlipClockWidgetProvider.updateAll(context)
        ClockScheduler.schedule(context)
    }
}
