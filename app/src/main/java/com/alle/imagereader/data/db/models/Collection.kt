package com.alle.imagereader.data.db.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.Gson

@Entity(indices = [Index(value = ["name"], unique = true)])
data class Collection(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo("name") val name: String,
) {
    override fun toString(): String {
        return Gson().toJson(this)
    }
}
