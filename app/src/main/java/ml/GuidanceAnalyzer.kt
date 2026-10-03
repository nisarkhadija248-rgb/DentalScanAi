package com.dentalscan.ml

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * Live camera guidance. Looks at the camera frames with simple image statistics (no AI model):
 *  - too dark   : average brightness is low
 *  - too blurry : the image has very little fine detail (low variance of the Laplacian)
 *  - teeth      : a rough colour check on the middle of the frame, for a bright area with little
 *                 colour (white / slightly yellow), the way teeth look. It is NOT a teeth detector:
 *                 a white wall or a bright light will also pass, and teeth in strong colour casts
 *                 can fail. The detection model itself runs later, on the saved photos.
 *
 * If anything goes wrong while reading a frame, it reports "all good" so a bug here can never
 * block the user from taking a photo.
 *
 * The thresholds below were chosen without testing on the phone. If the Capture button stays
 * disabled in good conditions, lower them (or set ENFORCE_BLUR_CHECK / ENFORCE_TEETH_CHECK to false).
 */
class GuidanceAnalyzer(private val listener: (GuidanceResult) -> Unit) : ImageAnalysis.Analyzer {

    data class GuidanceResult(
        val hasTeeth: Boolean = true,
        val teethConfidence: Float = 1.0f,
        val isTooDark: Boolean = false,
        val isTooBlurry: Boolean = false,
        val isTooFar: Boolean = false,
        val isTooClose: Boolean = false
    )

    companion object {
        private const val MIN_INTERVAL_MS = 150L

        // Average luma (0..255) below this = too dark
        private const val DARK_LUMA = 55f
        // Laplacian variance below this = too blurry (typical sharp frames are in the hundreds)
        private const val BLUR_VARIANCE = 25f
        // Share of the middle area that must look like teeth: >= HAS_TEETH to count as visible,
        // >= GOOD_RATIO (teethConfidence > 0.7) to enable Capture
        private const val HAS_TEETH_RATIO = 0.03f
        private const val FULL_CONFIDENCE_RATIO = 0.12f

        // Safety switches
        private const val ENFORCE_BLUR_CHECK = true
        private const val ENFORCE_TEETH_CHECK = true

        private const val SMOOTHING = 0.4f // weight of the newest frame
    }

    private var lastRun = 0L
    private var avgLuma = -1f
    private var avgSharp = -1f
    private var avgRatio = -1f

    override fun analyze(imageProxy: ImageProxy) {
        try {
            val now = SystemClock.elapsedRealtime()
            if (now - lastRun < MIN_INTERVAL_MS) return
            lastRun = now
            listener(measure(imageProxy))
        } catch (e: Exception) {
            listener(GuidanceResult()) // fail open
        } finally {
            imageProxy.close()
        }
    }

    private fun smooth(old: Float, new: Float) = if (old < 0f) new else old + SMOOTHING * (new - old)

    private fun measure(image: ImageProxy): GuidanceResult {
        val planes = image.planes
        if (planes.size < 3) return GuidanceResult()

        val w = image.width
        val h = image.height
        val yBuf = planes[0].buffer
        val yRow = planes[0].rowStride
        val yPix = planes[0].pixelStride
        val uBuf = planes[1].buffer
        val vBuf = planes[2].buffer
        val cRow = planes[1].rowStride
        val cPix = planes[1].pixelStride

        fun luma(x: Int, y: Int): Int = yBuf.get(y * yRow + x * yPix).toInt() and 0xFF

        // Brightness and sharpness over the central 70% of the frame
        val x0 = (w * 0.15f).toInt()
        val x1 = (w * 0.85f).toInt()
        val y0 = (h * 0.15f).toInt()
        val y1 = (h * 0.85f).toInt()
        val step = 4

        var lumaSum = 0L
        var count = 0
        var lapSum = 0.0
        var lapSqSum = 0.0
        var lapCount = 0
        var y = y0
        while (y < y1) {
            var x = x0
            while (x < x1) {
                val c = luma(x, y)
                lumaSum += c
                count++
                if (x >= 2 && y >= 2 && x < w - 2 && y < h - 2) {
                    val lap = (4 * c - luma(x - 2, y) - luma(x + 2, y) - luma(x, y - 2) - luma(x, y + 2)).toDouble()
                    lapSum += lap
                    lapSqSum += lap * lap
                    lapCount++
                }
                x += step
            }
            y += step
        }
        if (count == 0 || lapCount == 0) return GuidanceResult()

        val mean = lumaSum.toFloat() / count
        val lapMean = lapSum / lapCount
        val sharp = ((lapSqSum / lapCount) - lapMean * lapMean).toFloat()

        // Teeth-like pixels in the middle 50%: bright, with little colour (slightly yellow-white)
        val tx0 = (w * 0.25f).toInt()
        val tx1 = (w * 0.75f).toInt()
        val ty0 = (h * 0.25f).toInt()
        val ty1 = (h * 0.75f).toInt()
        var toothLike = 0
        var samples = 0
        y = ty0
        while (y < ty1) {
            var x = tx0
            while (x < tx1) {
                val lum = luma(x, y)
                val ci = (y / 2) * cRow + (x / 2) * cPix
                val u = (uBuf.get(ci).toInt() and 0xFF) - 128
                val v = (vBuf.get(ci).toInt() and 0xFF) - 128
                if (lum > 110 && u in -20..8 && v in -12..14) toothLike++
                samples++
                x += 6
            }
            y += 6
        }
        val ratio = if (samples == 0) 0f else toothLike.toFloat() / samples

        avgLuma = smooth(avgLuma, mean)
        avgSharp = smooth(avgSharp, sharp)
        avgRatio = smooth(avgRatio, ratio)

        val dark = avgLuma < DARK_LUMA
        val blurry = ENFORCE_BLUR_CHECK && avgSharp < BLUR_VARIANCE
        val hasTeeth = !ENFORCE_TEETH_CHECK || avgRatio >= HAS_TEETH_RATIO
        val confidence = if (ENFORCE_TEETH_CHECK) (avgRatio / FULL_CONFIDENCE_RATIO).coerceIn(0f, 1f) else 1f

        return GuidanceResult(
            hasTeeth = hasTeeth,
            teethConfidence = confidence,
            isTooDark = dark,
            isTooBlurry = blurry,
            // Teeth are visible but small in the frame: ask the user to come closer
            isTooFar = hasTeeth && ENFORCE_TEETH_CHECK && confidence <= 0.7f
        )
    }
}
