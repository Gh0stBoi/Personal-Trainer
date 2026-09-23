package com.personaltrainer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.personaltrainer.data.dao.*
import com.personaltrainer.data.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfile::class,
        BodyMetric::class,
        Exercise::class,
        Program::class,
        PlannedSession::class,
        PlannedSet::class,
        SetLog::class,
        Checkin::class,
        MealLog::class,
        SleepLog::class,
        Reminder::class,
        AppEvent::class,
        LlmCache::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun bodyMetricDao(): BodyMetricDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun programDao(): ProgramDao
    abstract fun plannedSessionDao(): PlannedSessionDao
    abstract fun plannedSetDao(): PlannedSetDao
    abstract fun setLogDao(): SetLogDao
    abstract fun checkinDao(): CheckinDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun sleepLogDao(): SleepLogDao
    abstract fun reminderDao(): ReminderDao
    abstract fun appEventDao(): AppEventDao
    abstract fun llmCacheDao(): LlmCacheDao

    companion object {
        const val DATABASE_NAME = "personal_trainer.db"
    }
}
