package com.personaltrainer.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltrainer.data.dao.*
import com.personaltrainer.data.entities.*
import com.personaltrainer.engine.nutrition.MacroTargets
import com.personaltrainer.engine.nutrition.NutritionEngine
import com.personaltrainer.engine.readiness.ReadinessEngine
import com.personaltrainer.engine.readiness.ReadinessRecommendation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val plannedSessionDao: PlannedSessionDao,
    private val plannedSetDao: PlannedSetDao,
    private val mealLogDao: MealLogDao,
    private val checkinDao: CheckinDao,
    private val sleepLogDao: SleepLogDao,
    private val bodyMetricDao: BodyMetricDao,
    private val appEventDao: AppEventDao
) : ViewModel() {

    private val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val profile: StateFlow<UserProfile?> = userProfileDao.observeProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todaySessions: StateFlow<List<PlannedSession>> = plannedSessionDao.observeByDate(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayMeals: StateFlow<List<MealLog>> = mealLogDao.observeByDate(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayCheckins: StateFlow<List<Checkin>> = checkinDao.observeByDate(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived: macro targets from profile
    val macroTargets: StateFlow<MacroTargets?> = profile.map { p ->
        if (p == null) return@map null
        val ageYears = 2026 - p.birthYear
        val bmr = NutritionEngine.bmr(p.weightKg, p.heightCm, ageYears, p.sex)
        val tdee = NutritionEngine.tdee(bmr, p.daysPerWeek)
        val targetKcal = NutritionEngine.targetCalories(tdee, p.goal, p.sex, ageYears)
        NutritionEngine.macros(targetKcal, p.weightKg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Derived: today's nutrition totals
    val todayNutritionTotals: StateFlow<NutritionTotals> = todayMeals.map { meals ->
        NutritionTotals(
            kcal = meals.sumOf { it.kcal.toDouble() }.toFloat(),
            proteinG = meals.sumOf { it.proteinG.toDouble() }.toFloat(),
            carbsG = meals.sumOf { it.carbsG.toDouble() }.toFloat(),
            fatG = meals.sumOf { it.fatG.toDouble() }.toFloat()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NutritionTotals())

    // Derived: readiness recommendation from morning check-in
    val readinessResult = todayCheckins.map { checkins ->
        val morning = checkins.filter { it.kind == CheckinKind.MORNING }
        if (morning.isEmpty()) return@map null

        val sleepHours = morning.find { it.questionKey == "sleep_hours" }?.answer?.toFloatOrNull() ?: 7f
        val sleepQuality = morning.find { it.questionKey == "sleep_quality" }?.answer?.toIntOrNull() ?: 3
        val soreness = morning.find { it.questionKey == "soreness" }?.answer?.toIntOrNull() ?: 2
        val energy = morning.find { it.questionKey == "energy" }?.answer?.toIntOrNull() ?: 3
        val pain = morning.find { it.questionKey == "pain_flag" }?.answer?.lowercase() == "true"

        ReadinessEngine.score(sleepHours, sleepQuality, soreness, energy, painFlag = pain)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun markSessionStarted(sessionId: Long) {
        viewModelScope.launch {
            plannedSessionDao.setStatus(sessionId, SessionStatus.STARTED)
            appEventDao.insert(AppEvent(type = "SessionStarted", payload = """{"sessionId":$sessionId}"""))
        }
    }

    fun markSessionDone(sessionId: Long) {
        viewModelScope.launch {
            plannedSessionDao.setStatus(sessionId, SessionStatus.DONE)
            appEventDao.insert(AppEvent(type = "SessionDone", payload = """{"sessionId":$sessionId}"""))
        }
    }

    fun markSessionSkipped(sessionId: Long, reason: String) {
        viewModelScope.launch {
            plannedSessionDao.setStatus(sessionId, SessionStatus.SKIPPED)
            appEventDao.insert(AppEvent(type = "SessionSkipped", payload = """{"sessionId":$sessionId,"reason":"$reason"}"""))
        }
    }
}

data class NutritionTotals(
    val kcal: Float = 0f,
    val proteinG: Float = 0f,
    val carbsG: Float = 0f,
    val fatG: Float = 0f
)
