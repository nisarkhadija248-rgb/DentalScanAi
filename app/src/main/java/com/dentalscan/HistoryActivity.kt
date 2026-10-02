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
        btnStartScanEmpty.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        val isUrdu = LanguageManager.isUrdu(this)
        titleText.text = if (isUrdu) "سابقہ ریکارڈ" else "Scan History"

        recyclerView.layoutManager = LinearLayoutManager(this)

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
            } else {
                emptyText.visibility = View.GONE
                emptyState.visibility = View.GONE
                historySubtitle.text = if (isUrdu) "${scans.size} ریکارڈ" else
                    if (scans.size == 1) "1 scan" else "${scans.size} scans"
                historySubtitle.visibility = View.VISIBLE
                recyclerView.visibility = View.VISIBLE
                val adapter = ScanHistoryAdapter { scan ->
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
                }
                adapter.submitList(scans)
                recyclerView.adapter = adapter
                showProgressFeedback(scans)
                showStats(scans, isUrdu)
            }
        }
    }

    /** Donut chart and counts of saved scans per condition (display only). */
    private fun showStats(scans: List<com.dentalscan.data.ScanEntity>, isUrdu: Boolean) {
        val order = listOf("Caries", "Calculus", "Gingivitis")
        val counts = order.map { name ->
            name to scans.count { ConditionStyle.of(it.condition)?.shortName == name }
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