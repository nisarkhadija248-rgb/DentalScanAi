package com.dentalscan.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object ImageCompressor {

    private const val TARGET_SIZE = 640
    private const val JPEG_QUALITY = 75

    /**
     * Compresses and resizes an image file to 640x640 at 75% JPEG quality.
     * Automatically corrects rotation using EXIF data.
     * Returns the path of the compressed file (replaces the original).
     * Falls back to original path if anything fails.
     */
    fun compressAndSave(originalPath: String): String {
        return try {
            // Read EXIF rotation before decoding
            val rotation = getExifRotation(originalPath)

            // Decode original bitmap
            val original = BitmapFactory.decodeFile(originalPath)
                ?: return originalPath // fallback if decode fails

            // Fix rotation if needed
            val rotated = if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
                    .also { if (it != original) original.recycle() }
            } else {
                original
            }

            // Resize to 640x640
            val resized = Bitmap.createScaledBitmap(rotated, TARGET_SIZE, TARGET_SIZE, true)
                .also { if (it != rotated) rotated.recycle() }

            // Save compressed JPEG back to same path
            val outputFile = File(originalPath)
            FileOutputStream(outputFile).use { out ->
                resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            resized.recycle()

            outputFile.absolutePath

        } catch (e: Exception) {
            e.printStackTrace()
            originalPath // safe fallback — returns original if anything fails
        }
    }

    private fun getExifRotation(imagePath: String): Float {
        return try {
            val exif = ExifInterface(imagePath)
            when (exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: Exception) {
            0f
        }
    }
}