package com.dentalscan.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteHelper(private val context: Context) {

    companion object {
        const val DUMMY_MODE = false
        private const val MODEL_FILE = "dental_model.tflite"
        private const val INPUT_SIZE = 640
        private const val NUM_CLASSES = 3
        private const val CONF_THRESHOLD = 0.10f
        private const val IOU_THRESHOLD = 0.45f
        private val CLASS_NAMES = arrayOf("Calculus", "Caries", "Gingivitis")
    }

    private var interpreter: Interpreter? = null

    // True when the model's input is [1, 3, H, W] instead of [1, H, W, 3]
    private var channelsFirst = false

    init {
        if (!DUMMY_MODE) {
            val options = Interpreter.Options().apply { numThreads = 4 }
            val model = Interpreter(loadModelFile(), options)
            interpreter = model

            val shape = model.getInputTensor(0).shape()
            channelsFirst = shape.size == 4 && shape[1] == 3 && shape[3] != 3
        }
    }

    data class Detection(
        val classId: Int,
        val className: String,
        val confidence: Float,
        val boundingBox: RectF
    )

    fun detect(image: Bitmap): List<Detection> {
        if (DUMMY_MODE) {
            ImagePreprocessor.preprocess(image)
            return listOf(Detection(0, "Caries", 0.85f, RectF(0.3f, 0.3f, 0.7f, 0.7f)))
        }

        // inputArray is height x width x 3, interleaved R,G,B
        val inputArray = ImagePreprocessor.preprocess(image)

        // Models are exported either channels-last [1,640,640,3] or channels-first [1,3,640,640].
        // Read which one this model wants from its input shape, so both kinds work.
        val reshapedInput: Any = if (channelsFirst) {
            val chw = Array(1) { Array(3) { Array(INPUT_SIZE) { FloatArray(INPUT_SIZE) } } }
            var idx = 0
            for (y in 0 until INPUT_SIZE)
                for (x in 0 until INPUT_SIZE)
                    for (c in 0..2)
                        chw[0][c][y][x] = inputArray[idx++]
            chw
        } else {
            val hwc = Array(1) { Array(INPUT_SIZE) { Array(INPUT_SIZE) { FloatArray(3) } } }
            var idx = 0
            for (y in 0 until INPUT_SIZE)
                for (x in 0 until INPUT_SIZE)
                    for (c in 0..2)
                        hwc[0][y][x][c] = inputArray[idx++]
            hwc
        }

        val output = Array(1) { Array(4 + NUM_CLASSES) { FloatArray(8400) } }
        interpreter?.run(reshapedInput, output)

        return parseYoloOutput(output[0])
    }

    private fun parseYoloOutput(output: Array<FloatArray>): List<Detection> {
        val detections = mutableListOf<Detection>()
        val numBoxes = output[0].size

        for (i in 0 until numBoxes) {
            val cx = output[0][i]
            val cy = output[1][i]
            val w = output[2][i]
            val h = output[3][i]

            var maxConf = 0f
            var classId = 0
            for (c in 0 until NUM_CLASSES) {
                val conf = output[4 + c][i]
                if (conf > maxConf) {
                    maxConf = conf
                    classId = c
                }
            }

            if (maxConf >= CONF_THRESHOLD) {
                detections.add(
                    Detection(
                        classId = classId,
                        className = CLASS_NAMES[classId],
                        confidence = maxConf,
                        boundingBox = RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
                    )
                )
            }
        }

        return applyNMS(detections)
    }

    private fun applyNMS(detections: List<Detection>): List<Detection> {
        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val result = mutableListOf<Detection>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            result.add(best)
            sorted.removeAll { iou(best.boundingBox, it.boundingBox) > IOU_THRESHOLD }
        }

        return result
    }

    private fun iou(a: RectF, b: RectF): Float {
        val interLeft = maxOf(a.left, b.left)
        val interTop = maxOf(a.top, b.top)
        val interRight = minOf(a.right, b.right)
        val interBottom = minOf(a.bottom, b.bottom)
        val interArea = maxOf(0f, interRight - interLeft) * maxOf(0f, interBottom - interTop)
        val unionArea = (a.width() * a.height()) + (b.width() * b.height()) - interArea
        return if (unionArea > 0) interArea / unionArea else 0f
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fd = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fd.fileDescriptor)
        val channel = inputStream.channel
        return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    fun close() {
        interpreter?.close()
    }
}