package com.alle.imagereader.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alle.imagereader.data.db.models.Collection
import com.alle.imagereader.data.db.models.Screenshot

@Dao
interface ScreenshotDao {
    @Query("SELECT * FROM screenshot")
    fun getAll(): List<Screenshot>

    @Query("SELECT * FROM screenshot WHERE id IN (:ids)")
    fun loadAllByIds(ids: Int): List<Screenshot>

    @Query("SELECT * FROM Screenshot WHERE file_uri LIKE :fileUri LIMIT 1")
    fun findByUri(fileUri:String): Screenshot

    @Insert
    fun insert(screenshot: Screenshot)

    @Query("UPDATE screenshot SET note = :note WHERE id =:id")
    fun update(note: String, id: Int)

    @Query("UPDATE screenshot SET collections = :collection WHERE id =:id")
    fun update(collection: ArrayList<String>, id: Int)

    @Insert
    fun insertAll(vararg screenshot: Screenshot)

    @Delete
    fun delete(screenshot: Screenshot)

}