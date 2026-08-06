package com.jdrvirtuel.watcher.core.di

import com.jdrvirtuel.watcher.data.repository.NoOpNewContentNotifier
import com.jdrvirtuel.watcher.domain.repository.NewContentNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindNewContentNotifier(
        noOpNewContentNotifier: NoOpNewContentNotifier
    ): NewContentNotifier
}
