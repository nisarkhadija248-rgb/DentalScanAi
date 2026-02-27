package com.dentalscan.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF

class TFLiteHelper(context: Context) {

    companion object {
        // ─────────────────────────────────────────────
        // DUMMY_MODE: Set to true during development and testing.
        // Set to false when the real YOLOv8n .tflite model is ready.
        // ─────────────────────────────────────────────
        const val DUMMY_MODE = true
    }

    data class Detection(
        val classId: Int,
        val className: String,
        val confidence: Float,
        val boundingBox: RectF
    )

    fun detect(image: Bitmap): List<Detection> {
        if (DUMMY_MODE) {
            // Preprocessing still runs in dummy mode —
            // pipeline is tested even though result is ignored
            ImagePreprocessor.preprocess(image)

            return listOf(
                Detection(
                    classId = 0,
                    className = "Caries",
                    confidence = 0.85f,
                    boundingBox = RectF(0.3f, 0.3f, 0.7f, 0.7f)
                )
            )
        }

        // ─────────────────────────────────────────────
        // REAL INFERENCE — runs when DUMMY_MODE = false
        // ─────────────────────────────────────────────

        // Step 1: Preprocess — CLAHE + normalize
        val inputArray = ImagePreprocessor.preprocess(image)

        // Step 2: Run TFLite inference
        // val outputArray = runInference(inputArray)

        // Step 3: Parse YOLO output → Detection list
        // return parseYoloOutput(outputArray)

        return emptyList()
    }

    fun close() {
        if (!DUMMY_MODE) {
            // Release TFLite interpreter here when real model is integrated
        }
    }
}