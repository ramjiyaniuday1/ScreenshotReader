package com.alle.imagereader.data.db.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Screenshot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo("file_uri")
    val fileUri: String,

    @ColumnInfo("file_id") val fileId: Long,
    @ColumnInfo("name") val name: String,

    @ColumnInfo("description") val description: String = "",
    @ColumnInfo("note") val note: String = "",

    @ColumnInfo("collections") val collections: ArrayList<String> = arrayListOf()

)
