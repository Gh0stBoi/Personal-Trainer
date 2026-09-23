package com.personaltrainer.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single check-in answer.
 *
 * [kind] distinguishes the check-in moment (MORNING, POST_GYM, MEAL, EVENING, etc.).
 * [questionKey] identifies which question within that kind.
 * [answer] is a raw string — could be a number, "yes"/"no", a 1–5 scale, or free text.
 * [source] is "user" for manual answers or "auto" for Health Connect fills.
 */
@Entity(tableName = "checkin")
data class Checkin(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    /** yyyy-MM-dd date this check-in belongs to */
    val date: String,
    val kind: CheckinKind,
    val questionKey: String,
    val answer: String,
    val source: CheckinSource = CheckinSource.USER
)

enum class CheckinKind {
    MORNING,        // sleep, quality, energy, soreness, pain
    PRE_GYM,        // eaten, ready?
    POST_GYM,       // done, effort, pain
    MEAL,           // what did you eat, on-plan?
    EVENING,        // steps, off-plan, water, mood
    WEEKLY,         // weight, waist, photos
    MONTHLY         // full measurements
}

enum class CheckinSource { USER, AUTO }
