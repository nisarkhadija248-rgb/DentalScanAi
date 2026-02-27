package com.dentalscan.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.dentalscan.ml.TFLiteHelper

object ImageUtils {

    fun drawDetections(bitmap: Bitmap, detections: List<TFLiteHelper.Detection>): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 32f
            style = Paint.Style.FILL
        }

        val colors = mapOf(
            "Caries" to Color.RED,
            "Plaque" to Color.YELLOW,
            "Calculus" to Color.MAGENTA
        )

        detections.forEach { detection ->
            paint.color = colors[detection.className] ?: Color.GREEN

            val rect = RectF(
                detection.boundingBox.left * mutableBitmap.width,
                detection.boundingBox.top * mutableBitmap.height,
                detection.boundingBox.right * mutableBitmap.width,
                detection.boundingBox.bottom * mutableBitmap.height
            )
            canvas.drawRect(rect, paint)

            val label = "${detection.className} ${(detection.confidence * 100).toInt()}%"
            canvas.drawText(label, rect.left, rect.top - 10, textPaint)
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
            when (condition) {
                "Caries" -> "کیریز کا پتہ چلا۔ براہ کرم جلد ڈینٹسٹ سے رجوع کریں۔"
                "Plaque" -> "دانتوں پر پلاک جمع ہے۔ برشنگ اور فلاسنگ بہتر کریں۔"
                "Calculus" -> "کیلکولس کا پتہ چلا۔ پیشہ ورانہ صفائی تجویز کی جاتی ہے۔"
                else -> "اپنی دانتوں کی صحت کا باقاعدگی سے جائزہ لیں۔"
            }
        } else {
            when (condition) {
                "Caries" -> "Caries detected. Please consult a dentist soon."
                "Plaque" -> "Plaque buildup found. Improve brushing and flossing."
                "Calculus" -> "Calculus detected. Professional cleaning recommended."
                else -> "Monitor your oral health regularly."
            }
        }
    }
}