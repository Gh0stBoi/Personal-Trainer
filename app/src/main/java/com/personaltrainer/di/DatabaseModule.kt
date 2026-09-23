package com.personaltrainer.di

import android.content.Context
import androidx.room.Room
import com.personaltrainer.data.AppDatabase
import com.personaltrainer.data.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()   // Replace with proper migration in production
        .build()
    }

    @Provides fun provideUserProfileDao(db: AppDatabase) = db.userProfileDao()
    @Provides fun provideBodyMetricDao(db: AppDatabase) = db.bodyMetricDao()
    @Provides fun provideExerciseDao(db: AppDatabase) = db.exerciseDao()
    @Provides fun provideProgramDao(db: AppDatabase) = db.programDao()
    @Provides fun providePlannedSessionDao(db: AppDatabase) = db.plannedSessionDao()
    @Provides fun providePlannedSetDao(db: AppDatabase) = db.plannedSetDao()
    @Provides fun provideSetLogDao(db: AppDatabase) = db.setLogDao()
    @Provides fun provideCheckinDao(db: AppDatabase) = db.checkinDao()
    @Provides fun provideMealLogDao(db: AppDatabase) = db.mealLogDao()
    @Provides fun provideSleepLogDao(db: AppDatabase) = db.sleepLogDao()
    @Provides fun provideReminderDao(db: AppDatabase) = db.reminderDao()
    @Provides fun provideAppEventDao(db: AppDatabase) = db.appEventDao()
    @Provides fun provideLlmCacheDao(db: AppDatabase) = db.llmCacheDao()
}
