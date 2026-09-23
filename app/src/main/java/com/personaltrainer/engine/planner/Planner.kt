package com.personaltrainer.engine.planner

import com.personaltrainer.data.entities.*
import com.personaltrainer.engine.safety.SafetyEngine

/**
 * Plan generator — section 6.2 of the implementation plan.
 *
 * Pure Kotlin, no Android imports, fully unit-testable.
 *
 * Responsibilities:
 *  1. Choose the training split from days-per-week.
 *  2. Assign session types to week days.
 *  3. Apply double-progression rules.
 *  4. Detect deload triggers.
 */
object Planner {

    // ── Session volume parameters ──────────────────────────────────────────
    private const val EXERCISES_PER_60_MIN = 7    // ~6–8 min per exercise
    private const val SETS_PER_EXERCISE_DEFAULT = 3

    // ── Weekly volume targets (hard sets per muscle group) ─────────────────
    private const val WEEKLY_SETS_MIN = 8
    private const val WEEKLY_SETS_TARGET = 15
    private const val WEEKLY_SETS_MAX = 20

    // ── Double progression ────────────────────────────────────────────────
    private const val LOAD_INCREASE_UPPER_KG = 2.5f
    private const val LOAD_INCREASE_LOWER_KG = 5.0f
    private const val LOAD_DROP_FRACTION = 0.90f   // 10% deload

    // ── Deload rules ──────────────────────────────────────────────────────
    private const val DELOAD_VOLUME_FRACTION = 0.60f  // −40%
    private const val DELOAD_LOAD_FRACTION = 0.90f    // −10%
    private const val DELOAD_EVERY_WEEKS = 5          // every 4–6

    /**
     * Choose a split type based on days per week.
     */
    fun chooseSplit(daysPerWeek: Int): SplitType = when {
        daysPerWeek <= 3 -> SplitType.FULL_BODY
        daysPerWeek == 4 -> SplitType.UPPER_LOWER
        else -> SplitType.PUSH_PULL_LEGS
    }

    /**
     * Returns the ordered list of [SessionType] for one training week.
     * Rest days are not included — they fill the gaps.
     */
    fun weekSessionOrder(split: SplitType, daysPerWeek: Int): List<SessionType> = when (split) {
        SplitType.FULL_BODY -> List(daysPerWeek) { SessionType.FULL_BODY }
        SplitType.UPPER_LOWER -> buildList {
            repeat(daysPerWeek) { i -> add(if (i % 2 == 0) SessionType.UPPER else SessionType.LOWER) }
        }
        SplitType.PUSH_PULL_LEGS -> {
            val ppl = listOf(SessionType.PUSH, SessionType.PULL, SessionType.LEGS)
            List(daysPerWeek) { i -> ppl[i % 3] }
        }
        SplitType.PUSH_PULL_LEGS_UPPER_LOWER -> {
            val sequence = listOf(
                SessionType.PUSH, SessionType.PULL, SessionType.LEGS,
                SessionType.UPPER, SessionType.LOWER
            )
            List(daysPerWeek) { i -> sequence[i % sequence.size] }
        }
    }

    /**
     * Number of exercises that fit in a session of [targetMinutes].
     */
    fun exercisesForDuration(targetMinutes: Int): Int =
        (targetMinutes / 8).coerceIn(3, 10)   // 8 min/exercise, 3–10 exercises

    /**
     * Apply double-progression logic to a [PlannedSet].
     *
     * If ALL sets in the last session hit [repMax] with the target RIR,
     * increase load and reset reps to [repMin].
     * If the user missed [repMin] twice in a row, drop load by 10%.
     *
     * @param current   the planned set we're updating
     * @param lastSets  the logged sets from the previous session for this exercise
     * @param missCount how many consecutive sessions they've missed repMin
     */
    fun applyProgression(
        current: PlannedSet,
        lastSets: List<SetLog>,
        missCount: Int,
        isUpperBody: Boolean
    ): PlannedSet {
        if (lastSets.isEmpty()) return current

        val loadStep = if (isUpperBody) LOAD_INCREASE_UPPER_KG else LOAD_INCREASE_LOWER_KG

        // Check if all sets hit repMax
        val allHitMax = lastSets.all { it.reps >= current.repMax && it.rir <= current.targetRir }

        // Check consecutive misses
        val consecutiveMisses = missCount >= 2

        return when {
            consecutiveMisses -> {
                val newLoad = (current.targetLoad ?: 0f) * LOAD_DROP_FRACTION
                current.copy(targetLoad = newLoad)
            }
            allHitMax -> {
                val newLoad = (current.targetLoad ?: 0f) + loadStep
                current.copy(targetLoad = newLoad)
            }
            else -> current
        }
    }

    /**
     * Whether a deload should be triggered.
     *
     * Deload when:
     *  - Week number is a multiple of [DELOAD_EVERY_WEEKS], or
     *  - Low readiness for 3 consecutive sessions.
     */
    fun shouldDeload(weekNumber: Int, consecutiveLowReadiness: Int): Boolean {
        return weekNumber % DELOAD_EVERY_WEEKS == 0 || consecutiveLowReadiness >= 3
    }

    /**
     * Apply deload scaling to a [PlannedSet]: reduce volume and load.
     */
    fun applyDeload(set: PlannedSet): PlannedSet = set.copy(
        sets = maxOf(1, (set.sets * DELOAD_VOLUME_FRACTION).toInt()),
        targetLoad = set.targetLoad?.let { it * DELOAD_LOAD_FRACTION }
    )

    /**
     * Gap re-entry: scale load and volume based on days since last session.
     * Section 6.6.
     */
    fun applyGapReEntry(set: PlannedSet, daysSinceLastSession: Int): PlannedSet {
        val (loadFraction, setsReduction) = when {
            daysSinceLastSession < 3 -> 1.00f to 0
            daysSinceLastSession < 7 -> 0.90f to 1
            daysSinceLastSession < 14 -> 0.80f to 1
            else -> 0.70f to 2
        }
        return set.copy(
            targetLoad = set.targetLoad?.let { it * loadFraction },
            sets = maxOf(1, set.sets - setsReduction)
        )
    }
}
