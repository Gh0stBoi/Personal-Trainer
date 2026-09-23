package com.personaltrainer.data.dao

import androidx.room.*
import com.personaltrainer.data.entities.*
import kotlinx.coroutines.flow.Flow

// ─── Checkin DAO ──────────────────────────────────────────────────────────────

@Dao
interface CheckinDao {

    @Insert
    suspend fun insert(checkin: Checkin): Long

    @Query("SELECT * FROM checkin WHERE date = :date ORDER BY timestamp ASC")
    fun observeByDate(date: String): Flow<List<Checkin>>

    @Query("SELECT * FROM checkin WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getByDate(date: String): List<Checkin>

    @Query("SELECT * FROM checkin WHERE date = :date AND kind = :kind")
    suspend fun getByDateAndKind(date: String, kind: CheckinKind): List<Checkin>

    @Query("SELECT * FROM checkin WHERE date BETWEEN :from AND :to ORDER BY date ASC, timestamp ASC")
    suspend fun getBetween(from: String, to: String): List<Checkin>
}

// ─── Meal Log DAO ─────────────────────────────────────────────────────────────

@Dao
interface MealLogDao {

    @Insert
    suspend fun insert(log: MealLog): Long

    @Update
    suspend fun update(log: MealLog)

    @Delete
    suspend fun delete(log: MealLog)

    @Query("SELECT * FROM meal_log WHERE date = :date ORDER BY slot ASC")
    fun observeByDate(date: String): Flow<List<MealLog>>

    @Query("SELECT * FROM meal_log WHERE date = :date ORDER BY slot ASC")
    suspend fun getByDate(date: String): List<MealLog>

    @Query("SELECT * FROM meal_log WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    suspend fun getBetween(from: String, to: String): List<MealLog>

    /** Weekly calorie totals — used for the off-plan budget calculation. */
    @Query("""
        SELECT date, SUM(kcal) as totalKcal, SUM(proteinG) as totalProtein
        FROM meal_log
        WHERE date BETWEEN :from AND :to
        GROUP BY date
    """)
    suspend fun getDailyTotals(from: String, to: String): List<DailyNutritionTotal>
}

data class DailyNutritionTotal(
    val date: String,
    val totalKcal: Float,
    val totalProtein: Float
)

// ─── Sleep Log DAO ────────────────────────────────────────────────────────────

@Dao
interface SleepLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: SleepLog): Long

    @Query("SELECT * FROM sleep_log WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): SleepLog?

    @Query("SELECT * FROM sleep_log ORDER BY date DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 14): List<SleepLog>

    @Query("SELECT * FROM sleep_log WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    suspend fun getBetween(from: String, to: String): List<SleepLog>
}

// ─── Reminder DAO ─────────────────────────────────────────────────────────────

@Dao
interface ReminderDao {

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Insert
    suspend fun insertAll(reminders: List<Reminder>)

    @Update
    suspend fun update(reminder: Reminder)

    @Query("SELECT * FROM reminder WHERE state = 'PENDING' ORDER BY fireAt ASC")
    suspend fun getPending(): List<Reminder>

    @Query("SELECT * FROM reminder WHERE id = :id")
    suspend fun getById(id: Long): Reminder?

    @Query("UPDATE reminder SET state = :state WHERE id = :id")
    suspend fun setState(id: Long, state: ReminderState)

    @Query("DELETE FROM reminder WHERE state IN ('FIRED', 'DISMISSED', 'CANCELLED') AND createdAt < :before")
    suspend fun pruneOld(before: Long)

    @Query("DELETE FROM reminder WHERE refId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)
}

// ─── Event DAO ────────────────────────────────────────────────────────────────

@Dao
interface AppEventDao {

    @Insert
    suspend fun insert(event: AppEvent): Long

    @Query("SELECT * FROM event ORDER BY ts DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<AppEvent>

    @Query("SELECT * FROM event WHERE type = :type ORDER BY ts DESC LIMIT :limit")
    suspend fun getByType(type: String, limit: Int = 50): List<AppEvent>

    @Query("SELECT * FROM event WHERE ts BETWEEN :from AND :to ORDER BY ts ASC")
    suspend fun getBetween(from: Long, to: Long): List<AppEvent>
}

// ─── LLM Cache DAO ────────────────────────────────────────────────────────────

@Dao
interface LlmCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: LlmCache)

    @Query("SELECT * FROM llm_cache WHERE inputHash = :hash LIMIT 1")
    suspend fun get(hash: String): LlmCache?

    @Query("DELETE FROM llm_cache WHERE createdAt < :before")
    suspend fun pruneOlderThan(before: Long)
}
