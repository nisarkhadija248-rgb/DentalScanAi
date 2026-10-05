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
import com.dentalscan.ml.DetectionInput
import com.dentalscan.ml.MouthFocus
import com.dentalscan.ml.TeethGate
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
import com.dentalscan.utils.ConditionStyle
import com.dentalscan.utils.ActionPlanBuilder
import com.dentalscan.utils.PhotoLoader
import com.dentalscan.utils.AngleNames
import com.dentalscan.utils.HealthScore
import com.dentalscan.utils.ReminderScheduler
import com.dentalscan.utils.RiskAssessment
import android.graphics.RectF
import android.widget.ScrollView
import java.io.File
import android.graphics.Paint
import android.provider.Settings
import android.view.animation.OvershootInterpolator
import kotlinx.coroutines.NonCancellable
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import android.animation.ValueAnimator
import android.os.SystemClock
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator

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

    // Visual-only views
    private lateinit var resultCard: MaterialCardView
    private lateinit var resultIcon: ImageView
    private lateinit var conditionChip: Chip
    private lateinit var severityChip: Chip
    private lateinit var loadingIndicator: LinearProgressIndicator
    private lateinit var confidenceBar: LinearProgressIndicator
    private lateinit var actionPlanStripe: View
    private lateinit var actionPlanIcon: ImageView
    private lateinit var conditionInfoHeader: View
    private lateinit var conditionInfoChevron: ImageView
    private lateinit var conditionWhatTitle: TextView

    // Animation views / animators (visual only)
    private lateinit var scanOverlay: View
    private lateinit var scanBeam: View
    private lateinit var gaugeContainer: View
    private lateinit var confidenceGauge: CircularProgressIndicator
    private lateinit var confidenceGaugeText: TextView
    // Quick tips row
    private lateinit var tipsSection: LinearLayout
    private lateinit var tipsTitle: TextView
    private lateinit var tipsList: LinearLayout

    // Action Plan checklist
    private lateinit var planFoundText: TextView
    private lateinit var planProgressText: TextView
    private lateinit var planProgressBar: LinearProgressIndicator
    private lateinit var planStepsList: LinearLayout
    private lateinit var planCongratsRow: View
    private lateinit var planCongratsIcon: ImageView
    private lateinit var planCongratsText: TextView
    private var plan: ActionPlanBuilder.Plan? = null
    private val checkedSteps = mutableSetOf<Int>()
    private var currentScanId: Long = -1L
    private var isViewOnlyMode = false

    private lateinit var findingsCard: View
    private lateinit var findingsTitle: TextView
    private lateinit var findingsList: LinearLayout
    private lateinit var photosSection: View
    private lateinit var photosLabel: TextView
    private lateinit var photosList: LinearLayout
    private var scanAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var gaugeAnimator: ValueAnimator? = null

    private lateinit var multiConditionsText: TextView
    private lateinit var resultScroll: ScrollView

    private var tfliteHelper: TFLiteHelper? = null
    // One detection per condition (the highest confidence one), worst first. Drives the summary,
    // the Action Plan and what gets saved as the scan's main condition.
    private var currentDetections: List<TFLiteHelper.Detection> = emptyList()
    private var imagePath: String = ""

    /** A detection together with the capture angle (0..4) it was found in. */
    private class Hit(val detection: TFLiteHelper.Detection, val angle: Int)

    // Multi-angle state. anglePaths has one slot per capture step ("" = skipped step).
    private var anglePaths: List<String> = emptyList()
    private var anglesTrusted = false
    private var allHits: List<Hit> = emptyList()      // every detection from every angle
    private var mergedHits: List<Hit> = emptyList()   // one per condition, worst first
    private val angleBitmaps = HashMap<Int, Bitmap>() // annotated photo (shrunk for the screen) per angle
    private var selectedAngle = 0
    private var angleSwitchEnabled = false            // false while the analysis is still running
    private class ThumbRef(val card: MaterialCardView, val strokeWidth: Int, val strokeColor: Int)
    private val thumbRefs = HashMap<Int, ThumbRef>()
    private val conditionGaugeAnimators = mutableListOf<ValueAnimator>()

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

        resultCard = findViewById(R.id.resultCard)
        resultIcon = findViewById(R.id.resultIcon)
        conditionChip = findViewById(R.id.conditionChip)
        severityChip = findViewById(R.id.severityChip)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        confidenceBar = findViewById(R.id.confidenceBar)
        actionPlanStripe = findViewById(R.id.actionPlanStripe)
        actionPlanIcon = findViewById(R.id.actionPlanIcon)
        conditionInfoHeader = findViewById(R.id.conditionInfoHeader)
        conditionInfoChevron = findViewById(R.id.conditionInfoChevron)
        conditionWhatTitle = findViewById(R.id.conditionWhatTitle)
        scanOverlay = findViewById(R.id.scanOverlay)
        scanBeam = findViewById(R.id.scanBeam)
        gaugeContainer = findViewById(R.id.gaugeContainer)
        confidenceGauge = findViewById(R.id.confidenceGauge)
        confidenceGaugeText = findViewById(R.id.confidenceGaugeText)
        tipsSection = findViewById(R.id.tipsSection)
        tipsTitle = findViewById(R.id.tipsTitle)
        tipsList = findViewById(R.id.tipsList)
        planFoundText = findViewById(R.id.planFoundText)
        planProgressText = findViewById(R.id.planProgressText)
        planProgressBar = findViewById(R.id.planProgressBar)
        planStepsList = findViewById(R.id.planStepsList)
        planCongratsRow = findViewById(R.id.planCongratsRow)
        planCongratsIcon = findViewById(R.id.planCongratsIcon)
        planCongratsText = findViewById(R.id.planCongratsText)
        findingsCard = findViewById(R.id.findingsCard)
        findingsTitle = findViewById(R.id.findingsTitle)
        findingsList = findViewById(R.id.findingsList)
        photosSection = findViewById(R.id.photosSection)
        photosLabel = findViewById(R.id.photosLabel)
        photosList = findViewById(R.id.photosList)
        multiConditionsText = findViewById(R.id.multiConditionsText)
        resultScroll = findViewById(R.id.resultScroll)

        imagePath = intent.getStringExtra("image_path") ?: return

        // One slot per capture step; "" = skipped step. Plain-path (old) scans give a single slot.
        anglePaths = ScanEntity(imagePath = imagePath).getAllImagePaths()
        anglesTrusted = AngleNames.positionsAreAngles(anglePaths)
        selectedAngle = anglePaths.indexOfFirst { it.isNotEmpty() }.coerceAtLeast(0)

        val isUrdu = LanguageManager.isUrdu(this)
        setupLegend(isUrdu)
        setupPhotos(isUrdu)
        resultText.text = if (isUrdu) "تجزیہ ہو رہا ہے ..." else "Analyzing..."
        confidenceText.text = if (isUrdu) "براہ کرم انتظار کریں" else "Please wait"

        saveButton.setOnClickListener { saveScan() }
        retakeButton.setOnClickListener { finish() }
        VoiceFeedback.attachMuteButton(findViewById(R.id.resultMuteButton))

        val isViewOnly = intent.getBooleanExtra("view_only", false)
        isViewOnlyMode = isViewOnly

        if (isViewOnly) {
            saveButton.visibility = View.GONE
            retakeButton.visibility = View.GONE

            // Saved scan: which row to update, and which steps were ticked last time
            currentScanId = intent.getLongExtra("scan_id", -1L)
            checkedSteps.addAll(ActionPlanBuilder.parseChecked(intent.getStringExtra("checked_steps")))

            val condition = intent.getStringExtra("condition") ?: ""
            val confidence = intent.getFloatExtra("confidence", 0f)
            val severity = intent.getStringExtra("severity") ?: ""

            // imagePath may be a JSON array or a plain path — getThumbnailPath handles both
            val tempScan = com.dentalscan.data.ScanEntity(imagePath = imagePath)
            val bitmap = BitmapFactory.decodeFile(tempScan.getThumbnailPath())
            imageView.setImageBitmap(bitmap) // shown at once, replaced by the annotated photo below

            // A saved healthy scan: green healthy card, no conditions, no Action Plan
            if (ConditionStyle.isHealthy(condition)) {
                angleSwitchEnabled = true
                showHealthyState(isUrdu)
                selectAngle(selectedAngle)
                return
            }

            // Rebuild what was found (and in which angle) from the saved scan.
            // Scans saved before multi-angle analysis have no angle: everything counts as angle 0.
            allHits = parseSavedHits(intent.getStringExtra("bounding_boxes"))
            mergedHits = mergeHits(allHits)
            if (mergedHits.isEmpty() && condition.isNotEmpty()) {
                // Nothing usable saved: show just the main condition (display only, no box to draw)
                mergedHits = listOf(Hit(TFLiteHelper.Detection(0, condition, confidence, RectF()), 0))
            }
            angleSwitchEnabled = true

            showResultUi(condition, (confidence * 100).toInt(), severity, isUrdu)
            showConditions(mergedHits, isUrdu)
            selectAngle(selectedAngle)

            // Show educational content in view only mode too
            if (condition.isNotEmpty() && severity.isNotEmpty()) {
                lifecycleScope.launch {
                    // The History list keeps the scans it loaded when it opened, so the ticks it
                    // passes along can be out of date. The database is the source of truth.
                    if (currentScanId > 0L) {
                        val saved = withContext(Dispatchers.IO) {
                            AppDatabase.getDatabase(this@AnalysisActivity)
                                .scanDao()
                                .getScanById(currentScanId)
                        }
                        if (saved != null) {
                            checkedSteps.clear()
                            checkedSteps.addAll(ActionPlanBuilder.parseChecked(saved.checkedSteps))
                        }
                    }
                    showEducationalContent(condition, severity, savedFindings(condition, confidence))
                }
            }

        } else {
            // Re-analysis of a saved scan (from History): run the current model again, never save it,
            // because the photos belong to the original scan
            if (intent.getBooleanExtra("reanalyse", false)) {
                saveButton.visibility = View.GONE
                android.widget.Toast.makeText(this, R.string.scan_reanalyse_note, android.widget.Toast.LENGTH_LONG).show()
            }

            // Save is only possible once the result is in (otherwise an empty result would look healthy)
            saveButton.isEnabled = false
            startScanAnimation()
            analyzeImage()
        }
    }

    private fun analyzeImage() {
        val startedAt = SystemClock.elapsedRealtime()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                tfliteHelper = TFLiteHelper(this@AnalysisActivity)

                // Angles that actually have a photo (skipped steps are "" and missing files are ignored).
                // In dummy mode only the first photo is analysed (it returns a fake result).
                val existing = anglePaths.indices.filter {
                    anglePaths[it].isNotEmpty() && File(anglePaths[it]).exists()
                }
                val toAnalyse = if (TFLiteHelper.DUMMY_MODE) existing.take(1) else existing

                if (toAnalyse.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        val isUrdu = LanguageManager.isUrdu(this@AnalysisActivity)
                        hideLoading()
                        resultText.text = if (isUrdu) "تصویر نہیں ملی" else "Image not found"
                        confidenceText.text = if (isUrdu) "دوبارہ اسکین کریں" else "Please retake scan"
                    }
                    return@launch
                }

                // One photo after the other on this background thread (the model is shared)
                val hits = mutableListOf<Hit>()
                val annotated = HashMap<Int, Bitmap>()
                var analysed = 0
                // Finds where the mouth is, so eyes / skin / background are left out of the analysis.
                // null (model file missing, or dummy mode) = analyse whole photos as before.
                val gate = if (MouthFocus.ENABLED && !TFLiteHelper.DUMMY_MODE) {
                    TeethGate.create(this@AnalysisActivity)
                } else {
                    null
                }
                try {
                toAnalyse.forEachIndexed { n, angle ->
                    withContext(Dispatchers.Main) {
                        confidenceText.text =
                            getString(R.string.analysing_angle, n + 1, toAnalyse.size)
                    }

                    // The saved photo (up to 1280 px, its own shape) is what the detector looks at;
                    // a smaller copy is only for showing on screen and drawing the boxes
                    val source = BitmapFactory.decodeFile(anglePaths[angle]) ?: return@forEachIndexed
                    val scaled = DetectionInput.scaleToMax(source, DISPLAY_MAX_SIDE)

                    // Show the photo being analysed so the scan line sweeps over it
                    withContext(Dispatchers.Main) { imageView.setImageBitmap(scaled) }

                    // The part of the photo with teeth (null = whole photo)
                    val region = try {
                        gate?.let { MouthFocus.findRegion(it, scaled) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }

                    val found = try {
                        // Detect on the mouth area only (or the whole photo), at full sharpness,
                        // then put the boxes back onto the full photo
                        val target = if (region != null) MouthFocus.cut(source, region) else source
                        val prepared = DetectionInput.prepare(target)
                        val onTarget = (tfliteHelper?.detect(prepared.bitmap) ?: emptyList())
                            .map { DetectionInput.toSource(it, prepared) }
                        if (region != null) onTarget.map { MouthFocus.toFullPhoto(it, region) } else onTarget
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return@forEachIndexed // one bad photo must not stop the others
                    }
                    analysed++

                    found.forEach { hits += Hit(it, angle) }
                    val drawn = if (found.isEmpty()) scaled else ImageUtils.drawDetections(scaled, found)
                    // A dashed outline shows which area was analysed
                    annotated[angle] = if (region != null) MouthFocus.withOutline(drawn, region) else drawn
                }
                } finally {
                    gate?.close()
                }
                if (analysed == 0) throw IllegalStateException("No photo could be analysed")

                val merged = mergeHits(hits)
                val primaryDetection = merged.firstOrNull()?.detection

                // Inference is very fast on-device; keep the scanning animation visible
                // long enough to be seen (presentation only, the result is already computed)
                // (skipped when animations are off, so there is nothing to wait for)
                val remaining = if (animationsEnabled()) {
                    MIN_SCAN_MS - (SystemClock.elapsedRealtime() - startedAt)
                } else {
                    0L
                }
                if (remaining > 0) kotlinx.coroutines.delay(remaining)

                withContext(Dispatchers.Main) {
                    allHits = hits
                    mergedHits = merged
                    currentDetections = merged.map { it.detection }
                    angleBitmaps.clear()
                    angleBitmaps.putAll(annotated)

                    // Main image: the front view (first photo) with its own boxes
                    selectedAngle = toAnalyse.first()
                    updateThumbSelection()
                    imageView.setImageBitmap(annotated[selectedAngle])
                    angleSwitchEnabled = true
                    saveButton.isEnabled = true

                    displayResults(primaryDetection)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val isUrdu = LanguageManager.isUrdu(this@AnalysisActivity)
                    hideLoading()
                    resultText.text = if (isUrdu) "تجزیے میں خرابی" else "Analysis error"
                    confidenceText.text = e.message ?: if (isUrdu) "دوبارہ کوشش کریں" else "Please try again"
                }
            }
        }
    }

    /** Green "no issues" card; no conditions list, Action Plan or tips. */
    private fun showHealthyState(isUrdu: Boolean) {
        resultText.text = if (isUrdu) "کوئی مسئلہ نہیں ملا" else "No dental issues detected"
        confidenceText.text = if (isUrdu) "آپ کے دانت صحت مند ہیں" else "Your teeth appear healthy"
        showHealthyUi()
        multiConditionsText.visibility = View.GONE
        findingsCard.visibility = View.GONE
    }

    /**
     * One hit per condition: the highest-confidence detection of that condition across all angles
     * (the lowest angle wins a tie), sorted worst (highest confidence) first.
     */
    private fun mergeHits(hits: List<Hit>): List<Hit> =
        hits.groupBy { ConditionStyle.of(it.detection.className)?.shortName ?: it.detection.className }
            .values
            .map { group -> group.maxByOrNull { it.detection.confidence }!! }
            .sortedByDescending { it.detection.confidence }

    private fun displayResults(detection: TFLiteHelper.Detection?) {
        val isUrdu = LanguageManager.isUrdu(this)

        if (detection == null) {
            showHealthyState(isUrdu)
            // A healthy result can be saved too (stored as condition "Healthy")
            VoiceFeedback.speak(if (isUrdu) "کوئی مسئلہ نہیں ملا" else "No dental issues detected")
            return
        }

        val condition = detection.className
        val confidence = (detection.confidence * 100).toInt()
        val severity = ImageUtils.getSeverityText(detection.confidence)

        showResultUi(condition, confidence, severity, isUrdu)
        showConditions(mergedHits, isUrdu)

        VoiceFeedback.speakDetectionResult(condition, severity, isUrdu)

        val guidance = ImageUtils.getGuidanceMessage(condition, isUrdu)
        lifecycleScope.launch {
            kotlinx.coroutines.delay(3000)
            VoiceFeedback.speak(guidance)
        }

        // Show educational content after results
        showEducationalContent(
            condition,
            severity,
            currentDetections.map { ActionPlanBuilder.Finding(it.className, it.confidence) }
        )
    }

    /** Conditions of a saved scan: every saved detection, or just the main one for older scans. */
    private fun savedFindings(condition: String, confidence: Float): List<ActionPlanBuilder.Finding> {
        val fromBoxes = parseSavedDetections(intent.getStringExtra("bounding_boxes"))
            .map { ActionPlanBuilder.Finding(it.first, it.second) }
        return if (fromBoxes.any { ConditionStyle.of(it.condition) != null }) {
            fromBoxes
        } else {
            listOf(ActionPlanBuilder.Finding(condition, confidence))
        }
    }

    private fun showEducationalContent(
        condition: String,
        severity: String,
        findings: List<ActionPlanBuilder.Finding>
    ) {
        val isUrdu = LanguageManager.isUrdu(this)

        // Interactive Action Plan (numbered steps with checkboxes)
        val aboutDelay = renderActionPlan(findings, isUrdu)

        // Option A — Condition Info Card
        val info = ConditionEducation.getConditionInfo(condition, isUrdu)
        conditionInfoTitle.text = if (isUrdu) "اس بیماری کے بارے میں" else "About this condition"
        conditionWhatText.text = info.what
        conditionCauseTitle.text = if (isUrdu) "وجوہات" else "Causes"
        conditionCauseText.text = info.causes
        conditionPreventTitle.text = if (isUrdu) "بچاؤ" else "Prevention"
        conditionPreventText.text = info.prevention
        conditionInfoCard.visibility = View.VISIBLE

        conditionWhatTitle.text = if (isUrdu) "جائزہ" else "Overview"
        setLabelIcon(conditionWhatTitle, R.drawable.ic_info)
        setLabelIcon(conditionCauseTitle, R.drawable.ic_warning)
        setLabelIcon(conditionPreventTitle, R.drawable.ic_check_circle)

        // Toggle expand/collapse (the whole header row is tappable; chevron rotates)
        conditionInfoToggle.text = if (isUrdu) "دکھائیں" else "Show"
        conditionInfoChevron.rotation = 0f
        conditionInfoContent.visibility = View.GONE
        conditionInfoHeader.setOnClickListener {
            if (conditionInfoContent.visibility == View.GONE) {
                conditionInfoContent.visibility = View.VISIBLE
                conditionInfoToggle.text = if (isUrdu) "چھپائیں" else "Hide"
                conditionInfoChevron.animate().rotation(180f).setDuration(200).start()
            } else {
                conditionInfoContent.visibility = View.GONE
                conditionInfoToggle.text = if (isUrdu) "دکھائیں" else "Show"
                conditionInfoChevron.animate().rotation(0f).setDuration(200).start()
            }
        }

        // "About this condition" glides in last, after the plan and the tips
        playIn(conditionInfoCard, aboutDelay, dy = dp(24).toFloat())
    }

    // ---------- Visual helpers (styling only) ----------

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /**
     * Scrollable row with a thumbnail for every captured angle (skipped steps are left out).
     * Tap one to show that photo, with its own boxes, in the main image; long-press to enlarge it.
     * The selected thumbnail has a teal outline.
     */
    private fun setupPhotos(isUrdu: Boolean) {
        val present = anglePaths.withIndex().filter { it.value.isNotEmpty() }
        if (present.isEmpty()) return

        photosLabel.text = getString(R.string.photos_captured, present.size)
        photosSection.visibility = View.VISIBLE
        photosList.removeAllViews()
        thumbRefs.clear()

        present.forEach { (index, path) ->
            val item = layoutInflater.inflate(R.layout.item_photo_thumb, photosList, false)
            val card = item.findViewById<MaterialCardView>(R.id.thumbCard)
            val img = item.findViewById<ImageView>(R.id.thumbImage)
            val label = item.findViewById<TextView>(R.id.thumbLabel)

            label.text = AngleNames.label(this, index, anglesTrusted)
            thumbRefs[index] = ThumbRef(card, card.strokeWidth, card.strokeColor)

            item.setOnClickListener { selectAngle(index) }
            item.setOnLongClickListener {
                showPhotoDialog(path, label.text.toString(), isUrdu)
                true
            }
            photosList.addView(item)

            lifecycleScope.launch {
                val bmp = withContext(Dispatchers.IO) { PhotoLoader.decodeSampled(path, dp(76)) }
                if (bmp != null) {
                    img.setImageBitmap(bmp)
                } else {
                    img.scaleType = ImageView.ScaleType.CENTER_INSIDE
                    img.setImageResource(R.drawable.ic_camera)
                }
            }
        }
        updateThumbSelection()
    }

    /** Teal outline on the selected angle's thumbnail, the normal outline on the others. */
    private fun updateThumbSelection() {
        thumbRefs.forEach { (index, ref) ->
            if (index == selectedAngle) {
                ref.card.strokeColor = ContextCompat.getColor(this, R.color.primary)
                ref.card.strokeWidth = dp(2)
            } else {
                ref.card.strokeColor = ref.strokeColor
                ref.card.strokeWidth = ref.strokeWidth
            }
        }
    }

    /** Show [angle]'s photo with the boxes found in it. Ignored while the analysis is running. */
    private fun selectAngle(angle: Int) {
        if (!angleSwitchEnabled) return
        selectedAngle = angle
        updateThumbSelection()
        lifecycleScope.launch {
            val bmp = angleBitmaps[angle] ?: withContext(Dispatchers.IO) { buildAngleBitmap(angle) }
            // The user may have tapped another angle while this one was loading
            if (bmp != null && selectedAngle == angle) {
                angleBitmaps[angle] = bmp
                imageView.setImageBitmap(bmp)
            }
        }
    }

    /**
     * The photo of [angle] (shrunk for the screen, its own shape) with the boxes of the detections found in it.
     * Used for saved scans, and for angles that were not analysed (dummy mode): then no boxes.
     * Boxes are fractions of the whole photo, so this works for both old square photos and new ones.
     */
    private fun buildAngleBitmap(angle: Int): Bitmap? {
        val path = anglePaths.getOrNull(angle)?.takeIf { it.isNotEmpty() } ?: return null
        val raw = BitmapFactory.decodeFile(path) ?: return null
        val scaled = DetectionInput.scaleToMax(raw, DISPLAY_MAX_SIDE)
        val found = allHits.filter { it.angle == angle }.map { it.detection }
        return if (found.isEmpty()) scaled else ImageUtils.drawDetections(scaled, found)
    }

    private fun showPhotoDialog(path: String, title: String, isUrdu: Boolean) {
        lifecycleScope.launch {
            val bmp = withContext(Dispatchers.IO) { PhotoLoader.decodeSampled(path, 1024) } ?: return@launch
            val view = ImageView(this@AnalysisActivity).apply {
                setImageBitmap(bmp)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(dp(12), dp(8), dp(12), dp(8))
            }
            androidx.appcompat.app.AlertDialog.Builder(this@AnalysisActivity)
                .setTitle(title)
                .setView(view)
                .setPositiveButton(if (isUrdu) "بند کریں" else "Close", null)
                .show()
        }
    }

    // ---------- Interactive Action Plan ----------

    /**
     * Builds the header, headline, numbered step cards and progress for the detected conditions.
     * Returns the delay (ms) at which the card after the tips ("About this condition") should appear.
     */
    private fun renderActionPlan(findings: List<ActionPlanBuilder.Finding>, isUrdu: Boolean): Long {
        val p = ActionPlanBuilder.build(this, findings)
        plan = p

        // Severity colors live inside this card only (condition colors are used everywhere else)
        val accentRes = when (p.urgency) {
            ActionPlanBuilder.Urgency.HIGH -> R.color.caries_red
            ActionPlanBuilder.Urgency.MEDIUM -> R.color.calculus_orange
            ActionPlanBuilder.Urgency.LOW -> R.color.healthy_green
        }
        // Orange is too light for text on a white card, so the medium header text uses a deeper orange
        val headerTextRes =
            if (p.urgency == ActionPlanBuilder.Urgency.MEDIUM) R.color.notice_fg else accentRes
        val accent = ContextCompat.getColor(this, accentRes)
        val headerColor = ContextCompat.getColor(this, headerTextRes)

        actionPlanTitle.text = p.header
        actionPlanTitle.setTextColor(headerColor)
        actionPlanText.text = p.headline
        actionPlanStripe.setBackgroundColor(accent)
        actionPlanIcon.setImageResource(
            when (p.urgency) {
                ActionPlanBuilder.Urgency.HIGH -> R.drawable.ic_warning
                ActionPlanBuilder.Urgency.MEDIUM -> R.drawable.ic_info
                ActionPlanBuilder.Urgency.LOW -> R.drawable.ic_check_circle
            }
        )
        actionPlanIcon.imageTintList = ColorStateList.valueOf(headerColor)

        if (p.foundLine != null) {
            val names = p.conditions.joinToString(" · ") { ConditionStyle.displayName(it, isUrdu) }
            planFoundText.text = "${p.foundLine}: $names"
            planFoundText.visibility = View.VISIBLE
        } else {
            planFoundText.visibility = View.GONE
        }

        // Ignore any restored tick that no longer matches a step
        checkedSteps.retainAll { it in p.steps.indices }

        val numberTextColor = ContextCompat.getColor(
            this,
            if (p.urgency == ActionPlanBuilder.Urgency.MEDIUM) R.color.color_on_yellow else R.color.white
        )

        planStepsList.removeAllViews()
        p.steps.forEachIndexed { index, stepText ->
            val row = layoutInflater.inflate(R.layout.item_action_step, planStepsList, false) as MaterialCardView
            val number = row.findViewById<TextView>(R.id.stepNumber)
            number.text = (index + 1).toString()
            number.backgroundTintList = ColorStateList.valueOf(accent)
            number.setTextColor(numberTextColor)
            row.findViewById<TextView>(R.id.stepText).text = stepText
            row.setOnClickListener { toggleStep(index, row) }
            applyStepState(row, index, animate = false)
            planStepsList.addView(row)
            // Steps appear one by one
            playIn(row, stepDelay(index), dy = dp(20).toFloat(), duration = 300L)
        }

        planProgressBar.setIndicatorColor(accent)
        updateChecklistProgress(animate = false)
        actionPlanCard.visibility = View.VISIBLE
        playIn(actionPlanCard, PLAN_CARD_DELAY_MS, dy = dp(24).toFloat())

        // Tips slide in once the last step is on its way
        val tipsStart = stepDelay(p.steps.lastIndex) + 200L
        return renderQuickTips(p.tips, tipsStart)
    }

    // ---------- Quick tips ----------

    /** Compact tip cards for the detected condition(s). Collapsed by default; tap to expand. */
    private fun renderQuickTips(tips: List<ActionPlanBuilder.Tip>, baseDelay: Long): Long {
        tipsList.removeAllViews()
        if (tips.isEmpty()) {
            tipsSection.visibility = View.GONE
            return baseDelay
        }

        tipsTitle.text = getString(R.string.plan_quick_tips)

        tips.forEach { tip ->
            val card = layoutInflater.inflate(R.layout.item_quick_tip, tipsList, false) as MaterialCardView
            card.findViewById<ImageView>(R.id.tipIcon).setImageResource(tip.iconRes)
            card.findViewById<TextView>(R.id.tipTitle).text = tip.title
            card.findViewById<TextView>(R.id.tipSummary).text = tip.summary
            card.findViewById<TextView>(R.id.tipFull).text = tip.fullText
            card.findViewById<TextView>(R.id.tipMore).text = getString(R.string.tip_tap_more)
            card.setOnClickListener { toggleTip(card) }
            tipsList.addView(card)
        }

        tipsSection.visibility = View.VISIBLE
        slideInTips(baseDelay)
        // The next card starts after the last tip has begun to slide in
        return baseDelay + TIP_STAGGER_MS * tips.size
    }

    /** Expand inline to show the full tip (card also gets wider); tap again to collapse. */
    private fun toggleTip(card: MaterialCardView) {
        val full = card.findViewById<TextView>(R.id.tipFull)
        val more = card.findViewById<TextView>(R.id.tipMore)
        val chevron = card.findViewById<ImageView>(R.id.tipChevron)
        val expanding = full.visibility != View.VISIBLE

        // Smooth grow / shrink of the card and the row
        android.transition.TransitionManager.beginDelayedTransition(
            tipsSection,
            android.transition.AutoTransition().setDuration(220)
        )

        full.visibility = if (expanding) View.VISIBLE else View.GONE
        more.text = getString(if (expanding) R.string.tip_tap_less else R.string.tip_tap_more)
        card.layoutParams = card.layoutParams.apply { width = dp(if (expanding) 264 else 160) }
        chevron.animate().rotation(if (expanding) 180f else 0f).setDuration(200).start()
    }

    /** Tip cards slide in from the right, one after the other. */
    private fun slideInTips(baseDelay: Long) {
        for (i in 0 until tipsList.childCount) {
            playIn(tipsList.getChildAt(i), baseDelay + i * TIP_STAGGER_MS, dx = dp(72).toFloat())
        }
    }

    /** Ticked: green check, soft green card, struck-through text. Unticked: empty circle. */
    private fun applyStepState(row: MaterialCardView, index: Int, animate: Boolean) {
        val checked = index in checkedSteps
        val text = row.findViewById<TextView>(R.id.stepText)
        val check = row.findViewById<ImageView>(R.id.stepCheck)

        row.setCardBackgroundColor(
            ContextCompat.getColor(this, if (checked) R.color.healthy_bg else R.color.surface_white)
        )
        row.strokeColor =
            ContextCompat.getColor(this, if (checked) R.color.healthy_green else R.color.divider)

        text.paintFlags = if (checked) {
            text.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            text.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
        text.alpha = if (checked) 0.75f else 1f

        check.setImageResource(if (checked) R.drawable.ic_check_circle else R.drawable.ic_radio_unchecked)
        check.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, if (checked) R.color.healthy_green else R.color.text_secondary)
        )
        check.contentDescription = getString(
            if (checked) R.string.plan_step_uncheck_desc else R.string.plan_step_check_desc,
            index + 1
        )

        if (animate) {
            check.scaleX = 0.6f
            check.scaleY = 0.6f
            check.animate().scaleX(1f).scaleY(1f).setDuration(220)
                .setInterpolator(OvershootInterpolator()).start()
        }
    }

    private fun toggleStep(index: Int, row: MaterialCardView) {
        if (!checkedSteps.add(index)) checkedSteps.remove(index)
        applyStepState(row, index, animate = true)
        updateChecklistProgress(animate = true)
        persistChecklist()
    }

    /** "2 of 4 steps completed", the progress bar, and the congratulations message at 100%. */
    private fun updateChecklistProgress(animate: Boolean) {
        val total = plan?.steps?.size ?: 0
        val done = checkedSteps.count { it in 0 until total }

        planProgressText.text = getString(R.string.plan_progress, done, total)
        planProgressBar.max = maxOf(total, 1)
        planProgressBar.setProgressCompat(done, animate)

        val complete = total > 0 && done == total
        if (complete) {
            val wasHidden = planCongratsRow.visibility != View.VISIBLE
            planCongratsText.text = getString(R.string.plan_congrats)
            planCongratsRow.visibility = View.VISIBLE
            if (wasHidden && animate) {
                // Green check pops in
                planCongratsIcon.scaleX = 0f
                planCongratsIcon.scaleY = 0f
                planCongratsIcon.animate().scaleX(1f).scaleY(1f).setDuration(450)
                    .setInterpolator(OvershootInterpolator(2.5f)).start()
                planCongratsRow.alpha = 0f
                planCongratsRow.animate().alpha(1f).setDuration(250).start()
            }
        } else {
            planCongratsRow.visibility = View.GONE
        }
    }

    /**
     * Opened from History: save every tick straight away.
     * New scan: nothing is saved yet. The ticks are kept in memory and written together with the
     * scan when Save Scan is tapped (see saveScan).
     */
    private fun persistChecklist() {
        if (!isViewOnlyMode || currentScanId <= 0L) return
        val encoded = ActionPlanBuilder.encodeChecked(checkedSteps)
        val scanId = currentScanId
        // NonCancellable: still finish the write if the user leaves the screen right after ticking
        lifecycleScope.launch(Dispatchers.IO + NonCancellable) {
            AppDatabase.getDatabase(this@AnalysisActivity)
                .scanDao()
                .updateCheckedSteps(scanId, encoded)
        }
    }

    /**
     * Every detection saved with a scan, with the angle it came from. Entries from scans saved
     * before multi-angle analysis have no "angle" and count as angle 0 (the front view).
     */
    private fun parseSavedHits(json: String?): List<Hit> {
        if (json.isNullOrBlank()) return emptyList()
        val classOrder = listOf("calculus", "caries", "gingivitis")
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val name = o.optString("class")
                if (name.isBlank()) return@mapNotNull null
                val cx = o.optDouble("x", 0.0).toFloat()
                val cy = o.optDouble("y", 0.0).toFloat()
                val w = o.optDouble("width", 0.0).toFloat()
                val h = o.optDouble("height", 0.0).toFloat()
                Hit(
                    TFLiteHelper.Detection(
                        classId = classOrder.indexOf(name.trim().lowercase()).coerceAtLeast(0),
                        className = name,
                        confidence = o.optDouble("confidence", 0.0).toFloat(),
                        boundingBox = RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
                    ),
                    o.optInt("angle", 0)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Reads the detections that were saved with a scan (JSON written by detectionsToJson). */
    private fun parseSavedDetections(json: String?): List<Pair<String, Float>> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                o.optString("class") to o.optDouble("confidence", 0.0).toFloat()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * "CONDITIONS FOUND (N)": one row per condition (highest confidence across all angles) with a
     * small animated gauge, the condition name in its color and the angle it was found in.
     * Tapping a row jumps to that angle's photo. Also shows "N conditions detected" at the top
     * of the result card when there is more than one. Display only.
     */
    private fun showConditions(hits: List<Hit>, isUrdu: Boolean) {
        conditionGaugeAnimators.forEach { it.cancel() }
        conditionGaugeAnimators.clear()
        findingsList.removeAllViews()

        if (hits.isEmpty()) {
            findingsCard.visibility = View.GONE
            multiConditionsText.visibility = View.GONE
            return
        }

        if (hits.size > 1) {
            multiConditionsText.text = getString(R.string.conditions_detected_summary, hits.size)
            multiConditionsText.visibility = View.VISIBLE
        } else {
            multiConditionsText.visibility = View.GONE
        }

        findingsTitle.text = getString(R.string.conditions_found, hits.size)

        hits.forEachIndexed { i, hit ->
            val name = hit.detection.className
            val pct = (hit.detection.confidence * 100).toInt().coerceIn(0, 100)
            val color = ConditionStyle.color(this, name)

            val row = layoutInflater.inflate(R.layout.item_condition_found, findingsList, false)
            row.backgroundTintList = ColorStateList.valueOf(ConditionStyle.tint(this, name))

            val gauge = row.findViewById<CircularProgressIndicator>(R.id.condGauge)
            val gaugeText = row.findViewById<TextView>(R.id.condGaugeText)
            gauge.setIndicatorColor(color)

            val title = row.findViewById<TextView>(R.id.condName)
            title.text = ConditionStyle.displayName(name, isUrdu)
            title.setTextColor(color)
            row.findViewById<TextView>(R.id.condFoundIn).text =
                getString(R.string.found_in, AngleNames.label(this, hit.angle, anglesTrusted))

            // A weak finding is shown as such instead of looking like a firm result
            row.findViewById<TextView>(R.id.condLowConfidence).apply {
                if (hit.detection.confidence < LOW_CONFIDENCE) {
                    text = getString(R.string.finding_low_confidence)
                    visibility = View.VISIBLE
                }
            }

            row.setOnClickListener {
                if (anglePaths.getOrNull(hit.angle).isNullOrEmpty()) return@setOnClickListener
                selectAngle(hit.angle)
                resultScroll.smoothScrollTo(0, 0)
            }

            // Gauge fills after the card has slid in
            if (animationsEnabled()) {
                gauge.setProgressCompat(0, false)
                gaugeText.text = "0%"
                val anim = ValueAnimator.ofInt(0, pct).apply {
                    startDelay = FINDINGS_DELAY_MS + 250L + i * 150L
                    duration = 700
                    interpolator = DecelerateInterpolator()
                    addUpdateListener { a ->
                        val v = a.animatedValue as Int
                        gauge.setProgressCompat(v, false)
                        gaugeText.text = "$v%"
                    }
                }
                conditionGaugeAnimators += anim
                anim.start()
            } else {
                gauge.setProgressCompat(pct, false)
                gaugeText.text = "$pct%"
            }

            findingsList.addView(row)
        }

        findingsCard.visibility = View.VISIBLE
        playIn(findingsCard, FINDINGS_DELAY_MS, dy = dp(24).toFloat())
    }

    // ---------- Result animation sequence ----------
    //
    //   0 ms    result header card pops in
    //   150 ms  confidence gauge fills
    //   400 ms  detected-regions card
    //   600 ms  Action Plan card
    //   700 ms  plan steps, one by one (first 4 every 200 ms, the rest every 100 ms)
    //   after   quick tips slide in from the right, then the "About this condition" card
    //
    // With "Remove animations" / animation scale 0 in the phone's settings, everything
    // appears instantly in its final state.

    /** False when the user has turned system animations off (reduce motion). */
    private fun animationsEnabled(): Boolean = try {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    } catch (e: Exception) {
        true
    }

    /** Fade a view in from an offset after [delay] ms. Shows it instantly if animations are off. */
    private fun playIn(
        view: View,
        delay: Long,
        dx: Float = 0f,
        dy: Float = 0f,
        duration: Long = 350L
    ) {
        if (!animationsEnabled()) {
            view.alpha = 1f
            view.translationX = 0f
            view.translationY = 0f
            return
        }
        view.alpha = 0f
        view.translationX = dx
        view.translationY = dy
        view.animate()
            .alpha(1f)
            .translationX(0f)
            .translationY(0f)
            .setDuration(duration)
            .setStartDelay(delay)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    /** When plan step [index] starts appearing. */
    private fun stepDelay(index: Int): Long =
        STEPS_START_MS + if (index < 4) 200L * index else 200L * 3 + 100L * (index - 3)

    /** Hides the loading bar and stops the scanning animation. */
    private fun hideLoading() {
        loadingIndicator.visibility = View.GONE
        stopScanAnimation()
    }

    /** A bright line sweeps up and down over the photo while the model runs. */
    private fun startScanAnimation() {
        // Reduce motion: no sweeping line or pulsing title, the loading bar is enough
        if (!animationsEnabled()) return

        scanOverlay.visibility = View.VISIBLE
        scanOverlay.alpha = 1f

        scanAnimator?.cancel()
        scanAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1400
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { a ->
                // Read the height each frame: the image area grows once the photo is set
                val travel = (scanOverlay.height - scanBeam.height).coerceAtLeast(0)
                scanBeam.translationY = travel * (a.animatedValue as Float)
            }
            start()
        }

        // Gentle pulse on the "Analyzing..." title
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(1f, 0.45f).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { a -> resultText.alpha = a.animatedValue as Float }
            start()
        }
    }

    private fun stopScanAnimation() {
        scanAnimator?.cancel()
        scanAnimator = null
        pulseAnimator?.cancel()
        pulseAnimator = null
        resultText.alpha = 1f

        if (scanOverlay.visibility == View.VISIBLE) {
            scanOverlay.animate().alpha(0f).setDuration(250).withEndAction {
                scanOverlay.visibility = View.GONE
                scanOverlay.alpha = 1f
            }.start()
        }
    }

    /** Circular confidence gauge: the arc fills and the number counts up together. */
    private fun showGauge(color: Int, confidencePct: Int) {
        val target = confidencePct.coerceIn(0, 100)
        confidenceGauge.setIndicatorColor(color)
        confidenceGauge.setProgressCompat(0, false)
        confidenceGaugeText.text = "0%"
        gaugeContainer.visibility = View.VISIBLE

        gaugeAnimator?.cancel()
        if (!animationsEnabled()) {
            confidenceGauge.setProgressCompat(target, false)
            confidenceGaugeText.text = "$target%"
            return
        }
        gaugeAnimator = ValueAnimator.ofInt(0, target).apply {
            startDelay = GAUGE_DELAY_MS
            duration = 800
            interpolator = DecelerateInterpolator()
            addUpdateListener { a ->
                val v = a.animatedValue as Int
                confidenceGauge.setProgressCompat(v, false)
                confidenceGaugeText.text = "$v%"
            }
            start()
        }
    }

    private fun setupLegend(isUrdu: Boolean) {
        fun legendName(raw: String) =
            if (isUrdu) ConditionStyle.displayName(raw, true) else ConditionStyle.shortName(raw)

        findViewById<TextView>(R.id.legendCariesText).text = legendName("Caries")
        findViewById<TextView>(R.id.legendCalculusText).text = legendName("Calculus")
        findViewById<TextView>(R.id.legendGingivitisText).text = legendName("gingivitis")
    }

    /** Small tinted icon at the start of a section label. */
    private fun setLabelIcon(target: TextView, iconRes: Int) {
        val icon = ContextCompat.getDrawable(this, iconRes)?.mutate() ?: return
        icon.setBounds(0, 0, dp(16), dp(16))
        icon.setTint(ContextCompat.getColor(this, R.color.text_secondary))
        target.setCompoundDrawablesRelative(icon, null, null, null)
    }

    /** Colored condition chip, severity chip and confidence bar for a detected condition. */
    private fun showResultUi(condition: String, confidencePct: Int, severity: String, isUrdu: Boolean) {
        val color = ConditionStyle.color(this, condition)
        val tint = ConditionStyle.tint(this, condition)

        hideLoading()
        resultIcon.visibility = View.GONE
        resultCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface_white))
        resultCard.setStrokeColor(ContextCompat.getColor(this, R.color.divider))

        resultText.text = ConditionStyle.displayName(condition, isUrdu)
        val lowSuffix = if (confidencePct < (LOW_CONFIDENCE * 100).toInt()) " ${getString(R.string.confidence_low_suffix)}" else ""
        confidenceText.text =
            (if (isUrdu) "اعتماد: $confidencePct%" else "Confidence: $confidencePct%") + lowSuffix

        conditionChip.text = if (isUrdu) ConditionStyle.displayName(condition, true) else ConditionStyle.shortName(condition)
        conditionChip.chipBackgroundColor = ColorStateList.valueOf(tint)
        conditionChip.chipStrokeColor = ColorStateList.valueOf(color)
        conditionChip.chipIcon = ContextCompat.getDrawable(this, R.drawable.bg_dot)?.mutate()
        conditionChip.chipIconTint = ColorStateList.valueOf(color)
        conditionChip.chipIconSize = dp(10).toFloat()
        conditionChip.isChipIconVisible = true
        conditionChip.visibility = View.VISIBLE

        if (severity.isNotEmpty()) {
            val severityLabel = ConditionStyle.severityLabel(severity, isUrdu)
            severityChip.text = if (isUrdu) "$severityLabel شدت" else "$severity severity"
            severityChip.visibility = View.VISIBLE
        } else {
            severityChip.visibility = View.GONE
        }

        // The circular gauge replaces the linear bar
        confidenceBar.visibility = View.GONE
        showGauge(color, confidencePct)

        // Small "pop" so the result card feels like it lands (first step of the sequence)
        if (animationsEnabled()) {
            resultCard.scaleX = 0.96f
            resultCard.scaleY = 0.96f
            resultCard.animate().scaleX(1f).scaleY(1f).setDuration(300).start()
        } else {
            resultCard.scaleX = 1f
            resultCard.scaleY = 1f
        }
    }

    /** Soft green card with a check icon when nothing was detected. */
    private fun showHealthyUi() {
        hideLoading()
        conditionChip.visibility = View.GONE
        severityChip.visibility = View.GONE
        confidenceBar.visibility = View.GONE
        gaugeContainer.visibility = View.GONE
        resultIcon.visibility = View.VISIBLE
        resultCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.healthy_bg))
        resultCard.setStrokeColor(ContextCompat.getColor(this, R.color.healthy_green))
    }

    private fun saveScan() {
        // Wait for the analysis; an empty result before that would be saved as "Healthy" by mistake
        if (!angleSwitchEnabled || !saveButton.isEnabled) return
        saveButton.isEnabled = false // no double saves

        // null = nothing detected in any angle: saved as a healthy scan
        val detection = currentDetections.maxByOrNull { it.confidence }
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
            condition = detection?.className ?: "Healthy",
            confidence = detection?.confidence ?: 0f,
            severity = if (detection != null) ImageUtils.getSeverityText(detection.confidence) else "None",
            // Main condition above is the worst one (History/Comparison use it). Every detection
            // from every angle is stored here, each with its angle, to rebuild the view later.
            boundingBoxes = detectionsToJson(allHits),
            checkedSteps = ActionPlanBuilder.encodeChecked(checkedSteps)
        )

        lifecycleScope.launch(Dispatchers.IO) {
            // Dental Health Score before this scan (null = the profile had no scans yet)
            val earlierScans = HealthScore.scansFor(this@AnalysisActivity)
            val previousScore = HealthScore.compute(earlierScans)

            val newId = AppDatabase.getDatabase(this@AnalysisActivity)
                .scanDao()
                .insertScan(scan)

            // Score after this scan, saved to the profile
            val scansNow = earlierScans + scan.copy(id = newId)
            val newScore = HealthScore.compute(scansNow) ?: HealthScore.START
            HealthScore.sync(this@AnalysisActivity, newScore)
            // Risk level from the last 5 scans (null below 3 scans)
            val risk = RiskAssessment.assess(scansNow)
            // Smart reminders: work the frequency out again from this new result
            ReminderScheduler.updateAfterScan(this@AnalysisActivity, scansNow)

            withContext(Dispatchers.Main) {
                val msg = if (isUrdu) "ریکارڈ محفوظ ہو گیا" else "Scan saved successfully"
                VoiceFeedback.speak(msg)
                if (previousScore != null) {
                    // The profile had earlier scans: show how this one changed the score, then "Done"
                    showScoreUpdate(previousScore, newScore, risk)
                } else {
                    finish()
                }
            }
        }
    }

    /** Score card: old score, arrow up (green) / down (red), new score counting up or down, message. */
    private fun showScoreUpdate(previous: Int, now: Int, risk: RiskAssessment.Result?) {
        val change = now - previous
        val card = findViewById<View>(R.id.scoreCard)
        val oldText = findViewById<TextView>(R.id.scoreOldText)
        val newText = findViewById<TextView>(R.id.scoreNewText)
        val arrow = findViewById<ImageView>(R.id.scoreArrow)
        val message = findViewById<TextView>(R.id.scoreMessage)
        findViewById<TextView>(R.id.scoreTitle).text = getString(R.string.health_score_title)

        oldText.text = previous.toString()
        newText.setTextColor(ContextCompat.getColor(this, HealthScore.levelOf(now).colorRes))

        val changeColor = ContextCompat.getColor(
            this,
            when {
                change > 0 -> R.color.healthy_green
                change < 0 -> R.color.caries_red
                else -> R.color.text_secondary
            }
        )
        arrow.setImageResource(if (change < 0) R.drawable.ic_arrow_downward else R.drawable.ic_arrow_upward)
        arrow.imageTintList = ColorStateList.valueOf(changeColor)
        arrow.visibility = if (change == 0) View.INVISIBLE else View.VISIBLE

        message.text = when {
            change > 0 -> resources.getQuantityString(R.plurals.health_improved, change, change)
            change < 0 -> resources.getQuantityString(R.plurals.health_dropped, -change, -change)
            else -> getString(R.string.health_same, now)
        }
        message.setTextColor(changeColor)

        // The number counts from the old score to the new one
        if (animationsEnabled() && change != 0) {
            newText.text = previous.toString()
            ValueAnimator.ofInt(previous, now).apply {
                duration = 900
                interpolator = DecelerateInterpolator()
                addUpdateListener { newText.text = (it.animatedValue as Int).toString() }
                start()
            }
        } else {
            newText.text = now.toString()
        }

        card.visibility = View.VISIBLE
        playIn(card, 0L, dy = dp(24).toFloat())

        // Updated risk level, only when there are at least 3 saved scans
        if (risk != null) {
            val riskCard = findViewById<View>(R.id.resultRiskCard)
            val badge = findViewById<TextView>(R.id.resultRiskBadge)
            findViewById<TextView>(R.id.resultRiskTitle).text = getString(R.string.risk_title)
            badge.text = getString(risk.level.labelRes)
            badge.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, risk.level.colorRes))
            badge.setTextColor(ContextCompat.getColor(this, risk.level.onColorRes))
            findViewById<TextView>(R.id.resultRiskRecommendation).text =
                getString(risk.level.recommendationRes)
            riskCard.visibility = View.VISIBLE
            playIn(riskCard, 250L, dy = dp(24).toFloat())
        }
        resultScroll.post { resultScroll.smoothScrollTo(0, card.top) }

        // Saving is finished: the button now just closes the screen
        saveButton.text = getString(R.string.score_done)
        saveButton.isEnabled = true
        saveButton.setOnClickListener { finish() }
        retakeButton.visibility = View.GONE
    }

    private fun detectionsToJson(hits: List<Hit>): String {
        val jsonArray = JSONArray()
        hits.forEach { hit ->
            val d = hit.detection
            val obj = JSONObject().apply {
                put("angle", hit.angle)
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
        scanAnimator?.cancel()
        pulseAnimator?.cancel()
        gaugeAnimator?.cancel()
        conditionGaugeAnimators.forEach { it.cancel() }
        tfliteHelper?.close()
    }

    companion object {
        // Minimum time the scanning animation stays on screen (presentation only)
        private const val MIN_SCAN_MS = 1600L

        // Longest side of the photo copy shown on screen (the detector uses the sharp saved photo)
        private const val DISPLAY_MAX_SIDE = 960

        // Findings below this confidence are labelled "low confidence"
        private const val LOW_CONFIDENCE = 0.40f

        // Result animation timeline (milliseconds)
        private const val GAUGE_DELAY_MS = 150L
        private const val FINDINGS_DELAY_MS = 400L
        private const val PLAN_CARD_DELAY_MS = 600L
        private const val STEPS_START_MS = 700L
        private const val TIP_STAGGER_MS = 110L
    }
}