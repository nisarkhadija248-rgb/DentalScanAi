package com.dentalscan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.content.res.ColorStateList
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dentalscan.ml.GuidanceAnalyzer
import com.dentalscan.ml.TeethGate
import com.dentalscan.ml.TFLiteHelper
import com.dentalscan.utils.ImageCompressor
import com.dentalscan.utils.LanguageManager
import com.dentalscan.utils.VoiceFeedback
import org.json.JSONArray
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : BaseActivity() {

    // ── Views ──────────────────────────────────────────
    private lateinit var viewFinder: PreviewView
    private lateinit var captureButton: Button
    private lateinit var skipButton: Button
    private lateinit var guidanceText: TextView
    private lateinit var stepCounterText: TextView
    private lateinit var stepNameText: TextView
    private lateinit var stepInstructionText: TextView
    private lateinit var stepIllustrationEmoji: ImageView
    private lateinit var stepIllustrationText: TextView
    private lateinit var stepProgressBar: ProgressBar
    private lateinit var capturedDotsText: LinearLayout
    private lateinit var guideFrame: View
    private lateinit var flashButton: ImageView
    private lateinit var cameraMuteButton: ImageView

    // ── Camera ─────────────────────────────────────────
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var teethGate: TeethGate? = null

    // ── Wizard State ───────────────────────────────────
    private var currentStep = 1
    private val totalSteps = 5
    private val capturedPaths = mutableListOf<String>()

    // Step definitions — icon, name EN, name UR, instruction EN, instruction UR
    data class StepInfo(
        val iconRes: Int,
        val nameEn: String,
        val nameUr: String,
        val instructionEn: String,
        val instructionUr: String,
        val illustrationEn: String,
        val illustrationUr: String,
        val canSkip: Boolean
    )

    private val steps = listOf(
        StepInfo(
            iconRes = R.drawable.ic_face,
            nameEn = "Front View",
            nameUr = "سامنے سے",
            instructionEn = "Open your mouth and face the camera directly",
            instructionUr = "منہ کھولیں اور کیمرے کی طرف سیدھے دیکھیں",
            illustrationEn = "Face camera directly",
            illustrationUr = "کیمرے کی طرف دیکھیں",
            canSkip = false  // Front view is required
        ),
        StepInfo(
            iconRes = R.drawable.ic_arrow_upward,
            nameEn = "Upper Teeth",
            nameUr = "اوپر کے دانت",
            instructionEn = "Tilt head back slightly and show upper teeth",
            instructionUr = "سر تھوڑا پیچھے جھکائیں اور اوپر کے دانت دکھائیں",
            illustrationEn = "Tilt head back",
            illustrationUr = "سر پیچھے جھکائیں",
            canSkip = true
        ),
        StepInfo(
            iconRes = R.drawable.ic_arrow_downward,
            nameEn = "Lower Teeth",
            nameUr = "نیچے کے دانت",
            instructionEn = "Tilt head forward slightly and show lower teeth",
            instructionUr = "سر تھوڑا آگے جھکائیں اور نیچے کے دانت دکھائیں",
            illustrationEn = "Tilt head forward",
            illustrationUr = "سر آگے جھکائیں",
            canSkip = true
        ),
        StepInfo(
            iconRes = R.drawable.ic_arrow_back,
            nameEn = "Left Side",
            nameUr = "بائیں طرف",
            instructionEn = "Turn head right to show the left side of your teeth",
            instructionUr = "سر دائیں طرف موڑیں تاکہ بائیں جانب کے دانت نظر آئیں",
            illustrationEn = "Turn head right",
            illustrationUr = "سر دائیں موڑیں",
            canSkip = true
        ),
        StepInfo(
            iconRes = R.drawable.ic_arrow_forward,
            nameEn = "Right Side",
            nameUr = "دائیں طرف",
            instructionEn = "Turn head left to show the right side of your teeth",
            instructionUr = "سر بائیں طرف موڑیں تاکہ دائیں جانب کے دانت نظر آئیں",
            illustrationEn = "Turn head left",
            illustrationUr = "سر بائیں موڑیں",
            canSkip = true
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        initViews()
        updateWizardUI()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        captureButton.setOnClickListener { takePhoto() }
        skipButton.setOnClickListener { advanceToNextStep(null) }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun initViews() {
        viewFinder = findViewById(R.id.viewFinder)
        captureButton = findViewById(R.id.captureButton)
        skipButton = findViewById(R.id.skipButton)
        guidanceText = findViewById(R.id.guidanceText)
        stepCounterText = findViewById(R.id.stepCounterText)
        stepNameText = findViewById(R.id.stepNameText)
        stepInstructionText = findViewById(R.id.stepInstructionText)
        stepIllustrationEmoji = findViewById(R.id.stepIllustrationEmoji)
        stepIllustrationText = findViewById(R.id.stepIllustrationText)
        stepProgressBar = findViewById(R.id.stepProgressBar)
        capturedDotsText = findViewById(R.id.capturedDotsText)
        guideFrame = findViewById(R.id.guideFrame)
        flashButton = findViewById(R.id.flashButton)
        cameraMuteButton = findViewById(R.id.cameraMuteButton)

        VoiceFeedback.attachMuteButton(cameraMuteButton)
        flashButton.setOnClickListener {
            // The icon follows the real torch state (see observeTorch), so a failure can't leave it wrong
            val cam = camera ?: return@setOnClickListener
            val turnOn = cam.cameraInfo.torchState.value != TorchState.ON
            cam.cameraControl.enableTorch(turnOn)
        }
    }

    private fun showTorchIcon(on: Boolean) {
        flashButton.setImageResource(if (on) R.drawable.ic_flashlight_on else R.drawable.ic_flashlight_off)
        flashButton.contentDescription =
            getString(if (on) R.string.flash_on_desc else R.string.flash_off_desc)
    }

    /** Show the flashlight button only if the rear camera has a flash, and keep its icon in sync. */
    private fun observeTorch(cam: Camera) {
        if (!cam.cameraInfo.hasFlashUnit()) {
            flashButton.visibility = View.GONE
            return
        }
        flashButton.visibility = View.VISIBLE
        cam.cameraInfo.torchState.observe(this) { state -> showTorchIcon(state == TorchState.ON) }
    }

    /** The torch is always switched off when the user leaves the camera screen. */
    override fun onPause() {
        camera?.cameraControl?.enableTorch(false)
        super.onPause()
    }

    // ── Wizard Logic ───────────────────────────────────

    private fun updateWizardUI() {
        val isUrdu = LanguageManager.isUrdu(this)
        val step = steps[currentStep - 1]

        // Step counter
        stepCounterText.text = if (isUrdu)
            "مرحلہ $currentStep از $totalSteps"
        else
            "Step $currentStep of $totalSteps"

        // Step name
        stepNameText.text = if (isUrdu) step.nameUr else step.nameEn

        // Progress bar
        stepProgressBar.progress = currentStep - 1

        // Illustration
        stepIllustrationEmoji.setImageResource(step.iconRes)
        stepIllustrationText.text = if (isUrdu) step.illustrationUr else step.illustrationEn

        // Instruction
        stepInstructionText.text = if (isUrdu) step.instructionUr else step.instructionEn

        // Skip button — hidden on step 1, visible on steps 2-5
        skipButton.visibility = if (step.canSkip) View.VISIBLE else View.GONE
        skipButton.text = if (isUrdu) "چھوڑیں" else "Skip"

        // Capture button text
        captureButton.text = if (currentStep == totalSteps) {
            if (isUrdu) "آخری تصویر" else "Last Photo"
        } else {
            if (isUrdu) "تصویر لیں" else "Capture"
        }

        // Captured dots — green dot = captured, white circle = not yet
        updateCapturedDots()

        // Default guidance
        updateGuidanceText(isUrdu, "position_teeth")

        // Reset capture button state for new step
        if (TFLiteHelper.DUMMY_MODE) {
            captureButton.isEnabled = true
            setGuidanceReady(true)
        } else {
            captureButton.isEnabled = false
        }
        guideFrame.isActivated = captureButton.isEnabled
    }

    private fun updateCapturedDots() {
        // filled = captured, ring = current step, dim = not yet
        for (i in 1..totalSteps) {
            val dot = capturedDotsText.getChildAt(i - 1) ?: continue
            dot.setBackgroundResource(
                when {
                    // done = this step has a photo (skipped steps are stored as "")
                    i < currentStep && capturedPaths.getOrNull(i - 1)?.isNotEmpty() == true -> R.drawable.dot_done
                    i == currentStep -> R.drawable.dot_current
                    else -> R.drawable.dot_todo
                }
            )
        }
    }

    /** Guidance pill color: green when ready to capture, teal otherwise. */
    private fun setGuidanceReady(ready: Boolean) {
        val color = ContextCompat.getColor(
            this, if (ready) R.color.guidance_ready else R.color.primary_dark
        )
        guidanceText.backgroundTintList = ColorStateList.valueOf(color)
    }

    /** Check icon at the start of the guidance pill, only for the "Perfect" state. */
    private fun setGuidanceCheckIcon(show: Boolean) {
        if (!show) {
            guidanceText.setCompoundDrawablesRelative(null, null, null, null)
            return
        }
        val size = (20 * resources.displayMetrics.density).toInt()
        val icon = ContextCompat.getDrawable(this, R.drawable.ic_check_circle)?.mutate() ?: return
        icon.setBounds(0, 0, size, size)
        icon.setTint(ContextCompat.getColor(this, R.color.white))
        guidanceText.setCompoundDrawablesRelative(icon, null, null, null)
    }

    /**
     * Core wizard state machine.
     * savedPath = file path if user captured, null if user skipped.
     * Handles all 6 scenarios described in planning.
     */
    private fun advanceToNextStep(savedPath: String?) {
        // One entry per step, so the position in the list always equals the step number
        // (0 = front, 1 = upper, 2 = lower, 3 = left, 4 = right). A skipped step is stored as "".
        capturedPaths.add(savedPath ?: "")

        if (currentStep >= totalSteps) {
            // All steps done — launch analysis
            launchAnalysis()
        } else {
            // Move to next step
            currentStep++
            updateWizardUI()

            val isUrdu = LanguageManager.isUrdu(this)
            val step = steps[currentStep - 1]
            VoiceFeedback.speak(
                if (isUrdu) step.instructionUr else step.instructionEn
            )
        }
    }

    private fun launchAnalysis() {
        if (capturedPaths.none { it.isNotEmpty() }) {
            // Edge case: user skipped every step including step 1
            // This shouldn't happen since step 1 can't be skipped,
            // but handle it gracefully anyway
            val isUrdu = LanguageManager.isUrdu(this)
            Toast.makeText(
                this,
                if (isUrdu) "کم از کم ایک تصویر ضروری ہے" else "At least one photo is required",
                Toast.LENGTH_SHORT
            ).show()
            currentStep = 1
            updateWizardUI()
            return
        }

        // Build JSON array of all captured paths
        val pathsJson = JSONArray().apply {
            capturedPaths.forEach { put(it) }
        }.toString()

        val intent = Intent(this, AnalysisActivity::class.java)
        intent.putExtra("image_path", pathsJson)
        startActivity(intent)
        finish()
    }

    // ── Camera ─────────────────────────────────────────

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(viewFinder.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    teethGate = TeethGate.create(this)
                    it.setAnalyzer(cameraExecutor, GuidanceAnalyzer(teethGate) { result ->
                        runOnUiThread { updateGuidanceFromResult(result) }
                    })
                }

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA,
                    preview, imageCapture, imageAnalyzer
                )
                camera?.let { observeTorch(it) }
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera failed", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun updateGuidanceText(isUrdu: Boolean, state: String) {
        guidanceText.text = when (state) {
            "position_teeth" -> if (isUrdu) "دانتوں کو فریم میں درست رکھیں" else "Position your teeth in the frame"
            "move_closer" -> if (isUrdu) "قریب آئیں" else "Move closer"
            "too_dark" -> if (isUrdu) "روشنی بڑھائیں" else "Increase lighting"
            "too_blurry" -> if (isUrdu) "کیمرا سیدھا رکھیں" else "Hold camera steady"
            "good" -> if (isUrdu) "بہترین! اب تصویر لیں" else "Perfect! Capture now"
            else -> if (isUrdu) "دانتوں کو فریم میں درست رکھیں" else "Position your teeth in the frame"
        }
        setGuidanceCheckIcon(state == "good")
    }

    private fun updateGuidanceFromResult(result: GuidanceAnalyzer.GuidanceResult) {
        val isUrdu = LanguageManager.isUrdu(this)

        // Light and focus first (they decide whether the photo is usable), then whether teeth are in view
        val state = when {
            result.isTooDark -> "too_dark"
            result.isTooBlurry -> "too_blurry"
            !result.hasTeeth -> "position_teeth"
            result.isTooFar -> "move_closer"
            else -> "good"
        }

        updateGuidanceText(isUrdu, state)

        // In DUMMY_MODE guidance is bypassed — button stays enabled
        if (!TFLiteHelper.DUMMY_MODE) {
            // The teeth check is only a hint (green frame and "Perfect!"): the classifier is not perfect,
            // so it never blocks Capture. Only a dark or blurry frame does.
            captureButton.isEnabled = !result.isTooDark && !result.isTooBlurry

            val ready = captureButton.isEnabled && result.hasTeeth
            setGuidanceReady(ready)
            guideFrame.isActivated = ready
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        captureButton.isEnabled = false
        guideFrame.isActivated = false

        val photoFile = File(
            externalMediaDirs.first(),
            "dental_scan_step${currentStep}_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    // Scenario 6: capture failed — stay on current step
                    captureButton.isEnabled = true
                    guideFrame.isActivated = true
                    val isUrdu = LanguageManager.isUrdu(this@CameraActivity)
                    Toast.makeText(
                        baseContext,
                        if (isUrdu) "تصویر لینے میں خرابی" else "Capture failed, try again",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Compress on background thread
                    cameraExecutor.execute {
                        val compressedPath = ImageCompressor.compressAndSave(
                            photoFile.absolutePath
                        )
                        runOnUiThread {
                            val isUrdu = LanguageManager.isUrdu(this@CameraActivity)
                            val stepName = if (isUrdu)
                                steps[currentStep - 1].nameUr
                            else
                                steps[currentStep - 1].nameEn

                            VoiceFeedback.speak(
                                if (isUrdu) "$stepName محفوظ" else "$stepName captured"
                            )

                            // Advance wizard with the saved path
                            advanceToNextStep(compressedPath)
                        }
                    }
                }
            }
        )
    }

    // ── Permissions ────────────────────────────────────

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) startCamera()
            else {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        teethGate?.close()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}