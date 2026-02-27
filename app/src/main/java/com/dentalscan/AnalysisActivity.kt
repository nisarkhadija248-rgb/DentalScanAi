package com.dentalscan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.dentalscan.data.AppDatabase
import com.dentalscan.data.ScanEntity
import com.dentalscan.ml.TFLiteHelper
import com.dentalscan.utils.ImageUtils
import com.dentalscan.utils.LanguageManager
import com.dentalscan.utils.VoiceFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import android.view.View
import com.dentalscan.utils.ProfileManager
import android.widget.LinearLayout
import com.dentalscan.utils.ConditionEducation

class AnalysisActivity : BaseActivity() {

    private lateinit var imageView: ImageView
    private lateinit var resultText: TextView
    private lateinit var confidenceText: TextView
    private lateinit var saveButton: Button
    private lateinit var retakeButton: Button
    private lateinit var actionPlanCard: androidx.cardview.widget.CardView
    private lateinit var actionPlanTitle: TextView
    private lateinit var actionPlanText: TextView
    private lateinit var conditionInfoCard: androidx.cardview.widget.CardView
    private lateinit var conditionInfoTitle: TextView
    private lateinit var conditionInfoToggle: TextView
    private lateinit var conditionInfoContent: LinearLayout
    private lateinit var conditionWhatText: TextView
    private lateinit var conditionCauseTitle: TextView
    private lateinit var conditionCauseText: TextView
    private lateinit var conditionPreventTitle: TextView
    private lateinit var conditionPreventText: TextView

    private var tfliteHelper: TFLiteHelper? = null
    private var currentDetections: List<TFLiteHelper.Detection> = emptyList()
    private var imagePath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis)

        // Main views
        imageView = findViewById(R.id.resultImageView)
        resultText = findViewById(R.id.resultText)
        confidenceText = findViewById(R.id.confidenceText)
        saveButton = findViewById(R.id.saveButton)
        retakeButton = findViewById(R.id.retakeButton)

        // Educational content views
        actionPlanCard = findViewById(R.id.actionPlanCard)
        actionPlanTitle = findViewById(R.id.actionPlanTitle)
        actionPlanText = findViewById(R.id.actionPlanText)
        conditionInfoCard = findViewById(R.id.conditionInfoCard)
        conditionInfoTitle = findViewById(R.id.conditionInfoTitle)
        conditionInfoToggle = findViewById(R.id.conditionInfoToggle)
        conditionInfoContent = findViewById(R.id.conditionInfoContent)
        conditionWhatText = findViewById(R.id.conditionWhatText)
        conditionCauseTitle = findViewById(R.id.conditionCauseTitle)
        conditionCauseText = findViewById(R.id.conditionCauseText)
        conditionPreventTitle = findViewById(R.id.conditionPreventTitle)
        conditionPreventText = findViewById(R.id.conditionPreventText)

        imagePath = intent.getStringExtra("image_path") ?: return

        val isUrdu = LanguageManager.isUrdu(this)
        resultText.text = if (isUrdu) "تجزیہ ہو رہا ہے ..." else "Analyzing..."
        confidenceText.text = if (isUrdu) "براہ کرم انتظار کریں" else "Please wait"

        saveButton.setOnClickListener { saveScan() }
        retakeButton.setOnClickListener { finish() }

        val isViewOnly = intent.getBooleanExtra("view_only", false)

        if (isViewOnly) {
            saveButton.visibility = View.GONE
            retakeButton.visibility = View.GONE

            val condition = intent.getStringExtra("condition") ?: ""
            val confidence = intent.getFloatExtra("confidence", 0f)
            val severity = intent.getStringExtra("severity") ?: ""

            // imagePath may be a JSON array or a plain path — getThumbnailPath handles both
            val tempScan = com.dentalscan.data.ScanEntity(imagePath = imagePath)
            val bitmap = BitmapFactory.decodeFile(tempScan.getThumbnailPath())
            imageView.setImageBitmap(bitmap)

            val conditionUrdu = when (condition) {
                "Caries" -> "کیریز (سڑن)"
                "Plaque" -> "پلاک"
                "Calculus" -> "کیلکولس (پتھر)"
                else -> condition
            }
            val severityUrdu = when (severity) {
                "High" -> "زیادہ"
                "Medium" -> "درمیانی"
                "Low" -> "کم"
                else -> severity
            }

            resultText.text = if (isUrdu) "$conditionUrdu پایا گیا" else "$condition Detected"
            confidenceText.text = if (isUrdu) {
                "اعتماد: ${(confidence * 100).toInt()}% ($severityUrdu)"
            } else {
                "Confidence: ${(confidence * 100).toInt()}% ($severity)"
            }

            // Show educational content in view only mode too
            if (condition.isNotEmpty() && severity.isNotEmpty()) {
                showEducationalContent(condition, severity)
            }

        } else {
            analyzeImage()
        }
    }

    private fun analyzeImage() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                tfliteHelper = TFLiteHelper(this@AnalysisActivity)

                // imagePath may be a JSON array OR a plain path
                // Always extract the front view (index 0) for analysis
                val frontPath = extractFrontPath(imagePath)

                if (frontPath.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        val isUrdu = LanguageManager.isUrdu(this@AnalysisActivity)
                        resultText.text = if (isUrdu) "تصویر نہیں ملی" else "Image not found"
                        confidenceText.text = if (isUrdu) "دوبارہ اسکین کریں" else "Please retake scan"
                    }
                    return@launch
                }

                val bitmap = BitmapFactory.decodeFile(frontPath)

                if (bitmap == null) {
                    withContext(Dispatchers.Main) {
                        val isUrdu = LanguageManager.isUrdu(this@AnalysisActivity)
                        resultText.text = if (isUrdu) "تصویر نہیں ملی" else "Image not found"
                        confidenceText.text = if (isUrdu) "دوبارہ اسکین کریں" else "Please retake scan"
                    }
                    return@launch
                }

                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true)
                currentDetections = tfliteHelper?.detect(scaledBitmap) ?: emptyList()
                val resultBitmap = ImageUtils.drawDetections(scaledBitmap, currentDetections)
                val primaryDetection = currentDetections.maxByOrNull { it.confidence }

                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(resultBitmap)
                    displayResults(primaryDetection)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val isUrdu = LanguageManager.isUrdu(this@AnalysisActivity)
                    resultText.text = if (isUrdu) "تجزیے میں خرابی" else "Analysis error"
                    confidenceText.text = e.message ?: if (isUrdu) "دوبارہ کوشش کریں" else "Please try again"
                }
            }
        }
    }

    /**
     * Extracts the front view path from either:
     * - A JSON array string: ["path1","path2"] → returns path1
     * - A plain path string: /data/scan.jpg → returns as-is
     */
    private fun extractFrontPath(path: String): String {
        return try {
            if (path.trim().startsWith("[")) {
                // It's a JSON array — extract index 0
                val arr = JSONArray(path)
                if (arr.length() > 0) arr.getString(0) else ""
            } else {
                // Plain path — use directly
                path
            }
        } catch (e: Exception) {
            path // fallback
        }
    }

    private fun displayResults(detection: TFLiteHelper.Detection?) {
        val isUrdu = LanguageManager.isUrdu(this)

        if (detection == null) {
            resultText.text = if (isUrdu) "کوئی مسئلہ نہیں ملا" else "No dental issues detected"
            confidenceText.text = if (isUrdu) "آپ کے دانت صحت مند ہیں" else "Your teeth appear healthy"
            VoiceFeedback.speak(if (isUrdu) "کوئی مسئلہ نہیں ملا" else "No dental issues detected")
            return
        }

        val condition = detection.className
        val confidence = (detection.confidence * 100).toInt()
        val severity = ImageUtils.getSeverityText(detection.confidence)

        val conditionUrdu = when (condition) {
            "Caries" -> "کیریز (سڑن)"
            "Plaque" -> "پلاک"
            "Calculus" -> "کیلکولس (پتھر)"
            else -> condition
        }

        val severityUrdu = when (severity) {
            "High" -> "زیادہ"
            "Medium" -> "درمیانی"
            "Low" -> "کم"
            else -> severity
        }

        resultText.text = if (isUrdu) "$conditionUrdu پایا گیا" else "$condition Detected"
        confidenceText.text = if (isUrdu) {
            "اعتماد: $confidence% ($severityUrdu)"
        } else {
            "Confidence: $confidence% ($severity)"
        }

        VoiceFeedback.speakDetectionResult(condition, severity, isUrdu)

        val guidance = ImageUtils.getGuidanceMessage(condition, isUrdu)
        lifecycleScope.launch {
            kotlinx.coroutines.delay(3000)
            VoiceFeedback.speak(guidance)
        }

        // Show educational content after results
        showEducationalContent(condition, severity)
    }

    private fun showEducationalContent(condition: String, severity: String) {
        val isUrdu = LanguageManager.isUrdu(this)

        // Option C — Action Plan Card
        val actionPlan = ConditionEducation.getActionPlan(condition, severity, isUrdu)
        actionPlanTitle.text = if (isUrdu) "کیا کریں؟" else "Action Plan"
        actionPlanText.text = actionPlan.message

        val cardColor = when (actionPlan.urgency) {
            "high" -> resources.getColor(R.color.caries_red, theme)
            "medium" -> resources.getColor(R.color.plaque_yellow, theme)
            else -> resources.getColor(R.color.healthy_green, theme)
        }
        actionPlanCard.setCardBackgroundColor(cardColor)
        actionPlanCard.visibility = View.VISIBLE

        // Option A — Condition Info Card
        val info = ConditionEducation.getConditionInfo(condition, isUrdu)
        conditionInfoTitle.text = if (isUrdu) "اس بیماری کے بارے میں" else "About this condition"
        conditionWhatText.text = info.what
        conditionCauseTitle.text = if (isUrdu) "وجوہات" else "Causes"
        conditionCauseText.text = info.causes
        conditionPreventTitle.text = if (isUrdu) "بچاؤ" else "Prevention"
        conditionPreventText.text = info.prevention
        conditionInfoCard.visibility = View.VISIBLE

        // Toggle expand/collapse
        conditionInfoToggle.setOnClickListener {
            if (conditionInfoContent.visibility == View.GONE) {
                conditionInfoContent.visibility = View.VISIBLE
                conditionInfoToggle.text = if (isUrdu) "▲ چھپائیں" else "▲ Hide"
            } else {
                conditionInfoContent.visibility = View.GONE
                conditionInfoToggle.text = if (isUrdu) "▼ دکھائیں" else "▼ Show"
            }
        }
    }

    private fun saveScan() {
        val detection = currentDetections.maxByOrNull { it.confidence } ?: return
        val isUrdu = LanguageManager.isUrdu(this)

        // imagePath is already a JSON array from the wizard — save it as-is
        // If it's a plain path (old flow), wrap it
        val imagePathJson = if (imagePath.trim().startsWith("[")) {
            imagePath  // already a JSON array — use directly
        } else {
            "[\"${imagePath.replace("\\", "\\\\").replace("\"", "\\\"")}\"]"
        }

        val scan = ScanEntity(
            userId = getCurrentUserId(),
            userName = getCurrentUserName(),
            imagePath = imagePathJson,
            condition = detection.className,
            confidence = detection.confidence,
            severity = ImageUtils.getSeverityText(detection.confidence),
            boundingBoxes = detectionsToJson(currentDetections)
        )

        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(this@AnalysisActivity)
                .scanDao()
                .insertScan(scan)

            withContext(Dispatchers.Main) {
                val msg = if (isUrdu) "ریکارڈ محفوظ ہو گیا" else "Scan saved successfully"
                VoiceFeedback.speak(msg)
                finish()
            }
        }
    }

    private fun detectionsToJson(detections: List<TFLiteHelper.Detection>): String {
        val jsonArray = JSONArray()
        detections.forEach { d ->
            val obj = JSONObject().apply {
                put("class", d.className)
                put("confidence", d.confidence)
                put("x", d.boundingBox.centerX())
                put("y", d.boundingBox.centerY())
                put("width", d.boundingBox.width())
                put("height", d.boundingBox.height())
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private fun getCurrentUserId(): String {
        return ProfileManager.getActiveProfileId(this).toString()
    }

    private fun getCurrentUserName(): String {
        return ProfileManager.getActiveProfileName(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        tfliteHelper?.close()
    }
}