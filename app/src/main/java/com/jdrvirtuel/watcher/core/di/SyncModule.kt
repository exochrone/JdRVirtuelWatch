package com.jdrvirtuel.watcher.core.di

import com.jdrvirtuel.watcher.domain.repository.NewContentNotifier
import com.jdrvirtuel.watcher.notification.SystemNewContentNotifier
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
        systemNewContentNotifier: SystemNewContentNotifier
    ): NewContentNotifier
}
