package com.personaltrainer.engine.nutrition

import com.personaltrainer.data.entities.Goal
import com.personaltrainer.data.entities.Sex
import com.personaltrainer.data.entities.UserProfile
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure Kotlin nutrition engine — no Android imports.
 *
 * All formulas from the implementation plan, section 6.7.
 * All safety floors are enforced here in code, not in prompts.
 */
object NutritionEngine {

    // ── Calorie floors (safety hard-caps) ─────────────────────────────────
    const val CALORIE_FLOOR_MALE = 1500f
    const val CALORIE_FLOOR_FEMALE = 1200f
    const val MAX_DEFICIT_KCAL_PER_DAY = 500f

    // ── Protein targets (g/kg) ────────────────────────────────────────────
    const val PROTEIN_G_PER_KG_MIN = 1.6f
    const val PROTEIN_G_PER_KG_DEFAULT = 1.8f
    const val PROTEIN_G_PER_KG_MAX = 2.2f

    // ── Fat minimum ───────────────────────────────────────────────────────
    const val FAT_G_PER_KG_MIN = 0.7f

    // ── Water ─────────────────────────────────────────────────────────────
    const val WATER_ML_PER_KG_MIN = 30f
    const val WATER_ML_PER_KG_MAX = 40f

    /**
     * Mifflin-St Jeor BMR (kcal/day).
     *
     * @param weightKg current body weight
     * @param heightCm height
     * @param ageYears age
     * @param sex biological sex used for offset
     */
    fun bmr(weightKg: Float, heightCm: Float, ageYears: Int, sex: Sex): Float {
        val base = 10f * weightKg + 6.25f * heightCm - 5f * ageYears
        return when (sex) {
            Sex.MALE -> base + 5f
            Sex.FEMALE -> base - 161f
            Sex.OTHER -> base - 78f   // midpoint approximation
        }
    }

    /**
     * TDEE = BMR × activity multiplier.
     *
     * [sessionsPerWeek] is used to pick the multiplier bracket.
     */
    fun tdee(bmr: Float, sessionsPerWeek: Int): Float {
        val multiplier = when {
            sessionsPerWeek == 0 -> 1.2f            // sedentary
            sessionsPerWeek in 1..2 -> 1.375f       // lightly active
            sessionsPerWeek in 3..4 -> 1.55f        // moderately active
            else -> 1.725f                          // very active (5+)
        }
        return bmr * multiplier
    }

    /**
     * Target daily calorie intake after applying goal adjustment.
     * Enforces [CALORIE_FLOOR_MALE]/[CALORIE_FLOOR_FEMALE] and [MAX_DEFICIT_KCAL_PER_DAY].
     */
    fun targetCalories(
        tdee: Float,
        goal: Goal,
        sex: Sex,
        ageYears: Int
    ): Float {
        val floor = if (sex == Sex.FEMALE) CALORIE_FLOOR_FEMALE else CALORIE_FLOOR_MALE

        // Under-18 rule: no deficits
        if (ageYears < 18) return tdee

        val adjusted = when (goal) {
            Goal.FAT_LOSS -> tdee - min(MAX_DEFICIT_KCAL_PER_DAY, tdee * 0.20f)
            Goal.MUSCLE_GAIN -> tdee * 1.075f  // +7.5% surplus (midpoint of 5–10%)
            Goal.RECOMPOSITION -> tdee          // maintenance with body composition change
            Goal.GENERAL_HEALTH -> tdee
        }
        return max(adjusted, floor)
    }

    /**
     * Macro split given daily calorie target and body weight.
     *
     * Priority order: protein → fat floor → carbs fill the rest.
     */
    fun macros(targetKcal: Float, weightKg: Float): MacroTargets {
        val proteinG = weightKg * PROTEIN_G_PER_KG_DEFAULT
        val fatG = max(weightKg * FAT_G_PER_KG_MIN, targetKcal * 0.25f / 9f)
        val proteinKcal = proteinG * 4f
        val fatKcal = fatG * 9f
        val carbsKcal = max(0f, targetKcal - proteinKcal - fatKcal)
        val carbsG = carbsKcal / 4f
        val waterMl = weightKg * 35f   // midpoint of 30–40 ml/kg
        return MacroTargets(
            kcal = targetKcal.roundToInt(),
            proteinG = proteinG.roundToInt(),
            carbsG = carbsG.roundToInt(),
            fatG = fatG.roundToInt(),
            waterMl = waterMl.roundToInt()
        )
    }

    /**
     * Adaptive TDEE estimate after 2+ weeks of logged data.
     *
     * Formula: real_tdee ≈ avg_intake − (weight_change_kg × 7700 / days)
     */
    fun adaptiveTdee(
        averageDailyIntakeKcal: Float,
        weightChangeKg: Float,   // negative = lost weight
        days: Int
    ): Float? {
        if (days < 14 || days == 0) return null   // not enough data
        val kcalFromWeightChange = weightChangeKg * 7700f / days
        return averageDailyIntakeKcal - kcalFromWeightChange
    }

    /**
     * Off-plan budget distribution.
     *
     * When [excessKcal] is consumed over the weekly budget, spread the deficit
     * across the next [spreadDays] days, capped at [maxReductionPerDay] each day
     * and never below [floorKcal].
     *
     * Returns the reduced daily target.
     */
    fun offPlanDailyTarget(
        normalDailyTarget: Float,
        excessKcal: Float,
        spreadDays: Int = 3,
        maxReductionPerDay: Float = 0.15f,
        floorKcal: Float = CALORIE_FLOOR_MALE
    ): Float {
        val perDayReduction = min(excessKcal / spreadDays, normalDailyTarget * maxReductionPerDay)
        return max(normalDailyTarget - perDayReduction, floorKcal)
    }
}

data class MacroTargets(
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val waterMl: Int
)
