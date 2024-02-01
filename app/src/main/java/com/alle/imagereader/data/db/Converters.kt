package com.alle.imagereader.data.db

import androidx.room.TypeConverter
import com.alle.imagereader.data.db.models.Collection
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

inline fun <reified T> Gson.fromJson(json: String) =
    fromJson<T>(json, object : TypeToken<T>() {}.type)

class Converters {
   /* @TypeConverter
    fun collectionToString(collection: Collection?): String? {
        return collection?.toString()
    }

    @TypeConverter
    fun stringToCollection( jsonString: String?): Collection?{
        return if (jsonString !=null) Gson().fromJson(jsonString, Collection::class.java) else null
    }*/
    @TypeConverter
    fun fromStringArrayList(value: ArrayList<String>): String {

        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringArrayList(value: String): ArrayList<String> {
        return try {
            Gson().fromJson<ArrayList<String>>(value) //using extension function
        } catch (e: Exception) {
            arrayListOf()
        }
    }
}
