package com.personaltrainer.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.personaltrainer.data.Converters

/**
 * Persistent user profile.  Only one row ever exists (id = 1).
 */
@Entity(tableName = "user_profile")
@TypeConverters(Converters::class)
data class UserProfile(
    @PrimaryKey val id: Int = 1,

    // ── Demographics ──────────────────────────────────────────────────────
    val sex: Sex = Sex.MALE,
    val birthYear: Int = 1990,
    val heightCm: Float = 175f,
    val weightKg: Float = 75f,          // latest recorded, not the average

    // ── Goal ─────────────────────────────────────────────────────────────
    val goal: Goal = Goal.GENERAL_HEALTH,
    val experience: ExperienceLevel = ExperienceLevel.BEGINNER,

    // ── Schedule ─────────────────────────────────────────────────────────
    val daysPerWeek: Int = 3,
    val sessionMinutes: Int = 60,
    val wakeTimeMinutes: Int = 6 * 60,       // minutes from midnight (6:00)
    val sleepTimeMinutes: Int = 22 * 60 + 30, // 22:30
    val preferredWorkoutMinutes: Int = 18 * 60, // 18:00

    // ── Equipment & restrictions ──────────────────────────────────────────
    val equipment: List<Equipment> = listOf(Equipment.BARBELL, Equipment.DUMBBELLS),
    val injuries: List<String> = emptyList(),
    val dietPreferences: List<DietPreference> = emptyList(),
    val allergies: List<String> = emptyList(),

    // ── Gym location (for geofencing, optional) ───────────────────────────
    val gymLatitude: Double? = null,
    val gymLongitude: Double? = null,
    val gymRadiusMeters: Int = 100,

    // ── Coaching preferences ──────────────────────────────────────────────
    val strictness: Strictness = Strictness.NORMAL,
    val aiEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val nudgeBudgetPerDay: Int = 6,

    // ── Onboarding state ──────────────────────────────────────────────────
    val onboardingComplete: Boolean = false,
    val hasAcceptedDisclaimer: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class Sex { MALE, FEMALE, OTHER }

enum class Goal {
    FAT_LOSS,
    MUSCLE_GAIN,
    RECOMPOSITION,
    GENERAL_HEALTH
}

enum class ExperienceLevel {
    BEGINNER,      // < 1 year
    INTERMEDIATE,  // 1–3 years
    ADVANCED       // 3+ years
}

enum class Equipment {
    BODYWEIGHT,
    RESISTANCE_BANDS,
    DUMBBELLS,
    BARBELL,
    KETTLEBELL,
    CABLE_MACHINE,
    SMITH_MACHINE,
    PULL_UP_BAR,
    BENCH,
    SQUAT_RACK,
    FULL_GYM
}

enum class DietPreference {
    NONE,
    VEGETARIAN,
    VEGAN,
    KETO,
    PALEO,
    GLUTEN_FREE,
    DAIRY_FREE,
    HALAL,
    KOSHER
}

enum class Strictness { GENTLE, NORMAL, STRICT }
