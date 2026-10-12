package com.jhon.mihealthreader.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StepSampleEntity::class,
        SleepSessionEntity::class,
        SleepStageEntity::class,
        SyncStateEntity::class,
        MetricSampleEntity::class,
        WorkoutEntity::class,
        BodyMeasurementEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class MiHealthDatabase : RoomDatabase() {
    abstract fun stepSampleDao(): StepSampleDao
    abstract fun sleepDao(): SleepDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun metricDao(): MetricDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao

    companion object {
        fun build(context: Context): MiHealthDatabase =
            Room.databaseBuilder(context, MiHealthDatabase::class.java, "mihealth.db")
                .addMigrations(*ALL_MIGRATIONS)
                .build()
    }
}
