package com.alle.imagereader.di

import com.alle.imagereader.data.db.ScreenshotDao
import com.alle.imagereader.data.db.ScreenshotRepoImpl
import com.alle.imagereader.domain.models.ScreenshotRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideScreenshotRepo(
        screenshotDao: ScreenshotDao
    ): ScreenshotRepo = ScreenshotRepoImpl(
        screenshotDao = screenshotDao
    )
}