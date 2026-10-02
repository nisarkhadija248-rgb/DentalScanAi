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
import com.dentalscan.utils.ConditionStyle
import com.dentalscan.utils.ActionPlanBuilder
import com.dentalscan.utils.PhotoLoader
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

        imagePath = intent.getStringExtra("image_path") ?: return

        val isUrdu = LanguageManager.isUrdu(this)
        setupLegend(isUrdu)
        setupPhotos(isUrdu)
        resultText.text = if (isUrdu) "تجزیہ ہو رہا ہے ..." else "Analyzing..."
        confidenceText.text = if (isUrdu) "براہ کرم انتظار کریں" else "Please wait"

        saveButton.setOnClickListener { saveScan() }
        retakeButton.setOnClickListener { finish() }

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
            imageView.setImageBitmap(bitmap)

            showResultUi(condition, (confidence * 100).toInt(), severity, isUrdu)
            showFindings(parseSavedDetections(intent.getStringExtra("bounding_boxes")), isUrdu)

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
            startScanAnimation()
            analyzeImage()
        }
    }

    private fun analyzeImage() {
        val startedAt = SystemClock.elapsedRealtime()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                tfliteHelper = TFLiteHelper(this@AnalysisActivity)

                // imagePath may be a JSON array OR a plain path
                // Always extract the front view (index 0) for analysis
                val frontPath = extractFrontPath(imagePath)

                if (frontPath.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        val isUrdu = LanguageManager.isUrdu(this@AnalysisActivity)
                        hideLoading()
                        resultText.text = if (isUrdu) "تصویر نہیں ملی" else "Image not found"
                        confidenceText.text = if (isUrdu) "دوبارہ اسکین کریں" else "Please retake scan"
                    }
                    return@launch
                }

                val bitmap = BitmapFactory.decodeFile(frontPath)

                if (bitmap == null) {
                    withContext(Dispatchers.Main) {
                        val isUrdu = LanguageManager.isUrdu(this@AnalysisActivity)
                        hideLoading()
                        resultText.text = if (isUrdu) "تصویر نہیں ملی" else "Image not found"
                        confidenceText.text = if (isUrdu) "دوبارہ اسکین کریں" else "Please retake scan"
                    }
                    return@launch
                }

                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true)

                // Show the photo right away so the scan line has something to sweep over
                withContext(Dispatchers.Main) { imageView.setImageBitmap(scaledBitmap) }

                currentDetections = tfliteHelper?.detect(scaledBitmap) ?: emptyList()
                val resultBitmap = ImageUtils.drawDetections(scaledBitmap, currentDetections)
                val primaryDetection = currentDetections.maxByOrNull { it.confidence }

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
                    imageView.setImageBitmap(resultBitmap)
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
            showHealthyUi()
            // Nothing to save when nothing was detected, so don't offer a Save button that does nothing
            saveButton.visibility = View.GONE
            VoiceFeedback.speak(if (isUrdu) "کوئی مسئلہ نہیں ملا" else "No dental issues detected")
            return
        }

        val condition = detection.className
        val confidence = (detection.confidence * 100).toInt()
        val severity = ImageUtils.getSeverityText(detection.confidence)

        showResultUi(condition, confidence, severity, isUrdu)
        showFindings(currentDetections.map { it.className to it.confidence }, isUrdu)

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
     * Scrollable row with every captured photo. Photo 1 is always the front view
     * (the first step cannot be skipped) and is the one the model analyses.
     * Tap a thumbnail to see it larger. Display only; analysis is unchanged.
     */
    private fun setupPhotos(isUrdu: Boolean) {
        val paths = ScanEntity(imagePath = imagePath).getAllImagePaths().filter { it.isNotEmpty() }
        if (paths.isEmpty()) return

        photosLabel.text = if (isUrdu) "لی گئی تصاویر (${paths.size})" else "Captured photos (${paths.size})"
        photosSection.visibility = View.VISIBLE
        photosList.removeAllViews()

        paths.forEachIndexed { index, path ->
            val item = layoutInflater.inflate(R.layout.item_photo_thumb, photosList, false)
            val card = item.findViewById<MaterialCardView>(R.id.thumbCard)
            val img = item.findViewById<ImageView>(R.id.thumbImage)
            val label = item.findViewById<TextView>(R.id.thumbLabel)

            label.text = when {
                index == 0 -> if (isUrdu) "سامنے (تجزیہ)" else "Front · analysed"
                else -> if (isUrdu) "تصویر ${index + 1}" else "Photo ${index + 1}"
            }

            // Outline the photo that was analysed
            if (index == 0) {
                card.strokeColor = ContextCompat.getColor(this, R.color.primary)
                card.strokeWidth = dp(2)
            }

            item.setOnClickListener { showPhotoDialog(path, label.text.toString(), isUrdu) }
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

    /** One row per detected region: condition color dot, name, confidence. Display only. */
    private fun showFindings(detections: List<Pair<String, Float>>, isUrdu: Boolean) {
        findingsList.removeAllViews()
        val rows = detections.sortedByDescending { it.second }.take(6)
        if (rows.isEmpty()) {
            findingsCard.visibility = View.GONE
            return
        }

        findingsTitle.text =
            if (isUrdu) "نشاندہی شدہ حصے (${detections.size})" else "Detected regions (${detections.size})"

        rows.forEach { (name, confidence) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))
            }

            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(10), dp(10)).apply { marginEnd = dp(10) }
                setBackgroundResource(R.drawable.bg_dot)
                backgroundTintList = ColorStateList.valueOf(ConditionStyle.color(this@AnalysisActivity, name))
            }

            val nameView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = ConditionStyle.displayName(name, isUrdu)
                setTextAppearance(R.style.TextAppearance_App_Body)
            }

            val pct = TextView(this).apply {
                text = "${(confidence * 100).toInt()}%"
                setTextAppearance(R.style.TextAppearance_App_Title)
                textSize = 15f
            }

            row.addView(dot)
            row.addView(nameView)
            row.addView(pct)
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
        confidenceText.text = if (isUrdu) "اعتماد: $confidencePct%" else "Confidence: $confidencePct%"

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
            boundingBoxes = detectionsToJson(currentDetections),
            checkedSteps = ActionPlanBuilder.encodeChecked(checkedSteps)
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
        scanAnimator?.cancel()
        pulseAnimator?.cancel()
        gaugeAnimator?.cancel()
        tfliteHelper?.close()
    }

    companion object {
        // Minimum time the scanning animation stays on screen (presentation only)
        private const val MIN_SCAN_MS = 1600L

        // Result animation timeline (milliseconds)
        private const val GAUGE_DELAY_MS = 150L
        private const val FINDINGS_DELAY_MS = 400L
        private const val PLAN_CARD_DELAY_MS = 600L
        private const val STEPS_START_MS = 700L
        private const val TIP_STAGGER_MS = 110L
    }
}