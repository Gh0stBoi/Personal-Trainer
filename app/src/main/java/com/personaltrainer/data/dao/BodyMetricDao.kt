package com.personaltrainer.data.dao

import androidx.room.*
import com.personaltrainer.data.entities.BodyMetric
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMetricDao {

    @Insert
    suspend fun insert(metric: BodyMetric): Long

    @Query("SELECT * FROM body_metric ORDER BY date DESC")
    fun observeAll(): Flow<List<BodyMetric>>

    @Query("SELECT * FROM body_metric ORDER BY date DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 30): List<BodyMetric>

    @Query("SELECT * FROM body_metric WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    suspend fun getBetween(from: String, to: String): List<BodyMetric>

    @Query("SELECT * FROM body_metric WHERE weightKg IS NOT NULL ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentWeights(limit: Int = 90): List<BodyMetric>

    @Query("SELECT * FROM body_metric ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): BodyMetric?

    @Delete
    suspend fun delete(metric: BodyMetric)
}
