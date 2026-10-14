package com.jhon.mihealthreader.di

import android.content.Context
import com.jhon.mihealthreader.local.BodyMeasurementDao
import com.jhon.mihealthreader.local.MetricDao
import com.jhon.mihealthreader.local.MiHealthDatabase
import com.jhon.mihealthreader.local.SleepDao
import com.jhon.mihealthreader.local.StepSampleDao
import com.jhon.mihealthreader.local.SyncStateDao
import com.jhon.mihealthreader.local.WorkoutDao
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
    fun database(@ApplicationContext context: Context): MiHealthDatabase =
        MiHealthDatabase.build(context)

    @Provides
    fun stepSampleDao(db: MiHealthDatabase): StepSampleDao = db.stepSampleDao()

    @Provides
    fun sleepDao(db: MiHealthDatabase): SleepDao = db.sleepDao()

    @Provides
    fun syncStateDao(db: MiHealthDatabase): SyncStateDao = db.syncStateDao()

    @Provides
    fun metricDao(db: MiHealthDatabase): MetricDao = db.metricDao()

    @Provides
    fun workoutDao(db: MiHealthDatabase): WorkoutDao = db.workoutDao()

    @Provides
    fun bodyMeasurementDao(db: MiHealthDatabase): BodyMeasurementDao = db.bodyMeasurementDao()
}
