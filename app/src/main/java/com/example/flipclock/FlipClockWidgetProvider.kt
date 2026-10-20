package com.example.flipclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

class FlipClockWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, manager, it) }
        ClockScheduler.schedule(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateWidget(context, manager, appWidgetId, newOptions)
    }

    override fun onEnabled(context: Context) = ClockScheduler.schedule(context)

    override fun onDisabled(context: Context) = ClockScheduler.cancel(context)

    companion object {
        private fun design3DigitResource(
            digit: Char,
            index: Int,
            compact12Hour: Boolean
        ): Int {
            // When the leading 12-hour zero is hidden, the remaining hour digit
            // becomes the digit immediately before the colon.  Right-align it
            // inside its slot so it sits close to the colon like the reference.
            val firstInPair = if (compact12Hour && index == 1) true else index % 2 == 0
            return when (digit) {
                '0' -> if (firstInPair) R.drawable.design3_arial_pair_first_0 else R.drawable.design3_arial_pair_second_0
                '1' -> if (firstInPair) R.drawable.design3_arial_pair_first_1 else R.drawable.design3_arial_pair_second_1
                '2' -> if (firstInPair) R.drawable.design3_arial_pair_first_2 else R.drawable.design3_arial_pair_second_2
                '3' -> if (firstInPair) R.drawable.design3_arial_pair_first_3 else R.drawable.design3_arial_pair_second_3
                '4' -> if (firstInPair) R.drawable.design3_arial_pair_first_4 else R.drawable.design3_arial_pair_second_4
                '5' -> if (firstInPair) R.drawable.design3_arial_pair_first_5 else R.drawable.design3_arial_pair_second_5
                '6' -> if (firstInPair) R.drawable.design3_arial_pair_first_6 else R.drawable.design3_arial_pair_second_6
                '7' -> if (firstInPair) R.drawable.design3_arial_pair_first_7 else R.drawable.design3_arial_pair_second_7
                '8' -> if (firstInPair) R.drawable.design3_arial_pair_first_8 else R.drawable.design3_arial_pair_second_8
                '9' -> if (firstInPair) R.drawable.design3_arial_pair_first_9 else R.drawable.design3_arial_pair_second_9
                else -> if (firstInPair) R.drawable.design3_arial_pair_first_0 else R.drawable.design3_arial_pair_second_0
            }
        }

        /**
         * Design 3 deliberately uses images for its digits.  Some One UI widget
         * hosts substitute a system font for a RemoteViews TextView, while an
         * ImageView retains the exact Arial Black glyph outlines packaged here.
         */
        private fun setDigitFace(
            views: RemoteViews,
            design: Int,
            index: Int,
            viewId: Int,
            digit: String,
            compact12Hour: Boolean
        ) {
            if (design == 3) {
                views.setImageViewResource(
                    viewId,
                    design3DigitResource(digit.first(), index, compact12Hour)
                )
            } else {
                views.setTextViewText(viewId, digit)
            }
        }

        private fun createDesign3LabelBitmap(
            context: Context,
            text: String,
            color: Int,
            textSizeSp: Float
        ): Bitmap {
            val metrics = context.resources.displayMetrics
            val width = (74f * metrics.density).roundToInt().coerceAtLeast(1)
            val height = (24f * metrics.density).roundToInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                typeface = context.resources.getFont(R.font.arial_black)
                this.color = color
                textAlign = Paint.Align.RIGHT
                textSize = textSizeSp * metrics.scaledDensity
            }
            val availableWidth = width - metrics.density
            if (paint.measureText(text) > availableWidth) {
                // Keep date and weekday at the same visual height.  Compress
                // only the long date horizontally instead of shrinking its
                // entire font size to fit the metadata column.
                paint.textScaleX = availableWidth / paint.measureText(text)
            }
            val fontMetrics = paint.fontMetrics
            val baseline = height / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(text, width - metrics.density / 2f, baseline, paint)
            return bitmap
        }

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, FlipClockWidgetProvider::class.java)
            manager.getAppWidgetIds(provider).forEach { id ->
                updateWidget(context, manager, id, manager.getAppWidgetOptions(id))
            }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            options: Bundle = manager.getAppWidgetOptions(widgetId)
        ) {
            val design = ClockTime.design(context)
            val layoutId = when (design) {
                2 -> R.layout.widget_flip_clock_design2
                3 -> R.layout.widget_flip_clock_design3
                else -> R.layout.widget_flip_clock
            }
            val views = RemoteViews(context.packageName, layoutId)
            val display = ClockTime.current(context)
            val widgetBackground = if (design == 3) {
                android.R.color.transparent
            } else {
                R.drawable.widget_background
            }
            views.setInt(R.id.widget_root, "setBackgroundResource", widgetBackground)
            if (design != 3) {
                views.setInt(R.id.hours_card, "setBackgroundResource", R.drawable.digit_tile)
                views.setInt(R.id.minutes_card, "setBackgroundResource", R.drawable.digit_tile)
            }
            val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 220).toFloat()
            val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 96).toFloat()
            // Keep the digits fitted to the available widget size in either layout.
            val digitWidthAllowance = when (design) {
                2 -> 84f
                3 -> 136f
                else -> 72f
            }
            val visibleDigitCount = if (
                design == 3 && !ClockTime.uses24Hour(context) && display.digits[0] == '0'
            ) 3f else 4f
            val sizingDigitCount = if (design == 3) visibleDigitCount else 4f
            val digitWidthDp = ((widthDp - digitWidthAllowance) / sizingDigitCount)
                .coerceAtLeast(if (design == 3) 16f else 24f)
            val contentHeightAllowance = when (design) {
                2 -> 80f
                3 -> 12f
                else -> 57f
            }
            val contentHeightDp = (heightDp - contentHeightAllowance)
                .coerceAtLeast(if (design == 2) 16f else if (design == 3) 24f else 40f)
            val fontScale = context.resources.configuration.fontScale.coerceAtLeast(0.85f)
            val contentHeightScale = if (design == 3) 1.25f else 0.90f
            val digitWidthScale = if (design == 3) 2.25f else 1.7f
            val textSize = min(digitWidthDp * digitWidthScale, contentHeightDp * contentHeightScale)
                .coerceIn(if (design == 2) 14f else if (design == 3) 18f else 36f, 136f) / fontScale
            val headerTextSize = (if (design == 3) 17f else 15f) / fontScale
            val mainFlipperIds = intArrayOf(
                R.id.digit_flipper_0, R.id.digit_flipper_1,
                R.id.digit_flipper_2, R.id.digit_flipper_3
            )
            val mainDigitFaceIds = arrayOf(
                intArrayOf(R.id.digit_0_a, R.id.digit_0_b),
                intArrayOf(R.id.digit_1_a, R.id.digit_1_b),
                intArrayOf(R.id.digit_2_a, R.id.digit_2_b),
                intArrayOf(R.id.digit_3_a, R.id.digit_3_b)
            )
            val hideLeadingHourZero = design == 3 &&
                !ClockTime.uses24Hour(context) && display.digits[0] == '0'
            if (design == 3) {
                views.setViewVisibility(
                    mainFlipperIds[0],
                    if (hideLeadingHourZero) View.GONE else View.VISIBLE
                )
            }
            val showSeconds = ClockTime.showsSeconds(context)
            val flipperIds = mainFlipperIds
            val digitFaceIds = mainDigitFaceIds
            val digitsToDisplay = display.digits
            val digitState = context.getSharedPreferences("flip_clock_digit_state", Context.MODE_PRIVATE)
            val stateEditor = digitState.edit()
            val changingDigitPairs = BooleanArray(2) { pairIndex ->
                val firstDigitIndex = pairIndex * 2
                (firstDigitIndex until firstDigitIndex + 2).any { index ->
                    val previousDigit = digitState.getString("${widgetId}_digit_$index", null)
                    previousDigit != null && previousDigit != digitsToDisplay[index].toString()
                }
            }
            if (design == 3) {
                val now = Calendar.getInstance().time
                val locale = Locale.getDefault()
                views.setImageViewBitmap(
                    R.id.date_header,
                    createDesign3LabelBitmap(
                        context,
                        SimpleDateFormat("MMM d", locale).format(now),
                        Color.rgb(182, 199, 224),
                        headerTextSize
                    )
                )
                views.setImageViewBitmap(
                    R.id.weekday_header,
                    createDesign3LabelBitmap(
                        context,
                        SimpleDateFormat("EEEE", locale).format(now),
                        Color.rgb(240, 243, 248),
                        headerTextSize
                    )
                )
                // Align the right-side date/day labels to the visible digit column's
                // top and bottom edges, while keeping their 10dp right inset.
                val digitFontSizeDp = textSize * fontScale
                val digitInkHeightDp = digitFontSizeDp * 0.74f
                val maxInsetDp = ((contentHeightDp - 36f) / 2f).coerceAtLeast(0f)
                val metadataInsetDp = (((contentHeightDp - digitInkHeightDp) / 2f) - 2f)
                    .coerceIn(0f, maxInsetDp)
                val metadataInsetPx =
                    (metadataInsetDp * context.resources.displayMetrics.density).roundToInt()
                val rightPaddingPx = (10f * context.resources.displayMetrics.density).roundToInt()
                views.setViewPadding(
                    R.id.metadata_box,
                    0,
                    metadataInsetPx,
                    rightPaddingPx,
                    metadataInsetPx
                )
            } else {
                views.setTextViewTextSize(R.id.date_header, TypedValue.COMPLEX_UNIT_SP, headerTextSize)
                views.setTextViewTextSize(R.id.weekday_header, TypedValue.COMPLEX_UNIT_SP, headerTextSize)
            }
            flipperIds.forEachIndexed { index, flipperId ->
                val newDigit = digitsToDisplay[index].toString()
                val digitKey = "${widgetId}_digit_$index"
                val activeKey = "${widgetId}_active_$index"
                val oldDigit = digitState.getString(digitKey, null)
                val activeFace = digitState.getInt(activeKey, 0)
                val nextFace = 1 - activeFace
                val activeViewId = digitFaceIds[index][activeFace]
                val nextViewId = digitFaceIds[index][nextFace]

                if (oldDigit == null) {
                    // First render has no previous value to animate from.
                    setDigitFace(views, design, index, digitFaceIds[index][0], newDigit, hideLeadingHourZero)
                    setDigitFace(views, design, index, digitFaceIds[index][1], newDigit, hideLeadingHourZero)
                    stateEditor.putInt(activeKey, 0)
                } else if (changingDigitPairs[index / 2]) {
                    setDigitFace(views, design, index, activeViewId, oldDigit, hideLeadingHourZero)
                    setDigitFace(views, design, index, nextViewId, newDigit, hideLeadingHourZero)
                    views.setDisplayedChild(flipperId, nextFace)
                    stateEditor.putInt(activeKey, nextFace)
                } else {
                    // Keep both faces current for launcher re-inflation, but do not
                    // change the displayed child when this two-digit pair is unchanged.
                    setDigitFace(views, design, index, digitFaceIds[index][0], newDigit, hideLeadingHourZero)
                    setDigitFace(views, design, index, digitFaceIds[index][1], newDigit, hideLeadingHourZero)
                }
                if (design != 3) {
                    views.setTextViewTextSize(activeViewId, TypedValue.COMPLEX_UNIT_SP, textSize)
                    views.setTextViewTextSize(nextViewId, TypedValue.COMPLEX_UNIT_SP, textSize)
                }
                stateEditor.putString(digitKey, newDigit)
            }
            stateEditor.apply()
            if (design != 3) {
                if (display.period == null) {
                    views.setViewVisibility(R.id.period, View.GONE)
                } else {
                    views.setTextViewText(R.id.period, display.period)
                    views.setViewVisibility(R.id.period, View.VISIBLE)
                }
            }
            if (design == 3) {
                views.setViewVisibility(
                    R.id.seconds_container,
                    if (showSeconds) View.VISIBLE else View.GONE
                )
                views.setViewVisibility(
                    R.id.time_separator,
                    if (showSeconds) View.GONE else View.VISIBLE
                )
                if (showSeconds) {
                    // In this design the seconds option blinks the colon once per second.
                    views.setDisplayedChild(R.id.seconds_flipper, display.seconds % 2)
                }
            } else {
                views.setViewVisibility(
                    R.id.seconds_container,
                    // Design 1 needs this 18dp footer to remain in the layout
                    // even when the seconds text is hidden.  It mirrors the
                    // fixed 18dp AM/PM footer in the hours card, keeping both
                    // split seams on the same horizontal line.
                    if (design == 1) View.VISIBLE
                    else if (showSeconds) View.VISIBLE else View.GONE
                )
                if (design == 1) {
                    views.setViewVisibility(
                        R.id.seconds_flipper,
                        if (showSeconds) View.VISIBLE else View.GONE
                    )
                }
                if (showSeconds) {
                    // The launcher-side ViewFlipper advances once per second without
                    // waking the app process for every displayed second.
                    views.setDisplayedChild(R.id.seconds_flipper, display.seconds)
                }
            }
            manager.updateAppWidget(widgetId, views)
        }
    }
}

object ClockScheduler {
    private const val ACTION_TICK = "com.example.flipclock.CLOCK_TICK"
    private const val REQUEST_CODE = 4120

    fun schedule(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        if (manager.getAppWidgetIds(ComponentName(context, FlipClockWidgetProvider::class.java)).isEmpty()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ClockTickReceiver::class.java).setAction(ACTION_TICK)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val now = System.currentTimeMillis()
        val nextMinute = ((now / 60_000L) + 1L) * 60_000L
        val exactAlarmsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (exactAlarmsAllowed) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextMinute, pending)
        } else {
            // Keep the clock functional when exact-alarm access has not been granted.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMinute, pending)
        }
    }

    fun cancel(context: Context) {
        val intent = Intent(context, ClockTickReceiver::class.java).setAction(ACTION_TICK)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
    }
}
