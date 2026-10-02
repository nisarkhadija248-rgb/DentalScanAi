package com.dentalscan.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.dentalscan.ml.TFLiteHelper

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

        detections.forEach { detection ->
            val boxColor = ConditionStyle.paintColor(detection.className)
            paint.color = boxColor

            val rect = RectF(
                detection.boundingBox.left * mutableBitmap.width,
                detection.boundingBox.top * mutableBitmap.height,
                detection.boundingBox.right * mutableBitmap.width,
                detection.boundingBox.bottom * mutableBitmap.height
            )
            canvas.drawRoundRect(rect, 8f, 8f, paint)

            // Label on a filled pill in the same color as the box
            val label = "${ConditionStyle.shortName(detection.className)} ${(detection.confidence * 100).toInt()}%"
            val metrics = textPaint.fontMetrics
            val textHeight = metrics.descent - metrics.ascent
            val pillWidth = textPaint.measureText(label) + padX * 2
            val pillHeight = textHeight + padY * 2

            // Sit above the box; drop inside it if there is no room at the top of the image
            val pillTop = if (rect.top - pillHeight >= 0f) rect.top - pillHeight else rect.top
            val pillRect = RectF(rect.left, pillTop, rect.left + pillWidth, pillTop + pillHeight)

            pillPaint.color = boxColor
            canvas.drawRoundRect(pillRect, 10f, 10f, pillPaint)

            textPaint.color = ConditionStyle.paintTextColor(detection.className)
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