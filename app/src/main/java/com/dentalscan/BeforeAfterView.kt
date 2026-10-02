package com.dentalscan

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max

/**
 * Draggable before/after comparison. The "after" photo fills the view; the "before"
 * photo is drawn on top and revealed from the left up to the divider. Drag anywhere
 * to move the divider. Both photos are center-cropped to the same frame.
 */
class BeforeAfterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var before: Bitmap? = null
    private var after: Bitmap? = null
    private var beforeLabel = "Before"
    private var afterLabel = "After"

    /** Divider position as a fraction of the width (0 = all "after", 1 = all "before"). */
    private var fraction = 0.5f
    private var hintPlayed = false
    private var hintAnimator: ValueAnimator? = null

    private val density = resources.displayMetrics.density

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val placeholderPaint = Paint().apply { color = Color.parseColor("#33000000") }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 3f * density
    }
    private val handleShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#55000000") }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0B6E8A")
        style = Paint.Style.FILL
    }
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#99000000") }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 12f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val src = Rect()
    private val dst = RectF()
    private val arrow = Path()

    fun setImages(beforeBitmap: Bitmap?, afterBitmap: Bitmap?) {
        before = beforeBitmap
        after = afterBitmap
        invalidate()
        if (!hintPlayed && beforeBitmap != null && afterBitmap != null) {
            hintPlayed = true
            playHint()
        }
    }

    fun setLabels(beforeText: String, afterText: String) {
        beforeLabel = beforeText
        afterLabel = afterText
        invalidate()
    }

    /** Short sweep the first time it appears, so it is obvious that the divider moves. */
    private fun playHint() {
        hintAnimator?.cancel()
        hintAnimator = ValueAnimator.ofFloat(0.12f, 0.5f).apply {
            duration = 900
            interpolator = DecelerateInterpolator()
            addUpdateListener { a ->
                fraction = a.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // "After" fills the frame, "before" is revealed on the left of the divider
        drawCover(canvas, after, w, h)
        val x = w * fraction
        canvas.save()
        canvas.clipRect(0f, 0f, x, h)
        drawCover(canvas, before, w, h)
        canvas.restore()

        // Divider line
        canvas.drawLine(x, 0f, x, h, linePaint)

        // Round handle with two arrows
        val cy = h / 2f
        val r = 20f * density
        canvas.drawCircle(x, cy + 1.5f * density, r + 1.5f * density, handleShadowPaint)
        canvas.drawCircle(x, cy, r, handlePaint)
        drawArrow(canvas, x - 7f * density, cy, pointLeft = true)
        drawArrow(canvas, x + 7f * density, cy, pointLeft = false)

        // Labels, hidden when the divider is too close to their corner
        val pad = 12f * density
        val beforeW = textPaint.measureText(beforeLabel) + pad * 2
        val afterW = textPaint.measureText(afterLabel) + pad * 2
        if (x > beforeW + pad) drawPill(canvas, beforeLabel, pad, pad)
        if (w - x > afterW + pad) drawPill(canvas, afterLabel, w - pad - afterW, pad)
    }

    private fun drawCover(canvas: Canvas, bmp: Bitmap?, w: Float, h: Float) {
        dst.set(0f, 0f, w, h)
        if (bmp == null) {
            canvas.drawRect(dst, placeholderPaint)
            return
        }
        // Center-crop: scale to cover the frame, then crop the overflow
        val scale = max(w / bmp.width, h / bmp.height)
        val srcW = w / scale
        val srcH = h / scale
        val left = (bmp.width - srcW) / 2f
        val top = (bmp.height - srcH) / 2f
        src.set(left.toInt(), top.toInt(), (left + srcW).toInt(), (top + srcH).toInt())
        canvas.drawBitmap(bmp, src, dst, bitmapPaint)
    }

    private fun drawArrow(canvas: Canvas, cx: Float, cy: Float, pointLeft: Boolean) {
        val s = 5f * density
        arrow.reset()
        if (pointLeft) {
            arrow.moveTo(cx - s * 0.6f, cy)
            arrow.lineTo(cx + s * 0.6f, cy - s)
            arrow.lineTo(cx + s * 0.6f, cy + s)
        } else {
            arrow.moveTo(cx + s * 0.6f, cy)
            arrow.lineTo(cx - s * 0.6f, cy - s)
            arrow.lineTo(cx - s * 0.6f, cy + s)
        }
        arrow.close()
        canvas.drawPath(arrow, arrowPaint)
    }

    private fun drawPill(canvas: Canvas, text: String, left: Float, top: Float) {
        val padX = 12f * density
        val metrics = textPaint.fontMetrics
        val textH = metrics.descent - metrics.ascent
        val padY = 5f * density
        val rect = RectF(left, top, left + textPaint.measureText(text) + padX * 2, top + textH + padY * 2)
        canvas.drawRoundRect(rect, 12f * density, 12f * density, pillPaint)
        canvas.drawText(text, rect.left + padX, rect.top + padY - metrics.ascent, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                hintAnimator?.cancel()
                // We sit inside a ScrollView: keep the drag for ourselves
                parent?.requestDisallowInterceptTouchEvent(true)
                moveDivider(event.x)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                moveDivider(event.x)
                return true
            }
            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()

    private fun moveDivider(x: Float) {
        if (width <= 0) return
        fraction = (x / width).coerceIn(0.03f, 0.97f)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        hintAnimator?.cancel()
        super.onDetachedFromWindow()
    }
}
