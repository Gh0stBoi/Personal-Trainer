package com.personaltrainer.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personaltrainer.data.dao.UserProfileDao
import com.personaltrainer.data.entities.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userProfileDao: UserProfileDao
) : ViewModel() {

    // Whether onboarding is already complete (null = loading)
    val isOnboardingComplete: StateFlow<Boolean?> = userProfileDao.observeProfile()
        .map { it?.onboardingComplete }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Form state ────────────────────────────────────────────────────────
    var sex: Sex = Sex.MALE
    var birthYear: Int = 1995
    var heightCm: Float = 175f
    var weightKg: Float = 75f
    var goal: Goal = Goal.GENERAL_HEALTH
    var experience: ExperienceLevel = ExperienceLevel.BEGINNER
    var daysPerWeek: Int = 3
    var sessionMinutes: Int = 60
    var equipment: List<Equipment> = listOf(Equipment.BARBELL, Equipment.DUMBBELLS)
    var strictness: Strictness = Strictness.NORMAL
    var wakeTimeMinutes: Int = 6 * 60
    var sleepTimeMinutes: Int = 22 * 60 + 30
    var preferredWorkoutMinutes: Int = 18 * 60
    var injuries: List<String> = emptyList()
    var dietPreferences: List<DietPreference> = emptyList()
    var hasAcceptedDisclaimer: Boolean = false

    fun saveProfile(onDone: () -> Unit) {
        viewModelScope.launch {
            val profile = UserProfile(
                sex = sex,
                birthYear = birthYear,
                heightCm = heightCm,
                weightKg = weightKg,
                goal = goal,
                experience = experience,
                daysPerWeek = daysPerWeek,
                sessionMinutes = sessionMinutes,
                equipment = equipment,
                strictness = strictness,
                wakeTimeMinutes = wakeTimeMinutes,
                sleepTimeMinutes = sleepTimeMinutes,
                preferredWorkoutMinutes = preferredWorkoutMinutes,
                injuries = injuries,
                dietPreferences = dietPreferences,
                hasAcceptedDisclaimer = hasAcceptedDisclaimer,
                onboardingComplete = true
            )
            userProfileDao.upsert(profile)
            onDone()
        }
    }
}
