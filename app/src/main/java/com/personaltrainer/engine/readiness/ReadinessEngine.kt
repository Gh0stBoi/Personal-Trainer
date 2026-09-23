package com.personaltrainer.engine.readiness

/**
 * Readiness score engine — section 6.8 of the implementation plan.
 *
 * Pure Kotlin, no Android imports.
 * All weights sum to 1.0.
 *
 * Score 0–100:
 *   70–100 → Train as planned
 *   40–69  → Reduce volume ~20%, add 1 RIR
 *   <40    → Swap to mobility/walk/rest
 *
 * Pain flag always overrides the numeric score.
 */
object ReadinessEngine {

    // ── Weights (must sum to 1.0) ─────────────────────────────────────────
    private const val W_SLEEP_HOURS = 0.35f
    private const val W_SLEEP_QUALITY = 0.15f
    private const val W_SORENESS = 0.20f
    private const val W_ENERGY_MOOD = 0.15f
    private const val W_HRV = 0.15f

    /**
     * Compute the readiness score (0–100).
     *
     * @param sleepHours      hours slept last night (clamped 0–12)
     * @param sleepQuality    user-reported 1–5
     * @param sorenessLevel   user-reported 1–5 (1 = none, 5 = very sore)
     * @param energyMood      user-reported 1–5 (1 = exhausted, 5 = great)
     * @param restingHrRatio  (optional) today's resting HR / 7-day average;
     *                        1.0 = normal, >1 = elevated (lower score)
     * @param painFlag        true if the user reports sharp or persistent pain
     */
    fun score(
        sleepHours: Float,
        sleepQuality: Int,       // 1–5
        sorenessLevel: Int,      // 1–5, higher = more sore
        energyMood: Int,         // 1–5
        restingHrRatio: Float? = null,
        painFlag: Boolean = false
    ): ReadinessResult {
        if (painFlag) {
            return ReadinessResult(
                score = 0,
                recommendation = ReadinessRecommendation.REST,
                painFlag = true
            )
        }

        val sleepHoursScore = normalizeSleepHours(sleepHours)        // 0–1
        val sleepQualityScore = (sleepQuality - 1) / 4f              // 0–1
        val sorenessScore = 1f - (sorenessLevel - 1) / 4f            // inverted: less sore = higher
        val energyScore = (energyMood - 1) / 4f                      // 0–1

        // HRV/HR component — only when available
        val hrScore = restingHrRatio?.let { ratio ->
            when {
                ratio <= 0.95f -> 1.0f           // lower than average → great
                ratio <= 1.05f -> 0.75f          // normal
                ratio <= 1.10f -> 0.50f          // slightly elevated
                ratio <= 1.20f -> 0.25f          // elevated
                else -> 0.0f                     // very elevated
            }
        } ?: 0.75f   // no data → assume neutral-good

        val hrWeight = if (restingHrRatio != null) W_HRV else 0f
        val totalWeight = W_SLEEP_HOURS + W_SLEEP_QUALITY + W_SORENESS + W_ENERGY_MOOD + hrWeight
        val raw = (
            W_SLEEP_HOURS * sleepHoursScore +
            W_SLEEP_QUALITY * sleepQualityScore +
            W_SORENESS * sorenessScore +
            W_ENERGY_MOOD * energyScore +
            hrWeight * hrScore
        ) / totalWeight

        val finalScore = (raw * 100).toInt().coerceIn(0, 100)

        val recommendation = when {
            finalScore >= 70 -> ReadinessRecommendation.TRAIN_AS_PLANNED
            finalScore >= 40 -> ReadinessRecommendation.REDUCED_VOLUME
            else -> ReadinessRecommendation.REST
        }

        return ReadinessResult(
            score = finalScore,
            recommendation = recommendation,
            painFlag = false,
            sleepHoursScore = sleepHoursScore,
            sleepQualityScore = sleepQualityScore,
            sorenessScore = sorenessScore,
            energyScore = energyScore,
            hrScore = hrScore
        )
    }

    /** Maps sleep hours to a 0–1 score. Target is 8h, minimum useful is 5h. */
    private fun normalizeSleepHours(hours: Float): Float = when {
        hours >= 8f  -> 1.0f
        hours >= 7f  -> 0.85f
        hours >= 6f  -> 0.65f
        hours >= 5f  -> 0.40f
        else -> 0.10f
    }
}

data class ReadinessResult(
    val score: Int,                              // 0–100
    val recommendation: ReadinessRecommendation,
    val painFlag: Boolean,
    // Component scores for debugging / display
    val sleepHoursScore: Float = 0f,
    val sleepQualityScore: Float = 0f,
    val sorenessScore: Float = 0f,
    val energyScore: Float = 0f,
    val hrScore: Float = 0f
)

enum class ReadinessRecommendation {
    TRAIN_AS_PLANNED,
    REDUCED_VOLUME,
    REST
}
