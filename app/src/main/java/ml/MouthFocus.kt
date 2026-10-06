package com.dentalscan.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

/**
 * "Only analyse the mouth": photos often contain eyes, skin or background, which the disease detector
 * can mistake for caries. This finds the part of a photo where teeth are, so the detector is run on
 * that part only.
 *
 *  1. The teeth classifier ([TeethGate]) scores a grid of windows over the photo (two sizes, 9 positions each).
 *  2. The windows that look like teeth are joined into one region, with a margin so that gums
 *     (gingivitis) and the gumline (calculus) stay inside.
 *  3. The detector runs on that region, and its boxes are mapped back to the full photo.
 *
 * If no window looks like teeth, or the region is nearly the whole photo, [findRegion] returns null and the
 * caller analyses the whole photo as before. So this can only remove false findings outside the mouth,
 * it never blocks an analysis.
 */
object MouthFocus {

    /**
     * Master switch: false = always analyse the whole photo (the old behaviour).
     *
     * OFF on purpose. Tested with the real teeth model on close-up photos: almost every window scores
     * as "teeth" (0.96 to 1.00, even over a nostril), because the classifier recognises "a dental
     * photo" but cannot say WHERE the teeth are. So the joined region is the whole photo and cropping
     * never happens, while the 18 extra scorings cost time. It would need a real localiser
     * (a teeth detector, or face landmarks) to be useful.
     */
    const val ENABLED = false

    private const val MIN_SCORE = 0.6f        // a window must look at least this much like teeth
    private const val KEEP_WITHIN = 0.2f      // ...and be within this of the best window
    private const val MARGIN = 0.08f          // extra border around the joined windows (gums, gumline)
    private const val MAX_AREA = 0.8f         // region bigger than this share of the photo: no point cropping
    private val WINDOW_SIZES = floatArrayOf(0.5f, 0.7f)

    /** A part of the photo as fractions (0..1) of its width and height. */
    class Region(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val width get() = right - left
        val height get() = bottom - top
        val area get() = width * height
    }

    private class Window(val region: Region, val score: Float)

    /** The part of [image] to analyse, or null to analyse all of it. */
    fun findRegion(gate: TeethGate, image: Bitmap): Region? {
        val windows = ArrayList<Window>()
        for (size in WINDOW_SIZES) {
            val step = (1f - size) / 2f // three positions along each side: start, middle, end
            for (iy in 0..2) for (ix in 0..2) {
                val left = ix * step
                val top = iy * step
                val region = Region(left, top, left + size, top + size)
                windows += Window(region, gate.scoreRegion(image, region.left, region.top, region.right, region.bottom))
            }
        }

        val best = windows.maxByOrNull { it.score } ?: return null
        if (best.score < MIN_SCORE) return null

        val keep = windows.filter { it.score >= max(MIN_SCORE, best.score - KEEP_WITHIN) }
        val left = max(0f, keep.minOf { it.region.left } - MARGIN)
        val top = max(0f, keep.minOf { it.region.top } - MARGIN)
        val right = min(1f, keep.maxOf { it.region.right } + MARGIN)
        val bottom = min(1f, keep.maxOf { it.region.bottom } + MARGIN)
        val region = Region(left, top, right, bottom)

        return if (region.area >= MAX_AREA) null else region
    }

    /**
     * Grows [region] (fractions of the photo) so that its shape matches the photo's own shape
     * (for example 3:4), keeping the same centre. The detector squashes whatever it is given to a
     * square, and its training photos were squashed from photos of that shape, so a crop with the
     * same shape is squashed the same way and the teeth do not look stretched.
     */
    fun matchAspect(region: Region, imageW: Int, imageH: Int): Region {
        val target = imageW.toFloat() / imageH
        var w = region.width * imageW
        var h = region.height * imageH
        if (w / h > target) h = w / target else w = h * target
        if (w > imageW) { w = imageW.toFloat(); h = w / target }
        if (h > imageH) { h = imageH.toFloat(); w = h * target }

        val cx = (region.left + region.right) / 2f * imageW
        val cy = (region.top + region.bottom) / 2f * imageH
        val left = (cx - w / 2f).coerceIn(0f, max(0f, imageW - w))
        val top = (cy - h / 2f).coerceIn(0f, max(0f, imageH - h))
        return Region(left / imageW, top / imageH, (left + w) / imageW, (top + h) / imageH)
    }

    /** The region cut out of [image] at the image's own resolution (not resized). */
    fun cut(image: Bitmap, region: Region): Bitmap {
        val x = (region.left * image.width).toInt().coerceIn(0, image.width - 1)
        val y = (region.top * image.height).toInt().coerceIn(0, image.height - 1)
        val w = (region.width * image.width).toInt().coerceIn(1, image.width - x)
        val h = (region.height * image.height).toInt().coerceIn(1, image.height - y)
        return Bitmap.createBitmap(image, x, y, w, h)
    }

    /** A detection made on the cropped region, with its box converted to the full photo. */
    fun toFullPhoto(d: TFLiteHelper.Detection, region: Region): TFLiteHelper.Detection =
        TFLiteHelper.Detection(
            d.classId,
            d.className,
            d.confidence,
            RectF(
                region.left + d.boundingBox.left * region.width,
                region.top + d.boundingBox.top * region.height,
                region.left + d.boundingBox.right * region.width,
                region.top + d.boundingBox.bottom * region.height
            )
        )

    /** A copy of [image] with a thin dashed outline around the area that was analysed. */
    fun withOutline(image: Bitmap, region: Region): Bitmap {
        val copy = image.copy(Bitmap.Config.ARGB_8888, true)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.WHITE
            pathEffect = DashPathEffect(floatArrayOf(18f, 12f), 0f)
        }
        Canvas(copy).drawRoundRect(
            RectF(
                region.left * copy.width, region.top * copy.height,
                region.right * copy.width, region.bottom * copy.height
            ),
            12f, 12f, paint
        )
        return copy
    }
}
