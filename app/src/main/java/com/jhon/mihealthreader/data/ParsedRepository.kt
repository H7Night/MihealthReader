package com.jhon.mihealthreader.data

import com.jhon.mihealthreader.local.LocalStore
import com.jhon.mihealthreader.parse.ActivityParser
import com.jhon.mihealthreader.parse.DailyActivity
import com.jhon.mihealthreader.parse.SleepSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ParsedRepository(private val localStore: LocalStore) {

    fun dailyActivity(): Flow<List<DailyActivity>> =
        localStore.stepSamplesFlow().map { samples ->
            ActivityParser.aggregate(samples).sortedByDescending { it.date }
        }

    fun sleepSessions(): Flow<List<SleepSession>> = localStore.sleepSessionsFlow()
}
