package com.alle.imagereader

import android.app.Application
import androidx.room.Room
import com.alle.imagereader.data.db.AppDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App: Application() {

    lateinit var db:AppDatabase

    override fun onCreate() {
        super.onCreate()

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "imageReader"
        ).build()
    }
}