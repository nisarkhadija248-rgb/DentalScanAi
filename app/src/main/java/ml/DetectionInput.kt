package com.dentalscan.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Turns a photo of any shape into the 640x640 picture the disease detector expects, and puts the
 * detector's boxes back onto the original photo.
 *
 * There are two ways to make a non-square photo square:
 *  - STRETCH   : squash it to 640x640 (the shape changes). This is what the app always did.
 *  - LETTERBOX : keep the shape, shrink to fit and fill the rest with grey (what YOLO/Ultralytics
 *                does by default).
 * The detector should be fed the way its training images were prepared. [MODE] is that choice.
 */
object DetectionInput {

    enum class Mode { STRETCH, LETTERBOX }

    /** Change to LETTERBOX if the detector was trained on letterboxed images (see the notes in the app docs). */
    val MODE = Mode.STRETCH

    private const val SIZE = 640
    private const val GREY = 114 // YOLO's padding colour

    /** The 640x640 picture, plus where the original photo sits inside it. */
    class Prepared(
        val bitmap: Bitmap,
        /** Left / top of the photo inside the 640x640 picture (pixels). */
        val padX: Float,
        val padY: Float,
        /** Size of the photo inside the 640x640 picture (pixels). */
        val contentWidth: Float,
        val contentHeight: Float
    )

    fun prepare(source: Bitmap): Prepared {
        if (MODE == Mode.STRETCH) {
            val squared = if (source.width == SIZE && source.height == SIZE) source
            else Bitmap.createScaledBitmap(source, SIZE, SIZE, true)
            return Prepared(squared, 0f, 0f, SIZE.toFloat(), SIZE.toFloat())
        }

        val scale = SIZE.toFloat() / max(source.width, source.height)
        val w = (source.width * scale).roundToInt().coerceIn(1, SIZE)
        val h = (source.height * scale).roundToInt().coerceIn(1, SIZE)
        val padX = (SIZE - w) / 2
        val padY = (SIZE - h) / 2

        val canvasBitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)
        canvas.drawColor(Color.rgb(GREY, GREY, GREY))
        canvas.drawBitmap(
            Bitmap.createScaledBitmap(source, w, h, true),
            padX.toFloat(), padY.toFloat(), null
        )
        return Prepared(canvasBitmap, padX.toFloat(), padY.toFloat(), w.toFloat(), h.toFloat())
    }

    /** A detection on the prepared picture, converted to boxes relative to the original photo (0..1). */
    fun toSource(d: TFLiteHelper.Detection, p: Prepared): TFLiteHelper.Detection {
        fun x(v: Float) = ((v * SIZE - p.padX) / p.contentWidth).coerceIn(0f, 1f)
        fun y(v: Float) = ((v * SIZE - p.padY) / p.contentHeight).coerceIn(0f, 1f)
        return TFLiteHelper.Detection(
            d.classId, d.className, d.confidence,
            RectF(x(d.boundingBox.left), y(d.boundingBox.top), x(d.boundingBox.right), y(d.boundingBox.bottom))
        )
    }

    /** Shrinks a photo so its longest side is at most [maxSide], keeping its shape. */
    fun scaleToMax(source: Bitmap, maxSide: Int): Bitmap {
        val longest = max(source.width, source.height)
        if (longest <= maxSide) return source
        val scale = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(
            source,
            max(1, (source.width * scale).roundToInt()),
            max(1, min(maxSide, (source.height * scale).roundToInt())).coerceAtLeast(1),
            true
        )
    }
}
