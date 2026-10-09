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
    ],
    version = 1,
    exportSchema = false,
)
abstract class MiHealthDatabase : RoomDatabase() {
    abstract fun stepSampleDao(): StepSampleDao
    abstract fun sleepDao(): SleepDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        fun build(context: Context): MiHealthDatabase =
            Room.databaseBuilder(context, MiHealthDatabase::class.java, "mihealth.db").build()
    }
}
