package com.alle.imagereader.data.repo

import com.alle.imagereader.data.db.ScreenshotDao
import com.alle.imagereader.data.db.models.Screenshot
import com.alle.imagereader.domain.repo.ScreenshotRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ScreenshotRepoImpl @Inject constructor(
    private val screenshotDao: ScreenshotDao
): ScreenshotRepo {
    override fun getAll(): Flow<List<Screenshot>> {
        return flow {
            emit(screenshotDao.getAll())
        }
    }

    override fun loadAllByIds(ids: Int): Flow<List<Screenshot>> {
        TODO("Not yet implemented")
    }

    override fun findByUri(fileUri: String): Flow<Screenshot?> {
        return flow {
            emit(screenshotDao.findByUri(fileUri))
        }
    }

    override fun insert(screenshot: Screenshot) {
        screenshotDao.insert(screenshot)
    }

    override fun updateNote(screenshot: Screenshot) {
        screenshotDao.update(note = screenshot.note, id = screenshot.id)
    }

    override fun updateCollection(screenshot: Screenshot) {
        screenshotDao.update(collection = screenshot.collections, id = screenshot.id)
    }

    override fun insertAll(vararg screenshot: Screenshot) {
    }

    override fun delete(screenshot: Screenshot) {
        screenshotDao.delete(screenshot)
    }
}