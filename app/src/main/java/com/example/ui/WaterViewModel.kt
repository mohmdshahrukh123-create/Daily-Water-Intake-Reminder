package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.HydrationPreferences
import com.example.data.local.WaterDatabase
import com.example.data.model.WaterLog
import com.example.data.repository.WaterRepository
import com.example.receiver.NotificationReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WaterViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val prefs = HydrationPreferences(context)
    private val repository: WaterRepository

    // Onboarding UI step index
    private val _onboardingStep = MutableStateFlow(0)
    val onboardingStep = _onboardingStep.asStateFlow()

    // Temporary/Editable State properties
    // On Onboarding complete, they are stored, but they're reactive here too
    val gender = MutableStateFlow(prefs.gender)
    val weight = MutableStateFlow(prefs.weight)
    val height = MutableStateFlow(prefs.height)
    val workoutMinutes = MutableStateFlow(prefs.workoutMinutes)
    val wakeHour = MutableStateFlow(prefs.wakeHour)
    val wakeMinute = MutableStateFlow(prefs.wakeMinute)
    val bedHour = MutableStateFlow(prefs.bedHour)
    val bedMinute = MutableStateFlow(prefs.bedMinute)
    val isImperial = MutableStateFlow(prefs.isImperial)
    val glassSize = MutableStateFlow(prefs.glassSize)
    val dailyGoal = MutableStateFlow(prefs.dailyGoal)
    val weatherAdjust = MutableStateFlow(prefs.weatherAdjust)
    val remindAfterGoal = MutableStateFlow(prefs.remindAfterGoal)
    val notificationsEnabled = MutableStateFlow(prefs.notificationsEnabled)
    val soundEnabled = MutableStateFlow(prefs.soundEnabled)
    val vibrateEnabled = MutableStateFlow(prefs.vibrateEnabled)

    // Splash animation helper state
    private val _showSplash = MutableStateFlow(true)
    val showSplash = _showSplash.asStateFlow()

    // App screen index state (if onboarding is completed, show DASHBOARD, else onboarding)
    private val _isOnboardingCompleted = MutableStateFlow(prefs.isOnboardingCompleted)
    val isOnboardingCompleted = _isOnboardingCompleted.asStateFlow()

    // Today's water log flows
    private val _startOfToday = MutableStateFlow(getStartOfTodayMs())
    val startOfToday = _startOfToday.asStateFlow()

    init {
        val database = WaterDatabase.getDatabase(context)
        repository = WaterRepository(database.waterLogDao())

        // Disable splash screen after 500ms as strictly mandated in spec ("MUST NOT exceed 0.5s")
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            _showSplash.value = false
        }
    }

    // Active flow of logs recorded today
    val todayLogs: StateFlow<List<WaterLog>> = combine(
        repository.allLogs,
        _startOfToday
    ) { logs, todayStart ->
        logs.filter { it.timestamp >= todayStart }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sum of logged ml of water today
    val todaySum: StateFlow<Int> = todayLogs.combine(MutableStateFlow(0)) { logs, _ ->
        logs.sumOf { it.amountMl }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Expose all logs for full historical stats
    val allLogs: StateFlow<List<WaterLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Hydration Streak (Calculated on state load)
    private val _streak = MutableStateFlow(prefs.streak)
    val streak = _streak.asStateFlow()

    fun nextStep() {
        _onboardingStep.value += 1
    }

    fun prevStep() {
        if (_onboardingStep.value > 0) {
            _onboardingStep.value -= 1
        }
    }

    fun updateGender(value: String) {
        gender.value = value
        prefs.gender = value
    }

    fun updateWeight(value: Float) {
        weight.value = value
        prefs.weight = value
    }

    fun updateHeight(value: Float) {
        height.value = value
        prefs.height = value
    }

    fun updateWorkoutMinutes(value: Int) {
        workoutMinutes.value = value
        prefs.workoutMinutes = value
    }

    fun updateWakeTime(hour: Int, minute: Int) {
        wakeHour.value = hour
        wakeMinute.value = minute
        prefs.wakeHour = hour
        prefs.wakeMinute = minute
    }

    fun updateBedTime(hour: Int, minute: Int) {
        bedHour.value = hour
        bedMinute.value = minute
        prefs.bedHour = hour
        prefs.bedMinute = minute
    }

    fun updateWeatherAdjust(enabled: Boolean) {
        weatherAdjust.value = enabled
        prefs.weatherAdjust = enabled
        recalculateAndSaveGoal()
    }

    fun updateUnits(useImperialVal: Boolean) {
        isImperial.value = useImperialVal
        prefs.isImperial = useImperialVal
        
        // Adjust values dynamically
        if (useImperialVal) {
            // Convert existing and round
            glassSize.value = 250 // reset to default 250ml (8oz display)
            prefs.glassSize = 250
        } else {
            glassSize.value = 250
            prefs.glassSize = 250
        }
        recalculateAndSaveGoal()
    }

    fun updateGlassSize(ml: Int) {
        glassSize.value = ml
        prefs.glassSize = ml
        // Instantly recalculate notification schedule
        retriggerSchedule()
    }

    fun updateCustomGoal(goalMl: Int) {
        dailyGoal.value = goalMl
        prefs.dailyGoal = goalMl
        retriggerSchedule()
    }

    fun updateRemindAfterGoal(enabled: Boolean) {
        remindAfterGoal.value = enabled
        prefs.remindAfterGoal = enabled
        retriggerSchedule()
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled.value = enabled
        prefs.notificationsEnabled = enabled
        retriggerSchedule()
    }

    fun updateSoundEnabled(enabled: Boolean) {
        soundEnabled.value = enabled
        prefs.soundEnabled = enabled
    }

    fun updateVibrateEnabled(enabled: Boolean) {
        vibrateEnabled.value = enabled
        prefs.vibrateEnabled = enabled
    }

    // Smart goal calculations based on biometric settings
    fun recalculateAndSaveGoal() {
        val recommended = prefs.calculateRecommendedGoal()
        dailyGoal.value = recommended
        prefs.dailyGoal = recommended
        retriggerSchedule()
    }

    fun completeOnboarding() {
        prefs.gender = gender.value
        prefs.weight = weight.value
        prefs.height = height.value
        prefs.workoutMinutes = workoutMinutes.value
        prefs.wakeHour = wakeHour.value
        prefs.wakeMinute = wakeMinute.value
        prefs.bedHour = bedHour.value
        prefs.bedMinute = bedMinute.value
        prefs.isImperial = isImperial.value
        prefs.isOnboardingCompleted = true
        
        recalculateAndSaveGoal()
        
        _isOnboardingCompleted.value = true
        _onboardingStep.value = 0
    }

    fun logWater(amountMl: Int) {
        viewModelScope.launch {
            repository.insert(WaterLog(amountMl = amountMl))
            checkStreakUpdate()
            retriggerSchedule()
        }
    }

    fun deleteLog(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
            retriggerSchedule()
        }
    }

    private fun checkStreakUpdate() {
        val todayYmd = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastLogged = prefs.lastLoggedDateYmd

        if (lastLogged != todayYmd) {
            val sum = todaySum.value + glassSize.value
            if (sum >= dailyGoal.value) {
                // Streak milestone reached
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val yesterdayYmd = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time)

                if (lastLogged == yesterdayYmd) {
                    prefs.streak = prefs.streak + 1
                } else if (lastLogged == "") {
                    prefs.streak = 1
                } else {
                    prefs.streak = 1 // Reset if gap occurred
                }
                prefs.lastLoggedDateYmd = todayYmd
                _streak.value = prefs.streak
            }
        }
    }

    fun retriggerSchedule() {
        NotificationReceiver.recalculateAndSchedule(context)
    }

    fun postponeAlarmBy20m() {
        val nextTime = System.currentTimeMillis() + (20 * 60 * 1000)
        prefs.postponedTime = nextTime
        retriggerSchedule()
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.clearAll()
            prefs.clearAllData()
            
            // Restore default viewmodel states
            gender.value = "Male"
            weight.value = 70f
            height.value = 175f
            workoutMinutes.value = 30
            wakeHour.value = 7
            wakeMinute.value = 0
            bedHour.value = 22
            bedMinute.value = 0
            isImperial.value = false
            glassSize.value = 250
            dailyGoal.value = 2500
            weatherAdjust.value = false
            remindAfterGoal.value = false
            notificationsEnabled.value = true
            soundEnabled.value = true
            vibrateEnabled.value = true
            _streak.value = 0

            _isOnboardingCompleted.value = false
            _onboardingStep.value = 0
            prefs.isOnboardingCompleted = false

            NotificationReceiver.recalculateAndSchedule(context)
        }
    }

    // Refresh today's context timestamp if a day shifts
    fun checkDayTransition() {
        val newTodayStart = getStartOfTodayMs()
        if (newTodayStart != _startOfToday.value) {
            _startOfToday.value = newTodayStart
            // Evaluate streak integrity
            viewModelScope.launch {
                val yesterday = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val calendarYmd = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(yesterday.time)
                if (prefs.lastLoggedDateYmd != "" && prefs.lastLoggedDateYmd != calendarYmd && prefs.lastLoggedDateYmd != SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())) {
                    // Broke daily streak
                    prefs.streak = 0
                    _streak.value = 0
                }
            }
        }
    }

    private fun getStartOfTodayMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // Analytical grouping aggregations
    fun getWeeklySummary(logs: List<WaterLog>): List<BarItem> {
        val calendar = Calendar.getInstance()
        val result = mutableListOf<BarItem>()
        val dayFormatter = SimpleDateFormat("E", Locale.getDefault())

        // Calculate for last 7 days
        for (i in 6 downTo 0) {
            val dCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val startOfDay = dCal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1

            val dayLabel = dayFormatter.format(dCal.time)
            val sum = logs.filter { it.timestamp in startOfDay..endOfDay }.sumOf { it.amountMl }
            result.add(BarItem(dayLabel, sum))
        }
        return result
    }

    fun getMonthlySummary(logs: List<WaterLog>): List<BarItem> {
        val result = mutableListOf<BarItem>()
        // Summarize last 4 weeks
        for (w in 3 downTo 0) {
            val dCal = Calendar.getInstance()
            dCal.add(Calendar.WEEK_OF_YEAR, -w)
            val startOfWeek = dCal.apply {
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfWeek = startOfWeek + (7L * 24 * 60 * 60 * 1000) - 1
            val weekSum = logs.filter { it.timestamp in startOfWeek..endOfWeek }.sumOf { it.amountMl }
            result.add(BarItem("W${4 - w}", weekSum))
        }
        return result
    }

    fun getYearlySummary(logs: List<WaterLog>): List<BarItem> {
        val result = mutableListOf<BarItem>()
        val monthFormatter = SimpleDateFormat("MMM", Locale.getDefault())
        val cal = Calendar.getInstance()
        // Last 6 months
        for (m in 5 downTo 0) {
            val mCal = Calendar.getInstance().apply {
                add(Calendar.MONTH, -m)
            }
            val startOfMonth = mCal.apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val lastDay = mCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val endOfMonth = Calendar.getInstance().apply {
                timeInMillis = startOfMonth
                set(Calendar.DAY_OF_MONTH, lastDay)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis

            val label = monthFormatter.format(mCal.time)
            val monthSum = logs.filter { it.timestamp in startOfMonth..endOfMonth }.sumOf { it.amountMl }
            result.add(BarItem(label, monthSum))
        }
        return result
    }
}

// Data holder for custom styled interactive bar-charts
data class BarItem(
    val label: String,
    val amountMl: Int
)
