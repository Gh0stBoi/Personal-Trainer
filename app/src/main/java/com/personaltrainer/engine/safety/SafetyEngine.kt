package com.personaltrainer.engine.safety

import com.personaltrainer.data.entities.Goal
import com.personaltrainer.data.entities.Sex
import com.personaltrainer.engine.nutrition.NutritionEngine
import kotlin.math.abs

/**
 * Hard safety limits — section 8 of the implementation plan.
 *
 * These rules live in code, NOT in prompts.
 * The LLM CANNOT override them.
 *
 * All methods return [SafetyViolation] lists that the engine must
 * surface to the user as warnings.
 */
object SafetyEngine {

    // ── Weight loss rate ──────────────────────────────────────────────────
    /** Max weekly weight loss as fraction of body weight (1%). */
    const val MAX_WEEKLY_LOSS_FRACTION = 0.01f

    // ── Volume increase cap ───────────────────────────────────────────────
    /** Maximum weekly hard-set increase per muscle group (10%). */
    const val MAX_WEEKLY_VOLUME_INCREASE = 0.10f

    // ── Consecutive training days cap ─────────────────────────────────────
    const val MAX_CONSECUTIVE_TRAINING_DAYS = 4

    // ── Minimum rest days per week ────────────────────────────────────────
    const val MIN_REST_DAYS_PER_WEEK = 1

    /**
     * Checks whether a proposed calorie target is safe.
     */
    fun checkCaloricTarget(
        targetKcal: Float,
        sex: Sex,
        ageYears: Int
    ): List<SafetyViolation> {
        val violations = mutableListOf<SafetyViolation>()
        val floor = if (sex == Sex.FEMALE)
            NutritionEngine.CALORIE_FLOOR_FEMALE
        else
            NutritionEngine.CALORIE_FLOOR_MALE

        if (ageYears < 18 && targetKcal < floor) {
            violations += SafetyViolation(
                code = "UNDER_18_DEFICIT",
                message = "Calorie deficits are not recommended for users under 18.",
                severity = Severity.CRITICAL
            )
        }
        if (targetKcal < floor) {
            violations += SafetyViolation(
                code = "BELOW_CALORIE_FLOOR",
                message = "Calorie target (${targetKcal.toInt()} kcal) is below the safety floor " +
                          "(${floor.toInt()} kcal). Raising to floor.",
                severity = Severity.WARNING
            )
        }
        if (targetKcal < NutritionEngine.CALORIE_FLOOR_FEMALE * 0.8f) {
            violations += SafetyViolation(
                code = "DANGEROUSLY_LOW_CALORIES",
                message = "Calorie target is dangerously low. Please consult a healthcare professional.",
                severity = Severity.CRITICAL
            )
        }
        return violations
    }

    /**
     * Checks whether the weekly weight loss rate is within safe limits.
     *
     * @param weeklyChangeKg negative = lost weight
     * @param bodyWeightKg current body weight
     */
    fun checkWeightLossRate(
        weeklyChangeKg: Float,
        bodyWeightKg: Float
    ): List<SafetyViolation> {
        val violations = mutableListOf<SafetyViolation>()
        val lossFraction = abs(weeklyChangeKg) / bodyWeightKg
        if (weeklyChangeKg < 0 && lossFraction > MAX_WEEKLY_LOSS_FRACTION) {
            violations += SafetyViolation(
                code = "EXCESSIVE_WEIGHT_LOSS_RATE",
                message = "Weight loss rate (${String.format("%.2f", lossFraction * 100)}% per week) " +
                          "exceeds the safe limit of 1% per week. Consider increasing calories.",
                severity = Severity.WARNING
            )
        }
        return violations
    }

    /**
     * Checks whether weekly hard-set volume increase is within the 10% cap.
     */
    fun checkVolumeIncrease(
        previousWeekSets: Int,
        proposedWeekSets: Int
    ): List<SafetyViolation> {
        val violations = mutableListOf<SafetyViolation>()
        if (previousWeekSets > 0) {
            val increase = (proposedWeekSets - previousWeekSets).toFloat() / previousWeekSets
            if (increase > MAX_WEEKLY_VOLUME_INCREASE) {
                violations += SafetyViolation(
                    code = "VOLUME_SPIKE",
                    message = "Proposed volume increase (${String.format("%.0f", increase * 100)}%) " +
                              "exceeds the 10% weekly cap. Capping at ${(previousWeekSets * 1.10f).toInt()} sets.",
                    severity = Severity.INFO
                )
            }
        }
        return violations
    }

    /**
     * Checks for pain-related flags that should force exercise avoidance.
     */
    fun checkPainFlag(painReport: String?): List<SafetyViolation> {
        if (painReport.isNullOrBlank()) return emptyList()
        val keywords = listOf("sharp", "stabbing", "burning", "radiating", "severe", "acute")
        return if (keywords.any { painReport.lowercase().contains(it) }) {
            listOf(SafetyViolation(
                code = "PAIN_FLAG",
                message = "Sharp or severe pain reported. Stop this exercise and consider " +
                          "seeing a healthcare professional if it persists.",
                severity = Severity.CRITICAL
            ))
        } else emptyList()
    }
}

data class SafetyViolation(
    val code: String,
    val message: String,
    val severity: Severity
)

enum class Severity { INFO, WARNING, CRITICAL }
