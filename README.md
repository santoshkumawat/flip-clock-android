# Flip Clock for Android

A small Android home-screen clock widget inspired by a mechanical split-flap clock. It works across compatible Android phones and tablets, displays four large digits, supports 12-hour and 24-hour formats, and includes a center seam and dark raised tiles.

## Open and build

1. Install Android Studio with Android SDK Platform 35 and a JDK 17 runtime.
2. Open this `FlipClockWidget` folder in Android Studio and allow Gradle sync to finish.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Android Studio writes the debug APK to `app/build/outputs/apk/debug/app-debug.apk`.

Alternatively, from this directory run `gradle :app:assembleDebug` with Gradle 8.9 or newer and Android SDK Platform 35 installed.

## Install on Android

Copy `app/build/outputs/apk/debug/app-debug.apk` to an Android phone or tablet and open it to install. If Android blocks the install, allow this file manager or browser to install unknown apps, then retry. Long-press the Home screen, open **Widgets**, find **Flip Clock**, and add it. It uses the device's 12/24-hour preference by default. Open **Flip Clock** from the app drawer to choose a format; the choice updates all placed widgets. If the launcher supports widget reconfiguration, the format screen is also available from the widget's long-press menu. For minute changes with less delay, open Flip Clock, tap **Enable precise minute updates**, and allow **Alarms & reminders** in system settings.

The app's **Show seconds** setting adds a live seconds readout to the center of the widget. It uses Android's `TextClock` view, which updates independently of the minute alarm.

Digit text size adapts to the widget's available width and height whenever the launcher reports a resize.

Changed digits use a short vertical split-flap animation. The widget remembers each digit's current face so minute changes animate the correct panel even after a resize or process restart.

The widget skips mandatory first-run configuration so Android launchers can place it directly, and declares the configuration screen optional and reconfigurable. It targets the Home screen; lock-screen app widgets are not part of the supported Android widget categories on modern Android.

## Notes

Time refreshes are scheduled at the next minute boundary and rescheduled after each tick, boot, manual time change, or time-zone change. Android may defer alarms while in deep idle. The project uses platform `RemoteViews` widgets and has no third-party runtime libraries.
