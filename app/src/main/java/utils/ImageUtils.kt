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

        // 2. Labels on filled pills in the box colour. A label always stays right next to its own box:
        // above it, or below it. If the full label ("Calculus 37%") would cover another label, a short one
        // ("37%") is used instead. The colour tells which condition it is (see the legend under the photo).
        val placed = ArrayList<RectF>()
        ordered.forEachIndexed { i, d ->
            val rect = boxes[i]
            val percent = "${(d.confidence * 100).toInt()}%"
            val full = "${ConditionStyle.shortName(d.className)} $percent"

            // A pill for [text] next to the box at [top], kept inside the picture horizontally
            fun pillFor(text: String, top: Float): RectF {
                val w = textPaint.measureText(text) + padX * 2
                val left = rect.left.coerceIn(0f, max(0f, imageW - w))
                return RectF(left, top, left + w, top + pillHeight)
            }
            fun fits(r: RectF) = r.top >= 0f && r.bottom <= imageH && placed.none { RectF.intersects(it, r) }

            var text = full
            var pill: RectF? = listOf(rect.top - pillHeight, rect.bottom).map { pillFor(full, it) }.firstOrNull { fits(it) }
            if (pill == null) {
                text = percent
                pill = listOf(rect.top - pillHeight, rect.bottom).map { pillFor(percent, it) }.firstOrNull { fits(it) }
            }
            // Still no room: put the short label at the top of the box rather than far away
            val pillRect = pill ?: pillFor(percent, rect.top.coerceIn(0f, max(0f, imageH - pillHeight)))
            if (pill == null) text = percent
            placed += pillRect

            pillPaint.color = ConditionStyle.paintColor(d.className)
            canvas.drawRoundRect(pillRect, 10f, 10f, pillPaint)
            textPaint.color = ConditionStyle.paintTextColor(d.className)
            canvas.drawText(text, pillRect.left + padX, pillRect.top + padY - metrics.ascent, textPaint)
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