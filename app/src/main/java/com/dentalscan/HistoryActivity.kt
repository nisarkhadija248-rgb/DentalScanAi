package com.dentalscan

import android.content.Intent
import android.os.Bundle
import android.content.res.ColorStateList
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dentalscan.adapter.ScanHistoryAdapter
import com.dentalscan.data.AppDatabase
import android.graphics.Color
import android.widget.LinearLayout
import com.dentalscan.utils.ConditionStyle
import com.dentalscan.utils.HealthScore
import com.dentalscan.utils.ReminderScheduler
import com.dentalscan.utils.RiskAssessment
import com.dentalscan.utils.ScanFiles
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.dentalscan.utils.LanguageManager
import com.dentalscan.utils.ProfileManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HistoryActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var titleText: TextView
    private lateinit var progressFeedbackText: TextView
    private lateinit var emptyState: View
    private lateinit var emptyHint: TextView
    private lateinit var historySubtitle: TextView
    private lateinit var btnStartScanEmpty: Button
    private lateinit var statsCard: View
    private lateinit var statsChart: PieChart
    private lateinit var statsTitle: TextView
    private lateinit var statsLegend: LinearLayout
    private lateinit var healthScoreCard: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // All findViewByIds MUST come after setContentView
        titleText = findViewById(R.id.titleText)
        recyclerView = findViewById(R.id.recyclerView)
        emptyText = findViewById(R.id.emptyText)
        progressFeedbackText = findViewById(R.id.progressFeedbackText)
        emptyState = findViewById(R.id.emptyState)
        emptyHint = findViewById(R.id.emptyHint)
        historySubtitle = findViewById(R.id.historySubtitle)
        btnStartScanEmpty = findViewById(R.id.btnStartScanEmpty)
        statsCard = findViewById(R.id.statsCard)
        statsChart = findViewById(R.id.statsChart)
        statsTitle = findViewById(R.id.statsTitle)
        statsLegend = findViewById(R.id.statsLegend)
        healthScoreCard = findViewById(R.id.healthScoreCard)
        btnStartScanEmpty.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        val isUrdu = LanguageManager.isUrdu(this)
        titleText.text = if (isUrdu) "سابقہ ریکارڈ" else "Scan History"

        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    // Also runs when coming back from a scan that was updated, so the list and score are current
    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@HistoryActivity)
            val activeProfileId = ProfileManager.getActiveProfileId(this@HistoryActivity)
            val isUrdu = LanguageManager.isUrdu(this@HistoryActivity)

            val scans = if (activeProfileId == -1L) {
                database.scanDao().getAllScans().first()
            } else {
                database.scanDao()
                    .getScansForUser(activeProfileId.toString())
                    .first()
            }

            if (scans.isEmpty()) {
                emptyText.text = if (isUrdu) "کوئی ریکارڈ نہیں ملا" else "No scans yet"
                emptyText.visibility = View.VISIBLE
                emptyHint.text = if (isUrdu) "آپ کے محفوظ کردہ اسکین یہاں نظر آئیں گے۔" else "Your saved scans will appear here."
                btnStartScanEmpty.text = if (isUrdu) "اسکین شروع کریں" else "Start a Scan"
                emptyState.visibility = View.VISIBLE
                historySubtitle.visibility = View.GONE
                recyclerView.visibility = View.GONE
                progressFeedbackText.visibility = View.GONE
                statsCard.visibility = View.GONE
                healthScoreCard.visibility = View.GONE
                findViewById<View>(R.id.riskCard).visibility = View.GONE
                HealthScore.sync(this@HistoryActivity, null) // no scans: back to the starting 100
            } else {
                emptyText.visibility = View.GONE
                emptyState.visibility = View.GONE
                val countText = if (isUrdu) "${scans.size} ریکارڈ" else
                    if (scans.size == 1) "1 scan" else "${scans.size} scans"
                historySubtitle.text = "$countText · ${getString(R.string.history_hold_hint)}"
                historySubtitle.visibility = View.VISIBLE
                recyclerView.visibility = View.VISIBLE
                val adapter = ScanHistoryAdapter(
                    onItemClick = { scan ->
                        val intent = Intent(this@HistoryActivity, AnalysisActivity::class.java)
                        intent.putExtra("image_path", scan.imagePath)
                        intent.putExtra("view_only", true)
                        intent.putExtra("condition", scan.condition)
                        intent.putExtra("confidence", scan.confidence)
                        intent.putExtra("severity", scan.severity)
                        intent.putExtra("bounding_boxes", scan.boundingBoxes)
                        // Needed by the Action Plan checklist: which scan to update, and what was ticked
                        intent.putExtra("scan_id", scan.id)
                        intent.putExtra("checked_steps", scan.checkedSteps)
                        startActivity(intent)
                    },
                    onItemLongClick = { scan -> showScanOptions(scan) }
                )
                adapter.submitList(scans)
                recyclerView.adapter = adapter
                showProgressFeedback(scans)
                showStats(scans, isUrdu)
                showHealthScore(scans)
                showRisk(scans)
            }
        }
    }

    /** Hold a scan: re-run the current model on its saved photos, or delete it. */
    private fun showScanOptions(scan: com.dentalscan.data.ScanEntity) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setItems(
                arrayOf(
                    getString(R.string.scan_option_reanalyse),
                    getString(R.string.scan_option_delete)
                )
            ) { _, which ->
                if (which == 0) {
                    // Opens the normal analysis on the saved photos. Nothing changes unless the user
                    // taps "Update saved scan" there (scan_id says which scan to update).
                    val intent = Intent(this, AnalysisActivity::class.java)
                    intent.putExtra("image_path", scan.imagePath)
                    intent.putExtra("reanalyse", true)
                    intent.putExtra("scan_id", scan.id)
                    startActivity(intent)
                } else {
                    confirmDelete(scan)
                }
            }
            .show()
    }

    /** Asks first, then deletes the scan and its photo files. */
    private fun confirmDelete(scan: com.dentalscan.data.ScanEntity) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.delete_scan_title)
            .setMessage(R.string.delete_scan_message)
            .setPositiveButton(R.string.delete_scan_confirm) { _, _ -> deleteScan(scan) }
            .setNegativeButton(R.string.delete_scan_cancel, null)
            .show()
    }

    private fun deleteScan(scan: com.dentalscan.data.ScanEntity) {
        lifecycleScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                ScanFiles.deletePhotos(scan)
                AppDatabase.getDatabase(this@HistoryActivity).scanDao().deleteScan(scan)
                // Smart reminders follow the latest scan, so work the frequency out again
                ReminderScheduler.updateAfterScan(this@HistoryActivity, HealthScore.scansFor(this@HistoryActivity))
            }
            android.widget.Toast.makeText(this@HistoryActivity, R.string.delete_scan_done, android.widget.Toast.LENGTH_SHORT).show()
            // Reload: the list, score, risk, trend and stats are all worked out from the remaining scans
            loadHistory()
        }
    }

    /** Risk Assessment card from the last 5 scans, or "Not enough data" below 3 scans. */
    private fun showRisk(scans: List<com.dentalscan.data.ScanEntity>) {
        val card = findViewById<View>(R.id.riskCard)
        val badge = findViewById<TextView>(R.id.riskBadge)
        val explanation = findViewById<TextView>(R.id.riskExplanation)
        val recommendation = findViewById<TextView>(R.id.riskRecommendation)
        val infoText = findViewById<TextView>(R.id.riskInfoText)

        findViewById<TextView>(R.id.riskTitle).text = getString(R.string.risk_title)
        infoText.text = getString(R.string.risk_info_text)

        // The info button shows / hides the explanation of how the level is worked out
        findViewById<View>(R.id.riskInfoButton).setOnClickListener {
            infoText.visibility = if (infoText.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        val result = RiskAssessment.assess(scans)
        if (result == null) {
            // Neutral grey badge, no recommendation yet
            badge.text = getString(R.string.risk_not_enough)
            badge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.surface_variant))
            badge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            explanation.text = getString(R.string.risk_not_enough_hint, scans.size)
            recommendation.visibility = View.GONE
        } else {
            val level = result.level
            badge.text = getString(level.labelRes)
            badge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, level.colorRes))
            badge.setTextColor(ContextCompat.getColor(this, level.onColorRes))
            explanation.text = RiskAssessment.explanation(this, result)

            recommendation.text = getString(level.recommendationRes)
            recommendation.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            val icon = ContextCompat.getDrawable(
                this,
                if (level == RiskAssessment.Level.LOW) R.drawable.ic_check_circle else R.drawable.ic_warning
            )?.mutate()
            val size = (20 * resources.displayMetrics.density).toInt()
            icon?.setBounds(0, 0, size, size)
            icon?.setTint(ContextCompat.getColor(this, level.colorRes))
            recommendation.setCompoundDrawablesRelative(icon, null, null, null)
            recommendation.visibility = View.VISIBLE
        }
        card.visibility = View.VISIBLE
    }

    /** Prominent score card: arc gauge in the score's colour, label, and the change over the last 30 days. */
    private suspend fun showHealthScore(scans: List<com.dentalscan.data.ScanEntity>) {
        val score = HealthScore.compute(scans) ?: return
        HealthScore.sync(this, score) // keep the saved profile copy equal to the real score

        val level = HealthScore.levelOf(score)
        val color = ContextCompat.getColor(this, level.colorRes)

        findViewById<TextView>(R.id.healthScoreTitle).text = getString(R.string.health_score_title)
        val label = findViewById<TextView>(R.id.healthScoreLabel)
        label.text = getString(level.labelRes)
        label.setTextColor(color)

        val valueText = findViewById<TextView>(R.id.healthScoreValue)
        val gauge = findViewById<com.google.android.material.progressindicator.CircularProgressIndicator>(R.id.healthGauge)
        gauge.setIndicatorColor(color)

        val trend = HealthScore.trend(scans)
        val trendText = findViewById<TextView>(R.id.healthScoreTrend)
        if (trend == 0) {
            trendText.text = getString(R.string.health_trend_none)
            trendText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        } else {
            val signed = if (trend > 0) "+$trend" else "$trend"
            trendText.text = getString(R.string.health_trend_month, signed)
            trendText.setTextColor(
                ContextCompat.getColor(this, if (trend > 0) R.color.healthy_green else R.color.caries_red)
            )
        }

        healthScoreCard.visibility = View.VISIBLE
        val animate = try {
            android.provider.Settings.Global.getFloat(
                contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            ) > 0f
        } catch (e: Exception) {
            true
        }
        if (animate) {
            gauge.setProgressCompat(0, false)
            valueText.text = "0"
            android.animation.ValueAnimator.ofInt(0, score).apply {
                duration = 900
                interpolator = android.view.animation.DecelerateInterpolator()
                addUpdateListener { a ->
                    val v = a.animatedValue as Int
                    gauge.setProgressCompat(v, false)
                    valueText.text = v.toString()
                }
                start()
            }
        } else {
            gauge.setProgressCompat(score, false)
            valueText.text = score.toString()
        }
    }

    /** Donut chart and counts of saved scans per condition (display only). */
    private fun showStats(scans: List<com.dentalscan.data.ScanEntity>, isUrdu: Boolean) {
        val order = listOf("Caries", "Calculus", "Gingivitis", "Healthy")
        val counts = order.map { name ->
            name to scans.count {
                if (name == "Healthy") ConditionStyle.isHealthy(it.condition)
                else ConditionStyle.of(it.condition)?.shortName == name
            }
        }.filter { it.second > 0 }

        if (counts.isEmpty()) {
            statsCard.visibility = View.GONE
            return
        }

        val total = counts.sumOf { it.second }
        statsTitle.text = if (isUrdu) "حالت کے لحاظ سے اسکین" else "Scans by condition"

        val dataSet = PieDataSet(
            counts.map { PieEntry(it.second.toFloat(), it.first) },
            ""
        ).apply {
            colors = counts.map { ConditionStyle.color(this@HistoryActivity, it.first) }
            sliceSpace = 2f
            setDrawValues(false)
        }

        statsChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 62f
            transparentCircleRadius = 0f
            setHoleColor(Color.TRANSPARENT)
            setDrawEntryLabels(false)
            setTouchEnabled(false)
            centerText = total.toString()
            setCenterTextSize(20f)
            setCenterTextColor(ContextCompat.getColor(this@HistoryActivity, R.color.text_primary))
            animateY(800)
            invalidate()
        }

        // Legend: colored dot, condition name, count
        statsLegend.removeAllViews()
        val density = resources.displayMetrics.density
        counts.forEach { (name, count) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, (3 * density).toInt(), 0, (3 * density).toInt())
            }
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams((10 * density).toInt(), (10 * density).toInt())
                    .apply { marginEnd = (8 * density).toInt() }
                setBackgroundResource(R.drawable.bg_dot)
                backgroundTintList = ColorStateList.valueOf(ConditionStyle.color(this@HistoryActivity, name))
            }
            val label = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = if (isUrdu) ConditionStyle.displayName(name, true) else name
                setTextAppearance(R.style.TextAppearance_App_Body)
            }
            val countView = TextView(this).apply {
                text = count.toString()
                setTextAppearance(R.style.TextAppearance_App_Title)
                textSize = 15f
            }
            row.addView(dot)
            row.addView(label)
            row.addView(countView)
            statsLegend.addView(row)
        }

        statsCard.visibility = View.VISIBLE
    }

    private fun showProgressFeedback(scans: List<com.dentalscan.data.ScanEntity>) {
        val isUrdu = LanguageManager.isUrdu(this)

        if (scans.size < 2) {
            progressFeedbackText.visibility = View.GONE
            return
        }

        // The list is newest-first, so the OLDER scans are at the end of it.
        // "first" = older half, "second" = newer half, so the trend reads in time order.
        val firstHalf = scans.takeLast(scans.size / 2)
        val secondHalf = scans.take(scans.size / 2)

        val firstHighCount = firstHalf.count { it.severity == "High" }
        val secondHighCount = secondHalf.count { it.severity == "High" }

        val message = when {
            secondHighCount < firstHighCount -> if (isUrdu)
                "شاباش! آپ کے دانتوں کی صحت بہتر ہو رہی ہے!"
            else
                "Great progress! Your dental health is improving!"

            secondHighCount == firstHighCount && firstHighCount == 0 -> if (isUrdu)
                "بہترین! آپ کے دانت مسلسل صحت مند ہیں!"
            else
                "Excellent! Your teeth have been consistently healthy!"

            secondHighCount == firstHighCount -> if (isUrdu)
                "آپ کی حالت مستحکم ہے۔ معمول برقرار رکھیں۔"
            else
                "Your condition is stable. Keep up your routine."

            else -> if (isUrdu)
                "حالت خراب ہو رہی ہے۔ براہ کرم ڈینٹسٹ سے ملیں۔"
            else
                "Condition worsening. Please consult a dentist soon."
        }

        progressFeedbackText.text = message
        progressFeedbackText.visibility = View.VISIBLE

        // Tinted background + matching icon instead of a solid color block
        val iconRes: Int
        val accentRes: Int
        val tintRes: Int
        when {
            secondHighCount < firstHighCount -> {
                iconRes = R.drawable.ic_trending_up
                accentRes = R.color.healthy_green
                tintRes = R.color.healthy_bg
            }
            secondHighCount == firstHighCount && firstHighCount == 0 -> {
                iconRes = R.drawable.ic_check_circle
                accentRes = R.color.healthy_green
                tintRes = R.color.healthy_bg
            }
            secondHighCount == firstHighCount -> {
                iconRes = R.drawable.ic_info
                accentRes = R.color.primary
                tintRes = R.color.surface_variant
            }
            else -> {
                iconRes = R.drawable.ic_warning
                accentRes = R.color.caries_red
                tintRes = R.color.caries_bg
            }
        }
        progressFeedbackText.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, tintRes))

        val size = (24 * resources.displayMetrics.density).toInt()
        val icon = ContextCompat.getDrawable(this, iconRes)?.mutate()
        icon?.setBounds(0, 0, size, size)
        icon?.setTint(ContextCompat.getColor(this, accentRes))
        progressFeedbackText.setCompoundDrawablesRelative(icon, null, null, null)
    }
}