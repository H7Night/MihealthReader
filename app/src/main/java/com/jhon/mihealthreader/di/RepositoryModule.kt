package com.jhon.mihealthreader.di

import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.local.LocalStore
import com.jhon.mihealthreader.local.MiHealthDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun localStore(db: MiHealthDatabase): LocalStore = LocalStore(db)

    @Provides
    @Singleton
    fun parsedRepository(localStore: LocalStore): ParsedRepository = ParsedRepository(localStore)
}
