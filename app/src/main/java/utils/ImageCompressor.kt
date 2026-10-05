package com.dentalscan.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ImageCompressor {

    /**
     * Longest side of a saved photo. The mouth is cut out of this photo for the disease detector, so a
     * sharp photo matters (it used to be shrunk to 640x640 here, which threw the detail away).
     */
    private const val MAX_SIDE = 1280
    private const val JPEG_QUALITY = 90

    /**
     * Rotates the photo upright (EXIF), shrinks it to at most 1280 pixels on the longest side keeping its
     * shape, and saves it as a JPEG over the original file.
     * Returns the path of the saved file, or the original path if anything fails.
     */
    fun compressAndSave(originalPath: String): String {
        return try {
            val rotation = getExifRotation(originalPath)

            // Decode already shrunk (a phone photo can be 12 megapixels), but never below the target size
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(originalPath, bounds)
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val original = BitmapFactory.decodeFile(
                originalPath, BitmapFactory.Options().apply { inSampleSize = sample }
            ) ?: return originalPath // fallback if decode fails

            // Fix rotation if needed
            val rotated = if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
                    .also { if (it != original) original.recycle() }
            } else {
                original
            }

            // Shrink to MAX_SIDE, keeping the shape
            val longest = max(rotated.width, rotated.height)
            val resized = if (longest > MAX_SIDE) {
                val scale = MAX_SIDE.toFloat() / longest
                Bitmap.createScaledBitmap(
                    rotated,
                    (rotated.width * scale).roundToInt().coerceAtLeast(1),
                    (rotated.height * scale).roundToInt().coerceAtLeast(1),
                    true
                ).also { if (it != rotated) rotated.recycle() }
            } else {
                rotated
            }

            // Save the JPEG back to the same path
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
