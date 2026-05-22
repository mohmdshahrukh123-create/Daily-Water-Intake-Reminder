package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.HydrationPreferences
import com.example.data.local.WaterDatabase
import com.example.data.model.WaterLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("NotificationReceiver", "Received action: $action")

        val prefs = HydrationPreferences(context)
        if (!prefs.notificationsEnabled) {
            Log.d("NotificationReceiver", "Notifications are disabled in settings.")
            return
        }

        when (action) {
            ACTION_TRIGGER_NOTIFICATION -> {
                showSipNotification(context, prefs)
            }
            ACTION_DRINK -> {
                handleDrinkAction(context, prefs)
            }
            ACTION_POSTPONE -> {
                handlePostponeAction(context, prefs)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Re-schedule alarm on reboot
                recalculateAndSchedule(context)
            }
        }
    }

    private fun showSipNotification(context: Context, prefs: HydrationPreferences) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(notificationManager)

        val glassSize = prefs.glassSize
        val goal = prefs.dailyGoal
        val isOz = prefs.isImperial
        val formattedGlass = if (isOz) "${Math.round(glassSize * 0.0338f)} oz" else "$glassSize ml"
        val formattedGoal = if (isOz) "${Math.round(goal * 0.0338f)} oz" else "$goal ml"

        // Intent to launch Main Activity on click
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Drink Now
        val drinkIntent = Intent(context, NotificationReceiver::class.java).apply {
            this.action = ACTION_DRINK
        }
        val drinkPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            drinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Postpone (20 mins)
        val postponeIntent = Intent(context, NotificationReceiver::class.java).apply {
            this.action = ACTION_POSTPONE
        }
        val postponePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            postponeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appNameIdentifier = context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)
        val smallIcon = if (appNameIdentifier != 0) appNameIdentifier else android.R.drawable.stat_sys_warning

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle("Water Sip Reminder! 💧")
            .setContentText("Drink $formattedGlass to stay on track for your $formattedGoal daily goal!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_today, "Drink [+$formattedGlass]", drinkPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Remind in 20m", postponePendingIntent)

        // Play default sound if enabled
        if (prefs.soundEnabled) {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            notificationBuilder.setSound(alertUri)
        }

        // Trigger vibration if enabled
        if (prefs.vibrateEnabled) {
            notificationBuilder.setVibrate(longArrayOf(0, 250, 250, 250))
            triggerVibration(context)
        }

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())

        // Recalculate next standard alarm so it behaves in a continuous chain
        recalculateAndSchedule(context)
    }

    private fun triggerVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(250)
            }
        } catch (e: Exception) {
            Log.e("NotificationReceiver", "Failed to trigger vibration: ${e.message}")
        }
    }

    private fun handleDrinkAction(context: Context, prefs: HydrationPreferences) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Insert drink log to Room
                val glassSize = prefs.glassSize
                val database = WaterDatabase.getDatabase(context)
                database.waterLogDao().insertLog(WaterLog(amountMl = glassSize))

                // Reset postponed time
                prefs.postponedTime = 0L

                // Show feedback or just update
                Log.d("NotificationReceiver", "Log recorded from notification: $glassSize ml")

                // Cancel current notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID)

                // Recalculate next alarm
                recalculateAndSchedule(context)
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Error drinking from notification: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handlePostponeAction(context: Context, prefs: HydrationPreferences) {
        // Set postponed time to current time + 20 minutes
        val postponeTimeMs = System.currentTimeMillis() + (20 * 60 * 1000)
        prefs.postponedTime = postponeTimeMs

        Log.d("NotificationReceiver", "Postponing hydration alert by 20 minutes")

        // Cancel notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)

        // Schedule postponed alarm
        scheduleAlarmAt(context, postponeTimeMs)
    }

    private fun createChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hydration Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Periodic alerts to keep your hydration levels optimal"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "hydration_companion_reminders"
        const val NOTIFICATION_ID = 4829
        const val ACTION_TRIGGER_NOTIFICATION = "com.example.hydration.ACTION_TRIGGER_NOTIFICATION"
        const val ACTION_DRINK = "com.example.hydration.ACTION_DRINK"
        const val ACTION_POSTPONE = "com.example.hydration.ACTION_POSTPONE"

        fun recalculateAndSchedule(context: Context) {
            val prefs = HydrationPreferences(context)
            if (!prefs.notificationsEnabled) {
                cancelAlarm(context)
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                val database = WaterDatabase.getDatabase(context)
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfToday = cal.timeInMillis
                val loggedToday = database.waterLogDao().getWaterSumSince(startOfToday) ?: 0

                val nextDrinkMs = calculateNextTime(
                    currentTimeMs = System.currentTimeMillis(),
                    loggedTodayMl = loggedToday,
                    dailyGoalMl = prefs.dailyGoal,
                    glassSizeMl = prefs.glassSize,
                    wakeUpHour = prefs.wakeHour,
                    wakeUpMinute = prefs.wakeMinute,
                    bedTimeHour = prefs.bedHour,
                    bedTimeMinute = prefs.bedMinute,
                    remindAfterGoal = prefs.remindAfterGoal,
                    postponedTimeMs = prefs.postponedTime
                )

                if (nextDrinkMs > 0L) {
                    scheduleAlarmAt(context, nextDrinkMs)
                } else {
                    cancelAlarm(context)
                }
            }
        }

        private fun calculateNextTime(
            currentTimeMs: Long,
            loggedTodayMl: Int,
            dailyGoalMl: Int,
            glassSizeMl: Int,
            wakeUpHour: Int,
            wakeUpMinute: Int,
            bedTimeHour: Int,
            bedTimeMinute: Int,
            remindAfterGoal: Boolean,
            postponedTimeMs: Long
        ): Long {
            // Check if goal reached
            if (loggedTodayMl >= dailyGoalMl && !remindAfterGoal) {
                return 0L // No more alarms needed today
            }

            // If a valid postponement is active
            if (postponedTimeMs > currentTimeMs) {
                return postponedTimeMs
            }

            val calendar = Calendar.getInstance().apply {
                timeInMillis = currentTimeMs
            }
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            val currentMins = currentHour * 60 + currentMinute
            val wakeMins = wakeUpHour * 60 + wakeUpMinute
            val bedMins = bedTimeHour * 60 + bedTimeMinute

            val totalActiveMins = if (bedMins > wakeMins) bedMins - wakeMins else (24 * 60 - wakeMins) + bedMins
            val totalGlassesNeeded = Math.ceil(dailyGoalMl.toDouble() / glassSizeMl).toInt()
            val rawInterval = if (totalGlassesNeeded > 0) totalActiveMins / totalGlassesNeeded else 120
            val intervalMins = maxOf(30, rawInterval) // Interval space

            if (currentMins < wakeMins) {
                // Today at wake up time
                val targetCal = Calendar.getInstance().apply {
                    timeInMillis = currentTimeMs
                    set(Calendar.HOUR_OF_DAY, wakeUpHour)
                    set(Calendar.MINUTE, wakeUpMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return targetCal.timeInMillis
            } else if (currentMins >= bedMins) {
                // Tomorrow at wake up time
                val targetCal = Calendar.getInstance().apply {
                    timeInMillis = currentTimeMs
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, wakeUpHour)
                    set(Calendar.MINUTE, wakeUpMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return targetCal.timeInMillis
            } else {
                // Within active drinking hours, schedule next at [Now + Interval minutes]
                val targetCal = Calendar.getInstance().apply {
                    timeInMillis = currentTimeMs
                    add(Calendar.MINUTE, intervalMins)
                }
                val checkHour = targetCal.get(Calendar.HOUR_OF_DAY)
                val checkMinute = targetCal.get(Calendar.MINUTE)
                val checkMins = checkHour * 60 + checkMinute

                if (checkMins >= bedMins) {
                    // Capped at Bedtime, push to tomorrow's Wakeup instead of waking user up
                    val tomorrowWake = Calendar.getInstance().apply {
                        timeInMillis = currentTimeMs
                        add(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, wakeUpHour)
                        set(Calendar.MINUTE, wakeUpMinute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    return tomorrowWake.timeInMillis
                }
                return targetCal.timeInMillis
            }
        }

        private fun scheduleAlarmAt(context: Context, triggerTimeMs: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_TRIGGER_NOTIFICATION
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                } else {
                    @Suppress("DEPRECATION")
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                }
                Log.d("NotificationReceiver", "Successfully scheduled alarm at: ${java.util.Date(triggerTimeMs)}")
            } catch (e: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                Log.d("NotificationReceiver", "Fallback inexact alarm scheduled because of permissions constraint")
            }
        }

        private fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_TRIGGER_NOTIFICATION
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d("NotificationReceiver", "Active reminder alarm cancelled")
        }
    }
}
