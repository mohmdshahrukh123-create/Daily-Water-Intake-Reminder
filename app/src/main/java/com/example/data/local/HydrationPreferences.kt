package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class HydrationPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "hydration_prefs"
        private const val KEY_GENDER = "gender"
        private const val KEY_WEIGHT = "weight"
        private const val KEY_HEIGHT = "height"
        private const val KEY_WORKOUT_MINUTES = "workout_minutes"
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_GLASS_SIZE = "glass_size"
        private const val KEY_WAKE_HOUR = "wake_hour"
        private const val KEY_WAKE_MINUTE = "wake_minute"
        private const val KEY_BED_HOUR = "bed_hour"
        private const val KEY_BED_MINUTE = "bed_minute"
        private const val KEY_IS_IMPERIAL = "is_imperial"
        private const val KEY_REMIND_AFTER_GOAL = "remind_after_goal"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_WEATHER_ADJUST = "weather_adjust"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATE_ENABLED = "vibrate_enabled"
        private const val KEY_ONBOARDED = "onboarded"
        private const val KEY_STREAK = "streak"
        private const val KEY_LAST_LOGGED_DATE = "last_logged_date"
        private const val KEY_POSTPONED_TIME = "postponed_time"
        private const val KEY_GATED_PERMISSION_SHOWN = "gated_permission_shown"
    }

    var gender: String
        get() = prefs.getString(KEY_GENDER, "Male") ?: "Male"
        set(value) = prefs.edit().putString(KEY_GENDER, value).apply()

    var weight: Float
        get() = prefs.getFloat(KEY_WEIGHT, 70f)
        set(value) = prefs.edit().putFloat(KEY_WEIGHT, value).apply()

    var height: Float
        get() = prefs.getFloat(KEY_HEIGHT, 175f)
        set(value) = prefs.edit().putFloat(KEY_HEIGHT, value).apply()

    var workoutMinutes: Int
        get() = prefs.getInt(KEY_WORKOUT_MINUTES, 30)
        set(value) = prefs.edit().putInt(KEY_WORKOUT_MINUTES, value).apply()

    var dailyGoal: Int
        get() = prefs.getInt(KEY_DAILY_GOAL, 2500)
        set(value) = prefs.edit().putInt(KEY_DAILY_GOAL, value).apply()

    var glassSize: Int
        get() = prefs.getInt(KEY_GLASS_SIZE, 250)
        set(value) = prefs.edit().putInt(KEY_GLASS_SIZE, value).apply()

    var wakeHour: Int
        get() = prefs.getInt(KEY_WAKE_HOUR, 7)
        set(value) = prefs.edit().putInt(KEY_WAKE_HOUR, value).apply()

    var wakeMinute: Int
        get() = prefs.getInt(KEY_WAKE_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_WAKE_MINUTE, value).apply()

    var bedHour: Int
        get() = prefs.getInt(KEY_BED_HOUR, 22)
        set(value) = prefs.edit().putInt(KEY_BED_HOUR, value).apply()

    var bedMinute: Int
        get() = prefs.getInt(KEY_BED_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_BED_MINUTE, value).apply()

    var isImperial: Boolean
        get() = prefs.getBoolean(KEY_IS_IMPERIAL, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_IMPERIAL, value).apply()

    var remindAfterGoal: Boolean
        get() = prefs.getBoolean(KEY_REMIND_AFTER_GOAL, false)
        set(value) = prefs.edit().putBoolean(KEY_REMIND_AFTER_GOAL, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var weatherAdjust: Boolean
        get() = prefs.getBoolean(KEY_WEATHER_ADJUST, false)
        set(value) = prefs.edit().putBoolean(KEY_WEATHER_ADJUST, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var vibrateEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE_ENABLED, value).apply()

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    var streak: Int
        get() = prefs.getInt(KEY_STREAK, 0)
        set(value) = prefs.edit().putInt(KEY_STREAK, value).apply()

    var lastLoggedDateYmd: String
        get() = prefs.getString(KEY_LAST_LOGGED_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_LOGGED_DATE, value).apply()

    var postponedTime: Long
        get() = prefs.getLong(KEY_POSTPONED_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_POSTPONED_TIME, value).apply()

    var hasShownPermissionGate: Boolean
        get() = prefs.getBoolean(KEY_GATED_PERMISSION_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_GATED_PERMISSION_SHOWN, value).apply()

    fun calculateRecommendedGoal(): Int {
        // Daily heavy workout calculation
        // Base intake depending on gender: male: 35ml/kg, female: 30ml/kg
        val baseMultiplier = if (gender.lowercase() == "male") 35f else 30f
        var recommended = (weight * baseMultiplier) + (workoutMinutes * 12f)
        
        // Slight height adjustments
        recommended += (height - 150f).coerceAtLeast(0f) * 2f
        
        // Add weather adjustment modifier if enabled
        if (weatherAdjust) {
            recommended += 400f // Hard day hot day boost
        }
        
        return recommended.toInt().coerceIn(1000, 6000)
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}
