package com.alle.imagereader.ui.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory

object ImageUtils {

    fun convertCompressedByteArrayToBitmap(src: ByteArray): Bitmap? {
        return BitmapFactory.decodeByteArray(src, 0, src.size)
    }
}