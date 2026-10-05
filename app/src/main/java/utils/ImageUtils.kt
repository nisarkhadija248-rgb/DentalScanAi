package com.dentalscan.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.dentalscan.ml.TFLiteHelper
import kotlin.math.max

object ImageUtils {

    fun drawDetections(bitmap: Bitmap, detections: List<TFLiteHelper.Detection>): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeJoin = Paint.Join.ROUND
        }

        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            style = Paint.Style.FILL
        }

        val padX = 12f
        val padY = 8f
        val metrics = textPaint.fontMetrics
        val textHeight = metrics.descent - metrics.ascent
        val pillHeight = textHeight + padY * 2
        val imageW = mutableBitmap.width.toFloat()
        val imageH = mutableBitmap.height.toFloat()

        // Strongest findings first, so they get the best label position
        val ordered = detections.sortedByDescending { it.confidence }

        // 1. All boxes
        val boxes = ordered.map { d ->
            RectF(
                d.boundingBox.left * imageW, d.boundingBox.top * imageH,
                d.boundingBox.right * imageW, d.boundingBox.bottom * imageH
            )
        }
        ordered.forEachIndexed { i, d ->
            paint.color = ConditionStyle.paintColor(d.className)
            canvas.drawRoundRect(boxes[i], 8f, 8f, paint)
        }

        // 2. Labels on filled pills in the box colour, placed so they do not cover each other
        val placed = ArrayList<RectF>()
        ordered.forEachIndexed { i, d ->
            val rect = boxes[i]
            val label = "${ConditionStyle.shortName(d.className)} ${(d.confidence * 100).toInt()}%"
            val pillWidth = textPaint.measureText(label) + padX * 2

            // Keep the pill inside the picture horizontally
            val left = rect.left.coerceIn(0f, max(0f, imageW - pillWidth))

            // Try: above the box, below it, then stacked further above / below until one is free
            val tops = ArrayList<Float>()
            tops += rect.top - pillHeight
            tops += rect.bottom
            for (k in 1..4) {
                tops += rect.top - pillHeight * (k + 1)
                tops += rect.bottom + pillHeight * k
            }
            var chosen: RectF? = null
            for (top in tops) {
                if (top < 0f || top + pillHeight > imageH) continue
                val candidate = RectF(left, top, left + pillWidth, top + pillHeight)
                if (placed.none { RectF.intersects(it, candidate) }) {
                    chosen = candidate
                    break
                }
            }
            // No free spot: sit at the top of the box (the old behaviour)
            val pillRect = chosen ?: RectF(
                left, rect.top.coerceIn(0f, max(0f, imageH - pillHeight)),
                left + pillWidth, rect.top.coerceIn(0f, max(0f, imageH - pillHeight)) + pillHeight
            )
            placed += pillRect

            pillPaint.color = ConditionStyle.paintColor(d.className)
            canvas.drawRoundRect(pillRect, 10f, 10f, pillPaint)
            textPaint.color = ConditionStyle.paintTextColor(d.className)
            canvas.drawText(label, pillRect.left + padX, pillRect.top + padY - metrics.ascent, textPaint)
        }

        return mutableBitmap
    }

    fun getSeverityText(confidence: Float): String {
        return when {
            confidence > 0.8f -> "High"
            confidence > 0.6f -> "Medium"
            else -> "Low"
        }
    }

    fun getGuidanceMessage(condition: String, isUrdu: Boolean): String {
        return if (isUrdu) {
            when (condition.lowercase()) {
                "caries" -> "کیریز کا پتہ چلا۔ براہ کرم جلد ڈینٹسٹ سے رجوع کریں۔"
                "gingivitis" -> "مسوڑوں کی سوزش کا پتہ چلا۔ برشنگ اور فلاسنگ بہتر کریں۔"
                "calculus" -> "کیلکولس کا پتہ چلا۔ پیشہ ورانہ صفائی تجویز کی جاتی ہے۔"
                else -> "اپنی دانتوں کی صحت کا باقاعدگی سے جائزہ لیں۔"
            }
        } else {
            when (condition.lowercase()) {
                "caries" -> "Caries detected. Please consult a dentist soon."
                "gingivitis" -> "Gingivitis detected. Improve brushing and flossing."
                "calculus" -> "Calculus detected. Professional cleaning recommended."
                else -> "Monitor your oral health regularly."
            }
        }
    }
}