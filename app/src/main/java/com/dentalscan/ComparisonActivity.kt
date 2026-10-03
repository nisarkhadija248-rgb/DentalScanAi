package com.dentalscan

import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.dentalscan.data.AppDatabase
import com.dentalscan.data.ScanEntity
import com.dentalscan.utils.LanguageManager
import com.dentalscan.utils.ProfileManager
import com.dentalscan.utils.ConditionStyle
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlin.math.abs
import kotlin.math.roundToInt
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class ComparisonActivity : BaseActivity() {

    private lateinit var conditionSpinner: Spinner
    private lateinit var chart: LineChart
    private lateinit var beforeImage: ImageView
    private lateinit var afterImage: ImageView
    private lateinit var beforeDate: TextView
    private lateinit var afterDate: TextView
    private lateinit var improvementText: TextView
    private lateinit var titleText: TextView
    private lateinit var trendLabel: TextView
    private lateinit var comparisonLabel: TextView
    private lateinit var sliderCard: View
    private lateinit var beforeAfterView: BeforeAfterView
    private lateinit var sliderHint: TextView

    private var allScans: List<ScanEntity> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val chartDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comparison)

        initViews()
        setupLanguage()
        loadData()
    }

    private fun initViews() {
        conditionSpinner = findViewById(R.id.conditionSpinner)
        chart = findViewById(R.id.trendChart)
        beforeImage = findViewById(R.id.beforeImage)
        afterImage = findViewById(R.id.afterImage)
        beforeDate = findViewById(R.id.beforeDate)
        afterDate = findViewById(R.id.afterDate)
        improvementText = findViewById(R.id.improvementText)
        titleText = findViewById(R.id.titleText)
        trendLabel = findViewById(R.id.trendLabel)
        comparisonLabel = findViewById(R.id.comparisonLabel)
        sliderCard = findViewById(R.id.sliderCard)
        beforeAfterView = findViewById(R.id.beforeAfterView)
        sliderHint = findViewById(R.id.sliderHint)
    }

    private fun setupLanguage() {
        val isUrdu = LanguageManager.isUrdu(this)

        titleText.text = if (isUrdu) "موازنہ" else "Compare Scans"
        trendLabel.text = if (isUrdu) "ترقی کا گراف" else "Progress Trend"
        comparisonLabel.text = if (isUrdu) "پہلے vs بعد" else "Before vs After"

        val conditions = if (isUrdu) {
            listOf("تمام حالات", "کیریز", "مسوڑوں کی سوزش", "کیلکولس")
        } else {
            listOf("All Conditions", "Caries", "Gingivitis", "Calculus")
        }

        conditionSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            conditions
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        conditionSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?,
                    position: Int, id: Long
                ) {
                    updateComparison()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(this@ComparisonActivity)
            val activeProfileId = ProfileManager.getActiveProfileId(this@ComparisonActivity)

            // Load scans for active profile only
            allScans = if (activeProfileId == -1L) {
                database.scanDao().getAllScans().first()
            } else {
                database.scanDao()
                    .getScansForUser(activeProfileId.toString())
                    .first()
            }

            withContext(Dispatchers.Main) {
                updateComparison()
            }
        }
    }

    private fun updateComparison() {
        val isUrdu = LanguageManager.isUrdu(this)

        if (allScans.size < 2) {
            improvementText.text = if (isUrdu)
                "موازنہ کے لیے کم از کم 2 اسکینز درکار ہیں"
            else
                "Need at least 2 scans for comparison"
            resetImprovementStyle()
            setSliderVisible(false)
            return
        }

        val conditionFilter = when (conditionSpinner.selectedItemPosition) {
            1 -> "Caries"
            2 -> "gingivitis"
            3 -> "Calculus"
            else -> "All"
        }

        val filteredScans = (if (conditionFilter == "All") allScans
        else allScans.filter { it.condition.equals(conditionFilter, ignoreCase = true) })
            .sortedBy { it.timestamp }

        if (filteredScans.size < 2) {
            improvementText.text = if (isUrdu)
                "اس حالت کے لیے کافی اسکینز نہیں"
            else
                "Not enough scans for this condition"
            resetImprovementStyle()
            setSliderVisible(false)
            return
        }

        val firstScan = filteredScans.first()
        val latestScan = filteredScans.last()

        // Load images (the same bitmaps feed the side-by-side cards and the slider)
        val beforeBitmap = BitmapFactory.decodeFile(firstScan.getThumbnailPath())
        val afterBitmap = BitmapFactory.decodeFile(latestScan.getThumbnailPath())
        beforeImage.setImageBitmap(beforeBitmap)
        afterImage.setImageBitmap(afterBitmap)

        beforeAfterView.setLabels(
            if (isUrdu) "پہلے" else "Before",
            if (isUrdu) "بعد" else "After"
        )
        beforeAfterView.setImages(beforeBitmap, afterBitmap)
        sliderHint.text = if (isUrdu) "موازنہ کے لیے ہینڈل کو کھینچیں" else "Drag the handle to compare"
        setSliderVisible(true)

        // Dates
        beforeDate.text = if (isUrdu)
            "پہلے: ${dateFormat.format(firstScan.timestamp)}"
        else
            "Before: ${dateFormat.format(firstScan.timestamp)}"

        afterDate.text = if (isUrdu)
            "بعد: ${dateFormat.format(latestScan.timestamp)}"
        else
            "After: ${dateFormat.format(latestScan.timestamp)}"

        // Improvement message
        val improvement = calculateImprovement(filteredScans)
        improvementText.text = when {
            improvement > 20 -> if (isUrdu)
                "بہتری ہوئی! اچھی حفظان صحت جاری رکھیں۔"
            else "Significant improvement! Keep up the good oral hygiene."
            improvement > 0 -> if (isUrdu)
                "تھوڑی بہتری دیکھی گئی۔"
            else "Slight improvement noted."
            improvement > -20 -> if (isUrdu)
                "حالت مستحکم ہے۔ موجودہ معمول برقرار رکھیں۔"
            else "Stable condition. Maintain current routine."
            else -> if (isUrdu)
                "حالت خراب ہو رہی ہے۔ براہ کرم ڈینٹسٹ سے رجوع کریں۔"
            else "Condition worsening. Please consult a dentist."
        }
        // Icon + tint replace the old symbol characters
        when {
            improvement > 20 -> styleImprovement(R.drawable.ic_trending_up, R.color.healthy_green, R.color.healthy_bg)
            improvement > 0 -> styleImprovement(R.drawable.ic_trending_up, R.color.healthy_green, R.color.healthy_bg)
            improvement > -20 -> styleImprovement(R.drawable.ic_info, R.color.primary, R.color.surface_variant)
            else -> styleImprovement(R.drawable.ic_warning, R.color.caries_red, R.color.caries_bg)
        }

        updateTrendChart(filteredScans)
    }

    private fun setSliderVisible(visible: Boolean) {
        val state = if (visible) View.VISIBLE else View.GONE
        sliderCard.visibility = state
        sliderHint.visibility = state
    }

    /** Neutral banner (no icon) for the "not enough scans" messages. */
    private fun resetImprovementStyle() {
        improvementText.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.surface_variant))
        improvementText.setCompoundDrawablesRelative(null, null, null, null)
    }

    /** Tinted background and matching leading icon for the result banner. */
    private fun styleImprovement(iconRes: Int, accentRes: Int, tintRes: Int) {
        improvementText.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, tintRes))
        val size = (24 * resources.displayMetrics.density).toInt()
        val icon = ContextCompat.getDrawable(this, iconRes)?.mutate()
        icon?.setBounds(0, 0, size, size)
        icon?.setTint(ContextCompat.getColor(this, accentRes))
        improvementText.setCompoundDrawablesRelative(icon, null, null, null)
    }

    private fun calculateImprovement(scans: List<ScanEntity>): Float {
        val earlySevere = scans.take(scans.size / 2).count { it.severity == "High" }
        val recentSevere = scans.takeLast(scans.size / 2).count { it.severity == "High" }
        return if (earlySevere == 0) 0f
        else ((earlySevere - recentSevere).toFloat() / earlySevere) * 100
    }

    private fun updateTrendChart(scans: List<ScanEntity>) {
        val isUrdu = LanguageManager.isUrdu(this)

        val entries = scans.mapIndexed { index, scan ->
            Entry(
                index.toFloat(), when {
                    ConditionStyle.isHealthy(scan.condition) -> 0f   // healthy scans sit at the bottom
                    scan.severity == "High" -> 3f
                    scan.severity == "Medium" -> 2f
                    else -> 1f
                }
            )
        }
        val hasHealthy = scans.any { ConditionStyle.isHealthy(it.condition) }

        val primary = ContextCompat.getColor(this, R.color.primary)
        val axisText = ContextCompat.getColor(this, R.color.text_secondary)
        val gridLineColor = ContextCompat.getColor(this, R.color.divider)

        val dataSet = LineDataSet(
            entries,
            if (isUrdu) "دانتوں کی صحت" else "Dental Health"
        ).apply {
            color = primary
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.15f
            lineWidth = 2.5f
            setDrawValues(false)
            // Each point is colored by the condition of that scan (red / orange / yellow)
            setCircleColors(scans.map { ConditionStyle.color(this@ComparisonActivity, it.condition) })
            circleRadius = 6f
            circleHoleRadius = 3f
            setDrawCircleHole(true)
            // Soft fill under the line
            setDrawFilled(true)
            fillColor = primary
            fillAlpha = 40
        }

        // Y axis reads Low / Medium / High instead of 1 / 2 / 3
        chart.axisRight.isEnabled = false
        chart.axisLeft.apply {
            textColor = axisText
            textSize = 11f
            axisMinimum = if (hasHealthy) -0.5f else 0.5f   // room for the "Healthy" level only when needed
            axisMaximum = 3.5f
            granularity = 1f
            isGranularityEnabled = true
            gridColor = gridLineColor
            axisLineColor = gridLineColor
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val level = value.roundToInt()
                    if (abs(value - level) > 0.01f) return ""
                    return when (level) {
                        0 -> if (isUrdu) "صحت مند" else "Healthy"
                        1 -> if (isUrdu) "کم" else "Low"
                        2 -> if (isUrdu) "درمیانی" else "Medium"
                        3 -> if (isUrdu) "زیادہ" else "High"
                        else -> ""
                    }
                }
            }
        }

        // X axis shows the date of each scan
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = axisText
            textSize = 11f
            granularity = 1f
            isGranularityEnabled = true
            setDrawGridLines(false)
            axisLineColor = gridLineColor
            axisMinimum = -0.3f
            axisMaximum = (scans.size - 1) + 0.3f
            setLabelCount(minOf(scans.size, 5), false)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val i = value.roundToInt()
                    return if (abs(value - i) < 0.01f && i in scans.indices) {
                        chartDateFormat.format(scans[i].timestamp)
                    } else ""
                }
            }
        }

        chart.legend.textColor = axisText
        chart.description.textColor = axisText
        chart.setExtraOffsets(4f, 8f, 12f, 8f)

        chart.data = LineData(dataSet)
        chart.description.text = if (isUrdu) "کم بہتر ہے" else "Lower is better"
        chart.animateX(900)
        chart.invalidate()
    }
}