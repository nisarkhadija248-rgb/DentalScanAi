package com.dentalscan.ml

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class GuidanceAnalyzer(private val listener: (GuidanceResult) -> Unit) : ImageAnalysis.Analyzer {

    data class GuidanceResult(
        val hasTeeth: Boolean = true,
        val teethConfidence: Float = 1.0f,
        val isTooDark: Boolean = false,
        val isTooBlurry: Boolean = false,
        val isTooFar: Boolean = false,
        val isTooClose: Boolean = false
    )

    override fun analyze(imageProxy: ImageProxy) {
        listener(GuidanceResult())
        imageProxy.close()
    }
}