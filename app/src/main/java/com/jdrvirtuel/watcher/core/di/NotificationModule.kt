package com.jdrvirtuel.watcher.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {
    // AppNotifier and SystemNewContentNotifier use @Inject constructor and @Singleton
    // They are automatically discovered by Hilt.
}
