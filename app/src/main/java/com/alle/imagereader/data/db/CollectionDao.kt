package com.alle.imagereader.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.alle.imagereader.data.db.models.Collection
import com.alle.imagereader.data.db.models.Screenshot

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collection")
    fun getAll(): List<Collection>

    @Query("SELECT * FROM collection WHERE name IN (:name)")
    fun loadAllByName(name: String): List<Collection>

    @Query("SELECT * FROM Screenshot WHERE file_uri LIKE :fileUri LIMIT 1")
    fun findByName(fileUri:String): Screenshot

    @Insert
    fun insert(screenshot: Screenshot)

    @Insert
    fun insertAll(vararg screenshot: Screenshot)

    @Delete
    fun delete(screenshot: Screenshot)

}