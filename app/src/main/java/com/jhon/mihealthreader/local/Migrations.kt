package com.jhon.mihealthreader.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Additive-only migrations. There is deliberately no destructive fallback: the
 * extracted snapshot is the user's own health history, and silently dropping it
 * on a schema mismatch is unacceptable. A missing migration must fail loudly.
 *
 * SQL is exposed as constants so a plain-JVM test can pin the exact generated
 * shape without opening a real database.
 */

/** v1 -> v2: introduced the metric/workout/body tables. Existing tables unchanged. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_METRIC_SAMPLE_SQL)
        db.execSQL(CREATE_WORKOUT_SQL)
        db.execSQL(CREATE_BODY_MEASUREMENT_SQL)
    }
}

internal const val CREATE_METRIC_SAMPLE_SQL =
    "CREATE TABLE IF NOT EXISTS `metric_sample` (`metricType` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `time` INTEGER NOT NULL, `value` REAL NOT NULL, PRIMARY KEY(`metricType`, `sourceId`, `time`))"

internal const val CREATE_WORKOUT_SQL =
    "CREATE TABLE IF NOT EXISTS `workout` (`sourceId` TEXT NOT NULL, `start` INTEGER NOT NULL, `end` INTEGER NOT NULL, `durationSec` INTEGER NOT NULL, `calories` INTEGER NOT NULL, `avgHr` INTEGER, `maxHr` INTEGER, `minHr` INTEGER, `distanceMeters` INTEGER, `type` TEXT NOT NULL, `typeName` TEXT NOT NULL, PRIMARY KEY(`sourceId`, `start`))"

internal const val CREATE_BODY_MEASUREMENT_SQL =
    "CREATE TABLE IF NOT EXISTS `body_measurement` (`sourceId` TEXT NOT NULL, `time` INTEGER NOT NULL, `weightKg` REAL, `bmi` REAL, `bodyFatRate` REAL, `muscleKg` REAL, `waterPct` REAL, `boneKg` REAL, `bmrKcal` REAL, `visceralFat` REAL, `proteinRate` REAL, PRIMARY KEY(`sourceId`, `time`))"

/** The complete migration set registered with the database builder. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
