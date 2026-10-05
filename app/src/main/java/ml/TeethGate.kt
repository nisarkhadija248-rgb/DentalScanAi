package com.dentalscan.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Small image classifier ("teeth_gate.tflite", MobileNetV3-Small): how likely is it that teeth are
 * visible in an image? Input is the whole bitmap resized to 256x256 (RGB, values 0 to 255), output is a
 * probability between 0 and 1.
 *
 * Used to find the mouth in a photo (see [MouthFocus]) and to guide the camera (see GuidanceAnalyzer).
 * It is NOT the disease detector. [create] returns null if the model file is missing, and every caller
 * then falls back to its old behaviour.
 */
class TeethGate private constructor(private val interpreter: Interpreter) : AutoCloseable {

    companion object {
        private const val MODEL_FILE = "teeth_gate.tflite"
        const val INPUT_SIZE = 256

        fun create(context: Context): TeethGate? = try {
            val fd = context.assets.openFd(MODEL_FILE)
            val buffer = FileInputStream(fd.fileDescriptor).channel
                .map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            TeethGate(Interpreter(buffer, Interpreter.Options().apply { numThreads = 2 }))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val input: ByteBuffer =
        ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * 4).order(ByteOrder.nativeOrder())
    private val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
    private val output = Array(1) { FloatArray(1) }

    /** Probability that teeth are visible in the whole of [bitmap]. */
    @Synchronized
    fun score(bitmap: Bitmap): Float {
        val scaled = if (bitmap.width == INPUT_SIZE && bitmap.height == INPUT_SIZE) bitmap
        else Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)

        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        input.rewind()
        for (p in pixels) {
            input.putFloat(((p shr 16) and 0xFF).toFloat())  // R
            input.putFloat(((p shr 8) and 0xFF).toFloat())   // G
            input.putFloat((p and 0xFF).toFloat())           // B
        }
        input.rewind()
        interpreter.run(input, output)
        return output[0][0].coerceIn(0f, 1f)
    }

    /** Probability for a part of [bitmap]; the part is given as fractions (0..1) of its width and height. */
    fun scoreRegion(bitmap: Bitmap, left: Float, top: Float, right: Float, bottom: Float): Float {
        val x = (left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = (top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val w = ((right - left) * bitmap.width).toInt().coerceIn(1, bitmap.width - x)
        val h = ((bottom - top) * bitmap.height).toInt().coerceIn(1, bitmap.height - y)
        return score(Bitmap.createBitmap(bitmap, x, y, w, h))
    }

    override fun close() {
        interpreter.close()
    }
}
