package com.dentalscan.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** Decodes a photo at (roughly) the size it will be shown, to keep memory use low. */
object PhotoLoader {

    /** Call from a background thread. Returns null if the file cannot be decoded. */
    fun decodeSampled(path: String, reqSize: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= reqSize &&
                bounds.outHeight / (sample * 2) >= reqSize
            ) {
                sample *= 2
            }

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(path, opts)
        } catch (e: Exception) {
            null
        }
    }
}
