package com.personaltrainer.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Body measurement snapshot.  One row per measurement session.
 * All measurement columns are nullable because you might only log weight.
 */
@Entity(tableName = "body_metric")
data class BodyMetric(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** yyyy-MM-dd formatted date string for easy daily grouping */
    val date: String,                   // e.g. "2026-09-23"

    val weightKg: Float? = null,
    val waistCm: Float? = null,
    val chestCm: Float? = null,
    val armCm: Float? = null,           // dominant arm, flexed
    val thighCm: Float? = null,         // dominant thigh, standing
    val hipsCm: Float? = null,
    val neckCm: Float? = null,

    /** URI to a progress photo stored in app-private storage. */
    val photoUri: String? = null,

    /**
     * Where the data came from.
     * "manual" = typed by user, "health_connect" = synced from Health Connect.
     */
    val source: String = "manual",

    val createdAt: Long = System.currentTimeMillis()
)
