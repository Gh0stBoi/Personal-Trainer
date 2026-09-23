package com.personaltrainer.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.personaltrainer.data.Converters

// ─── Training Program ─────────────────────────────────────────────────────────

/**
 * A mesocycle: a multi-week training block.
 * Only one program should be ACTIVE at a time.
 */
@Entity(tableName = "program")
data class Program(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val splitType: SplitType,
    val startDate: String,              // "yyyy-MM-dd"
    val weeks: Int = 4,
    val status: ProgramStatus = ProgramStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SplitType {
    FULL_BODY,
    UPPER_LOWER,
    PUSH_PULL_LEGS,
    PUSH_PULL_LEGS_UPPER_LOWER   // 5–6 day hybrid
}

enum class ProgramStatus { ACTIVE, COMPLETED, ARCHIVED }

// ─── Planned Session ─────────────────────────────────────────────────────────

/**
 * One training session in the plan.  May be moved from its [originalDate].
 */
@Entity(
    tableName = "planned_session",
    foreignKeys = [ForeignKey(
        entity = Program::class,
        parentColumns = ["id"],
        childColumns = ["programId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("programId"), Index("date")]
)
@TypeConverters(Converters::class)
data class PlannedSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programId: Long,

    /** The date this session is currently scheduled for. */
    val date: String,                   // "yyyy-MM-dd"

    /** The date the session was originally planned (before any reschedule). */
    val originalDate: String,

    val type: SessionType,
    val status: SessionStatus = SessionStatus.PLANNED,

    /**
     * KEY sessions (heavy compound days) get priority in the rescheduler.
     * MINOR sessions (accessories, cardio) can be dropped first.
     */
    val priority: SessionPriority = SessionPriority.KEY,
    val targetMinutes: Int = 60,

    /** Readiness score at the time the session was started (0–100). */
    val readinessAtStart: Int? = null,

    /** User-reported effort after session (1–10). */
    val effortScore: Int? = null,

    /** Any pain reported in post-session check-in. */
    val painReported: String? = null,

    val startedAt: Long? = null,
    val completedAt: Long? = null,

    /** Reason for skipping, from the reason picker. */
    val skipReason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SessionType {
    FULL_BODY,
    UPPER, LOWER,
    PUSH, PULL, LEGS,
    CARDIO, MOBILITY, DELOAD, RE_ENTRY, MINIMUM_VIABLE
}

enum class SessionStatus {
    PLANNED, NOTIFIED, STARTED, DONE, PARTIAL, SKIPPED, MOVED
}

enum class SessionPriority { KEY, MINOR }

// ─── Planned Set ──────────────────────────────────────────────────────────────

/**
 * One exercise slot within a [PlannedSession].
 */
@Entity(
    tableName = "planned_set",
    foreignKeys = [ForeignKey(
        entity = PlannedSession::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class PlannedSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val order: Int,                     // display order within session
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val targetLoad: Float? = null,      // kg; null = bodyweight / auto
    val targetRir: Int = 2             // reps in reserve
)
