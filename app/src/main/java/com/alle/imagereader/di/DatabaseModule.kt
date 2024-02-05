package com.alle.imagereader.di

import android.content.Context
import androidx.room.Room
import com.alle.imagereader.data.db.AppDatabase
import com.alle.imagereader.data.db.CollectionDao
import com.alle.imagereader.data.db.ScreenshotDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Singleton
    @Provides
    fun provideRoomDb(
        @ApplicationContext context: Context
    ) : AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "imageReader"
        ).build()
    }

    @Singleton
    @Provides
    fun provideScreenshotDao(appDatabase: AppDatabase): ScreenshotDao {
        return appDatabase.screenshotDao()
    }

    @Singleton
    @Provides
    fun provideCollectionDao(appDatabase: AppDatabase): CollectionDao {
        return appDatabase.collectionDao()
    }
}