package com.personaltrainer.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Actual logged set data captured during a workout.
 */
@Entity(
    tableName = "set_log",
    foreignKeys = [ForeignKey(
        entity = PlannedSession::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val setNo: Int,
    val reps: Int,
    val loadKg: Float,
    /** Reps in reserve: 0 = to failure, 1 = 1 left in tank, etc. */
    val rir: Int = 2,
    /** Estimated 1-rep max (Epley formula): weight × (1 + reps / 30) */
    val e1rm: Float = loadKg * (1f + reps / 30f),
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
