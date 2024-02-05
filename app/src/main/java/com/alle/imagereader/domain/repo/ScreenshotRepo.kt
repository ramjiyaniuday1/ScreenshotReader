package com.alle.imagereader.domain.repo

import com.alle.imagereader.data.db.models.Screenshot
import kotlinx.coroutines.flow.Flow

interface ScreenshotRepo {

    fun getAll(): Flow<List<Screenshot>>

    fun loadAllByIds(ids: Int): Flow<List<Screenshot>>

    fun findByUri(fileUri:String): Flow<Screenshot?>

    fun insert(screenshot: Screenshot)

    fun updateNote(screenshot: Screenshot)

    fun updateCollection(screenshot: Screenshot)

    fun insertAll(vararg screenshot: Screenshot)

    fun delete(screenshot: Screenshot)
}