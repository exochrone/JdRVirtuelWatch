package com.jdrvirtuel.watcher.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.jdrvirtuel.watcher.data.local.dao.ForumDao
import com.jdrvirtuel.watcher.data.local.dao.TopicDao
import com.jdrvirtuel.watcher.data.local.db.AppDatabase
import com.jdrvirtuel.watcher.data.local.db.DatabaseSeeder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        seederProvider: Provider<DatabaseSeeder>,
        @ApplicationScope applicationScope: CoroutineScope
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "jdrvirtuel_watcher.db"
        )
        .addCallback(object : androidx.room.RoomDatabase.Callback() {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onCreate(db)
                applicationScope.launch {
                    seederProvider.get().seedIfEmpty()
                }
            }
        })
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
    }

    @Provides
    fun provideForumDao(database: AppDatabase): ForumDao = database.forumDao()

    @Provides
    fun provideTopicDao(database: AppDatabase): TopicDao = database.topicDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("jdrvirtuel_watcher_prefs") }
        )
    }
}
