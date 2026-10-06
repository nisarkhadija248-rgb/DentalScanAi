package com.dentalscan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.max
import kotlin.math.min

/**
 * An ImageView you can zoom (pinch, or double-tap) and move (drag) once zoomed in.
 * It starts "fit to view" like a normal ImageView, so it looks the same until the user zooms.
 * While zoomed in or pinching, it keeps its parent (the scrolling page) from stealing the touch,
 * so dragging the photo does not scroll the page. At normal size, dragging still scrolls the page.
 * The zoom resets whenever a new picture is set (for example when another angle is chosen).
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val baseMatrix = Matrix()   // fit-centre
    private val userMatrix = Matrix()   // zoom and pan applied by the user
    private val drawMatrix = Matrix()
    private val values = FloatArray(9)

    private val maxScale = 5f
    private val doubleTapScale = 2.5f

    /** Called when the picture becomes zoomed in or back to normal size. */
    var onZoomChanged: ((zoomedIn: Boolean) -> Unit)? = null
    private var lastZoomedIn = false

    fun isZoomedIn(): Boolean = userScale() > ZOOMED_THRESHOLD

    /**
     * The part of the picture that is on screen right now, as fractions (0..1) of the whole picture:
     * (left, top, right, bottom). Null if there is no picture yet.
     */
    fun visibleRegion(): RectF? {
        val d = drawable ?: return null
        val dw = d.intrinsicWidth.toFloat()
        val dh = d.intrinsicHeight.toFloat()
        if (dw <= 0f || dh <= 0f || width <= 0 || height <= 0) return null

        val inverse = Matrix()
        if (!Matrix(baseMatrix).apply { postConcat(userMatrix) }.invert(inverse)) return null
        val r = RectF(0f, 0f, width.toFloat(), height.toFloat())
        inverse.mapRect(r) // the view's rectangle, in picture pixels
        r.intersect(0f, 0f, dw, dh)
        return RectF(r.left / dw, r.top / dh, r.right / dw, r.bottom / dh)
    }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val current = userScale()
            val target = (current * detector.scaleFactor).coerceIn(1f, maxScale)
            val factor = target / current
            userMatrix.postScale(factor, factor, detector.focusX, detector.focusY)
            apply()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val current = userScale()
            if (current > 1.05f) {
                userMatrix.reset() // zoomed in: back to fit
            } else {
                userMatrix.postScale(doubleTapScale, doubleTapScale, e.x, e.y)
            }
            apply()
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (userScale() > 1.01f) {
                userMatrix.postTranslate(-distanceX, -distanceY)
                apply()
                return true
            }
            return false
        }
    })

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        userMatrix.reset()
        apply()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        apply()
    }

    private fun userScale(): Float {
        userMatrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }

    /** Fit-centre matrix for the current picture and view size. */
    private fun updateBase() {
        val d = drawable ?: return
        val dw = d.intrinsicWidth.toFloat()
        val dh = d.intrinsicHeight.toFloat()
        val vw = width.toFloat()
        val vh = height.toFloat()
        if (dw <= 0f || dh <= 0f || vw <= 0f || vh <= 0f) return
        val scale = min(vw / dw, vh / dh)
        baseMatrix.reset()
        baseMatrix.postScale(scale, scale)
        baseMatrix.postTranslate((vw - dw * scale) / 2f, (vh - dh * scale) / 2f)
    }

    /** Keeps the picture from being dragged away so that empty space shows beside it. */
    private fun keepInside() {
        val d = drawable ?: return
        val rect = RectF(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
        val m = Matrix(baseMatrix).apply { postConcat(userMatrix) }
        m.mapRect(rect)
        val vw = width.toFloat()
        val vh = height.toFloat()
        val dx = when {
            rect.width() <= vw -> (vw - rect.width()) / 2f - rect.left
            rect.left > 0f -> -rect.left
            rect.right < vw -> vw - rect.right
            else -> 0f
        }
        val dy = when {
            rect.height() <= vh -> (vh - rect.height()) / 2f - rect.top
            rect.top > 0f -> -rect.top
            rect.bottom < vh -> vh - rect.bottom
            else -> 0f
        }
        userMatrix.postTranslate(dx, dy)
    }

    private fun apply() {
        if (drawable == null) return
        updateBase()
        keepInside()
        drawMatrix.set(baseMatrix)
        drawMatrix.postConcat(userMatrix)
        imageMatrix = drawMatrix

        val zoomedIn = isZoomedIn()
        if (zoomedIn != lastZoomedIn) {
            lastZoomedIn = zoomedIn
            onZoomChanged?.invoke(zoomedIn)
        }
    }

    private companion object {
        const val ZOOMED_THRESHOLD = 1.15f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        if (!scaleDetector.isInProgress) gestureDetector.onTouchEvent(event)

        // Zoomed in, or pinching with two fingers: this view owns the touch (do not scroll the page)
        val owns = event.pointerCount > 1 || userScale() > 1.01f
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                parent?.requestDisallowInterceptTouchEvent(owns)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}
