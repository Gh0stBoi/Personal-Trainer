package com.personaltrainer.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.personaltrainer.data.Converters
import com.personaltrainer.data.entities.Equipment

/**
 * Exercise definition — seed data loaded from assets/exercises.json.
 * Users can also add custom exercises (isCustom = true).
 */
@Entity(tableName = "exercise")
@TypeConverters(Converters::class)
data class Exercise(
    @PrimaryKey val id: String,         // e.g. "squat_barbell", "bench_press_flat"

    val name: String,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    val equipment: List<Equipment> = emptyList(),
    val pattern: MovementPattern,

    /** True for squat, bench, deadlift, row, OHP, pull-up — affects volume weighting */
    val isCompound: Boolean = false,

    /**
     * List of injury/condition tags that should exclude this exercise.
     * E.g. "lower_back", "knee", "shoulder_impingement"
     */
    val avoidIf: List<String> = emptyList(),

    /** Optional YouTube or in-app video link for form guidance. */
    val videoUrl: String? = null,

    /** Brief cue text shown in the workout screen. */
    val cues: String? = null,

    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class MuscleGroup {
    CHEST, BACK, SHOULDERS,
    BICEPS, TRICEPS, FOREARMS,
    QUADS, HAMSTRINGS, GLUTES, CALVES,
    ABS, OBLIQUES,
    HIP_FLEXORS, ADDUCTORS, ABDUCTORS,
    FULL_BODY, CARDIO
}

enum class MovementPattern {
    SQUAT, HINGE, PUSH, PULL, CARRY, ROTATION, GAIT, ISOLATION
}
