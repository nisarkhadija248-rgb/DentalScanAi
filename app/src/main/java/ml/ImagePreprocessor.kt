package com.dentalscan.ml

import android.graphics.Bitmap
import android.graphics.Color

object ImagePreprocessor {

    /**
     * Full preprocessing pipeline without OpenCV:
     * 1. Apply histogram equalization per channel (CLAHE approximation)
     * 2. Normalize pixel values to 0.0–1.0 float array for TFLite
     */
    fun preprocess(bitmap: Bitmap): FloatArray {
        // CLAHE disabled — model trained on raw images
        // Re-enable applyAdaptiveContrast if testing shows improvement
        return normalizeToFloatArray(bitmap)
    }

    /**
     * Adaptive contrast enhancement — approximates CLAHE effect.
     * Builds histogram for each channel and applies equalization.
     * Preserves color balance while improving local contrast.
     */
    private fun applyAdaptiveContrast(bitmap: Bitmap): Bitmap {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            // Build histograms for R, G, B channels
            val histR = IntArray(256)
            val histG = IntArray(256)
            val histB = IntArray(256)

            for (pixel in pixels) {
                histR[(pixel shr 16) and 0xFF]++
                histG[(pixel shr 8) and 0xFF]++
                histB[pixel and 0xFF]++
            }

            // Build cumulative distribution functions (CDF)
            val cdfR = buildCDF(histR, pixels.size)
            val cdfG = buildCDF(histG, pixels.size)
            val cdfB = buildCDF(histB, pixels.size)

            // Apply equalization mapping to each pixel
            val outputPixels = IntArray(pixels.size)
            for (i in pixels.indices) {
                val pixel = pixels[i]
                val a = (pixel shr 24) and 0xFF
                val r = cdfR[(pixel shr 16) and 0xFF]
                val g = cdfG[(pixel shr 8) and 0xFF]
                val b = cdfB[pixel and 0xFF]
                outputPixels[i] = Color.argb(a, r, g, b)
            }

            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            result.setPixels(outputPixels, 0, width, 0, 0, width, height)
            result

        } catch (e: Exception) {
            e.printStackTrace()
            bitmap // safe fallback
        }
    }

    /**
     * Builds a normalized cumulative distribution function
     * for histogram equalization mapping.
     */
    private fun buildCDF(histogram: IntArray, totalPixels: Int): IntArray {
        val cdf = IntArray(256)
        var cumulative = 0
        var cdfMin = -1

        for (i in 0..255) {
            cumulative += histogram[i]
            if (cdfMin == -1 && cumulative > 0) cdfMin = cumulative
            // Normalize to 0-255 range
            cdf[i] = if (totalPixels - cdfMin > 0) {
                ((cumulative - cdfMin).toFloat() / (totalPixels - cdfMin) * 255).toInt()
                    .coerceIn(0, 255)
            } else {
                0
            }
        }
        return cdf
    }

    /**
     * Normalizes bitmap pixels to float array [0.0, 1.0]
     * Format: [R, G, B, R, G, B, ...] for TFLite input
     * 640x640x3 = 1,228,800 floats
     */
    private fun normalizeToFloatArray(bitmap: Bitmap): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val floatArray = FloatArray(width * height * 3)

        var index = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                floatArray[index++] = ((pixel shr 16) and 0xFF) / 255.0f  // R
                floatArray[index++] = ((pixel shr 8) and 0xFF) / 255.0f   // G
                floatArray[index++] = (pixel and 0xFF) / 255.0f            // B
            }
        }
        return floatArray
    }
}