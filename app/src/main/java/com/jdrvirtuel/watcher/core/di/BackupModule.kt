package com.jdrvirtuel.watcher.core.di

import com.jdrvirtuel.watcher.data.repository.BackupRepositoryImpl
import com.jdrvirtuel.watcher.domain.repository.BackupRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {
    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}
