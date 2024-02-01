package com.alle.imagereader.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.alle.imagereader.data.db.models.Collection
import com.alle.imagereader.data.db.models.Screenshot

@Database(entities = [Screenshot::class, Collection::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun screenshotDao(): ScreenshotDao
    abstract fun collectionDao(): CollectionDao
}